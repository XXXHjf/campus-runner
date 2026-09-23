# 项目文档导航

本目录按当前代码维护业务约束与操作方法。接口路径先核对 `apps/api/src/main/java/.../controller/`，精确 DTO 和响应结构以当前运行版本生成的 OpenAPI 为准；数据库结构先核对迁移 SQL、Mapper 和目标环境实际表结构。生产环境状态无法仅由仓库代码证明。

## 开发与业务

- [项目入口](../README.md)；[协作约束](../AGENTS.md)；[小程序约束](../apps/mini-program/AGENTS.md)。
- [当前 API 分组](api/current-api-summary.md)、[管理端接口](api/admin-management.md)、[用户注册与认证](api/user-onboarding.md)、[地址簿](api/address-book.md)、[图片资源](api/media-assets.md)、[二手交易](api/second-hand.md)。
- [跑腿订单与接单接口](mini-program/api-orders.md)、[跑腿业务及资金流程](mini-program/order-flow.md)、[小程序组件与视觉基线](mini-program/ui-components.md)。

## 数据库与生产

- [数据库变更导航](database/README.md)：按目标环境实际版本选择迁移，不把历史建表快照当作现行结构。
- [生产服务器操作](deployment/server-operations.md)、[Nginx 配置](deployment/nginx-deployment.md)。发布前须执行生产发布必检清单。

## 历史资料

`archive/` 保存旧项目指南、旧表结构和过期流程，用于追溯，不作为现行实现或发布依据。旧版接口示例、一次性设计计划和生成快照已从日常导航移除；需要历史原文时可查 Git 历史。
