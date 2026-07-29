# 生产服务器部署与更新

本文记录 Campus Runner 当前生产服务器的部署结构、日常更新、验证和回滚流程。

## 当前部署结构

| 项目 | 当前配置 |
| --- | --- |
| 公网 IP | `116.62.135.75` |
| 域名 | `www.campusrunner.top`、`campusrunner.top` |
| HTTPS 入口 | Nginx，监听 `80` 和 `443` |
| 后端服务 | Spring Boot JAR，监听本机 `8080` |
| 管理后台 | React 静态文件，由 Nginx 提供 |
| 后端目录 | `/root` |
| 后端 JAR | `/root/campus-runner-0.0.1-SNAPSHOT.jar` |
| 后端日志 | `/root/nohup.out` |
| 管理后台目录 | `/var/www/campusrunner/admin/` |
| Nginx 配置 | `/etc/nginx/conf.d/campusrunner.conf` |
| SSL 证书 | `/etc/nginx/ssl/campusrunner/cert.pem` |
| SSL 私钥 | `/etc/nginx/ssl/campusrunner/cert.key` |

Nginx 将 `/api/` 和 `/admin/api/` 请求转发至
`http://127.0.0.1:8080`，其他请求由管理后台单页应用处理。

## 发布前检查

后端使用 Java 21：

```bash
java -version
```

### 生产发布必检清单

每次发布 API 或管理后台前逐项核对。任何一项未确认时，不应继续覆盖生产版本。

- [ ] 已确认本次发布范围：API、admin、数据库迁移以及 Nginx 是否有变化。
- [ ] 已备份将被替换的 JAR、admin 静态文件和受影响的数据库。
- [ ] API 使用生产 MySQL 地址、数据库名、用户名和密码。生产密码与本地开发密码
      `admin` 不同，必须通过服务器 `/root/campus-runner.env` 中的 `DB_PASSWORD`
      注入；真实密码不得写入本文档或 Git。
- [ ] `server.port` 为 `8080`，`server.ssl.enabled` 为 `false`。
- [ ] 生产模拟支付已关闭，微信支付商户号、回调地址和私钥均为生产配置。
- [ ] `application.yaml` 和 `apiclient_key.pem` 存在，并确认它们将被打入本次 JAR。
- [ ] 本次代码要求的数据库迁移已在备份后执行。
- [ ] admin 的 `VITE_API_BASE_URL` 符合当前部署拓扑；当前同域部署应保持为空。
- [ ] Nginx 仍将 `/api/` 和 `/admin/api/` 转发到 `http://127.0.0.1:8080`。
- [ ] 已准备发布后验证命令和可用的回滚版本。

注意：`apps/api/src/main/resources/application.yaml` 被 Git 忽略，但 Maven 仍会将其
复制到 JAR。`git status` 看不到该文件的变化，不能据此判断生产配置已经正确。

本地打包前确认以下文件存在：

```text
apps/api/src/main/resources/application.yaml
apps/api/src/main/resources/apiclient_key.pem
```

这两个文件包含生产配置和支付私钥，已被 Git 忽略，不会随代码仓库上传。
打包前还应确认生产配置满足：

```yaml
server:
  port: 8080
  ssl:
    enabled: false

com:
  mikasa:
    campus-runner:
      dev:
        mock-payment-enabled: false
```

数据库配置必须使用生产凭据，不能沿用本地开发配置。当前 YAML 使用
`${DB_PASSWORD:admin}`：本地未设置环境变量时默认使用 `admin`，生产服务器必须在
`/root/campus-runner.env` 中设置 `DB_PASSWORD`。该文件权限必须为 `600`。日志和
检查输出只能显示“已配置”，不能回显密码。

## 2026-07-24 API 登录超时事故复盘

### 现象

- admin 页面可以正常加载，但登录在 15 秒后提示请求超时。
- Nginx 和 Java 进程看似存在，API 却没有稳定监听 `8080`。
- 直接访问不需要数据库的链路仍可能快速返回，因此不能仅凭“进程存在”判断后端健康。

### 根因

1. 本地 `application.yaml` 使用开发数据库密码，并且该文件虽然被 Git 忽略，仍会被
   Maven 打入 JAR。重新打包时，本地凭据被带进了生产版本，MySQL 返回 `1045 Access denied`。
2. 手工将密码写成 `password: [value]` 后，YAML 把方括号内容解释成列表，而不是可靠的
   字符串配置。生产秘密不应继续通过手改 YAML 维护。
3. 首次改为安全启动脚本后，非登录 shell 没有开发者登录时的 Java PATH，导致
   `nohup: failed to run command 'java'`。启动脚本必须使用已验证的 Java 绝对路径。

### 长期修复

