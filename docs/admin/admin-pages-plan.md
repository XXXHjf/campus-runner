# Admin Pages Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace 13 ComingSoon placeholder pages with real antd-based admin pages across 4 modules (Category, User, Order, TakeOrder), backed by the newly built admin APIs.

**Architecture:** Single-page-per-module with internal Tab switching via URL path. Ant Design provides Table/Form/Modal/Message components. Backend gets a `PageResult` wrapper to return `total` for paginated endpoints. Two new service files + one extended service.

**Tech Stack:** React 19, TypeScript, Ant Design 5, Axios

**Prerequisite — Backend:** Before frontend work, add `total` to all paginated API responses so antd Table pagination works. The current `wrap()` in controllers only returns `list`/`page`/`pageSize` without `total`.

---

### Task 1: Add PageResult to backend + update all paginated endpoints

**Files:**
- Create: `src/main/java/com/mikasa/campusrunner/pojo/dto/PageResult.java`
- Modify: `src/main/java/com/mikasa/campusrunner/controller/admin/AdminOrderController.java`
- Modify: `src/main/java/com/mikasa/campusrunner/controller/admin/AdminUserController.java`
- Modify: `src/main/java/com/mikasa/campusrunner/controller/admin/AdminTakeOrderController.java`
- Modify: `src/main/java/com/mikasa/campusrunner/service/impl/admin/AdminOrderServiceImpl.java`
- Modify: `src/main/java/com/mikasa/campusrunner/service/impl/admin/AdminUserServiceImpl.java`
- Modify: `src/main/java/com/mikasa/campusrunner/service/impl/admin/AdminTakeOrderServiceImpl.java`
- Modify: `src/main/java/com/mikasa/campusrunner/service/admin/AdminOrderService.java`
- Modify: `src/main/java/com/mikasa/campusrunner/service/admin/AdminUserService.java`
- Modify: `src/main/java/com/mikasa/campusrunner/service/admin/AdminTakeOrderService.java`

- [ ] **1.1 Create PageResult.java**

```java
package com.mikasa.campusrunner.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class PageResult<T> {
    private long total;
    private int page;
    private int pageSize;
    private List<T> list;
}
```

- [ ] **1.2 Update AdminOrderService interface** — change list method return types from `List<AdminOrderListVO>` to `PageResult<AdminOrderListVO>`

- [ ] **1.3 Update AdminOrderServiceImpl** — add count queries and return PageResult

- [ ] **1.4 Update AdminOrderController** — remove `wrap()` method, use PageResult directly

- [ ] **1.5 Repeat for AdminUserService + AdminUserServiceImpl + AdminUserController**

- [ ] **1.6 Repeat for AdminTakeOrderService + AdminTakeOrderServiceImpl + AdminTakeOrderController**

- [ ] **1.7 Compile and verify**

Run: `cd /c/03_Work/Projects/campus-runner-mini-program-service && mvn compile -q`
Expected: BUILD SUCCESS (exit 0, no errors)

---

### Task 2: Install antd + create frontend types and services

**Files:**
- Create: `src/types/admin.ts`
- Create: `src/services/order.service.ts`
- Create: `src/services/takeOrder.service.ts`
- Modify: `src/types/order.ts`
- Modify: `src/types/user.ts`
- Modify: `src/types/takeOrder.ts`
- Modify: `src/services/user.service.ts`
- Modify: `src/services/index.ts`

- [ ] **2.1 Install antd**

Run from `/c/03_Work/Projects/campus-runner-admin`:
```bash
npm install antd
```
Expected: added antd 5.x to node_modules

- [ ] **2.2 Create `src/types/admin.ts`** 

Admin-specific response types for paginated list and statistics:

