---
artifact: spec
status: active
scope: 激励、协作、文件存储与内容安全
---

# 激励、协作、文件存储与内容安全

本规格覆盖 Code-Nest 后端 5 个模块：`xiaou-points`（积分与抽奖）、`xiaou-team`（学习小组协作）、`xiaou-filestorage`（文件存储）、`xiaou-sensitive`（敏感词内容安全）、`xiaou-sensitive-api`（内容安全跨模块契约），共约 281 个 Java 文件。所有事实来自实际读取的 pom、Controller、关键 Service/Domain 与 Mapper XML 表名；未读取的部分显式标注"未确认"。

## 模块总览

| 模块 | 职责一句话 | 关键入口（包/类/文件） |
| --- | --- | --- |
| xiaou-points（101 文件） | 用户积分账户与流水、每日打卡、积分抽奖（概率/库存/风控/熔断/调参） | `com.xiaou.points.controller.user.UserPointsController`、`UserLotteryController`、`controller.admin.AdminPointsController`、`AdminLotteryController`；`service.impl.PointsServiceImpl`、`LotteryServiceImpl`；`strategy.impl.*Strategy`；`chain.RiskCheckChainBuilder` |
| xiaou-team（61 文件） | 学习小组成员/申请/任务/打卡/讨论/排行/统计 | `controller.user.UserTeamController`（唯一 Controller）；`service.impl.StudyTeamServiceImpl` 等 7 实现 |
| xiaou-filestorage（47 文件） | 文件上传下载、多存储后端策略、存储配置与迁移 | `controller.pub.FileController`；`controller.admin.Admin{File,Storage,Migration,System}Controller`；`factory.StorageStrategyFactory`；`strategy.impl.{Local,Oss,Cos,Kodo,Obs}StorageStrategy` |
| xiaou-sensitive（69 文件） | 敏感词库/策略/白名单/词源/统计/版本管理与检测引擎 | `controller.api.SensitiveWordController`、`controller.admin.SensitiveWordAdminController`、`controller.Sensitive*Controller`；`service.impl.SensitiveCheckServiceImpl`；`engine.AhoCorasickEngine`、`engine.TextPreprocessor` |
| xiaou-sensitive-api（3 文件） | 敏感词检测跨模块契约（接口 + 出入参 DTO） | `com.xiaou.sensitive.api.SensitiveCheckService`、`api.dto.SensitiveCheckRequest`、`SensitiveCheckResponse` |

## xiaou-points（xiaou-points）

**职责与边界**：积分账户/流水、打卡（位图）、抽奖全链路。依赖 `xiaou-user-api`、`xiaou-common-cache`（Redisson）、Caffeine；不依赖 team/filestorage/sensitive。

**核心领域对象**：`UserPointsBalance`、`UserPointsDetail`、`UserCheckinBitmap`、`LotteryPrizeConfig`、`LotteryDrawRecord`、`UserLotteryLimit`、`LotteryStatisticsDaily`、`LotteryAdjustHistory`。积分类型枚举 `PointsType`：1 后台发放、2 打卡积分、3 抽奖消耗、4 抽奖奖励、5 OJ 通过。

**对外接口**（`Result<T>` 统一返回；管理端均 `@RequireAdmin`）：

| 分区 | 路径 |
| --- | --- |
| 用户端 `/user/points` | GET `/balance`、POST `/checkin`、POST `/detail`、POST `/checkin-calendar`、POST `/checkin-statistics`（均 `StpUserUtil.checkLogin()`） |
| 用户端 `/user/lottery` | POST `/draw`、GET `/prizes`、POST `/records`、GET `/statistics`、GET `/rules`、GET `/remaining-count` |
| 管理端 `/admin/points` | POST `/grant`、`/batch-grant`、`/detail-list`、`/user-list`；GET `/statistics`、`/user-info/{userId}` |
| 管理端 `/admin/lottery` | 奖品：`prize/save`、`prize/list`、`prize/toggle-status`、`prize/suspend`、`prize/adjust-probability`、`prize/batch-adjust`、`prize/batch-toggle`、`prize/normalize`、`prize/validate-probability`；监控：`monitor/realtime`、`monitor/prize/{id}`、`monitor/alerts`、`records`、`statistics/history`、`adjust-history`、`analysis/comprehensive`、`cache/refresh`；用户：`user/reset-limit`、`user/blacklist`、`user/risk-list`、`user/evaluate-risk`、`user/detect-abnormal`；应急：`emergency/circuit-break`、`emergency/resume`、`emergency/degradation/enable`、`emergency/degradation/disable` |

