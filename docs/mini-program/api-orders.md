# 小程序订单与接单接口

精确 DTO 和响应模型以当前 API 的 OpenAPI 文档为准。本页记录小程序当前使用的稳定路径。

## 跑腿订单 `/api/order`

- `POST /api/order`：发布订单；图片只提交 `imageAssetId`。
- `GET /api/order/my`：查询本人订单。
- `GET /api/order/detail/{id}`：查询订单详情。
- `GET /api/order/showByPrice/{status}`、`showByTime/{status}`、`showByCategory/{id}`：订单列表筛选。
- `PUT /api/order/cancel`：取消订单。
- `PUT /api/order/confirm/{id}`：确认收货。
- `DELETE /api/order/{id}`：删除允许删除的订单。

## 接单 `/api/takeOrders`

- `POST /api/takeOrders/{id}`：接单。
- `PUT /api/takeOrders`：更新接单状态；送达凭证只提交 `imageAssetId`。
- `GET /api/takeOrders`、`GET /api/takeOrders/query`：查询接单列表。
- `GET /api/takeOrders/{orderId}`：查询订单的接单人资料。
- `GET /api/takeOrders/image/{orderId}`：在权限校验后获取当前送达凭证地址。
- `GET /api/takeOrders/notWithdrawn`：查询已完成但尚未结算的接单记录。

## 支付与收款

跑腿支付使用 `/api/wx-pay`，骑手结算使用 `/api/wx-transfer`。用户收款码和私下扫码转账接口
已经下线，客户端不得保存或查询个人收款码。

图片上传统一使用 `/api/media/images`，规则见 [统一图片资源接口](../api/media-assets.md)。
