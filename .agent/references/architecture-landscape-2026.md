---
artifact: reference
status: active
updated: 2026-10-09
---

# Paideia 架构选型调研（2026-10）

本文是架构选型的共享依据：核实过的版本事实 + 主要架构形态的取舍 + 持久层与 AI 引擎的结论。事实均于 2026-10-09 联网核实并附来源；标注“未能核实”的条目表示当日无法取得权威来源。

## 一、版本事实

### 后端运行时

| 组件 | 当前版本 | 关键事实 | 来源 |
| --- | --- | --- | --- |
| Spring Boot | 4.1.1（2026-08-20）；4.0.0 GA 2025-11-20 | JDK 基线 17，官方推荐 25，最高支持到 26 | [system-requirements](https://docs.spring.io/spring-boot/4.1/system-requirements.html) |
| Spring Framework | 7.0.9 | Jakarta EE 11 / Servlet 6.1；JSpecify 空安全；内建 API 版本管理；HTTP Service Clients（`@ImportHttpServices`） | [7.0 Release Notes](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes) |
| JDK | 25（LTS，GA 2025-09-16） | 虚拟线程 `synchronized` 钉住问题已由 JEP 491（JDK 24 final）解决；Scoped Values final（JEP 506）；AOT 缓存 final（JEP 514/515）；紧凑对象头 final（JEP 519） | [JDK 25](https://openjdk.org/projects/jdk/25/) |
| Spring Modulith | 2.1.1（2026-08-26） | 对应 Boot 4.1.x；构建期强制模块边界；事件发布注册表即 outbox；可生成 PlantUML C4 文档 | [Modulith 文档](https://docs.spring.io/spring-modulith/reference/verification.html) |
| Spring AI | 2.0.1（2026-08-21，对齐 Boot 4.1） | ChatClient 同步+流式、Embedding、17+ 向量库、RAG advisor、工具调用、MCP、可观测性 | [Spring AI](https://docs.spring.io/spring-ai/reference/index.html) |
| LangChain4j | 1.22.0（2026-10-08） | 同时提供 Boot 3 / Boot 4 starter | [langchain4j](https://docs.langchain4j.dev/tutorials/spring-boot-integration/) |
| MyBatis | 3.5.19 | **不存在 3.6.x GA**，按 3.5.x 规划 | [maven-metadata](https://repo1.maven.org/maven2/org/mybatis/mybatis/maven-metadata.xml) |
| PageHelper | 6.1.1 | 方言覆盖含达梦/人大金仓/OpenGauss；但用 `ThreadLocal`，官方文档自承存在“未消费分页参数污染后续查询”的坑 | [PageHelper 文档](https://pagehelper.github.io/docs/howtouse/) |
| Flyway / Liquibase | 13.10.0 / 5.0.4 | Flyway 是 SQL 优先、不翻译方言；Liquibase 有真正的 changelog 抽象 + `dbms` 属性 | [Flyway](https://documentation.red-gate.com/flyway/reference/database-driver-reference)、[dbms](https://docs.liquibase.com/concepts/changelogs/attributes/dbms.html) |

Boot 4 破坏性变更（必须知道）：starter 改名（`spring-boot-starter-web` → `spring-boot-starter-webmvc`）；**移除 Undertow**；移除 `@MockBean`/`@SpyBean`；移除经典 uber-jar loader。来源：[Boot 4.0 迁移指南](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)。

JDK 25 注意：**结构化并发仍是第 5 次预览（JEP 505），不要建在生产代码上**；虚拟线程已可用于阻塞式 IO。

### 前端

| 组件 | 当前版本 | 来源 |
| --- | --- | --- |
| React | **19.3**（2026-09-09）；**没有 React 20** | [react.dev/blog](https://react.dev/blog) |
| React Compiler | 1.0 稳定（2025-10-07） | [公告](https://react.dev/blog/2025/10/07/react-compiler-1) |
| Vite | 8.3.4（2026-10-08） | [releases](https://github.com/vitejs/vite/releases) |
| Next.js | 16.4.0（2026-10-07） | [Next 16](https://nextjs.org/blog/next-16) |
| React Router | 8.4.0（2026-09-15），框架模式可 `ssr:false` 走 SPA | [reactrouter.com](https://reactrouter.com/how-to/spa) |
| TanStack Query | 5.104.1（2026-10-02） | [releases](https://github.com/TanStack/query/releases) |
| Zustand / Jotai | 5.0.15 / 3.0.1 | 各自 releases |
| Tailwind CSS | v4.3（2026-05-08） | [tailwindcss.com/blog](https://tailwindcss.com/blog) |
| shadcn/ui | Base UI 已为默认基座（Base UI 1.8.0） | [changelog](https://ui.shadcn.com/docs/changelog) |
| Vercel AI SDK | `ai@7.0.136`（2026-10-09） | [releases](https://github.com/vercel/ai/releases) |
| Vitest / Playwright | 5.0.3 / 1.64.0 | 各自 releases |
| Turborepo / Nx | 2.11.7 / 23.3.0 | 各自 releases |

未能核实：TanStack Start 是否已算 v1 稳定（文档仍标 RC）；Radix primitives 具体版本（按包发布，无 GitHub Release）。

## 二、架构形态取舍

| 形态 | 它最强的一面 | 未选或慎用的理由 |
| --- | --- | --- |
| 模块化单体 | 边界由工具在构建期强制；一套流水线一套库；重构成本最低 | 需要纪律与工具，否则退化成大泥球——本方案用 Spring Modulith 解决 |
| 微服务 | 独立扩缩容、多团队独立发布 | Fowler 的「微服务溢价」：分布式带来的自动化部署/监控/失败处理/最终一致性成本，只有复杂到单体管不动时才划算 |
| 服务化（4–12 个粗粒度服务，共享库） | 大部分可部署性收益、无按服务拆数据的代价 | 共享库会成为耦合点 |
| 事件驱动 + Broker | 解耦、异步扩缩容 | 双写问题、幂等、乱序、毒消息；事件 schema 会变成隐性共享契约 |
| CQRS / 事件溯源 | 读写分别优化、审计与时间旅行 | Fowler：CQRS 增加“有风险的复杂度”，他见过的大多数案例“并不好”；只适合局部 |
| DDD 限界上下文 | 按语言/团队切分大模型 | 几乎零基础设施成本，但需要技能与领域专家 |
| 六边形 / 整洁架构 | 业务逻辑不泄漏到 UI/DB，可测试、驱动可替换 | 每个依赖都配 port/DTO/mapper 的仪式感是标准批评；只在真正会变的边界用 |
| 多租户 pool / bridge / silo | pool 最省，silo 隔离最强 | silo 对上千小租户会放大迁移与备份运维；pool 漏写过滤条件即跨租户泄漏 |
| 纵向切片 | 按功能组织，减少跨切片耦合 | 共享领域规则时会在切片间重复 |
| BFF | 按体验聚合后端 | 只有一个 Web 客户端且聚合很少时无必要 |

**行业证据（分布式设计被回退的实例）**：Shopify 维持 280 万行模块化单体并用 Packwerk 强制依赖；Segment 把 140+ 服务合并回一个；Istio 把 Pilot/Citadel/Galley/Mixer 合并为单个 `istiod`；Amazon Prime Video 的音视频监控服务从 serverless 微服务改回单体，成本降约 90%。Khan Academy 的 Python 单体迁 Go 后承认服务边界“更慢更脆”；Canvas LMS 至今是 Rails 单体 + React 前端。

来源：[MonolithFirst](https://martinfowler.com/bliki/MonolithFirst.html)、[MicroservicePremium](https://martinfowler.com/bliki/MicroservicePremium.html)、[Shopify](https://shopify.engineering/shopify-monolith)、[Segment](https://www.twilio.com/en-us/blog/developers/best-practices/goodbye-microservices/)、[istiod](https://istio.io/latest/blog/2020/istiod/)、[Khan Academy](https://blog.khanacademy.org/go-services-one-goliath-project/)、[Canvas](https://github.com/instructure/canvas-lms)、[CQRS](https://martinfowler.com/bliki/CQRS.html)、[BFF](https://samnewman.io/patterns/architectural/bff/)、[多租户存储模型](https://docs.aws.amazon.com/whitepapers/latest/multi-tenant-saas-storage-strategies/multi-tenant-storage-models.html)

## 三、多数据库可移植的持久层结论

这是本项目最容易被低估的一块。要点：

1. **MyBatis 的 `databaseId` 机制**：`DB_VENDOR` 取的是 `DatabaseMetaData.getDatabaseProductName()`，靠字符串前缀匹配，只做“整个语句替换”，没有方言继承或就近回退。国产库与 MySQL 兼容驱动常匹配失败，`databaseId` 静默为 `null` 后回退到无 `databaseId` 的语句——**必须在启动时打印解析结果**。来源：[MyBatis 配置](https://mybatis.org/mybatis-3/configuration.html#databaseIdProvider)
2. **分页**：`RowBounds` 不是 SQL 分页，它在驱动取回全部结果后于内存中跳过，且不给 COUNT。方言只有两族：`LIMIT`（MySQL/PG/国产）与 `OFFSET..FETCH NEXT`（Oracle 12c+/SQL Server，后者强制要求 `ORDER BY`）。
3. **主键**：数据库自增/序列/`RETURNING`/`OUTPUT` 的取值方式不可移植（MySQL 无 `RETURNING`，MySQL 无 `CREATE SEQUENCE`，Oracle 的 `useGeneratedKeys` 取值失败）。**应用侧生成 ID 是唯一真正可移植的方案**：Snowflake `long`（紧凑、近单调、利于索引）或 UUIDv7/ULID（RFC 9562，时间有序）。
4. **不可移植的语法陷阱**：upsert（`ON DUPLICATE KEY UPDATE` / `ON CONFLICT` / `MERGE` 三方不同，且 SQL Server 官方劝退 `MERGE`）、JSON 列、布尔类型、时区类型、标识符引用（反引号 vs 双引号）、`SKIP LOCKED`（**SQL Server 没有，只有 `READPAST` 提示**）、`FOR UPDATE`（SQL Server 用提示）。
5. **迁移工具**：Flyway 需按方言分目录；Liquibase 的 changelog 抽象能覆盖“容易的 80%”，遇到 JSON/布尔/时区做类型和性能索引时仍要写 `dbms` 标记的重复 changeset。两者最终都收敛到“难的那 20% 按库写”。

**诚实的反面意见**：数据库无关的持久层会把你锁死在各方言的**最小交集**——不能用于任何库的高级特性（JSON 算子、`SKIP LOCKED`、`RETURNING`、部分索引），且第二种数据库通常只在演示里跑过、从未承压，真到迁移那天才暴露漂移。**每一天都在为一个可能永远不会发生的迁移付税。**

来源：[ON DUPLICATE KEY](https://dev.mysql.com/doc/refman/8.4/en/insert-on-duplicate.html)、[PG INSERT](https://www.postgresql.org/docs/current/sql-insert.html)、[SQL Server MERGE 警告](https://learn.microsoft.com/en-us/sql/t-sql/statements/merge-transact-sql)、[SQL Server 表提示](https://learn.microsoft.com/en-us/sql/t-sql/queries/hints-transact-sql-table)、[MyBatis sqlmap](https://mybatis.org/mybatis-3/sqlmap-xml.html)、[RFC 9562](https://www.rfc-editor.org/rfc/rfc9562)

## 四、AI 个性化引擎结论

- **学习者建模的性价比分层**：BKT / Elo 式评分 / **FSRS** 都是每次交互 O(1)、一行数据即可承载，是当前的落地点；DKT（深度知识追踪）需要 GPU 训练与按领域重训，**不适合放进第一版**。FSRS 已是间隔重复的事实标准（fsrs4anki 6.1.3，2026-09-08；Anki 26.09.3 默认调度器）。来源：[fsrs4anki](https://github.com/open-spaced-repetition/fsrs4anki/releases)、[Anki](https://github.com/ankitects/anki/releases)
- **前置知识建模**：课程大纲的有向无环图用关系表（`skill_prereq`）+ 拓扑排序就够；图数据库（Neo4j/NebulaGraph）只有把多跳遍历或 GraphRAG 做成产品功能才值得引入。
- **RAG 现状**：2023 年那种“定长切块 + 余弦 top-k、无重排”已不被认为够用；当前基线是结构化切块 → 混合检索（BM25 + 向量 + 元数据过滤）→ 交叉编码器重排 → 带引用的上下文组装，并且**必须做评测**（RAGAS 0.4.3）。来源：[RAGAS](https://github.com/explodinggradients/ragas/releases)
- **向量存储的关键坑**：MySQL 9.x 确有 `VECTOR` 类型，但 **`DISTANCE()`/向量排序只在 HeatWave on OCI / MySQL AI 上可用，社区版和企业版都用不了**——即“MySQL 社区版能存向量但不能排序”。自托管的关系型方案应选 pgvector（0.8.7）；独立库选 Qdrant（1.19.2）/ Milvus（3.0.2）/ Weaviate（1.40.0）/ OpenSearch 3.9。来源：[MySQL 向量函数](https://dev.mysql.com/doc/refman/9.4/en/vector-functions.html)、[pgvector](https://github.com/pgvector/pgvector/tags)
- **引擎选型**：Java + Spring 栈选 **Spring AI**（第一方、与 Boot 4.1 对齐、MCP 与可观测性齐备）；只有明确需要 LangChain4j 的 AI Service 抽象或同时做 Quarkus 才选它。
- **运行时**：token 流式默认走 **SSE**（不是 WebSocket，除非需要双向），Spring AI 流式返回 `Flux`，要避免占用 Web 线程；确定性逻辑（FSRS 排程、评分、前置校验、内容 CRUD）**不要交给 Agent**，Agent 只用于学习路径编排与开放式答疑这类规划/对话场景。
- 未能核实：Duolingo「Birdbrain」官方页面（博客地址均 404）；Khanmigo 2026 年实际底层模型（站点仍写 GPT-4）。

## 五、前端结论

- **认证型应用不需要 SSR**：学习平台绝大部分界面在登录后，Vite SPA + React Router 数据模式是最简且最省的组合；SSR 会带来服务端成本、cookie 鉴权坑与 RSC 安全面。只有在公开课程页/营销页需要 SEO 时才转向 Next.js。
- **状态分层共识**：服务端数据交给 TanStack Query，临时 UI 状态交给 Zustand/Jotai，**不要把服务端数据镜像进客户端 store**。
- **微前端已非主流**：Module Federation 仍在维护，但业界已转向单仓 + 包边界（pnpm workspaces + Turborepo/Nx）。只有“独立部署的团队需要运行时组合”才用微前端。
- **流式 UI**：Vercel AI SDK 的 `useChat` 走 SSE，与 shadcn/ui 的 chat 组件配套。

## 六、来源可信度说明

- 所有版本号来自官方 release 元数据或官方文档页。
- 三条信息当日无法核实：TanStack Start 稳定状态、Radix 版本、BIRD/Khanmigo 产品细节；另有两篇经典文章（“You are not Google”、Service-based architecture 书页）在本环境无法抓取，故只作定性引用未附 URL 断言。
