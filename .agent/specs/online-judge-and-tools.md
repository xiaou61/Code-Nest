---
artifact: spec
status: active
scope: 在线判题与效率工具相关模块
---

# 在线判题与效率工具

覆盖 `xiaou-oj`、`xiaou-sql-optimizer`、`xiaou-knowledge`、`xiaou-version`、`xiaou-moyu` 五个后端模块（约 182 个 Java 文件），以及 `docker/go-judge/` 判题沙箱编排。五者共同点是"工具型能力"：OJ 面向算法练习，SQL 优化面向性能调优，知识图谱面向学习导航，版本历史面向信息发布，摸鱼模块面向开发者娱乐。除 `xiaou-sql-optimizer` 依赖 `xiaou-ai`、`xiaou-oj` 依赖 `xiaou-points`/`xiaou-user-api` 外，全部只依赖 `xiaou-common-*`。

## 模块总览

| 模块 | 职责一句话 | 关键入口（包/类/文件） |
| --- | --- | --- |
| xiaou-oj | 在线判题：题库/标签/题解/测试用例/提交/评论/赛事/排行榜，判题走外部 go-judge 沙箱 | `com.xiaou.oj.judge.JudgeService`、`judge.sandbox.GoJudgeClient`、`judge.CodeRunnerService`、`contest.ContestRankingCalculator`、`service.OjRankingService`、`controller/{pub,admin}`（8 个 Controller） |
| xiaou-sql-optimizer | 慢 SQL 智能分析与"工作台 2.0"（分析/重写/批量/收益对比/案例库） | `controller.SqlOptimizerController`、`service.impl.SqlOptimizerServiceImpl`、`mapper.SqlOptimizeRecordMapper`（注解 SQL） |
| xiaou-knowledge | 知识图谱：图谱 + 树形节点（节点外链飞书文档），管理端维护、用户端只读 | `controller.admin.AdminKnowledgeMapController`/`AdminKnowledgeNodeController`、`controller.pub.PubKnowledgeMapController`、`service.impl.KnowledgeNodeServiceImpl` |
| xiaou-version | 版本更新历史的发布与展示（时间轴/详情/搜索/最新 N 条） | `controller.pub.VersionHistoryController`、`controller.admin.VersionHistoryAdminController`、`service.impl.VersionHistoryServiceImpl` |
| xiaou-moyu | 开发者娱乐工具集合：程序员日历、每日内容、Bug 商店、热榜、时薪计算器 | `controller.{DeveloperCalendarController,DailyContentController,BugStoreController,HotTopicController,SalaryCalculatorController}` + 3 个 admin Controller、`task.HotTopicTask`、`config.MoyuConfig` |
| docker/go-judge | 判题沙箱运行时（`criyle/go-judge` + 多语言编译器） | `docker/go-judge/Dockerfile`、`docker-compose.yml`、`README.md` |

## 模块详情

## 在线判题（xiaou-oj）

**职责与边界**
- 负责题目、标签、题解、测试用例、提交记录、题目评论、赛事（contest）与榜单接口前缀为 `/oj`（用户端）与 `/admin/oj`（管理端）。
- 判题本身**不在本进程内执行用户代码**：`JudgeService`/`CodeRunnerService` 通过 HTTP 调用外部 go-judge 沙箱，模块内不含任何 `ProcessBuilder`/本地编译实现。
- 边界外：积分发放委托 `xiaou-points`（`PointsService.grantSystemPoints`），排行榜用户昵称/头像委托 `xiaou-user-api`（`UserInfoApiService.getSimpleUserInfoBatch`）。

**核心领域对象**（`com.xiaou.oj.domain`，共 10 个）
`OjProblem`（含 `timeLimit` ms、`memoryLimit` MB、`difficulty`、`sampleInput/Output`、`acceptedCount`、`submitCount`、`status` 0 隐藏/1 公开）、`OjProblemTag`、`OjTestCase`（`input`/`expectedOutput`/`isSample`）、`OjSolution`、`OjSubmission`（`status`/`timeUsed`/`memoryUsed`/`passCount`/`totalCount`/`errorMessage`/`contestId`）、`OjProblemComment`、`OjProblemCommentLike`、`OjContest`、`OjContestProblem`、`OjContestParticipant`；枚举 `JudgeLanguage`、`SubmissionStatus`、`ProblemDifficulty`。

