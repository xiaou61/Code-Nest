# 部署指南

## 目录说明

本目录存放项目部署相关文档，包括各种部署方式的详细指南。

## 部署方式

### 1. 本地部署

#### 1.1 环境要求

| 软件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | Java开发环境 |
| Maven | 3.6+ | 构建工具 |
| Node.js | 16+ | 前端构建 |
| MySQL | 8.0+ | 数据库 |
| Redis | 6.0+ | 缓存 |
| Nginx | 1.20+ | 反向代理 |

#### 1.2 部署步骤

**1. 数据库准备**

```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE code_nest CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入数据
mysql -u root -p code_nest < sql/MySql/code_nest.sql
mysql -u root -p code_nest < sql/MySql/code_nest_data.sql
```

**2. 后端部署**

```bash
# 克隆项目
git clone https://github.com/xiaou/Code-Nest.git
cd Code-Nest

# 修改配置
vim xiaou-application/src/main/resources/application-prod.yml

# 构建项目
mvn clean package -DskipTests

# 启动服务
java -jar xiaou-application/target/xiaou-application-2.2.2.jar --spring.profiles.active=prod
```

**3. 前端部署**

```bash
# 用户端
cd vue3-user-front
npm install
npm run build

# 管理端
cd vue3-admin-front
npm install
npm run build
```

**4. Nginx 配置**

```nginx
server {
    listen 80;
    server_name example.com;
    
    # 用户端
    location / {
        root /usr/share/nginx/html/user;
        try_files $uri $uri/ /index.html;
    }
    
    # 管理端
    location /admin {
        alias /usr/share/nginx/html/admin;
        try_files $uri $uri/ /admin/index.html;
    }
    
    # 后端 API
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### 2. Docker 部署

#### 2.1 环境要求

| 软件 | 版本 | 说明 |
|------|------|------|
| Docker | 20.10+ | 容器引擎 |
| Docker Compose | 2.0+ | 容器编排 |

#### 2.2 部署步骤

**1. 准备配置文件**

```bash
# 创建配置目录
mkdir -p config

# 复制配置文件
cp xiaou-application/src/main/resources/application-prod.yml config/
```

**2. 启动服务**

```bash
# 进入 docker 目录
cd docker

# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps
```

**3. 停止服务**

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据
docker-compose down -v
```

#### 2.3 Docker Compose 配置

```yaml
version: '3.8'

services:
  # 后端服务
  app:
    build:
      context: ..
      dockerfile: docker/Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - MYSQL_HOST=mysql
      - REDIS_HOST=redis
    depends_on:
      - mysql
      - redis
    restart: unless-stopped
    
  # 用户端
  user-front:
    build:
      context: ../vue3-user-front
      dockerfile: Dockerfile
    ports:
      - "3001:80"
    restart: unless-stopped
    
  # 管理端
  admin-front:
    build:
      context: ../vue3-admin-front
      dockerfile: Dockerfile
    ports:
      - "3000:80"
    restart: unless-stopped
    
  # MySQL
  mysql:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      - MYSQL_ROOT_PASSWORD=root
      - MYSQL_DATABASE=code_nest
    volumes:
      - mysql-data:/var/lib/mysql
      - ../sql/MySql:/docker-entrypoint-initdb.d
    restart: unless-stopped
    
  # Redis
  redis:
    image: redis:6.0
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    restart: unless-stopped

volumes:
  mysql-data:
  redis-data:
```

### 3. Kubernetes 部署

#### 3.1 环境要求

| 软件 | 版本 | 说明 |
|------|------|------|
| Kubernetes | 1.20+ | 容器编排 |
| kubectl | 1.20+ | K8s命令行 |
| Helm | 3.0+ | 包管理器 |

#### 3.2 部署步骤

**1. 创建命名空间**

```bash
kubectl create namespace code-nest
```

**2. 部署配置**

```bash
# 部署配置
kubectl apply -f k8s/configmap.yaml -n code-nest
kubectl apply -f k8s/secret.yaml -n code-nest

# 部署服务
kubectl apply -f k8s/mysql.yaml -n code-nest
kubectl apply -f k8s/redis.yaml -n code-nest
kubectl apply -f k8s/app.yaml -n code-nest
kubectl apply -f k8s/frontend.yaml -n code-nest
```

