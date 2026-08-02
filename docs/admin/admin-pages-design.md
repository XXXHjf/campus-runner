# 管理后台页面补充 — 实现设计

> **目标：** 将 4 个模块的 ComingSoon 占位页替换为真实数据页面，对接后端新编写的 21 个管理端 API。

**架构：**
- 使用 Ant Design 组件库加速开发
- 每个模块一个页面组件，内部 Tab 切换不同数据视图
- Axios 请求层复用现有 `request.ts` 封装
- 新建 2 个 service 文件，扩展 1 个现有 service

**涉及模块：** 分类管理、用户管理、订单管理、接单管理

---

## 文件结构

```
src/
  pages/
    CategoryManagement/
      CategoryManagement.tsx    # 分类 CRUD 列表页
      CategoryManagement.css
    UserManagement/
      UserManagement.tsx        # Tab：全部/已认证/待审核/统计
      UserManagement.css
    OrderManagement/
      OrderManagement.tsx       # Tab：全部/待接单/进行中/已完成
      OrderDetail.tsx           # Modal 内嵌详情
      OrderManagement.css
    TakeOrderManagement/
      TakeOrderManagement.tsx   # Tab：全部/未收款/统计
      TakeOrderManagement.css
  services/
    order.service.ts            # 新增：订单管理 API
    takeOrder.service.ts        # 新增：接单管理 API
    user.service.ts             # 追加：用户管理 API
    index.ts                    # 注册新服务
  types/
    order.ts                    # 追加 Admin 相关类型
    user.ts                     # 追加 Admin 相关类型
    takeOrder.ts                # 追加 Admin 相关类型
  router/
    index.tsx                   # 更新路由到真实组件
```

## 页面设计

### 分类管理 (`/category`)
- **Table 列表**：ID、名称、图标（图片缩略图）、操作
- **操作**：新增（Modal 表单）、编辑（Modal 表单）、删除（确认弹窗）
- API：`GET/POST/PUT/DELETE /admin/api/categories`

### 用户管理 (`/users/*`)
- **Tab 切换**：全部用户 | 已认证 | 待审核 | 用户统计
- **列表**：Table 展示头像、昵称、姓名、手机号、学校、认证状态、发单/接单数、注册时间
- **行操作**：查看详情（Drawer/Modal 展示用户资料、认证状态和业务统计）
- **统计 Tab**：StatisticCard 展示总数/认证/未认证/待审核/今日新增/管理员
- API：`GET /admin/api/users/*`

### 订单管理 (`/orders/*`)
- **Tab 切换**：全部 | 待接单 | 进行中 | 已完成 | 已取消/退款
- **列表**：Table 展示订单号、分类、金额、状态、发单人、地址、时间
- **行操作**：查看详情、取消订单（待接单状态）、退款（已支付状态）
- **统计 Tab**：StatisticCard + 今日概览
- API：`GET /admin/api/orders/*`

### 接单管理 (`/takes/*`)
- **Tab 切换**：全部接单 | 未收款订单 | 接单统计
- **列表**：Table 展示接单员、订单信息、状态、时间
- **统计 Tab**：接单总数/进行中/未收款/金额
- API：`GET /admin/api/take-orders/*`

## 路由变更

所有路由路径不变，只将 `element: <ComingSoon />` 替换为具体组件：

```tsx
path: 'category',        element: <CategoryManagement />
path: 'users/all',        element: <UserManagement />
path: 'orders/all',       element: <OrderManagement />
path: 'takes/all',        element: <TakeOrderManagement />
```

各子路径（`users/authenticated`, `orders/pending` 等）也统一指向同一组件组件内部通过 `location.pathname` 区分当前 Tab。