- 数据库密码通过 `/root/campus-runner.env` 的 `DB_PASSWORD` 注入，文件权限为 `600`；
  JAR 内只保留 `${DB_PASSWORD:admin}`，其中 `admin` 仅作为本地开发默认值。
- 生产启动统一使用 `apps/api/deploy/` 中的脚本，显式指定 Java 21 路径、HTTP `8080`、
 关闭 Spring SSL、关闭模拟支付并应用生产日志覆盖项。
- 登录日志不再打印 DTO、明文密码或 JWT；生产 MyBatis mapper 日志不使用 DEBUG。
- 定时提现查询只有存在本地转账记录时才请求微信，避免历史订单持续产生无效 404。

### 快速判断方法

按以下顺序检查，可以区分“前端问题”和“后端未真正启动”：

```bash
ps -ef | grep campus-runner-0.0.1-SNAPSHOT.jar | grep -v grep
ss -lntp | grep ':8080'
tail -n 200 /root/nohup.out
```

重点搜索：

```bash
grep -E 'Access denied|errorCode 1045|failed to run command|APPLICATION FAILED' \
  /root/nohup.out
```

只有进程、`8080`、登录数据库查询和 HTTPS 反向代理均通过，才能认为发布成功。

## 更新后端

### 1. 本地打包

在项目根目录执行：

```powershell
cd apps/api
mvn clean package -DskipTests
```

生成文件：

```text
apps/api/target/campus-runner-0.0.1-SNAPSHOT.jar
```

### 2. 上传新 JAR

先将新版本上传为临时文件，避免覆盖正在运行的 JAR：

```powershell
scp apps/api/target/campus-runner-0.0.1-SNAPSHOT.jar `
  root@116.62.135.75:/root/campus-runner-0.0.1-SNAPSHOT.jar.new
```

### 3. 备份并替换

登录服务器：

```bash
ssh root@116.62.135.75
cd /root
```

确认上传结果：

```bash
ls -lh campus-runner-0.0.1-SNAPSHOT.jar*
```

备份旧版本并替换：

```bash
cp -p campus-runner-0.0.1-SNAPSHOT.jar \
  campus-runner-0.0.1-SNAPSHOT.jar.bak-$(date +%Y%m%d-%H%M%S)

mv campus-runner-0.0.1-SNAPSHOT.jar.new \
  campus-runner-0.0.1-SNAPSHOT.jar
```

### 4. 重启服务

服务器使用 `/root/run.sh` 重启后端：

```bash
cd /root
./run.sh
```

相关脚本：

`/root/stop.sh`

```bash
仓库文件：apps/api/deploy/stop.sh
```

`/root/start.sh`

```bash
#!/bin/bash
仓库文件：apps/api/deploy/start-wrapper.sh
```

`/root/campus-runner-start.sh` 来自仓库
`apps/api/deploy/start.sh`。它会读取权限为 `600` 的
`/root/campus-runner.env`，并强制生产端口、SSL 和模拟支付配置。

`/root/run.sh`

```bash
仓库文件：apps/api/deploy/run.sh
```

首次部署或启动脚本变化后上传并设置权限：

```powershell
scp apps/api/deploy/start.sh `
  root@116.62.135.75:/root/campus-runner-start.sh
scp apps/api/deploy/start-wrapper.sh `
  root@116.62.135.75:/root/start.sh
scp apps/api/deploy/stop.sh `
  root@116.62.135.75:/root/stop.sh
scp apps/api/deploy/run.sh `
  root@116.62.135.75:/root/run.sh
```

```bash
chmod 700 /root/campus-runner-start.sh
chmod 600 /root/campus-runner.env
chmod +x /root/start.sh /root/stop.sh /root/run.sh
```

## 更新数据库

部署包含数据库结构变化的版本时，应先备份数据库，再执行对应迁移。

二手交易模块使用：

```text
docs/database/second-hand-schema.sql
docs/database/second-hand-delivery-migration.sql
```

统一图片资源使用：

```text
docs/database/media-asset-migration.sql
docs/database/media-asset-phase2-migration.sql
```

已执行第一阶段 `media-asset-migration.sql` 的环境，本版本只需在发布新版 API 前执行
`media-asset-phase2-migration.sql`。该脚本仅为 `tb_media_asset` 增加多图排序字段，
不修改历史 URL，也不删除 OSS 对象。

执行示例：

```bash
mysql -h 数据库地址 -u 数据库用户名 -p 数据库名 \
  < docs/database/second-hand-schema.sql

mysql -h 数据库地址 -u 数据库用户名 -p 数据库名 \
  < docs/database/second-hand-delivery-migration.sql

mysql -h 数据库地址 -u 数据库用户名 -p 数据库名 \
  < docs/database/media-asset-phase2-migration.sql
```

以上脚本不会清空已有数据，并对建表或新增字段进行了重复执行保护。

执行后可检查关键结构：

```sql
SHOW TABLES LIKE 'tb_second_hand_%';

