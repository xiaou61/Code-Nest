# 文档同步基线

本页记录文档站与源码之间的同步基线。当你怀疑某个模块文档已经和源码不一致时，可以按照本页的步骤快速定位漂移。

## 基线版本

| 项 | 值 |
| --- | --- |
| 项目版本 | v2.2.2 |
| 基线提交 | `9fed3fb chore(release): prepare v2.2.2 baseline` |
| 文档站提交 | 同上 |
| 文档页数量 | 约 45 个 .md 文件 |
| 源码模块数 | 24 个 xiaou-* 模块 |
| Controller 数 | 100 个 |
| REST 端点数 | 约 757 个 |
| 数据库表数 | 142 张表（基线 136 + 增量 6） |

## 文档页与源码对应关系

### 模块文档（docs-site/modules/）

| 文档页 | 对应模块 | 核心源码目录 | 基线检查要点 |
| --- | --- | --- | --- |
| auth.md | xiaou-user, xiaou-system | `xiaou-user/.../controller/user/AuthController.java`、`xiaou-system/.../controller/admin/AuthAdminController.java` | 登录注册接口、Sa-Token 配置 |
| user-account.md | xiaou-user | `xiaou-user/.../controller/user/UserController.java`、`xiaou-user/.../controller/admin/AdminUserController.java` | 用户 CRUD、个人中心 |
| system-ops.md | xiaou-system | `xiaou-system/.../controller/admin/` | 仪表盘、日志、角色权限 |
| dashboard-logs.md | xiaou-system | `xiaou-system/.../controller/admin/DashboardController.java`、`LogController.java` | 统计接口、日志 CRUD |
| interview.md | xiaou-interview | `xiaou-interview/.../controller/` | 题库、分类、学习记录 |
| interview-and-growth.md | xiaou-interview, xiaou-plan | 驾驶舱、成长自动驾驶 | 驾驶舱接口、成长分数公式 |
| mock-interview-job-battle.md | xiaou-mock-interview | `xiaou-mock-interview/.../controller/` | 模拟面试、求职闭环、岗位匹配 |
| plan-team.md | xiaou-plan, xiaou-team | `xiaou-plan/.../controller/user/`、`xiaou-team/.../controller/` | 计划打卡、小组 CRUD |
| flashcard.md | xiaou-flashcard | `xiaou-flashcard/.../controller/` | 卡组、学习、SM-2 |
| oj.md | xiaou-oj | `xiaou-oj/.../controller/` | 题目、判题、赛事 |
| community.md | xiaou-community | `xiaou-community/.../controller/` | 帖子、评论、分类 |
| community-content.md | xiaou-community, xiaou-moment, xiaou-blog | 三模块用户端+管理端 Controller | 社区、动态、博客交叉 |
| moments.md | xiaou-moment | `xiaou-moment/.../controller/` | 动态发布、互动 |
| blog.md | xiaou-blog | `xiaou-blog/.../controller/` | 博客文章、分类标签 |
| codepen.md | xiaou-codepen | `xiaou-codepen/.../controller/` | 代码工坊、模板 |
| resume.md | xiaou-resume | `xiaou-resume/.../controller/` | 简历模板、分析 |
| file-storage.md | xiaou-filestorage | `xiaou-filestorage/.../controller/` | 上传、下载、存储配置、迁移 |
| notification.md | xiaou-notification | `xiaou-notification/.../controller/` | 通知 CRUD、模板 |
| chat.md | xiaou-chat | `xiaou-chat/.../controller/`、`xiaou-chat/.../websocket/` | REST + WebSocket |
| points.md | xiaou-points | `xiaou-points/.../controller/` | 积分、签到、抽奖 |
| sensitive.md | xiaou-sensitive | `xiaou-sensitive/.../controller/` | 敏感词、白名单、版本 |
| version-history.md | xiaou-version | `xiaou-version/.../controller/` | 版本发布 |
| tools-moyu-version.md | xiaou-moyu, xiaou-version | 摸鱼工具 + 版本历史 | 日历、薪资、Bug 商店 |
| knowledge.md | xiaou-knowledge | `xiaou-knowledge/.../controller/` | 图谱、节点 |
| learning-assets.md | xiaou-learning-asset | `xiaou-learning-asset/.../controller/` | 资产记录、审核 |
| sql-optimizer.md | xiaou-sql-optimizer | `xiaou-sql-optimizer/.../controller/` | SQL 监控、优化 |
| ai-runtime.md | xiaou-ai, xiaou-system(AI Config) | `xiaou-ai/.../controller/`、`xiaou-system/.../AiConfigController.java` | AI 配置、治理 |
| dev-tools.md | 前端本地能力 | 无后端 | JSON 工具、文本 Diff |
| user-operations.md | 全模块用户端 | 截图目录 | 操作手册 |
| admin-operations.md | 全模块管理端 | 截图目录 | 操作手册 |