**判题通信与沙箱（重点）**
- 协议：go-judge REST API。`GoJudgeClient.run` → `POST {oj.judge.go-judge-url}/run`；请求体 `{"cmd":[{args, env(PATH), cpuLimit(ns), memoryLimit(bytes), procLimit=50, files:[stdin content, stdout collector max=10240, stderr collector max=10240], copyIn, copyOut, copyOutCached}]}`；`deleteFile` → `DELETE /file/{fileId}`。（`judge/sandbox/GoJudgeClient.java`）
- 传输：`RestTemplate` bean `ojRestTemplate`，connect 5s / read 30s（`judge/config/OjConfig.java`）。**无 MQ、无 Redis 队列**，只有进程内 `@Async`（`JudgeService.judge`，`xiaou-bootstrap` 的 `ApplicationTaskExecutorConfig` 提供线程池；`CodeNestApplication` 带 `@EnableAsync`）。
- 配置（`OjJudgeProperties` + `xiaou-bootstrap/src/main/resources/application.yml` L389-399）：`go-judge-url` 代码默认 `http://localhost:5050`，实配 `http://154.222.18.220:5050`；`max-compile-time=10000`ms；`default-time-limit=2000`ms；`default-memory-limit=256`MB。
- 沙箱镜像（`docker/go-judge/Dockerfile`）：`FROM criyle/go-judge:latest`，root 安装 `default-jdk gcc g++ python3 golang nodejs`；`docker-compose.yml` 暴露 `5050`，`privileged: true`，`GOJUDGE_PARALLELISM=4`，`GOJUDGE_FILE_STORE=/tmp/gojudge`，限制 1G/2CPU，日志 50m×3。
- 超时/资源：题目的 `timeLimit`/`memoryLimit` 换算为 ns/bytes 传给沙箱；编译阶段改用 `max-compile-time` 作 CPU 上限，但**内存上限复用题目的 memoryLimit**；`procLimit` 固定 50；stdout/stderr 各截断 10KB。

**支持语言与命令**（`judge/strategy`，6 种）

| 语言 | 源文件 | 编译 | 运行 |
| --- | --- | --- | --- |
| java | Main.java | `/usr/bin/javac Main.java`（copyOutCached `Main.class`） | `/usr/bin/java Main` |
| cpp | main.cpp | `/usr/bin/g++ -o main main.cpp -O2` | `main` |
| c | main.c | `/usr/bin/gcc -o main main.c -O2` | `main` |
| go | main.go | `/usr/bin/go build -o main main.go` | `main` |
| python | main.py | 无（`getCompileArgs()` 返回 null） | `/usr/bin/python3 main.py` |
| javascript | main.js | 无 | `/usr/bin/node main.js` |

**对外接口**
- 用户端 `/oj`：`POST /run`（自由运行）、`POST /test`（自测）、`POST /submit`、`GET /submissions/{id}`、`POST /submissions/my`、`GET /problems/{problemId}/submissions`、`GET /statistics/me`；`POST /problems/list`、`GET /problems/{id}`、`GET /tags`、`GET /problems/{id}/solutions`、`GET /ranking?type=all|weekly`、`GET /daily-problem`；评论 `POST /problems/{problemId}/comments`、`POST .../comments/create`、`POST /comments/{id}/reply`、`POST|DELETE /comments/{id}/like`、`POST /comments/{id}/replies`。
- 用户端赛事 `/oj/contests`：`POST /list`、`GET /{id}`、`POST /{id}/join`、`GET /{id}/ranking`。
- 管理端（全部 `@RequireAdmin`）：`/admin/oj/problems`（7 个：创建/更新/删除/详情/列表/标签查询/建标签）、`/admin/oj/solutions`（4）、`/admin/oj/test-cases`（4）、`/admin/oj/contests`（6，含 `POST /{id}/status`）。
- 注意：`/oj/**` 用户端未使用 `@RequireAdmin`，登录态由 `StpUserUtil.getLoginIdAsLong()` 隐式要求；`/oj/problems/list`、`/tags`、`/ranking`、`/daily-problem` 在代码层面无登录调用（是否由全局 Sa-Token 拦截器兜底：未确认）。

