# 中后台管理 API 设计文档

日期：2026-05-12

## 概述

在现有 Campus Runner 项目基础上，为中后台管理系统新增 21 个管理端 API，覆盖订单管理、接单管理、用户管理、分类管理四个模块。

## 现有架构

- 框架：Spring Boot 3 + MyBatis + Lombok
- 两套 API：客户端 `/api/**`、管理端 `/admin/api/**`
- 管理端已有：登录/注册、学生认证审核、轮播图管理、校园地址管理、KPI 统计、系统配置

## 新增模块

### 1. 订单管理（9 个接口）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /admin/api/orders/all | 全部订单 |
| GET | /admin/api/orders/waiting | 待接单 (status=0) |
| GET | /admin/api/orders/in-progress | 进行中 (status=1/2/3) |
| GET | /admin/api/orders/completed | 已完成 (status=5/6/7) |
| GET | /admin/api/orders/canceled | 已取消/退款 (status=4/-2/-3/-4) |
| GET | /admin/api/orders/{id} | 订单详情（含接单/支付/退款） |
| GET | /admin/api/orders/statistics | 统计概览 |
| POST | /admin/api/orders/{id}/cancel | 取消订单 |
| POST | /admin/api/orders/{id}/refund | 退款 |

### 2. 接单管理（3 个接口）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /admin/api/take-orders/all | 全部接单 |
| GET | /admin/api/take-orders/unpaid | 未收款订单 |
| GET | /admin/api/take-orders/statistics | 统计概览 |

### 3. 用户管理（5 个接口）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /admin/api/users/all | 全部用户 |
| GET | /admin/api/users/authenticated | 已认证用户 |
| GET | /admin/api/users/pending-review | 待审核用户 |
| GET | /admin/api/users/{id} | 用户详情 |
| GET | /admin/api/users/statistics | 统计概览 |

### 4. 分类管理（4 个接口）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /admin/api/categories | 分类列表 |
| POST | /admin/api/categories | 新增分类 |
| PUT | /admin/api/categories/{id} | 修改分类 |
| DELETE | /admin/api/categories/{id} | 删除分类 |

## 技术方案

### 分层

Controller → Service → Mapper，遵循项目现有分层模式。

### 新增文件

- `controller/admin/AdminOrderController.java`
- `controller/admin/AdminTakeOrderController.java`
- `controller/admin/AdminUserController.java`
- `controller/admin/AdminCategoryController.java`
- `service/admin/AdminOrderService.java` + impl
- `service/admin/AdminTakeOrderService.java` + impl
- `service/admin/AdminUserService.java` + impl
- `service/admin/AdminCategoryService.java` + impl
- 对应的 VO 类（如需要）

### 拦截器

所有 `/admin/api/**` 路径已被 `JwtTokenAdminUserInterceptor` 拦截校验，新增接口自动受保护。

### 分页

列表接口统一使用 PageHelper 或手动 LIMIT 分页，返回 `{total, page, pageSize, list}` 结构。

## 参考

- 详细 API 文档：`docs/api-admin-orders.md`、`docs/api-admin-take-orders.md`、`docs/api-admin-users.md`、`docs/api-admin-categories.md`
- 数据库表结构：`docs/数据库表结构.txt`
- 业务流程：`docs/发单与接单业务流程详解.md`
