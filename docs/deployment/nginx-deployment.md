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
curl -i https://www.campusrunner.top/api/second-hand/categories
sudo tail -n 200 /var/log/nginx/error.log
```