**数据表**：`user_points_balance`、`user_points_detail`、`user_checkin_bitmap`、`lottery_prize_config`、`lottery_draw_record`、`user_lottery_limit`、`lottery_statistics_daily`、`lottery_adjust_history`。

**关键流程**

- 积分获取来源：① 打卡 `PointsServiceImpl.checkin` → `CheckinPointsCalculator`（基础 50 + 每周期递增 10，第 7 天额外周奖励 50，7 天一循环）；② 管理员发放 `/admin/points/grant`、`/batch-grant`；③ 抽奖中奖 `LotteryServiceImpl.issueReward`；④ OJ 首次通过：`xiaou-oj/.../judge/JudgeService.java:208` 调 `PointsService.grantSystemPoints(...)`（类型 `PointsType.OJ_AC`=5）；⑤ 新用户建号：`xiaou-user/UserInfoServiceImpl.java:96,540` 调 `createPointsAccountForNewUser`。
- 积分消耗来源：仅抽奖。`LotteryConstants.DRAW_COST_POINTS`=100；`deductPoints` 走 `UserPointsBalanceMapper.deductPoints`（SQL 带 `AND total_points >= #{points}`），`rows == 0` 抛"积分不足"。
- 并发与幂等：抽奖全程持 Redisson 用户级锁 `lottery:lock:user:{userId}`（`tryLock(3s)`），方法 `@Transactional(rollbackFor = Exception.class)`；库存由 `LotteryStockService.deductStock/rollbackStock` 补偿；余额增减用 `addPoints`/`deductPoints` 原子 UPDATE。打卡靠 `user_checkin_bitmap` 的 `userId + yearMonth` 位图 + `CheckinBitmapUtil.isCheckedIn` 判重后抛异常；未见唯一键或分布式锁（表约束未确认）。
- 抽奖概率：`LotteryStrategyFactory` 选策略，实现有 `AliasMethodStrategy`、`DynamicWeightStrategy`、`GuaranteeStrategy`（默认 `ALIAS_METHOD`）。`GuaranteeStrategy`：连续未中奖 ≥ `GUARANTEE_COUNT`(20) 触发保底，在 `prizeLevel <= 4` 中随机；否则按 `currentProbability` 累计轮盘。调参常量集中在 `LotteryConstants`：目标回报率 0.75、目标利润率 0.25、回报率熔断阈值 0.90、每日上限 10 次、冷却 10 秒、限流 10/分·100/时·500/天、单次调幅 0.2、归一化容差 0.0001、连续高价值 10 次触发熔断、每 100 次抽奖触发调整检查。
- 风控链：`chain/RiskCheckChainBuilder` + `BlacklistCheckHandler`、`CooldownCheckHandler`、`PointsCheckHandler`、`RateLimitCheckHandler`；通过 `LotteryScheduler` 定时任务、`LotteryCacheWarmer` 预热、`LotteryEventPublisher` 事件（`StatisticsListener`、`ReturnRateMonitorListener`）解耦统计与回报率监控。
- 兑换比例：`POINTS_TO_YUAN_RATE` = 1000（1000 积分 = 1 元）。

**依赖关系**：被 `xiaou-blog`、`xiaou-codepen`（引用 `PointsType`）、`xiaou-oj`（`grantSystemPoints`）、`xiaou-user`（建号）、`xiaou-application`（`web.home.UserHomeOverviewService`、`web.learning.LearningCockpitService`）、`xiaou-system`（`SysDashboardServiceImpl`、`system.agent.tools.LotteryRealtimeMonitorAgentTool` → `LotteryAdminService`）引用。无模块实现 points 的 API 契约（points 未拆 api 模块）。

**约束与注意事项**：`getUserPointsList` 先 `PageHelper.doPage` 再在内存 filter/sort（`UserPointsBalanceMapper.selectBalanceList` 无 where 条件），筛选条件不参与分页；`getAdminStatistics` 的 `dailyTrends` 恒为空、用户名用 `"用户" + userId`、打卡日历每日积分硬编码 50（源码注释标注"简化处理"）；`calculateTotalCheckinPoints` 以 `pageSize=10000` 查明细求和。

## xiaou-team（xiaou-team）