**数据表**（10 个 Mapper XML，特例位于 `xiaou-oj/src/main/java/com/xiaou/oj/mapper/*.xml`，非 `src/main/resources`）
`oj_problem`、`oj_problem_tag`、`oj_problem_tag_relation`、`oj_test_case`、`oj_solution`、`oj_submission`、`oj_problem_comment`、`oj_problem_comment_like`、`oj_contest`、`oj_contest_problem`、`oj_contest_participant`。

**关键流程：提交 → 判题 → 回写**
1. `OjSubmissionController.submitCode` → `OjSubmissionServiceImpl.submitCode`：`JudgeLanguage.of()` 校验语言、题目存在性、若带 `contestId` 则 `ContestRuleValidator.checkCanSubmit` + 报名校验 + 题目属于赛事校验。
2. `insert oj_submission`（`status=pending`）→ `problemMapper.increaseSubmitCount` → `judgeService.judge(id)` 异步返回 `submissionId`。
3. `JudgeService.judge`（`@Async`）：置 `judging` → 载入题目与全部测试用例（为空直接 `SYSTEM_ERROR "没有测试用例"`）→ 选择 `JudgeStrategy`。
4. 编译：`getCompileArgs() != null` 时执行编译，产出经 `copyOutCached` 得到 `fileIds`；非 Accepted 或退出码非 0 → `COMPILE_ERROR`（stderr 截断 4000 字符）。
5. 运行：逐用例 `go-judge` 执行（有编译产物则用 `cachedFileIn` 引用 fileId，解释型语言直接 `copyIn` 源码）；按沙箱 `status` 判定 `TIME_LIMIT_EXCEEDED` / `MEMORY_LIMIT_EXCEEDED` / 非 0 退出 `RUNTIME_ERROR` / 输出比对失败 `WRONG_ANSWER`，任一失败即回写并 return（不跑剩余用例）。
6. 全部通过 → `ACCEPTED`，`maxTime`/`maxMemory` 取各用例最大值；首次 AC（`existsAccepted` 在更新前判定）→ `increaseAcceptedCount` + 发放积分（easy 100 / medium 200 / hard 500，`PointsType.OJ_AC`）。
7. `finally` 中删除 go-judge 缓存文件；整段被 `catch (Throwable)` 包裹，异常统一落 `SYSTEM_ERROR`。
8. 输出比对规则：`JudgeService.compareOutput` 用 `stripTrailing()` 相等；`CodeRunnerService.compareOutput` 用 `strip()` 相等——**同一仓库两套比对口径**，且均无忽略空白/行尾/浮点容差策略。

**关键流程：排行榜**
- 站点榜 `OjRankingService`（无 Impl，`service` 包下直接 `@Service` 类）→ `OjSubmissionMapper.selectRankingAll/Weekly`：`SELECT user_id, COUNT(DISTINCT problem_id) acceptedCount, COUNT(*) submissionCount FROM oj_submission WHERE status='accepted' [AND create_time>=#{weekStart}] GROUP BY user_id ORDER BY acceptedCount DESC, submissionCount ASC LIMIT 50`；周榜起点为本周一 00:00:00；随后内存填 `rank=i+1` 并批量补用户昵称/头像（缺失时兜底 `"用户"+userId`）。
- 赛事榜 `ContestRankingCalculator`：ACM 规则，按 `createTime,id` 排序重放提交，每题首次 AC 记 `acMinutes + wrongBeforeAc*20` 罚时，排序 `solvedCount DESC → penalty ASC → lastAcTime ASC → userId`，再填 rank。

**限流**
- 未发现任何限流：`xiaou-oj` 全模块 grep `RateLimit|rateLimit` 无匹配；`/oj/run`、`/oj/test`、`/oj/submit` 均无频率/并发闸门。

**依赖关系**：`xiaou-common-core`、`xiaou-common-web`、`xiaou-common-security`、`xiaou-common-persistence`、`xiaou-user-api`、`xiaou-points`、`spring-boot-starter-web`、`mysql-connector-j`、`hutool-all`、lombok；运行期强依赖外部 go-judge 服务与 docker 主机。

