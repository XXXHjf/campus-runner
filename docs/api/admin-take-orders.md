# 管理端 API — 接单管理

Base URL: `http://localhost:8080/admin/api`

所有接口需要在 Header 中携带管理员 token：
```
token: <admin_login_token>
```

---

## 1. 全部接单

**`GET /admin/api/take-orders/all`**

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
    "total": 182,
    "page": 1,
    "pageSize": 20,
    "list": [
      {
        "id": 5001,
        "orderId": 1001,
        "orderNumber": "1715266800000",
        "orderStatus": 5,
        "takeOrderStatus": 2,
        "categoryName": "代取快递",
        "price": 5.00,
        "orderNote": "小件快递",
        "pickUpAddress": "浙江工业大学 朝晖校区 宿舍楼 尚德园3号楼",
        "reciveAddress": "浙江工业大学 朝晖校区 教学楼 子良楼",
        "publisherName": "张三",
        "publisherPhone": "13800138000",
        "takerName": "李四",
        "takerPhone": "13900139000",
        "takeOrderTime": "2026-05-10 14:35:00",
        "deliveryTime": "2026-05-10 15:20:00",
        "takeOrderImage": "https://xxx.oss.com/delivery/xyz.jpg"
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
| list | array | 接单列表 |

list 项：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 接单记录主键 |
| orderId | long | 关联订单id |
| orderNumber | string | 订单编号 |
| orderStatus | int | 订单状态码 |
| takeOrderStatus | int | 接单状态：0已接单 1派送中 2已送达 3已取消 |
| categoryName | string | 订单分类 |
| price | decimal | 订单金额（骑手报酬） |
| orderNote | string | 订单说明 |
| pickUpAddress | string | 取件地址 |
| reciveAddress | string | 收件地址 |
| publisherName | string | 发单人姓名 |
| publisherPhone | string | 发单人电话 |
| takerName | string | 接单人姓名 |
| takerPhone | string | 接单人电话 |
| takeOrderTime | string | 接单时间 |
| deliveryTime | string | 送达时间 |
| takeOrderImage | string | 送达凭证图片 |

---

## 2. 未收款订单

接单人已完成配送且发单人已确认收货，但尚未提现或提现失败。

**`GET /admin/api/take-orders/unpaid`**

请求参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 是 | 页码 |
| pageSize | int | 是 | 每页条数 |

返回示例：

```json
{
  "code": 1,
  "data": {
    "total": 15,
    "page": 1,
    "pageSize": 20,
    "list": [
      {
        "id": 5005,
        "orderId": 1018,
        "orderNumber": "1715353200000",
        "orderStatus": 5,
        "takeOrderStatus": 2,
        "categoryName": "代取快递",
        "price": 8.00,
        "serviceFee": 0.50,
        "payAmount": 8.50,
        "orderNote": "顺丰快递",
        "publisherName": "王五",
        "takerName": "赵六",
        "takerPhone": "13700137000",
        "takeOrderTime": "2026-05-10 16:30:00",
        "deliveryTime": "2026-05-10 17:00:00",
        "completeTime": "2026-05-10 17:15:00",
        "withdrawalStatus": null
      }
    ]
  }
}
```

list 项：同全部接单，额外字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| serviceFee | decimal | 平台服务费 |
| payAmount | decimal | 用户支付总额 |
| completeTime | string | 发单人确认收货时间 |
| withdrawalStatus | int | 提现状态：6=提现成功 7=提现失败 null=尚未提现 |

---

## 3. 接单统计

**`GET /admin/api/take-orders/statistics`**

返回示例：

```json
{
  "code": 1,
  "data": {
    "totalCount": 182,
    "todayNewCount": 3,
    "unpaidCount": 15,
    "unpaidTotalAmount": 126.00,
    "todayCompletedCount": 8,
    "todayCompletedAmount": 42.50
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| totalCount | int | 接单总数 |
| todayNewCount | int | 今日新增接单数 |
| unpaidCount | int | 未收款订单数（已完成但未提现） |
| unpaidTotalAmount | decimal | 未收款总额（骑手待收报酬） |
| todayCompletedCount | int | 今日送达数 |
| todayCompletedAmount | decimal | 今日送达订单金额合计 |
