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
