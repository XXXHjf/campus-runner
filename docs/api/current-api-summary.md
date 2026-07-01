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

# 用户相关接口

<a id="opIdgetCurrentUser"></a>

## GET 查询当前用户

GET /api/user

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{"id":0,"username":"string","realname":"string","openid":"string","headImg":"string","sex":0,"phone":"string","authentication":0,"schoolId":0,"schoolName":"string","stuId":"string","studentIdCard":"string","studentIdCardReview":0,"score":0,"money":0,"alipayPaymentCode":"string","weChatPaymentCode":"string","isManager":0,"deleted":0,"createTime":"2019-08-24T14:15:22Z","updateTime":"2019-08-24T14:15:22Z"}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultUserVO](#schemaresultuservo)|

<a id="opIdauthentication"></a>

## PUT 用户认证

PUT /api/user

用户选择学校，输入学号、真实姓名，选择佐证照片后发起认证请求。

> Body 请求参数

```json
{
  "schoolId": 0,
  "realname": "string",
  "stuId": "string",
  "studentIdCard": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[UserAuthenDTO](#schemauserauthendto)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[Result](#schemaresult)|

<a id="opIdupdate"></a>

## PUT 用户信息更新

PUT /api/user/update

> Body 请求参数

```json
{
  "phone": "string",
  "username": "string",
  "headImg": "string",
  "sex": 0
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[UserSaveDTO](#schemausersavedto)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[Result](#schemaresult)|

<a id="opIdupdatePaymentCode"></a>

## PUT 更新收款码

PUT /api/user/updatePaymentCode

> Body 请求参数

```json
{
  "aliPaymentCode": "string",
  "weChatPaymentCode": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[UserPaymentDTO](#schemauserpaymentdto)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[Result](#schemaresult)|

<a id="opIdlogin"></a>

## POST 用户登录

POST /api/user/login

> Body 请求参数

```json
{
  "code": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[UserLoginDTO](#schemauserlogindto)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{"id":0,"username":"string","realname":"string","openid":"string","headImg":"string","sex":0,"phone":"string","authentication":0,"schoolId":0,"stuId":"string","studentIdCard":"string","studentIdCardReview":0,"score":0,"money":0,"alipayPaymentCode":"string","weChatPaymentCode":"string","isManager":0,"deleted":0,"createTime":"2019-08-24T14:15:22Z","updateTime":"2019-08-24T14:15:22Z","token":"string"}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultUserLoginVO](#schemaresultuserloginvo)|

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
  "pickUpAddress": 136,
  "reciveAddress": 137,
  "price": 10,
  "serviceFeeRate": 0.05,
  "serviceFee": 0.5,
  "payAmount": 10.5,
  "gap": 500,
  "createTime": "2025-09-22 00:51:56",
  "doorAccess": 65,
  "username": "胡椒粉",
  "phone": "13869823548",
  "note": "fugiat in ea",
  "image": "null",
  "categoryId": 9
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[OrderSubmitDTO](#schemaordersubmitdto)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{"id":0,"pickUpAddress":136,"reciveAddress":137,"orderNumber":"string","price":10,"serviceFeeRate":0.05,"serviceFee":0.5,"payAmount":10.5,"deliveryTime":"2019-08-24T14:15:22Z","cancelTime":"2019-08-24T14:15:22Z","cancelReson":"string","exceedTime":"2019-08-24T14:15:22Z","gap":500,"createTime":"2019-08-24T14:15:22Z","doorAccess":0,"userId":0,"username":"胡椒粉","phone":"13869823548","status":0,"note":"string","image":"null","categoryId":0,"deleted":0,"realPrice":0}}
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

# 地址簿相关接口

<a id="opIdquery_1"></a>

## GET 条件查询

GET /api/address

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|isDefault|query|integer(int32)| 否 |none|
|type|query|integer(int32)| 否 |none|
|id|query|integer(int64)| 否 |none|
|label|query|string| 否 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":[{"id":0,"schoolName":"string","compusName":"string","buildCategoryName":"string","buildingName":"string","details":"string","label":"string","userId":0,"isDefault":0,"type":0,"deleted":0}]}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultListAddressBookShowVO](#schemaresultlistaddressbookshowvo)|

<a id="opIdsetDefault"></a>

## PUT 设置默认地址

PUT /api/address

> Body 请求参数

```json
{
  "id": 0,
  "type": 0
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[AddressBookDefaultDTO](#schemaaddressbookdefaultdto)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[Result](#schemaresult)|

<a id="opIdsave"></a>

## POST 新增地址

POST /api/address

> Body 请求参数

```json
{
  "schoolNumberId": 0,
  "compusNumberId": 0,
  "buildCategoryNumberId": 0,
  "buildingNumberId": 0,
  "details": "string",
  "label": "string",
  "type": 0
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[AddressBookDTO](#schemaaddressbookdto)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[Result](#schemaresult)|

<a id="opIdupdate_1"></a>

## PUT 修改我的地址

PUT /api/address/update

> Body 请求参数

```json
{
  "id": 0,
  "schoolNumberId": 0,
  "compusNumberId": 0,
  "buildCategoryNumberId": 0,
  "buildingNumberId": 0,
  "details": "string",
  "label": "string",
  "type": 0
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[AddressBookUpdateDTO](#schemaaddressbookupdatedto)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[Result](#schemaresult)|

<a id="opIdthree"></a>

## GET 地址筛选

GET /api/address/three

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{"school":[{"id":0,"schoolName":"string","numberId":0,"deleted":0}],"compus":[{"numberId":0,"compusName":"string"}],"buildCategory":[{"numberId":0,"buildCategoryName":"string"}],"building":[{"numberId":0,"buildingName":"string"}]}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultAddressBookThreeVO](#schemaresultaddressbookthreevo)|

<a id="opIdshow"></a>

## GET 我的地址展示

GET /api/address/show

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":[{"id":0,"schoolName":"string","compusName":"string","buildCategoryName":"string","buildingName":"string","details":"string","label":"string","userId":0,"isDefault":0,"type":0,"deleted":0}]}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultListAddressBookShowVO](#schemaresultlistaddressbookshowvo)|

<a id="opIddeleteById"></a>

## DELETE 删除地址

DELETE /api/address/{id}

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

# 微信商家转账相关接口

<a id="opIdwxTransfer"></a>

## POST 商家发起转账

POST /api/wx-transfer/transfer/{orderId}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|orderId|path|integer(int64)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{"outBillNo":"string","transferBillNo":"string","createTime":"string","state":"string","failReason":"string","packageInfo":"string"}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultWeChatTransferVO](#schemaresultwechattransfervo)|

<a id="opIdnotifyTransfer"></a>

## POST 商家转账回调通知

POST /api/wx-transfer/notify

> 返回示例

> 200 Response

```
"string"
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|string|

<a id="opIdwxCloseTransfer"></a>

## POST 撤销转账(测试用)

POST /api/wx-transfer/close/{orderNumber}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|orderNumber|path|string| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[Result](#schemaresult)|

<a id="opIdwxQueryOrder"></a>

## GET 根据商户单号查询账单(测试用)

GET /api/wx-transfer/{orderNumber}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|orderNumber|path|string| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":"string"}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultString](#schemaresultstring)|

# 微信支付相关接口

<a id="opIdrefunds"></a>

## POST 微信支付退款

POST /api/wx-pay/refunds

> Body 请求参数

```json
{
  "orderNumber": "string",
  "reason": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[RefundInfoDTO](#schemarefundinfodto)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[Result](#schemaresult)|

<a id="opIdrefundsNotify"></a>

## POST 退款结果回调通知

POST /api/wx-pay/refunds/notify

> 返回示例

> 200 Response

```
"string"
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|string|

<a id="opIdjsapiPay"></a>

## POST 用户发单，支付

POST /api/wx-pay/jspai/{orderId}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|orderId|path|integer(int64)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{"prepayId":"string","timeStamp":"string","nonceStr":"string","signType":"string","paySign":"string"}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultWeChatPrePayVO](#schemaresultwechatprepayvo)|

<a id="opIdjsapiNotifyPay"></a>

## POST 微信支付成功回调通知

POST /api/wx-pay/jsapi/notify

> 返回示例

> 200 Response

```
"string"
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|string|

<a id="opIdqueryRefunds"></a>

## GET 查询退款单状态(测试用)

GET /api/wx-pay/query-refunds/{refundNumber}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|refundNumber|path|string| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":"string"}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultString](#schemaresultstring)|

<a id="opIdweChatQueryOrder"></a>

## GET 商户订单号查询订单(测试用)

GET /api/wx-pay/jsapi/{orderNumber}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|orderNumber|path|string| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":"string"}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultString](#schemaresultstring)|

# 其他接口

<a id="opIdupload"></a>

## POST 文件上传

POST /api/upload

文件上传；img为body参数，传入正常的图片数据；dirName为Params参数，即/api/upload?dirName=other/swiper 为图片保存的目录名
此接口可以用 用户的token 或 管理员的token 作为请求头header中的token参数进行访问, 遇到无token或token不合法时会显示"NO_TOKEN"和"ILLEGAL_TOKEN"具体看返回示例
具体业务：
1. 上传轮播图时，格式为banner/<path>。具体学校的广告<path>统一命名为学校名的拼音大驼峰，例如ZheJiangDaXue；公用轮播图<path>为common

> Body 请求参数

```yaml
img: ""

```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|dirName|query|string| 否 |目录名，传到阿里OSS里的目录名称|
|body|body|object| 否 |none|
|» img|body|string(binary)| 否 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":"string"}
```

```json
{
  "code": 0,
  "msg": "NO_TOKEN 当前请求无token..."
}
```

```json
{
  "code": 0,
  "msg": "ILLEGAL_TOKEN 当前token非法, 禁止使用此请求"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultString](#schemaresultstring)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|none|[ResultString](#schemaresultstring)|

# 学校相关接口

<a id="opIdgetAll"></a>

## GET 查询所有学校

GET /api/school

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":[{"id":0,"schoolName":"string","numberId":0,"deleted":0}]}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultListSchool](#schemaresultlistschool)|

<a id="opIdaddReserve"></a>

## POST 添加预设地址信息

POST /api/school

管理员新增地址信息

> Body 请求参数

```json
{
  "schoolName": "广州大学",
  "compusName": "大学城校区",
  "buildCategoryName": "学生公寓",
  "buildingName": "梅苑1栋"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[SchoolReserveDTO](#schemaschoolreservedto)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[Result](#schemaresult)|

<a id="opIdgetNameById"></a>

## GET 根据id查询学校名称

GET /api/school/{id}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer(int64)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":"string"}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultString](#schemaresultstring)|

# 小程序消息发送相关接口

<a id="opIdsendPickUp"></a>

## POST 发送接单人已取货消息

POST /api/message/pickUp

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|orderId|query|integer(int64)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":"string"}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultString](#schemaresultstring)|

<a id="opIdsendDelivered"></a>

## POST 发送订单已送达消息

POST /api/message/delivered

> Body 请求参数

```json
{
  "orderId": 0,
  "takeOrderUserId": 0
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MessageDeliveredDTO](#schemamessagedelivereddto)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":"string"}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultString](#schemaresultstring)|

<a id="opIdsendTakeOrder"></a>

## POST 发送订单已接单通知

POST /api/message/alreadyTakeOrder

> Body 请求参数

```json
{
  "orderId": 0,
  "takeOrderUserId": 0
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MessageTakeOrderDTO](#schemamessagetakeorderdto)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":"string"}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultString](#schemaresultstring)|

# 商品类别相关接口

<a id="opIdgetAll_1"></a>

## GET 获取所有订单类型

GET /api/category

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":[{"id":0,"categoryName":"string","image":"string","deleted":0}]}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultListCategory](#schemaresultlistcategory)|

<a id="opIdgetById"></a>

## GET 根据id查询类别

GET /api/category/{id}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer(int64)| 是 |none|

> 返回示例

> 200 Response

```
{"code":0,"msg":"string","data":{"id":0,"categoryName":"string","image":"string","deleted":0}}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|OK|[ResultCategory](#schemaresultcategory)|

# 管理员相关/登录相关

## POST 管理员登录

POST /admin/api/login

管理员通过账号密码登录网页端中后台
token时间为7天

> Body 请求参数

```json
{
  "username": "admin",
  "password": "password"
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 是 ||none|
|» username|body|string| 是 | 用户名|none|
|» password|body|string| 是 | 密码|none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "id": 0,
    "username": "string",
    "passwordHash": "string",
    "nickname": "string",
    "avatar": "string",
    "status": 0,
    "school": 0,
    "lastLoginIp": "string",
    "lastLoginTime": "string",
    "createTime": "string",
    "updateTime": "string",
    "deleted": 0,
    "adminToken": "string"
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ResultAdminLoginVO](#schemaresultadminloginvo)|

## POST 管理员用户注册

POST /admin/api/register

管理员用户注册，现在都默认为超级管理员

> Body 请求参数

```json
{
  "username": "string",
  "password": "string",
  "nickname": "string",
  "avatar": "string",
  "status": 0,
  "school": 0
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|[AdminRegisterDTO](#schemaadminregisterdto)| 是 ||none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[Result](#schemaresult)|

# 管理员相关/地址管理

## DELETE 删除地址

DELETE /admin/api/address/delete/{buildingID}

根据楼宇id删除地址信息，逻辑删除

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|buildingID|path|integer| 是 ||楼宇id|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[Result](#schemaresult)|

## PUT 修改地址

PUT /admin/api/address/update

修改地址信息，修改楼宇名字。
会验证楼宇名字是否重复, 若修改后楼宇名字重复, 则会返回提示, 在返回结果中code = 0, msg = "REPEAT"

> Body 请求参数

```json
{
  "buildingID": 0,
  "buildingName": "string"
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 是 ||none|
|» buildingID|body|integer| 是 | 楼宇ID|none|
|» buildingName|body|string| 是 | 楼宇名字|none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "REPEAT",
  "data": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|false|none||响应码 0失败 1成功|
|» msg|string|false|none||当code=0, msg显示REPEAT时, 代表输入了重复的地址|
|» data|object|false|none||返回的数据|

## GET 根据学校获取详细地址

GET /admin/api/address/getListBySchool/{schoolID}

根据学校查询该学校下的所有地址

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|schoolID|path|integer| 是 ||学校id (这是路径参数 即/12)|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": [
    {
      "id": 0,
      "schoolId": 0,
      "schoolName": "string",
      "compusId": 0,
      "compusName": "string",
      "buildCategoryId": 0,
      "buildCategoryName": "string",
      "numberId": 0,
      "buildingName": "string"
    }
  ]
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|false|none||none|
|» msg|string|false|none||none|
|» data|[[AdminAdressBuildingVO](#schemaadminadressbuildingvo)]|false|none||none|
|»» id|integer|false|none||楼宇id|
|»» schoolId|integer|false|none||学校id|
|»» schoolName|string|false|none||学校名字|
|»» compusId|integer|false|none||校区id|
|»» compusName|string|false|none||校区名字|
|»» buildCategoryId|integer|false|none||楼宇分类id|
|»» buildCategoryName|string|false|none||楼宇分类名字|
|»» numberId|integer|false|none||numberid|
|»» buildingName|string|false|none||楼宇名字|

## POST 管理员端添加预设地址信息

POST /admin/api/address

添加预设地址
当遇到添加的地址重复时，返回的code==0，会在msg里显示 REPEAT

> Body 请求参数

```json
{
  "schoolName": "string",
  "compusName": "string",
  "buildCategoryName": "string",
  "buildingName": "string"
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|[SchoolReserveDTO](#schemaschoolreservedto)| 是 ||none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "REPEAT",
  "data": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|false|none||响应码 0失败 1成功|
|» msg|string|false|none||当code=0, msg显示REPEAT时, 代表输入了重复的地址|
|» data|object|false|none||返回的数据|

## GET 管理员端查询所有学校

GET /admin/api/address/schools

查询所有学校

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": [
    {
      "id": 0,
      "schoolName": "string",
      "numberId": 0,
      "deleted": 0
    }
  ]
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ResultListSchool](#schemaresultlistschool)|

# 管理员相关/审核

## PUT 审核学生认证

PUT /admin/api/auth/review

审核学生认证
审核结果为 审核通过 时即学生认证成功
审核结果为 审核未通过 时学生可以继续提交学生认证审核申请

> Body 请求参数

```json
{
  "userID": 0,
  "review": 0
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 是 ||none|
|» userID|body|integer| 是 | 用户ID|none|
|» review|body|integer| 是 | 审核信息|0未审核 1审核中 2审核通过 3审核不通过|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[Result](#schemaresult)|

## GET 获取待审核列表

GET /admin/api/auth/pendingList

获取所有发起了学生认证审核的学生列表

> Body 请求参数

```json
{}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 是 ||none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": [
    {
      "id": 0,
      "username": "string",
      "realname": "string",
      "openid": "string",
      "headImg": "string",
      "sex": 0,
      "phone": "string",
      "authentication": 0,
      "schoolId": 0,
      "schoolName": "string",
      "stuId": "string",
      "studentIdCard": "string",
      "studentIdCardReview": 0,
      "score": 0,
      "money": 0,
      "alipayPaymentCode": "string",
      "weChatPaymentCode": "string",
      "isManager": 0,
      "deleted": 0,
      "createTime": "2019-08-24T14:15:22Z",
      "updateTime": "2019-08-24T14:15:22Z"
    }
  ]
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|false|none||响应码 0失败 1成功|
|» msg|string|false|none||响应消息，失败时会输出原因|
|» data|[[UserVO](#schemauservo)]|false|none||none|
|»» id|integer(int64)|false|none||id|
|»» username|string|false|none||昵称|
|»» realname|string|false|none||学生认证的真实姓名|
|»» openid|string|false|none||微信的openId|
|»» headImg|string|false|none||头像链接|
|»» sex|integer(int32)|false|none||暂时禁用，性别|
|»» phone|string|false|none||手机号|
|»» authentication|integer(int32)|false|none||0未认证 1认证通过，可以进行发单接单操作|
|»» schoolId|integer(int64)|false|none||学生认证的学校Id|
|»» schoolName|string|false|none||学生认证的学校名|
|»» stuId|string|false|none||学生认证的学号|
|»» studentIdCard|string|false|none||学生认证图片的链接|
|»» studentIdCardReview|integer(int32)|false|none||学生认证的通过情况：0未审核 1审核中 2审核通过 3审核不通过|
|»» score|integer(int32)|false|none||暂时禁用，信誉分|
|»» money|number|false|none||暂时禁用，余额|
|»» alipayPaymentCode|string|false|none||暂时禁用，支付宝付款码链接|
|»» weChatPaymentCode|string|false|none||暂时禁用，微信付款码链接|
|»» isManager|integer(int32)|false|none||暂时禁用，是否为管理员|
|»» deleted|integer(int32)|false|none||软删除|
|»» createTime|string(date-time)|false|none||账号创建时间|
|»» updateTime|string(date-time)|false|none||账号最后更新时间|

# 管理员相关/轮播图

## GET 获取对应学校的轮播图列表

GET /admin/api/banner/getList/{schoolId}

获取对应学校的轮播图片 
注：此接口不需要传递token，直接即可请求。考虑到这是管理员端的请求，而且需要在小程序页面上显示，且本身就属于广告性质，就不需要传递token了
当scoolId == 0 时 返回所有的通用轮播图图片
当schoolId != 0 时 返回所有指定学校id的轮播图图片

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|schoolId|path|integer| 是 ||学校id，为0则代表所有学校通用的轮播图|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "msg": null,
  "data": [
    {
      "id": 1,
      "imgUrl": "这是新增的轮播图地址1图片路径",
      "title": "新增轮播图1",
      "schoolId": 0,
      "schoolName": null,
      "jumpType": 0,
      "jumpTarget": "",
      "remark": "这是新增的轮播图地址1图片路径 通用，无跳转",
      "createBy": 3,
      "createTime": "2025-12-19 10:52:06",
      "deleted": 0
    },
    {
      "id": 2,
      "imgUrl": "这是新增的轮播图地址2图片路径",
      "title": "新增轮播图2",
      "schoolId": 0,
      "schoolName": null,
      "jumpType": 1,
      "jumpTarget": "https://www.baidu.com",
      "remark": "这是新增的轮播图地址1图片路径 通用，有网页跳转",
      "createBy": 3,
      "createTime": "2025-12-19 10:52:48",
      "deleted": 0
    },
    {
      "id": 3,
      "imgUrl": "这是新增的轮播图地址3图片路径",
      "title": "新增轮播图3",
      "schoolId": 0,
      "schoolName": null,
      "jumpType": 2,
      "jumpTarget": "/user/Login",
      "remark": "这是新增的轮播图地址3图片 通用，有站内跳转",
      "createBy": 3,
      "createTime": "2025-12-19 10:53:41",
      "deleted": 0
    },
    {
      "id": 4,
      "imgUrl": "这是新增的轮播图地址4图片路径",
      "title": "新增轮播图4",
      "schoolId": 0,
      "schoolName": null,
      "jumpType": 3,
      "jumpTarget": "/page/Admin",
      "remark": "这是新增的轮播图地址4图片 通用，有小程序跳转",
      "createBy": 3,
      "createTime": "2025-12-19 10:54:08",
      "deleted": 0
    }
  ]
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ResultListBannerVO](#schemaresultlistbannervo)|

## DELETE 删除轮播图

DELETE /admin/api/banner/delete/{id}

根据轮播图id删除轮播图数据

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|id|path|integer| 是 ||轮播图id|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[Result](#schemaresult)|

## POST 新增轮播图

POST /admin/api/banner/add

> Body 请求参数

```json
{
  "imgUrl": "string",
  "title": "string",
  "schoolId": 0,
  "jumpType": 0,
  "jumpTarget": "string",
  "remark": "string"
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|[AdminBannerAddDTO](#schemaadminbanneradddto)| 是 ||none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[Result](#schemaresult)|

## PUT 修改轮播图信息

PUT /admin/api/banner/update

> 返回示例

> 200 Response

```json
{}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

# 管理员相关/KPI展示

## GET 查询所有订单总数

GET /admin/api/kpi/orders

返回系统内订单总数量（累计），不区分时间范围

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "msg": null,
  "data": 14
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|false|none||响应码 0失败 1成功|
|» msg|string|false|none||响应消息，失败时会输出原因|
|» data|integer(int64)|false|none||返回总的订单数量|

## GET 查询待接单数

GET /admin/api/kpi/pending

返回当前处于“待接单”状态的订单数量

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "msg": null,
  "data": 85
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|false|none||响应码 0失败 1成功|
|» msg|string|false|none||响应消息，失败时会输出原因|
|» data|integer(int64)|false|none||查询待接单状态的订单数量|

## GET 查询已完成订单数

GET /admin/api/kpi/completed

返回系统内处于“已完成”状态的订单数量（累计）

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "msg": null,
  "data": 68
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|false|none||响应码 0失败 1成功|
|» msg|string|false|none||响应消息，失败时会输出原因|
|» data|integer(int64)|false|none||返回所有已完成状态的订单数量|

## GET 查询用户总数

GET /admin/api/kpi/users

返回用户总数量（累计）

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "msg": null,
  "data": 99
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|false|none||响应码 0失败 1成功|
|» msg|string|false|none||响应消息，失败时会输出原因|
|» data|integer(int64)|false|none||总用户数量|

## GET 查询接单总数

GET /admin/api/kpi/accepted

返回累计“已被接单”的订单数量，只要曾被接单就算，不管后续的订单状态

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "msg": null,
  "data": 46
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|false|none||响应码 0失败 1成功|
|» msg|string|false|none||响应消息，失败时会输出原因|
|» data|integer(int64)|false|none||累计已被接单的订单总数|

## GET 查询今日订单数

GET /admin/api/kpi/today

返回“今日新增订单数”。“今日”的定义建议固定为 服务器所在时区的自然日 00:00:00 ~ 23:59:59，以避免前后端时区不一致。

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "msg": null,
  "data": 39
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|false|none||响应码 0失败 1成功|
|» msg|string|false|none||响应消息，失败时会输出原因|
|» data|integer(int64)|false|none||今日新增订单数量|

# 管理员相关/配置信息

## GET 获取服务费率

GET /admin/api/config/service_fee_rate

获取服务费率，返回在result的data里，以字符串形式返回
没有任何传参

> Body 请求参数

```json
{}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 是 ||none|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "msg": null,
  "data": "0.05"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|false|none||none|
|» msg|string|false|none||none|
|» data|string|false|none||none|

## PUT 修改服务费率

PUT /admin/api/config/service_fee_rate

修改服务费率，需要在请求体body中传递参数，json格式，
如示例所示，只需设置serviceFeeRate的值为想要修改成的值即可

> Body 请求参数

```json
{
  "serviceFeeRate": 0.5
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 是 ||none|
|» serviceFeeRate|body|number| 否 ||服务费率|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "msg": null,
  "data": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|false|none||响应码 0失败 1成功|
|» msg|string|false|none||响应消息，失败时会输出原因|
|» data|object|false|none||返回的数据|

## GET 获取最低服务费

GET /admin/api/config/service_fee_min

获取最低服务费，返回在result的data里，以字符串形式返回
没有任何传参

> Body 请求参数

```json
{}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 是 ||none|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "msg": null,
  "data": "0.5"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|false|none||none|
|» msg|string|false|none||none|
|» data|string|false|none||none|

## PUT 修改最低服务费

PUT /admin/api/config/service_fee_min

修改最低服务费，需要在请求体body中传递参数，json格式，
如示例所示，只需设置serviceFeeMin的值为想要修改成的值即可

> Body 请求参数

```json
{
  "serviceFeeMin": 0.1
}
```

### 请求参数

|名称|位置|类型|必选|中文名|说明|
|---|---|---|---|---|---|
|body|body|object| 是 ||none|
|» serviceFeeMin|body|number| 否 ||最低服务费用|

> 返回示例

> 200 Response

```json
{
  "code": 1,
  "msg": null,
  "data": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|false|none||响应码 0失败 1成功|
|» msg|string|false|none||响应消息，失败时会输出原因|
|» data|object|false|none||返回的数据|

# 数据模型

<h2 id="tocS_UserAuthenDTO">UserAuthenDTO</h2>

<a id="schemauserauthendto"></a>
<a id="schema_UserAuthenDTO"></a>
<a id="tocSuserauthendto"></a>
<a id="tocsuserauthendto"></a>

```json
{
  "schoolId": 0,
  "realname": "string",
  "stuId": "string",
  "studentIdCard": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|schoolId|integer(int64)|false|none||学生认证的学校Id|
|realname|string|false|none||学生认证的真实姓名|
|stuId|string|false|none||学生认证的学号|
|studentIdCard|string|false|none||学生认证图片的链接|

<h2 id="tocS_UserSaveDTO">UserSaveDTO</h2>

<a id="schemausersavedto"></a>
<a id="schema_UserSaveDTO"></a>
<a id="tocSusersavedto"></a>
<a id="tocsusersavedto"></a>

```json
{
  "phone": "string",
  "username": "string",
  "headImg": "string",
  "sex": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|phone|string|false|none||none|
|username|string|false|none||none|
|headImg|string|false|none||none|
|sex|integer(int32)|false|none||none|

<h2 id="tocS_UserPaymentDTO">UserPaymentDTO</h2>

<a id="schemauserpaymentdto"></a>
<a id="schema_UserPaymentDTO"></a>
<a id="tocSuserpaymentdto"></a>
<a id="tocsuserpaymentdto"></a>

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

<h2 id="tocS_AddressBookDefaultDTO">AddressBookDefaultDTO</h2>

<a id="schemaaddressbookdefaultdto"></a>
<a id="schema_AddressBookDefaultDTO"></a>
<a id="tocSaddressbookdefaultdto"></a>
<a id="tocsaddressbookdefaultdto"></a>

```json
{
  "id": 0,
  "type": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||none|
|type|integer(int32)|false|none||none|

<h2 id="tocS_AddressBookUpdateDTO">AddressBookUpdateDTO</h2>

<a id="schemaaddressbookupdatedto"></a>
<a id="schema_AddressBookUpdateDTO"></a>
<a id="tocSaddressbookupdatedto"></a>
<a id="tocsaddressbookupdatedto"></a>

```json
{
  "id": 0,
  "schoolNumberId": 0,
  "compusNumberId": 0,
  "buildCategoryNumberId": 0,
  "buildingNumberId": 0,
  "details": "string",
  "label": "string",
  "type": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||none|
|schoolNumberId|integer(int64)|false|none||none|
|compusNumberId|integer(int64)|false|none||none|
|buildCategoryNumberId|integer(int64)|false|none||none|
|buildingNumberId|integer(int64)|false|none||none|
|details|string|false|none||none|
|label|string|false|none||none|
|type|integer(int32)|false|none||none|

<h2 id="tocS_RefundInfoDTO">RefundInfoDTO</h2>

<a id="schemarefundinfodto"></a>
<a id="schema_RefundInfoDTO"></a>
<a id="tocSrefundinfodto"></a>
<a id="tocsrefundinfodto"></a>

```json
{
  "orderNumber": "string",
  "reason": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|orderNumber|string|false|none||none|
|reason|string|false|none||none|

<h2 id="tocS_UserLoginDTO">UserLoginDTO</h2>

<a id="schemauserlogindto"></a>
<a id="schema_UserLoginDTO"></a>
<a id="tocSuserlogindto"></a>
<a id="tocsuserlogindto"></a>

```json
{
  "code": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|string|false|none||none|

<h2 id="tocS_SchoolReserveDTO">SchoolReserveDTO</h2>

<a id="schemaschoolreservedto"></a>
<a id="schema_SchoolReserveDTO"></a>
<a id="tocSschoolreservedto"></a>
<a id="tocsschoolreservedto"></a>

```json
{
  "schoolName": "string",
  "compusName": "string",
  "buildCategoryName": "string",
  "buildingName": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|schoolName|string|false|none||none|
|compusName|string|false|none||none|
|buildCategoryName|string|false|none||none|
|buildingName|string|false|none||none|

<h2 id="tocS_OrderSubmitDTO">OrderSubmitDTO</h2>

<a id="schemaordersubmitdto"></a>
<a id="schema_OrderSubmitDTO"></a>
<a id="tocSordersubmitdto"></a>
<a id="tocsordersubmitdto"></a>

```json
{
  "pickUpAddress": 136,
  "reciveAddress": 137,
  "price": 10,
  "serviceFeeRate": 0.05,
  "serviceFee": 0.5,
  "payAmount": 10.5,
  "gap": 500,
  "createTime": "2019-08-24T14:15:22Z",
  "doorAccess": 0,
  "username": "胡椒粉",
  "phone": "13869823548",
  "note": "string",
  "image": "null",
  "categoryId": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|pickUpAddress|integer(int64)|false|none||取件地址|
|reciveAddress|integer(int64)|false|none||收件地址|
|price|number|false|none|订单基础金额(骑手实际收入)|10|
|serviceFeeRate|number|false|none|付费费率快照|0.05，从系统配置表中查询|
|serviceFee|number|false|none|服务费|0.5|
|payAmount|number|false|none|用户支付总额|10.5|
|gap|integer(int32)|false|none||none|
|createTime|string(date-time)|false|none||订单创建时间|
|doorAccess|integer(int32)|false|none||是否有门禁|
|username|string|false|none||发单人昵称|
|phone|string|false|none||发单人手机号|
|note|string|false|none||订单说明文本|
|image|string|false|none||订单说明图片|
|categoryId|integer(int64)|false|none||订单类别id|

<h2 id="tocS_MessageDeliveredDTO">MessageDeliveredDTO</h2>

<a id="schemamessagedelivereddto"></a>
<a id="schema_MessageDeliveredDTO"></a>
<a id="tocSmessagedelivereddto"></a>
<a id="tocsmessagedelivereddto"></a>

```json
{
  "orderId": 0,
  "takeOrderUserId": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|orderId|integer(int64)|false|none||none|
|takeOrderUserId|integer(int64)|false|none||none|

<h2 id="tocS_MessageTakeOrderDTO">MessageTakeOrderDTO</h2>

<a id="schemamessagetakeorderdto"></a>
<a id="schema_MessageTakeOrderDTO"></a>
<a id="tocSmessagetakeorderdto"></a>
<a id="tocsmessagetakeorderdto"></a>

```json
{
  "orderId": 0,
  "takeOrderUserId": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|orderId|integer(int64)|false|none||none|
|takeOrderUserId|integer(int64)|false|none||none|

<h2 id="tocS_AddressBookDTO">AddressBookDTO</h2>

<a id="schemaaddressbookdto"></a>
<a id="schema_AddressBookDTO"></a>
<a id="tocSaddressbookdto"></a>
<a id="tocsaddressbookdto"></a>

```json
{
  "schoolNumberId": 0,
  "compusNumberId": 0,
  "buildCategoryNumberId": 0,
  "buildingNumberId": 0,
  "details": "string",
  "label": "string",
  "type": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|schoolNumberId|integer(int64)|false|none||none|
|compusNumberId|integer(int64)|false|none||none|
|buildCategoryNumberId|integer(int64)|false|none||none|
|buildingNumberId|integer(int64)|false|none||none|
|details|string|false|none||none|
|label|string|false|none||none|
|type|integer(int32)|false|none||none|

<h2 id="tocS_AdminRegisterDTO">AdminRegisterDTO</h2>

<a id="schemaadminregisterdto"></a>
<a id="schema_AdminRegisterDTO"></a>
<a id="tocSadminregisterdto"></a>
<a id="tocsadminregisterdto"></a>

```json
{
  "username": "string",
  "password": "string",
  "nickname": "string",
  "avatar": "string",
  "status": 0,
  "school": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|username|string|false|none||登录的用户名|
|password|string|false|none||登录密码|
|nickname|string|false|none||昵称|
|avatar|string|false|none||头像图片地址|
|status|integer|false|none||0是超级管理员 1不是|
|school|integer|false|none||0是超级管理员 其他则是具体对应的学校id,对应为分别的管理员|

<h2 id="tocS_AdminBannerAddDTO">AdminBannerAddDTO</h2>

<a id="schemaadminbanneradddto"></a>
<a id="schema_AdminBannerAddDTO"></a>
<a id="tocSadminbanneradddto"></a>
<a id="tocsadminbanneradddto"></a>

```json
{
  "imgUrl": "string",
  "title": "string",
  "schoolId": 0,
  "jumpType": 0,
  "jumpTarget": "string",
  "remark": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|imgUrl|string|false|none||轮播图地址|
|title|string|false|none||轮播图标题|
|schoolId|integer(int64)|false|none||轮播图对应的学校id(如果为0则代表所有学校，即通用的轮播图，否则为对应学校的轮播图)|
|jumpType|integer|false|none||跳转类型(0无跳转 1网页链接 2站内网页 3小程序页面)|
|jumpTarget|string|false|none||跳转目标：URL或路由/页面标识|
|remark|string|false|none||备注说明|

<h2 id="tocS_AdminSystemConfigDTO">AdminSystemConfigDTO</h2>

<a id="schemaadminsystemconfigdto"></a>
<a id="schema_AdminSystemConfigDTO"></a>
<a id="tocSadminsystemconfigdto"></a>
<a id="tocsadminsystemconfigdto"></a>

```json
{
  "serviceFeeRate": 0,
  "serviceFeeMin": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|serviceFeeRate|number|false|none||服务费率|
|serviceFeeMin|number|false|none||最低服务费用|

<h2 id="tocS_ResultWeChatTransferVO">ResultWeChatTransferVO</h2>

<a id="schemaresultwechattransfervo"></a>
<a id="schema_ResultWeChatTransferVO"></a>
<a id="tocSresultwechattransfervo"></a>
<a id="tocsresultwechattransfervo"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "outBillNo": "string",
    "transferBillNo": "string",
    "createTime": "string",
    "state": "string",
    "failReason": "string",
    "packageInfo": "string"
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|msg|string|false|none||none|
|data|[WeChatTransferVO](#schemawechattransfervo)|false|none||none|

<h2 id="tocS_WeChatTransferVO">WeChatTransferVO</h2>

<a id="schemawechattransfervo"></a>
<a id="schema_WeChatTransferVO"></a>
<a id="tocSwechattransfervo"></a>
<a id="tocswechattransfervo"></a>

```json
{
  "outBillNo": "string",
  "transferBillNo": "string",
  "createTime": "string",
  "state": "string",
  "failReason": "string",
  "packageInfo": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|outBillNo|string|false|none||none|
|transferBillNo|string|false|none||none|
|createTime|string|false|none||none|
|state|string|false|none||none|
|failReason|string|false|none||none|
|packageInfo|string|false|none||none|

<h2 id="tocS_ResultWeChatPrePayVO">ResultWeChatPrePayVO</h2>

<a id="schemaresultwechatprepayvo"></a>
<a id="schema_ResultWeChatPrePayVO"></a>
<a id="tocSresultwechatprepayvo"></a>
<a id="tocsresultwechatprepayvo"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "prepayId": "string",
    "timeStamp": "string",
    "nonceStr": "string",
    "signType": "string",
    "paySign": "string"
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|msg|string|false|none||none|
|data|[WeChatPrePayVO](#schemawechatprepayvo)|false|none||none|

<h2 id="tocS_WeChatPrePayVO">WeChatPrePayVO</h2>

<a id="schemawechatprepayvo"></a>
<a id="schema_WeChatPrePayVO"></a>
<a id="tocSwechatprepayvo"></a>
<a id="tocswechatprepayvo"></a>

```json
{
  "prepayId": "string",
  "timeStamp": "string",
  "nonceStr": "string",
  "signType": "string",
  "paySign": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|prepayId|string|false|none||none|
|timeStamp|string|false|none||none|
|nonceStr|string|false|none||none|
|signType|string|false|none||none|
|paySign|string|false|none||none|

<h2 id="tocS_ResultUserLoginVO">ResultUserLoginVO</h2>

<a id="schemaresultuserloginvo"></a>
<a id="schema_ResultUserLoginVO"></a>
<a id="tocSresultuserloginvo"></a>
<a id="tocsresultuserloginvo"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "id": 0,
    "username": "string",
    "realname": "string",
    "openid": "string",
    "headImg": "string",
    "sex": 0,
    "phone": "string",
    "authentication": 0,
    "schoolId": 0,
    "stuId": "string",
    "studentIdCard": "string",
    "studentIdCardReview": 0,
    "score": 0,
    "money": 0,
    "alipayPaymentCode": "string",
    "weChatPaymentCode": "string",
    "isManager": 0,
    "deleted": 0,
    "createTime": "2019-08-24T14:15:22Z",
    "updateTime": "2019-08-24T14:15:22Z",
    "token": "string"
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|msg|string|false|none||none|
|data|[UserLoginVO](#schemauserloginvo)|false|none||none|

<h2 id="tocS_UserLoginVO">UserLoginVO</h2>

<a id="schemauserloginvo"></a>
<a id="schema_UserLoginVO"></a>
<a id="tocSuserloginvo"></a>
<a id="tocsuserloginvo"></a>

```json
{
  "id": 0,
  "username": "string",
  "realname": "string",
  "openid": "string",
  "headImg": "string",
  "sex": 0,
  "phone": "string",
  "authentication": 0,
  "schoolId": 0,
  "stuId": "string",
  "studentIdCard": "string",
  "studentIdCardReview": 0,
  "score": 0,
  "money": 0,
  "alipayPaymentCode": "string",
  "weChatPaymentCode": "string",
  "isManager": 0,
  "deleted": 0,
  "createTime": "2019-08-24T14:15:22Z",
  "updateTime": "2019-08-24T14:15:22Z",
  "token": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||none|
|username|string|false|none||none|
|realname|string|false|none||none|
|openid|string|false|none||none|
|headImg|string|false|none||none|
|sex|integer(int32)|false|none||none|
|phone|string|false|none||none|
|authentication|integer(int32)|false|none||none|
|schoolId|integer(int64)|false|none||none|
|stuId|string|false|none||none|
|studentIdCard|string|false|none||none|
|studentIdCardReview|integer(int32)|false|none||none|
|score|integer(int32)|false|none||none|
|money|number|false|none||none|
|alipayPaymentCode|string|false|none||none|
|weChatPaymentCode|string|false|none||none|
|isManager|integer(int32)|false|none||none|
|deleted|integer(int32)|false|none||none|
|createTime|string(date-time)|false|none||none|
|updateTime|string(date-time)|false|none||none|
|token|string|false|none||20h后过期|

<h2 id="tocS_ResultUserVO">ResultUserVO</h2>

<a id="schemaresultuservo"></a>
<a id="schema_ResultUserVO"></a>
<a id="tocSresultuservo"></a>
<a id="tocsresultuservo"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "id": 0,
    "username": "string",
    "realname": "string",
    "openid": "string",
    "headImg": "string",
    "sex": 0,
    "phone": "string",
    "authentication": 0,
    "schoolId": 0,
    "schoolName": "string",
    "stuId": "string",
    "studentIdCard": "string",
    "studentIdCardReview": 0,
    "score": 0,
    "money": 0,
    "alipayPaymentCode": "string",
    "weChatPaymentCode": "string",
    "isManager": 0,
    "deleted": 0,
    "createTime": "2019-08-24T14:15:22Z",
    "updateTime": "2019-08-24T14:15:22Z"
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|msg|string|false|none||none|
|data|[UserVO](#schemauservo)|false|none||none|

<h2 id="tocS_UserVO">UserVO</h2>

<a id="schemauservo"></a>
<a id="schema_UserVO"></a>
<a id="tocSuservo"></a>
<a id="tocsuservo"></a>

```json
{
  "id": 0,
  "username": "string",
  "realname": "string",
  "openid": "string",
  "headImg": "string",
  "sex": 0,
  "phone": "string",
  "authentication": 0,
  "schoolId": 0,
  "schoolName": "string",
  "stuId": "string",
  "studentIdCard": "string",
  "studentIdCardReview": 0,
  "score": 0,
  "money": 0,
  "alipayPaymentCode": "string",
  "weChatPaymentCode": "string",
  "isManager": 0,
  "deleted": 0,
  "createTime": "2019-08-24T14:15:22Z",
  "updateTime": "2019-08-24T14:15:22Z"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||id|
|username|string|false|none||昵称|
|realname|string|false|none||学生认证的真实姓名|
|openid|string|false|none||微信的openId|
|headImg|string|false|none||头像链接|
|sex|integer(int32)|false|none||暂时禁用，性别|
|phone|string|false|none||手机号|
|authentication|integer(int32)|false|none||0未认证 1认证通过，可以进行发单接单操作|
|schoolId|integer(int64)|false|none||学生认证的学校Id|
|schoolName|string|false|none||学生认证的学校名|
|stuId|string|false|none||学生认证的学号|
|studentIdCard|string|false|none||学生认证图片的链接|
|studentIdCardReview|integer(int32)|false|none||学生认证的通过情况：0未审核 1审核中 2审核通过 3审核不通过|
|score|integer(int32)|false|none||暂时禁用，信誉分|
|money|number|false|none||暂时禁用，余额|
|alipayPaymentCode|string|false|none||暂时禁用，支付宝付款码链接|
|weChatPaymentCode|string|false|none||暂时禁用，微信付款码链接|
|isManager|integer(int32)|false|none||暂时禁用，是否为管理员|
|deleted|integer(int32)|false|none||软删除|
|createTime|string(date-time)|false|none||账号创建时间|
|updateTime|string(date-time)|false|none||账号最后更新时间|

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

<h2 id="tocS_AddressBookShowVO">AddressBookShowVO</h2>

<a id="schemaaddressbookshowvo"></a>
<a id="schema_AddressBookShowVO"></a>
<a id="tocSaddressbookshowvo"></a>
<a id="tocsaddressbookshowvo"></a>

```json
{
  "id": 0,
  "schoolName": "string",
  "compusName": "string",
  "buildCategoryName": "string",
  "buildingName": "string",
  "details": "string",
  "label": "string",
  "userId": 0,
  "isDefault": 0,
  "type": 0,
  "deleted": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||none|
|schoolName|string|false|none||none|
|compusName|string|false|none||none|
|buildCategoryName|string|false|none||none|
|buildingName|string|false|none||none|
|details|string|false|none||none|
|label|string|false|none||none|
|userId|integer(int64)|false|none||none|
|isDefault|integer(int32)|false|none||none|
|type|integer(int32)|false|none||none|
|deleted|integer(int32)|false|none||none|

<h2 id="tocS_ResultListAddressBookShowVO">ResultListAddressBookShowVO</h2>

<a id="schemaresultlistaddressbookshowvo"></a>
<a id="schema_ResultListAddressBookShowVO"></a>
<a id="tocSresultlistaddressbookshowvo"></a>
<a id="tocsresultlistaddressbookshowvo"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": [
    {
      "id": 0,
      "schoolName": "string",
      "compusName": "string",
      "buildCategoryName": "string",
      "buildingName": "string",
      "details": "string",
      "label": "string",
      "userId": 0,
      "isDefault": 0,
      "type": 0,
      "deleted": 0
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|msg|string|false|none||none|
|data|[[AddressBookShowVO](#schemaaddressbookshowvo)]|false|none||none|

<h2 id="tocS_AddressBookThreeVO">AddressBookThreeVO</h2>

<a id="schemaaddressbookthreevo"></a>
<a id="schema_AddressBookThreeVO"></a>
<a id="tocSaddressbookthreevo"></a>
<a id="tocsaddressbookthreevo"></a>

```json
{
  "school": [
    {
      "id": 0,
      "schoolName": "string",
      "numberId": 0,
      "deleted": 0
    }
  ],
  "compus": [
    {
      "numberId": 0,
      "compusName": "string"
    }
  ],
  "buildCategory": [
    {
      "numberId": 0,
      "buildCategoryName": "string"
    }
  ],
  "building": [
    {
      "numberId": 0,
      "buildingName": "string"
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|school|[[School](#schemaschool)]|false|none||none|
|compus|[[Compus](#schemacompus)]|false|none||none|
|buildCategory|[[BuildCategory](#schemabuildcategory)]|false|none||none|
|building|[[Building](#schemabuilding)]|false|none||none|

<h2 id="tocS_ResultAddressBookThreeVO">ResultAddressBookThreeVO</h2>

<a id="schemaresultaddressbookthreevo"></a>
<a id="schema_ResultAddressBookThreeVO"></a>
<a id="tocSresultaddressbookthreevo"></a>
<a id="tocsresultaddressbookthreevo"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "school": [
      {
        "id": 0,
        "schoolName": "string",
        "numberId": 0,
        "deleted": 0
      }
    ],
    "compus": [
      {
        "numberId": 0,
        "compusName": "string"
      }
    ],
    "buildCategory": [
      {
        "numberId": 0,
        "buildCategoryName": "string"
      }
    ],
    "building": [
      {
        "numberId": 0,
        "buildingName": "string"
      }
    ]
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|msg|string|false|none||none|
|data|[AddressBookThreeVO](#schemaaddressbookthreevo)|false|none||none|

<h2 id="tocS_ResultAdminLoginVO">ResultAdminLoginVO</h2>

<a id="schemaresultadminloginvo"></a>
<a id="schema_ResultAdminLoginVO"></a>
<a id="tocSresultadminloginvo"></a>
<a id="tocsresultadminloginvo"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "id": 0,
    "username": "string",
    "passwordHash": "string",
    "nickname": "string",
    "avatar": "string",
    "status": 0,
    "school": 0,
    "lastLoginIp": "string",
    "lastLoginTime": "string",
    "createTime": "string",
    "updateTime": "string",
    "deleted": 0,
    "adminToken": "string"
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|msg|string|false|none||none|
|data|[AdminLoginVO](#schemaadminloginvo)|false|none||none|

<h2 id="tocS_AdminLoginVO">AdminLoginVO</h2>

<a id="schemaadminloginvo"></a>
<a id="schema_AdminLoginVO"></a>
<a id="tocSadminloginvo"></a>
<a id="tocsadminloginvo"></a>

```json
{
  "id": 0,
  "username": "string",
  "passwordHash": "string",
  "nickname": "string",
  "avatar": "string",
  "status": 0,
  "school": 0,
  "lastLoginIp": "string",
  "lastLoginTime": "string",
  "createTime": "string",
  "updateTime": "string",
  "deleted": 0,
  "adminToken": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||none|
|username|string|false|none||登录的用户名|
|passwordHash|string|false|none||密码的哈希值|
|nickname|string|false|none||昵称|
|avatar|string|false|none||头像地址|
|status|integer|false|none||状态 0是超级管理员 1不是|
|school|integer|false|none||0是超级管理员 其他则是具体对应的学校id,对应为分别的管理员|
|lastLoginIp|string|false|none||最后一次登录ip地址|
|lastLoginTime|string|false|none||最后一次登录时间|
|createTime|string|false|none||创建时间|
|updateTime|string|false|none||更新时间|
|deleted|integer|false|none||假删除(0未删除 1已删除)|
|adminToken|string|false|none||管理员用户的token，7天后过期|

<h2 id="tocS_AdminAdressBuildingVO">AdminAdressBuildingVO</h2>

<a id="schemaadminadressbuildingvo"></a>
<a id="schema_AdminAdressBuildingVO"></a>
<a id="tocSadminadressbuildingvo"></a>
<a id="tocsadminadressbuildingvo"></a>

```json
{
  "id": 0,
  "schoolId": 0,
  "schoolName": "string",
  "compusId": 0,
  "compusName": "string",
  "buildCategoryId": 0,
  "buildCategoryName": "string",
  "numberId": 0,
  "buildingName": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|false|none||楼宇id|
|schoolId|integer|false|none||学校id|
|schoolName|string|false|none||学校名字|
|compusId|integer|false|none||校区id|
|compusName|string|false|none||校区名字|
|buildCategoryId|integer|false|none||楼宇分类id|
|buildCategoryName|string|false|none||楼宇分类名字|
|numberId|integer|false|none||numberid|
|buildingName|string|false|none||楼宇名字|

<h2 id="tocS_ResultAdminAdressBuildingVO">ResultAdminAdressBuildingVO</h2>

<a id="schemaresultadminadressbuildingvo"></a>
<a id="schema_ResultAdminAdressBuildingVO"></a>
<a id="tocSresultadminadressbuildingvo"></a>
<a id="tocsresultadminadressbuildingvo"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": [
    {
      "id": 0,
      "schoolId": 0,
      "schoolName": "string",
      "compusId": 0,
      "compusName": "string",
      "buildCategoryId": 0,
      "buildCategoryName": "string",
      "numberId": 0,
      "buildingName": "string"
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|msg|string|false|none||none|
|data|[[AdminAdressBuildingVO](#schemaadminadressbuildingvo)]|false|none||none|

<h2 id="tocS_ResultListBannerVO">ResultListBannerVO</h2>

<a id="schemaresultlistbannervo"></a>
<a id="schema_ResultListBannerVO"></a>
<a id="tocSresultlistbannervo"></a>
<a id="tocsresultlistbannervo"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": [
    {
      "id": 0,
      "imgUrl": "string",
      "title": "string",
      "schoolId": 0,
      "schoolName": "string",
      "jumpType": 0,
      "jumpTarget": "string",
      "remark": "string",
      "createBy": 0,
      "createTime": "string",
      "deleted": 0
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||响应码 0失败 1成功|
|msg|string|false|none||响应消息，失败时会输出原因|
|data|[[Banner](#schemabanner)]|false|none||返回的数据|

<h2 id="tocS_Order">Order</h2>

<a id="schemaorder"></a>
<a id="schema_Order"></a>
<a id="tocSorder"></a>
<a id="tocsorder"></a>

```json
{
  "id": 0,
  "pickUpAddress": 136,
  "reciveAddress": 137,
  "orderNumber": "string",
  "price": 10,
  "serviceFeeRate": 0.05,
  "serviceFee": 0.5,
  "payAmount": 10.5,
  "deliveryTime": "2019-08-24T14:15:22Z",
  "cancelTime": "2019-08-24T14:15:22Z",
  "cancelReson": "string",
  "exceedTime": "2019-08-24T14:15:22Z",
  "gap": 500,
  "createTime": "2019-08-24T14:15:22Z",
  "doorAccess": 0,
  "userId": 0,
  "username": "胡椒粉",
  "phone": "13869823548",
  "status": 0,
  "note": "string",
  "image": "null",
  "categoryId": 0,
  "deleted": 0,
  "realPrice": 0
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
|serviceFeeRate|number|false|none|付费费率快照|0.05，从系统配置表中查询|
|serviceFee|number|false|none|服务费|0.5|
|payAmount|number|false|none|用户支付总额|10.5|
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
|realPrice|number|false|none||暂时禁用，实际价格|

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
    "pickUpAddress": 136,
    "reciveAddress": 137,
    "orderNumber": "string",
    "price": 10,
    "serviceFeeRate": 0.05,
    "serviceFee": 0.5,
    "payAmount": 10.5,
    "deliveryTime": "2019-08-24T14:15:22Z",
    "cancelTime": "2019-08-24T14:15:22Z",
    "cancelReson": "string",
    "exceedTime": "2019-08-24T14:15:22Z",
    "gap": 500,
    "createTime": "2019-08-24T14:15:22Z",
    "doorAccess": 0,
    "userId": 0,
    "username": "胡椒粉",
    "phone": "13869823548",
    "status": 0,
    "note": "string",
    "image": "null",
    "categoryId": 0,
    "deleted": 0,
    "realPrice": 0
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|msg|string|false|none||none|
|data|[Order](#schemaorder)|false|none||none|

<h2 id="tocS_ResultListSchool">ResultListSchool</h2>

<a id="schemaresultlistschool"></a>
<a id="schema_ResultListSchool"></a>
<a id="tocSresultlistschool"></a>
<a id="tocsresultlistschool"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": [
    {
      "id": 0,
      "schoolName": "string",
      "numberId": 0,
      "deleted": 0
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|msg|string|false|none||none|
|data|[[School](#schemaschool)]|false|none||none|

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

<h2 id="tocS_School">School</h2>

<a id="schemaschool"></a>
<a id="schema_School"></a>
<a id="tocSschool"></a>
<a id="tocsschool"></a>

```json
{
  "id": 0,
  "schoolName": "string",
  "numberId": 0,
  "deleted": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||none|
|schoolName|string|false|none||none|
|numberId|integer(int64)|false|none||none|
|deleted|integer(int32)|false|none||none|

<h2 id="tocS_Category">Category</h2>

<a id="schemacategory"></a>
<a id="schema_Category"></a>
<a id="tocScategory"></a>
<a id="tocscategory"></a>

```json
{
  "id": 0,
  "categoryName": "string",
  "image": "string",
  "deleted": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||none|
|categoryName|string|false|none||none|
|image|string|false|none||none|
|deleted|integer(int32)|false|none||none|

<h2 id="tocS_ResultListCategory">ResultListCategory</h2>

<a id="schemaresultlistcategory"></a>
<a id="schema_ResultListCategory"></a>
<a id="tocSresultlistcategory"></a>
<a id="tocsresultlistcategory"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": [
    {
      "id": 0,
      "categoryName": "string",
      "image": "string",
      "deleted": 0
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|msg|string|false|none||none|
|data|[[Category](#schemacategory)]|false|none||none|

<h2 id="tocS_ResultCategory">ResultCategory</h2>

<a id="schemaresultcategory"></a>
<a id="schema_ResultCategory"></a>
<a id="tocSresultcategory"></a>
<a id="tocsresultcategory"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "id": 0,
    "categoryName": "string",
    "image": "string",
    "deleted": 0
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|false|none||none|
|msg|string|false|none||none|
|data|[Category](#schemacategory)|false|none||none|

<h2 id="tocS_BuildCategory">BuildCategory</h2>

<a id="schemabuildcategory"></a>
<a id="schema_BuildCategory"></a>
<a id="tocSbuildcategory"></a>
<a id="tocsbuildcategory"></a>

```json
{
  "numberId": 0,
  "buildCategoryName": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|numberId|integer(int64)|false|none||none|
|buildCategoryName|string|false|none||none|

<h2 id="tocS_Building">Building</h2>

<a id="schemabuilding"></a>
<a id="schema_Building"></a>
<a id="tocSbuilding"></a>
<a id="tocsbuilding"></a>

```json
{
  "numberId": 0,
  "buildingName": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|numberId|integer(int64)|false|none||none|
|buildingName|string|false|none||none|

<h2 id="tocS_Compus">Compus</h2>

<a id="schemacompus"></a>
<a id="schema_Compus"></a>
<a id="tocScompus"></a>
<a id="tocscompus"></a>

```json
{
  "numberId": 0,
  "compusName": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|numberId|integer(int64)|false|none||none|
|compusName|string|false|none||none|

<h2 id="tocS_Banner">Banner</h2>

<a id="schemabanner"></a>
<a id="schema_Banner"></a>
<a id="tocSbanner"></a>
<a id="tocsbanner"></a>

```json
{
  "id": 0,
  "imgUrl": "string",
  "title": "string",
  "schoolId": 0,
  "schoolName": "string",
  "jumpType": 0,
  "jumpTarget": "string",
  "remark": "string",
  "createBy": 0,
  "createTime": "string",
  "deleted": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||none|
|imgUrl|string|false|none||轮播图图片地址|
|title|string|false|none||轮播图标题|
|schoolId|integer(int64)|false|none||轮播图对应的学校id(如果为0则代表所有学校，即通用的轮播图，否则为对应学校的轮播图)|
|schoolName|string|false|none||轮播图对应的学校名字|
|jumpType|integer|false|none||跳转类型(0无跳转 1网页链接 2站内网页 3小程序页面)|
|jumpTarget|string|false|none||跳转目标：URL或路由/页面标识|
|remark|string|false|none||备注说明|
|createBy|integer(int64)|false|none||创建人id|
|createTime|string|false|none||创建时间|
|deleted|integer|false|none||逻辑删除字段(0 未删除, 1 已删除)|

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

