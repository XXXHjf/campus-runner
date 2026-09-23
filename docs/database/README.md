# 数据库变更导航

`schema.txt` 是项目早期建表快照，缺少现行代码使用的跑腿代买、二手交易、媒体资源等表和列，已移至 `../archive/schema-initial.txt`。它不能用于重建或校验当前生产数据库。仓库 SQL 是分阶段迁移材料，也不代表任何环境已执行；上线前须只读核对目标环境结构和迁移记录，备份后按依赖顺序执行必要脚本。

| 主题 | 说明 | SQL |
| --- | --- | --- |
| 跑腿代买 | [业务字段与额度](purchase-order-migration.md) | `purchase-order-migration.sql` |
| 二手交易 | [线下交易模式](second-hand-offline-trade-migration.md)、[收藏](second-hand-favorite-migration.md)、[私信与历史卖家收款](second-hand-conversation-transfer-migration.md) | `second-hand-schema.sql`、对应迁移 SQL |
| 图片资源 | [历史图片迁移审计](media-asset-history-migration.md)、[旧兼容链路清理](legacy-compatibility-cleanup.md) | `media-asset-*.sql`、`legacy-compatibility-cleanup.sql` |
| 地址簿 | [移除地址类型](address-book-type-removal.md) | `address-book-type-removal.sql`、回滚 SQL |

`second-hand-delivery-migration.sql`、`second-hand-bargain-expiry-repair.sql` 和 `second-hand-seed.sql` 各有特定目的；不要把种子数据或历史修复脚本视为常规生产迁移。结构、发布顺序、验证和回滚方法以各主题文档和 [生产发布必检清单](../deployment/server-operations.md#生产发布必检清单) 为准。