SHOW COLUMNS FROM tb_second_hand_product
LIKE 'pickup_address_snapshot';

SHOW COLUMNS FROM tb_second_hand_order
LIKE 'buyer_delivery_address_snapshot';

SHOW COLUMNS FROM tb_media_asset
LIKE 'sort_order';
```

## 更新管理后台

### 1. 本地构建

```powershell
cd apps/admin
npm install
npm run build
```

构建结果位于：

```text
apps/admin/dist/
```

### 2. 上传和替换

将 `dist` 内容上传至服务器临时目录 `/tmp/campusrunner-admin/`，然后在服务器执行：

```bash
sudo rsync -av --delete \
  /tmp/campusrunner-admin/ \
  /var/www/campusrunner/admin/
```

`--delete` 会删除服务器中已不属于新版构建的旧静态文件。执行前必须确认源目录和目标目录正确。

## 发布后验证

### 后端进程和端口

```bash
ps -ef | grep campus-runner-0.0.1-SNAPSHOT.jar | grep -v grep
ss -lntp | grep ':8080'
```

### 后端日志

```bash
tail -n 200 /root/nohup.out
```

持续查看日志：

```bash
tail -f /root/nohup.out
```

重点确认没有以下数据库认证错误：

```text
Access denied for user
errorCode 1045
```

### 接口

先验证服务器内部服务：

```bash
curl -i http://127.0.0.1:8080/admin/api/banner/getList/1
```

再验证 Nginx 和 HTTPS：

```bash
curl -i https://www.campusrunner.top/admin/api/banner/getList/1
curl -I https://www.campusrunner.top/
```

轮播图列表接口允许匿名访问并会查询数据库，适合作为发布探针。当前
`/api/second-hand/categories` 受鉴权保护，未携带 token 时返回 `401` 属于预期行为；
不要使用 `curl -f` 将该 `401` 误判为后端启动失败。

验证 admin 登录链路时，应使用安全提供的测试账号，不要把密码留在 shell 历史中。
至少确认 `POST /admin/api/login` 能在前端 15 秒超时之前返回，并检查后端日志确实
完成了数据库访问。

如外部接口返回 `502 Bad Gateway`，优先检查后端进程、`8080` 端口和
Nginx 错误日志：

```bash
sudo tail -n 200 /var/log/nginx/error.log
```

## 回滚后端

先列出备份并选择目标版本：

```bash
cd /root
ls -lh campus-runner-0.0.1-SNAPSHOT.jar.bak-*
```

停止当前服务，保留失败版本，再恢复备份：

```bash
/root/stop.sh

mv campus-runner-0.0.1-SNAPSHOT.jar \
  campus-runner-0.0.1-SNAPSHOT.jar.failed-$(date +%Y%m%d-%H%M%S)

cp campus-runner-0.0.1-SNAPSHOT.jar.bak-实际时间 \
  campus-runner-0.0.1-SNAPSHOT.jar

/root/start.sh
```

回滚 JAR 不会自动回滚数据库。若发布包含不兼容的数据库变更，应按该次发布的
迁移说明单独处理数据库。

## SSL 证书自动续签

### 当前方案

生产服务器不再使用阿里云手动申请、下载和替换的个人测试证书。当前方案为：

| 项目 | 当前配置 |
| --- | --- |
| ACME 客户端 | acme.sh `3.1.3` |
| 签发机构 | Let's Encrypt |
| 验证方式 | 阿里云 DNS API，即 `dns_ali` DNS-01 验证 |
| 证书算法 | ECC P-256 |
| 主域名 | `campusrunner.top` |
| 通配符域名 | `*.campusrunner.top` |
| acme.sh 目录 | `/root/.acme.sh/` |
| 域名配置 | `/root/.acme.sh/campusrunner.top_ecc/campusrunner.top.conf` |
| Nginx 完整证书链 | `/etc/nginx/ssl/campusrunner/cert.pem` |
| Nginx 私钥 | `/etc/nginx/ssl/campusrunner/cert.key` |
| 自动检查 | root cron 每天 `10:10` |
| 续签后动作 | `systemctl reload nginx` |

acme.sh 使用保存在 `/root/.acme.sh/account.conf` 中的阿里云 DNS API
凭据完成 DNS-01 验证。该文件包含敏感信息，不得加入 Git、复制到文档或输出到日志。

当前 root crontab：

```cron
10 10 * * * "/root/.acme.sh"/acme.sh --cron --home "/root/.acme.sh" > /dev/null
```

cron 每天检查一次证书，只有进入续签窗口才会真正申请新证书。续签成功后，
acme.sh 会将新私钥和完整证书链安装到 Nginx 目录，并平滑重载 Nginx。

### 2026-07-24 巡检快照

| 检查项 | 结果 |
| --- | --- |
| acme.sh | `3.1.3` |
| 当前证书签发时间 | 2026-07-21 01:12:31 UTC |
| 当前证书到期时间 | 2026-10-19 01:12:30 UTC |
| acme.sh 计划续签时间 | 2026-08-19 02:11:05 UTC |
| 签发机构 | Let's Encrypt `YE1` |
| SAN | `campusrunner.top`、`*.campusrunner.top` |
| crond | 已启用且正在运行 |
| cron 执行记录 | 2026-07-19 至 2026-07-24 每天 `10:10` 均有记录 |
| Nginx 配置检查 | 通过 |

当前待整改项：

- `/etc/nginx/ssl/campusrunner/cert.key` 权限为 `644`，应改为 `600`。
- 旧的手动证书和私钥仍保留在 Nginx 证书目录中，且旧私钥权限为 `644`。
  确认不再需要回滚后，应安全归档或删除。
- cron 将标准输出丢弃，且未配置 acme.sh 专用日志。`/var/log/cron` 只能证明任务
  被启动，不能完整证明续签结果。建议增加专用日志并配置轮转。

### 日常检查

查看证书清单和计划续签时间：

```bash
/root/.acme.sh/acme.sh --list
```

查看线上证书的签发机构和有效期：

```bash
openssl x509 \
  -in /etc/nginx/ssl/campusrunner/cert.pem \
  -noout -subject -issuer -serial -dates
