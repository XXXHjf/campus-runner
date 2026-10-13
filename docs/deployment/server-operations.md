# 生产服务器部署与更新

本文是生产发布、验证和回滚的唯一操作入口。最近业务发布为 2026-10-13，范围及验证见下节；实际监听、证书、数据库和运行开关仍须在每次发布时现场核实。此前的发布、网络防护实施记录与三批整改准备清单是历史阶段记录，不能作为当前业务版本判断依据。

## 2026-10-13 议价替换补发

在下述工作区发布之后，补发 API 的同一买家新报价替换旧待回复报价逻辑；该增量无需数据库结构迁移。管理端、Nginx、生产秘密及小程序均未在本次补发中替换。发布前确认新旧 JAR 内配置和支付密钥一致、环境文件权限为 600、API 回环监听和主机端口防护服务有效，保留既有模拟支付开关及二手 `OFFLINE` 模式。数据库、旧 JAR 和启动脚本备份于 `/root/backups/campus-update-20261013-185237-bargain-replace`。API 使用既有 `/root/run.sh` 更新并重启，测试按用户要求跳过；发布后单进程及回环监听、启动日志、内部和公网业务探针、公开分类与订单接口及 HTTPS 首页通过。报价替换的有效账号交互与小程序真机体验未在本次验证，由用户手动上传并验收小程序。回滚使用该目录旧 JAR 和既有启动脚本；不要以旧数据库转储覆盖发布后的新业务记录。

## 2026-10-13 工作区迁移与发布记录

用户明确要求一起发布当前工作区，已更新 API、管理端及必要数据库迁移，同步现有启动/停止脚本；未上传小程序、未修改 Nginx、未启用请求保护、未提交或推送 Git。本次包括议价拒绝原因、商品变化导致议价失效及通知，以及工作区已有的订单公开信息、图片批量读取、跑腿确认完成通知和接单错误反馈等改动。发布源码及 JAR 快照保存在本地 `tmp/update-source-20261013-bargain`，临时材料不应提交 Git。

发布前确认新 JAR 内配置和支付密钥与原生产版本一致，数据库凭据已确认，环境文件权限 600，管理端同域 API 配置已确认。保留 `MOCK_PAYMENT_ENABLED=true`、请求保护关闭及二手 `OFFLINE` 模式；模拟结果不能视为真实资金到账。存量 ONLINE 订单 ID 1 状态 10、ID 2 状态 8 在迁移前后及重启后保持不变。本次未变更生产秘密或微信模板配置。

备份目录为 `/root/backups/campus-update-20261013-bargain`，包含完整数据库转储、旧 JAR、管理端、启动/停止脚本、受限权限环境文件、站点配置及迁移候选。通过既有脚本停止 API 并确认无写入进程后，转储在隔离库完整恢复，27 张表的记录数量逐表一致；隔离库演练字段迁移（含重复执行）和历史失效修复后已移除。正式执行 `second-hand-bargain-reject-reason.sql`，新增可空 `reject_reason_code varchar(32)` 和 `reject_note varchar(40)`（utf8mb4），不回填虚构原因；执行修订后的 `second-hand-bargain-expiry-repair.sql`，本次候选为 0 条。迁移期间全部业务表记录数量不变，先完成迁移再启动新 API。

API 打包及相关 112 项服务端测试通过，管理端构建通过；议价小程序 16 项逻辑测试在开发阶段通过。上传和部署 JAR 校验和一致。发布后仅一个 API 进程，HTTP 8080 监听回环，端口/SSL 启动参数符合约束；IPv4/IPv6 主机拒绝规则及端口防护服务有效，外部 IPv4 8080 无法连接。Nginx 配置检查通过，回环与 HTTPS 数据库探针、二手分类、公开订单和商品返回 HTTP 200、业务 `code=1`，`pageSize=1` 生效；管理端新首页及两项构建资源返回 200，目录权限 755。匿名议价读取、拒绝写入和管理端私有读取被拒绝，无效管理员账号登录快速返回业务失败；启动日志未见数据库认证、缺列、SQL 语法或 Bean 初始化失败。

六个资金通知路径拒绝无签名测试报文（HTTP 500）；这不证明合法签名通知、重放防护或真实资金联调通过。有效身份议价读写、通知模板适用性和实际送达、图片上传及小程序视觉/键盘/真机交互仍需对应账号与人工验收；云安全组及外部 IPv6未在本次重新核验。用户负责上传当前小程序版本，不能用服务器健康检查替代客户端验收。

回滚使用该目录旧 JAR、管理端及启动脚本，通过既有停止/启动流程执行，保留回环监听和端口防护。新增可空列、已保存拒绝原因和失效记录继续保留；不自动恢复报价，不用整库旧转储覆盖发布后新增业务。备份已完成隔离恢复验证，正式数据库恢复仍须逐条核对发布后业务的保留方案。

