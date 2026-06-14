# 中间件文档

## 目录说明

本目录存放中间件使用文档，包括 Redis、MySQL、RabbitMQ、Elasticsearch 等。

## 中间件清单

### 1. Redis

#### 1.1 概述

- **版本**: Redis 6.0+
- **用途**: 缓存、会话管理、分布式锁、消息队列
- **连接方式**: Spring Data Redis

#### 1.2 配置

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: 
    database: 0
    timeout: 10000
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

#### 1.3 使用场景

| 场景 | Key 格式 | 过期时间 | 说明 |
|------|---------|---------|------|
| 用户会话 | `user:session:{token}` | 2小时 | Sa-Token 会话 |
| 验证码 | `captcha:{key}` | 5分钟 | 登录验证码 |
| 分布式锁 | `lock:{business}:{id}` | 10秒 | 防止重复操作 |
| 缓存 | `cache:{type}:{id}` | 30分钟 | 数据缓存 |
| 限流 | `rate:{ip}:{path}` | 1分钟 | 接口限流 |

#### 1.4 常用命令

```bash
# 连接 Redis
redis-cli -h localhost -p 6379

# 查看所有键
KEYS *

# 查看键过期时间
TTL key

# 清空当前数据库
FLUSHDB
```

### 2. MySQL

#### 2.1 概述

- **版本**: MySQL 8.0
- **用途**: 主数据库
- **连接方式**: Spring Data JPA / MyBatis

#### 2.2 配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/code_nest?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 30000
      max-lifetime: 1800000
      connection-timeout: 30000
```

#### 2.3 连接池配置

| 参数 | 默认值 | 说明 |
|------|--------|------|
| minimum-idle | 5 | 最小空闲连接数 |
| maximum-pool-size | 20 | 最大连接数 |
| idle-timeout | 30000 | 空闲连接超时时间(ms) |
| max-lifetime | 1800000 | 连接最大生命周期(ms) |
| connection-timeout | 30000 | 连接超时时间(ms) |

#### 2.4 慢查询配置

```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;
SET GLOBAL slow_query_log_file = '/var/log/mysql/slow.log';
```

### 3. RabbitMQ

#### 3.1 概述

- **版本**: RabbitMQ 3.8+
- **用途**: 消息队列、异步处理
- **连接方式**: Spring AMQP

#### 3.2 配置

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    listener:
      simple:
        concurrency: 10
        max-concurrency: 20
        prefetch: 10
```

#### 3.3 队列清单

| 队列名 | 说明 | 消费者 |
|--------|------|--------|
| notification.queue | 通知消息队列 | NotificationConsumer |
| email.queue | 邮件发送队列 | EmailConsumer |
| points.queue | 积分变动队列 | PointsConsumer |
| log.queue | 日志记录队列 | LogConsumer |

#### 3.4 交换机清单

| 交换机名 | 类型 | 说明 |
|---------|------|------|
| notification.exchange | direct | 通知交换机 |
| email.exchange | direct | 邮件交换机 |
| points.exchange | direct | 积分交换机 |
| log.exchange | fanout | 日志交换机 |

### 4. Elasticsearch

#### 4.1 概述

- **版本**: Elasticsearch 7.0+
- **用途**: 全文搜索、日志分析
- **连接方式**: Spring Data Elasticsearch

#### 4.2 配置

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
    username: 
    password: 
```

#### 4.3 索引清单

| 索引名 | 说明 | 主要字段 |
|--------|------|---------|
| blog_article | 博客文章索引 | title, content, tags |
| community_post | 社区帖子索引 | title, content, tags |
| interview_question | 面试题索引 | title, content, category |

### 5. Nginx

#### 5.1 概述

- **版本**: Nginx 1.20+
- **用途**: 反向代理、负载均衡、静态资源
- **配置文件**: nginx.conf

#### 5.2 配置示例

```nginx
server {
    listen 80;
    server_name example.com;
    
    # 前端静态资源
    location / {
        root /usr/share/nginx/html/user;
        try_files $uri $uri/ /index.html;
    }
    
    # 后端 API 代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
    
    # 静态文件缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

### 6. Docker

#### 6.1 概述

- **版本**: Docker 20.10+
- **用途**: 容器化部署
- **配置文件**: docker-compose.yml

#### 6.2 配置示例

```yaml
version: '3.8'
services:
  app:
    image: code-nest:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    depends_on:
      - mysql
      - redis
      
  mysql:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      - MYSQL_ROOT_PASSWORD=root
      - MYSQL_DATABASE=code_nest
      
  redis:
    image: redis:6.0
    ports:
      - "6379:6379"
```

### 7. Prometheus

#### 7.1 概述

- **版本**: Prometheus 2.0+
- **用途**: 监控指标收集
- **配置文件**: prometheus.yml

#### 7.2 配置示例

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'code-nest'
    metrics_path: '/api/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

### 8. Grafana

#### 8.1 概述

- **版本**: Grafana 8.0+
- **用途**: 监控可视化
- **默认端口**: 3000

#### 8.2 配置

- **默认账号**: admin
- **默认密码**: admin123
- **数据源**: Prometheus

## 最佳实践

### 1. Redis

- 合理设置过期时间，避免内存溢出
- 使用 Pipeline 批量操作，减少网络开销
- 避免使用 KEYS 命令，使用 SCAN 替代
- 使用连接池，避免频繁创建连接

### 2. MySQL

- 合理创建索引，避免全表扫描
- 使用连接池，避免频繁创建连接
- 定期优化表，清理碎片
- 开启慢查询日志，分析性能问题

### 3. RabbitMQ

- 合理设置队列长度，避免消息堆积
- 使用死信队列处理失败消息
- 设置消息持久化，避免消息丢失
- 使用消息确认机制，确保消息处理成功

### 4. Elasticsearch

- 合理设置分片数，避免过多分片
- 使用 Bulk API 批量操作，提高性能
- 定期清理过期索引，释放存储空间
- 使用别名管理索引，方便切换

## 故障排查

### 1. Redis

```bash
# 查看 Redis 状态
redis-cli info

# 查看慢查询
redis-cli slowlog get 10

# 查看内存使用
redis-cli info memory
```

### 2. MySQL

```sql
-- 查看连接数
SHOW STATUS LIKE 'Threads_connected';

-- 查看慢查询
SHOW VARIABLES LIKE 'slow_query_log';

-- 查看表状态
SHOW TABLE STATUS;
```

### 3. RabbitMQ

```bash
# 查看队列状态
rabbitmqctl list_queues

# 查看连接状态
rabbitmqctl list_connections

# 查看交换机状态
rabbitmqctl list_exchanges
```

## 版本历史

| 版本 | 日期 | 更新内容 | 作者 |
|------|------|---------|------|
| v1.0.0 | 2026-05-29 | 初始版本 | AI Assistant |
