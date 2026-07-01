# 管理端 API — 订单管理

Base URL: `http://localhost:8080/admin/api`

所有接口需要在 Header 中携带管理员 token：
```
token: <admin_login_token>
```

---

## 1. 全部订单

**`GET /admin/api/orders/all`**

请求参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 是 | 页码，从1开始 |
| pageSize | int | 是 | 每页条数 |

返回示例：

```json
{
  "code": 1,
  "data": {
    "total": 236,
    "page": 1,
    "pageSize": 20,
    "list": [
      {
        "id": 1001,
        "orderNumber": "1715266800000",
        "status": 0,
        "categoryName": "代取快递",
        "price": 5.00,
        "serviceFee": 0.50,
        "payAmount": 5.50,
        "username": "张三",
        "phone": "13800138000",
        "pickUpAddress": "浙江工业大学 朝晖校区 宿舍楼 尚德园3号楼",
        "reciveAddress": "浙江工业大学 朝晖校区 教学楼 子良楼",
        "note": "小件快递，菜鸟驿站3号柜",
        "doorAccess": 0,
        "createTime": "2026-05-10 14:30:00"
      }
    ]
  }
}
```

返回字段说明：

| 字段 | 类型 | 说明 |
|------|------|------|
| total | int | 总条数 |
| page | int | 当前页码 |
| pageSize | int | 每页条数 |
| list | array | 订单列表 |

list 项：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 订单主键 |
| orderNumber | string | 订单编号 |
| status | int | 状态码：-4退款异常 -3退款成功 -2退款中 -1未支付 0待接单 1已接单 2派送中 3已送达 4已取消 5已完成 6提现成功 7提现失败 |
| categoryName | string | 订单分类名称 |
| price | decimal | 订单基础金额（骑手报酬） |
| serviceFee | decimal | 平台服务费 |
| payAmount | decimal | 用户支付总额 |
| username | string | 发单人昵称 |
| phone | string | 发单人电话 |
| pickUpAddress | string | 取件地址（拼接后的完整地址） |
| reciveAddress | string | 收件地址（拼接后的完整地址） |
| note | string | 订单说明 |
| doorAccess | int | 是否门禁 0否 1是 |
| createTime | string | 创建时间 |

---

## 2. 待接单

**`GET /admin/api/orders/waiting`**

请求参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 是 | 页码 |
| pageSize | int | 是 | 每页条数 |

> 返回字段同 1.全部订单

---

## 3. 进行中

包含状态：已接单(1)、派送中(2)、已送达(3)

**`GET /admin/api/orders/in-progress`**

请求参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 是 | 页码 |
| pageSize | int | 是 | 每页条数 |

> 返回字段同 1.全部订单，额外字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| takerName | string | 接单人姓名 |
| takerPhone | string | 接单人电话 |
| takeOrderTime | string | 接单时间 |
| deliveryTime | string | 送达时间 |

---

## 4. 已完成

包含状态：已完成(5)、提现成功(6)、提现失败(7)

**`GET /admin/api/orders/completed`**

请求参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 是 | 页码 |
| pageSize | int | 是 | 每页条数 |

> 返回字段同 3.进行中，额外字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| completeTime | string | 发单人确认收货时间 |
| withdrawalStatus | int | 提现状态：6=成功 7=失败，未提现则为 null |

---

## 5. 已取消/退款

包含状态：已取消(4)、退款中(-2)、退款成功(-3)、退款异常(-4)

**`GET /admin/api/orders/canceled`**

请求参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 是 | 页码 |
| pageSize | int | 是 | 每页条数 |

> 返回字段同 1.全部订单，额外字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| cancelTime | string | 取消时间 |
| cancelReason | string | 取消原因 |
| refundAmount | decimal | 退款金额（退款单涉及时有值） |
| refundStatus | string | 退款状态：退款中 / 退款成功 / 退款异常 |

---

## 6. 订单详情

**`GET /admin/api/orders/{id}`**

返回示例：