**职责与边界**：学习小组的完整协作域（建组、招募、任务、打卡、讨论、排行、统计）。**仅 1 个 Controller 的原因**：`UserTeamController` 注入 7 个服务接口（`StudyTeamService`、`TeamMemberService`、`TeamTaskService`、`TeamCheckinService`、`TeamRankService`、`TeamDiscussionService`、`TeamStatsService`），每个接口对应 1 个 `*ServiceImpl` = 7 + 7 = 14 个 Service 类；即"按业务子域拆服务、按用户端聚合到一个 Controller"。模块无管理端 Controller（未确认是否存在后台入口）。

**核心领域对象**：`StudyTeam`、`StudyTeamMember`、`StudyTeamApplication`、`StudyTeamTask`、`StudyTeamCheckin`、`StudyTeamCheckinLike`、`StudyTeamCheckinComment`、`StudyTeamDiscussion`、`StudyTeamDiscussionLike`、`StudyTeamDailyStats`；枚举 `TeamStatus`、`TeamType`、`JoinType`、`MemberRole`、`MemberStatus`、`ApplicationStatus`、`TaskType`、`RepeatType`、`DiscussionCategory`。

**对外接口**：全部挂在 `/user/team`（同一 Controller）。写操作统一 `StpUserUtil.checkLogin()`；读操作常以 `StpUserUtil.isLogin()` 判断后允许游客（`userId=null`）。

| 子域 | 路径（`/user/team` 前缀） |
| --- | --- |
| 小组 | POST `/create`、PUT `/{teamId}`、DELETE `/{teamId}`、GET `/{teamId}`、POST `/list`、GET `/my`、`/created`、`/recommend`、`/{teamId}/invite-code`、POST `/{teamId}/invite-code/refresh`、GET `/by-code/{inviteCode}` |
| 成员 | POST `/{teamId}/join`、`/join-by-code`、`/{teamId}/quit`、`/{teamId}/application/{applicationId}/approve`、`/reject`、`/application/{applicationId}/cancel`；GET `/{teamId}/members`、`/{teamId}/applications`、`/applications/my`；DELETE `/{teamId}/member/{targetUserId}`；PUT `/{teamId}/member/{targetUserId}/role`、`/{teamId}/transfer`；POST/DELETE `/{teamId}/member/{targetUserId}/mute` |
| 任务 | POST `/{teamId}/task`、PUT `/task/{taskId}`、`/task/{taskId}/status`、DELETE `/task/{taskId}`、GET `/task/{taskId}`、`/{teamId}/tasks`、`/{teamId}/tasks/today` |
| 打卡 | POST `/{teamId}/checkin`、`/{teamId}/checkin/supplement`、DELETE `/checkin/{checkinId}`、GET `/checkin/{checkinId}`、`/{teamId}/checkins`、`/{teamId}/checkins/my`、`/{teamId}/checkin/calendar`、`/{teamId}/checkin/streak`、`/{teamId}/checkin/total`；POST/DELETE `/checkin/{checkinId}/like` |
| 排行 | GET `/{teamId}/rank/checkin`、`/rank/streak`、`/rank/duration`、`/rank/contribution`、`/rank/my` |
| 讨论 | POST `/{teamId}/discussion`、PUT `/discussion/{discussionId}`、DELETE `/discussion/{discussionId}`、GET `/discussion/{discussionId}`、`/{teamId}/discussions`、PUT `/discussion/{discussionId}/top`、`/essence`、POST/DELETE `/discussion/{discussionId}/like` |
| 统计 | GET `/{teamId}/stats`、`/stats/weekly`、`/stats/monthly`、`/stats/my` |

**数据表**：`study_team`、`study_team_member`、`study_team_application`、`study_team_task`、`study_team_checkin`、`study_team_checkin_like`、`study_team_discussion`、`study_team_discussion_like`、`study_team_daily_stats`。`StudyTeamCheckinComment` 领域类存在但无对应 Mapper/XML（未确认该表是否已落地）。

**关键流程**

- 建组：校验名称 2–50 字，`countByCreatorId >= 3` 拒绝（`MAX_CREATE_TEAMS`=3），创建者自动写入 `study_team_member`（`MemberRole.LEADER`），默认 `maxMembers`=20、`joinType`=申请制、邀请码 `RandomUtil.randomString(8).toUpperCase()`。
- 权限：改组/刷新邀请码/审批/移除/转让均以 `memberMapper.selectRole(teamId, userId)` 判定；解散额外要求 `creatorId` 匹配且 `currentMembers <= 10`。
- 打卡率：7 日窗口（`today-6 ~ today`）用 `countRecentCheckinUsersByTeamIds` 统计活跃人数，`activeUsers * 100 / (memberCount * 7)`，上限 100；列表接口用 `convertToTeamResponses` 批量聚合（批量取用户信息、角色、打卡率、待审申请数）避免 N+1。
- 统计落库：`study_team` 上有 `total_checkins`、`total_discussions`、`active_days`、`invite_code` 的原子自增/更新语句，与 `study_team_daily_stats` 配合。

