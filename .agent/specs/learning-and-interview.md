---
artifact: spec
status: active
scope: 学习与面试相关模块
---

# 学习与面试

覆盖 6 个后端 Maven 模块：`xiaou-interview`、`xiaou-mock-interview`、`xiaou-flashcard`、`xiaou-learning-asset`、`xiaou-resume`、`xiaou-plan`。
它们构成 Code-Nest 的"学—练—测—复盘—投递"链路：`interview` 提供题库与掌握度，`mock-interview` 消费题库并调用 `xiaou-ai` 做模拟面试与求职作战，
`flashcard` 做 SM-2 间隔重复，`learning-asset` 把博客/社区/代码片段等学习行为产物转化为可发布资产，`resume` 做简历制作与导出，`plan` 做打卡计划与成长自动驾驶。
统一返回体 `com.xiaou.common.core.domain.Result<T>`（成功 `200`，`701/702/703/704` 见 `xiaou-common-core/.../ResultCode.java`）。

## 模块总览

| 模块 | 职责一句话 | 关键入口（包/类/文件） |
| --- | --- | --- |
| xiaou-interview | 面试题库（分类/题单/题目）+ 掌握度与复习调度 + 收藏 + 学习记录 | `com.xiaou.interview.controller.{admin,pub}`、`service.impl.InterviewMasteryServiceImpl`、`mapper.InterviewMasteryMapper.xml` |
| xiaou-mock-interview | AI 模拟面试会话、求职闭环中台、求职作战台（JD 解析/匹配/补短板计划） | `controller.{MockInterviewController,MockInterviewSessionController,CareerLoopController,JobBattleController}`、`service.CareerLoopStateMachine` |
| xiaou-flashcard | 闪卡卡组与 SM-2 间隔重复复习 | `algorithm.SM2Algorithm`、`service.impl.FlashcardStudyServiceImpl`、`controller.user.FlashcardStudyController` |
| xiaou-learning-asset | 把外部内容快照转化为 4 类学习资产的转化引擎 + 审核发布 | `service.LearningAssetTransformEngine`、`service.impl.LearningAssetPublishServiceImpl`、`enums.TargetAssetType` |
| xiaou-resume | 在线简历（模板/内容/版本/分享/统计）+ PDF/Word/HTML 导出 | `controller.{user,admin}`、`service.impl.ResumeServiceImpl`、`service.support.ResumeExportBuilder` |
| xiaou-plan | 个人计划打卡 + 成长闭环自动驾驶（周计划生成/重排） | `controller.user.{UserPlanController,UserGrowthAutopilotController}`、`growth.planner.GrowthPlanConstraintPlanner`、`scheduler.PlanRemindScheduler` |

## 面试题库（xiaou-interview）

**职责与边界**：题目、题单、分类的唯一权威来源（admin CRUD + Markdown 导入）；掌握度与复习调度；收藏；题单学习进度；学习热力图。不含出题 AI、不含会话过程，不依赖 `xiaou-ai`。

**核心领域对象**：`InterviewCategory`、`InterviewQuestionSet`、`InterviewQuestion`（含 `sortOrder`/浏览量/收藏数）、`InterviewMasteryRecord`（`masteryLevel 1-4`、`reviewCount`、`nextReviewTime`）、`InterviewLearnRecord`、`InterviewFavorite`（`targetType`+`targetId` 泛化收藏）、`InterviewDailyStats`。

**对外接口**

| 端 | 方法与路径 |
| --- | --- |
| 管理端 | `POST/PUT/DELETE /admin/interview/categories`；`/admin/interview/question-sets`（含 `POST /import` Markdown 导入）；`/admin/interview/questions`（含 `/search`、`/batch`、`/set/{id}/next|prev`） |
| 用户端（`/interview/**`） | `GET /interview/categories`；`GET /interview/question-sets`、`/{id}`、`/{id}/questions`、`/{setId}/questions/{questionId}`(+`/next`、`/prev`)、`POST /interview/question-sets/search`、`POST /interview/question-sets/questions/random`；`POST /interview/mastery/mark`、`GET /interview/mastery/{questionId}`、`/batch`、`/review/stats`、`/review/list`、`/heatmap`(+`/detail`)；`POST /interview/learn/record`、`GET /interview/learn/progress/{setId}`、`/questions/{setId}`、`/total`；`POST /interview/favorites/{add,remove,check,my,my/page,count}` |

