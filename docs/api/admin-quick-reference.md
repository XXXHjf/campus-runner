# 管理端新增接口清单

> 本地 Base URL: `http://localhost:8080`
> Header: `token: <admin_login_token>`

---

## 分类管理

| 方法 | 路径 | 说明 | 描述 |
|------|------|------|------|
| GET | `/admin/api/categories` | 分类列表 | 获取所有可用订单分类，返回分类ID、名称和图标地址 |
| POST | `/admin/api/categories` | 新增 | 创建新的订单分类，需提供分类名称和可选图标地址 |
| PUT | `/admin/api/categories/{id}` | 修改 | 修改指定分类的名称或图标地址，只传需要修改的字段即可 |
| DELETE | `/admin/api/categories/{id}` | 删除 | 逻辑删除指定分类（软删除），不影响已有订单历史数据 |

---

## 用户管理

| 方法 | 路径 | 说明 | 描述 |
|------|------|------|------|
| GET | `/admin/api/users/all?page=1&pageSize=20` | 全部用户 | 分页查询所有注册用户，包含认证状态、发单数和接单数 |
| GET | `/admin/api/users/authenticated?page=1&pageSize=20` | 已认证用户 | 分页查询已完成学生认证的用户列表 |
| GET | `/admin/api/users/pending-review?page=1&pageSize=20` | 待审核用户 | 分页查询学生证待审核的用户，包含学生证照片 |
| GET | `/admin/api/users/{id}` | 用户详情 | 根据ID查询用户资料、认证材料、累计收入和发单接单统计 |
| GET | `/admin/api/users/statistics` | 用户统计 | 获取用户总数、已认证/未认证数、待审核数、今日新增和管理员数量 |

---

## 订单管理

| 方法 | 路径 | 说明 | 描述 |
|------|------|------|------|
| GET | `/admin/api/orders/all?page=1&pageSize=20` | 全部订单 | 分页查询所有订单，含订单信息、接单员信息、地址详情和退款信息 |
| GET | `/admin/api/orders/waiting?page=1&pageSize=20` | 待接单 | 查询待接单订单（status=0），含订单基本信息和发单人地址 |
| GET | `/admin/api/orders/in-progress?page=1&pageSize=20` | 进行中 | 查询进行中订单（status=1/2/3），含配送流程中的订单信息 |
| GET | `/admin/api/orders/completed?page=1&pageSize=20` | 已完成 | 查询已完成订单（status=5/6/7），含已送达和已结算的订单信息 |
| GET | `/admin/api/orders/canceled?page=1&pageSize=20` | 已取消/退款 | 查询已取消或退款订单（status=4/-2/-3/-4），含取消原因和退款结果 |
| GET | `/admin/api/orders/{id}` | 订单详情 | 根据ID查询完整订单，含基本信息、接单员信息、支付记录和退款记录 |
| GET | `/admin/api/orders/statistics` | 订单统计 | 获取今日订单概览，含总订单数、待接单数、进行中数、已完成数和支付金额 |
| POST | `/admin/api/orders/{id}/cancel` | 取消订单 | 管理员手动取消指定订单，需填写取消原因，自动处理退款 |
| POST | `/admin/api/orders/{id}/refund` | 退款 | 对已支付订单执行退款，需填写退款原因，调用微信支付接口 |

---

## 接单管理

| 方法 | 路径 | 说明 | 描述 |
|------|------|------|------|
| GET | `/admin/api/take-orders/all?page=1&pageSize=20` | 全部接单 | 分页查询所有接单记录，含接单员信息、关联订单信息和结算状态 |
| GET | `/admin/api/take-orders/unpaid?page=1&pageSize=20` | 未收款订单 | 查询已完成但未收款的接单记录（任务完成但接单员未提现） |
| GET | `/admin/api/take-orders/statistics` | 接单统计 | 获取接单概览，含接单总数、进行中数、已完成数和未收款数 |

---

## 测试步骤

1. 启动项目，先用登录接口获取 token：

```
POST http://localhost:8080/admin/api/login
Body: {"username":"admin","password":"xxx"}
```

2. 拿返回的 token 设置到 Header，调用上述接口即可。
