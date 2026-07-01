# 帮帮校园送中后台 - 开发指南（整合版）

本指南整合了框架设计、快速开始、API 说明、Token 配置等文档内容，作为后续开发的唯一入口（`新增接口说明.md` 保持独立，随后端更新）。

## 1) 项目概览
- 技术栈：React 19 + TypeScript + Vite 7 + Axios + React Router 7。
- 目标：校园跑腿中后台，涵盖用户、订单、接单、地址、分类、学校等管理模块。
- 代码规范：单引号、无分号、尾逗号、每行 ≤ 100 字符、2 空格缩进；保存自动格式化（Prettier）。
- 环境要求：Node >= 18，npm >= 9。

## 2) 快速开始
1. 安装依赖：`npm install`
2. 配置环境变量：
   - `.env.production` `VITE_API_BASE_URL=https://www.campusrunner.top:443`
   - 开发环境默认走 Vite 代理（`baseURL` 为空，相对路径、`/admin/api`），无需再配置本地地址；如需强制直连自建网关，可在 `.env.development` 设置 `VITE_API_BASE_URL=http://your-host:port`。
3. 启动/构建：`npm run dev` | `npm run build` | `npm run preview`
4. 代码检查：`npm run lint`

### 2.1 常用示例
```typescript
import { userService, orderService, addressService } from '@/services'
import { useRequest } from '@/hooks'

// 服务调用
await userService.login({ username: 'admin', password: 'password' })
const userInfo = await userService.getUserInfo()
const orders = await orderService.getOrdersByTime()
await addressService.createUserAddress({
  schoolId: '1030000000000',
  campusId: '1030100000000',
  buildingId: '1030101000000',
  detailAddress: '301室',
  level: 3,
})

// useRequest 使用
const { data, loading, error, run } = useRequest(orderService.getAllOrders, {
  onSuccess: (res) => console.log('成功', res),
  onError: (err) => console.error('失败', err),
})

// 认证上下文
import { useAuthContext } from '@/contexts/AuthContext'
const { userInfo: info, logout } = useAuthContext()
```

## 3) 配置与鉴权
- API 基础地址、超时、存储键位于 `src/config/config.ts`，默认基址 `https://www.campusrunner.top:443`。
- 真实登录：`POST /admin/api/login`，默认账号 `admin` / 密码 `password`，在 `src/pages/Login/Login.tsx` 已接通接口。
- 登录后写入 `localStorage`（token: `admin_token`, 用户信息：`admin_user_info`），401 会自动清除并跳转 `/login`。

### 3.1 备用：手工获取 token（调试用）
- 如需手工替换 token，可从小程序抓包或接口工具获取 `token`，再用浏览器 `localStorage.setItem('admin_token', token)` 进行调试。

### 3.2 验证登录
- 登录后确认：
  - `localStorage.getItem('admin_token')` 是否存在。
  - Network 请求头是否带 `token`。

## 4) 目录与路由
```
src/
├── layouts/       主布局 MainLayout（侧边栏 + 顶部栏）
├── pages/         页面
│   ├── Login/     静态登录
│   ├── Dashboard/ DashboardNew 已实现
│   ├── AuthManagement/ 审核管理（学生认证审核）
│   ├── SchoolManagement/ 学校管理（地址展开、编辑/删除、添加弹窗）
│   └── ComingSoon/ 占位页
├── router/        路由定义与守卫（PrivateRoute/ PublicRoute）
├── services/      Axios 请求封装与业务服务
├── types/         业务类型定义
├── hooks/         useRequest/usePagination/useAuth 等
├── contexts/      AuthContext
└── utils/         storage/token/validators/format 等
```
- 路由：`src/router/index.tsx`。`/login` 走 `PublicRoute`（已登录则跳转 `/dashboard`），其余走 `PrivateRoute`（无 token 则重定向登录）。
- 菜单：`src/layouts/MainLayout/MainLayout.tsx` 定义，含订单、接单、用户、地址、分类、财务、消息、系统等分组；未实现的页面统一指向 `ComingSoon`。