**依赖关系**：仅依赖 `xiaou-user-api`（`UserInfoApiService.getSimpleUserInfo/simpleUserInfoBatch`）与公共模块。**不依赖 xiaou-points，也不依赖 xiaou-sensitive**：小组打卡与积分模块的每日打卡是两套独立体系，团队讨论/打卡内容不做敏感词检测。

**约束与注意事项**：模块无管理端接口与后台治理能力（封禁、违规小组处理未见入口）；写操作靠 Service 内手工 `BusinessException`，Controller 层多用 `success ? Result.success(...) : Result.error(...)` 返回。

## xiaou-filestorage（xiaou-filestorage）

**职责与边界**：文件元数据 + 多后端存储 + 存储配置/健康/迁移/备份/统计/系统设置。SDK 依赖：`aliyun-sdk-oss` 3.17.4、`cos_api` 5.6.155、`qiniu-java-sdk` 7.13.1、`esdk-obs-java` 3.23.5、`tika-core` 2.9.1（文件类型检测）。`FileStorageConfig` 提供 `@EnableAsync` 与 `fileBackupExecutor`（2/4 线程，队列 100，CallerRunsPolicy）。

**核心领域对象**：`FileInfo`、`FileStorage`、`FileAccess`、`FileMigration`、`StorageConfig`、`FileSystemSetting`、`FileUploadResult`（DTO）。

**存储后端与切换方式**

| 后端 | `getStorageType()` | 来源文件 |
| --- | --- | --- |
| 本地磁盘 | `LOCAL` | `strategy/impl/LocalStorageStrategy.java` |
| 阿里云 OSS | `OSS` | `strategy/impl/OssStorageStrategy.java` |
| 腾讯云 COS | `COS` | `strategy/impl/CosStorageStrategy.java` |
| 七牛云 Kodo | `KODO` | `strategy/impl/KodoStorageStrategy.java` |
| 华为云 OBS | `OBS` | `strategy/impl/ObsStorageStrategy.java` |

- 注册：`StorageStrategyFactory` 构造注入 `List<FileStorageStrategy>`，按 `getStorageType()` 存入 `strategyMap`；`getSupportedStorageTypes()` 即返回该 keySet（`GET /admin/storage/types`）。
- 实例化：`createAndInitialize(configId, storageType, configParams)` 用无参构造反射 `newInstance()` + `initialize(configParams)`，并按 `configId` 缓存于 `initializedStrategies`（配置改动后需 `removeInitializedStrategy/clearAllInitializedStrategies`，未见自动失效逻辑）。
- 切换：`storage_config` 表存 `storageType`/`configParams`(JSON)/`isEnabled`/isDefault；上传、下载、取 URL 都调 `storageConfigMapper.selectDefault()` 取默认配置（`FileStorageServiceImpl.java:110,211,262`）；`PUT /admin/storage/config/{id}/default` 设默认、`/enable` 启停、`POST /config/{id}/test` 测通、`DELETE /config/{id}` 删除（校验 `strategyFactory.isSupported`）。
- 兜底：默认配置不可用时 `tryUploadWithStorage` / `tryLocalStorage` 回落本地，参数硬编码 `basePath = user.dir + "/uploads"`、`urlPrefix = http://localhost:9999/files`；`resolveLocalStorageConfig()` 先查 `selectByCondition("LOCAL", 1)`。

**对外接口**

