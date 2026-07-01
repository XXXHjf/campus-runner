---
title: 帮帮送校园跑腿项目接口文档
language_tabs:
  - shell: Shell
  - http: HTTP
  - javascript: JavaScript
  - ruby: Ruby
  - python: Python
  - php: PHP
  - java: Java
  - go: Go
toc_footers: []
includes: []
search: true
code_clipboard: true
highlight_theme: darkula
headingLevel: 2
generator: "@tarslib/widdershins v4.0.30"

---

# 帮帮送校园跑腿项目接口文档

campusRunner接口文档

Base URLs:

License: <a href="http://springdoc.org">Apache 2.0</a>

# Authentication

* API Key (apikey-header-token)
    - Parameter Name: **token**, in: header. 

# 接单相关接口

<a id="opIdmy"></a>

## GET 我的接单

GET /api/takeOrders

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":[{"id":0,"orderId":0,"pickUpAddress":"string","reciveAddress":"string","orderNumber":"string","price":0,"realPrice":0,"deliveryTime":"2019-08-24T14:15:22Z","cancelTime":"2019-08-24T14:15:22Z","cancelReson":"string","exceedTime":"2019-08-24T14:15:22Z","gap":0,"createTime":"2019-08-24T14:15:22Z","doorAccess":0,"userId":0,"username":"string","phone":"string","realname":"string","status":0,"note":"string","image":"string","categoryId":0,"categoryImage":"string","categoryName":"string","takeOrderCreateTime":"2019-08-24T14:15:22Z","takeOrderImage":"string","takeOrderDeliveryTime":"2019-08-24T14:15:22Z","takeOrderCancelTime":"2019-08-24T14:15:22Z","takeOrderCancelReason":"string"}]}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultListTakeOrderVO](#schemaresultlisttakeordervo)|

<a id="opIdupdateStatus"></a>

## PUT 修改状态

PUT /api/takeOrders

用于更新订单状态，包括：
- 接单 → 派送中
- 派送中 → 派送完成（需上传送达图片）
- 订单取消（需填写取消原因）

| 参数名        | 类型     | 是否必填 | 说明                                   |
|---------------|----------|----------|----------------------------------------|
| `id`          | integer  | ✅       | 订单 ID                                |
| `status`      | integer  | ✅       | 订单状态码：<br>1 = 派送中<br>2 = 已完成<br>48 = 已取消 |
| `image`       | string   | ❌       | 派送完成时上传的图片 URL（仅 status=2 时必填） |
| `cancelReason`| string   | ❌       | 取消订单原因（仅 status=4时必填）   |

### 示例请求