**数据表**（`src/main/resources/mapper/*.xml`）：`interview_category`、`interview_question_set`、`interview_question`、`interview_mastery_record`、`interview_mastery_history`、`interview_learn_record`、`interview_favorite`、`interview_daily_stats`。

**关键流程（复习调度）**：`InterviewMasteryServiceImpl.markMastery` 首次写入按 `learnCount` 计，再次写入按 `reviewCount` 计并写 `interview_mastery_history`；
下次复习间隔 = `BASE_INTERVALS = {1,2,4,7}`（按 `masteryLevel` 索引）× `2^min(reviewCount,5)`，上限 `MAX_INTERVAL = 60` 天（`InterviewMasteryServiceImpl:252-259`）。
**这不是 SM-2**，而是"分级基础间隔 × 指数放大"的简化艾宾浩斯；热力图 streak 由 `interview_daily_stats.totalCount > 0` 逆序连算。

**依赖关系**：不依赖 `xiaou-ai`。依赖 `xiaou-notification`（`InterviewFavoriteServiceImpl:72` 调用 `NotificationPublisher.publish(NotificationCommand.interview(...))`）。

**约束与注意事项**：
- 用户端路由使用 `/interview/**`，**不落在 `SaTokenConfig` 的 `/user/**` 全局登录拦截内**，登录校验全靠控制器内 `StpUserUtil.getLoginIdAsLong()`。
- `InterviewQuestionSetPublicController:59` 的权限判断写作 `if (userId != null && !hasAccessPermission(...))`，**未登录时跳过题单权限校验**；`hasAccessPermission` 的实现未确认。

## AI 模拟面试（xiaou-mock-interview）

**职责与边界**：面试会话过程建模（出题→作答→AI 评价→追问→报告）、求职闭环进度、JD 解析/简历匹配/补短板计划。**题库与题目不重复建模**，直接复用 `xiaou-interview`。

**核心领域对象**：`MockInterviewDirection`（`directionCode` + 逗号分隔 `categoryIds` 关联 `interview_category`）、`MockInterviewSession`（`status 0进行中/1已完成/2已中断`、`questionMode 1本地题库/2AI出题`、四项分项得分）、`MockInterviewQA`（`QAStatusEnum` PENDING/ANSWERED/SKIPPED + `questionType` FOLLOW_UP + `parentId` 追问树）、`MockInterviewUserStats`、`CareerLoopSession/Snapshot/Action/StageLog`、`CareerApplicationRecord`、`JobBattlePlanRecord`、`JobBattleMatchRecord`。

**对外接口**

| 端 | 方法与路径 |
| --- | --- |
| 用户端 | `GET /user/mock-interview/directions|config|question-sets`、`POST /create`、`POST /history`、`GET /{id}/report`、`DELETE /{id}`、`GET /stats/overview`、`POST /{id}/summary`；`POST /user/mock-interview/session/{start,answer,skip,end,follow-up}`、`GET /next`、`GET /{id}/status` |
| 用户端 | `/user/career-loop/{start,current,timeline,actions,applications,applications/summary,profile,sync,event}`、`PUT|DELETE /applications/{id}`、`POST /actions/{id}/done` |
| 用户端 | `/user/job-battle/{jd/parse,resume/match,plan/generate,plan/history,plan/history/{id},match-engine/run,match-engine/history,match-engine/history/{id},match-engine/latest,interview/review}` |
| 管理端 | `GET/POST/PUT/DELETE /admin/mock-interview/directions`(+`/{id}/status`)、`POST /admin/mock-interview/sessions`、`GET /sessions/{id}`、`GET /stats/overview` |

