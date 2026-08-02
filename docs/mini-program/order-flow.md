# 发单与接单核心业务流程详解

## 1. 角色模型（物理同一用户，逻辑双角色）

系统“物理上”只有 `User`，不区分两套账号；在订单上下文中分为：
- 发单人（publisher）：创建订单并发起支付
- 接单人（taker）：接单、履约、最终收款

同一用户在不同订单里可扮演不同角色。

用户是否可参与发单/接单受认证状态约束：
- `authentication=0`：未认证
- `authentication=1`：认证通过，可进行发单接单

---

## 2. 核心业务对象

### 2.1 订单（tb_orders）

订单是主业务单，承载：
- 交易信息（price/service_fee/pay_amount）
- 履约信息（接单、派送、送达、确认）
- 结算信息（提现成功/失败）
- 支付异常信息（未支付/退款中/退款成功/退款异常）

### 2.2 接单视图（TakeOrderVO）

`takeOrders` 系列接口返回的是“接单视角的订单聚合信息”，本质仍围绕订单，只是附带接单侧时间与图片字段（如 `takeOrderCreateTime`、`takeOrderImage`）。

### 2.3 系统配置（tb_system_config）

服务费相关全局配置：
- `service_fee_rate`：服务费率（如 0.05）
- `service_fee_min`：最低服务费（如 0.5）

---

## 3. 金额模型（有偿/无偿）

### 3.1 字段定义

- `price`：订单基础金额（接单人最终报酬基数）
- `serviceFeeRate` / `service_fee_rate`：本单服务费率快照（下单时固化）
- `serviceFee` / `service_fee`：本单服务费金额
- `payAmount` / `pay_amount`：发单人支付总额

### 3.2 关系

- 有偿单：`payAmount = price + serviceFee`
- 无偿单：通常 `price=0, serviceFee=0, payAmount=0`（且客户端不触发支付）

### 3.3 服务费来源

- 费率与最低服务费来自系统配置接口：
  - `GET /admin/api/config/service_fee_rate`
  - `GET /admin/api/config/service_fee_min`

下单时应把结果写入订单快照字段，避免后续配置变更影响历史订单结算。

---

## 4. 订单状态机（最关键）

`tb_orders.status` 定义：
- `-4` 退款异常
- `-3` 退款成功
- `-2` 退款中
- `-1` 未支付
- `0` 待接单
- `1` 已接单
- `2` 派送中
- `3` 已送达
- `4` 已取消
- `5` 已完成
- `6` 提现成功
- `7` 提现失败

文档给出的正常完成路径：
- 无偿订单：`0 -> 1 -> 2 -> 3 -> 5`
- 有偿订单：`0 -> 1 -> 2 -> 3 -> 5 -> 6`

补充解释：
- `5` 表示履约闭环完成（发单人确认收货）
- `6/7` 表示有偿订单进入接单人收款（提现）结果态

---

## 5. 主流程（发单到收款）

## 5.1 发单阶段

1. 发单人填写订单表单：
- 取件地址、收件地址、订单说明、联系电话、分类、是否门禁、超时参数等
- 关键选项：是否支付跑腿费（决定有偿/无偿）

2. 平台计算金额（有偿时）：
- 读取 `service_fee_rate` 与 `service_fee_min`
- 计算 `serviceFee`
- 计算 `payAmount=price+serviceFee`

3. 创建订单：
- `POST /api/order`
- 请求体带入 `price/serviceFeeRate/serviceFee/payAmount` 等快照字段

4. 若有偿，发起微信支付：
- `POST /api/wx-pay/jspai/{orderId}`（文档路径拼写为 `jspai`）
- 返回 JSAPI 拉起支付参数：`prepayId/timeStamp/nonceStr/signType/paySign`

5. 微信支付异步回调：
- `POST /api/wx-pay/jsapi/notify`
- 回调成功后订单应可进入可被接单状态（`-1 -> 0`）

## 5.2 接单与履约阶段

1. 接单人接单：
- `POST /api/takeOrders/{id}`
- 订单状态进入 `1`（已接单）

2. 接单人更新履约状态：
- `PUT /api/takeOrders`
- 典型迁移：
  - `1`（已接单）-> `2`（派送中）
  - `2`（派送中）-> `3`（已送达，需上传送达图片）

3. 发单人确认收货：
- `PUT /api/order/confirm/{id}`
- 状态 `3 -> 5`

## 5.3 结算阶段（有偿单）

1. 订单完成后，接单人进入待收款：
- `GET /api/takeOrders/notWithdrawn` 可查询“已完成但未提现”订单

2. 平台向接单人打款（微信提现）：
- `POST /api/wx-transfer/transfer/{orderId}`
- 异步回调：`POST /api/wx-transfer/notify`

3. 根据转账结果更新订单：
- 成功：`5 -> 6`（提现成功）
- 失败：`5 -> 7`（提现失败）

---

## 6. 退款与取消分支

### 6.1 取消订单

- 接口：`PUT /api/order/cancel`
- 参数：`id/orderNumber/cancelReason`
- 适用场景（建议）：
  - 待接单（`0`）可取消
  - 已接单/派送中/已送达（`1/2/3`）不应由发单人直接取消，应走履约或申诉路径
  - 未支付（`-1`）建议走“超时自动删除”而非“取消+退款”
