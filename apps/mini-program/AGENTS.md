# Repository Guidelines

## 项目结构与模块组织
- 入口与全局配置：`app.js`（生命周期与全局方法）、`app.json`（路由/窗口配置）、`app.wxss`（全局样式）。
- 功能页面：`pages/` 下按功能分目录（如 `pages/index`、`pages/orders`、`pages/mine`），每个目录包含 `.js/.wxml/.wxss/.json`。
- 复用模块：通用组件在 `components/`，工具与通用逻辑在 `utils/`，接口与网络调用集中在 `services/`。
- 资源与依赖：静态资源在 `images/`；`miniprogram_npm/` 存放通过 DevTools 构建的三方包；环境/IDE 配置在 `project.config.json` 与 `project.private.config.json`。

## 构建、测试与开发命令
- 安装依赖：`npm install`（保持 `tdesign-miniprogram` 与锁文件一致）。
- 构建 npm：微信开发者工具中执行 **工具 → 构建 npm**，更新 `miniprogram_npm/`。
- 运行/调试：用微信开发者工具打开项目，点击 **编译**；需要真机验证时开启真机调试。
- `package.json` 未定义额外脚本，预览、上传均使用开发者工具面板。

## 编码风格与命名
- 统一 2 空格缩进，文件保持 UTF-8/ASCII。
- JavaScript 使用 CommonJS（`require`）；变量与函数用 `camelCase`，组件名用 `PascalCase`。
- 工具函数尽量保持纯净并放在 `utils/`；网络请求与接口封装集中在 `services/`，避免视图层掺杂请求。
- WXSS 遵循 TDesign 类名（如 `t-` 前缀），自定义样式使用 `kebab-case`。

## 测试指引
- 暂无自动化测试；依赖微信开发者工具模拟器和真机冒烟测试。
- 提交前检查：登录/令牌流程（`tokenManager` 在启动初始化）、页面跳转、`globalData.API_URL` 下的网络请求是否正常。
- 新增页面或组件时，确认常见分辨率布局正常，控制台无运行告警。

## 提交与 PR 规范
- 提交信息简洁、祈使式；历史记录常用简短中文摘要（如“修复…”、“新增…”）。
- PR 需说明目的与范围，附关键 UI 截图/GIF，列出受影响页面及新增配置项。
- 如有相关 issue/任务请关联，并标明环境变更（新的存储键、接口地址等）。

## 安全与配置提示
- 不要硬编码令牌；使用 `tokenManager` 与 `wx.setStorage` / `wx.getStorage` 管理认证状态。
- API 主机放在 `globalData`，敏感配置写入 `project.private.config.json`，避免提交密钥。
- 调整依赖后务必重新执行 **构建 npm**，确保上线代码与锁文件一致。