**约束与注意事项**
- 判题强依赖外部沙箱进程：沙箱不可达时 `GoJudgeClient` 把异常吞成 `ExecuteResult.systemError`，用户看到的是 `System Error` 而非"判题服务不可用"。
- read timeout 30s 是**整体 HTTP 超时**，而沙箱 CPU 限制按题（默认 2s）下发；沙箱侧排队（parallelism=4）时表现为判题变慢甚至 30s 超时。
- 无判题队列/背压：`@Async` 默认线程池 + 沙箱并发 4，高并发提交会直接压到沙箱。
- 沙箱容器 `privileged: true`，且镜像内 `USER root` 安装编译器；沙箱侧隔离强度依赖 go-judge 自身，本模块未做额外校验。
- 提交代码明文入库（`oj_submission.code`），测试用例全量 `selectByProblemId` 载入内存。
- 编译阶段内存上限复用题目的 `memory_limit`：题目内存设置过小时可能误判编译失败。
- Mapper XML 放在 `src/main/java` 下属于仓库特例；根 `pom.xml` 已把 `src/main/java` 也声明为 resource（`pom.xml` L214-224），因此可正常打包，但与其他模块（`src/main/resources/mapper`）不一致。

## 慢SQL智能优化（xiaou-sql-optimizer）

**职责与边界**：接收原始 SQL + EXPLAIN 结果 + 表结构 + MySQL 版本，调用 `xiaou-ai` 的分析能力产出评分/问题/建议/重写/收益对比，并落库为个人案例。前端前缀 `/user/sql-optimizer`。不直接连数据库执行用户的 SQL。

**核心领域对象**：`domain.SqlOptimizeRecord`（`userId`、`originalSql`、`explainResult`、`explainFormat`、`tableStructures`(JSON)、`mysqlVersion`、`analysisResult`(JSON)、`score`、`isFavorite`、`deleted` 逻辑删除）；DTO 家族含 `SqlAnalyzeRequest(.TableStructure)`、`SqlWorkbenchAnalyzeResponse`、`SqlWorkbenchBatchAnalyze/Compare*`、`SqlWorkbenchRecordPayload`（v2 载荷：`workflowVersion/fallback/analysis/rewrite/compare`）。

**对外接口**（`SqlOptimizerController`，全部通过 `StpUserUtil.getLoginIdAsLong()` 取本人 userId，无 `@RequireAdmin`）
`POST /user/sql-optimizer/analyze`、`POST /workbench/analyze`、`POST /workbench/rewrite`、`POST /workbench/batch-analyze`、`POST /workbench/compare`、`GET /workbench/cases`（9 个查询参数：pageNum/pageSize/favorite/hasRewrite/hasCompare/highestSeverity/sortBy/sortOrder）、`GET /workbench/cases/{id}`、`GET /history`、`GET /{id}`、`POST /favorite/{id}`、`DELETE /history/{id}`。

**数据表**：`sql_optimize_record`（`mapper/SqlOptimizeRecordMapper.java` 全注解 SQL，无 XML：`deleted=0` 过滤、`update_favorite`、`update_analysis_result`、逻辑删除置 `deleted=1`）。

**关键流程**：`analyze` → `JSONUtil.toJsonStr(tableStructures)` → `AiSqlOptimizeService.analyzeSql(...)` → 落库（V1 直接存 `SqlAnalyzeResult`）；`analyzeWorkbench/rewrite` → `analyzeSqlV2` / `analyzeAndRewriteSqlV2` → 组装 v2 payload 落库 → 返回 `recordId + workflowVersion + fallback + analysis + rewrite`；`compare` → `compareSqlV2`，若带 `recordId` 则把 compare 追加进原记录 payload。读取时 `parseRecordPayload` 同时兼容 v1（裸 `SqlAnalyzeResult`）与 v2（含 `analysis/rewrite/compare/workflowVersion` 键）两种历史格式，解析失败按 v1 兜底。

**约束与注意事项**
- 批量上限硬编码 20（`MAX_BATCH_SIZE`），超限抛 `BusinessException("批量分析最多支持20条")`；逐项 try/catch，失败项不影响其他项。
- `getWorkbenchCases` 的过滤/排序/分页**全在内存完成**（先 `selectAllByUserId` 取全量），随记录数增长有性能风险；`pageSize` 上限 50。
- 每次分析/重写都写一条新记录（无去重）；`analyze`（v1）与工作台（v2）并存，`workflowVersion` 用于区分。
- `fallback=true` 表示 AI 侧降级返回，前端需据此提示；降级判定同时看 analysis/rewrite/compare 三者。
- `AiSqlOptimizeService` 内部实现属 `xiaou-ai`，本次未读（是否 HTTP 调模型、超时与重试策略：未确认）。