- 状态到 `4`（已取消）

### 6.2 微信退款

- 发起退款：`POST /api/wx-pay/refunds`
- 回调：`POST /api/wx-pay/refunds/notify`
- 关联状态：`-2`（退款中）、`-3`（退款成功）、`-4`（退款异常）

### 6.3 取消与退款的关系（有偿单重点）

有偿单在“已支付但尚未接单”场景下，推荐链路：
1. 发单人发起取消：`PUT /api/order/cancel`
2. 平台发起退款：`POST /api/wx-pay/refunds`
3. 等待退款回调：`POST /api/wx-pay/refunds/notify`
4. 订单进入退款结果态：`-3`（成功）或 `-4`（异常）

说明：
- `orderNumber` 是取消与退款链路的关键关联键，建议前后端统一透传。
- 退款处理为异步，不应以“发起退款接口返回成功”作为最终到账依据。

### 6.4 退款状态下的前端行为约束（当前最小可用策略）

- `-2` 退款中：
  - 主按钮显示“退款处理中”，禁用
  - 不允许删除订单（避免退款未决时用户误删）
  - 页面给出“处理中”提示文案
- `-3` 退款成功：
  - 允许用户删除订单（仅前台视图删除，后端建议保留流水）
- `-4` 退款异常：
  - 不建议直接给“删除订单”作为主操作
  - 主操作应引导“反馈异常”（跳转我的页意见反馈入口）

---

## 7. 关键字段字典

### 7.1 订单主字段（tb_orders）

- `id`：订单主键
- `order_number`：订单编号（支付、退款、查询链路关键关联键）
- `user_id`：发单人用户 ID
- `pick_up_address`：取件地址 ID
- `recive_address`：收件地址 ID（字段名有拼写 `recive`）
- `category_id`：订单类型 ID
- `note`：订单说明
- `image`：订单说明图片
- `phone`：发单电话
- `username`：发单昵称快照
- `door_access`：门禁标识（0 否 1 是）
- `gap`：超时间隔（单位需以后端约定为准）
- `exceed_time`：超时时间点
- `price`：订单基础金额（接单报酬）
- `service_fee_rate`：服务费率快照
- `service_fee`：服务费
- `pay_amount`：发单人支付总额
- `real_price`：文档标注“暂时禁用”
- `status`：订单状态机核心字段
- `delivery_time`：送达时间
- `cancel_time`：取消时间
- `cancel_reson`：取消原因（字段名 `reson` 为历史拼写）
- `create_time`：订单创建时间
- `deleted`：逻辑删除标记

### 7.2 接单视图字段（TakeOrderVO 补充）

- `orderId`：关联订单 ID
- `takeOrderCreateTime`：接单时间
- `takeOrderImage`：送达凭证图片
- `takeOrderDeliveryTime`：接单侧送达时间
- `takeOrderCancelTime`：接单侧取消时间
- `takeOrderCancelReason`：接单侧取消原因

## 8. 关键接口时序

有偿单推荐时序：
1. `POST /api/order` 创建订单（金额快照入库）
2. `POST /api/wx-pay/jspai/{orderId}` 获取微信预支付参数
3. 小程序发起支付
4. `POST /api/wx-pay/jsapi/notify` 支付回调（更新支付态）
5. `POST /api/takeOrders/{id}` 接单
6. `PUT /api/takeOrders` 更新为派送中
7. `PUT /api/takeOrders` 更新为已送达（含图片）
8. `PUT /api/order/confirm/{id}` 发单人确认收货
9. `POST /api/wx-transfer/transfer/{orderId}` 平台打款接单人
10. `POST /api/wx-transfer/notify` 转账回调，订单进入 `6/7`

无偿单时序：
- 可跳过第 2~4 步（微信支付）
- 完成路径到 `5` 即业务闭环

异常分支时序（有偿单取消退款）：
1. `PUT /api/order/cancel` 发单人取消订单
2. `POST /api/wx-pay/refunds` 发起退款
3. `POST /api/wx-pay/refunds/notify` 退款回调
4. 状态流转：`0 -> 4 -> -2 -> (-3 | -4)`（具体是否经过 `4` 取决于后端实现）

异常分支时序（未支付超时）：
1. 订单创建后停留 `-1`（未支付）
2. 达到超时阈值（当前客户端按 30 分钟倒计时）
3. 自动删除订单（不进入退款链路）

---

## 9. 最小结论

- 这是“同一用户双角色”的跑腿交易系统：发单人先下单，若为有偿则支付 `price + serviceFee`；接单人履约后，发单人确认收货，最后接单人收款。
- 订单是唯一主单据，`status` 覆盖支付、履约、取消、退款、提现全生命周期。
- 有偿订单核心路径：`下单 -> 微信支付 -> 接单 -> 派送 -> 送达 -> 确认 -> 提现成功`，对应状态主线 `0/1/2/3/5/6`（含支付前后与异常分支）。
- 结算金额上，接单人报酬是 `price`，平台收取 `serviceFee`；服务费按系统配置计算并在订单中做快照。
- 非正常链路需独立治理：取消不等于退款完成，退款结果以异步回调落态为准；`-2/-3/-4` 应有明确前端交互策略，避免误删与误导操作。