```typescript
// Generic paginated response
export interface PageResponse<T> {
  total: number
  page: number
  pageSize: number
  list: T[]
}

// Order management - admin list item
export interface AdminOrderItem {
  id: number
  orderNumber: string
  status: number
  categoryName: string
  price: number
  serviceFee: number
  payAmount: number
  username: string
  phone: string
  pickUpAddress: string
  reciveAddress: string
  note: string
  doorAccess: number
  createTime: string
  // in-progress / completed fields
  takerName?: string
  takerPhone?: string
  takeOrderTime?: string
  deliveryTime?: string
  completeTime?: string
  withdrawalStatus?: number | null
  // canceled fields
  cancelTime?: string
  cancelReason?: string
  refundAmount?: number
  refundStatus?: string
}

export interface AdminOrderDetailResponse {
  order: {
    id: number
    orderNumber: string
    status: number
    categoryName: string
    price: number
    serviceFeeRate: number
    serviceFee: number
    payAmount: number
    username: string
    phone: string
    pickUpAddress: string
    reciveAddress: string
    note: string
    image: string | null
    doorAccess: number
    gap: number
    exceedTime: string
    createTime: string
    cancelTime?: string
    cancelReason?: string
  }
  taker: {
    userId: number
    realname: string
    phone: string
    takeTime: string
    deliveryTime: string | null
    image: string | null
  } | null
  payment: {
    transactionId: string
    tradeState: string
    total: number
    serviceFee: number
    payerOpenid: string
    successTime: string
  } | null
  refund: {
    refundNumber: string
    refundId: string
    refundAmount: number
    refundStatus: string
    reason: string
    createTime: string
  } | null
}

export interface AdminOrderStatistics {
  totalCount: number
  waitingCount: number
  inProgressCount: number
  completedCount: number
  canceledCount: number
  todayNewCount: number
  todayTotalAmount: number
  todayServiceFee: number
}

// User management - admin list item
export interface AdminUserItem {
  id: number
  username: string
  realname: string
  headImg: string
  sex: number
  phone: string
  schoolName: string
  stuId: string
  authentication: number
  studentIdCardReview: number
  score: number
  orderCount: number
  takeOrderCount: number
  createTime: string
  studentIdCard?: string // only in pending review
}

export interface AdminUserDetail {
  id: number
  username: string
  realname: string
  headImg: string
  sex: number
  phone: string
  schoolName: string
  stuId: string
  authentication: number
  studentIdCard: string
  studentIdCardReview: number
  score: number
  alipayPaymentCode: string
  weChatPaymentCode: string
  isManager: number
  orderCount: number
  takeOrderCount: number
  totalEarned: number
  createTime: string
  updateTime: string
}

export interface AdminUserStatistics {
  totalCount: number
  authenticatedCount: number
  unauthenticatedCount: number
  pendingReviewCount: number
  todayNewCount: number
  managerCount: number
}

// Take order management - admin list item
export interface AdminTakeOrderItem {
  id: number
  orderId: number
  orderNumber: string
  orderStatus: number
  takeOrderStatus: number
  categoryName: string
  price: number
  orderNote: string
  pickUpAddress: string
  reciveAddress: string
  publisherName: string
  publisherPhone: string
  takerName: string
  takerPhone: string
  takeOrderTime: string
  deliveryTime: string
  takeOrderImage: string
  // unpaid fields
  serviceFee?: number
  payAmount?: number
  completeTime?: string
  withdrawalStatus?: number | null
}

export interface AdminTakeOrderStatistics {
  totalCount: number
  todayNewCount: number
  unpaidCount: number
  unpaidTotalAmount: number
  todayCompletedCount: number
  todayCompletedAmount: number
}

// Category
export interface AdminCategory {
  id: number
  categoryName: string
  image?: string
}
```

- [ ] **2.3 Create `src/services/order.service.ts`**