**数据表**：`mock_interview_direction`、`mock_interview_session`、`mock_interview_qa`、`mock_interview_user_stats`、`career_loop_session`、`career_loop_snapshot`、`career_loop_action`、`career_loop_stage_log`、`career_application_record`、`job_battle_plan_record`、`job_battle_match_record`（11 个 mapper XML 一一对应）。

**关键流程**
- **出题**：`QuestionSelectorServiceImpl.selectQuestions` 按方向取 `categoryIds` → `InterviewQuestionMapper.selectByQuestionSetIds` → 过滤已答（`qaMapper.selectAnsweredQuestionIdsByUserId`）→ 不足则回退全量 → `Collections.shuffle` 随机截取。`generateQuestionsByAI` 调 `AiInterviewService.generateQuestions`，失败/为空降级到**硬编码本地题库**（java/frontend/python，`QuestionSelectorServiceImpl:199-231`）。
- **会话状态机**：`MockInterviewServiceImpl` 创建时 `SessionStatusEnum.ONGOING`、每题 `QAStatusEnum.PENDING`；`submitAnswer` 调 `aiInterviewerService.evaluateAnswer` 后置 `ANSWERED`；`skip` 置 `SKIPPED`；`getValidSession` 校验必须为 `ONGOING`（`MockInterviewServiceImpl:651`）；`endInterview` 置 `COMPLETED`（重复结束时直接返回）。
- **AI 调用与降级**：`AIInterviewerServiceImpl` 注入 `com.xiaou.ai.service.AiInterviewService`，`evaluateAnswer` / `generateSummary` / `generateFollowUpQuestion` 三处调用；AI 返回 `isFallback()` 或抛异常时走本地评估（按回答长度给分、按 `InterviewStyleEnum.getFollowUpRate()` 概率追问，`AIInterviewerServiceImpl:156-199`）。
- **求职闭环**：`CareerLoopStateMachine.next` 只允许平级幂等或 `getOrder()` 前进，回退抛 `BusinessException`；模拟面试结束时 `pushLoopInterviewDone` 上报 `CareerLoopStageEnum.INTERVIEW_DONE` 事件（`MockInterviewServiceImpl:885`）。

**依赖关系**：**明确调用 `xiaou-ai`** —— `AiInterviewService`（出题、评价、追问、总结）与 `AiJobBattleService`（`JobBattleServiceImpl:10`，JD 解析/简历匹配/计划/复盘/目标分析，返回 `com.xiaou.ai.dto.jobbattle.*`）。同时依赖 `xiaou-interview`（题目实体、mapper、service）。

**约束与注意事项**：
- `AdminMockInterviewController` 直接注入 3 个 Mapper 并在控制器内写业务逻辑与统计聚合（313 行），**无 Service 层**，与"业务逻辑落在业务模块 Service"的分层惯例不一致。
- 该控制器内**没有任何 `@RequireAdmin`**（grep 无命中）；其 `/admin/**` 路由仅靠 `SaTokenConfig:38-40` 的 `StpAdminUtil.checkLogin()` 路径拦截兜底，缺少方法级细粒度鉴权。
- `MockInterviewDirection.categoryIds` 以逗号分隔字符串跨模块引用 `interview_category`，无外键约束；解析失败仅记日志并退化为全量题目。

## 闪卡记忆（xiaou-flashcard）

**职责与边界**：卡组与闪卡的增删改查、副本 fork、公开卡组浏览、SM-2 复习调度、学习统计与热力图。**唯一实现真正 SM-2 的模块**。

**核心领域对象**：`FlashcardDeck`（含公开/标签/学习人数）、`Flashcard`（`frontContent`/`backContent`/`contentType`/`tags`）、`FlashcardStudyRecord`（`repetitions`、`easeFactor`、`intervalDays`、`masteryLevel 1新卡/2学习中/3已掌握`、`nextReviewTime`、`totalReviews`、`correctCount`）、`FlashcardDailyStats`。

**对外接口**

