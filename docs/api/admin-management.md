# 管理端接口与页面

路径以 `controller/admin/` 中的映射为准，精确请求及响应字段以对应 DTO/VO 和运行版本 OpenAPI 为准。页面视觉及组件约束见根目录 `AGENTS.md`，不另维护一套管理端设计标准。

管理端登录为 `POST /admin/api/login`；其余管理接口需要管理员令牌，公开轮播图读取例外。管理后台路由定义在 `apps/admin/src/router/index.tsx`，订单、接单、用户、分类页面均已接入，不再是占位页。

## 路径

| 模块 | 当前路径 | 作用 |
| --- | --- | --- |
| 分类 | `GET/POST /admin/api/categories`、`PUT/DELETE /admin/api/categories/{id}` | 列表、新增、修改、逻辑删除 |
| 用户 | `GET /admin/api/users/{all,authenticated,pending-review,statistics,id}` | 分页列表、统计、详情 |
| 订单 | `GET /admin/api/orders/{all,waiting,in-progress,completed,canceled,statistics,id}` | 分页列表、统计、详情 |
| 订单操作 | `POST /admin/api/orders/{id}/{cancel,refund}` | 请求体包含 `reason`；是否允许操作由服务端状态检查决定 |
| 接单 | `GET /admin/api/take-orders/{all,unpaid,statistics}` | 分页列表、未收款、统计 |
| 配置 | `GET/PUT /admin/api/config/{service_fee_rate,service_fee_min,runner_transfer_single_max,second_hand_service_fee_rate}` | 跑腿计费及额度；二手费率仅供历史线上订单兼容 |
| 待审核人数 | `GET /admin/api/kpi/pending-auth` | 返回待审核总人数，角标不是未读数 |

表中的花括号表示分别取其中一个路径片段，`id` 表示实际数字 ID。分页列表接口的 `page`、`pageSize` 是必传查询参数。管理端前端路由和后端路径不是同一套命名，例如 `/users/pending-auth` 页面请求 `/admin/api/users/pending-review`；`/takes/withdrawn` 页面展示未收款数据。

用户、订单、接单列表统一拒绝 `page < 1`、`pageSize` 不在 1–50 范围及超出旧 Mapper 整数偏移容量的请求；取数前校验，不能通过巨大页大小绕过分页或产生负偏移。创建时间相同时按编号降序稳定排序。用户头像和接单送达凭证按当前页批量解析，完整总数仍从匹配范围计算；接单凭证仅在管理员鉴权的管理查询范围内签名，不开放客户端私有批量入口。详情授权与操作规则不变。

旧 `GET /admin/api/auth/pendingList` 保留数组响应，接受可选 `page/pageSize`（默认 1/20，最大 50），按未删除待审核用户、原学校关联条件查询，时间/编号降序分页；头像与学生证按页分别公开/授权批量解析。当前前端没有调用该旧接口，审核页继续使用 `/admin/api/users/pending-review` 和用户详情，待审核角标仍用完整计数。旧内部查询有首屏上限；读取全部旧列表的外部调用须传页码逐页加载。API 更新时核对外部旧调用方，不能将数组长度作为待审核总数。无数据库迁移，回退这些分页边界或批量解析时保留原业务记录及管理员鉴权。

用户、订单、接单列表支持可选的 `keyword` 查询参数。服务端先按当前列表状态和关键词筛选，再分页；响应 `total` 是匹配总数。用户匹配昵称、姓名、手机号、学校名或用户 ID；订单匹配订单编号、发单人或联系电话；接单匹配订单编号、发单人、接单人或接单人电话。空关键词返回原列表。管理端搜索时回到第一页，切换标签时清空搜索。

订单取消自动退款、退款重试及历史漏退处理以 [跑腿订单与资金流程](../mini-program/order-flow.md#取消关单与退款规则) 为准。已接单及结算中的订单不开放独立退款。取消与退款成功响应的 `data` 返回 `orderStatus`、`refundNumber`、`refundStatus`；后者区分本地申请已受理 `REQUESTED`、微信处理中 `PROCESSING`、确认成功 `SUCCESS` 和失败/异常。业务失败返回 `code=0` 和可操作的中文原因，页面不能将申请提交视为到账。

## 认证审核提醒

待审核角标统计未删除且审核中的用户；打开页面不会清零。前端每 60 秒刷新，页面隐藏时暂停，重新显示或本窗口审核成功后刷新；失败时隐藏旧数字。群提醒只发送待办人数与后台链接，不发送姓名、学号或证件图片。运行配置、验证和回滚见 [生产服务器操作](../deployment/server-operations.md#待审核认证群通知)。

## 核对与回退

接口变更时先核对 Controller、Service、DTO/VO，再更新本页；页面入口还须核对前端 router 和 service。可用管理员测试账号验证登录、一个列表与详情，以及受影响的写操作。文档变更无需部署；业务回退以实际版本、数据库迁移约束及 [生产服务器操作](../deployment/server-operations.md) 为准。
