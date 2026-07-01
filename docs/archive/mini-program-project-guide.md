# 校园跑腿小程序 - 项目指南（整合版）

本仓库是「帮帮校园送 / 校园跑腿」微信小程序端代码。本文档用于统一说明项目结构、关键业务流程、接口与状态字段、以及后续改动的约定，方便开发与协作（也方便 AI 助手在不“乱改动关键逻辑”的前提下做增量修改）。

相关接口字段以根目录 `接口说明.md` 为准。

## 1) 项目概览

- 项目形态：微信小程序（WeChat Mini Program）。
- UI 组件库：TDesign 小程序版（`tdesign-miniprogram`）。
- 工程化：无独立构建脚本，依赖微信开发者工具；三方依赖通过 **工具 → 构建 npm** 输出到 `miniprogram_npm/`。
- 分层目标：
  - 视图层（pages/components）尽量只写交互和状态；
  - 网络请求集中在 `services/`；
  - 纯工具与校验集中在 `utils/`。

## 2) 运行与开发

### 2.1 环境要求

- 微信开发者工具（项目 `libVersion` 见 `project.config.json` / `project.private.config.json`）。
- Node.js（用于安装依赖 & 开发者工具构建 npm）。

### 2.2 常用操作

1. 安装依赖：`npm install`
2. 打开微信开发者工具，导入本项目根目录
3. 执行：**工具 → 构建 npm**（生成/更新 `miniprogram_npm/`）
4. 点击：**编译**（模拟器/真机调试）

> 本仓库暂无自动化测试；常用验证方式为模拟器与真机冒烟测试。

## 3) 目录结构

```
app.js                 小程序入口（生命周期、全局 globalData）
app.json               路由、窗口、tabBar 配置
app.wxss               全局样式
pages/                 页面（按业务分目录）
components/            复用组件
services/              网络请求封装与业务服务
utils/                 工具函数、常量、校验、token 管理
images/                静态资源
miniprogram_npm/       DevTools 构建的三方包输出
接口说明.md             Swagger 导出的接口文档（字段权威来源）
```

### 3.1 页面路由与 tabBar

见 `app.json`：

- 首页：`pages/index/index`
- 发布跑腿：`pages/orders/myOrders/ordersAdd/add`
- 我的：`pages/mine/mine/mine`

## 4) 关键业务与状态字段（非常重要）

### 4.1 用户认证字段（以 `接口说明.md` 为准）

`GET /api/user` 返回里与认证相关字段（摘录）：

- `authentication`：最终认证开关（仅 `0/1`）
  - `0` 未完成认证（不能发单/接单）
  - `1` 已完成认证（可发单/接单）
- `studentIdCardReview`：学生证审核状态（`0/1/2/3`）
  - `0` 未审核（通常表示未提交或等待后台处理的初始态）
  - `1` 审核中
  - `2` 审核通过
  - `3` 审核不通过

重要原则：

- **发单/接单门槛**仍以 `authentication` 为准（不要把它替换成 2/3 等“审核态”）。
- **学生证材料审核提示与交互**使用 `studentIdCardReview`（0/1/2/3）。

### 4.2 认证相关页面说明

- `pages/mine/identify/identify`
  - 顶部 tag：显示 `authentication`（已认证/未认证）。
  - 页面内的“学生证审核提示条”：显示 `studentIdCardReview`（未审核/审核中/审核通过/未通过）。
  - 图片：使用时间戳参数避免微信图片缓存导致“换图不生效”。
  - 未通过（`studentIdCardReview==3`）场景：
    - 允许在本页直接修改学校/学号/姓名并重新提交材料（常见原因：信息与材料不匹配）。
    - 底部按钮文案为“重新审核”，点击后会二次确认（避免误触直接提交）。
    - 支持“可复用当前材料，也可以重新上传”作为重新上传入口。
  - 审核中/审核通过（`studentIdCardReview==1/2`）场景：
    - 仅展示材料与状态，不允许重复提交或修改。
- `pages/mine/reIdentify/reIdentify`
  - 重新提交材料与信息。
  - 审核中（`studentIdCardReview==1`）时，禁止重复提交。
  - 定位：用于“更改认证信息”（例如账号认证信息与实际不符时的更正流程）。