| 端 | 方法与路径 |
| --- | --- |
| 用户端 | `/flashcard/deck`（`POST`、`PUT`、`DELETE /{id}`、`GET /{id}`、`GET /my`、`POST /{id}/fork`）；`/flashcard/card`（`POST`、`POST /batch`、`PUT /{id}`、`DELETE /{id}`、`GET /deck/{deckId}`、`POST /import` 从面试题库导入）；`/flashcard/study`（`GET /today`、`GET /deck/{deckId}/next`、`POST /submit`、`GET /stats`、`GET /heatmap`） |
| 公共 | `GET /pub/flashcard/deck/list`、`/{id}`、`/{id}/cards`（未登录放行，用 `StpUserUtil.isLogin()` 试取 userId） |
| 管理端 | 无管理端控制器 |

**数据表**：`flashcard_deck`、`flashcard`、`flashcard_study_record`、`flashcard_daily_stats`。

**关键流程（复习调度 = 真 SM-2）**：`SM2Algorithm.calculate` 实现标准公式 `EF' = EF + (0.1 - (5-q)*(0.08 + (5-q)*0.02))`，`EF` 下限 `1.30`、默认 `2.50`，间隔 `0→1 天`、`1→6 天`、之后 `interval * EF`，上限 `365` 天（`SM2Algorithm:86-136`）。
`FlashcardStudyServiceImpl.submitStudyResult` 把用户 1-4 评分经 `mapQualityToSM2`（1→1、2→3、3→4、4→5）映射后调用算法，写 `nextReviewTime = now.plusDays(intervalDays)`，并聚合同日 `flashcard_daily_stats`。
注意：`SM2Algorithm.convertToSM2Quality`（1→0、2→2、3→4、4→5）**未被 `FlashcardStudyServiceImpl` 使用**，两处映射规则不一致，属冗余/潜在陷阱。
今日卡片：`selectDueCards` 优先返回到期卡，不足 `limit`（默认 20，`DAILY_NEW_CARD_LIMIT=20`）再补未学新卡（`FlashcardStudyServiceImpl:55-106`）。

**依赖关系**：不依赖 `xiaou-ai`。依赖 `xiaou-user-api`；依赖 `xiaou-interview` 用于 `importFromQuestionBank`（`FlashcardServiceImpl:181-202` 经 `InterviewQuestionService.getQuestionById` 逐题转换）。

**约束与注意事项**：用户端路由为 `/flashcard/**` 与 `/pub/flashcard/deck`，**均不在 `SaTokenConfig` 的 `/user/**`、`/admin/**` 拦截范围**，未登录访问会直接把 `null` userId 传入 Service（`StpUserUtil.getLoginIdAsLong()` 在未登录时的返回值/异常行为未确认）；无管理端接口，公开卡组与私有卡组共用 `getDeckById(id, userId)`。

## 学习资产（xiaou-learning-asset）

**职责与边界**：把用户在博客/社区/代码片段/模拟面试中产生的**内容快照**转化为可复用的学习资产候选，经用户确认与管理员审核后**发布到下游业务模块**。它不自己存题库/闪卡/知识图谱，只做转化、候选管理与发布编排。

**核心领域对象**：`LearningAssetRecord`（一次转化任务，含 `status`、`sourceType/sourceId`）、`LearningAssetCandidate`（候选项，`assetType`/`title`/`contentJson`/`tags`/`confidenceScore`/`targetModule`/`targetId`/`status`）、`LearningAssetPublishLog`、枚举 `TargetAssetType`、`TransformMode`、`LearningAssetCandidateStatus`、`LearningAssetRecordStatus`、`LearningAssetSourceSnapshot`、`TransformCandidateDraft`、`TransformResult`。

**资产类型（`TargetAssetType`，4 类）**

| code | 目标模块 | 内容 JSON 关键字段 |
| --- | --- | --- |
| `flashcard` | `xiaou-flashcard` | `frontContent`/`backContent`/`contentType` |
| `knowledge_node` | `xiaou-knowledge` | `title`/`summary`/`sourceTitle` |
| `practice_plan` | `xiaou-plan` | `planName`/`planDesc`/`targetValue`/`targetUnit` |
| `interview_question` | `xiaou-interview` | `title`/`answer`/`questionSetTitle` |