## 2026-10-12 业务发布记录

用户确认能够在服务器更新后立即手动发布小程序，本次发布当前工作区 API 与管理端业务改动，同步仓库启动脚本；未发布小程序、未安装 Nginx 连接限制模板、未启用请求保护，也未提交或推送 Git。小程序创建请求、列表分页、地址选择和消息游标已发生配套变化，旧版本无法正常创建订单或发布商品，必须立即协调小程序发布；不能以服务器健康检查替代客户端验收。

发布前核对生产配置与支付私钥同本次 JAR 内资源一致，环境文件权限为 600，数据库凭据已确认。同域管理端配置已确认，Nginx 语法、回环上游和 20m 上传限制通过。保留既有 `MOCK_PAYMENT_ENABLED=true` 与通知开关；模拟状态不能视为真实资金到账。二手模式为 `OFFLINE`，逐单核对两笔历史 ONLINE 订单（ID 1 状态 10、ID 2 状态 8），未人工修改其资金状态或历史编号。

数据库、旧 JAR、admin、启动脚本及站点配置备份位于 `/root/backups/campus-update-20261012-002256`。停止既有 API 后，完整数据库转储在隔离库恢复，核对 24 张原有表记录数量并演练迁移；正式执行 `order-number-uniqueness.sql`、`submission-idempotency.sql` 与 `request-guard.sql`。跑腿表新增 `uk_runner_order_number`，二手已有唯一索引保留；新增三张 InnoDB 表。原有业务记录数量不变，95 笔跑腿订单的 190 条地址快照完整。历史自接单和未结清资金仍需负责人逐单核对，未在本次自动修复。

验证：API/admin 构建通过，关键服务端测试 29 项与小程序逻辑测试 46 项通过；JAR 校验和匹配。生产仅一个 API 进程，HTTP 8080 监听回环，IPv4/IPv6 主机防护规则及服务有效，外部 IPv4 8080 不可连接。管理端首页、构建静态资源、HTTPS 数据库探针、游客订单及分类返回 HTTP 200、业务 `code=1`；公开订单及商品 `pageSize=1` 生效，公开订单未暴露受检查的私人字段，匿名私有读取和写入被拒绝。无效管理员账号登录快速返回业务拒绝；未使用有效测试账号完成登录。启动日志未见数据库认证、缺列或 Bean 初始化失败。

六个资金通知路径对缺少签名的测试报文均返回 HTTP 500，未确认通知成功；这只记录拒绝结果，不证明合法通知、重放或真实支付链路通过。有效身份分页/完整计数、图片上传、真机交互、真实资金联调、云安全组及外部 IPv6仍需对应条件下验收。请求保护维持 `false`，兼容小程序完成发布及上传验收后再按后文流程显式启用。

回滚优先使用上述备份的 JAR、admin 与启动脚本，保持回环监听和主机端口防护，并协调兼容小程序版本。保留新增唯一索引、幂等/保护表、地址与资金证据；禁止用整库旧转储覆盖发布后新增业务。完整数据库备份已完成隔离恢复验证，但恢复正式业务仍须另行核对新增数据保留方案。

## 部署约定与源码边界

| 项目 | 当前配置 |
| --- | --- |
| 公网 IP | `116.62.135.75` |
| 域名 | `www.campusrunner.top`、`campusrunner.top` |
| HTTPS 入口 | Nginx，监听 `80` 和 `443` |
| 后端服务 | Spring Boot JAR，HTTP `8080`；启动模板指定 `127.0.0.1`，发布后仍须现场核实 |
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
- [ ] `server.port` 为 `8080`，`server.ssl.enabled` 为 `false`；已核实实际监听地址及公网 8080 防护，不能把 Nginx 使用回环上游等同于 API 仅监听回环。
- [ ] 图片上传的 multipart 单文件上限为 `10MB`、单请求上限为 `20MB`；生产
      `campus-runner-start.sh` 已更新为仓库当前版本。