## 知识图谱（xiaou-knowledge）

**职责与边界**：以"图谱（map）→ 树形节点（node）"两表维护学习路线，节点本身只存标题与外链（`url` 注释为"飞书云文档链接"），不含正文。管理端做 CRUD/发布/排序，用户端只读已发布内容。

**核心领域对象**：`KnowledgeMap`（`title/description/coverImage/userId/nodeCount/viewCount/sortOrder`，内部枚举 `Status`：0 草稿 / 1 已发布 / 2 已隐藏）、`KnowledgeNode`（`mapId`、`parentId`（0 为根）、`title`、`url`、`nodeType`（1 普通/2 重点/3 难点）、`sortOrder`、`levelDepth`、`isExpanded`、`viewCount`、`lastViewTime`）。

**知识图谱存储方式（重点）**：**纯 MySQL 邻接表 + Java 内存构树，没有图数据库、没有递归 CTE、没有缓存。** 表为 `knowledge_map`、`knowledge_node`（`KnowledgeMapMapper.xml`、`KnowledgeNodeMapper.xml`，位于 `src/main/resources/mapper/`）。`KnowledgeNodeServiceImpl.getTreeByMapId` → `selectTreeByMapId` 一次取全图节点 → `groupingBy(parentId)` → `buildTree` 从 `parentId=0` 递归挂 `children`。层级用冗余字段 `level_depth` 维护（创建时 `parent.levelDepth+1`），排序用 `sort_order`（创建时取同级 `selectMaxSortOrder+1`，另支持 `batchUpdateOrder` 批量重排）。节点增删后回写 `knowledge_map.node_count`；删除有子节点的节点被拒绝。

**对外接口**
- 管理端（全部 `@RequireAdmin` + `@Log`）：`POST /admin/knowledge/maps/list`、`GET /{id}`、`POST /maps`（建）、`PUT /{id}`、`POST /{id}/publish`、`POST /{id}/hide`、`DELETE /{id}`、`DELETE /batch`；`GET /admin/knowledge/maps/{mapId}/nodes`、`GET /nodes/{id}`、`POST /maps/{mapId}/nodes`、`PUT /nodes/{id}`、`PUT /maps/{mapId}/nodes/sort`、`DELETE /nodes/{id}`、`GET /maps/{mapId}/nodes/search`。
- 用户端 `/pub/knowledge/maps`：`POST /list`、`GET /{id}`、`GET /{mapId}/nodes`、`GET /{mapId}/nodes/search?keyword=`、`POST /nodes/{nodeId}/view`。详情/树/搜索三处均先校验 `Status.PUBLISHED`，否则 `Result.error(ResultCode.DATA_NOT_EXIST, "知识图谱未发布或不存在")`；`GET /{id}` 成功时顺带 `incrementViewCount`。

**约束与注意事项**：用户端接口未显式登录校验（是否全局拦截：未确认）；搜索为 DB LIKE + 内存构树，无全文索引；全模块无 `CacheStore`/`@Cacheable`/Redis 引用；节点 URL 为外部文档链接，暂无本地内容存储与版本管理。

## 版本更新历史（xiaou-version）

**职责与边界**：维护产品版本条目并通过时间轴对外展示。用户端前缀 `/version`，管理端 `/admin/version`。不涉及构建流水线，纯内容发布。

**核心领域对象**：`domain.VersionHistory`（`versionNumber`、`releaseTime`（`java.util.Date`）、`updateType` 1 重大/2 功能/3 修复/4 其他、`status` 0 草稿/1 已发布/2 已隐藏、`viewCount`、`createdBy`/`updatedBy`）；`dto.VersionHistoryResponse` 额外提供 `updateTypeName`/`statusName` 中文名。

**对外接口**
- 用户端 `/version`：`POST /timeline`、`GET /{id}`、`POST /view`、`POST /search`、`GET /latest?limit=5`（`/timeline` 与 `/search` 目前调用同一 `getPublishedVersionList`）。
- 管理端 `/admin/version`（全部 `@RequireAdmin`）：`POST /list`、`GET /{id}`、`POST /create`、`POST /update`、`POST /delete`、`POST /publish`、`POST /hide`、`POST /unpublish`、`POST /batch/publish`、`POST /batch/hide`、`POST /batch/delete`、`GET /check-version/{versionNumber}?excludeId=`。