**对外接口**

| 端 | 方法与路径 |
| --- | --- |
| 用户端 | `POST /user/learning-assets/convert`、`POST /records/list`、`GET /records/{id}`、`PUT /candidates/{id}`、`POST /records/{id}/confirm`、`POST /candidates/{id}/discard`、`POST /records/{id}/publish`、`POST /records/{id}/retry` |
| 管理端 | `POST /admin/learning-assets/candidates/list`、`GET /candidates/{id}`、`PUT /candidates/{id}`、`POST /candidates/{id}/{approve,merge,reject}`、`GET /statistics`（全部 `@RequireAdmin`） |

**数据表**：`learning_asset_record`、`learning_asset_candidate`、`learning_asset_publish_log`。

**关键流程（转化 → 审核 → 发布）**
1. **转化**：`LearningAssetTransformEngineImpl.transform(snapshot, mode, targetTypes)` —— 摘要取 `summary` 或 `content` 并截断 180 字（`StrUtil.maxLength`），亮点由 tags + 按 `[，。；]` 切句取前 4 条；按目标类型生成候选草稿，置信度硬编码（flashcard `0.86`、plan `0.82`、knowledge `0.74`、interview `0.71`）；`QUICK` 模式最多 1 张卡，否则 2 张。**纯规则实现，不调用 AI**。
2. **来源**：`LearningAssetSourceServiceImpl` 引 `BlogArticleService`、`CodePenService`、`CommunityPostService`/`CommunityAiSummaryService`、`MockInterviewService`（`InterviewReportResponse`）。
3. **发布**：`LearningAssetPublishServiceImpl.publish` 依次 `publishFlashcards`（`FlashcardDeckService`/`FlashcardService`）→ `publishPlans`（`PlanService`）→ `submitReviewCandidates`（送审）→ `updateRecordStatus` → `sendPublishSummaryNotification`（`NotificationPublisher`）。
4. **审核**：`approve` 按 `TargetAssetType` 分派到 `approveKnowledgeNode` / `approveInterviewQuestion`，其他类型抛"当前资产类型无需管理员审批"（`LearningAssetPublishServiceImpl:132-135`）；另有 `merge`（合并同类候选）与 `reject`。

**依赖关系**：不依赖 `xiaou-ai`。作为**下游聚合枢纽**依赖 `xiaou-blog`、`xiaou-community`、`xiaou-codepen`、`xiaou-knowledge`、`xiaou-mock-interview`、`xiaou-flashcard`、`xiaou-plan`、`xiaou-interview`、`xiaou-notification`。

**约束与注意事项**：这是全仓库出度最高的学习类模块之一，"转化引擎 + 发布编排"集中在一个 Service，新增资产类型需同时改 `TargetAssetType`、转化引擎 `switch`、发布 `switch` 与审核 `switch` 四处；`contentJson` 为自由 JSON 字符串，无 schema 校验（`updateReviewCandidate` 仅做 `JSONUtil.parse` 语法校验）。

## 在线简历（xiaou-resume）

**职责与边界**：简历模板维护、用户简历 CRUD、模块化 section 内容、版本快照、分享链接、访问/导出统计、平台巡检报告、多格式导出。**不涉及 AI、不涉及的面试/学习数据**。

**核心领域对象**：`ResumeTemplate`、`ResumeInfo`（`version`、`status`、`visibility`）、`ResumeSection`（`sectionType`/`title`/`content`/`sortOrder`）、`ResumeVersion`（JSON 快照 + `changeLog`）、`ResumeShare`（`shareCode`、7 天过期、`accessCount`）、`ResumeAnalytics`（view/export/share/uniqueVisitors）。

**对外接口**

