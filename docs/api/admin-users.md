# 管理端 API — 用户管理

Base URL: `http://localhost:8080/admin/api`

所有接口需要在 Header 中携带管理员 token：
```
token: <admin_login_token>
```

---

## 1. 全部用户

**`GET /admin/api/users/all`**

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
    "total": 356,
    "page": 1,
    "pageSize": 20,
    "list": [
      {
        "id": 2001,
        "username": "张三",
        "realname": "张三丰",
        "headImg": "https://xxx.oss.com/avatar/abc.png",
        "sex": 1,
        "phone": "13800138000",
        "schoolName": "浙江工业大学",
        "stuId": "2021001001",
        "authentication": 1,
        "studentIdCardReview": 2,
        "score": 100,
        "orderCount": 12,
        "takeOrderCount": 8,
        "createTime": "2026-03-15 10:30:00"
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
| list | array | 用户列表 |

list 项：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 用户id |
| username | string | 昵称 |
| realname | string | 真实姓名 |
| headImg | string | 头像地址 |
| sex | int | 性别：0未知 1男 2女 |
| phone | string | 手机号 |
| schoolName | string | 所属学校 |
| stuId | string | 学号 |
| authentication | int | 认证状态：0未认证 1已认证 |
| studentIdCardReview | int | 学生证审核：0未提交 1审核中 2通过 3不通过 |
| score | int | 信誉分 |
| orderCount | int | 发单数 |
| takeOrderCount | int | 接单数 |
| createTime | string | 注册时间 |

---

## 2. 已认证用户

**`GET /admin/api/users/authenticated`**

请求参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 是 | 页码 |
| pageSize | int | 是 | 每页条数 |

> 返回字段同 1.全部用户

---

## 3. 待审核用户

学生证已提交、审核中的用户。

**`GET /admin/api/users/pending-review`**

请求参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 是 | 页码 |
| pageSize | int | 是 | 每页条数 |

> 返回字段同 1.全部用户，额外字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| studentIdCard | string | 学生证照片地址 |

---

## 4. 用户详情

**`GET /admin/api/users/{id}`**

返回示例：

```json
{
  "code": 1,
  "data": {
    "id": 2001,
    "username": "张三",
    "realname": "张三丰",
    "headImg": "https://xxx.oss.com/avatar/abc.png",
    "sex": 1,
    "phone": "13800138000",
    "schoolName": "浙江工业大学",
    "stuId": "2021001001",
    "authentication": 1,
    "studentIdCard": "https://xxx.oss.com/stuid/abc.jpg",
    "studentIdCardReview": 2,
    "score": 100,
    "isManager": 0,
    "orderCount": 12,
    "takeOrderCount": 8,
    "totalEarned": 156.00,
    "createTime": "2026-03-15 10:30:00",
    "updateTime": "2026-05-10 18:00:00"
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 用户id |
| username | string | 昵称 |
| realname | string | 真实姓名 |
| headImg | string | 头像 |
| sex | int | 性别 |
| phone | string | 手机号 |
| schoolName | string | 学校 |
| stuId | string | 学号 |
| authentication | int | 认证状态 |
| studentIdCard | string | 学生证照片 |
| studentIdCardReview | int | 审核状态 |
| score | int | 信誉分 |
| isManager | int | 是否管理员 |
| orderCount | int | 发单数 |
| takeOrderCount | int | 接单数 |
| totalEarned | decimal | 累计收入（接单报酬合计） |
| createTime | string | 注册时间 |
| updateTime | string | 最近操作时间 |

---

## 5. 用户统计

**`GET /admin/api/users/statistics`**

返回示例：

```json
{
  "code": 1,
  "data": {
    "totalCount": 356,
    "authenticatedCount": 210,
    "unauthenticatedCount": 146,
    "pendingReviewCount": 8,
    "todayNewCount": 3,
    "managerCount": 5
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| totalCount | int | 用户总数 |
| authenticatedCount | int | 已认证用户数 |
| unauthenticatedCount | int | 未认证用户数 |
| pendingReviewCount | int | 待审核用户数 |
| todayNewCount | int | 今日新增用户 |
| managerCount | int | 管理员数量 |
