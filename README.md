# Code Nest

![Version](https://img.shields.io/badge/version-v2.1.2-blue.svg)
![Java](https://img.shields.io/badge/java-17-orange.svg)
![Spring Boot](https://img.shields.io/badge/spring%20boot-3.4.4-brightgreen.svg)
![Vue](https://img.shields.io/badge/vue-3.x-4fc08d.svg)
![License](https://img.shields.io/badge/license-MIT-green.svg)

## 📖 项目简介

Code Nest 是一个面向开发者的成长型社区与知识运营平台，采用 Spring Boot 3.4.4 + Vue3 + Vite 的前后端分离架构，后台整合 Sa-Token 权限、Redisson 缓存、MySQL 题库/内容系统以及 Prometheus 监控指标，提供包含题库、面试辅导、学习资产转化引擎、知识图谱、博客、代码工坊、在线简历、IM 聊天、积分激励与抽奖等在内的多模块能力。

### 平台定位

- **vue3-admin-front**：面向运营/管理员的后台，覆盖菜单/角色、内容审核、题库管理、版本追踪、任务配置、观测看板等能力。
- **vue3-user-front**：面向开发者的用户端，提供刷题、简历制作、动态广场、博客阅读、代码分享、学习资产沉淀、通知消息等场景。
- **xiaou-application**：多模块聚合的 Spring Boot API，整合 `xiaou-*` 业务模块，对外暴露统一的 `/api` 网关、鉴权、日志与监控。

## 🧯 v2.1.2 聊天室防刷增强版

`v2.1.2` 在 `v2.1.1` 的安全基线上继续补齐聊天室运行保护，重点解决高频刷屏、异常消息入库和前端失败态不可追踪的问题。

### 本次小版本完成

- **聊天室消息限流**：新增 Redis 固定窗口限流，默认每个用户 10 秒最多发送 8 条消息，可通过 `xiaou.chat.rate-limit.*` 调整。
- **输入中事件降噪**：`TYPING` 事件也纳入限流，避免输入状态广播在高频输入或异常脚本下冲击 WebSocket。
- **后端消息校验**：文本、图片消息按类型做空值、长度和图片 URL 协议校验，非法消息不再进入数据库。
- **前端失败态闭环**：后端返回限流或校验错误时，用户端会把本地乐观消息标记为失败并保留错误提示，方便重试或定位。
- **版本基线升级**：后端 Maven、管理端前端、用户端前端、README Jar/Docker 示例统一升级到 `v2.1.2`。

## 🛡️ v2.1.1 安全加固版

`v2.1.1` 基于 `v2.1.0` Growth Intelligence 稳定线做小版本安全与稳定性更新，重点收敛文件接口、WebSocket 握手与前端 HTML 渲染风险，不改变既有 AI 学习成长驾驶舱和 AI Runtime 质量中心的功能基线。

### 本次小版本完成

- **文件接口权限收口**：上传、删除、列表、存在性检查需要用户端或管理端登录；文件信息、下载与 URL 获取会按公开/私有状态校验访问权限。
- **WebSocket 握手票据化**：聊天室连接不再把长期 Token 放进 URL，改为通过登录态换取 60 秒一次性票据并在握手时消费。
- **聊天室实时链路补齐**：服务端补充 PONG 心跳响应和 TYPING 输入中事件，前端自适应心跳与输入状态提示可以闭环工作。
- **CORS 白名单配置化**：HTTP 与 WebSocket 的跨域来源从通配默认值改为可配置白名单，默认覆盖本地前端开发地址。
- **Markdown / v-html 净化**：用户端与管理端 Markdown 渲染统一接入 DOMPurify，并补齐通知、摸鱼内容、闪卡、JSON 工具等高风险 `v-html` 场景的转义或净化。

## 🚀 v2.1.0 Growth Intelligence

`v2.1.0` 继续沿着 AI Runtime 重构后的治理底座往上建设，把版本主题升级为 **Growth Intelligence**：用户端关注“开发者成长闭环”，管理端关注“AI Runtime 质量闭环”。

### 本次大版本新增

- **AI 学习成长驾驶舱 2.1**：用户端学习驾驶舱新增成长分、能力雷达、短板诊断、今日任务闭环和 AI 学习复盘，把 OJ、题库、闪卡、计划、积分串成一条可执行的成长路线。
- **AI Runtime 质量与治理中心**：管理端 AI 治理页升级为质量中心，新增质量分、Prompt/Schema/RAG/Regression/Runtime 覆盖矩阵、运行时洞察和风险队列。
- **治理数据复用现有接口**：继续沿用 `/user/learning-cockpit/overview` 和 `/admin/ai/governance/overview`，通过兼容字段增强前端体验，不额外增加部署复杂度。
- **README 与发布基线统一**：后端 Maven、管理端前端、用户端前端、Jar 命令与 Docker 镜像标签统一升级到 `v2.1.0`。

## 🚀 v2.0.0 AI Runtime 重构版

`v2.0.0` 是一次完整的 AI 基础设施重构版本，这次不是“在旧链路上继续补功能”，而是彻底移除了历史 Coze 运行链路，统一切换到新的可治理 AI Runtime。

### 本次重构完成了什么

- **彻底移除 Coze 链路**：删除 Coze SDK、Coze 配置、Coze 工具类与历史工作流依赖，主干不再保留旧运行路径。
- **统一 AI Runtime 底座**：`xiaou-ai` 以 `LangChain4j + LangGraph4j + LlamaIndex` 重构，形成“模型层 + Prompt 层 + Graph 编排层 + RAG 接入层 + 场景层”的清晰分层。
- **支持 OpenAI 兼容中转站**：当前后台 AI 配置测试与运行时接入面向 OpenAI Compatible API 设计，可直接对接中转站模型。
- **建立 Prompt 治理体系**：Prompt、RAG Query、Structured Output Schema 全部显式命名、版本化、可调试、可回归、可观测，不再把长提示词散落在业务代码里。
- **引入知识库 sidecar**：采用独立 `LlamaIndex` 服务承载知识库导入、检索、召回解释与文档管理，Java 主项目通过网关访问，不强耦合 Python 内部实现。
- **补齐 AI 管理后台**：新增 `AI 配置与观测` 页面，覆盖模型连通性测试、Prompt 在线试跑、RAG 检索调试、知识库文档导入/导出/删除、运行指标、黄金样例回归。
- **补齐质量治理闭环**：新增 AI 黄金样例回归、最近一次结果恢复、最近历史回看、Prompt/RAG/Schema catalog、运行观测与失败定位基础能力。

### 当前已重构接入的 AI 场景

- **社区摘要**：基于 `LangChain4j` 的结构化摘要生成。
- **模拟面试**：基于 `LangGraph4j` 的多轮问答、评分、追问、总结流程。
- **求职作战台**：JD 解析、简历匹配、行动计划、面试复盘等链路统一切到新 Runtime。
- **SQL 优化**：SQL 分析、重写、收益对比与组合流程统一切到新 Runtime，并可接入知识库辅助解释。

## ✨ 功能亮点

### 平台级能力

- **细粒度鉴权**：基于 Sa-Token 构建双端（管理员/用户）登录域、权限树、会话隔离与 Token 签名机制，支持并发登录及多场景鉴权。
- **智能题库体系**：`xiaou-interview` 内置题型分类、刷题记录、自动组卷、错题重练及统计报表，配合知识图谱实现学习闭环。
- **内容创作矩阵**：博客系统、动态广场、Bug 趣味墙、代码工坊与在线简历模块组合为全栈内容生产链路。
- **成长激励与运营**：积分体系、摸鱼任务、抽奖活动、版本发布墙、消息通知与活动打卡全面强化用户粘性。
- **内容转学习资产**：支持从博客、社区帖子、CodePen 作品、AI 模拟面试报告中提炼闪卡、练习清单、面试题草稿与知识节点候选，形成可审核、可复用的学习资产闭环。
- **统一资产管理**：`xiaou-filestorage` 负责文件/附件/导出作品上传，多种存储适配；`xiaou-resume` 支持简历模板、模块化数据、版本历史与多格式导出。
- **AI学习成长驾驶舱**：用户端聚合 OJ、题库、闪卡、计划、积分、排名与求职目标，提供成长分、能力雷达、短板诊断和今日任务闭环。
- **AI Runtime质量治理**：`xiaou-ai` 统一沉淀 AI Runtime 场景目录、配置状态、质量评分、覆盖矩阵、运行指标与风险队列，管理端可直接定位 Prompt、Schema、RAG 和运行时风险。
- **企业级可观测性**：SQL/行为审计、Redisson 限流、异步任务监控、Prometheus + Grafana 指标、Nginx/ Docker 部署脚本开箱即用。
- **统一 AI Runtime**：`xiaou-ai` 已重构为 `LangChain4j + LangGraph4j + LlamaIndex` 分层架构，AI Prompt 采用模板化、版本化和可观测治理，覆盖社区摘要、模拟面试、求职作战台与 SQL 优化等核心场景。

### 模块亮点

- **在线简历（xiaou-resume）**：拖拽式编辑、模板市场、版本比对、PDF/Word/HTML 导出与分享统计。
- **代码工坊（xiaou-codepen）**：内置前端 + 后端 + SQL 演示沙盒，支持作品发布、Fork、收藏及后台运营。
- **知识图谱（xiaou-knowledge）**：以节点关系呈现技术体系，结合 Pinia/G6 构建交互式知识网络。
- **动态/社区（xiaou-moment、xiaou-community）**：用户动态流、话题标签、点赞/收藏、内容推荐权重与违规审核。
- **通知 & IM（xiaou-notification、xiaou-chat）**：系统/私信/群聊，WebSocket 实时消息、撤回、封禁、敏感词检测。
- **OJ 判题（xiaou-oj）**：支持题目管理、在线判题、提交记录、赛事报名、实时榜单与赛后评分预估。
- **敏感词/风控（xiaou-sensitive、xiaou-sensitive-api）**：分词匹配、命中日志、策略管理、WebHook 回调。
- **版本与运营（xiaou-version、xiaou-moyu、xiaou-points、lottery）**：版本里程碑、摸鱼打卡、积分规则、抽奖活动与奖品管理。

### 运维与安全

- SQL 慢查询与操作审计、行为日志沉淀到 `xiaou.monitor`，支持异步写入与保留周期配置。
- `docs/Prometheus运维部署指南.md` 提供 Prometheus + Grafana 一键部署说明，Actuator/Micrometer 暴露业务指标。
- Dockerfile、Nginx 反向代理配置与多环境配置文件（dev/prod）一应俱全，便于快速上线。

## 🏗️ 技术架构

### 后端技术栈

- Spring Boot 3.4.4、Spring MVC、Spring Validation、Sa-Token 鉴权。
- MySQL 8 + MyBatis（含 PageHelper）、多数据源、数据库脚本在 `sql/`。
- Redis 6 + Redisson（分布式锁、延迟队列），异步任务/定时调度。
- SpringDoc OpenAPI 3、Knife4j、Hutool、Lombok、MapStruct 等辅助库。
- 日志监控：Micrometer、Prometheus Exporter、SQL/操作日志框架、自定义监控配置。
- AI 工程：LangChain4j、LangGraph4j、LlamaIndex 检索网关、模板化 Prompt 治理与结构化结果解析。

### 前端技术栈

- Vue 3 + Composition API + TypeScript（可选），构建工具 Vite 5。
- Element Plus 组件库、Pinia 状态、Vue Router 4、Axios 网络层、SCSS/Sass。
- 可视化：ECharts、D3、@antv/g6、markdown-it、highlight.js、nprogress。
- 代码规范：ESLint + eslint-plugin-vue + Prettier（可选），统一 `.editorconfig`。

### 基础设施

- MySQL、Redis、对象存储（本地/Tencent COS 等）、Nginx、Docker、GitHub Actions（可扩展）。
- Prometheus + Grafana + Alertmanager、日志采集（logs/）、监控告警配置。

## 📦 模块一览

| 模块 | 说明 | 关键功能 |
| --- | --- | --- |
| xiaou-application | 主 API 工程 | 聚合所有业务模块、统一配置、网关、鉴权与监控出口 |
| xiaou-common | 通用基础库 | 自定义注解、AOP、统一返回体、异常、工具集 |
| xiaou-ai | 统一 AI 服务 | AI Runtime 编排、Prompt/RAG/Schema 治理、AI 结果兜底与治理总览 |
| xiaou-system | 系统管理 | 组织、角色、菜单、字典、参数、审计日志 |
| xiaou-user / xiaou-user-api | 用户中心 | 用户注册、认证、资料、登录态 API 隔离 |
| xiaou-interview | 面试题库 | 题目、试卷、刷题记录、统计、推荐 |
| xiaou-community | 社区/岗位 | 帖子、岗位、任务调度、运营后台 |
| xiaou-moment | 动态广场 | 动态发布、点赞、收藏、推荐算法、审核 |
| xiaou-blog | 博客 | Markdown 编辑、标签、归档、评论 |
| xiaou-codepen | 代码工坊 | 代码模板、在线运行、作品管理、Fork/收藏 |
| xiaou-resume | 在线简历 | 模板库、模块化填写、版本历史、多格式导出 |
| xiaou-filestorage | 文件存储 | 本地/云存储、上传、分片、权限、文本预览与统计 |
| xiaou-learning-asset | 学习资产转化引擎 | 内容转资产、候选审核、发布记录、统计分析 |
| xiaou-notification | 消息中心 | 系统通知、私信、消息状态、推送 |
| xiaou-chat | IM 聊天 | WebSocket 实时通信、撤回、禁言、房间 |
| xiaou-sensitive / xiaou-sensitive-api | 敏感词 | 词库维护、命中记录、外部 API |
| xiaou-knowledge | 知识图谱 | 节点管理、图谱渲染、知识库 |
| xiaou-version | 版本档案 | 版本时间线、发布记录、变更说明 |
| xiaou-moyu | 摸鱼面板 | 日常任务、健康打卡、工作台 |
| xiaou-points | 积分体系 | 积分规则、账户、排行榜、明细、CSV 导出 |
| xiaou-plan | 计划打卡 | 个人计划、每日打卡、连续统计、提醒通知 |
| xiaou-oj | OJ 判题 | 题目管理、在线判题、提交记录、赛事榜单、评分预估 |
| xiaou-mock-interview | AI模拟面试 | 多方向面试、AI出题/评价、求职闭环、Offer 跟踪 |
| xiaou-monitor | SQL/监控 | SQL 采集、慢查询、日志审计、观测面板 |

### 前端应用

|| 模块 | 说明 | 启动命令 |
|| --- | --- | --- |
|| vue3-admin-front | 管理后台，Element Plus + Pinia | `npm install && npm run dev`（端口 3000） |
|| vue3-user-front | 用户端，组件与依赖同后台 | `npm install && npm run dev`（端口 3001） |

### 桌面应用（Electron）

|| 模块 | 说明 | 打包命令 |
|| --- | --- | --- |
|| code-nest-admin-desktop | 管理端桌面应用 | `npm run dist:win`（生成 Windows exe） |
|| code-nest-user-desktop | 用户端桌面应用 | `npm run dist:win`（生成 Windows exe） |

## 🗂️ 目录结构

```
Code-Nest/
├── docker/                     # 后端 Dockerfile、compose 示例
├── docs/
│   ├── PRD/                    # 各业务模块 PRD/需求文档
│   └── Prometheus运维部署指南.md
├── logs/                       # 运行期日志（开发环境可忽略）
├── sql/
│   ├── MySql/
│   │   ├── code_nest.sql       # 最新完整结构（全量建表）
│   │   └── code_nest_data.sql  # 最新初始化数据（可重复执行）
│   ├── v1.8.0/                 # OJ 相关增量脚本
│   ├── v1.8.3/                 # 闭环中台与SQL优化相关增量脚本
│   └── v1.8.4/                 # 学习资产转化引擎增量脚本
├── vue3-admin-front/           # 管理端前端
├── vue3-user-front/            # 用户端前端
├── xiaou-common/               # 通用模块
├── xiaou-application/          # Spring Boot 聚合工程（启动入口）
├── xiaou-*/                    # 业务子模块（interview、blog、resume…）
├── pom.xml                     # 多模块 Maven 配置
└── README.md
```

## 🚀 Quick Start

### 环境要求

- Java 17+
- Node.js 18+（Vite 5 推荐）
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+
- 可选：Docker、Nginx、Prometheus/Grafana

### 1. 克隆项目

```bash
git clone https://github.com/your-username/Code-Nest.git
cd Code-Nest
```

### 2. 初始化数据库

```bash
mysql -u root -p

CREATE DATABASE code_nest DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE code_nest;
SOURCE sql/MySql/code_nest.sql;
SOURCE sql/MySql/code_nest_data.sql;
-- 如需增量升级，请按版本顺序执行 sql/v1.x.x/*.sql
```

### 3. 配置文件

编辑 `xiaou-application/src/main/resources/application-dev.yml`（或新建 `application-local.yml`）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/code_nest?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: your_mysql_user
    password: your_mysql_password
  data:
    redis:
      redisson:
        config: |
          singleServerConfig:
            address: "redis://127.0.0.1:6379"
            database: 3

sa-token:
  token-name: Authorization
  token-prefix: Bearer
  timeout: 604800
  activity-timeout: 1800

xiaou:
  monitor:
    enabled: true
    slow-sql-threshold: 1000
    async-save: true
    retention-days: 30
```

按需补充短信/OSS/COS 等密钥。

### 4. 启动后端

```bash
# 一键编译
mvn clean package -DskipTests

# 开发模式启动（默认使用 dev 配置）
mvn -pl xiaou-application -am spring-boot:run

# 或直接运行打包后的 jar
java -jar xiaou-application/target/xiaou-application-v2.1.2.jar --spring.profiles.active=prod
```

- API 根地址：`http://localhost:9999/api`
- Swagger / Knife4j：`http://localhost:9999/api/swagger-ui.html`

### 4.1 启动 AI 检索服务（启用 RAG 时需要）

如果只是启动 Java 主项目而不走知识库检索，可以先不启动这一段，并将 `xiaou.ai.rag.enabled=false` 或环境变量 `XIAOU_AI_RAG_ENABLED=false`。

如果要联调以下能力，则需要同时启动独立的 Python 检索服务：

- 管理后台 `AI 配置与观测` 页面中的 `LlamaIndex 检索调试`
- 模拟面试、SQL 优化、求职作战台等场景中的 RAG 召回

推荐在仓库根目录使用 PowerShell 脚本启动：

```powershell
# 首次本地启动，自动创建 .venv；如需安装依赖可额外加 -InstallDependencies
.\scripts\ai\start-llamaindex-service.ps1 -ApiKey "your-rag-key"
```

如果你希望把 “RAG sidecar + Java 主服务” 一起拉起，推荐直接使用：

```powershell
.\scripts\ai\start-ai-dev-stack.ps1 -RagApiKey "your-rag-key" -ImportSampleKnowledge
```

这条命令会：

- 新开一个 PowerShell 窗口启动 `llamaindex-service`
- 在当前终端注入 Java 侧所需的 `XIAOU_AI_RAG_*` 环境变量
- 可选导入样例知识
- 直接运行 `mvn -pl xiaou-application -am spring-boot:run`

导入一份本地样例知识：

```powershell
.\scripts\ai\import-sample-knowledge.ps1 -ApiKey "your-rag-key" -Replace
```

Java 侧配置需要与之对应：

```yaml
xiaou:
  ai:
    rag:
      enabled: true
      endpoint: http://127.0.0.1:18080
      api-key: your-rag-key
      default-top-k: 5
```

脚本说明：

- [start-llamaindex-service.ps1](/D:/onenodes/githubprojectstart/Code-Nest/scripts/ai/start-llamaindex-service.ps1)：启动独立检索服务，可选自动建虚拟环境与安装依赖。
- [start-ai-dev-stack.ps1](/D:/onenodes/githubprojectstart/Code-Nest/scripts/ai/start-ai-dev-stack.ps1)：面向 Windows 本地联调的一键启动脚本，负责拉起 RAG sidecar 并接着启动 Java 主服务。
- [import-sample-knowledge.ps1](/D:/onenodes/githubprojectstart/Code-Nest/scripts/ai/import-sample-knowledge.ps1)：把 [sample-documents.json](/D:/onenodes/githubprojectstart/Code-Nest/llamaindex-service/data/sample-documents.json) 导入到本地知识库。
- 检索服务健康检查：`http://127.0.0.1:18080/health`

补充说明：

- 只用普通后台管理、社区、非 RAG AI 场景时，不需要额外启动 Python sidecar。
- 只要开启了知识库检索、后台 RAG 调试、或需要验证 `LlamaIndex` 管理接口，就需要同步启动 sidecar。

### 5. 启动前端

#### 管理后台

```bash
cd vue3-admin-front
npm install
npm run dev
```

访问 `http://localhost:5173`

#### 用户端

```bash
cd vue3-user-front
npm install
npm run dev
```

访问 `http://localhost:5174`

### 6. 常用账号

| 端 | 账号 | 密码 | 备注 |
| --- | --- | --- | --- |
| 管理后台 | `admin` | `123456` | 可在用户管理中修改 |
| 用户端 | 已内置演示数据 | `sql/MySql/code_nest_data.sql` 中提供 | 也可自行注册 |

### 7. 构建与部署产物

```bash
# 后端
mvn clean package -DskipTests

# 前端
cd vue3-admin-front && npm run build
cd vue3-user-front && npm run build
```

静态资源输出至 `dist/`，可由 Nginx 或对象存储托管。

## 🔧 配置与运维

### Sa-Token 关键配置

```yaml
sa-token:
  token-name: Authorization
  token-style: uuid
  is-share: false
  is-concurrent: true
  token-prefix: Bearer
  alone-redis:
    host: 127.0.0.1
    port: 6379
    database: 4
```

### Redis / Redisson

```yaml
spring:
  data:
    redis:
      redisson:
        config: |
          singleServerConfig:
            address: "redis://127.0.0.1:6379"
            database: 3
            password: your_redis_password
```

### SQL 与监控

```yaml
xiaou:
  monitor:
    enabled: true
    slow-sql-threshold: 800
    async-save: true
    retention-days: 30
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

- Prometheus 抓取地址：`http://<host>:9999/api/actuator/prometheus`
- Grafana Dashboard/报警配置参考 `docs/Prometheus运维部署指南.md`

### 日志

- 默认输出到 `logs/` 目录，按日期分片。
- Sa-Token、SQL、操作日志支持异步保存，可在 `application-*.yml` 中自定义。

## ☁️ 部署指南

### Docker

```bash
# 构建镜像
docker build -t code-nest:v2.1.2 -f docker/Dockerfile .

# 运行容器
docker run -d \
  --name code-nest \
  -p 9999:9999 \
  -e SPRING_PROFILES_ACTIVE=prod \
  --env-file docker/env/example.env \
  code-nest:v2.1.2
```

如果要把 MySQL / Redis / Java 主服务 / `llamaindex-service` 一起编排起来，推荐使用：

```bash
cp docker/ai/.env.example docker/ai/.env
docker compose -f docker/ai/docker-compose.yml --env-file docker/ai/.env up -d --build
```

说明：

- [docker/Dockerfile](/D:/onenodes/githubprojectstart/Code-Nest/docker/Dockerfile)：Java 主服务镜像构建文件。
- [docker/env/example.env](/D:/onenodes/githubprojectstart/Code-Nest/docker/env/example.env)：`docker run` 示例环境变量模板。
- [docker/ai/docker-compose.yml](/D:/onenodes/githubprojectstart/Code-Nest/docker/ai/docker-compose.yml)：AI 联调全栈编排。
- [docker/ai/README.md](/D:/onenodes/githubprojectstart/Code-Nest/docker/ai/README.md)：Compose 使用说明。

### Nginx

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 管理端静态资源
    location /admin/ {
        root /opt/code-nest/vue3-admin-front/dist;
        try_files $uri $uri/ /admin/index.html;
    }

    # 用户端静态资源
    location / {
        root /opt/code-nest/vue3-user-front/dist;
        try_files $uri $uri/ /index.html;
    }

    # API 转发
    location /api/ {
        proxy_pass http://127.0.0.1:9999/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

## 📚 文档与资料

- `docs/PRD/`：覆盖简历、代码工坊、IM、积分、抽奖、知识图谱、敏感词、SQL 优化等 20+ 模块的产品文档。
- `docs/plans/2026-04-20-ai-runtime-replatform-design.md`：AI Runtime 从 Coze 迁移到 `LangChain4j + LangGraph4j + LlamaIndex` 的完整设计文档。
- `docs/plans/2026-04-20-ai-runtime-replatform.md`：AI Runtime 重构落地计划与实施拆解。
- `docs/plans/2026-04-20-ai-prompt-governance.md`：Prompt / RAG Query / Structured Output 的工程化治理规范。
- `docs/plans/2026-04-20-ai-regression-eval.md`：AI 黄金样例回归、历史记录与评测约定。
- `docs/plans/2026-04-26-v2.1.0-growth-intelligence-design.md`：v2.1.0 Growth Intelligence 双主线设计说明。
- `docs/plans/2026-04-26-v2.1.0-growth-intelligence.md`：v2.1.0 Growth Intelligence 实施计划与验证范围。
- `docs/PRD/学习资产转化引擎模块PRD-v1.0.0.md`：内容转学习资产的产品范围、链路设计与实现边界。
- `docs/PRD/OJ赛事系统PRD-v1.0.0.md`：OJ 赛事系统（周赛/挑战赛）需求定义与里程碑。
- `docs/Prometheus运维部署指南.md`：Prometheus + Grafana 安装、指标、告警策略。
- `sql/MySql/code_nest.sql`：最新完整结构脚本。
- `sql/MySql/code_nest_data.sql`：汇总初始化数据脚本（可重复执行）。
- `sql/v1.8.0~v1.8.4/`：近期增量脚本（OJ、求职作战台、闭环中台、学习资产转化引擎等）。
- `pom.xml`：多模块管理、版本统一、Flatten 插件配置。
- `docker/`：容器化部署示例。

## 📝 更新日志

仅列出最近版本，更多历史可查看 `git log`。

### v2.1.2 🧯 聊天室防刷增强

- 🚦 **消息发送限流**：新增 Redis 固定窗口限流，默认每个用户 10 秒最多 8 条消息，异常脚本或刷屏会收到结构化错误。
- 🧊 **输入状态降噪**：`TYPING` 事件纳入限流，降低高频输入状态广播对 WebSocket 的压力。
- 🧱 **消息入库校验**：文本和图片消息按类型校验空值、长度和图片 URL 协议，拦截非法或异常消息。
- 🔁 **失败态闭环**：用户端收到限流、校验失败等 WebSocket 错误时，会把对应本地消息标记失败并保留提示。
- 📦 **版本基线升级**：后端 Maven、管理端前端、用户端前端、README Jar/Docker 示例统一升级到 `v2.1.2`。

### v2.1.1 🛡️ 安全加固与聊天室稳定性

- 🔐 **文件接口权限收口**：上传、删除、列表与存在性检查要求登录；公开文件可匿名读取，私有文件读取需登录校验。
- 🎫 **WebSocket 一次性票据**：用户端先通过登录态申请 60 秒短期票据，聊天室握手消费票据后立即失效，避免长期 Token 暴露在 URL 中。
- 💬 **实时消息体验补齐**：服务端新增 PONG 心跳响应与 TYPING 输入中广播，并修复消息 ACK 回填本地临时消息 ID 的兼容逻辑。
- 🌐 **CORS 配置化**：HTTP 与 WebSocket 默认跨域来源改为本地开发白名单，并支持通过 `xiaou.cors.allowed-origin-patterns` 扩展生产域名。
- 🧼 **前端 HTML 净化**：Markdown 工具统一接入 DOMPurify，并补齐多处 `v-html` 用户内容展示的转义与净化。
- 📦 **版本基线升级**：后端 Maven、管理端前端、用户端前端、README Jar/Docker 示例统一升级到 `v2.1.1`。

### v2.1.0 🚀 Growth Intelligence

- 🧭 **AI 学习成长驾驶舱 2.1**：新增成长分、等级、趋势说明和 AI 学习复盘，帮助用户快速判断本周学习节奏。
- 📊 **能力雷达与短板诊断**：基于 OJ、题库、闪卡、计划、积分五模块完成率生成能力结构和短板队列。
- ✅ **今日任务闭环**：从下一步建议中提炼今日可执行任务，补齐“看到问题 -> 立即行动”的体验链路。
- 🛡️ **AI Runtime 质量中心**：后台 AI 治理升级为质量分 + 覆盖矩阵 + 运行洞察 + 风险队列，便于定位 Prompt、Schema、RAG、回归准备度和线上运行问题。
- 📦 **版本基线升级**：后端 Maven、管理端前端、用户端前端、README Jar/Docker 示例统一升级到 `v2.1.0`。

### v2.0.1 🔖 功能增强与版本升级

- 🚀 **发布版本升级**：后端 Maven 版本、管理端前端与用户端前端统一升级到 `v2.0.1`。
- 📦 **构建示例同步**：后端 Jar 运行命令与 Docker 镜像标签同步更新为 `v2.0.1`。
- 🤖 **AI Runtime 轻量治理**：新增 AI 治理总览接口与管理端页面，展示 AI Runtime 场景目录、配置状态、兜底覆盖率、风险等级与治理建议。
- 🔁 **求职闭环二期**：闭环状态机新增 `OFFER_TRACKING` 阶段，复盘完成后可继续推进投递记录、Offer 跟踪和反馈沉淀。
- 🏆 **OJ 赛事增强**：赛事榜单新增表现分、评分变化与赛后预估评分，帮助参赛者快速理解比赛表现。
- 🧰 **管理端体验补齐**：文件管理支持文本类文件在线预览，积分明细支持按当前筛选条件导出 UTF-8 CSV。
- 🧭 **升级分支准备**：从 `master` 开出 `v2.0.1` 分支，作为小版本功能增强与发布验证基线。

### v2.0.0 🤖 AI Runtime 全面重构

- ♻️ **彻底移除 Coze 运行链路**：仓库主干不再保留 Coze SDK、配置、工具类和历史工作流依赖，统一收口到新 AI Runtime。
- 🧠 **统一 AI 技术栈**：`xiaou-ai` 完成 `LangChain4j + LangGraph4j + LlamaIndex` 分层重构，覆盖社区摘要、模拟面试、求职作战台、SQL 优化等核心 AI 场景。
- 🧩 **Prompt 工程化治理**：新增 Prompt Catalog、RAG Query Catalog、Structured Schema Catalog，支持模板化、版本化、结构化输出校验与后台在线调试。
- 📚 **知识库 sidecar 落地**：引入独立 `llamaindex-service`，支持知识库健康检查、检索调试、样例知识导入、自定义文档导入、导出、单删和批量删除。
- 🛠️ **后台 AI 配置与观测中心**：新增 OpenAI Compatible 中转站连通性测试、AI 运行时配置查看、运行指标总览、最近调用、Prompt/RAG 调试、黄金样例回归。
- ✅ **AI 质量治理补齐**：增加黄金样例回归、最近一次执行结果持久化、最近历史回看、Prompt/RAG/Schema 可观测治理，便于模型替换和提示词迭代时快速验收。
- 🚚 **本地联调与部署增强**：新增 `scripts/ai` 启动脚本、Docker 相关资源和示例环境配置，降低本地启动 Java 主服务与 RAG sidecar 的成本。

### v1.8.2 🧠 学习资产转化引擎

- 🆕 **学习资产转化引擎模块**：新增 `xiaou-learning-asset` 模块，提供转化记录、候选资产、发布日志、审核流转与统计分析能力。
- 🪄 **多来源内容一键转资产**：用户可从博客文章、社区帖子、CodePen 作品、AI 模拟面试报告发起转化，统一生成个人学习资产记录。
- 📚 **用户端学习资产中心**：新增 `/learning-assets` 页面，支持查看记录、编辑候选、丢弃/恢复、确认发布，并提供学习与计划承接入口。
- 🛡️ **管理端审核与统计**：管理后台新增学习资产审核台与统计页，支持候选内容编辑、通过/驳回、合并与质量概览。
- 🔔 **跨模块通知联动**：学习资产发布摘要、审核通过、合并、驳回结果统一接入通知中心，打通内容消费到学习闭环。
- 🗄️ **数据库与文档补齐**：新增 `sql/v1.8.4/learning_asset.sql`，并补充 `docs/PRD/学习资产转化引擎模块PRD-v1.0.0.md` 作为产品说明。

### v1.8.1 🔁 求职闭环中台与作战链路升级

- 🆕 **求职闭环中台（Career Loop）**：新增后端状态机与会话模型（阶段、时间线、动作清单、快照），统一管理 `JD解析 -> 简历匹配 -> 计划生成 -> 计划执行 -> 面试完成 -> 复盘完成` 全链路。
- 🆕 **闭环用户端页面**：新增 `/career-loop`，支持阶段总览、动作清单、风险建议、时间线，并补充雷达图与近4周热力图可视化。
- 🔗 **跨模块事件联动**：Job Battle 与 AI 模拟面试关键节点自动推送闭环事件；作战台支持“同步到闭环”和“查看闭环进度”。
- 🎯 **作战台体验强化**：历史计划入口前置、步骤串行校验优化、计划生成结果升级为卡片+时间线+表格+风险区的可视化展示。
- 🗄️ **SQL脚本整理与数据脚本补齐**：`sql/MySql/code_nest.sql` 作为最新结构脚本，新增 `sql/MySql/code_nest_data.sql` 汇总初始化数据，并补充 `v1.8.3` 增量脚本。

### v1.8.0 🏆 OJ赛事系统（周赛/挑战赛）落地

- 🆕 **赛事数据模型**：新增赛事相关数据结构，并将提交记录与 `contest` 维度打通，为周赛/挑战赛提供基础能力。
- 🧩 **管理端赛事管理**：`vue3-admin-front` 新增 OJ 赛事管理页面，支持赛事配置与运营管理流程。
- 🔌 **后端赛事服务与 API**：`xiaou-oj` 完成赛事服务层与持久化实现，同时开放管理员端与用户端赛事 API。
- ✅ **参赛校验机制**：提交判题前新增参赛资格与规则校验，避免非参赛提交或越权提交。
- 📊 **赛事排行榜计算**：新增赛事排名计算逻辑，支持按规则统计成绩并生成榜单。
- 🖥️ **用户端赛事中心**：`vue3-user-front` 新增赛事中心页面并完成 API 集成，形成报名/参赛/查看榜单闭环。
- 🧪 **工程质量补强**：补充赛事规则校验测试（TDD）与 PRD/进度文档，提升功能可维护性与可追踪性。

### v1.7.2 🃏 闪卡与AI重构

- 🆕 **闪卡模块**：`xiaou-flashcard` 新增闪卡学习功能，支持卡片式记忆法，提升知识点复习效率。
- 🤖 **AI服务重构**：`xiaou-ai` 开始从历史 Coze 方案迁移到统一 AI 服务接口，为后续 `LangChain4j + LangGraph4j + LlamaIndex` 重构打基础。
- 🔐 **登录安全优化**：统一登录检查逻辑，修复安全性问题，增强系统防护能力。
- ⚡ **代码质量优化**：CodePen、积分模块代码质量与性能优化。
- 🔧 **SQL优化模块**：`xiaou-sql-optimizer` 新增 SQL 优化分析能力。

### v1.7.1 🖥️ 桌面应用支持

- 🆕 **Electron 桌面端打包**：支持将 vue3-admin-front 和 vue3-user-front 打包为独立桌面应用（Windows exe / macOS dmg / Linux AppImage）。
- 🖥️ **桌面端功能**：系统托盘、窗口状态记忆、本地存储、IPC 通信等原生桌面能力。
- 🔧 **electron-vite 构建**：采用 electron-vite + electron-builder 技术栈，支持开发热重载与生产打包。
- 👥 **小组头像上传**：学习小组支持自定义头像上传，优化组件交互体验。
- 📝 **PRD 文档**：新增 `docs/PRD/Electron桌面端打包PRD-v1.0.0.md`，完整记录桌面端技术方案。

### v1.7.0 📚 学习效果追踪

- 🌟 **掌握度标记**：支持不会/模糊/熟悉/已掌握四级评估，集成到做题模式。
- 📈 **GitHub风格学习热力图**：可视化展示全年学习轨迹，集成到面试题库首页。
- 🔔 **艾宾浩斯遗忘曲线复习提醒**：基于掌握度智能计算最佳复习时间。
- 📊 **复习中心**：新增 `/interview/review` 页面，统一管理待复习题目（逾期/今日/本周）。
- 🔥 **学习统计**：连续学习天数、最长连续、本月学习等数据展示。
- 💬 **IM消息回复**：支持引用回复历史消息，优化在线用户管理。
- 🗄️ **数据库**：新增 `interview_mastery_record`、`interview_daily_stats`、`interview_mastery_history` 表。

### v1.6.3 🌟 AI模拟面试

- 🆕 **AI模拟面试模块**：`xiaou-mock-interview` 新增完整的AI模拟面试功能，支持多方向、多难度、多风格的模拟面试体验。
- 🤖 **双模式出题**：支持本地题库模式（从Interview题库抽题）和 AI出题模式（历史上曾调用 Coze 工作流，现已迁移至统一 AI Runtime）。
- 📝 **实时AI评价**：用户回答后地获取AI评分、回答优点、改进建议，支持多种面试官风格（严厲/中性/温和）。
- 🔄 **用户主动追问**：用户可在回答后主动请求追问，深入考察知识点掌握情况，每题最多追问2次。
- 📊 **面试报告与统计**：面试结束后生成详细报告，包含AI总结、建议、各题得分明细；支持历史记录查询与用户统计面板。
- 🎯 **题库选择**：本地模式下用户可手动选择题库集进行定向训练。
- 🗂️ **前端页面**：新增面试入口页、配置页、面试进行页、报告页、历史记录页，全流程用户体验。
- 🗄️ **数据库表**：新增mock_interview_direction、mock_interview_session、mock_interview_qa、mock_interview_user_stats等表。

### v1.6.1 ✨

- 🆕 **计划打卡模块**：`xiaou-plan` 支持个人计划创建、每日打卡、连续打卡统计、站内提醒通知、打卡记录查询等功能。
- 🎨 **首页重新设计**：采用现代卡片式布局，新增 Hero 区域、核心功能展示、快速入口网格、特色亮点区域，提升视觉体验。
- 📱 **面试详情页优化**：优化移动端适配与样式细节，提升手机端刷题体验。
- 🗄️ **数据库脚本**：`sql/v1.6.1` 新增计划打卡相关表结构（user_plan、plan_checkin_record、plan_remind_task）。
- 🔔 **定时任务**：计划模块集成提醒调度器，支持每日任务生成与站内通知推送。
- 🔧 **导航优化**：首页快速入口新增「计划打卡」，顶部导航「学习」菜单新增入口。

### v1.6.0 🚀

- 🆕 **在线简历模块**：`xiaou-resume` 支持模板市场、模块化数据、版本快照、拖拽编辑与 PDF/Word/HTML 导出。
- 🆕 **代码工坊（CodePen）**：支持前端/后端/SQL 演示、作品发布、Fork/收藏/点赞、后台运营、商业化配置。
- 🆕 **可观测中心**：Prometheus + Grafana 监控整合 Micrometer/Actuator 指标、SQL 观测、告警与文档。
- 🆕 **刷题洞察**：题库模块新增统计面板、算法难度画像、错题同步订阅。
- 🗄️ **数据库脚本**：`sql/v1.6.0` 目录补充 CodePen/简历相关表结构。
- 🔧 **导出 & 存储**：简历导出自动上传至 `xiaou-filestorage`，新增 COS/MinIO 适配。
- 🔧 **内容体验**：社区/简历模板支持用户端/后台协同维护，README 与版本说明全面升级。

### v1.5.0 🎉

- 🆕 **博客系统**：Markdown 编辑、分类/标签、草稿箱、全文检索与评论互动。
- 🆕 **抽奖中心**：活动配置、奖品管理、概率算法、中奖记录及后台运营。
- 🆕 **标签体系统一**：社区/博客/动态共用标签维度，提供榜单与统计。
- ✨ **动态功能增强**：动态收藏夹、推荐算法（点赞/评论/权重）、违规拦截、可视化报表。
- 🛡️ **敏感词系统**：词库批量导入、命中记录、任务调度、接入 IM/动态/博客。
- 📊 **监控重构**：Prometheus 指标拆分、JVM/HTTP/DB 监控、告警模板。
- 🔐 **认证优化**：Token 生命周期管理、登录流程、统一异常语义。
- 📦 **数据库更新**：新增博客、抽奖、标签、动态收藏等表结构。

### v1.4.0 🚀

- 🔐 **Sa-Token 全面接管**：JWT 全量迁移至 Sa-Token，双端账号体系、并发控制、路由鉴权、WebSocket 鉴权。
- 💬 **IM 聊天模块**：WebSocket 实时通信，文本/图片/系统消息、撤回、禁言、封禁、后台管理。
- ⭐ **积分系统**：积分规则、明细、排行榜、任务中心。
- 🧱 **架构优化**：全局异常处理增强、模块解耦、WebSocket 鉴权同步、敏感词中台。
- 📦 **数据库**：新增聊天、积分相关表。

### v1.3.0 🔥

- 🤖 **历史架构阶段：Coze AI 集成**：早期版本曾以 Coze 作为统一 AI 能力入口，当前主干已完成新一代 Runtime 重构。
- 🎯 **Bug 趣味墙**：Bug 题库、抓取、分享、历史记录与运营活动。
- 🧘 **摸鱼工作台**：每日任务、时薪统计、排行榜、提醒。
- 🧰 **效率工具集**：常用开发工具聚合、清晰的路径引导。
- 🎨 **用户体验**：前台 UI 重构、响应式布局、主题优化。
- 🧠 **知识图谱强化**：节点内容升级、图谱编辑、后台维护流程。
- 📦 **模块化**：`xiaou-moyu`、`xiaou-version` 等独立为可复用模块。

### v1.2.x

- 🛠️ **在线工具/简历 2.0**：多格式导出、版本管理、实时协作。
- 🔔 **通知中心**：系统消息、站内信、订阅提醒。
- 📁 **文件存储**：本地/云存储适配、权限控制、统计。
- ⚙️ **系统优化**：SQL 规范化、结构调整、性能提升。

### v1.1.x

- ✨ **功能增强**：招聘/内推模块、问答、用户中心、数据结构升级。
- 🧱 **架构优化**：模块抽象、代码重构、性能调优。

### v1.0.0

- 🎉 初始发布，包含后台管理与基础题库/内容功能。

## 📞 联系方式

- 维护者：xiaou
- 邮箱：3153566913@qq.com
- GitHub：https://github.com/xiaou61/Code-Nest

## 📜 开源协议

本项目采用 [MIT License](LICENSE)。欢迎 Star / Fork，一起共建生态。

## 🖼️ 功能截图(bu'fen)

### 管理员端

![image-20251001192022408](https://11-1305448902.cos.ap-chengdu.myqcloud.com/imgs/202510011920552.png)
![image-20251001192046724](https://11-1305448902.cos.ap-chengdu.myqcloud.com/imgs/202510011920794.png)
![image-20251001192101335](https://11-1305448902.cos.ap-chengdu.myqcloud.com/imgs/202510011921406.png)
![image-20251001192116123](https://11-1305448902.cos.ap-chengdu.myqcloud.com/imgs/202510011921200.png)
![image-20251001192148551](https://11-1305448902.cos.ap-chengdu.myqcloud.com/imgs/202510011921665.png)

### 用户端



![image-20260128111943563](https://eduplan-1305448902.cos.ap-guangzhou.myqcloud.com/imgs/202601281119341.png)



![image-20260128111953084](https://eduplan-1305448902.cos.ap-guangzhou.myqcloud.com/imgs/202601281119446.png)

![image-20260128112011807](https://eduplan-1305448902.cos.ap-guangzhou.myqcloud.com/imgs/202601281120161.png)

![image-20260128112030776](https://eduplan-1305448902.cos.ap-guangzhou.myqcloud.com/imgs/202601281120041.png)

![image-20260128112045443](https://eduplan-1305448902.cos.ap-guangzhou.myqcloud.com/imgs/202601281120004.png)

![image-20260128112055648](https://eduplan-1305448902.cos.ap-guangzhou.myqcloud.com/imgs/202601281120061.png)

![image-20260128112110606](https://eduplan-1305448902.cos.ap-guangzhou.myqcloud.com/imgs/202601281121938.png)

![image-20260128112120106](https://eduplan-1305448902.cos.ap-guangzhou.myqcloud.com/imgs/202601281121448.png)

![image-20260128112137308](https://eduplan-1305448902.cos.ap-guangzhou.myqcloud.com/imgs/202601281121696.png)

![image-20260128112149380](https://eduplan-1305448902.cos.ap-guangzhou.myqcloud.com/imgs/202601281121774.png)

![image-20260128112200018](https://eduplan-1305448902.cos.ap-guangzhou.myqcloud.com/imgs/202601281122224.png)

![image-20260128112235813](https://eduplan-1305448902.cos.ap-guangzhou.myqcloud.com/imgs/202601281122565.png)

![image-20260128112246514](https://eduplan-1305448902.cos.ap-guangzhou.myqcloud.com/imgs/202601281122911.png)