| 端 | 方法与路径 |
| --- | --- |
| 用户端 | `/resume`（`POST`、`PUT /{id}`、`DELETE /{id}`、`GET` 分页）、`GET /resume/{id}/preview`、`POST /resume/{id}/export`、`POST /resume/{id}/share`、`GET /resume/{id}/analytics`；`GET /resume/templates`、`/{id}` |
| 管理端 | `GET/POST /admin/resume/templates`、`PUT|DELETE|GET /{id}`、`GET /admin/resume/analytics`、`GET /admin/resume/reports`（均 `@RequireAdmin`） |

**数据表**：`resume_templates`、`resume_info`、`resume_sections`、`resume_versions`、`resume_shares`、`resume_analytics`（注意本模块表名为**复数**，与其余模块单数命名惯例不一致）。

**关键流程（生成/导出）**：`updateResume` 采用"删 section 全量重建 + `version+1` + 写 `resume_versions` 快照"（`ResumeServiceImpl:98-111`）；
`exportResume` → `buildPreview` → `renderPlainText` → `ResumeExportBuilder.buildFile` 生成字节流 → 包成 `ByteArrayMultipartFile` → `FileStorageService.uploadSingle(file, "resume", "export-<format>")` → 返回 `accessUrl` 并 `increaseExportCount`。
导出格式：`PDF`（openpdf + `STSong-Light` 中文字体）、`WORD/DOC/DOCX`（POI XWPF，SimSun）、`HTML`（内联 CSS 卡片），其他格式抛 `BusinessException`；`watermark=true` 时追加"仅供 X 使用"字样。
`createShareLink` 幂等：先查未过期 `selectActiveShare`，命中直接返回，否则生成 8 位大写随机码，`shareUrl = /resume/share/{code}`，有效期 7 天。
`getHealthReports` 巡检两类问题：无任何 section 的简历（severity 2）、过期分享链接（severity 1）。

**依赖关系**：`xiaou-filestorage`（导出上传）、`org.apache.poi:poi-ooxml:5.3.0`、`com.github.librepdf:openpdf:1.3.39`。不依赖 `xiaou-ai`。

**约束与注意事项**：导出走文件存储服务，**未确认** `ResumeExportBuilder` 产出的字节流大小上限与 `FileStorageService` 的类型白名单是否放行 `text/html`；`getOwnedResume` 用 `selectByIdAndUserId` 做归属校验，跨用户访问会抛"无权访问该简历"。

## 计划与成长自动驾驶（xiaou-plan）

**职责与边界**：通用计划打卡（创建/暂停/恢复/今日任务/打卡记录/统计）+ 周维度"成长闭环自动驾驶"（目标、任务、重排、事件、风险等级），并持有跨模块的学习排名快照。**不直接调用** interview/flashcard/mock-interview，只生成带 `routePath` 的任务。

**核心领域对象**：`UserPlan`（`PlanType`/`PlanStatus`/`RepeatType`）、`PlanCheckinRecord`、`PlanRemindTask`（+`PlanRemindScheduler`）、`GrowthAutopilotGoal`（`weekStart/weekEnd`、`weeklyHours/weeklyMinutes`、`planVersion`、`currentStage foundation|practice|interview`、`completionRate`、`riskLevel`）、`GrowthAutopilotTask`（`moduleKey`/`moduleName`/`routePath`/`resourceType`/`resourceId`/`resourceVersion`/`plannedMinutes`/`priority P1-P3`/`status`）、`GrowthAutopilotEvent`、`GrowthAutopilotRevision`、`LearningCockpitRankSnapshot`。

**对外接口**（均为用户端 `/user/plan/**`，全部 `StpUserUtil.checkLogin()`）

| 分组 | 方法与路径 |
| --- | --- |
| 计划打卡 | `POST /user/plan/create`、`PUT /update/{planId}`、`DELETE /{planId}`、`GET /{planId}`、`POST /list`、`PUT /{planId}/pause|resume`、`GET /today-tasks`、`POST /checkin`、`GET /{planId}/checkin/list`、`GET /stats/overview` |
| 自动驾驶 | `GET /user/plan/autopilot/dashboard`、`POST /generate`、`POST /replan`、`POST /tasks/{taskId}/complete`、`POST /tasks/today/complete`、`POST /tasks/{taskId}/postpone` |

