---
name: code-nest-project-spec
description: Code-Nest 多模块全栈项目协作规范与落点导航。Use when modifying this repository for feature development, bug fixing, refactor, API change, SQL migration, or frontend-backend联调 so changes land in the correct module with consistent conventions.
---

# Code-Nest 项目规范

## 1. 目标

在本仓库内进行任何开发时，先用本规范完成三件事：

1. 快速定位改动应该落在哪个模块。
2. 按既有约定实现，避免破坏统一风格。
3. 用最小必要命令验证改动可编译、可运行、可联调。
4. 在我给需求后，你可以调用一些已经有的skill 来帮助你完善这些功能，更好的规范你

## 2. 仓库全景

- 后端是 Maven 多模块工程，根 `pom.xml` 管理版本与子模块。
- 后端启动入口在 `xiaou-application`，统一上下文路径是 `/api`。
- 前端分为两套：
1. `vue3-admin-front`（管理端，默认端口 `3000`）
2. `vue3-user-front`（用户端，默认端口 `3001`）
- 每个前端同时支持 Electron 打包，入口在各自 `electron/`。
- 数据库脚本在 `sql/`，采用“基线 + 版本增量”结构。
- PRD 与技术文档在 `docs/`。

## 3. 目录速查

- 后端聚合入口：`xiaou-application/src/main/java/com/xiaou/CodeNestApplication.java`
- 全局配置：`xiaou-application/src/main/resources/application.yml`
- 公共能力：`xiaou-common/`
- 用户端 API 封装：`vue3-user-front/src/api/`
- 管理端 API 封装：`vue3-admin-front/src/api/`
- 用户端路由：`vue3-user-front/src/router/index.js`
- 管理端路由：`vue3-admin-front/src/router/index.js`
- SQL 版本脚本：`sql/v*/`
- 需求文档：`docs/PRD/`

## 4. 模块映射（后端）

优先按业务模块改，不要在 `xiaou-application` 塞业务逻辑。

| 模块 | 主要职责 |
| --- | --- |
| `xiaou-common` | 统一返回体、异常处理、鉴权配置、通用工具、公共 Mapper |
| `xiaou-system` | 管理员认证、系统日志、系统管理能力 |
| `xiaou-user` | 用户注册登录与用户资料 |
| `xiaou-interview` | 面试题库、题单、学习记录与掌握度 |
| `xiaou-community` | 社区帖子、评论、标签、分类、用户态社区能力 |
| `xiaou-moment` | 动态广场与互动 |
| `xiaou-blog` | 博客文章、分类、标签 |
| `xiaou-codepen` | 代码作品、模板、收藏/评论/Fork |
| `xiaou-resume` | 在线简历、模板、分析与导出相关 |
| `xiaou-filestorage` | 文件上传、存储配置、迁移 |
| `xiaou-notification` | 通知中心 |
| `xiaou-chat` | 聊天室与实时消息管理 |
| `xiaou-sensitive` | 敏感词词库、策略、统计、检测接口实现 |
| `xiaou-knowledge` | 知识图谱管理与展示 |
| `xiaou-version` | 版本历史 |
| `xiaou-moyu` | 摸鱼工具（日历、热榜、Bug 商店等） |
| `xiaou-points` | 积分与抽奖 |
| `xiaou-plan` | 计划打卡 |
| `xiaou-mock-interview` | AI 模拟面试流程 |
| `xiaou-flashcard` | 闪卡学习 |
| `xiaou-oj` | 在线判题、提交、题解、评论、排行 |
| `xiaou-sql-optimizer` | 慢 SQL 分析与历史记录 |
| `xiaou-ai` | 统一 AI 服务（Coze 工作流调用） |
| `xiaou-user-api` | 用户跨模块 API 契约（接口与 DTO） |
| `xiaou-sensitive-api` | 敏感词跨模块 API 契约（接口与 DTO） |

## 5. 路由与鉴权约定（强约束）

按 `SaTokenConfig` 的现有规则组织接口路径：

- 管理端接口通常走 `/auth/**` 或 `/admin/**`
- 用户端接口通常走 `/user/**`
- 公共接口按业务定义（如 `/oj/**`、`/community/**`、`/version/**`）
- 验证码：`/captcha/**`（免登录）
- Swagger：`/v3/api-docs/**`、`/swagger-ui/**`（免登录）

鉴权工具类必须复用：

- 管理员：`StpAdminUtil`
- 用户：`StpUserUtil`

需要管理员权限的方法优先使用 `@RequireAdmin`。

## 6. 返回体与错误码约定（强约束）

后端接口统一返回 `Result<T>`，定义在：

