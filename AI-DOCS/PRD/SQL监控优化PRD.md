# SQL监控功能优化 PRD

## 一、背景与现状

### 1.1 当前实现
项目目前采用自研的SQL监控方案，主要包括：

**后端模块（xiaou-monitor）：**
- `MonitorConfig.java` - 监控配置类
- `SqlInterceptor.java` - MyBatis SQL拦截器
- `SqlCallTreeContext.java` - SQL调用树上下文管理
- `SqlTreeController.java` - SQL监控API接口
- 相关配置在 `application.yml` 中（xiaou.monitor.*）

**前端模块：**
- `vue3-admin-front/src/views/system/monitor/SqlMonitor.vue` - SQL监控可视化界面
- `vue3-admin-front/src/api/monitor.js` - 监控API调用

**功能特性：**
- SQL执行时间监控
- 慢SQL统计与分析
- SQL调用树追踪
- 错误SQL捕获
- 实时统计与可视化

### 1.2 存在的问题

#### 1.2.1 架构层面
- **维护成本高**：自研方案需要持续投入人力维护和优化
- **功能单一**：仅限SQL监控，无法覆盖JVM、Redis、接口等其他监控需求
- **扩展性差**：无法与其他监控系统集成，形成数据孤岛
- **性能开销**：MyBatis拦截器在每次SQL执行时都会介入，存在性能损耗

#### 1.2.2 运维层面
- **缺乏告警机制**：无法设置阈值告警，需要人工查看
- **历史数据有限**：数据存储在内存中，重启即丢失
- **可视化能力弱**：缺少丰富的图表和趋势分析
- **多实例监控困难**：无法统一监控多个应用实例

#### 1.2.3 业务层面
- **排查效率低**：问题发生后才能看到，无法提前预警
- **数据不完整**：缺少请求链路、系统资源等关联数据
- **团队协作难**：开发、运维、DBA需要不同的监控视角

## 二、解决方案

### 2.1 方案选择：Prometheus + Grafana

采用业界成熟的监控方案替代自研SQL监控：

**核心组件：**
- **Prometheus**：指标采集与存储
- **Grafana**：数据可视化与告警
- **Spring Boot Actuator**：应用指标暴露
- **Micrometer**：指标统一抽象层

### 2.2 方案优势

#### 2.2.1 功能更强大
- **全面监控**：涵盖JVM、数据库连接池、HTTP请求、缓存、线程池等
- **SQL监控**：通过HikariCP/Druid连接池指标监控SQL性能
- **链路追踪**：可集成SkyWalking/Zipkin实现分布式链路追踪
- **自定义指标**：可根据业务需求自定义监控指标

#### 2.2.2 运维更便捷
- **持久化存储**：Prometheus提供时序数据库，数据不丢失
- **丰富的告警**：支持阈值告警、趋势告警、多级告警策略
- **多实例聚合**：自动发现和监控多个应用实例
- **标准化**：符合云原生监控标准，易于迁移和扩展

#### 2.2.3 成本更低
- **开源免费**：Prometheus和Grafana都是开源方案
- **社区成熟**：有大量现成的Dashboard和告警模板
- **维护成本低**：无需维护自研代码，专注于业务开发

### 2.3 技术实现