### 参考文档（docs-site/reference/）

| 文档页 | 数据来源 | 基线检查要点 |
| --- | --- | --- |
| permission-boundaries.md | `xiaou-common/.../aspect/AdminAuthAspect.java`、`StpInterfaceImpl.java`、`SaTokenConfig.java` | @RequireAdmin 切面逻辑、白名单路径、StpInterface 实现 |
| response-errors.md | `xiaou-common/.../domain/Result.java`、`GlobalExceptionHandler.java`、`ResultCode.java` | 错误码枚举、异常处理映射、NotLoginException 子类型 |
| websocket.md | `xiaou-chat/.../websocket/`、`ChatWebSocketTicketService.java`、`ChatRateLimitService.java` | ws-ticket 参数、限流配置、消息类型 |
| database-tables.md | `sql/MySql/code_nest.sql`、各模块 Mapper XML | 表数量、索引、字段约定 |
| frontend-rendering-security.md | `vue3-user-front/src/utils/markdown.js`、`vue3-admin-front/src/utils/markdown.js` | MarkdownIt 配置、DOMPurify 白名单、highlight.js 语言 |
| feature-coverage.md | 全模块 Controller 源码 | API 端点数、@RequireAdmin 方法数、Controller 分布 |
| env-vars.md | `xiaou-application/src/main/resources/application*.yml`、`@Value` 注入 | 环境变量、profile 差异、安全密钥 |
| docs-sync-baseline.md | 本页 | 基线版本、漂移检测 |

### 架构文档（docs-site/architecture/）

| 文档页 | 数据来源 | 基线检查要点 |
| --- | --- | --- |
| overview.md | 项目整体结构 | 模块清单、技术栈版本 |
| database.md | SQL 脚本、MyBatis 配置 | 表前缀映射、索引策略 |
| frontend.md | Vue3 前端项目 | 路由结构、状态管理 |
| ai-architecture.md | AI 模块源码 | AI 配置、RAG、回归测试 |

## 漂移检测步骤

当怀疑文档与源码不一致时，按以下步骤排查：

### 1. API 端点漂移

```bash
# 统计某模块的端点数
grep -r "@GetMapping\|@PostMapping\|@PutMapping\|@DeleteMapping" xiaou-INTERVIEW/src/main/java/ | wc -l

# 对比文档中记录的端点数
# 如果不一致，逐 Controller 检查新增/删除的接口
```

### 2. 数据库表漂移

```bash
# 统计主库表数
grep "CREATE TABLE" sql/MySql/code_nest.sql | wc -l

# 检查新增版本脚本
ls sql/v*/  | sort

# 对比文档中记录的表数量（应为 142）
```

### 3. 权限注解漂移

```bash
# 检查 @RequireAdmin 使用量
grep -r "@RequireAdmin" xiaou-*/src/main/java/ | wc -l

# 检查是否有人偷偷用了 @SaCheckRole 或 @SaCheckPermission
grep -r "@SaCheckRole\|@SaCheckPermission" xiaou-*/src/main/java/ | wc -l
# 预期结果：0

# 检查是否有人用了 @SaIgnore（项目约定只用 SaTokenConfig 白名单）
grep -r "@SaIgnore" xiaou-*/src/main/java/ | wc -l
# 预期结果：0
```

### 4. 错误码漂移

```bash
# 检查 ResultCode 枚举值数量
grep "^[[:space:]]*[A-Z_]*(" xiaou-common/src/main/java/com/xiaou/common/core/domain/ResultCode.java | wc -l
# 基线值：27

# 检查 GlobalExceptionHandler 处理的异常类型数量
grep "@ExceptionHandler" xiaou-common/src/main/java/com/xiaou/common/exception/GlobalExceptionHandler.java | wc -l
# 基线值：18
```

### 5. 配置项漂移

```bash
# 检查 @Value 注入点数量
grep -r "@Value" xiaou-*/src/main/java/ | wc -l

# 检查 YAML 配置项数量
grep -r "^[a-z]" xiaou-application/src/main/resources/application.yml | wc -l
```

### 6. 前端 v-html 漂移

```bash
# 检查 v-html 使用量
grep -r "v-html" vue3-user-front/src/ | wc -l
# 基线值：16 个文件

grep -r "v-html" vue3-admin-front/src/ | wc -l
# 基线值：3 个文件

# 检查是否有 v-html 没有走统一渲染链路
grep -r "v-html" vue3-user-front/src/ | grep -v "renderMarkdown\|sanitizeHtml\|escapeHtml"
```