### 4.1 模块规划与路径（当前状态）
- 数据概览 `/dashboard`：已完成。
- 订单管理 `/orders/*`：全部/待接单/进行中/已完成/统计（占位，待列表与详情、筛选、导出）。
- 接单管理 `/takes/*`：全部/未收款/统计（占位）。
- 用户管理 `/users/*`：全部/已认证/待审核/统计（占位）。
- 审核管理 `/auth`：学生认证审核（已完成：待审核列表 + 审核状态更新）。
- 地址管理 `/address/*`：系统地址/用户地址（占位）、学校管理（新增接口已接入）。
- 分类管理 `/category`：占位。
- 财务 `/finance/*`：概览/收益/提现（占位）。
- 消息 `/message/*`：订阅消息/模板（占位）。
- 系统设置 `/system/*`：配置/管理员/日志（占位）。

### 4.2 待办优先级
- 高：订单列表页、用户列表页、系统地址管理。
- 中：订单/用户详情页、分类管理、统计图表。
- 低：财务模块、消息模块、系统日志、导出功能。

## 5) 服务、类型与工具
- 请求封装：`src/services/request.ts`（自动注入 token、统一错误处理、上传支持）。
- 业务服务（示例方法，完整见各文件）：
  - `user.service.ts`：登录、用户信息、用户列表/详情、状态更新、学校列表。
  - `auth.service.ts`：审核管理（`getPendingList`、`reviewAuth`）。
  - `address.service.ts`：三级地址、用户地址 CRUD；管理端学校/楼宇管理（`getSchools`、`getBuildingsBySchool`、`createPresetAddress`、`updateBuilding`、`deleteBuilding`）。
  - `school.service.ts`：管理员端新增预设地址（`createSchoolAddress`）；其它查询/更新/删除待后端接口补齐。
  - `kpi.service.ts`：数据概览 KPI（`getTotalOrders`、`getPendingOrders`、`getCompletedOrders`、`getAcceptedOrders`、`getTodayOrders`、`getTotalUsers`），后端返回 code=1 视为成功。
  - 其他服务文件（订单、接单、分类等）暂未保留，待后端接口确定后再补全。
- 类型：`src/types/*`（用户/订单/地址/接单/分类、公用 ApiResponse、分页等）。
- 常量：`src/constants/index.ts`（状态标签/颜色、默认值、错误与提示文案）。
- 工具：`src/utils/*`（storage、token 管理、验证器、格式化）。
- Hooks：`useRequest`（请求状态管理）、`usePagination`、`useAuth`（封装 token 与用户信息读写）。
- Context：`AuthContext` 提供登录态与用户信息。

## 6) 页面现状
- `Dashboard/DashboardNew.tsx`：统计卡片使用 KPI 接口（总订单/待接单/已完成/今日订单/总用户/接单总数），失败时兜底为 0，控制台打印错误；含快捷操作与系统信息。
- `AuthManagement/AuthManagement.tsx`：学生认证审核管理，支持待审核列表（`/admin/api/auth/pendingList`）、查看材料（图片 URL 或 dataURL 可预览）、审核状态更新（`/admin/api/auth/review`）；点击“审核中/通过/不通过”使用自定义确认弹窗（非浏览器原生 confirm）。
- `BannerManagement/BannerManagement.tsx`：轮播图管理，支持按学校筛选列表、上传图片并新增、删除；上传目录根据学校名自动生成（通用 `banner/common`，学校 `banner/GuangZhouDaXue`），不暴露目录字段；学校列表未加载时禁用上传；列表缩略图支持预览与下载。
- `SchoolManagement/SchoolManagement.tsx`：管理端地址管理，按学校行展开/收起楼宇列表；支持楼宇编辑/删除（`updateBuilding`/`deleteBuilding`，遇重复名称返回 `msg: REPEAT` 或 `code=0` 时提示重复）；添加地址弹窗支持下拉选择学校或手动输入，成功后仅清空楼宇字段、保持其他字段便于批量录入，页面展开状态保留，重复地址（`msg: REPEAT` 或 `code=0`）提示“地址重复”；搜索框前端过滤学校/校区/类型/楼宇名称。
- 其他菜单页：`ComingSoon` 占位，提供返回操作与即将支持的功能列表。

## 7) 开发指引
- 新页面流程：
  1. 在 `src/pages/` 创建组件。
  2. 在 `src/router/index.tsx` 注册路由。
  3. 在 `MainLayout` 菜单添加入口。
  4. 视需要接入服务层与 hooks。
- 使用 API：直接从 `@/services` 导入对应方法；类型从 `@/types` 获取；常量从 `@/constants` 获取标签/颜色/默认值。
- 鉴权：默认依赖 token；401 会清理并跳转。开发时可在控制台检查 `localStorage.admin_token`。
- UI/组件库：可按需引入（如 Ant Design、MUI、TDesign），但需保持现有路由/状态结构。
- 数据展示建议：表格 + 分页（`usePagination`）、筛选表单、详情抽屉/弹窗；写操作完成后刷新列表或乐观更新。

