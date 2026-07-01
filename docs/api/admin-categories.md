# 管理端 API — 分类管理

Base URL: `http://localhost:8080/admin/api`

所有接口需要在 Header 中携带管理员 token：
```
token: <admin_login_token>
```

分类指的是创建订单时的订单类别，如"代取快递""代购""代排队"等。

---

## 1. 分类列表

**`GET /admin/api/categories`**

请求参数：无

返回示例：

```json
{
  "code": 1,
  "data": [
    {
      "id": 1,
      "categoryName": "代取快递",
      "image": "https://xxx.oss.com/category/express.png"
    },
    {
      "id": 2,
      "categoryName": "代购",
      "image": "https://xxx.oss.com/category/shopping.png"
    },
    {
      "id": 3,
      "categoryName": "代排队",
      "image": "https://xxx.oss.com/category/queue.png"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 分类id |
| categoryName | string | 分类名称 |
| image | string | 分类图标地址 |

---

## 2. 新增分类

**`POST /admin/api/categories`**

请求体：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| categoryName | string | 是 | 分类名称 |
| image | string | 否 | 分类图标地址 |

返回新增的分类对象。

---

## 3. 修改分类

**`PUT /admin/api/categories/{id}`**

请求体：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| categoryName | string | 否 | 分类名称 |
| image | string | 否 | 分类图标地址 |

返回修改后的分类对象。

---

## 4. 删除分类

**`DELETE /admin/api/categories/{id}`**

> 逻辑删除，将 deleted 标记为 1。如果该分类下已有订单，不影响历史订单数据。