## 同步规则

### 新增功能时

1. 新增 Controller 方法 → 更新对应模块文档的"API 端点"表
2. 新增数据表 → 更新 `database-tables.md` 的表索引和 `code_nest.sql` 基线
3. 新增 \`@RequireAdmin\` 方法 → 更新 \`permission-boundaries.md\` 和 \`feature-coverage.md\` 的方法分布表
4. 新增 \`@Value\` 注入 → 更新 \`env-vars.md\` 的配置表
5. 新增 \`v-html\` 使用 → 更新 \`frontend-rendering-security.md\` 的审计表
6. 新增 \`ResultCode\` 枚举 → 更新 \`response-errors.md\` 的枚举表
7. 新增 WebSocket 事件类型 → 更新 \`websocket.md\` 的事件表

### 修改功能时

1. 修改接口参数或返回值 → 更新对应模块文档的接口说明
2. 修改 DOMPurify 白名单 → 同步更新用户端和管理端两个 \`markdown.js\`，更新 \`frontend-rendering-security.md\`
3. 修改 SaTokenConfig 白名单 → 更新 \`permission-boundaries.md\` 的白名单表
4. 修改限流配置 → 更新 \`websocket.md\` 和 \`env-vars.md\` 的限流表
5. 修改错误码或异常处理 → 更新 \`response-errors.md\` 的映射表

### 删除功能时

1. 删除 Controller 方法 → 从对应模块文档移除相关条目
2. 删除数据表 → 更新 \`database-tables.md\`，添加版本增量脚本说明表已废弃
3. 不要直接删除文档页 → 标记为"已废弃"并添加指向替代页的链接

## 基线数值速查

以下是关键数值的基线值，用于快速检测漂移：

| 指标 | 基线值 | 检测命令 |
| --- | --- | --- |
| 数据库表数 | 142 | \`grep "CREATE TABLE" sql/MySql/code_nest.sql | wc -l\` + 增量 |
| Controller 类数 | 100 | \`find . -name "*Controller.java" | wc -l\` |
| REST 端点数 | ~757 | \`grep -r "@Get/Post/Put/Delete/PatchMapping" --include="*.java" | wc -l\` |
| @RequireAdmin 方法数 | ~270 | \`grep -r "@RequireAdmin" --include="*.java" | wc -l\` |
| @SaCheckRole 使用数 | 0 | \`grep -r "@SaCheckRole" --include="*.java" | wc -l\` |
| @SaCheckPermission 使用数 | 0 | \`grep -r "@SaCheckPermission" --include="*.java" | wc -l\` |
| @SaIgnore 使用数 | 0 | \`grep -r "@SaIgnore" --include="*.java" | wc -l\` |
| ResultCode 枚举值数 | 27 | \`grep "^[[:space:]]*[A-Z_]*(" ResultCode.java | wc -l\` |
| GlobalExceptionHandler 异常类型 | 18 | \`grep "@ExceptionHandler" GlobalExceptionHandler.java | wc -l\` |
| 用户端 v-html 文件数 | 16 | \`grep -rl "v-html" vue3-user-front/src/ | wc -l\` |
| 管理端 v-html 文件数 | 3 | \`grep -rl "v-html" vue3-admin-front/src/ | wc -l\` |
| highlight.js 注册语言数 | 14 | 检查 \`markdown.js\` 中 \`hljs.registerLanguage\` 调用数 |
| DOMPurify FORBID_TAGS 数 | 5 | 检查 \`markdown.js\` 中 \`FORBID_TAGS\` 数组长度 |
| DOMPurify FORBID_ATTR 数 | 5 | 检查 \`markdown.js\` 中 \`FORBID_ATTR\` 数组长度 |

## 源码导航

| 文件 | 说明 |
| --- | --- |
| \`docs-site/reference/docs-sync-baseline.md\` | 本页 |
| \`sql/MySql/code_nest.sql\` | 数据库基线脚本 |
| \`xiaou-common/.../domain/ResultCode.java\` | 错误码枚举基线 |
| \`xiaou-common/.../exception/GlobalExceptionHandler.java\` | 异常处理基线 |
| \`xiaou-common/.../aspect/AdminAuthAspect.java\` | 权限注解基线 |
| \`xiaou-common/.../config/SaTokenConfig.java\` | 白名单配置基线 |
| \`vue3-user-front/src/utils/markdown.js\` | 前端渲染安全基线 |
| \`vue3-admin-front/src/utils/markdown.js\` | 管理端渲染安全基线 |