```typescript
import { get, post, del } from './request'
import type { PageResponse, AdminOrderItem, AdminOrderDetailResponse, AdminOrderStatistics } from '../types/admin'

export async function listAll(page: number, pageSize: number): Promise<PageResponse<AdminOrderItem>> {
  return get<PageResponse<AdminOrderItem>>('/admin/api/orders/all', { page, pageSize })
}

export async function listWaiting(page: number, pageSize: number): Promise<PageResponse<AdminOrderItem>> {
  return get<PageResponse<AdminOrderItem>>('/admin/api/orders/waiting', { page, pageSize })
}

export async function listInProgress(page: number, pageSize: number): Promise<PageResponse<AdminOrderItem>> {
  return get<PageResponse<AdminOrderItem>>('/admin/api/orders/in-progress', { page, pageSize })
}

export async function listCompleted(page: number, pageSize: number): Promise<PageResponse<AdminOrderItem>> {
  return get<PageResponse<AdminOrderItem>>('/admin/api/orders/completed', { page, pageSize })
}

export async function listCanceled(page: number, pageSize: number): Promise<PageResponse<AdminOrderItem>> {
  return get<PageResponse<AdminOrderItem>>('/admin/api/orders/canceled', { page, pageSize })
}

export async function detail(id: number): Promise<AdminOrderDetailResponse> {
  return get<AdminOrderDetailResponse>(`/admin/api/orders/${id}`)
}

export async function statistics(): Promise<AdminOrderStatistics> {
  return get<AdminOrderStatistics>('/admin/api/orders/statistics')
}

export async function cancelOrder(id: number, reason: string): Promise<void> {
  return post<void>(`/admin/api/orders/${id}/cancel`, { reason })
}

export async function refundOrder(id: number, reason: string): Promise<void> {
  return post<void>(`/admin/api/orders/${id}/refund`, { reason })
}
```

- [ ] **2.4 Create `src/services/takeOrder.service.ts`**

```typescript
import { get } from './request'
import type { PageResponse, AdminTakeOrderItem, AdminTakeOrderStatistics } from '../types/admin'

export async function listAll(page: number, pageSize: number): Promise<PageResponse<AdminTakeOrderItem>> {
  return get<PageResponse<AdminTakeOrderItem>>('/admin/api/take-orders/all', { page, pageSize })
}

export async function listUnpaid(page: number, pageSize: number): Promise<PageResponse<AdminTakeOrderItem>> {
  return get<PageResponse<AdminTakeOrderItem>>('/admin/api/take-orders/unpaid', { page, pageSize })
}

export async function statistics(): Promise<AdminTakeOrderStatistics> {
  return get<AdminTakeOrderStatistics>('/admin/api/take-orders/statistics')
}
```

- [ ] **2.5 Update `src/services/user.service.ts`** — add admin user management APIs:

```typescript
// Append these to existing user.service.ts:
import { get } from './request'
import type { PageResponse, AdminUserItem, AdminUserDetail, AdminUserStatistics } from '../types/admin'

export async function listAllUsers(page: number, pageSize: number): Promise<PageResponse<AdminUserItem>> {
  return get<PageResponse<AdminUserItem>>('/admin/api/users/all', { page, pageSize })
}

export async function listAuthenticated(page: number, pageSize: number): Promise<PageResponse<AdminUserItem>> {
  return get<PageResponse<AdminUserItem>>('/admin/api/users/authenticated', { page, pageSize })
}

export async function listPendingReview(page: number, pageSize: number): Promise<PageResponse<AdminUserItem>> {
  return get<PageResponse<AdminUserItem>>('/admin/api/users/pending-review', { page, pageSize })
}

export async function userDetail(id: number): Promise<AdminUserDetail> {
  return get<AdminUserDetail>(`/admin/api/users/${id}`)
}

export async function userStatistics(): Promise<AdminUserStatistics> {
  return get<AdminUserStatistics>('/admin/api/users/statistics')
}
```

- [ ] **2.6 Update `src/services/index.ts`** — register new services:

```typescript
export * as orderService from './order.service'
export * as takeOrderService from './takeOrder.service'
```

- [ ] **2.7 Verify the dev server starts**

Run: `cd /c/03_Work/Projects/campus-runner-admin && npx tsc --noEmit`
Expected: TypeScript compilation passes (no errors)

---

### Task 3: Category Management Page

**Files:**
- Create: `src/pages/CategoryManagement/CategoryManagement.tsx`
- Create: `src/pages/CategoryManagement/CategoryManagement.css`

- [ ] **3.1 Create the page component** — full CRUD with antd Table + Modal Form:

Key features:
- Table listing all categories (ID, name, icon thumbnail, actions)
- "Add" button opens Modal with Form (categoryName required, image optional)
- "Edit" action in each row opens same Modal pre-filled
- "Delete" uses Popconfirm
- After success, refresh the list

- [ ] **3.2 Verify: no TS errors**

