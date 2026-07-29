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
| `PAYMENT_QR` | `aliPaymentCodeAssetId` 或 `weChatPaymentCodeAssetId` | 每类 1 | 私有 |
| `BANNER` | `imageAssetId` | 1 | 公开 |

公开图片查询 URL 的签名有效期为 7 天。学生证、收款码、订单图片和送达凭证只能通过已完成
业务鉴权的接口返回，签名有效期为 15 分钟。

## 生命周期与兼容

- `TEMP` 资源只能由上传者绑定，且用途必须匹配。
- 已绑定资源不能通过临时资源删除接口删除。
- 替换或删除业务图片后，旧 OSS 对象进入 7 天延迟删除期。
- `tb_media_asset.bound_type + bound_id + sort_order` 是新图片与业务记录的标准关联。
- 旧业务表中的 URL 字段和二手分类 `image_asset_id` 暂时保留为历史数据回显兜底；新客户端不再写入签名 URL。
- 旧 `POST /api/upload` 暂时保留用于滚动升级兼容，但项目内客户端不得继续调用。

数据库首次接入执行 `docs/database/media-asset-migration.sql`；已执行第一阶段迁移的环境，在发布
本版本 API 前执行 `docs/database/media-asset-phase2-migration.sql`。