> 注意：接口 `PUT /api/user`（用户认证）请求体仍是 `{ schoolId, realname, stuId, studentIdCard }`，审核流转由后端处理。

## 5) 网络请求与 Token 机制

### 5.1 Token 管理

- `utils/tokenManager.js`
  - `initTokenSync()`：`app.js` 启动时从本地缓存同步初始化 token
  - `waitForToken()`：页面/组件在需要 token 时等待“就绪”
  - `updateToken()`：更新全局与缓存

### 5.2 统一请求封装

- `services/request.js`
  - 自动注入 `token` header
  - 遇到 `401` 会走 `wx.login` 刷新 token 并重试请求

建议：

- 页面层优先使用 `services/*` 方法，而不是直接 `wx.request`。
- 仅微信特定能力（如 `wx.requestMerchantTransfer`）保留页面内直连逻辑。

## 6) 主要模块与代码位置

### 6.1 首页（订单列表 + 筛选）

- `pages/index/index`
  - tabs：综合排序 / 价格排序 / 快捷筛选
  - 服务层：`services/orderService.js`、`services/addressService.js`
  - 辅助：`utils/constants.js`、`utils/transformers.js`

### 6.2 发布订单

- `pages/orders/myOrders/ordersAdd/add`
  - 分步骤表单、草稿保存：`utils/orderDraftManager.js`
  - 校验：`utils/validators.js`
  - 服务：`services/userOrderService.js`

### 6.3 我的与登录

- `pages/mine/mine/mine`
  - 登录：微信授权 + `POST /api/user/login`
  - 用户信息：`services/userService.js`

## 7) 代码风格与约定

- 缩进：2 空格。
- JS：以 CommonJS `require` 为主；TDesign 组件调用允许 `import`（现状混用，新增代码尽量与所在文件保持一致）。
- 样式：优先使用 TDesign 组件样式能力；自定义 WXSS 采用 `kebab-case`。
- 不要硬编码敏感信息；token 走 `tokenManager` + storage。

## 8) 常见坑与调试建议

- 图片缓存：用户提交/更新学生证材料后，小程序可能继续显示旧图；建议给图片 URL 拼接 `?t=timestamp` 强制刷新（本项目已采用）。
- 学校名回显依赖学校列表：先拉 `GET /api/school`（或使用缓存）再做 `schoolId -> schoolName` 映射，避免 `undefined` 报错。
- 不要误改 `authentication` 的语义：它是业务门槛开关（0/1），不是审核状态机。

## 9) 给 AI 助手/协作者的工作提示（重要）

进行任何“认证/审核/发单接单门槛”的修改时：

1. 先查 `接口说明.md` 中字段定义，避免混淆 `authentication` 与 `studentIdCardReview`。
2. 改动尽量限定在 `pages/mine/identify`、`pages/mine/reIdentify`、以及明确的门槛判断处。
3. 任何涉及全局门槛（是否能发单/接单）的修改，必须先和维护者确认再动。

## 10) 登录与协议方案（固定实现说明）

设计与实现（保持单 token / 20h 过期不变）：

- `tokenManager` 维护 `tokenUpdatedAt`，用于 token 过期判断；启动阶段使用安全的 `getApp` 获取，避免 `getApp().globalData` 未就绪报错。
- 启动时若本地无 token，`app.js` 触发静默登录（`wx.login` → `/api/user/login`），并把 Promise 暴露到 `globalData.silentLoginPromise` 供页面等待。
- 首页登录态判断统一走 `userService.getUserInfo()`，让 401 自动刷新生效；并加入启动检测加载态避免“未登录”闪烁。
- 用户协议弹层与登录解耦，依赖 `privacyAcknowledged` 本地标记，仅提示一次且不再清空 `userInfo`。
- 新用户判断不再清空 token（`createTime === updateTime` 仅用于引导完善信息/注册流程）。

验证要点：

- 关闭微信后台 → 重新进入小程序应保持登录态（或静默登录后自动恢复）。
- token 过期 → 自动刷新；刷新失败才需手动登录。
- 用户协议弹层只出现一次（除非清缓存）。
- 重新编译后不应再出现 `getApp().globalData` 为空的启动报错。