**3. 查看状态**

```bash
# 查看 Pod 状态
kubectl get pods -n code-nest

# 查看服务状态
kubectl get svc -n code-nest

# 查看日志
kubectl logs -f deployment/code-nest-app -n code-nest
```

#### 3.3 K8s 配置示例

**Deployment 配置**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: code-nest-app
  namespace: code-nest
spec:
  replicas: 2
  selector:
    matchLabels:
      app: code-nest-app
  template:
    metadata:
      labels:
        app: code-nest-app
    spec:
      containers:
        - name: code-nest-app
          image: code-nest:latest
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
          resources:
            requests:
              memory: "512Mi"
              cpu: "500m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
```

**Service 配置**

```yaml
apiVersion: v1
kind: Service
metadata:
  name: code-nest-app
  namespace: code-nest
spec:
  selector:
    app: code-nest-app
  ports:
    - port: 8080
      targetPort: 8080
  type: ClusterIP
```

### 4. 云服务器部署

#### 4.1 阿里云部署

**1. 购买 ECS**

- 地域：华东1（杭州）
- 实例规格：ecs.c6.large（2核4GB）
- 操作系统：CentOS 7.9
- 带宽：5Mbps

**2. 环境配置**

```bash
# 安装 JDK
yum install -y java-17-openjdk

# 安装 MySQL
yum install -y mysql-server

# 安装 Redis
yum install -y redis

# 安装 Nginx
yum install -y nginx
```

**3. 部署应用**

```bash
# 上传代码
scp -r Code-Nest root@ecs-ip:/opt/

# 启动应用
cd /opt/Code-Nest
java -jar xiaou-application/target/xiaou-application-2.2.2.jar --spring.profiles.active=prod
```

#### 4.2 腾讯云部署

**1. 购买 CVM**

- 地域：广州
- 实例规格：S5.MEDIUM4（2核4GB）
- 操作系统：CentOS 7.6
- 带宽：5Mbps

**2. 部署步骤**

同阿里云部署步骤。

## 环境配置

### 1. 生产环境配置

```yaml
# application-prod.yml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/code_nest?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: ${MYSQL_PASSWORD}
    
  redis:
    host: localhost
    port: 6379
    password: ${REDIS_PASSWORD}
    
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: ${RABBITMQ_PASSWORD}

logging:
  level:
    root: INFO
    com.xiaou: INFO
```

### 2. 环境变量

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| MYSQL_PASSWORD | MySQL密码 | your_password |
| REDIS_PASSWORD | Redis密码 | your_password |
| RABBITMQ_PASSWORD | RabbitMQ密码 | your_password |
| JWT_SECRET | JWT密钥 | your_secret_key |

## 监控配置

### 1. Prometheus 配置

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'code-nest'
    metrics_path: '/api/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

### 2. Grafana 配置

- **数据源**: Prometheus
- **仪表盘**: 导入 Spring Boot 仪表盘
- **告警**: 配置邮件告警

## 备份策略

### 1. 数据库备份

```bash
# 每日备份
mysqldump -u root -p code_nest > backup/code_nest_$(date +%Y%m%d).sql

# 保留最近7天备份
find backup/ -name "*.sql" -mtime +7 -delete
```

### 2. 文件备份

```bash
# 备份上传文件
tar -czf backup/uploads_$(date +%Y%m%d).tar.gz uploads/

# 备份配置文件
tar -czf backup/config_$(date +%Y%m%d).tar.gz config/
```

## 故障排查

### 1. 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 启动失败 | 端口被占用 | 修改端口或停止占用进程 |
| 连接数据库失败 | 数据库未启动 | 启动MySQL服务 |
| 内存不足 | JVM内存设置过大 | 调整JVM参数 |
| 前端访问404 | Nginx配置错误 | 检查Nginx配置 |

### 2. 日志查看

```bash
# 查看应用日志
tail -f logs/app.log

# 查看错误日志
tail -f logs/error.log

# 查看Nginx日志
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log
```

## 版本历史

| 版本 | 日期 | 更新内容 | 作者 |
|------|------|---------|------|
| v1.0.0 | 2026-05-29 | 初始版本 | AI Assistant |
