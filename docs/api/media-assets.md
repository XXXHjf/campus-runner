# 统一图片资源接口

项目内新增图片统一采用“临时上传、业务提交时绑定”的两阶段流程。客户端不再把上传接口返回的
签名 URL 保存到业务表。

## 标准流程

1. 客户端上传图片，获得 `mediaId` 和仅用于预览的 `previewUrl`。
2. 用户确认业务表单时，把 `mediaId` 随业务 DTO 一起提交。
3. API 在同一事务中写入业务记录并把临时资源绑定到该业务主键。
4. 用户替换或取消选择时，客户端主动释放未绑定资源；其他遗留临时资源在 24 小时后自动清理。
5. 查询业务数据时，API 根据业务权限生成新的签名 URL。

`previewUrl`、业务查询返回的图片 URL 都可能过期，不得持久化或作为资源身份使用。

## 上传和释放

用户端：

```http
POST /api/media/images
DELETE /api/media/images/{mediaId}
```

管理端：

```http
POST /admin/api/media/images
DELETE /admin/api/media/images/{mediaId}
```

上传使用 `multipart/form-data`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `file` | file | 图片文件 |
| `purpose` | string | 图片用途，见下表 |

成功结果：

```json
{
  "mediaId": 123,
  "previewUrl": "临时签名地址",
  "status": "TEMP",
  "expiresAt": "2026-07-30T12:00:00"
}
```

## 统一图片处理与压缩

所有新图片都通过上述上传入口处理。客户端可以在上传前做一次温和压缩以减少弱网耗时，
但客户端结果不作为可信输入；API 必须完成格式识别、完整解码、像素上限校验、EXIF 方向
纠正、去除元数据、按用途缩放和重新编码，再将处理后的文件写入 OSS。这样旧客户端、管理端
和绕过客户端校验的请求都会得到一致结果。

上传原文件最大为 10MB，单边不超过 12000 像素，总像素不超过 4000 万；支持 JPEG、PNG
和静态 WebP，动态图片会被拒绝。处理后的规则如下：

| `purpose` | 输出格式 | 最大尺寸 | 建议质量 | 最大输出 |
| --- | --- | --- | --- | --- |
| `ORDER_CATEGORY_ICON` | PNG | 512 × 512 | 无损 | 512KB |
| `SECOND_HAND_CATEGORY_ICON` | PNG | 512 × 512 | 无损 | 512KB |
| `SECOND_HAND_PRODUCT_IMAGE` | JPEG | 1600 × 1600 | 84% | 1MB |
| `ORDER_IMAGE` | JPEG | 1920 × 1920 | 85% | 1.2MB |
| `DELIVERY_PROOF` | JPEG | 1920 × 1920 | 88% | 1.4MB |
| `AVATAR` | JPEG | 512 × 512 | 85% | 400KB |
| `STUDENT_CARD` | JPEG | 2400 × 2400 | 90% | 1.8MB |
| `BANNER` | JPEG | 1404 × 440 | 88% | 800KB |

当首次编码超过用途上限时，API 会先逐步降低 JPEG 质量，再逐步缩小尺寸；PNG 仅缩小
尺寸，不转为有损格式。学生证照片优先保留文字清晰度，轮播图仍由
管理端完成比例裁切，但最终格式和容量由 API 统一兜底。

验证时至少覆盖大尺寸 JPEG、透明 PNG、WebP、伪造文件头、超像素图片以及不同用途的
输出尺寸和容量。回滚本处理规则不涉及数据库：可回滚 API JAR 和客户端版本，同时恢复
对应的 multipart 配置与生产启动脚本；已生成的标准图片仍可被旧版本正常读取。

## 上传失败处理与验证

