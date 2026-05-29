# Prometheus + Grafana 监控部署指南

## 一、前置准备

### 1.1 环境要求
- Docker 或 Docker Compose（推荐）
- 或者直接下载二进制文件运行

### 1.2 端口规划
- **应用端口**：9999（Code-Nest应用）
- **Prometheus**：9090（默认）
- **Grafana**：3000（默认）

---

## 二、应用端配置（已完成✅）

### 2.1 依赖配置
已在 `xiaou-common/pom.xml` 中添加：
```xml
<!-- Spring Boot Actuator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer Prometheus Registry -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### 2.2 应用配置
已在 `application.yml` 中配置暴露Prometheus端点

### 2.3 验证端点
启动应用后，访问以下端点验证：

```bash
# 健康检查
curl http://localhost:9999/api/actuator/health

# Prometheus指标
curl http://localhost:9999/api/actuator/prometheus

# 所有可用端点
curl http://localhost:9999/api/actuator
```

---

## 三、Prometheus 部署

### 方式一：Docker Compose 部署（推荐）

#### 3.1 创建配置文件

在项目根目录创建 `docker/monitoring/` 目录：

```bash
mkdir -p docker/monitoring
```

**创建 `docker/monitoring/prometheus.yml`：**

```yaml
# Prometheus 配置文件
global:
  # 全局采集间隔（默认1分钟）
  scrape_interval: 15s
  # 采集超时时间
  scrape_timeout: 10s
  # 评估规则间隔
  evaluation_interval: 15s
  # 添加到所有时间序列的外部标签
  external_labels:
    monitor: 'code-nest-monitor'
    env: 'dev'

# 告警管理器配置（可选）
# alerting:
#   alertmanagers:
#     - static_configs:
#         - targets: ['alertmanager:9093']

# 规则文件（用于告警规则）
# rule_files:
#   - 'alert_rules.yml'

# 采集配置
scrape_configs:
  # Prometheus 自身监控
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # Code-Nest 应用监控
  - job_name: 'code-nest'
    # 指标采集路径
    metrics_path: '/api/actuator/prometheus'
    # 采集间隔（覆盖全局配置）
    scrape_interval: 10s
    static_configs:
      # Windows 本机：使用 host.docker.internal
      # Linux 本机：使用 172.17.0.1 或 宿主机IP
      - targets: ['host.docker.internal:9999']
        labels:
          application: 'code-nest'
          env: 'dev'
```

**创建 `docker/monitoring/docker-compose.yml`：**

```yaml
version: '3.8'

services:
  # Prometheus 服务
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    restart: unless-stopped
    ports:
      - "9090:9090"
    volumes:
      # 挂载配置文件
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      # 持久化存储（可选）
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      # 数据保留时间（默认15天）
      - '--storage.tsdb.retention.time=30d'
      # 开启 Web 生命周期 API
      - '--web.enable-lifecycle'
    networks:
      - monitoring

  # Grafana 服务
  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    restart: unless-stopped
    ports:
      - "3000:3000"
    environment:
      # 管理员账号密码
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin123
      # 允许匿名访问（可选）
      - GF_AUTH_ANONYMOUS_ENABLED=false
      # 设置时区
      - TZ=Asia/Shanghai
    volumes:
      # 持久化存储
      - grafana-data:/var/lib/grafana
      # 自定义配置（可选）
      # - ./grafana.ini:/etc/grafana/grafana.ini
    depends_on:
      - prometheus
    networks:
      - monitoring

volumes:
  prometheus-data:
  grafana-data:

networks:
  monitoring:
    driver: bridge
```

#### 3.2 启动服务

```bash
cd docker/monitoring
docker-compose up -d
```

#### 3.3 查看日志

```bash
# 查看 Prometheus 日志
docker logs -f prometheus

# 查看 Grafana 日志
docker logs -f grafana
```

#### 3.4 访问界面

- **Prometheus**：http://localhost:9090
- **Grafana**：http://localhost:3000（默认账号：admin / admin123）

---

### 方式二：二进制文件部署

#### 3.1 下载 Prometheus

访问：https://prometheus.io/download/

下载适合你操作系统的版本，例如 Windows：
```
prometheus-2.x.x.windows-amd64.zip
```

#### 3.2 配置 Prometheus

解压后，编辑 `prometheus.yml`（参考方式一的配置，但targets改为 `localhost:9999`）

#### 3.3 启动 Prometheus

```bash
# Windows
prometheus.exe --config.file=prometheus.yml

# Linux/Mac
./prometheus --config.file=prometheus.yml
```

访问：http://localhost:9090

---

## 四、Grafana 配置

### 4.1 登录 Grafana

1. 访问 http://localhost:3000
2. 默认账号：`admin` / `admin123`
3. 首次登录可能需要修改密码

### 4.2 添加 Prometheus 数据源

1. 点击左侧菜单 **Configuration** → **Data Sources**
2. 点击 **Add data source**
3. 选择 **Prometheus**
4. 配置：
   - **Name**: Prometheus
   - **URL**: 
     - Docker部署：`http://prometheus:9090`
     - 本地部署：`http://localhost:9090`
5. 点击 **Save & Test**，确认连接成功

### 4.3 导入 Dashboard

#### 推荐的现成 Dashboard：

**1. Spring Boot 2.1 Statistics (ID: 11378)**
   - 全面的Spring Boot应用监控
   - 包含JVM、HTTP、数据库连接池等

**2. JVM (Micrometer) (ID: 4701)**
   - 专注于JVM指标
   - 内存、GC、线程等详细监控

**3. Spring Boot Statistics (ID: 6756)**
   - 轻量级Spring Boot监控
   - 适合快速查看应用状态