- [ ] HTTPS 站点显式设置 `client_max_body_size 20m;`，且 `/api/`、`/admin/api/`
      没有更小的覆盖值，避免 Nginx 默认 `1m` 限制在 API 前拦截图片上传。配置修改前
      已备份，修改后先通过 `nginx -t` 再平滑重载；验证及回滚见
      [Nginx 图片上传大小限制](nginx-deployment.md#图片上传大小限制)。
- [ ] 已确认模拟支付开关状态。默认关闭；只有用户明确要求线上开发测试时才启用，
      由 `/root/campus-runner.env` 中的 `MOCK_PAYMENT_ENABLED` 控制。开启会将所属订单
      标记为模拟支付/收款成功并写入模拟流水，不会发生真实扣款或转账；不得把此结果
      当作真实资金到账。用户未明确要求关闭前，发布不能擅自将其关闭。
- [ ] 当前二手交易配置 `second_hand_trade_mode` 为 `OFFLINE`，新订单不发起支付、退款或
      卖家收款；发布前已逐单核对存量线上订单。
- [ ] 只有将来重新发布二手线上收款功能时，才核对微信正式准入和对应场景；不得把旧的
      `1010` 实现直接恢复为普通学生 C2C 收款。
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

spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 20MB

com:
  mikasa:
    campus-runner:
      dev:
        mock-payment-enabled: false
      wechat:
        second-hand-transfer-scene-id: 1010
```

数据库配置必须使用生产凭据，不能沿用本地开发配置。当前 YAML 使用
`${DB_PASSWORD:admin}`：本地未设置环境变量时默认使用 `admin`，生产服务器必须在
`/root/campus-runner.env` 中设置 `DB_PASSWORD`。该文件权限必须为 `600`。日志和
检查输出只能显示“已配置”，不能回显密码。

生产启动脚本读取同一环境文件中的 `MOCK_PAYMENT_ENABLED`，未设置时默认为 `false`，
并将值覆盖 JAR 配置。仅在用户明确要求线上开发测试时设置为 `true`；该状态跨 API
重启和常规版本更新保留，直到用户明确要求关闭，再将变量设为 `false` 并重启 API。
模拟支付和收款接口按登录用户与订单归属校验，但会真实改写订单状态和模拟流水；启用期间
不得将模拟状态理解为真实扣款、退款或转账。

### 诊断日志中的个人资料保护

小程序诊断日志只保留固定事件描述、必要订单 ID 和状态，不打印用户对象、姓名、手机、
地址详情、证件信息、订单说明、签名图片地址、登录凭证或支付响应。服务端请求日志不
打印整份 DTO；资金请求与回调日志不打印报文，异常仅记录类型和业务标识，不输出异常
消息、原因链或第三方返回体。原有资金证据仍保存在受控业务记录中，不依赖控制台日志。

`DiagnosticLoggingConfiguration` 禁用 MyBatis SQL 参数日志，即使本地 Mapper 日志
级别被设置为 DEBUG，也不会把资料写入 SQL 参数诊断。生产启动脚本继续关闭微信
`SignatureExec` 日志，并关闭 Apache HttpClient 的 `wire` 与 `headers` 日志；不得临时
打开它们排查真实用户请求。需要定位故障时使用订单 ID、失败类型、HTTP 状态和受控
业务记录，不能把完整资金报文或签名地址复制到聊天、工单或 Git。

该修改不自动清理已存在的本地或生产日志。历史日志的留存、访问权限与删除须单独
核实，不能用删除资金业务记录代替日志整改。发布仍执行生产必检清单，并同步现有
启动脚本；现场验证只能报告敏感值是否泄露，不回显具体值。验证包括
`DiagnosticLogPrivacyTest` 的资料/异常哨兵检查和 SQL 日志禁用检查；代码回退会恢复
资料泄露路径，回退前应限制诊断日志访问且不得打开 SQL/HTTP 报文日志。

## 配置与启动故障排查

历史登录超时的长期教训仍适用：被 Git 忽略的 YAML 仍会进入 JAR；环境密码未注入可导致 MySQL 1045；非登录 shell 可能没有 Java PATH。生产脚本使用已验证的 Java 21 绝对路径、环境文件和日志覆盖项。不得手改秘密进 YAML、延长前端超时或用进程存在代替数据库验证。

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

## API 监听与公网隔离

### 2026-10-11 已实施与核验证据

按用户确认的第二批建议，仅给生产 `/root/campus-runner-start.sh` 增加 `--server.address=127.0.0.1`，通过 `bash -n` 后用既有 `/root/run.sh` 重启原 JAR。变更前备份启动/停止脚本、JAR、Nginx 和 IPv4/IPv6 规则至 `/root/backups/second-batch-network-20261011-120313`。未替换业务 JAR、管理端或生产秘密，也没有执行数据库写入。

重启后 `ss -lnt` 显示 `[::ffff:127.0.0.1]:8080`，这是 Java 显示的 IPv4 回环映射，不能误认为 `[::]:8080` 通配监听。生产与仓库启动模板均已加入监听参数，但生产脚本并未整体同步仓库其他在途改动。后续发布必须保留监听参数，继续核对完整模板和实际环境。

主机安装 `/usr/local/sbin/campus-runner-port-guard` 与 `campus-runner-port-guard.service`，服务 enabled/active；脚本与单元的权威源分别为 `apps/api/deploy/port-guard.sh` 和同目录 `campus-runner-port-guard.service`。脚本幂等检查，在 iptables 与 ip6tables INPUT 最前方仅插入拒绝非回环 8080 TCP 的规则，不清空规则、不改默认策略、不影响 SSH、80/443。系统启动由该单元恢复规则；后续若启用会重载防火墙的管理工具，需再次核实规则，不能仅凭单元 active 断言规则仍在。

验证：环境文件权限 600；`nginx -t` 通过；回环及外部 HTTPS 的 `/admin/api/banner/getList/0` 都返回 HTTP 200、业务 `code=1`，覆盖数据库读取。外部绕过代理访问公网 IPv4 8080 得到 `http=000`，无法连接；IPv4/IPv6 主机规则均存在。此前 `/getList/1` 只证明 HTTP 连通，业务失败不算数据库健康证据，现已改用 `/getList/0`。

当前没有全局 IPv6 地址；未取得云控制面权限核实所有关联安全组，未修改云侧规则，也未完成外部 IPv6 测试。没有授权测试账号登录及真实支付/退款/转账通知联调证据。实际 HTTP 暴露已在主机收口，但不能宣称云侧全链路核验完成。业务 API 后续发布仍执行本页完整必检。

### 维护与回滚

部署防护脚本时用 `install -m 700` 放到 `/usr/local/sbin/campus-runner-port-guard`，单元用 `install -m 644` 放到 `/etc/systemd/system/`，再 `systemctl daemon-reload`、`systemctl enable --now campus-runner-port-guard`。先核对现有规则，安装后复查两个协议栈的规则、回环和 HTTPS。不要把完整防火墙快照覆盖到已有其他修改的主机。

HTTPS 异常先核对回环数据库探针和监听。恢复备份启动脚本必须通过 `bash -n`，再用原 `/root/run.sh` 重启；应保留端口防护以免恢复通配监听后重新暴露。确需回退防护时停止并禁用该单元，使用 `iptables -D INPUT ! -i lo -p tcp --dport 8080 -j REJECT --reject-with tcp-reset` 与对应 `ip6tables -D` 仅删除本次规则，不能 flush 或修改默认策略。停止 oneshot 单元本身不会删除已生效规则。移除本次单元/脚本后 daemon-reload，再复查 HTTPS 与监听；此操作可能重现公网暴露，不能作为日常故障处理捷径。

云控制面可用时核实关联安全组、宽端口规则、根域 A/AAAA 和外部 IPv6；只撤销公网 8080 放行，保留其他业务。401/403 仍代表端口可达，不能当作阻断成功。

## 更新后端

### 1. 本地打包

在仓库根目录使用独立 shell 执行（后续上传命令仍从仓库根目录执行）：

```powershell
mvn -f apps/api/pom.xml clean package -DskipTests
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

相关脚本对应关系：

| 仓库文件 | 服务器路径 |
| --- | --- |
| `apps/api/deploy/stop.sh` | `/root/stop.sh` |
| `apps/api/deploy/start-wrapper.sh` | `/root/start.sh` |
| `apps/api/deploy/start.sh` | `/root/campus-runner-start.sh` |
| `apps/api/deploy/run.sh` | `/root/run.sh` |

启动脚本读取 `/root/campus-runner.env`，强制端口、SSL、上传限制和日志配置；模拟支付默认关闭，明确授权的线上测试覆盖值由该环境文件保留。脚本变更前先比对服务器差异，保留已确认有效的配置，不以仓库模板直接覆盖线上差异。

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

先核对目标环境结构和待发布代码，按 [数据库变更导航](../database/README.md) 选择必要迁移；数据库迁移规则、只读审计、验证和回滚只维护在对应主题文档。不能按目录顺序执行所有 SQL，不能凭历史发布记录认定无需迁移。

涉及删列、历史回填或需要一致数据视图时，使用既有脚本停止全部写入实例，验证可恢复的完整备份后执行迁移，再启动依赖新结构的 API。MySQL DDL 会隐式提交，不能依赖事务回滚结构。

备份工具参数须先核对目标客户端版本；历史 MySQL 5.5 客户端不支持 `--set-gtid-purged`，不能照抄其他版本命令。恢复演练使用隔离库，不改全局 GTID，不将包含地址、手机或凭据的转储上传 Git。

订单地址快照的回填须验证每笔订单两条快照及来源分布；`LEGACY_CURRENT` 仅证明迁移时地址，不证明下单原貌。回滚代码保留快照、媒体及资金证据，禁止旧整库转储覆盖发布后新增业务。

### 最近发布的回滚线索

历史 2026-10-08 发布记录中的备份目录为 `/root/backups/campus-unified-20261008-221133`。本轮未验证目录存在性或恢复能力；使用前现场核实文件、校验和、对应版本及新增业务保留方案。它是回滚线索，不是当前生产状态或可直接整库恢复的授权。

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
使用 `rsync -a` 时还要注意，源目录权限会同步到站点目录。临时目录及其子目录应允许 Nginx
遍历（目录至少 `755`），静态文件应允许 Nginx 读取（通常为 `644`）；若站点根目录被同步成
`700`，首页会返回 `403`、静态资源可能返回 `404`。发布后若遇到该现象，先检查
`/var/www/campusrunner/admin/` 及 `assets/` 的目录权限，将站点根目录恢复为 `755` 后重验首页和资源；
无需因此重启 API 或 Nginx。

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
curl -i http://127.0.0.1:8080/admin/api/banner/getList/0
```

再验证 Nginx 和 HTTPS：

```bash
curl -i https://www.campusrunner.top/admin/api/banner/getList/0
curl -I https://www.campusrunner.top/
```

通用轮播图列表 `/admin/api/banner/getList/0` 允许匿名访问并会查询数据库，适合作为
发布探针；必须同时确认 HTTP 200 和业务 `code=1`。不要假设学校 ID `1` 永久存在：
学校被删除时 `/getList/1` 会返回业务失败，但不代表数据库连接故障。当前
游客浏览版本部署后，`/api/second-hand/categories` 及 `/api/order/public` 的 GET 也应允许
匿名读取并返回业务 `code=1`；返回 401 时应核对部署版本和公开读取配置，不能将其视为当前版本的正常行为。
游客能力应先发布 API，再上传小程序，且个人订单与写入仍必须拒绝匿名访问。

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

### 续签部署约定

生产服务器不再使用阿里云手动申请、下载和替换的个人测试证书。既有部署约定如下，客户端版本、定时任务和证书状态须现场复查：

| 项目 | 当前配置 |
| --- | --- |
| ACME 客户端 | acme.sh；版本现场核实 |
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

既有 cron 配置示例（实际任务须现场核实）：

```cron
10 10 * * * "/root/.acme.sh"/acme.sh --cron --home "/root/.acme.sh" > /dev/null
```

cron 每天检查一次证书，只有进入续签窗口才会真正申请新证书。续签成功后，
acme.sh 会将新私钥和完整证书链安装到 Nginx 目录，并平滑重载 Nginx。

续签检查须验证证书实际有效期、SAN、私钥权限、安装路径、任务执行及续签结果；cron 被启动不能单独证明续签成功。历史到期日、旧权限问题及旧巡检结果已删除，避免作为当前告警或整改状态。

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

每次续签后复查实际权限；若客户端重新写入了过宽权限，应把权限修正加入续签后的部署或重载步骤。

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

## 待审核认证群通知

当前实现是单 API 实例的待办汇总提醒，默认关闭，无数据库迁移。后台人数角标独立工作。
生产是否启用及机器人是否可用须现场核实；代码默认关闭，避免开发环境或其他实例意外向同一群发送。发布后需同时核对通知日志和待审核接口，
平台接收成功不等于每位管理员已读。
不用提交后内存事件作为唯一通知来源，是为了让进程重启后仍能发现未处理的认证，
并使外部网络故障完全脱离学生提交事务。通知在独立线程运行，不占用订单定时任务线程。

启用后启动约 30 秒开始检查；没有待办时每 30 秒检查一次。首次发现待办即尝试发送，
成功后冷却 30 分钟，冷却结束重新查询，只在仍有待办时提醒。因此冷却期内的新提交会合并，
最长约 30 分钟后提醒；并非逐条实时通知。全部审核完成后不再发送。
失败按 1、2、4、8、10 分钟退避，之后最多每 10 分钟重试，每次重读人数，
避免继续通知已处理的申请。HTTP 连接超时 3 秒、请求超时 5 秒，同时检查 HTTP 状态及业务 errcode。
日志只记录人数或重试次数，不打印 URL、密钥、响应正文或异常详情。

冷却状态只保留在当前进程：重启可能再发一次待办汇总；超时但平台实际已接收也可能重复。
此提醒不承诺恰好一次或 3 秒送达。多实例部署前必须增加共享冷却/分布式互斥，不能同时启用多个发送实例。

### 渠道前置与配置

此实现要求已确认可用的加签自定义机器人；不能假定所有账号都可新建机器人。没有可用渠道时保持关闭，根据组织实际权限另行接入，不能只改开关便视为通知可用。

在服务器权限为 `600` 的 `/root/campus-runner.env` 中配置以下变量，沿用仓库启动脚本加载：

| 环境变量 | 值/用途 |
| --- | --- |
| `NOTIFY_DINGTALK_ENABLED` | 默认 false，完成渠道核验后设 true |
| `NOTIFY_DINGTALK_WEBHOOK` | 已有机器人的 HTTPS Webhook，秘密，不写进仓库或聊天 |
| `NOTIFY_DINGTALK_SECRET` | 已开启的加签密钥，秘密，不写进仓库或聊天 |
| `NOTIFY_DINGTALK_REVIEW_URL` | 可选，默认 `https://www.campusrunner.top/users/pending-auth` |

对应 Spring 属性为 `notify.dingtalk.enabled/webhook/secret/review-url`。
启用但配置无效会拒绝启动；禁用时不创建通知组件、不读取待办或发送群消息。
Webhook 仅允许 `https://oapi.dingtalk.com/robot/send?access_token=...`，禁止重定向。
审核地址须是无查询参数的 HTTPS 页面地址，不得包含任何登录凭据。

### 验证与回滚

发布前执行本文生产必检清单，确认机器人仍可用、加签已启用、管理员群成员正确，秘密仅报告已确认/未确认。
启用会向所配置群发送真实待办提醒，首次应在授权测试群验证：有待办能收到汇总、多人可见；
未登录点击链接能登录后返回待审核页；审核通过/驳回后人数刷新；全部处理后不再提醒；
外部发送故障不影响认证提交。配置校验失败时不要替换线上版本，应修复配置或关闭通知后再发布。
静态检查与单元测试不代替实际群消息联调。

紧急关闭只需把 `NOTIFY_DINGTALK_ENABLED` 设为 false，再按仓库脚本重启 API，后台角标仍可用。
没有迁移需撤销；版本回滚按常规恢复 JAR 和管理端静态文件，不删除学生认证记录。


## 请求频次与上传额度保护（本地已实现，生产尚未启用）

2026-10-11 用户确认按建议实施共享存储、额度、可信代理与地址查询方案。本节替代此前待确认方案；本轮不授权业务 API 发布或生产限流启用。

### 当前实现与初始额度

共享存储使用现有 MySQL 的 InnoDB 短事务，新增表见 [请求保护迁移](../database/request-guard.sql)。账号键来自验签后的用户/管理员 ID，两个命名空间独立；不以 JWT 文本作为账号，不依赖 JVM 内存计数。计数和上传租约跨 API 实例、重启共享。配置入口是 `RequestGuardProperties` 的 `campus.guard.*`，`enabled` 默认 false，生产迁移前不得打开。

| 保护 | 初始值 | 实施位置 |
| --- | --- | --- |
| 账号读取 | 每分钟 120 次 | 身份拦截后；读写分组，用户/管理员分开 |
| 账号写入 | 每分钟 30 次 | 保留既有提交幂等，不自动重试写入 |
| 图片上传 | 每账号、用途组每分钟 30 次；每账号同时 3 个处理租约 | 早期 Filter，在 Spring multipart 解析前验签及检查 |
| IP 普通请求 | 每秒补充 100，突发容量 300 的共享令牌桶 | API Filter，匿名入口也受保护 |
| IP 上传 | 每分钟 300 次 | 外层粗粒度保护，不能替代账号额度 |
| 内容图片 | 每日 200 次、输入 200MiB；未完成资源 60 个、80MiB | 包含商品/订单内容、头像、学生证 |
| 业务凭证 | 独立每日 200 次、输入 200MiB；未完成资源 60 个、80MiB | 配送和采购凭证；内容额度耗尽不消耗此预算 |
| 管理员图片 | 独立每日 1000 次、输入 1GiB；未完成资源 200 个、256MiB | 分类图和轮播图 |

分钟采用共享数据库时钟的固定窗口，边界附近可连续用完两个窗口，不等同于滑动窗口；日窗口固定 Asia/Hong_Kong。阈值是已接受的初始策略，上线后按校园共享出口、正常翻页/轮询与上传误拦截情况调整，不宣称已经生产压测。

上传 URL 需携带唯一 `purpose` 查询参数，multipart 仅传 `file`。早期 Filter 不调用 `getParameter()`，以免触发解析：先用有效长度与用途单文件上限的较小值预占输入预算；无长度按 10MiB 预占。Service 按实际 multipart 文件大小核对身份、用途和预占量，只归还未使用输入字节，次数不返还；失败解析保留保守预占。结算沿用原日窗口，跨午夜也不向新窗口退款。日额度拒绝发生在解析前，未完成资源则在读取文件字节/解码/OSS 写入前，以账号排他锁和 `UPLOADING` 记录原子预占最大输出字节。

`UPLOADING/TEMP/FAILED/PENDING_DELETE` 都计入未完成存储；实际处理后缩减输出预占。结果不明不视为对象不存在；沿用现有确认删除后记 `DELETED` 的清理链路。绑定只转移存储类别，不归还日尝试量；不自动删除正式绑定图片腾额度。历史未登记对象不包含在配额中，不能据此推断整个 OSS 账单。

上传租约期限一小时，活跃请求每 30 秒续租并延后其 `UPLOADING` 清理期限；仅本实例的活跃工作用于续租，准入仍由共享库决定。崩溃租约自然到期；不恢复已过期租约，处理/OSS 写入前后核验其仍有效。额度存储异常返回中文 HTTP 503，保护层不退回本机 Map 或静默放行。频次/日额度返回 HTTP 429 和 `Retry-After`；资源容量要求用户先完成或取消操作，按业务失败返回。小程序上传和管理端均显示可操作中文提示，不展示原始服务错误。

### 代理与资金通知边界

2026-10-11 只读筛选服务器 `nginx -T`：站点 API 代理到 `127.0.0.1:8080`，明确覆盖 `X-Real-IP $remote_addr`，`X-Forwarded-For $proxy_add_x_forwarded_for`；筛选未见 `real_ip_header/set_real_ip_from`。该证据是磁盘可解析配置，不证明完整 CDN/WAF 链路或当前重载状态。只信回环 `127.0.0.1/::1` 的合法数字 `X-Real-IP`；其他来源只使用连接地址。任意客户端 `X-Forwarded-For` 不参与选址。发布启动脚本指定 `server.forward-headers-strategy=none`，避免框架先改写连接地址；如未来引入 CDN/WAF，需核实来源与覆盖行为后再调整，不能直接扩大可信网段。

六个通知路径精确豁免账号/IP/连接小额度：`/api/wx-pay/jsapi/notify`、`/api/wx-pay/refunds/notify`、`/api/wx-transfer/notify`、`/api/second-hand/pay/notify`、`/api/second-hand/refunds/notify`、`/api/second-hand/transfer/notify`。原验签、重放和持久化幂等继续执行；不豁免支付前缀。异常编码/矩阵上传路径在 MVC 规范化之前拒绝，避免绕过解析前保护。

Nginx 外层连接方案已准备 `apps/api/deploy/nginx-request-guard-http.conf.example` 和 `nginx-request-guard-server.conf.example`：100 个连接/并发请求，资金通知使用空键豁免；仅为本地模板，未安装或重载。它不是 API 多实例共享计数的替代。Nginx 在读完请求头后才计连接，HTTP/2 每个并发请求单独计数，依据 [官方连接限制说明](https://nginx.org/en/docs/http/ngx_http_limit_conn_module.html)。存在 CDN/WAF 时先核实实际源地址，不能把所有 CDN 流量误聚合为一个校园用户。

### 地址与配置候选集

个人 `/api/address/show` 与 `/api/address` 支持 `page/pageSize`，默认 1/20、最多 50；保持当前账号授权和默认优先、ID 倒序。`/api/address/summary` 独立返回总量、默认数量及一条默认地址，默认修复使用聚合查询，不再加载全部 ID。通知按具体地址 ID 查询。地址页/取送件弹层/二手配送弹层支持续读；草稿及当前选择的旧地址按 ID 单独查验，不能因不在首批就清空或当作已删除。

`/api/address/three` 按页读取学校/校区/类型元数据，默认不返回建筑；指定 `compusNumberId` 后仅返回当前允许学校、所选校区的建筑页。现有选择器逐页收齐元数据，选择校区时收齐该分支建筑，不下载所有校区的建筑；未增加页面或重设计。异步父级切换按版本拒绝旧结果，加载中或选择值已不在当前列时不能确认。

学校、跑腿/二手分类、管理端学校建筑和轮播图候选接口也接受分页且最多 50 条，媒体按页批量解析。小程序及管理端需要完整候选集时逐页收齐再显示，失败不交付局部候选，不限制已有地址/分类数、不删历史记录。客户端拒绝超量或反复相同的旧响应，避免旧 API 忽略页码时无限续读。偏移分页遇到并发增删仍有边界，不承诺数据库快照。

### 发布、验证与回滚

1. 先确认本次业务发布范围、目标环境和兼容小程序版本，保护配置保持关闭；不得一并部署其他未确认的在途变更。
2. 按本文生产必检核对打包配置及秘密注入，备份数据库、JAR、脚本、Nginx。执行 `docs/database/request-guard.sql`，核实新表的主键/索引和 InnoDB，不修改业务或资金记录。部署 API 后再开启 `CAMPUS_GUARD_ENABLED=true`；其他阈值通过 `--campus.guard.*` 显式参数配置。
3. 先上传/发布兼容小程序及管理端，再正式启用保护；用户负责小程序上传。旧上传仅把用途放在 multipart 的客户端，在开启保护后会提示更新，不能跳过解析前准入。地址树旧客户端也须协调更新。
4. 在隔离环境核验共享库、读/写/匿名/上传、跨实例重启、伪造转发头、三并发及租约过期、日输入预占结算、独立凭证预算和确认删除后释放；只用测试账号与媒体，不触发真实资金。Nginx 如采用模板，先合入明确作用域，`nginx -t` 后按本文流程重载，不自行改云网络。
5. 完成生产 HTTPS/数据库健康、有效身份分页/完整计数、上传和六种通知验签/幂等验证；生产查询计划及峰值容量另在目标库核实，不能从临时测试库推断。观察 429/503、共享出口峰值与误拦截，不记录 JWT、手机号、私有签名 URL 或私钥。
6. 回滚先设 `CAMPUS_GUARD_ENABLED=false` 并按现有脚本重启；如已启用 Nginx 模板，撤销该差异并校验/重载。保留计数表、上传资源、业务及幂等记录，结果不明的对象仍走清理核验；客户端与 API 一并选择兼容版本。新表可保留，不需为回滚删表。

本轮真实 MySQL 验证只创建/删除本机临时数据库；生产仅只读代理核对，未执行该迁移、上传业务 JAR 或启用保护。静态/逻辑检查不代替小程序人工交互验收和正式生产验证。

## 三批整改发布准备汇总（2026-10-11）

本节是准备清单，不是发布授权。第三批按用户要求跳过前端页面及其配套功能；只有历史线上二手取消提示的服务端修正，无新增接口、配置或数据库迁移。求助闭环仍缺产品范围/负责人，用户协议和隐私内容仍缺权威正文核验，不能将两项勾为全部完成。详细证据分别见 [订单异常求助](../mini-program/order-flow.md#异常订单求助核验2026-10-11)、[二手异常边界](../api/second-hand.md#异常订单求助核验2026-10-11) 和 [协议入口核验](../api/user-onboarding.md#用户协议与隐私入口核验2026-10-11)。

后续独立发布前还需完成：

- 第一批：限流四项方案已确认，不重复确认策略；业务 API/管理端/小程序尚需协调发布，真实目标库索引/查询计划、共享出口容量及上传误拦截待验收。外层 Nginx 连接模板未安装，保护默认关闭；按上节顺序迁移、更新兼容客户端再启用。
- 第二批：接单与额度本地规则已实施；历史自接单和未结清资金逐单处理、调低额度的旧预支付/已支付记录核对、微信商户实际能力仍由负责人确认。主机 8080 防护已有实施记录，云安全组、外部 IPv6及登录/资金通知联调仍待核验。公开内容审核/举报为用户明确跳过事项，不夹带上线。
- 第三批：指定求助处理负责人、覆盖范围、反馈/结案规则与证据保留策略；提供两份权威文件及版本，人工核验微信后台正文、原生授权和拒绝流程。页面及对应服务端功能留待另次授权实施，不在本次发布中伪装为已支持。
- 数据库：先只读核对目标结构与迁移记录，再备份并执行实际缺失的脚本；重点核对 `order-number-uniqueness.sql`、`submission-idempotency.sql`、`request-guard.sql`，并检查在途 API 所依赖的地址快照、认证反馈、预期时间、订单隐藏、媒体及二手相关既有结构。完整导航见 [数据库变更](../database/README.md)。不能把仓库 SQL 全部执行一遍；订单号重复候选须人工核对，不自动改历史资金编号。
- 客户端：提交幂等的新 API 要求 `clientRequestId`，旧客户端无法创建；历史消息游标、列表响应和地址树分页须配套更新；旧上传只在 multipart 内声明用途时不能通过启用后的早期保护。核对管理端分页响应、校园认证审核版本及同域 API；用户负责小程序上传。第三批仅提示文字变化，兼容现有客户端。
- 发布与回滚：明确本次选入的代码，保留其他在途修改；执行本文全部生产必检，核对 JAR 内配置/秘密注入、真实支付开关、线下模式和启动脚本，准备数据库/JAR/admin/Nginx 备份。发布后验证 HTTPS、数据库、鉴权、分页/上传及资金通知；客户端视觉/交互由用户验收。按各迁移说明协调回退，保留订单、证据、幂等与资金记录，不以清库修复兼容问题。

此清单没有重新核验生产状态，也不替代实际发布时的生产必检；本批未提交、未部署或执行生产迁移。
