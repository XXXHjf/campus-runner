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

# 数据模型

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