```json
{
  "code": 1,
  "data": {
    "order": {
      "id": 1001,
      "orderNumber": "1715266800000",
      "status": -2,
      "categoryName": "代取快递",
      "price": 5.00,
      "serviceFeeRate": 0.05,
      "serviceFee": 0.50,
      "payAmount": 5.50,
      "username": "张三",
      "phone": "13800138000",
      "pickUpAddress": "浙江工业大学 朝晖校区 宿舍楼 尚德园3号楼",
      "reciveAddress": "浙江工业大学 朝晖校区 教学楼 子良楼",
      "note": "小件快递",
      "image": "https://xxx.oss.com/order/abc.jpg",
      "doorAccess": 0,
      "gap": 30,
      "exceedTime": "2026-05-10 15:00:00",
      "createTime": "2026-05-10 14:30:00",
      "cancelTime": "2026-05-10 14:45:00",
      "cancelReason": "发错信息了"
    },
    "taker": {
      "userId": 2001,
      "realname": "李四",
      "phone": "13900139000",
      "takeTime": "2026-05-10 14:35:00",
      "deliveryTime": null,
      "image": null
    },
    "payment": {
      "transactionId": "4200001234567890",
      "tradeState": "REFUND",
      "total": 550,
      "serviceFee": 50,
      "payerOpenid": "oN0B-6xxxx",
      "successTime": "2026-05-10 14:31:00"
    },
    "refund": {
      "refundNumber": "RF1715266800001",
      "refundId": "5030001234567890",
      "refundAmount": 550,
      "refundStatus": "退款中",
      "reason": "发错信息了",
      "createTime": "2026-05-10 14:46:00"
    }
  }
}
```

order 字段：同列表字段 + `categoryName`、`serviceFeeRate`、`image`、`gap`、`exceedTime`、`cancelTime`、`cancelReason`

taker 字段（无接单人为 null）：

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | long | 接单人id |
| realname | string | 接单人姓名 |
| phone | string | 接单人电话 |
| takeTime | string | 接单时间 |
| deliveryTime | string | 送达时间 |
| image | string | 送达凭证图片 |

payment 字段（未支付订单为 null）：

| 字段 | 类型 | 说明 |
|------|------|------|
| transactionId | string | 微信支付订单号 |
| tradeState | string | 交易状态 |
| total | int | 支付总额（分） |
| serviceFee | int | 服务费（分） |
| payerOpenid | string | 支付者 openid |
| successTime | string | 支付完成时间 |

refund 字段（无退款为 null）：

| 字段 | 类型 | 说明 |
|------|------|------|
| refundNumber | string | 商户退款单号 |
| refundId | string | 微信退款单号 |
| refundAmount | int | 退款金额（分） |
| refundStatus | string | 退款状态 |
| reason | string | 退款原因 |
| createTime | string | 退款发起时间 |

---

## 7. 订单统计

**`GET /admin/api/orders/statistics`**

无请求参数。

返回示例：

```json
{
  "code": 1,
  "data": {
    "totalCount": 236,
    "waitingCount": 12,
    "inProgressCount": 8,
    "completedCount": 180,
    "canceledCount": 36,
    "todayNewCount": 5,
    "todayTotalAmount": 126.50,
    "todayServiceFee": 8.90
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| totalCount | int | 订单总数 |
| waitingCount | int | 待接单数 |
| inProgressCount | int | 进行中数 |
| completedCount | int | 已完成数 |
| canceledCount | int | 已取消/退款数 |
| todayNewCount | int | 今日新增订单数 |
| todayTotalAmount | decimal | 今日支付总额 |
| todayServiceFee | decimal | 今日服务费总额 |

---

## 8. 取消订单

**`POST /admin/api/orders/{id}/cancel`**

请求体：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reason | string | 是 | 取消原因 |

> 仅允许状态为 0(待接单) 或 -1(未支付) 的订单取消

---

## 9. 退款

**`POST /admin/api/orders/{id}/refund`**

请求体：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reason | string | 是 | 退款原因 |

> 仅允许已支付且未提现的订单退款（payAmount > 0 且 status 不为 6/7）
