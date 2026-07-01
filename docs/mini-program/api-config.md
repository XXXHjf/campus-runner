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

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|object| 是 |none|

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

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|object| 是 |none|
|» serviceFeeRate|body|number| 否 |服务费率|

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

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|object| 是 |none|

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

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|object| 是 |none|
|» serviceFeeMin|body|number| 否 |最低服务费用|

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

