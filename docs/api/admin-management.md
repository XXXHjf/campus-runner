# 管理端接口与页面

本页合并旧的接口速查、四份带示例数据的管理接口说明和一次性页面设计稿。路径以 `controller/admin/` 中的映射为准，精确请求及响应字段以对应 DTO/VO 和运行版本 OpenAPI 为准。示例数据不用于推断真实用户、金额或生产状态。

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

## 认证审核提醒

待审核角标统计未删除且审核中的用户；打开页面不会清零。前端每 60 秒刷新，页面隐藏时暂停，重新显示或本窗口审核成功后刷新；失败时隐藏旧数字。群提醒只发送待办人数与后台链接，不发送姓名、学号或证件图片。运行配置、验证和回滚见 [生产服务器操作](../deployment/server-operations.md#待审核认证群通知)。

## 核对与回退

接口变更时先核对 Controller、Service、DTO/VO，再更新本页；页面入口还须核对前端 router 和 service。可用管理员测试账号验证登录、一个列表与详情，以及受影响的写操作。文档变更无需部署；业务回退以实际版本、数据库迁移约束及 [生产服务器操作](../deployment/server-operations.md) 为准。
