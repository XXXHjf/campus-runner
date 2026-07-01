# Campus Runner

Campus Runner 是一个校园跑腿系统 monorepo，包含用户端微信小程序、管理后台和后端 API。

## 项目结构

```text
campus-runner/
  apps/
    mini-program/  # 微信小程序用户端
    admin/         # React + Vite 管理后台
    api/           # Spring Boot 后端服务
  docs/            # 统一项目文档
```

## 本地启动

### 后端 API

```bash
cd apps/api
./mvnw spring-boot:run
```

默认端口：`8080`。

后端配置文件位于 `apps/api/src/main/resources/application.yaml`，微信支付私钥位于 `apps/api/src/main/resources/apiclient_key.pem`。这两个文件包含本地/生产敏感配置，已被 `.gitignore` 忽略，不提交到 Git。打包部署时，确认这两个文件在 `src/main/resources` 下，再执行 Maven 打包即可。

### 管理后台

```bash
cd apps/admin
npm install
npm run dev
```

可通过 `VITE_API_BASE_URL` 指定后端地址。开发环境为空时走 Vite 代理或相对路径。

### 微信小程序

用微信开发者工具打开 `apps/mini-program`。

依赖构建：

```bash
cd apps/mini-program
npm install
```

然后在微信开发者工具中执行“工具 -> 构建 npm”。

## 文档

文档已集中到 [docs](docs/README.md)。

## 仓库说明

原来拆分的三个项目已合并到一个仓库目录中：

- `campus-runner-mini-program` -> `apps/mini-program`
- `campus-runner-admin` -> `apps/admin`
- `campus-runner-mini-program-service` -> `apps/api`

旧项目中的 `.git`、`node_modules`、`dist`、`target`、IDE 配置和本地工具状态没有迁入新仓库。