#### 导入步骤：

1. 点击左侧菜单 **+** → **Import**
2. 在 **Import via grafana.com** 输入 Dashboard ID（如：11378）
3. 点击 **Load**
4. 选择 Prometheus 数据源
5. 点击 **Import**

### 4.4 自定义 Dashboard

你也可以创建自定义Dashboard，监控特定业务指标。

---

## 五、监控指标说明

### 5.1 JVM 指标

| 指标名称 | 说明 |
|---------|------|
| `jvm_memory_used_bytes` | JVM内存使用量 |
| `jvm_memory_max_bytes` | JVM最大内存 |
| `jvm_gc_pause_seconds` | GC暂停时间 |
| `jvm_threads_live_threads` | 活动线程数 |
| `process_cpu_usage` | CPU使用率 |

### 5.2 HTTP 指标

| 指标名称 | 说明 |
|---------|------|
| `http_server_requests_seconds_count` | HTTP请求总数 |
| `http_server_requests_seconds_sum` | HTTP请求总耗时 |
| `http_server_requests_seconds_max` | HTTP请求最大耗时 |

### 5.3 数据库连接池指标（HikariCP/Druid）

| 指标名称 | 说明 |
|---------|------|
| `hikaricp_connections_active` | 活动连接数 |
| `hikaricp_connections_idle` | 空闲连接数 |
| `hikaricp_connections_pending` | 等待连接数 |
| `hikaricp_connections_timeout_total` | 连接超时总数 |

### 5.4 自定义业务指标（可扩展）

在代码中使用 Micrometer 创建自定义指标：

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class BusinessMetricsService {
    
    private final Counter userLoginCounter;
    
    public BusinessMetricsService(MeterRegistry registry) {
        // 创建计数器
        this.userLoginCounter = Counter.builder("user.login.count")
            .description("用户登录次数")
            .tag("type", "success")
            .register(registry);
    }
    
    public void recordLogin() {
        userLoginCounter.increment();
    }
}
```

---

## 六、告警配置（可选）

### 6.1 Prometheus 告警规则

创建 `docker/monitoring/alert_rules.yml`：

```yaml
groups:
  - name: code-nest-alerts
    interval: 30s
    rules:
      # JVM 内存使用率告警
      - alert: HighMemoryUsage
        expr: (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "JVM堆内存使用率过高"
          description: "{{ $labels.application }} 堆内存使用率超过90%，当前值：{{ $value }}"
      
      # HTTP 错误率告警
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "HTTP 5xx错误率过高"
          description: "{{ $labels.application }} 5xx错误率超过5%"
      
      # 数据库连接池告警
      - alert: DatabaseConnectionPoolExhausted
        expr: hikaricp_connections_pending > 0
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "数据库连接池耗尽"
          description: "{{ $labels.application }} 有{{ $value }}个请求在等待数据库连接"
```

### 6.2 Grafana 告警

在 Grafana Dashboard 中也可以直接配置告警：
1. 编辑 Panel
2. 点击 **Alert** 选项卡
3. 配置告警条件和通知渠道

---

## 七、常见问题

### Q1: Prometheus 无法连接到应用？

**A**: 检查以下几点：
1. 应用是否正常启动
2. 端口是否正确（9999）
3. Docker网络配置：
   - Windows: 使用 `host.docker.internal:9999`
   - Linux: 使用 `172.17.0.1:9999` 或宿主机IP
4. 防火墙是否拦截

### Q2: Grafana Dashboard 没有数据？

**A**: 
1. 检查 Prometheus 数据源连接是否正常
2. 检查 Prometheus 是否成功采集到数据（访问 http://localhost:9090/targets）
3. 检查 Dashboard 的时间范围选择
4. 确认应用端点有数据输出

### Q3: 如何查看 Prometheus 是否采集到数据？

**A**: 
1. 访问 http://localhost:9090
2. 在搜索框输入指标名，如：`jvm_memory_used_bytes`
3. 点击 **Execute** 查看数据

### Q4: 生产环境需要注意什么？

**A**:
1. **安全性**：
   - 限制 Actuator 端点暴露（只暴露必要的端点）
   - 配置访问认证（Spring Security）
   - 使用 HTTPS
2. **性能**：
   - 调整 Prometheus 采集间隔（避免频繁采集）
   - 配置合理的数据保留时间
3. **持久化**：
   - 配置数据卷持久化存储
   - 定期备份重要数据

---

## 八、验证清单

部署完成后，请逐项验证：

- [ ] 应用端点可访问：http://localhost:9999/api/actuator/prometheus
- [ ] Prometheus UI 可访问：http://localhost:9090
- [ ] Prometheus Targets 状态为 UP：http://localhost:9090/targets
- [ ] Grafana 可登录：http://localhost:3000
- [ ] Grafana 数据源连接成功
- [ ] Dashboard 显示数据正常

---

## 九、参考资料

- **Prometheus 官方文档**：https://prometheus.io/docs/
- **Grafana 官方文档**：https://grafana.com/docs/
- **Micrometer 文档**：https://micrometer.io/docs/
- **Spring Boot Actuator**：https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html
- **Grafana Dashboard 市场**：https://grafana.com/grafana/dashboards/

---

## 十、下一步优化

1. **集成 AlertManager**：实现告警通知（邮件、钉钉、企微等）
2. **集成链路追踪**：使用 SkyWalking 或 Zipkin
3. **日志聚合**：使用 ELK 或 Loki
4. **服务发现**：Kubernetes 环境下自动发现服务
5. **多环境监控**：区分开发、测试、生产环境

---

**部署完成后，你将拥有一套完整的应用监控体系！** 🎉

