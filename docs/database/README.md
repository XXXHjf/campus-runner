# 数据库变更导航

早期 `schema.txt` 和旧管理端表结构快照已删除，历史原文通过 Git 追溯；它们缺少现行表和列，不能用于重建或校验数据库。仓库 SQL 是分阶段迁移材料（包含一次性 DDL、种子及历史审计脚本），也不代表任何环境已执行；上线前须只读核对目标环境结构和迁移记录，备份后按依赖顺序执行必要脚本。

| 主题 | 说明 | SQL |
| --- | --- | --- |
| 跑腿代买 | [业务字段与额度](purchase-order-migration.md) | `purchase-order-migration.sql` |
| 二手交易 | [线下交易模式](second-hand-offline-trade-migration.md)、[收藏](second-hand-favorite-migration.md)、[私信与历史卖家收款](second-hand-conversation-transfer-migration.md) | `second-hand-schema.sql`、对应迁移 SQL |
| 议价拒绝原因与商品变化失效 | [互斥原因选项、历史兼容和通知](../api/second-hand.md#拒绝议价原因与商品变化失效) | `second-hand-bargain-reject-reason.sql`、`second-hand-bargain-expiry-repair.sql`；2026-10-13已完成生产迁移，历史修复候选0条 |
| 图片资源 | [历史图片迁移审计](media-asset-history-migration.md)、[旧兼容链路清理](legacy-compatibility-cleanup.md) | `media-asset-*.sql`、`legacy-compatibility-cleanup.sql` |
| 校园认证 | [驳回反馈与状态规则](../api/user-onboarding.md#校园认证审核反馈) | `campus-auth-feedback-migration.sql` |
| 地址簿 | [移除地址类型](address-book-type-removal.md) | `address-book-type-removal.sql`、回滚 SQL |
| 跑腿金额异常审计 | [只读候选查询与资金核对](order-amount-audit.md) | 无写入 SQL |
| 跑腿预期送达时间 | [兼容与回滚](../mini-program/order-flow.md#预期送达时间) | `runner-expected-delivery-time.sql` |
| 跑腿订单隐藏 | [数据保留与回滚](../mini-program/order-flow.md#跑腿订单隐藏与数据保留) | `runner-order-hide.sql` |
| 跑腿订单地址快照 | [回填边界、备份及回滚](order-address-snapshot-migration.md) | `order-address-snapshot-migration.sql` |
| 两类订单号唯一性 | [迁移、验证与回滚](order-number-uniqueness.md) | `order-number-uniqueness.sql` |
| 创建请求幂等 | [账号隔离、恢复重试、发布及回滚](submission-idempotency.md) | `submission-idempotency.sql` |

`second-hand-delivery-migration.sql`、`second-hand-bargain-expiry-repair.sql` 和 `second-hand-seed.sql` 各有特定目的；不要把种子数据或历史修复脚本视为常规生产迁移。结构、发布顺序、验证和回滚方法以各主题文档和 [生产发布必检清单](../deployment/server-operations.md#生产发布必检清单) 为准。

- [请求保护共享计数与上传租约](request-guard.sql)：新增两个 InnoDB 表，2026-10-12 已完成生产建表与 API 发布，保护仍关闭；兼容小程序发布并验收后再显式开启。证据见 [业务发布记录](../deployment/server-operations.md#2026-10-12-业务发布记录)，阈值、代理及回滚统一见 [生产操作文档](../deployment/server-operations.md#请求频次与上传额度保护本地已实现生产尚未启用)。