#### 2.3.1 后端改造
```yaml
# 添加依赖
spring-boot-starter-actuator
micrometer-registry-prometheus

# 配置暴露指标端点
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

#### 2.3.2 监控内容
- **JVM指标**：内存、GC、线程、CPU使用率
- **数据库指标**：连接池状态、活动连接数、慢查询
- **HTTP指标**：请求量、响应时间、错误率
- **业务指标**：自定义计数器、直方图、摘要

#### 2.3.3 Grafana Dashboard
- 使用现成的Spring Boot Dashboard模板（ID: 4701、11378等）
- 可视化SQL连接池状态、慢查询统计
- 设置告警规则（如：慢SQL超过阈值、连接池耗尽等）

## 三、实施计划

### 3.1 移除内容（第一阶段）

#### 3.1.1 后端移除
- [ ] 移除 `xiaou-monitor` 模块
- [ ] 从 `xiaou-application/pom.xml` 中移除 monitor 依赖
- [ ] 删除 `application.yml` 中的 `xiaou.monitor` 配置段
- [ ] 清理相关的日志配置

#### 3.1.2 前端移除
- [ ] 删除 `vue3-admin-front/src/views/system/monitor/SqlMonitor.vue`
- [ ] 删除 `vue3-admin-front/src/api/monitor.js`
- [ ] 从左侧导航菜单中移除SQL监控入口

### 3.2 Prometheus集成（第二阶段）

#### 3.2.1 后端集成
- [ ] 添加 `spring-boot-starter-actuator` 依赖
- [ ] 添加 `micrometer-registry-prometheus` 依赖
- [ ] 配置 `application.yml` 开启metrics端点
- [ ] （可选）添加自定义业务指标

#### 3.2.2 基础设施部署
- [ ] 部署 Prometheus 服务器
- [ ] 配置 Prometheus 采集规则
- [ ] 部署 Grafana 服务器
- [ ] 配置 Grafana 数据源
- [ ] 导入 Spring Boot Dashboard 模板

#### 3.2.3 告警配置
- [ ] 配置 Prometheus AlertManager
- [ ] 设置慢SQL告警规则
- [ ] 设置连接池告警规则
- [ ] 配置告警通知渠道（邮件/钉钉/企微）

### 3.3 时间规划
- **第一阶段（1天）**：移除现有SQL监控模块
- **第二阶段（2-3天）**：Prometheus + Grafana 部署与配置
- **验收测试（1天）**：功能验证与性能测试

## 四、影响范围

### 4.1 代码影响
- **删除模块**：xiaou-monitor（约500行代码）
- **删除前端页面**：SqlMonitor.vue（约800行代码）
- **配置文件**：application.yml 减少约60行配置

### 4.2 功能影响
- **短期**：移除后暂时失去SQL监控可视化界面
- **长期**：通过Grafana获得更强大的监控能力

### 4.3 数据影响
- **无历史数据丢失风险**：现有方案数据存储在内存，重启即丢失
- **新方案提供持久化存储**

### 4.4 用户影响
- **管理员**：需要切换到Grafana查看监控数据
- **开发人员**：获得更全面的应用性能数据
- **运维人员**：监控运维更加规范和高效

## 五、风险评估与应对

### 5.1 风险识别

| 风险项 | 风险等级 | 影响 | 应对措施 |
|--------|---------|------|----------|
| Prometheus部署失败 | 中 | 监控功能不可用 | 提前准备Docker镜像，准备回滚方案 |
| 指标采集遗漏 | 低 | 部分监控数据缺失 | 对比旧方案功能，确保指标完整性 |
| 告警配置错误 | 中 | 误报或漏报 | 先在测试环境验证告警规则 |
| 团队学习成本 | 低 | 短期使用不熟练 | 提供Grafana使用文档和培训 |

### 5.2 回滚方案
如果新方案出现严重问题，可快速回滚：
1. 保留 xiaou-monitor 模块代码到独立分支（backup/sql-monitor）
2. 如需回滚，恢复模块依赖和配置即可
3. 回滚时间预计：< 30分钟

## 六、验收标准

### 6.1 功能验收
- [ ] Prometheus 成功采集应用指标
- [ ] Grafana 能正常展示监控数据
- [ ] SQL连接池指标正常显示
- [ ] 慢SQL能够被识别和统计
- [ ] 告警功能正常工作

### 6.2 性能验收
- [ ] 应用启动时间无明显增加（< 5%）
- [ ] 接口响应时间无明显增加（< 2%）
- [ ] CPU和内存使用无明显增长（< 5%）

### 6.3 运维验收
- [ ] 监控数据持久化，重启不丢失
- [ ] 支持多实例监控
- [ ] 告警通知正常送达
- [ ] Dashboard 加载速度 < 3秒

## 七、后续优化方向

### 7.1 短期（1个月内）
- 完善自定义业务指标
- 优化Grafana Dashboard布局
- 建立监控数据分析制度

### 7.2 中期（3个月内）
- 集成分布式链路追踪（SkyWalking）
- 增加日志聚合分析（ELK）
- 建立APM性能管理体系

### 7.3 长期（6个月内）
- 构建完整的可观测性平台（Logs + Metrics + Traces）
- 实现智能告警与故障自愈
- 建立性能基准与容量规划

## 八、总结

将自研SQL监控替换为Prometheus + Grafana方案，是技术架构向标准化、云原生方向演进的重要一步。

**核心价值：**
1. **降本增效**：减少维护成本，提升监控能力
2. **标准化**：采用业界标准方案，易于团队协作
3. **可扩展**：为后续APM和可观测性建设打下基础
4. **生态丰富**：可集成更多开源监控工具

**建议：**
优先执行第一阶段移除工作，同步进行Prometheus环境准备，确保监控能力平滑过渡。