**数据表**：`version_history`（`VersionHistoryMapper.xml`，逻辑删除 `logicalDeleteById`/`batchLogicalDelete`，状态切换 `updateStatus(id, status, adminId)`）。

**关键流程/约束**：`createVersion` 先查 `selectByVersionNumber(versionNumber, 0)` 判重，`releaseTime` 用 `DateUtil.parse(...,"yyyy-MM-dd HH:mm:ss")` 解析（失败抛业务异常），`createdBy = StpAdminUtil.getLoginIdAsLong()`；分页用 `PageHelper.startPage(pageNum,pageSize,false)` 手动 `countByCondition`，`finally` 清 PageHelper；`pageSize` 归一化上限 100、下限 10，`pageNum<1` 取 1；`getLatestVersions` 强制 `status=1` 但未单独校验 `limit`（受内部 100 上限保护）。时间类型用 `java.util.Date`，与其余模块普遍的 `LocalDateTime` 不一致。

## 摸鱼工具（xiaou-moyu）

**职责与边界**：一组彼此独立的小工具，**不是聚合网关**——没有统一入口控制器，而是 5 个用户端 Controller + 3 个管理端 Controller，各自直连自己的 Service 与表。含 5 个子功能：

| 子功能 | 前缀 | 用户端接口 | 管理端接口 | 数据表 |
| --- | --- | --- | --- | --- |
| 程序员日历 | `/moyu/developer-calendar` | `GET /today`、`/month/{year}/{month}`、`/events/{date}`、`/events/type/{eventType}`、`/events/major`、`GET|POST /preference`、`POST /events/{id}/toggle-collection`、`GET /collections/events` | `/admin/moyu/developer-calendar`：`GET /events`、`/events/type/{type}`、`/events/{id}`、`POST /events`、`PUT /events/{id}`、`DELETE /events/{id}`、`POST /events/batch-delete`、`POST /events/{id}/status`、`GET /events/statistics` | `developer_calendar_event`、`user_calendar_preference`、`user_calendar_collection` |
| 每日内容 | `/moyu/daily-content` | `GET /today`、`/type/{t}`、`/random/{t}`、`/recommend`、`/popular`、`/language/{lang}`、`/{id}`、`POST /{id}/view`、`/{id}/like`、`/{id}/toggle-collection`、`GET /collections` | `/admin/moyu/daily-content`：`GET /list`、`/type/{t}`、`/{id}`、`POST /`、`PUT /{id}`、`DELETE /{id}`、`POST /batch-delete`、`POST /{id}/status`、`GET /statistics`、`/collections/statistics`、`/popular-ranking` | `daily_content` |
| Bug 商店 | `/moyu/bug-store` | `POST /random` | `/admin/moyu/bug-store`：`POST /list`、`GET /{id}`、`POST /`、`PUT /{id}`、`DELETE /{id}`、`POST /batch-import` | `bug_item`、`user_bug_history` |
| 热榜 | `/moyu/hot-topic` | `GET /categories`、`/data/{platform}`、`/data/all`、`POST /refresh` | 无 | 无（Redis 缓存） |
| 时薪计算器 | `/moyu/salary-calculator` | `GET /data`、`GET|POST|DELETE /config`、`POST /work-time` | 无 | `user_salary_config`、`work_record` |