**数据表**：`user_plan`、`plan_checkin_record`、`plan_remind_task`、`growth_autopilot_goal`、`growth_autopilot_task`、`growth_autopilot_event`、`growth_autopilot_revision`、`learning_cockpit_rank_snapshot`。

**关键流程（统一"学习成长"模型）**
1. `generateWeeklyPlan`：按 `weekStart` 归一化到周一，首次插入 goal（`planVersion=1`、`DEFAULT_WEEKLY_HOURS=8`、阶段限 3-40 小时）；已存在则走 `goalMapper.advancePlanVersion` **乐观锁**（返回 ≠1 抛"计划已发生变化，请刷新后重试"），并 `supersedeTodoByGoalId` 作废旧的 todo 任务，再 `buildWeeklyTasks` 批量插入并 `refreshGoalMetrics` + 写 `growth_autopilot_event`。
2. **权重模板**：`buildModuleTemplates` 定义 `ModuleTemplate(moduleKey, name, weight, routePath, ...)`，覆盖 `oj`/`interview`/`flashcard`/`plan`/`mock`/`points`（如 `interview` 权重 0.22、`flashcard` 0.16、`mock` 0.14，`GrowthAutopilotServiceImpl:671-677`），再按 `currentStage` 用 `rebalance` 覆盖权重（`foundation`/`practice`/`interview` 三套，`interview` 阶段 `mock` 升到 0.24、`interview` 0.30）。
3. **预算约束重排**：`GrowthPlanConstraintPlanner.preview` 是**纯函数式**计划器 —— 校验 `availableMinutes ∈ [15, 2400]`、`scheduleStart = max(today, goal.weekStart)` 且未超 `weekEnd`；候选任务需 `status=todo`、有 `resourceId` 或 `routePath`、`plannedMinutes > 0`；排序键为"是否优先面试(`moduleKey in {interview, mock}`) → 是否命中 `preferredModuleKeys` → `P1/P2/P3` 权重 → `taskDate` → `id`"；按天轮转分配，放不下的任务产出 `SUPERSEDE` 变更并保留已完成任务。禁止回退、每次调整递增 `planVersion`（`GrowthAutopilotRevision` 留档）。

**依赖关系**：不依赖 `xiaou-ai`、不依赖 interview/flashcard/mock-interview（`pom.xml` 仅引公共模块 + `xiaou-notification` + `xiaou-user-api`）。与学习模块是**弱耦合**：任务里存 `routePath`/`resourceId` 字符串指向前端路由或下游资源 ID，运行时不做跨模块校验。

**约束与注意事项**：这是当前唯一"跨模块统一成长视图"的落点，但它是**编排/展示层聚合**而非共享领域模型 —— 各学习模块各自的掌握度、复习、会话数据并不写入 `growth_autopilot_*`；`LearningCockpitRankSnapshot` 的写入方未确认（模块内只有 mapper 的 `upsert`/`select*`，未见 Service 调用）。

## 跨模块观察

**共性模式**
- 6 个模块统一 `Result<T>` + `@RequiredArgsConstructor` 构造注入 + `PageHelper.doPage` 分页；实体均为 Lombok `@Data`（部分 `@Accessors(chain=true)`），**无 `@TableName`**，表名硬编码在 Mapper XML。
- 每模块都有独立的 `interview_daily_stats` / `flashcard_daily_stats` 型"日粒度统计表"与各自的热力图/streak 实现：`InterviewMasteryServiceImpl.calculateCurrentStreak` 与 `FlashcardStudyServiceImpl.calculateStreakDays` 逻辑高度相似（均为"从今天或昨天逆推连续天"），**重复实现**。
- 日志风格统一 `@Slf4j`；异常统一 `com.xiaou.common.exception.BusinessException`。