## 8) API 速览（全量摘要）
- 说明：管理员端接口统一前缀 `/admin/api`；用户侧保持 `/api`。
- 用户
  - `POST /admin/api/login` 管理员登录
  - `GET /api/user` 获取用户信息
  - `PUT /api/user/update` 更新用户信息
  - `PUT /api/user` 用户认证
  - `GET /api/user/paycode` 获取支付码
  - `PUT /api/user/updatePaymentCode` 更新支付码
  - `GET /api/school` 获取学校列表
  - `GET /admin/api/users` 获取所有用户
  - `GET /admin/api/users/:id` 获取用户详情
  - `PUT /admin/api/users/:id/status` 更新用户状态
- 审核（学生认证）
  - `GET /admin/api/auth/pendingList` 获取待审核列表
  - `PUT /admin/api/auth/review` 审核学生认证（0未审核 1审核中 2通过 3不通过）
- 订单
  - `GET /api/order/showByTime/:status` 按时间
  - `GET /api/order/showByPrice/:status` 按价格
  - `GET /api/order/showByCategory/:categoryId` 按分类
  - `GET /api/order/showByPickUpAdd` 按取件地址
  - `GET /api/order/showByReciveAdd` 按收件地址
  - `GET /api/order/showByDoubleAdd` 按双向地址
  - `GET /api/order/my` 我的订单
  - `GET /api/order/detail/:id` 订单详情
  - `POST /api/order` 创建订单
  - `PUT /api/order/:id` 更新订单
  - `DELETE /api/order/:id` 删除订单
  - `PUT /api/order/cancel` 取消订单
  - `PUT /api/order/confirm/:id` 确认订单
  - `GET /admin/api/orders` 后台全部订单
  - `GET /admin/api/orders/statistics` 订单统计
- 接单
  - `GET /api/takeOrders` 我的接单
  - `GET /api/takeOrders/query` 按状态查询
  - `GET /api/takeOrders/:id` 接单详情
  - `POST /api/takeOrders` 接单
  - `POST /api/takeOrders/:id` 通过订单 ID 接单
  - `PUT /api/takeOrders/:id` 更新接单状态
  - `PUT /api/takeOrders/cancel/:id` 取消接单
  - `GET /api/takeOrders/image/:id` 送达图片
  - `GET /api/takeOrders/notWithdrawn` 未收款订单
  - `GET /admin/api/takeOrders` 后台接单列表
  - `GET /admin/api/takeOrders/statistics` 接单统计
- 地址
  - `GET /api/address/three` 三级地址
  - `GET /api/address/show` 获取所有地址
  - `GET /api/address` 获取地址
  - `POST /api/address` 创建地址
  - `PUT /api/address` 更新地址
  - `PUT /api/address/update` 更新地址详情
  - `DELETE /api/address/:id` 删除地址
  - `GET /admin/api/addresses` 后台系统地址
  - `POST /admin/api/addresses` 后台创建地址
- 分类
  - `GET /api/category` 分类列表
  - `GET /api/category/:id` 分类详情
  - `POST /admin/api/category` 创建分类
  - `PUT /admin/api/category/:id` 更新分类
  - `DELETE /admin/api/category/:id` 删除分类
  - `PUT /admin/api/category/sort` 更新分类排序
- 学校
  - `GET /api/school` 学校列表
  - `GET /api/school/:id` 学校详情
  - `POST /admin/api/school` 创建学校
  - `PUT /admin/api/school/:id` 更新学校
  - `DELETE /admin/api/school/:id` 删除学校

- 新增管理员/地址接口（Swagger 导出）请见 `prompt_docs/新增接口说明.md`，保持独立更新。

## 9) 待办与建议
- 接通真实登录、去除静态 token。
- 逐个落地列表页（订单/用户/接单/分类/地址），补齐筛选、分页、详情、导出等。
- 完善学校管理查询/编辑/删除，替换 mock 数据。
- 考虑接入组件库和图表库，丰富数据概览与统计。
- 增加权限体系（角色/路由权限）与操作日志。

—— 本文档作为唯一入口，优先参考；接口增量请继续查看 `prompt_docs/新增接口说明.md`。
