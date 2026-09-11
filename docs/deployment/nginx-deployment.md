# Nginx 部署

本文用于首次安装或重新配置生产服务器的 Nginx。日常发布和回滚流程见
[生产服务器部署与更新](server-operations.md)。

## 目录准备

```bash
sudo mkdir -p /var/www/campusrunner/admin
sudo mkdir -p /etc/nginx/ssl/campusrunner
```

管理后台构建文件放在：

```text
/var/www/campusrunner/admin/
```

SSL 证书由 acme.sh 自动签发和续签，并安装到：

```text
/etc/nginx/ssl/campusrunner/cert.pem
/etc/nginx/ssl/campusrunner/cert.key
```

不要再手动下载阿里云个人测试证书覆盖这些文件。自动续签方案、检查方法和
灾难恢复步骤见[生产服务器部署与更新](server-operations.md#ssl-证书自动续签)。

限制私钥权限：

```bash
sudo chmod 600 /etc/nginx/ssl/campusrunner/cert.key
```

## 安装和启动

```bash
sudo yum install -y nginx
sudo systemctl enable --now nginx
```

检查端口：

```bash
sudo ss -lntp | egrep ':80|:443|:8080' || true
```

## 站点配置

创建 `/etc/nginx/conf.d/campusrunner.conf`：

```nginx
server {
    listen 80;
    server_name www.campusrunner.top campusrunner.top;

    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name www.campusrunner.top campusrunner.top;

    # Match Spring multipart request limit; individual images remain limited to 10MB.
    client_max_body_size 20m;

    ssl_certificate     /etc/nginx/ssl/campusrunner/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/campusrunner/cert.key;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_session_timeout 10m;
    ssl_session_cache   shared:SSL:10m;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    root /var/www/campusrunner/admin;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /admin/api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }

    location ~* \.(js|css|png|jpg|jpeg|gif|svg|ico|woff2?)$ {
        expires 7d;
        access_log off;
        add_header Cache-Control "public";
        try_files $uri =404;
    }

    location ~ /\. {
        deny all;
    }
}
```

`proxy_pass` 末尾不要添加 `/`，否则可能改变转发后的接口路径。

### 图片上传大小限制

HTTPS 站点必须显式配置 `client_max_body_size 20m;`，同时覆盖 `/api/` 和
`/admin/api/`；不要在子 `location` 中设置更小的覆盖值。它与 Spring multipart 的
单请求 `20MB` 上限一致，单文件仍由客户端和 API 限制为 `10MB`，不能关闭大小限制。

Nginx 未配置时默认只接受 `1m` 请求。即使客户端已将原图压缩到约 1.65MB，也会在进入
API 前被拒绝并返回 HTTP 413 HTML 页面；后端的图片压缩和 JSON 异常处理都不会执行。
小程序应先判断 HTTP 状态，再解析成功响应，不能把错误页当成 JSON。

修改前备份实际站点配置（不要使用文档模板覆盖现有站点）：

```bash
cp -p /etc/nginx/conf.d/campusrunner.conf \
  /etc/nginx/conf.d/campusrunner.conf.bak-$(date +%Y%m%d-%H%M%S)
```

修改后执行 `nginx -t`，通过后才执行 `systemctl reload nginx`。验证包括：主页和公开
数据库探针 `/admin/api/banner/getList/0` 返回 HTTP 200 且 `code=1`；对用户和管理端
上传入口发送大于 1MB、小于 10MB 的请求，不再被 Nginx 返回 413；大于 20MB 的请求
仍返回 413。
不带登录凭据的合规大小上传请求应返回 401，这只证明代理限制已修复，不能替代已登录
用户的实际上传验收。最后在微信开发者工具和真机重新选择原图，确认能上传并预览。

如验证失败，恢复本次备份到原配置路径，重新执行 `nginx -t` 并平滑重载；不需要重启
Java 或回滚数据库。恢复旧的 1MB 限制也会恢复大图上传故障。

## 后端要求

Spring Boot 只监听本机 HTTP `8080`，HTTPS 由 Nginx 统一处理：

```yaml
server:
  port: 8080
  ssl:
    enabled: false
```

## SELinux 和防火墙

只有系统启用了 SELinux 时才需要：

```bash
sudo setsebool -P httpd_can_network_connect 1
sudo chcon -R -t httpd_sys_content_t /var/www/campusrunner/admin
```

只有 firewalld 正在运行时才需要：

```bash
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

## 检查和验证

```bash
sudo nginx -t
sudo systemctl reload nginx

curl -I https://www.campusrunner.top/
curl -i https://www.campusrunner.top/admin/api/banner/getList/0
sudo tail -n 200 /var/log/nginx/error.log
```