#### 1. 接单 → 派送中
```json
{
  "id": 24,
  "status": 1
}

> Body 请求参数

```json
{
  "id": 0,
  "status": 0,
  "image": "string",
  "cancelReason": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[TakeOrderUpdateStatusDTO](#schematakeorderupdatestatusdto)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[Result](#schemaresult)|

<a id="opIdtakeorder"></a>

## POST 接单

POST /api/takeOrders/{id}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer(int64)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[Result](#schemaresult)|

<a id="opIduserinfo"></a>

## GET 根据orderId查询,返回接单人用户信息

GET /api/takeOrders/{orderId}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|orderId|path|integer(int64)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{"username":"string","realname":"string","phone":"string","takeOrderTime":"2019-08-24T14:15:22Z","id":0}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultTakeOrderUserInfoVO](#schemaresulttakeorderuserinfovo)|

<a id="opIdquery"></a>

## GET 条件查询

GET /api/takeOrders/query

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|status|query|integer(int32)| 否 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":[{"id":0,"orderId":0,"pickUpAddress":"string","reciveAddress":"string","orderNumber":"string","price":0,"realPrice":0,"deliveryTime":"2019-08-24T14:15:22Z","cancelTime":"2019-08-24T14:15:22Z","cancelReson":"string","exceedTime":"2019-08-24T14:15:22Z","gap":0,"createTime":"2019-08-24T14:15:22Z","doorAccess":0,"userId":0,"username":"string","phone":"string","realname":"string","status":0,"note":"string","image":"string","categoryId":0,"categoryImage":"string","categoryName":"string","takeOrderCreateTime":"2019-08-24T14:15:22Z","takeOrderImage":"string","takeOrderDeliveryTime":"2019-08-24T14:15:22Z","takeOrderCancelTime":"2019-08-24T14:15:22Z","takeOrderCancelReason":"string"}]}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultListTakeOrderVO](#schemaresultlisttakeordervo)|

<a id="opIdgetPaymentCodeByOrderId"></a>

## GET 根据订单id查询接单人收款码

GET /api/takeOrders/paymentCode/{orderId}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|orderId|path|integer(int64)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{"aliPaymentCode":"string","weChatPaymentCode":"string"}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultUserPaymentVO](#schemaresultuserpaymentvo)|

<a id="opIdgetNoWithdrawn"></a>

## GET 查询当前用户接单已完成但未提现订单

GET /api/takeOrders/notWithdrawn

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":[{"id":0,"orderId":0,"pickUpAddress":"string","reciveAddress":"string","orderNumber":"string","price":0,"realPrice":0,"deliveryTime":"2019-08-24T14:15:22Z","cancelTime":"2019-08-24T14:15:22Z","cancelReson":"string","exceedTime":"2019-08-24T14:15:22Z","gap":0,"createTime":"2019-08-24T14:15:22Z","doorAccess":0,"userId":0,"username":"string","phone":"string","realname":"string","status":0,"note":"string","image":"string","categoryId":0,"categoryImage":"string","categoryName":"string","takeOrderCreateTime":"2019-08-24T14:15:22Z","takeOrderImage":"string","takeOrderDeliveryTime":"2019-08-24T14:15:22Z","takeOrderCancelTime":"2019-08-24T14:15:22Z","takeOrderCancelReason":"string"}]}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultListTakeOrderVO](#schemaresultlisttakeordervo)|

<a id="opIdgetImageByOrderId"></a>

## GET 根据订单id查询送达图片

GET /api/takeOrders/image/{orderId}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|orderId|path|integer(int64)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":"string"}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultString](#schemaresultstring)|

# 订单相关接口

<a id="opIdconfirm"></a>

## PUT 发单人确认订单已送达

PUT /api/order/confirm/{id}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer(int64)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[Result](#schemaresult)|

<a id="opIdcancel"></a>

## PUT 取消订单

PUT /api/order/cancel

> Body 请求参数

```json
{
  "id": 0,
  "orderNumber": "string",
  "cancelReason": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[OrderCancelDTO](#schemaordercanceldto)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[Result](#schemaresult)|

<a id="opIdsubmit"></a>

## POST 发布订单

POST /api/order

> Body 请求参数

```json
{
  "pickUpAddress": 0,
  "reciveAddress": 0,
  "price": 0,
  "service_fee_rate": 0,
  "service_fee": 0,
  "pay_amount": 0,
  "gap": 0,
  "createTime": "2019-08-24T14:15:22Z",
  "doorAccess": 0,
  "username": "string",
  "phone": "string",
  "note": "string",
  "image": "string",
  "categoryId": 0
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[OrderSubmitDTO](#schemaordersubmitdto)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{"id":0,"pickUpAddress":0,"reciveAddress":0,"orderNumber":"string","price":0,"service_fee_rate":0,"service_fee":0,"pay_amount":0,"realPrice":0,"deliveryTime":"2019-08-24T14:15:22Z","cancelTime":"2019-08-24T14:15:22Z","cancelReson":"string","exceedTime":"2019-08-24T14:15:22Z","gap":0,"createTime":"2019-08-24T14:15:22Z","doorAccess":0,"userId":0,"username":"string","phone":"string","status":0,"note":"string","image":"string","categoryId":0,"deleted":0}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultOrder](#schemaresultorder)|

<a id="opIdshowByTime"></a>

## GET 综合排序

GET /api/order/showByTime/{status}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|status|path|integer(int32)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":[{"id":0,"pickUpAddress":"string","reciveAddress":"string","orderNumber":"string","price":0,"realPrice":0,"deliveryTime":"2019-08-24T14:15:22Z","cancelTime":"2019-08-24T14:15:22Z","cancelReson":"string","exceedTime":"2019-08-24T14:15:22Z","gap":0,"createTime":"2019-08-24T14:15:22Z","doorAccess":0,"userId":0,"username":"string","phone":"string","status":0,"note":"string","image":"string","deleted":0,"categoryImage":"string","categoryName":"string","realname":"string"}]}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultListOrderShowVO](#schemaresultlistordershowvo)|

<a id="opIdshowByReciveAdd"></a>

## GET 按照收件地址筛选

GET /api/order/showByReciveAdd

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|schoolNumberId|query|integer(int64)| 是 |none|
|compusNumberId|query|integer(int64)| 是 |none|
|buildCategoryNumberId|query|integer(int64)| 是 |none|
|buildingNumberId|query|integer(int64)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":[{"id":0,"pickUpAddress":"string","reciveAddress":"string","orderNumber":"string","price":0,"realPrice":0,"deliveryTime":"2019-08-24T14:15:22Z","cancelTime":"2019-08-24T14:15:22Z","cancelReson":"string","exceedTime":"2019-08-24T14:15:22Z","gap":0,"createTime":"2019-08-24T14:15:22Z","doorAccess":0,"userId":0,"username":"string","phone":"string","status":0,"note":"string","image":"string","deleted":0,"categoryImage":"string","categoryName":"string","realname":"string"}]}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultListOrderShowVO](#schemaresultlistordershowvo)|

<a id="opIdshowByPrice"></a>

## GET 价格优先排序

GET /api/order/showByPrice/{status}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|status|path|integer(int32)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":[{"id":0,"pickUpAddress":"string","reciveAddress":"string","orderNumber":"string","price":0,"realPrice":0,"deliveryTime":"2019-08-24T14:15:22Z","cancelTime":"2019-08-24T14:15:22Z","cancelReson":"string","exceedTime":"2019-08-24T14:15:22Z","gap":0,"createTime":"2019-08-24T14:15:22Z","doorAccess":0,"userId":0,"username":"string","phone":"string","status":0,"note":"string","image":"string","deleted":0,"categoryImage":"string","categoryName":"string","realname":"string"}]}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultListOrderShowVO](#schemaresultlistordershowvo)|

<a id="opIdshowByPickUpAdd"></a>

## GET 按照取件地址筛选

GET /api/order/showByPickUpAdd

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|schoolNumberId|query|integer(int64)| 是 |none|
|compusNumberId|query|integer(int64)| 是 |none|
|buildCategoryNumberId|query|integer(int64)| 是 |none|
|buildingNumberId|query|integer(int64)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":[{"id":0,"pickUpAddress":"string","reciveAddress":"string","orderNumber":"string","price":0,"realPrice":0,"deliveryTime":"2019-08-24T14:15:22Z","cancelTime":"2019-08-24T14:15:22Z","cancelReson":"string","exceedTime":"2019-08-24T14:15:22Z","gap":0,"createTime":"2019-08-24T14:15:22Z","doorAccess":0,"userId":0,"username":"string","phone":"string","status":0,"note":"string","image":"string","deleted":0,"categoryImage":"string","categoryName":"string","realname":"string"}]}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultListOrderShowVO](#schemaresultlistordershowvo)|

<a id="opIdshowByDoubleAdd"></a>

## GET 地址双向筛选

GET /api/order/showByDoubleAdd

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|pickSchoolNumberId|query|integer(int64)| 否 |none|
|pickCompusNumberId|query|integer(int64)| 否 |none|
|pickBuildCategoryNumberId|query|integer(int64)| 否 |none|
|pickBuildingNumberId|query|integer(int64)| 否 |none|
|reciveSchoolNumberId|query|integer(int64)| 否 |none|
|reciveCompusNumberId|query|integer(int64)| 否 |none|
|reciveBuildCategoryNumberId|query|integer(int64)| 否 |none|
|reciveBuildingNumberId|query|integer(int64)| 否 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":[{"id":0,"pickUpAddress":"string","reciveAddress":"string","orderNumber":"string","price":0,"realPrice":0,"deliveryTime":"2019-08-24T14:15:22Z","cancelTime":"2019-08-24T14:15:22Z","cancelReson":"string","exceedTime":"2019-08-24T14:15:22Z","gap":0,"createTime":"2019-08-24T14:15:22Z","doorAccess":0,"userId":0,"username":"string","phone":"string","status":0,"note":"string","image":"string","deleted":0,"categoryImage":"string","categoryName":"string","realname":"string"}]}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultListOrderShowVO](#schemaresultlistordershowvo)|

<a id="opIdshowByCategory"></a>

## GET 按照订单类型筛选

GET /api/order/showByCategory/{id}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer(int64)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":[{"id":0,"pickUpAddress":"string","reciveAddress":"string","orderNumber":"string","price":0,"realPrice":0,"deliveryTime":"2019-08-24T14:15:22Z","cancelTime":"2019-08-24T14:15:22Z","cancelReson":"string","exceedTime":"2019-08-24T14:15:22Z","gap":0,"createTime":"2019-08-24T14:15:22Z","doorAccess":0,"userId":0,"username":"string","phone":"string","status":0,"note":"string","image":"string","deleted":0,"categoryImage":"string","categoryName":"string","realname":"string"}]}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultListOrderShowVO](#schemaresultlistordershowvo)|

<a id="opIdgetOrderStatus"></a>

## GET 查询订单状态

GET /api/order/orderStatus/{orderId}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|orderId|path|integer(int64)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":0}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultInteger](#schemaresultinteger)|

<a id="opIdshowMy"></a>

## GET 查看我发布的订单

GET /api/order/my

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":[{"id":0,"pickUpAddress":"string","reciveAddress":"string","orderNumber":"string","price":0,"realPrice":0,"deliveryTime":"2019-08-24T14:15:22Z","cancelTime":"2019-08-24T14:15:22Z","cancelReson":"string","exceedTime":"2019-08-24T14:15:22Z","gap":0,"createTime":"2019-08-24T14:15:22Z","doorAccess":0,"userId":0,"username":"string","phone":"string","status":0,"note":"string","image":"string","deleted":0,"categoryImage":"string","categoryName":"string","realname":"string"}]}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultListOrderShowVO](#schemaresultlistordershowvo)|

<a id="opIddetail"></a>

## GET 详细查询

GET /api/order/detail/{id}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer(int64)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{"id":0,"pickUpAddress":"string","reciveAddress":"string","orderNumber":"string","price":0,"realPrice":0,"deliveryTime":"2019-08-24T14:15:22Z","cancelTime":"2019-08-24T14:15:22Z","cancelReson":"string","exceedTime":"2019-08-24T14:15:22Z","gap":0,"createTime":"2019-08-24T14:15:22Z","doorAccess":0,"userId":0,"username":"string","phone":"string","status":0,"note":"string","image":"string","deleted":0,"categoryImage":"string","categoryName":"string","realname":"string"}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultOrderShowVO](#schemaresultordershowvo)|

<a id="opIddelete"></a>

## DELETE 删除订单

DELETE /api/order/{id}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer(int64)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[Result](#schemaresult)|

# 数据模型

<h2 id="tocS_TakeOrderUpdateStatusDTO">TakeOrderUpdateStatusDTO</h2>

<a id="schematakeorderupdatestatusdto"></a>
<a id="schema_TakeOrderUpdateStatusDTO"></a>
<a id="tocStakeorderupdatestatusdto"></a>
<a id="tocstakeorderupdatestatusdto"></a>

```json
{
  "id": 0,
  "status": 0,
  "image": "string",
  "cancelReason": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||none|
|status|integer(int32)|false|none||none|
|image|string|false|none||none|
|cancelReason|string|false|none||none|

<h2 id="tocS_OrderCancelDTO">OrderCancelDTO</h2>

<a id="schemaordercanceldto"></a>
<a id="schema_OrderCancelDTO"></a>
<a id="tocSordercanceldto"></a>
<a id="tocsordercanceldto"></a>

```json
{
  "id": 0,
  "orderNumber": "string",
  "cancelReason": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||none|
|orderNumber|string|false|none||none|
|cancelReason|string|false|none||none|

<h2 id="tocS_OrderSubmitDTO">OrderSubmitDTO</h2>

<a id="schemaordersubmitdto"></a>
<a id="schema_OrderSubmitDTO"></a>
<a id="tocSordersubmitdto"></a>
<a id="tocsordersubmitdto"></a>

```json
{
  "pickUpAddress": 0,
  "reciveAddress": 0,
  "price": 0,
  "service_fee_rate": 0,
  "service_fee": 0,
  "pay_amount": 0,
  "gap": 0,
  "createTime": "2019-08-24T14:15:22Z",
  "doorAccess": 0,
  "username": "string",
  "phone": "string",
  "note": "string",
  "image": "string",
  "categoryId": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|pickUpAddress|integer(int64)|false|none||取件地址|
|reciveAddress|integer(int64)|false|none||收件地址|
|price|number|false|none|订单基础金额(骑手实际收入)|10|
|service_fee_rate|number|true|none|付费费率快照|0.05，从系统配置表中查询|
|service_fee|number|true|none|服务费|0.5|
|pay_amount|number|true|none|用户支付总额|10.5|
|gap|integer(int32)|false|none||none|
|createTime|string(date-time)|false|none||订单创建时间|
|doorAccess|integer(int32)|false|none||是否有门禁|
|username|string|false|none||发单人昵称|
|phone|string|false|none||发单人手机号|
|note|string|false|none||订单说明文本|
|image|string|false|none||订单说明图片|
|categoryId|integer(int64)|false|none||订单类别id|

<h2 id="tocS_ResultListTakeOrderVO">ResultListTakeOrderVO</h2>

<a id="schemaresultlisttakeordervo"></a>
<a id="schema_ResultListTakeOrderVO"></a>
<a id="tocSresultlisttakeordervo"></a>
<a id="tocsresultlisttakeordervo"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": [
    {
      "id": 0,
      "orderId": 0,
      "pickUpAddress": "string",
      "reciveAddress": "string",
      "orderNumber": "string",
      "price": 0,
      "realPrice": 0,
      "deliveryTime": "2019-08-24T14:15:22Z",
      "cancelTime": "2019-08-24T14:15:22Z",
      "cancelReson": "string",
      "exceedTime": "2019-08-24T14:15:22Z",
      "gap": 0,
      "createTime": "2019-08-24T14:15:22Z",
      "doorAccess": 0,
      "userId": 0,
      "username": "string",
      "phone": "string",
      "realname": "string",
      "status": 0,
      "note": "string",
      "image": "string",
      "categoryId": 0,
      "categoryImage": "string",
      "categoryName": "string",
      "takeOrderCreateTime": "2019-08-24T14:15:22Z",
      "takeOrderImage": "string",
      "takeOrderDeliveryTime": "2019-08-24T14:15:22Z",
      "takeOrderCancelTime": "2019-08-24T14:15:22Z",
      "takeOrderCancelReason": "string"
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|msg|string|false|none||none|
|data|[[TakeOrderVO](#schematakeordervo)]|false|none||none|

<h2 id="tocS_ResultTakeOrderUserInfoVO">ResultTakeOrderUserInfoVO</h2>

<a id="schemaresulttakeorderuserinfovo"></a>
<a id="schema_ResultTakeOrderUserInfoVO"></a>
<a id="tocSresulttakeorderuserinfovo"></a>
<a id="tocsresulttakeorderuserinfovo"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "username": "string",
    "realname": "string",
    "phone": "string",
    "takeOrderTime": "2019-08-24T14:15:22Z",
    "id": 0
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|msg|string|false|none||none|
|data|[TakeOrderUserInfoVO](#schematakeorderuserinfovo)|false|none||none|

<h2 id="tocS_TakeOrderVO">TakeOrderVO</h2>

<a id="schematakeordervo"></a>
<a id="schema_TakeOrderVO"></a>
<a id="tocStakeordervo"></a>
<a id="tocstakeordervo"></a>

```json
{
  "id": 0,
  "orderId": 0,
  "pickUpAddress": "string",
  "reciveAddress": "string",
  "orderNumber": "string",
  "price": 0,
  "realPrice": 0,
  "deliveryTime": "2019-08-24T14:15:22Z",
  "cancelTime": "2019-08-24T14:15:22Z",
  "cancelReson": "string",
  "exceedTime": "2019-08-24T14:15:22Z",
  "gap": 0,
  "createTime": "2019-08-24T14:15:22Z",
  "doorAccess": 0,
  "userId": 0,
  "username": "string",
  "phone": "string",
  "realname": "string",
  "status": 0,
  "note": "string",
  "image": "string",
  "categoryId": 0,
  "categoryImage": "string",
  "categoryName": "string",
  "takeOrderCreateTime": "2019-08-24T14:15:22Z",
  "takeOrderImage": "string",
  "takeOrderDeliveryTime": "2019-08-24T14:15:22Z",
  "takeOrderCancelTime": "2019-08-24T14:15:22Z",
  "takeOrderCancelReason": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||none|
|orderId|integer(int64)|false|none||none|
|pickUpAddress|string|false|none||none|
|reciveAddress|string|false|none||none|
|orderNumber|string|false|none||none|
|price|number|false|none||none|
|realPrice|number|false|none||none|
|deliveryTime|string(date-time)|false|none||none|
|cancelTime|string(date-time)|false|none||none|
|cancelReson|string|false|none||none|
|exceedTime|string(date-time)|false|none||none|
|gap|integer(int32)|false|none||none|
|createTime|string(date-time)|false|none||none|
|doorAccess|integer(int32)|false|none||none|
|userId|integer(int64)|false|none||none|
|username|string|false|none||none|
|phone|string|false|none||none|
|realname|string|false|none||none|
|status|integer(int32)|false|none||none|
|note|string|false|none||none|
|image|string|false|none||none|
|categoryId|integer(int64)|false|none||none|
|categoryImage|string|false|none||none|
|categoryName|string|false|none||none|
|takeOrderCreateTime|string(date-time)|false|none||none|
|takeOrderImage|string|false|none||none|
|takeOrderDeliveryTime|string(date-time)|false|none||none|
|takeOrderCancelTime|string(date-time)|false|none||none|
|takeOrderCancelReason|string|false|none||none|

<h2 id="tocS_TakeOrderUserInfoVO">TakeOrderUserInfoVO</h2>

<a id="schematakeorderuserinfovo"></a>
<a id="schema_TakeOrderUserInfoVO"></a>
<a id="tocStakeorderuserinfovo"></a>
<a id="tocstakeorderuserinfovo"></a>

```json
{
  "username": "string",
  "realname": "string",
  "phone": "string",
  "takeOrderTime": "2019-08-24T14:15:22Z",
  "id": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|username|string|false|none||none|
|realname|string|false|none||none|
|phone|string|false|none||none|
|takeOrderTime|string(date-time)|false|none||none|
|id|integer(int64)|false|none||none|

<h2 id="tocS_ResultUserPaymentVO">ResultUserPaymentVO</h2>

<a id="schemaresultuserpaymentvo"></a>
<a id="schema_ResultUserPaymentVO"></a>
<a id="tocSresultuserpaymentvo"></a>
<a id="tocsresultuserpaymentvo"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "aliPaymentCode": "string",
    "weChatPaymentCode": "string"
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|msg|string|false|none||none|
|data|[UserPaymentVO](#schemauserpaymentvo)|false|none||none|

<h2 id="tocS_UserPaymentVO">UserPaymentVO</h2>

<a id="schemauserpaymentvo"></a>
<a id="schema_UserPaymentVO"></a>
<a id="tocSuserpaymentvo"></a>
<a id="tocsuserpaymentvo"></a>

```json
{
  "aliPaymentCode": "string",
  "weChatPaymentCode": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|aliPaymentCode|string|false|none||none|
|weChatPaymentCode|string|false|none||none|

<h2 id="tocS_OrderShowVO">OrderShowVO</h2>

<a id="schemaordershowvo"></a>
<a id="schema_OrderShowVO"></a>
<a id="tocSordershowvo"></a>
<a id="tocsordershowvo"></a>

```json
{
  "id": 0,
  "pickUpAddress": "string",
  "reciveAddress": "string",
  "orderNumber": "string",
  "price": 0,
  "realPrice": 0,
  "deliveryTime": "2019-08-24T14:15:22Z",
  "cancelTime": "2019-08-24T14:15:22Z",
  "cancelReson": "string",
  "exceedTime": "2019-08-24T14:15:22Z",
  "gap": 0,
  "createTime": "2019-08-24T14:15:22Z",
  "doorAccess": 0,
  "userId": 0,
  "username": "string",
  "phone": "string",
  "status": 0,
  "note": "string",
  "image": "string",
  "deleted": 0,
  "categoryImage": "string",
  "categoryName": "string",
  "realname": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||none|
|pickUpAddress|string|false|none||none|
|reciveAddress|string|false|none||none|
|orderNumber|string|false|none||none|
|price|number|false|none||none|
|realPrice|number|false|none||none|
|deliveryTime|string(date-time)|false|none||none|
|cancelTime|string(date-time)|false|none||none|
|cancelReson|string|false|none||none|
|exceedTime|string(date-time)|false|none||none|
|gap|integer(int32)|false|none||none|
|createTime|string(date-time)|false|none||none|
|doorAccess|integer(int32)|false|none||none|
|userId|integer(int64)|false|none||none|
|username|string|false|none||none|
|phone|string|false|none||none|
|status|integer(int32)|false|none||none|
|note|string|false|none||none|
|image|string|false|none||none|
|deleted|integer(int32)|false|none||none|
|categoryImage|string|false|none||none|
|categoryName|string|false|none||none|
|realname|string|false|none||none|

<h2 id="tocS_ResultListOrderShowVO">ResultListOrderShowVO</h2>

<a id="schemaresultlistordershowvo"></a>
<a id="schema_ResultListOrderShowVO"></a>
<a id="tocSresultlistordershowvo"></a>
<a id="tocsresultlistordershowvo"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": [
    {
      "id": 0,
      "pickUpAddress": "string",
      "reciveAddress": "string",
      "orderNumber": "string",
      "price": 0,
      "realPrice": 0,
      "deliveryTime": "2019-08-24T14:15:22Z",
      "cancelTime": "2019-08-24T14:15:22Z",
      "cancelReson": "string",
      "exceedTime": "2019-08-24T14:15:22Z",
      "gap": 0,
      "createTime": "2019-08-24T14:15:22Z",
      "doorAccess": 0,
      "userId": 0,
      "username": "string",
      "phone": "string",
      "status": 0,
      "note": "string",
      "image": "string",
      "deleted": 0,
      "categoryImage": "string",
      "categoryName": "string",
      "realname": "string"
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|msg|string|false|none||none|
|data|[[OrderShowVO](#schemaordershowvo)]|false|none||none|

<h2 id="tocS_ResultOrderShowVO">ResultOrderShowVO</h2>

<a id="schemaresultordershowvo"></a>
<a id="schema_ResultOrderShowVO"></a>
<a id="tocSresultordershowvo"></a>
<a id="tocsresultordershowvo"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "id": 0,
    "pickUpAddress": "string",
    "reciveAddress": "string",
    "orderNumber": "string",
    "price": 0,
    "realPrice": 0,
    "deliveryTime": "2019-08-24T14:15:22Z",
    "cancelTime": "2019-08-24T14:15:22Z",
    "cancelReson": "string",
    "exceedTime": "2019-08-24T14:15:22Z",
    "gap": 0,
    "createTime": "2019-08-24T14:15:22Z",
    "doorAccess": 0,
    "userId": 0,
    "username": "string",
    "phone": "string",
    "status": 0,
    "note": "string",
    "image": "string",
    "deleted": 0,
    "categoryImage": "string",
    "categoryName": "string",
    "realname": "string"
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|msg|string|false|none||none|
|data|[OrderShowVO](#schemaordershowvo)|false|none||none|

<h2 id="tocS_Order">Order</h2>

<a id="schemaorder"></a>
<a id="schema_Order"></a>
<a id="tocSorder"></a>
<a id="tocsorder"></a>

```json
{
  "id": 0,
  "pickUpAddress": 0,
  "reciveAddress": 0,
  "orderNumber": "string",
  "price": 0,
  "service_fee_rate": 0,
  "service_fee": 0,
  "pay_amount": 0,
  "realPrice": 0,
  "deliveryTime": "2019-08-24T14:15:22Z",
  "cancelTime": "2019-08-24T14:15:22Z",
  "cancelReson": "string",
  "exceedTime": "2019-08-24T14:15:22Z",
  "gap": 0,
  "createTime": "2019-08-24T14:15:22Z",
  "doorAccess": 0,
  "userId": 0,
  "username": "string",
  "phone": "string",
  "status": 0,
  "note": "string",
  "image": "string",
  "categoryId": 0,
  "deleted": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||订单id，唯一索引|
|pickUpAddress|integer(int64)|false|none||取件地址|
|reciveAddress|integer(int64)|false|none||收件地址|
|orderNumber|string|false|none||订单编号|
|price|number|false|none|订单基础金额(骑手实际收入)|10|
|service_fee_rate|number|true|none|付费费率快照|0.05，从系统配置表中查询|
|service_fee|number|true|none|服务费|0.5|
|pay_amount|number|true|none|用户支付总额|10.5|
|realPrice|number|false|none||暂时禁用，实际价格|
|deliveryTime|string(date-time)|false|none||订单发布时间|
|cancelTime|string(date-time)|false|none||订单取消时间|
|cancelReson|string|false|none||订单取消原因|
|exceedTime|string(date-time)|false|none||none|
|gap|integer(int32)|false|none||none|
|createTime|string(date-time)|false|none||订单创建时间|
|doorAccess|integer(int32)|false|none||是否有门禁|
|userId|integer(int64)|false|none||发单人id|
|username|string|false|none||发单人昵称|
|phone|string|false|none||发单人手机号|
|status|integer(int32)|false|none||订单状态，-4退款异常 -3退款成功 -2退款中 -1未支付 0待接单 1已接单 2派送中 3已送达 4已取消 5已完成 6提现成功 7提现失败<br />对于正常完成的订单，无偿订单的状态流程是：01235；有偿：012356|
|note|string|false|none||订单说明文本|
|image|string|false|none||订单说明图片|
|categoryId|integer(int64)|false|none||订单类别id|
|deleted|integer(int32)|false|none||假删除|

<h2 id="tocS_ResultOrder">ResultOrder</h2>

<a id="schemaresultorder"></a>
<a id="schema_ResultOrder"></a>
<a id="tocSresultorder"></a>
<a id="tocsresultorder"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "id": 0,
    "pickUpAddress": 0,
    "reciveAddress": 0,
    "orderNumber": "string",
    "price": 0,
    "service_fee_rate": 0,
    "service_fee": 0,
    "pay_amount": 0,
    "realPrice": 0,
    "deliveryTime": "2019-08-24T14:15:22Z",
    "cancelTime": "2019-08-24T14:15:22Z",
    "cancelReson": "string",
    "exceedTime": "2019-08-24T14:15:22Z",
    "gap": 0,
    "createTime": "2019-08-24T14:15:22Z",
    "doorAccess": 0,
    "userId": 0,
    "username": "string",
    "phone": "string",
    "status": 0,
    "note": "string",
    "image": "string",
    "categoryId": 0,
    "deleted": 0
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|msg|string|false|none||none|
|data|[Order](#schemaorder)|false|none||none|

<h2 id="tocS_ResultInteger">ResultInteger</h2>

<a id="schemaresultinteger"></a>
<a id="schema_ResultInteger"></a>
<a id="tocSresultinteger"></a>
<a id="tocsresultinteger"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|msg|string|false|none||none|
|data|integer(int32)|false|none||none|

<h2 id="tocS_ResultString">ResultString</h2>

<a id="schemaresultstring"></a>
<a id="schema_ResultString"></a>
<a id="tocSresultstring"></a>
<a id="tocsresultstring"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|msg|string|false|none||none|
|data|string|false|none||none|

<h2 id="tocS_Result">Result</h2>

<a id="schemaresult"></a>
<a id="schema_Result"></a>
<a id="tocSresult"></a>
<a id="tocsresult"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||响应码 0失败 1成功|
|msg|string|false|none||响应消息，失败时会输出原因|
|data|object|false|none||返回的数据|