| 分区 | 路径 |
| --- | --- |
| 公共 `/file` | POST `/upload/single`、`/upload/batch`（需登录）、GET `/download/{id}`、`/info/{id}`、`/url/{id}`、`/list`、POST `/urls`、`/exists`、DELETE `/{id}` |
| 管理端 `/admin/file`（`@RequireAdmin`） | GET `/list`、`/statistics`、`/storage-usage`；DELETE `/{id}/force`；PUT `/{id}/move`；POST `/migrate`；GET `/migration/{id}`、`/migrations`、`/migration/{id}/progress`；POST `/migration/{id}/execute`；PUT `/migration/{id}/stop`；DELETE `/migration/{id}` |
| 管理端 `/admin/storage`（`@RequireAdmin`） | GET `/configs`、`/config/{id}`、`/types`；POST `/config`、`/config/{id}/test`；PUT `/config/{id}`、`/config/{id}/enable`、`/config/{id}/default`；DELETE `/config/{id}` |
| 管理端 `/admin/system`（`@RequireAdmin`） | GET/PUT `/settings`、GET/PUT `/file-types`、GET `/summary` |

**数据表**：`file_info`、`file_storage`、`file_access`、`file_migration`、`storage_config`、`file_system_setting`。

**关键流程**

- 上传：`FileController` 先 `isAuthenticated()`（`StpUserUtil.isLogin() || StpAdminUtil.isLogin()`，双 try/catch）→ `FileStorageServiceImpl.uploadSingle(uploadBatch)` → 取默认 `StorageConfig` → `storageStrategyFactory.createAndInitialize(...).uploadFile(file, null)` → 写 `file_info` + `file_storage` 主记录（`isPrimary=1, syncStatus=1`）；失败调用 `deleteStoredObject` 清理远端对象。
- 读取与权限：`getAvailableFile` 要求 `status == 1`；`canReadFile` = `isPublic == 1` 或已登录。下载直接回 `InputStreamResource`，未走 `Result<T>`。
- 存储迁移：两条路径——单文件 `AdminFileController.moveFile`（源 `selectDefault()`，目标 `selectById` + `isEnabled == 1`，下载→上传→更新 `file_info`→删源；DB 更新失败则删目标对象）；批量 `FileMigrationService` + `file_migration` 表（创建/执行/停止/进度/删除）。
- 其它：`FileAccess` 记录访问（`FileOperationEventListener` 消费 `FileOperationEvent`，含按时间清理）；`StorageHealthService` 做健康检查（内部对 `"LOCAL"` 配置有特判）；`FileBackupService` 用 `fileBackupExecutor` 异步备份。

**依赖关系**：被 `xiaou-user`（`UserController` 注入 `FileStorageService`）与 `xiaou-resume`（`ResumeServiceImpl`）引用。模块不依赖 user-api/points/sensitive。

**约束与注意事项**：`/file/**` 不在 `.agent/rules/always.md` 声明的公共路由分区（`/oj`、`/community`、`/version`）内，也未列入免登录清单；`FileController` 手动鉴权而非 `@RequireAdmin`；私有文件仅校验"已登录"，未校验上传者/归属；5 个存储策略的 `initialize` 参数形态完全依赖 `config_params` JSON 键名（键名清单未逐一确认）。

## xiaou-sensitive（xiaou-sensitive）

**职责与边界**：敏感词库与检测引擎、处理策略、白名单、同音/形似字、词库来源同步、命中统计与用户违规、版本与回滚。依赖 `xiaou-sensitive-api`、Caffeine。模块内 Service 共 17 个 = 8 个接口（`SensitiveWord/Homophone/SimilarChar/Source/Statistics/Strategy/Version/Whitelist`）+ 9 个实现（含实现 api 契约的 `SensitiveCheckServiceImpl`）。

**核心领域对象**：`SensitiveWord`、`SensitiveCategory`、`SensitiveStrategy`、`SensitiveWhitelist`、`SensitiveLog`、`SensitiveHitStatistics`、`SensitiveUserViolation`、`SensitiveSource`、`SensitiveVersion`、`SensitiveHomophone`、`SensitiveSimilarChar`。

**对外接口**

| 分区 | 路径 | 鉴权 |
| --- | --- | --- |
| API | POST `/sensitive/check`、`/sensitive/check/batch` | 无鉴权注解（公开） |
| 管理 | `/admin/sensitive`：POST `/words/list`、`/words/{id}`、`/words`、`/words/update`、`/words/delete/{id}`、`/words/delete/batch`、`/words/import`、`/words/preview-import`、`/words/confirm-import`、`/words/export`、`/refresh`、`/categories` | `@RequireAdmin` |
| 管理 | `/sensitive/strategy`（list/getById/get(未标注)/update/reset/{id}/refresh）、`/sensitive/whitelist`、`/sensitive/source`(含 `test-connection/{id}`、`sync/{id}`)、`/sensitive/statistics`(overview/trend/hot-words/category-distribution/module-distribution/export)、`/sensitive/version`(list/{id}/rollback/{id}/latest(未标注))、`/sensitive/homophone`、`/sensitive/similar-char` | 除标注外均 `@RequireAdmin` |