上传入口前的 Nginx 请求上限必须与 Spring 的单请求 `20MB` 上限保持一致，具体配置、
验证及回滚见 [Nginx 图片上传大小限制](../deployment/nginx-deployment.md#图片上传大小限制)。
小程序公共上传服务先处理 HTTP 状态：413 提示压缩后重试、401 提示重新登录，其他
HTTP 错误及非 JSON 成功响应使用通用中文提示，不向用户展示 HTML 或内部错误。
HTTP 200 的业务失败保留 API 返回的可操作提示，正常上传和压缩规则不变。

在仓库根目录运行 `node --test tests/mini-program/mediaService.test.cjs`，覆盖上述响应
处理、上传前压缩和仍然超限时的本地拦截。该测试使用模拟的微信 API，不替代开发者工具
及真机上传验证；测试文件放在小程序目录之外，避免打入发布包。

## 用途与业务字段

| `purpose` | 业务提交字段 | 数量 | 可见性 |
| --- | --- | --- | --- |
| `ORDER_CATEGORY_ICON` | `imageAssetId` | 1 | 公开 |
| `SECOND_HAND_CATEGORY_ICON` | `imageAssetId` | 1 | 公开 |
| `SECOND_HAND_PRODUCT_IMAGE` | `imageAssetIds` | 最多 6 | 公开 |
| `ORDER_IMAGE` | `imageAssetId` | 1 | 私有 |
| `DELIVERY_PROOF` | `imageAssetId` | 1 | 私有 |
| `AVATAR` | `headImgAssetId` | 1 | 公开 |
| `STUDENT_CARD` | `studentIdCardAssetId` | 1 | 私有 |
| `BANNER` | `imageAssetId` | 1 | 公开 |

公开图片查询 URL 的签名有效期为 7 天。学生证、订单图片和送达凭证只能通过已完成
业务鉴权的接口返回，签名有效期为 15 分钟。

## 管理端学生认证图片回显

`GET /admin/api/auth/pendingList`（审核管理）与用户管理的待审核列表、用户详情均需在
管理员鉴权后解析媒体绑定。用户表查询本身不再包含头像和学生证图片，不能直接把查询结果
返回给审核页面，否则已有材料会显示为“无”。头像使用公开绑定解析，学生证使用
`resolveAuthorizedBinding(USER_STUDENT_CARD, userId, STUDENT_CARD)`，返回
`studentIdCardAssetId` 和短期签名地址 `studentIdCard`；不得改为公开资源或恢复旧图片列。

用户详情抽屉在“认证信息”中展示 `studentIdCard`，支持查看大图；全部用户、已认证及待审核
列表共用该详情入口。图片展示不以审核状态为条件，已审核材料仍可查看。加载失败时重新获取
详情以更新签名地址，无绑定时显示空状态。学生证审核状态须按 `0` 未审核、`1` 审核中、
`2` 已通过、`3` 未通过显示，不能复用仅判断 `1` 的认证状态逻辑。

验证：运行 `mvn -Dtest=AdminAuthServiceImplTest test`，覆盖有材料、无材料及空列表；
上线后使用管理员账号刷新审核列表并打开“查看材料”，确认学生证可预览，签名过期后刷新列表
可重新获取地址。该修复不涉及数据库迁移，回滚仅恢复发布前 API JAR，但会重新出现图片缺失。

## 生命周期

- `TEMP` 资源只能由上传者绑定，且用途必须匹配。
- 已绑定资源不能通过临时资源删除接口删除。
- 替换或删除业务图片后，旧 OSS 对象进入 7 天延迟删除期。
- `tb_media_asset.bound_type + bound_id + sort_order` 是新图片与业务记录的标准关联。
- 业务表不再存图片 URL 或媒体资源 id；`tb_media_asset` 绑定是唯一图片身份来源。
- 已弃用的 `/api/upload` 已删除，所有客户端必须使用本页的按用途上传接口。
- 历史图片运行时迁移器和旧字段回显兜底已经删除。

数据库首次接入执行 `docs/database/media-asset-migration.sql`；已执行第一阶段迁移的环境，在发布
本版本 API 前执行 `docs/database/media-asset-phase2-migration.sql`。
已完成历史迁移的环境执行 `docs/database/legacy-compatibility-cleanup.sql` 删除旧字段；停机发布、
验证和回滚要求见 `docs/database/legacy-compatibility-cleanup.md`。历史迁移文档与 SQL 仅保留作审计记录。