Run: `cd /c/03_Work/Projects/campus-runner-admin && npx tsc --noEmit`
Expected: no errors

---

### Task 4: User Management Page

**Files:**
- Create: `src/pages/UserManagement/UserManagement.tsx`
- Create: `src/pages/UserManagement/UserManagement.css`

- [ ] **4.1 Create UserManagement.tsx** — Tab-based page:

Tabs (driven by URL path):
- `users/all` → "全部用户" tab
- `users/authenticated` → "已认证" tab
- `users/pending-auth` → "待审核" tab
- `users/stats` → "用户统计" tab

List tabs show: antd Table with avatar, username, realname, phone, school, auth status (Tag), orderCount/takeOrderCount, createTime. Search by name/phone. Click row to open Drawer with full detail (payment QR codes, totalEarned, etc.).

Stats tab shows: antd Statistic + Card grid with total/authenticated/unauthenticated/pendingReview/todayNew/managerCount.

- [ ] **4.2 Verify: no TS errors**

---

### Task 5: Order Management Page

**Files:**
- Create: `src/pages/OrderManagement/OrderManagement.tsx`
- Create: `src/pages/OrderManagement/OrderDetail.tsx` (detail modal)
- Create: `src/pages/OrderManagement/OrderManagement.css`

- [ ] **5.1 Create OrderManagement.tsx** — Tab-based page:

Tabs: 全部 / 待接单 / 进行中 / 已完成 / 已取消退款

Table columns: orderNumber, status (Tag), categoryName, price, serviceFee, payAmount, username, phone, pickUpAddress, reciveAddress, createTime.

Actions per row:
- "详情" button → opens OrderDetail modal
- "取消订单" (only for pending/unpaid status, with reason textarea in antd Modal)
- "退款" (only for paid status)

Search by keyword across orderNumber, username, phone.

Stats tab: antd Card + Statistic grid showing totalCount, waitingCount, inProgressCount, completedCount, canceledCount, todayNewCount, todayTotalAmount, todayServiceFee.

- [ ] **5.2 Create OrderDetail.tsx** — antd Descriptions-based detail view:

Shows order info, taker info, payment info, refund info in separate sections. Handles null cases (no taker, no payment, no refund).

- [ ] **5.3 Verify: no TS errors**

---

### Task 6: Take Order Management Page

**Files:**
- Create: `src/pages/TakeOrderManagement/TakeOrderManagement.tsx`
- Create: `src/pages/TakeOrderManagement/TakeOrderManagement.css`

- [ ] **6.1 Create TakeOrderManagement.tsx** — Tab-based page:

Tabs: 全部接单 / 未收款订单 / 接单统计

Table columns: id, orderNumber, categoryName, price, publisherName, takerName, takerPhone, takeOrderTime, deliveryTime, orderNote.

Actions: "查看订单" link (navigate to orders/detail).

Stats tab: Card + Statistic grid showing totalCount, todayNewCount, unpaidCount, unpaidTotalAmount, todayCompletedCount, todayCompletedAmount.

- [ ] **6.2 Verify: no TS errors**

---

### Task 7: Update Router and MainLayout

**Files:**
- Modify: `src/router/index.tsx`

- [ ] **7.1 Update router** — Replace all ComingSoon imports with real pages, keep all sub-routes pointing to the same component:

```tsx
import CategoryManagement from '../pages/CategoryManagement/CategoryManagement'
import UserManagement from '../pages/UserManagement/UserManagement'
import OrderManagement from '../pages/OrderManagement/OrderManagement'
import TakeOrderManagement from '../pages/TakeOrderManagement/TakeOrderManagement'

// In router config:
{
  path: 'category',
  element: <CategoryManagement />,
},
{
  path: 'users/all',
  element: <UserManagement />,
},
{
  path: 'users/authenticated',
  element: <UserManagement />,
},
{
  path: 'users/pending-auth',
  element: <UserManagement />,
},
{
  path: 'users/stats',
  element: <UserManagement />,
},
// ... same for orders/* and takes/*
```

- [ ] **7.2 Verify full build**

Run: `cd /c/03_Work/Projects/campus-runner-admin && npx tsc --noEmit`
Expected: no errors
