# 小程序订单与接单接口

精确 DTO 和响应模型以当前 API 的 OpenAPI 文档为准。本页记录小程序当前使用的稳定路径。

## 跑腿订单 `/api/order`

- `GET /api/order/public`：游客和未认证账号的公开预览，按创建时间倒序最多 100 条待接单订单。
  返回 ID、业务类型、商品金额、跑腿报酬、创建时间、预期时长、分类和校园楼栋；采用独立投影，排除手机号、姓名、
  订单号、自由文本说明、照片及地址簿详细地址。游客在这批预览上排序和筛选，不能视为全量搜索。
  查看含取件说明的详情和接单前由客户端引导登录及校园认证；私有接口继续要求登录。
  验证匿名预览无上述个人字段、匿名详情和写入返回 401。先部署 API 再上传小程序，
  无数据库迁移；回滚需保留公开读取兼容或同步回滚客户端。

- `POST /api/order/amount-preview`：按分类和金额返回服务端计算的服务费、实付、接单人应收及额度；发布订单前使用。
- `POST /api/order`：发布订单；图片只提交 `imageAssetId`。服务端依据启用分类重新判断业务类型并计算金额。
  小程序发单只填写一个必填的订单描述（最多 100 字），直接提交为 `note`；无需另填短标题。
  这样发单和订单展示使用同一份内容。旧草稿中的短标题与详细要求在恢复时合并为描述。
  验证时检查旧草稿恢复、空描述拦截和跑腿大厅展示；如需回滚，仅恢复小程序表单与草稿处理，接口和数据库无需回滚。
- `GET /api/order/my`：查询本人订单。
- `GET /api/order/detail/{id}`：查询订单详情。
- `GET /api/order/showByPrice/{status}`、`showByTime/{status}`、`showByCategory/{id}`：订单列表筛选。
- `PUT /api/order/cancel`：取消订单。
- `PUT /api/order/{id}/content`：发单人仅在待接单时修改 `note` 与 `imageAssetId`；说明至少 4 个非空白字符、最多 100 个字符，图片必填且限一张。金额和履约信息不可修改。
- `PUT /api/order/confirm/{id}`：确认收货。
- `DELETE /api/order/{id}`：逻辑删除订单；当前服务端尚未检查订单状态和归属，客户端应限制入口，服务端校验缺口见 [资金流程](order-flow.md#操作与异常)。

## 接单 `/api/takeOrders`

- `POST /api/takeOrders/{id}`：接单。
- `PUT /api/takeOrders`：更新接单状态；普通送达和代买购买凭证均只提交相应用途图片的 `imageAssetId`。代买必须先提交购买凭证才能进入配送中。
- `GET /api/takeOrders`、`GET /api/takeOrders/query`：查询接单列表。
- `GET /api/takeOrders/{orderId}`：查询订单的接单人资料。
- `GET /api/takeOrders/image/{orderId}`：在权限校验后获取当前送达凭证地址。
- `GET /api/takeOrders/notWithdrawn`：查询已完成但尚未结算的接单记录。

## 支付与收款

跑腿支付使用 `/api/wx-pay`，骑手结算使用 `/api/wx-transfer`。用户收款码和私下扫码转账接口
已经下线，客户端不得保存或查询个人收款码。

代买的商品金额与跑腿报酬在发单人确认完成后合并为一笔接单人转账。代买用户实付和接单人收款共用 `runner_transfer_single_max` 上限，管理员按商家转账单笔额度同步调整。

图片上传统一使用 `/api/media/images`，规则见 [统一图片资源接口](../api/media-assets.md)。
