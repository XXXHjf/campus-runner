# 部署Nginx服务器

```bash
# 创建目录
mkdir -p /var/www/campusrunner/admin/

# 将前端项目 build 后 dist 文件夹里的所有文件移动到上面的目录：/var/www/campusrunner/admin/

# 创建目录放置 ssl 密钥文件
mkdir -p /etc/nginx/ssl/campusrunner/
# 阿里云下载 nginx 适配的密钥文件，日后统一将这两个文件命名为 cert.pem 和 cert.key，并移动到上面的目录：/etc/nginx/ssl/campusrunner/

# 创建 nginx 配置文件，记住路径
vi /etc/nginx/conf.d/campusrunner.conf


# 查看端口占用情况
sudo ss -lntp | egrep ':80|:443|:8080' || true
# 现在 Java 在用 443，先杀掉
kill -9 19459


# 安装并启动 nginx
sudo yum install -y nginx
sudo systemctl enable --now nginx

# 创建建议的目录，见上述步骤，已经完成了

# 设置私钥权限，并查看权限（我感觉麻烦就没限制，现在是所有用户都可以读取）
sudo chmod 600 /etc/nginx/ssl/campusrunner/cert.key
sudo ls -la /etc/nginx/ssl/campusrunner

# PS：下面这两步也没执行，因为服务器的 SELinux 压根没开
### 1. 允许 Nginx 反向代理到本机端口
sudo setsebool -P httpd_can_network_connect 1
### 2. 让 Nginx 能读你的静态目录
sudo chcon -R -t httpd_sys_content_t /var/www/campusrunner/admin

#写入 Nginx 的配置：
### 详细见地下的代码块

# 检查 nginx 配置并重载
sudo nginx -t
sudo systemctl reload nginx

# 放行防火墙（也不用，firewall没启动）
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload

# 验证正确性
### 1. 中后台页面（应该返回 200/304）
curl -I https://www.campusrunner.top/
### 2. API（应该不是 502）
curl -I https://www.campusrunner.top/api/example
### 3. 看 nginx 错误日志（出问题第一时间看这个）
sudo tail -n 200 /var/log/nginx/error.log
```

## 删除之前测试用的配置

```bash
# 查看有没有其他无用的配置文件
cd /etc/nginx/conf.d
ls

# 直接删掉
sudo rm /etc/nginx/conf.d/frontend_admin.conf
```

## Nginx 配置

```bash
# ---------------------------------------------------
# 1. HTTP 自动跳转 HTTPS (80 端口)
# ---------------------------------------------------
server {
    listen       80;
    server_name  www.campusrunner.top campusrunner.top;

    # 所有的 http 请求都永久重定向到 https
    return 301 https://$host$request_uri;
}

# ---------------------------------------------------
# 2. HTTPS 主配置 (443 端口)
# ---------------------------------------------------
server {
    listen       443 ssl http2;
    server_name  www.campusrunner.top campusrunner.top;

    # SSL 证书路径 (请确保文件已存在)
    ssl_certificate      /etc/nginx/ssl/campusrunner/cert.pem;
    ssl_certificate_key  /etc/nginx/ssl/campusrunner/cert.key;

    # SSL 安全优化设置
    ssl_protocols        TLSv1.2 TLSv1.3;
    ssl_session_timeout  10m;
    ssl_session_cache    shared:SSL:10m;
    ssl_ciphers          HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers  on;

    # [前端配置] React 静态资源根目录
    root  /var/www/campusrunner/admin;
    index index.html;

    # -----------------------------------------------
    # 路由 A：后端 API 接口转发 (反向代理)
    # -----------------------------------------------
    # 小程序 API
    location /api/ {
      proxy_pass http://127.0.0.1:8080;   # 注意：不加尾部 /
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 中后台 API
    location /admin/api/ {
      proxy_pass http://127.0.0.1:8080;   # 同样不加尾部 /
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # -----------------------------------------------
    # 路由 B：前端静态资源与单页应用 (SPA) 路由
    # -----------------------------------------------
    location / {
        # 核心配置：尝试找文件，找不到就回退到 index.html
        # 这解决了 React Router (如 /dashboard) 刷新页面 404 的问题
        try_files $uri $uri/ /index.html;
    }

    # -----------------------------------------------
    # 路由 C：静态资源缓存优化 (可选)
    # -----------------------------------------------
    location ~* \.(js|css|png|jpg|jpeg|gif|svg|ico|woff2?)$ {
        expires 7d;               # 浏览器缓存 7 天
        access_log off;           # 静态资源不记录访问日志，节省磁盘IO
        add_header Cache-Control "public";
        try_files $uri =404;
    }

    # -----------------------------------------------
    # 安全设置：禁止访问敏感文件
    # -----------------------------------------------
    location ~ /\. {
        deny all;
    }
}
```

## 后端改动

```yaml
server:
  port: 8080
  ssl:
    enabled: false
```