- `xiaou-common/src/main/java/com/xiaou/common/core/domain/Result.java`
- `xiaou-common/src/main/java/com/xiaou/common/core/domain/ResultCode.java`

关键业务码：

- `200`：成功
- `701`：Token 无效
- `702`：Token 过期
- `703`：权限不足
- `704`：账号禁用

前端拦截器已按上述业务码处理跳转与提示，不要随意改语义。

## 7. 后端开发落地步骤

1. 先确认业务归属模块，再改对应模块下 `controller/service/mapper`。
2. 新增接口时，保持现有路径风格（admin/user/pub）。
3. DTO、枚举、domain 放在当前业务模块，不要塞到 `xiaou-application`。
4. 公共逻辑先考虑放 `xiaou-common`，避免复制代码。
5. 跨模块调用先复用 API 契约模块（`xiaou-user-api`、`xiaou-sensitive-api`）。
6. MyBatis XML 一般在 `src/main/resources/mapper`。
7. OJ 模块是特例，Mapper XML 在 `xiaou-oj/src/main/java/com/xiaou/oj/mapper/`。
8. 需要新增模块时，同时更新根 `pom.xml` 的 `<modules>` 与 `xiaou-application/pom.xml` 依赖。

## 8. 前端开发落地步骤

1. 先看路由文件定位页面：`src/router/index.js`。
2. 再看 API 封装：`src/api/*.js`。
3. 最后改 `src/views/**` 与 `src/components/**`。
4. 两端都用 `@` 指向 `src`，按现有别名写法保持一致。
5. 请求统一走 `src/utils/request.js`，不要重复造 axios 实例。
6. 用户端与管理端 token key 不同：
1. 用户端常用 `user_token`
2. 管理端常用 `token`
7. 用户端主要页面在 `vue3-user-front/src/views/`：
1. `interview`、`oj`、`mock-interview`
2. `resume`、`community`、`moments`
3. `blog`、`codepen`、`flashcard`
4. `points`、`plan`、`team`、`knowledge`
8. 管理端主要页面在 `vue3-admin-front/src/views/`：
1. `user`、`interview`、`oj`
2. `community`、`moments`、`chat`
3. `blog`、`codepen`、`resume`
4. `sensitive`、`filestorage`、`logs`、`system`

## 9. SQL 与数据变更约定

1. 结构变更优先写版本增量脚本到 `sql/vX.Y.Z/`。
2. 脚本命名使用业务语义，例如 `oj_tables.sql`、`flashcard.sql`。
3. 表与字段命名沿用 `snake_case`。
4. 保持 `utf8mb4` 与合适索引策略。
5. 涉及线上兼容时，避免直接破坏性 `DROP`，优先可回滚的增量迁移。
6. 若功能需要初始化数据，提供 `INSERT` 示例并注明执行顺序。

## 10. 配置与安全约定

1. 主配置在 `xiaou-application/src/main/resources/application*.yml`。
2. 私密配置放 `application-sec.yml`，不要把真实密钥写入公开文档。
3. Coze 工作流 ID 统一在 `CozeWorkflowEnum` 管理。
4. 监控相关配置与文档：
1. `management.*` 在 `application.yml`
2. `docs/Prometheus监控部署指南.md`
3. `docker/monitoring/`

## 11. 不要手改的内容

1. `pom-xml-flattened`（由 flatten 插件生成）。
2. `target/`、`dist/`、`out/`（构建产物）。
3. `node_modules/`（依赖目录）。

## 12. 改动后验证清单

后端最小验证：

```bash
mvn -pl xiaou-application -am clean package -DskipTests
```

后端本地启动：

```bash
mvn -pl xiaou-application -am spring-boot:run
```

管理端本地启动：

```bash
cd vue3-admin-front
npm run dev
```

用户端本地启动：

```bash
cd vue3-user-front
npm run dev2
```

按需补充：

```bash
cd vue3-admin-front && npm run lint
cd vue3-user-front && npm run lint
```

## 13. 需求分析优先级

收到需求后按以下顺序判断：

1. 这是管理端需求、用户端需求，还是双端需求。
2. 是否需要后端接口变更。
3. 是否需要 SQL 结构变更。
4. 是否影响鉴权、错误码、日志、监控。
5. 是否需要同步 PRD 或部署文档。

## 14. 文档使用建议

需求细节不明确时，优先查：

1. `README.md`（整体能力、启动方式、版本历史）。
2. `docs/PRD/*.md`（模块级产品规则）。
3. `docs/upgrade/**`（专项升级方案）。
4. `docs/coze/*.md`（AI 工作流配置）。

优先复用现有模块与路径约定，避免“新建平行体系”。