**数据表**：`sensitive_word`、`sensitive_category`、`sensitive_strategy`、`sensitive_whitelist`、`sensitive_log`、`sensitive_hit_statistics`、`sensitive_user_violation`、`sensitive_source`、`sensitive_version`、`sensitive_homophone`、`sensitive_similar_char`。

**关键流程：同步检测 + 异步留痕**

- 主链路**同步**：`SensitiveCheckServiceImpl.checkText` 依次执行 —— ① 长度保护（`MAX_TEXT_LENGTH`=10000 截断）；② `TextPreprocessor.preprocess(text, true, true, true, true)` 生成变形文本；③ `AhoCorasickEngine.findSensitiveWords` 对原文与变形文本各检一次并合并；④ `SensitiveWhitelistService.isInWhitelist(word, module)` 过滤；⑤ `resolveHitPolicy` 取命中词的 `max(level)` / `max(action)`（`enabledWordMeta` 为 `volatile Map` 快照），缺失时按命中数回退 `calculateRiskLevel`（1 词=1，≤3=2，>3=3）；⑥ `SensitiveStrategyService.getStrategy(module, riskLevel)` 与词级 action 合成最终动作（`wordAction` 1→replace、2→reject、3→warn；`chooseAction` 中 reject 优先、其次 warn、否则 replace）；⑦ `processText` 按动作替换为 `"***"`、拒绝时返回空串；⑧ `allowed = !"reject".equals(action)`。
- **异步留痕**：命中后 `logSensitiveDetectionAsync`（写 `sensitive_log`）与 `recordStatisticsAsync`（`statisticsService.recordHit` 写 `sensitive_hit_statistics`、`recordUserViolation` 写用户违规）都提交到 `@PostConstruct` 创建的 `batchExecutor`（核心 2/最大 8，队列 200，守护线程 `SensitiveCheck-{n}`，`CallerRunsPolicy`，`@PreDestroy` 优雅关闭）。统计/日志失败仅告警，不影响检测结果。
- 批量：`checkTextBatch` ≤10 条串行，>10 条并行 `Future.get(10s)`，超时/异常返回默认响应并回退串行；`MAX_BATCH_SIZE`=100。
- 引擎：`AhoCorasickEngine` = Trie + BFS failure 指针 + `ReentrantReadWriteLock`（刷新持写锁、匹配持读锁），匹配前统一 `toLowerCase()`；failure 链遍历限制 `depth < 10` 防退化；`MAX_PATTERN_COUNT`=50000、`MAX_PATTERN_LENGTH`=100。
- 词库刷新：`refreshWordLibrary` 读 `sensitive_word`（`selectEnabledWordDetails`，status=1）→ 过滤空串与超 100 字 → `sensitiveEngine.refresh(wordSet)` 并重建 `enabledWordMeta`；失败时以空词库启动。触发点：启动 `@PostConstruct`、`SensitiveWordServiceImpl` 增删改/导入后（97/122/148/179/280 行）、`SensitiveSourceServiceImpl` 同步后（367 行）、`POST /admin/sensitive/refresh`。

**内容安全生效入口**（grep 全仓调用点）

| 入口 | 方式 | 位置 |
| --- | --- | --- |
| 社区发帖/评论 | `SensitiveWordUtils.checkText(content, "community", postId, userId)` | `xiaou-community/.../CommunityCommentServiceImpl.java:126-127,327-328` |
| 动态发布/编辑 | `SensitiveWordUtils.checkText(...)` | `xiaou-moment/.../MomentServiceImpl.java:64,202` |
| 博客文章 | 直接注入契约 `containsSensitiveWords(title/content, "blog")` | `xiaou-blog/.../BlogArticleServiceImpl.java:93,96,209,212` |
| 通用静态门面 | `com.xiaou.common.utils.SensitiveWordUtils`（`@Component` + `@Autowired` 注入静态字段），自带 5 分钟 / 最多 500 条本地缓存，超长文本截断后拼接回转 | `xiaou-common/.../utils/SensitiveWordUtils.java` |
| HTTP | `POST /sensitive/check` | `controller/api/SensitiveWordController.java` |

**未生效入口（重要盲区）**：`xiaou-team`（小组讨论、打卡内容）、`xiaou-filestorage`（上传文件内容）、`xiaou-oj`、`xiaou-resume` 等模块未发现任何敏感词调用点。

