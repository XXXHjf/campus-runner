# 旧兼容链路清理发布说明

本次清理结束图片系统滚动兼容期。当前图片身份只存于 `tb_media_asset`，业务表通过
`bound_type + bound_id + sort_order` 关联；用户私下转账收款码、旧 URL 字段、二手商品
旧配送字段和运行时历史图片迁移器不再属于当前系统。

## 为什么不能滚动发布

旧版 API 仍会读写即将删除的列。数据库删列后旧进程会立即报 SQL 错误，因此本次必须采用
短暂停机发布：先停止全部旧进程，再备份和迁移数据库，最后启动新版 API。以后常规版本仍可
恢复原有滚动策略。

## 发布顺序

1. 确认历史图片迁移验证通过，所有未删除且旧图片字段有值的业务记录均存在状态为 `BOUND` 的新媒体绑定。
2. 停止旧版 API，并确认本机 `8080` 已无监听。
3. 完成全库备份，实际执行一次恢复演练或至少验证备份文件可被 MySQL 读取。
4. 执行 `docs/database/legacy-compatibility-cleanup.sql`。脚本在发现缺失绑定或缺失自提快照时会主动终止。
5. 启动已移除旧字段读写的新版 API。
6. 验证主页、登录、分类/轮播图、跑腿订单图片、送达凭证、头像、学生证和二手商品图片。

收款码媒体不会立即从 OSS 删除。脚本会解除其业务绑定并进入七天延迟删除期，由统一媒体清理任务处理。
历史迁移台账可保留作审计记录，但运行时不再读取它。

## 绑定校验失败

错误 `1644` 表示删列保护生效，不是 SQL 语法问题。以跑腿分类为例，可用以下只读查询定位
仍缺少有效绑定的未删除记录；查询不会输出旧图片 URL：

```sql
SELECT c.id, c.category_name, COALESCE(c.deleted, 0) AS deleted
FROM tb_category c
WHERE c.image IS NOT NULL
  AND TRIM(c.image) <> ''
  AND COALESCE(c.deleted, 0) = 0
  AND NOT EXISTS (
    SELECT 1
    FROM tb_media_asset m
    WHERE m.bound_type = 'ORDER_CATEGORY'
      AND m.bound_id = c.id
      AND m.status = 'BOUND'
  );
```

如果结果为空，说明阻塞项只有逻辑删除记录，使用当前版本清理脚本重新执行即可。若查询返回
未删除分类，应先在管理端为这些分类重新上传图标，让媒体资源建立 `ORDER_CATEGORY` 绑定；
确认不再使用的分类应先按正常业务流程删除。禁止通过移除校验或直接清空旧 URL 绕过迁移。

## 验证

执行脚本末尾的 `remaining_legacy_columns` 查询，结果必须为 `0`。随后确认：

- API 启动日志没有 MyBatis 列映射错误，也没有 MySQL `1045`。
- `tb_media_asset` 中当前业务图片保持 `BOUND`，查询接口仍能返回有效签名地址。
- 二手商品新建、编辑、下单时只使用 `pickup_address_snapshot` 和 `pickup_only`。
- `PAYMENT_QR` 资源已进入 `PENDING_DELETE`，小程序和 API 均不存在收款码入口。

## 回滚

删列会永久移除旧字段值，不能用补列代替数据回滚。若迁移后验证失败：

1. 停止新版 API。
2. 恢复迁移前的完整数据库备份。
3. 恢复迁移前的 API JAR 和客户端版本。
4. 使用仓库启动脚本启动 API，再重新执行发布后检查。

仅回滚 JAR 而不恢复数据库会使旧版继续访问不存在的列，禁止这样操作。