**重复建模与边界结论**
- **题目/题库未重复建模**：全仓仅 `interview_question`/`interview_question_set`；`mock-interview`、`flashcard`、`learning-asset` 均通过 `xiaou-interview` 的 Service/Mapper 复用（`QuestionSelectorServiceImpl`、`FlashcardServiceImpl.importFromQuestionBank`、`LearningAssetPublishServiceImpl.approveInterviewQuestion`）。`mock-interview` 只新增了"会话/问答/方向"自己的表。
- **重复建模的是"掌握度 + 复习调度"**：`interview`（分级间隔 × 2^n，上限 60 天，无 EF）与 `flashcard`（完整 SM-2，含 EF，上限 365 天）两套算法、两套表、两套 mastery 语义（`1-4` vs `1-3`）互不相通，同一用户对"同一知识点"的掌握状态无法合并。
- **AI 边界清晰**：只有 `xiaou-mock-interview` 依赖 `xiaou-ai`；`learning-asset` 的转化引擎是纯规则（正则切句 + 硬编码置信度），**未用 AI**（其 pom 也不引 `xiaou-ai`）。

**潜在风险**
1. **路由分区与 `always.md` 不一致**：规则要求"用户端 `/user/**`"，但 `xiaou-interview` 用 `/interview/**`、`xiaou-flashcard` 用 `/flashcard/**` + `/pub/flashcard/deck`、`xiaou-resume` 用 `/resume/**`。`SaTokenConfig:38-53` 只对 `/auth/**`、`/admin/**`（`StpAdminUtil.checkLogin`）与 `/user/**`（`StpUserUtil.checkLogin`）做全局拦截，因此上述路由**没有框架级登录保护**，完全依赖控制器内手工校验，且各控制器写法不统一（`checkLogin()` / `getLoginIdAsLong()` 无判空 / 返回 `Result.error("请先登录")` 三种风格并存）。
2. `InterviewQuestionSetPublicController:59` 的 `userId != null &&` 短路使未登录用户绕过题单权限校验。
3. `AdminMockInterviewController` 缺少 `@RequireAdmin` 且无 Service 层，与规则"管理端方法优先 `@RequireAdmin`"不一致（当前仅靠 `/admin/**` 路径拦截）。
4. `SM2Algorithm.convertToSM2Quality` 与 `FlashcardStudyServiceImpl.mapQualityToSM2` 两套 1-4→0-5 映射不一致，前者未被使用，易误用。
5. `LearningAssetPublishServiceImpl` 在单个 `@Transactional` 内跨 4 个下游模块的 Service 写入（flashcard/plan/knowledge/interview）+ 发通知，**跨模块事务边界与失败回滚未确认**；`contentJson` 无 schema 校验。
6. `MockInterviewDirection.categoryIds` 逗号字符串、`GrowthAutopilotTask.routePath/resourceId` 字符串引用，均为无约束的跨模块弱引用。
7. 表名风格不统一：`resume_*` 复数、其余单数；`job_battle_*`/`career_*` 前缀游离于 `mock_interview_*` 之外，同属一个模块却不同命名域。

**未确认清单**
- `SaTokenConfig.excludePathPatterns` 的完整白名单内容（仅读到第 62 行起始）。
- `MockInterviewServiceImpl.calculateAndUpdateScores`（`:709`）的四项分项得分权重与总分算法。
- `LearningAssetServiceImpl.convert` 全流程、`TransformMode` 取值语义、`LearningAssetSourceServiceImpl` 的源类型枚举与权限过滤。
- `interview`/`flashcard` 是否有定时任务推送复习提醒（仅确认 `xiaou-plan` 有 `PlanRemindScheduler`）。
- `LearningCockpitRankSnapshot` 的写入调用方与排名计算口径。
- `InterviewFavorite.targetType` 取值语义与 `hasAccessPermission` 实现。
- `career_loop_*` / `job_battle_*` / `growth_autopilot_*` 的 DDL 与索引（未读 `sql/`）。
- `JobBattleServiceImpl` 是否持久化 AI 结果原文与失败重试策略。
