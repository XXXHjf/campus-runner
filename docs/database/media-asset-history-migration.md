# 历史图片资源迁移

历史图片必须先登记到 `tb_media_asset` 并完成观察，之后才能停止旧字段兜底。不得把删字段、
删旧上传接口或批量删除 OSS 对象与历史迁移合并为一次发布。

## 数据来源与目标绑定

| 旧字段 | `purpose` | `bound_type` | 可见性 |
| --- | --- | --- | --- |
| `tb_category.image` | `ORDER_CATEGORY_ICON` | `ORDER_CATEGORY` | 公开 |
| `tb_second_hand_category.image`、`image_asset_id` | `SECOND_HAND_CATEGORY_ICON` | `SECOND_HAND_CATEGORY` | 公开 |
| `tb_second_hand_product.images` | `SECOND_HAND_PRODUCT_IMAGE` | `SECOND_HAND_PRODUCT` | 公开 |
| `tb_orders.image` | `ORDER_IMAGE` | `ORDER` | 私有 |
| `tb_take_orders.image` | `DELIVERY_PROOF` | `TAKE_ORDER` | 私有 |
| `tb_user.head_img` | `AVATAR` | `USER_AVATAR` | 公开 |
| `tb_user.student_id_card` | `STUDENT_CARD` | `USER_STUDENT_CARD` | 私有 |
| `tb_user.alipay_payment_code` | `PAYMENT_QR` | `USER_ALIPAY_PAYMENT` | 私有 |
| `tb_user.wechat_payment_code` | `PAYMENT_QR` | `USER_WECHAT_PAYMENT` | 私有 |
| `tb_banner.img_url` | `BANNER` | `BANNER` | 公开 |

二手商品按逗号拆分图片，`sort_order` 使用原始序号。查询会根据迁移台账排除已经成功迁移
的旧 URL，因此同一对象的新旧签名不会重复显示。

## 台账与重复执行

先执行 `media-asset-history-migration.sql` 创建 `tb_media_migration_ledger`。唯一键由来源表、
来源字段、业务 ID、图片序号和规范化 URL 哈希组成；成功记录再次执行时直接复用，失败记录
可以在修复源数据后重试。

台账状态含义：

- `MIGRATED`：已建立并核验新资源绑定。
- `SUPERSEDED`：单图业务已有另一个有效的新资源，旧值不再迁入。
- `FAILED`：源对象缺失、不可访问或图片校验失败；旧字段必须继续保留。
- `MANUAL_REQUIRED`：共享关系或异常情况无法自动安全处理。

本项目 OSS URL 会去除签名参数并从 OSS 实际读取；同一对象被多个业务引用时，为每个额外
绑定复制独立对象。外部 URL 只允许配置的可信 HTTPS 域名，下载并校验后上传到项目 OSS。
校验包括文件大小、JPEG/PNG/WebP 文件特征、MIME 和图片尺寸。

## 执行顺序

1. 备份数据库和当前 API JAR。
2. 创建迁移台账。
3. 部署包含迁移器的 API JAR，但先执行 `dry-run`。
4. 核对统计和失败原因后，显式确认执行 `apply`。
5. 执行 `verify`，再启动至少 7 天的兜底观察。

服务器命令：

```bash
/root/campus-runner-media-migration.sh dry-run
/root/campus-runner-media-migration.sh apply --confirm-writes
/root/campus-runner-media-migration.sh verify
/root/check-media-migration.sh start
/root/check-media-migration.sh status
```

`apply` 是唯一写入模式，并且需要双重显式开关。`dry-run` 不创建台账记录、不写数据库、
不写 OSS，只读取现有对象完成校验；`verify` 会确认台账、资源绑定和 OSS 对象一致。迁移
进程禁用 Web 服务和定时任务，不应与另一个迁移进程并行。

## 观察与清理门槛

应用仅在实际返回旧字段时记录 `LEGACY_MEDIA_FALLBACK`，日志不包含 URL，并按来源记录每
小时限流。`check-media-migration.sh` 从观察起点检查日志连续性、兜底次数、台账失败和 OSS
核验；只有连续满 7 天、日志连续、兜底为零且失败为零时才输出 `cleanup_eligible=true`。
日志发生截断或轮转导致连续性无法证明时，必须重新开始观察。

清理分两次发布：

1. 停止新旧字段的旧值写入和读取，只清空已成功迁移的旧值，保留字段结构；再次观察至少
   7 天。失败或人工处理记录不得清空。
2. 确认无需回滚后，删除旧字段、Mapper 映射、DTO/VO URL 属性、`POST /api/upload`、
   `CommonController`、上传拦截器和旧 `AliOSSUtil.upload()`。

任何阶段都不得按 OSS 目录直接删除。应导出 Bucket 对象清单，与
`tb_media_asset.object_key` 做差集，将候选对象先隔离或延迟删除；经过观察期后再物理删除。

## 失败修复与回滚

失败记录不伪造资源，也不改变旧字段，因此接口仍可兜底显示。修复源图片后重新运行
`apply --confirm-writes` 和 `verify`，再重新开始观察。

若 API 行为异常，使用发布前 JAR 通过仓库部署脚本回滚；迁移新增的 `tb_media_asset` 和台账
记录先保留，不要在故障处理中删除 OSS 对象。只有确认需要恢复数据库且能接受回退发布后
新增数据时，才使用同一时间点的数据库备份恢复。
