# 项目文档导航

本目录按当前代码维护业务约束与操作方法。接口路径先核对 `apps/api/src/main/java/.../controller/`，精确 DTO 和响应结构以当前运行版本生成的 OpenAPI 为准；数据库结构先核对迁移 SQL、Mapper 和目标环境实际表结构。生产环境状态无法仅由仓库代码证明。

本轮源码核对日期：2026-10-09。文档描述仓库实现，不能据此认定小程序已上传、迁移已执行或生产开关已启用。代码中的已知缺口须明确记录，不能把拟议修复写成已实现规则。

## 开发与业务

- [项目入口](../README.md)；[协作约束](../AGENTS.md)；[小程序约束](../apps/mini-program/AGENTS.md)。
- [当前 API 分组](api/current-api-summary.md)、[管理端接口](api/admin-management.md)、[用户注册与认证](api/user-onboarding.md)、[地址簿](api/address-book.md)、[图片资源](api/media-assets.md)、[二手交易](api/second-hand.md)。
- [跑腿订单与接单接口](mini-program/api-orders.md)、[跑腿业务及资金流程](mini-program/order-flow.md)、[小程序组件与视觉基线](mini-program/ui-components.md)。

## 数据库与生产

- [数据库变更导航](database/README.md)：按目标环境实际版本选择迁移，不把历史建表快照当作现行结构。
- [生产服务器操作](deployment/server-operations.md)、[Nginx 配置](deployment/nginx-deployment.md)。发布前须执行生产发布必检清单。

## 维护与历史资料

同一主题只维护上文对应的领域文档；接口汇总只作导航，协作规则维护在 `AGENTS.md`，不复制到旧指南或设计计划。

旧项目指南、早期表结构、已过期 QA 已移除，需要时查 Git 历史。数据库迁移 SQL 保留作为增量升级材料，不能按文件名排序全部执行，也不能用历史建表脚本直接重建现行库。

`tmp/` 中的源码副本、`output/` 中的一次性审查、`.qa/` 和 `页面设计稿/` 的图片属于工作材料，不是当前标准或验收证明；日常文档核对应限定在本导航、两级 `AGENTS.md` 和对应源码，不能用这些材料覆盖现行实现。