**各子功能要点**
- 日历：事件日期以 `MM-dd` 存储（`eventDate.substring(3)` 取日），按年重复出现；`getTodayEvents` 命中 `isMajor=1` 时用 `blessingText` 作特殊问候；事件类型 1 程序员节日/2 技术纪念日/3 开源节日；用户偏好与方法内 `JsonUtils.listToJson` 序列化；所有用户端方法先 `StpUserUtil.checkLogin()`。
- 每日内容：类型 1 编程格言/2 技术小贴士/3 代码片段/4 历史上的今天；`getTodayContent` 按用户偏好类型逐类取随机内容；`getContentByType` 用 `selectRandomByContentType(contentType,1,limit)`；详情要求 `status==1`。
- Bug 商店：`POST /random` 随机返回一条 Bug，通过 `user_bug_history` 排除**最近 2 小时**已看过的最多 **50** 条（`DUPLICATE_WINDOW_HOURS=2`、`MAX_EXCLUDE_COUNT=50`），全被排除后清空排除列表重取；随后异步记录浏览历史；无 Bug 时返回 `Result.error(DATA_NOT_EXIST)`。
- 热榜：数据来自外部第三方聚合 API（`hot-topic.api.base-url` 代码默认 `http://113.44.190.45:9996/api`；`xiaou-bootstrap/application.yml` 未覆盖，实配以代码默认值为准）。缓存用 `xiaou-common-cache` 的 `CacheStore` 双层：`hot_topics:data:{platform}`（默认 15 分钟）+ `hot_topics:stale:data:{platform}`（默认 1440 分钟）作失败兜底；拉取用 `CompletableFuture.supplyAsync(..., hotTopicExecutor).orTimeout(默认 4s)` 并行，`/refresh` 用 30s。平台由 `enums.HotTopicEnum` 定义，共 26 个（抖音/快手/微博/虎扑/Linux.do/水木/贴吧/知乎/知乎日报/爱范儿/网易新闻/今日头条/CSDN/数字尾巴/极客公园/果壳/HelloGitHub/IT之家/掘金/豆瓣电影/简书/微信读书/地震速报/历史上的今天/气象预警…），分 6 类。`task.HotTopicTask`：`@Scheduled(fixedRate=15min)` 定时刷新 + 启动后 60s 执行一次串行初始化（每个平台间隔 100ms），`AtomicBoolean refreshing` 防重入；`config.MoyuConfig` 提供 `hotTopicExecutor`（核心 3 / 最大 8 / 队列 50 / `CallerRunsPolicy`）与热榜专用 `RestTemplate`（connect 2s、read 5s）。
- 时薪计算器：先配置月薪/月工作日/日工时（`user_salary_config`），`POST /work-time` 用动作状态机 `START→(PAUSE↔RESUME)→END`（`workStatus` 0 未开始/1 进行中/2 暂停中/3 已完成）；`END` 时 `工作时长=(end-start)-累计暂停分钟`（负数归 0），`当日收入=小时×时薪`（`hourlyRate` 由配置推导，`BigDecimal` 2 位）；`/data` 汇总今日/本周（周一至周日，`WeekFields.of(Locale.CHINA)`）/本月数据；未配置薪资时 `handleWorkTimeAction` 抛 `BusinessException("请先配置薪资信息")`。

**依赖关系**：`xiaou-common-core/web/security/persistence/cache`（是这 5 个模块中**唯一**依赖 `xiaou-common-cache` 的模块）；不依赖 `xiaou-ai`、`xiaou-points`。

**约束与注意事项**
- 热榜是唯一有缓存与定时任务的子功能；其余 4 个子功能全部直连 MySQL，无缓存。
- `HotTopicServiceImpl.refreshHotTopicData` 标注 `@Async("hotTopicExecutor")`，内部 `parallelMapWithTimeout` 又把 26 个子任务提交到**同一个** `hotTopicExecutor`——同池嵌套提交，池满时依赖 `CallerRunsPolicy` 退化，存在潜在饥饿/放大风险。
- 热榜外部依赖为硬编码公网 IP 的第三方服务，失败时依赖 1440 分钟 stale 缓存兜底；缓存与映射全为 `Map<String, String>` JSON 字符串。
- Bug 商店 `/random` 是 POST 语义的读操作；`getRandomBug` 内部 catch 全部异常并返回 null，Controller 再转成业务错误，真实故障会被吞成"暂无可用的Bug内容"。
- 管理端 3 个 Controller 全部 `@RequireAdmin`；用户端全部 `StpUserUtil.checkLogin()`（热榜除外——热榜接口未做登录校验）。
- `DailyContentServiceImpl.getRecommendedContent` 的偏好匹配细节、`DeveloperCalendarServiceImpl` 内部实现本次未读（未确认）。

## 跨模块观察