**依赖关系**：`xiaou-sensitive` 依赖 `xiaou-sensitive-api` 并实现其唯一接口；`xiaou-sensitive-api` 被 `xiaou-common`（`SensitiveWordUtils`）与 `xiaou-blog` 直接依赖。

**约束与注意事项**：7 个管理控制器挂在 `/sensitive/**` 而不是 `/admin/sensitive/**`（只有 `SensitiveWordAdminController` 用 `/admin/sensitive`），与"管理端 `/admin/**`"约定不一致；`/sensitive/strategy/get` 与 `/sensitive/version/latest` 未加 `@RequireAdmin`；`SensitiveCheckServiceImpl` 中残留未被调用的 `calculateRiskLevel(int)`/`determineAction(int)`/`normalize*` 兼容路径，`determineAction` 注释标注"已移除审核功能"；`SensitiveSourceSyncScheduler` 与 `RemoteUrlPolicy`（远程词库拉取与 SSRF 防护）行为未读取，未确认其调度周期与白名单策略。

## xiaou-sensitive-api（xiaou-sensitive-api）

**职责与边界**：只放契约与 DTO 的纯 jar 模块（`spring-context` + `lombok` + `jackson-annotations`，无持久化/Web 依赖），用于让非 sensitive 模块在不依赖实现与表结构的前提下做内容检测。

**核心契约**

| 成员 | 说明 |
| --- | --- |
| `SensitiveCheckResponse checkText(SensitiveCheckRequest)` | 主检测，返回 `hit`、`hitWords`、`processedText`、`riskLevel`、`action`、`allowed` |
| `List<SensitiveCheckResponse> checkTextBatch(List<SensitiveCheckRequest>)` | 批量检测 |
| `boolean containsSensitiveWords(String text, String module)` | 简化判定 |
| `String replaceSensitiveWords(String text, String module)` | 简化替换 |
| `boolean isAllowed(String text, String module, Long businessId, Long userId)` | 发布准入判定 |
| `void refreshWordLibrary()` | 刷新词库 |

`SensitiveCheckRequest`：`text`、`module`、`businessId`、`userId`。

**实现者**：全仓仅 `com.xiaou.sensitive.service.impl.SensitiveCheckServiceImpl`（`@Service`）。

**调用者**：`xiaou-sensitive` 内部（`SensitiveWordServiceImpl` 刷新词库、`SensitiveSourceServiceImpl` 同步后刷新、`SensitiveWordController`、`SensitiveWordAdminController`）；`xiaou-common` 的 `SensitiveWordUtils`（静态门面，供 community/moment 使用）；`xiaou-blog` 的 `BlogArticleServiceImpl`；以及 `SensitiveWordServiceImplTest` 的 mock。

**约束与注意事项**：契约方法均为同步阻塞签名，无法表达"异步审核"；`refreshWordLibrary()` 暴露给外部模块，语义上是全量重建引擎，调用方需自行控制频率；异常语义由实现方决定，契约未定义失败兜底（实现侧 `checkText` 异常时 `allowed=false`，而 `SensitiveWordUtils` 异常时默认"允许通过/不包含"，两者 fail-closed 与 fail-open 不一致）。

## 跨模块观察

**共性模式**

1. 统一 `Result<T>` + `@RequireAdmin` + `StpUserUtil/StpAdminUtil` 是管理端一致写法，`PageResult` + `PageHelper.doPage` 是分页一致写法（points/team/sensitive 均用）。
2. 策略/工厂模式被三处独立使用：points 抽奖策略（`LotteryStrategyFactory`）、filestorage 存储策略（`StorageStrategyFactory`）、sensitive 引擎（`SensitiveEngine` 接口 + `AhoCorasickEngine`）。filestorage 的工厂支持按配置反射实例化，points/filestorage 之外未见同类。
3. 事件解耦在 points（`LotteryEventPublisher` + 2 个 listener）与 filestorage（`FileOperationEventPublisher` + 文件访问记录 listener）都有落地。
4. 异步执行器各自新建：filestorage 用 Spring `ThreadPoolTaskExecutor("fileBackupExecutor")`，sensitive 用裸 `ThreadPoolExecutor`（`@PostConstruct`/`@PreDestroy` 手管），points 用 Redisson + 定时器，未统一。

**重复代码与不一致**