```

确认根域名和通配符域名均被覆盖：

```bash
openssl x509 \
  -in /etc/nginx/ssl/campusrunner/cert.pem \
  -noout -text |
  grep -A1 "Subject Alternative Name"
```

确认定时任务和 crond 正常：

```bash
crontab -l
systemctl status crond --no-pager
grep -F '/root/.acme.sh' /var/log/cron | tail -n 20
```

确认 Nginx 引用正确并且配置有效：

```bash
nginx -T 2>/dev/null | grep -E 'ssl_certificate(_key)?'
nginx -t
```

### 首次安装或灾难恢复

以下命令用于服务器重建或 acme.sh 配置丢失，不需要在日常续签时执行。

安装 acme.sh 后，在当前 shell 中临时设置阿里云 DNS API 凭据：

```bash
export Ali_Key='阿里云AccessKey ID'
export Ali_Secret='阿里云AccessKey Secret'
```

签发根域名和通配符域名证书：

```bash
/root/.acme.sh/acme.sh --issue \
  --dns dns_ali \
  -d campusrunner.top \
  -d '*.campusrunner.top' \
  --keylength ec-256 \
  --server letsencrypt
```

安装到 Nginx 使用的固定路径，并注册重载命令：

```bash
/root/.acme.sh/acme.sh --install-cert \
  -d campusrunner.top \
  --ecc \
  --key-file /etc/nginx/ssl/campusrunner/cert.key \
  --fullchain-file /etc/nginx/ssl/campusrunner/cert.pem \
  --reloadcmd "systemctl reload nginx"
```

随后清除当前 shell 中的敏感变量：

```bash
unset Ali_Key Ali_Secret
```

不要让 Nginx 直接引用 `/root/.acme.sh/` 内部文件。应始终通过
`--install-cert` 将证书部署到固定路径，否则 acme.sh 升级或证书重签后可能出现
路径变化。

### 故障处理

检查当前状态时优先使用：

```bash
/root/.acme.sh/acme.sh --list
tail -n 100 /var/log/cron
nginx -t
```

如需手动执行一次到期检查，可运行：

```bash
/root/.acme.sh/acme.sh --cron --home /root/.acme.sh
```

该命令只会续签已进入续签窗口的证书。不要在日常检查中随意使用
`--force`，以免触发 Let's Encrypt 频率限制。

阿里云 AccessKey 失效时，应在阿里云创建最小权限的 DNS 凭据，然后重新设置
`Ali_Key` 和 `Ali_Secret` 并执行一次签发。不要把 AccessKey 写进项目仓库。

### 私钥权限

私钥应仅允许 root 读取：

```bash
chmod 600 /etc/nginx/ssl/campusrunner/cert.key
```

2026-07-24 巡检时，该文件权限为 `644`，需要收紧。每次续签后也应复查权限；
如 acme.sh 再次将其写成 `644`，应把权限修正加入续签后的部署或重载命令。

## Nginx 维护

修改 Nginx 配置后，先检查配置，再平滑重载：

```bash
sudo nginx -t
sudo systemctl reload nginx
```

只有 Nginx 服务本身异常时才执行：

```bash
sudo systemctl restart nginx
sudo systemctl status nginx --no-pager -l
```

完整 Nginx 配置见 [Nginx 部署](nginx-deployment.md)。