**共性模式**
- 统一返回 `Result<T>` + `PageResult<T>`；`admin`/`pub` 包分流 + `@RequireAdmin` 标注管理接口；`controller → service(interface) → service.impl → mapper(XML/注解) → domain + dto` 分层在所有 5 个模块一致。
- 会话读取统一用 `StpUserUtil`/`StpAdminUtil`；业务异常统一 `BusinessException`；分页统一 `PageHelper` 或 `PageHelper.doPage`/`PageResult.of`。
- 状态机普遍用「Integer 状态 + 业务枚举类（含 `of()` 静态方法）」表达（`KnowledgeMap.Status`、`KnowledgeNode.NodeType`、版本 0/1/2、内容 status、workStatus）。

**重复代码**
- `xiaou-oj` 内 `JudgeService.judge` 与 `CodeRunnerService.selfTest` 高度重复：编译→`copyOutCached`→逐用例运行→`finally` 删缓存文件几乎逐行雷同；且两者的输出比对口径不一致（`stripTrailing()` vs `strip()`）。
- `DailyContentController`/`AdminDailyContentController`/`DeveloperCalendarController`/`AdminDeveloperCalendarController` 各自私有 `convertToEntity`/`convertToDto`/`getCurrentUserId`，四个文件重复同类映射代码。
- 浏览量自增 `incrementViewCount` 在 oj（题目/节点）、knowledge（图谱/节点）、version、moyu（内容）中各自实现。
- Mapper XML 位置不统一：`xiaou-oj` 在 `src/main/java/com/xiaou/oj/mapper/*.xml`，`xiaou-knowledge`/`xiaou-moyu`/`xiaou-version` 在 `src/main/resources/mapper/`，`xiaou-sql-optimizer` 干脆全注解 SQL。

**潜在风险**
1. **判题是单点外部依赖且无队列**：无 MQ/Redis 队列、无并发上限、无提交限流，`@Async` 线程池 + go-judge `parallelism=4` 直接对接；沙箱异常被降级为 `System Error`，运维可观测性依赖日志。
2. **沙箱资源边界偏紧**：容器 1G 内存/2CPU、并发 4，而单题默认内存上限 256MB 并会在编译阶段复用；JVM/Go 类语言编译+运行叠加时余量小。
3. **知识图谱无缓存且构树在内存**：大图谱每次请求全量取节点并在 JVM 递归装配；无图数据库/全文索引。
4. **SQL 工作台案例列表内存分页**：`selectAllByUserId` 全量拉取后过滤排序，记录多时接口退化。
5. **热榜同池嵌套异步 + 硬编码第三方地址**，且 `hot-topic` 用户端接口无登录校验。
6. **工具类模块普遍缺少防刷**：`/oj/run`、`/oj/test`、`/moyu/*/random` 等无频率限制。

**与 `.agent/rules/always.md` 的一致性**
- 一致：接口统一 `Result<T>`；业务逻辑均落在各自业务模块，未入侵 `xiaou-bootstrap`/`xiaou-application`；未触碰 `pom-xml-flattened`、`target/`、`node_modules/`。
- 无明显违规项：本批模块未出现 701/702/703/704 业务码的自定义改写（`xiaou-oj` 仅一处 `Result.error("题目不存在或未公开")`，`xiaou-knowledge` 用 `ResultCode.DATA_NOT_EXIST`）。
- 需留意：`always.md` 要求"新增业务模块必须同步根 `pom.xml` 的 `<modules>` 与 `xiaou-application/pom.xml` 依赖"——本次未核对这 5 个模块在 `xiaou-application/pom.xml` 中的登记情况（未确认）。

**未确认清单**
- `AiSqlOptimizeService`（`xiaou-ai`）的实际模型调用方式、超时与重试策略。
- `xiaou-moyu` 的 `DailyContentServiceImpl` 推荐偏好匹配细节、`DeveloperCalendarServiceImpl` 内部实现、`FlexibleDateDeserializer` 用途。
- `xiaou-oj` 的 `OjProblemServiceImpl`（每日一题选取算法）、`OjContestServiceImpl`（赛事状态流转）、`OjProblemCommentServiceImpl` 细节。
- `/oj/**` 与 `/pub/knowledge/**` 的登录校验是否由全局 Sa-Token 拦截器兜底。
- `hot-topic.api.base-url` 是否在私密配置 `application-sec.yml` 中被覆盖（该文件按规定未读取）。
- 这 5 个模块在 `xiaou-application/pom.xml` 中的依赖登记情况。