1. sensitive 下 6 个管理控制器（strategy/whitelist/source/homophone/similar-char/version）结构几乎完全同构：`list` + `getById` + `add` + `update` + `delete` + `refresh`，异常处理统一为 `try/catch → Result.error("...失败")`；filestorage 4 个 admin 控制器同样把异常吞成 `Result.error(e.getMessage())`，与 `xiaou-common-web` 的 `@RestControllerAdvice`（`GlobalExceptionHandler`、`ResultHttpStatusAdvice`）职责重叠，异常语义被改写为 HTTP 200。
2. 业务码超出约定：`.agent/rules/always.md` 只声明 200/701/702/703/704，但 sensitive 使用了 `ResultCode.DATA_NOT_EXIST`、`CONFLICT`、`BUSINESS_ERROR`、`PARAM_VALIDATE_ERROR`，filestorage 使用了 `UNAUTHORIZED`、`FORBIDDEN`、`FILE_NOT_EXIST`。
3. 路由分区不一致：sensitive 管理控制器用 `/sensitive/**`（非 `/admin/**`）；filestorage 公共控制器用 `/file/**`、sensitive 检测用 `/sensitive/check`，都不在 always.md 的公共分区清单内。
4. 分层方向可疑：`xiaou-common` 的 `SensitiveWordUtils` 依赖业务侧 `xiaou-sensitive-api`（是否已在 `xiaou-common/pom.xml` 声明该依赖**未确认**，本次未读该 pom），公共基础模块反向依赖业务契约会让契约模块成为全局编译前置。

**潜在风险**

1. **fail-open / fail-closed 不一致**：`SensitiveCheckServiceImpl.checkText` 异常时 `allowed=false`（拒绝发布），`SensitiveWordUtils` 异常时返回 `allowed=true`（放行）并在服务未初始化时"默认允许通过"。同一份内容经过不同入口，故障期行为相反。
2. **内容安全覆盖盲区**：team 讨论/打卡、文件上传内容、OJ 提交、简历等无检测；team 与 community 同为 UGC 但治理能力不对等。
3. **积分与团队打卡割裂**：`xiaou-points` 的 `PointsType` 只有 5 种来源，没有"团队打卡/讨论"类型；团队打卡不产生积分，也不触发敏感词检测。
4. **points 统计与列表实现取巧**：`getUserPointsList` 的筛选/排序在内存完成、`dailyTrends` 恒空、日历积分硬编码 50、`calculateTotalCheckinPoints` 以 10000 条分页拉全量，规模上升后统计口径与性能都存在偏差风险。
5. **filestorage 权限与配置**：私有文件只校验登录、不校验归属；本地存储兜底参数（`user.dir + "/uploads"`、`http://localhost:9999/files`）硬编码，与 `application.yml` 的 context-path `/api` 不匹配；策略实例按 `configId` 缓存但配置修改后无自动失效。
6. **团队模块治理缺口**：无管理端 Controller，无跨组封禁/违规处置入口；`StudyTeamCheckinComment` 领域类缺 Mapper。

**与 `.agent/rules/always.md` 不一致之处**：路由分区（`/file/**`、`/sensitive/**` 与 `/admin/**` 约定）；业务码集合（超出 200/701/702/703/704）；"业务逻辑落在对应业务模块"整体满足，但 sensitive 管理接口的路由归属与 filestorage 公共控制器的手工鉴权方式偏离"管理端优先 `@RequireAdmin`"的约定（`FileController` 未使用该注解）。

**本规格未确认清单**

1. `xiaou-common/pom.xml` 是否声明 `xiaou-sensitive-api` 依赖（影响分层结论）。
2. `SensitiveSourceSyncScheduler` 调度策略与 `RemoteUrlPolicy` 的远程拉取/SSRF 白名单实现。
3. `xiaou-team` 是否存在独立管理端模块或后台入口；`study_team_checkin_comment` 表是否存在。
4. 5 个存储策略 `initialize(configParams)` 接受的具体 JSON 键名与其校验行为。
5. `sql/` 基线与 `sql/v2.5.8/` 增量是否已包含本规格列出的全部表（未核对 SQL 文件）。
6. `xiaou-points` 打卡位图表 `user_checkin_bitmap` 的 `(user_id, year_month)` 唯一约束是否存在（并发打卡幂等的硬保证）。
7. sensitive `/sensitive/strategy/get` 与 `/sensitive/version/latest` 未加 `@RequireAdmin` 是否有意为之。
