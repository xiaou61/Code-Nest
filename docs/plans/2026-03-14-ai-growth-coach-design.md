# AI成长教练 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在不新增一级导航模块的前提下，为学习驾驶舱和求职闭环中台引入同一套 AI 成长教练能力，提供统一诊断、行动建议、追问式对话与后台统计配置。

**Architecture:** 后端能力集中落在 `xiaou-ai`，新增 `growth coach` 领域，负责快照生成、行动建议、对话会话与后台配置；前端以 `learning-cockpit` 作为完整承载页，以 `career-loop` 作为摘要承载页，两端共享同一套用户 API 和对话状态。AI 策略采用“规则前置聚合 + Coze 工作流结构化输出 + 本地兜底”的混合方案，其中 Coze 负责诊断快照、行动重排和追问式解释对话，一期避免直接做开放聊天与自动代执行。

**Tech Stack:** Spring Boot 3、MyBatis XML、Sa-Token、Maven 多模块、Vue3、Element Plus、Pinia、Vite、现有 `xiaou-ai`/`career-loop`/`learning-cockpit` 页面体系。

---

## 约束与执行规范

- 严格按 `@test-driven-development` 执行，后端先写失败测试再写实现。
- 接口遵循 `SKILL.md`：用户端走 `/user/**`，管理端走 `/admin/**`，统一返回 `Result<T>`。
- 后端能力不新开独立 Maven 模块，统一收敛到 `xiaou-ai`。
- 数据库脚本按现有递增规则写到 `sql/v1.8.5/ai_growth_coach.sql`。
- 需要同步维护 `docs/coze/AI成长教练工作流配置指南.md`，并通过 `CozeWorkflowEnum` 管理工作流 ID。
- 手工改文件时使用 `apply_patch`，不要编辑 `pom-xml-flattened`。
- 完成前按 `@verification-before-completion` 跑后端编译和前端构建。

---

### Task 1: 建立 AI成长教练数据模型与 SQL 脚本

**Files:**
- Create: `sql/v1.8.5/ai_growth_coach.sql`
- Modify: `xiaou-common/src/main/java/com/xiaou/common/enums/CozeWorkflowEnum.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/domain/growthcoach/AiGrowthCoachSnapshot.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/domain/growthcoach/AiGrowthCoachAction.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/domain/growthcoach/AiGrowthCoachChatSession.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/domain/growthcoach/AiGrowthCoachChatMessage.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/domain/growthcoach/AiGrowthCoachConfig.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/enums/growthcoach/AiGrowthCoachScene.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/enums/growthcoach/AiGrowthCoachSnapshotStatus.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/enums/growthcoach/AiGrowthCoachActionStatus.java`
- Test: `xiaou-ai/src/test/java/com/xiaou/ai/sql/AiGrowthCoachSchemaSqlTest.java`

**Step 1: Write the failing test**

```java
@Test
void aiGrowthCoachSqlShouldContainAllCoreTables() throws Exception {
    String sql = Files.readString(Path.of("../sql/v1.8.5/ai_growth_coach.sql"));
    assertTrue(sql.contains("ai_growth_coach_snapshot"));
    assertTrue(sql.contains("ai_growth_coach_action"));
    assertTrue(sql.contains("ai_growth_coach_chat_session"));
    assertTrue(sql.contains("ai_growth_coach_chat_message"));
    assertTrue(sql.contains("ai_growth_coach_config"));
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -pl xiaou-ai -am -Dtest=AiGrowthCoachSchemaSqlTest test`
Expected: FAIL，提示 SQL 文件或表名不存在。

**Step 3: Write minimal implementation**

- 新建 SQL 增量脚本。
- 在 `CozeWorkflowEnum` 中预留 `GROWTH_COACH_SNAPSHOT`、`GROWTH_COACH_ACTION_REPLAN`、`GROWTH_COACH_CHAT` 三个枚举。
- 定义领域对象和枚举，字段与 PRD 保持一致。

```sql
CREATE TABLE IF NOT EXISTS `ai_growth_coach_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `scene_scope` varchar(32) NOT NULL,
  `overall_score` int DEFAULT 0,
  `status` varchar(32) NOT NULL,
  `summary_json` longtext,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);
```

**Step 4: Run test to verify it passes**

Run: `mvn -pl xiaou-ai -am -Dtest=AiGrowthCoachSchemaSqlTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add sql/v1.8.5/ai_growth_coach.sql xiaou-common/src/main/java/com/xiaou/common/enums/CozeWorkflowEnum.java xiaou-ai/src/main/java/com/xiaou/ai/domain/growthcoach xiaou-ai/src/main/java/com/xiaou/ai/enums/growthcoach xiaou-ai/src/test/java/com/xiaou/ai/sql/AiGrowthCoachSchemaSqlTest.java
git commit -m "feat: add ai growth coach schema"
```

---

### Task 2: 建立 Mapper 与快照持久层

**Files:**
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/mapper/growthcoach/AiGrowthCoachSnapshotMapper.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/mapper/growthcoach/AiGrowthCoachActionMapper.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/mapper/growthcoach/AiGrowthCoachChatSessionMapper.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/mapper/growthcoach/AiGrowthCoachChatMessageMapper.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/mapper/growthcoach/AiGrowthCoachConfigMapper.java`
- Create: `xiaou-ai/src/main/resources/mapper/growthcoach/AiGrowthCoachSnapshotMapper.xml`
- Create: `xiaou-ai/src/main/resources/mapper/growthcoach/AiGrowthCoachActionMapper.xml`
- Create: `xiaou-ai/src/main/resources/mapper/growthcoach/AiGrowthCoachChatSessionMapper.xml`
- Create: `xiaou-ai/src/main/resources/mapper/growthcoach/AiGrowthCoachChatMessageMapper.xml`
- Create: `xiaou-ai/src/main/resources/mapper/growthcoach/AiGrowthCoachConfigMapper.xml`
- Test: `xiaou-ai/src/test/java/com/xiaou/ai/mapper/growthcoach/AiGrowthCoachSnapshotMapperTest.java`

**Step 1: Write the failing test**

```java
@Test
void shouldInsertAndQuerySnapshot() {
    AiGrowthCoachSnapshot snapshot = new AiGrowthCoachSnapshot();
    snapshot.setUserId(1L);
    snapshot.setSceneScope("hybrid");
    snapshot.setStatus("READY");
    mapper.insert(snapshot);
    assertNotNull(mapper.selectLatestByUserId(1L));
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -pl xiaou-ai -am -Dtest=AiGrowthCoachSnapshotMapperTest test`
Expected: FAIL，提示 mapper 或 XML 缺失。

**Step 3: Write minimal implementation**

- 建立 Mapper 接口与 XML。
- 先实现最小 CRUD：插入快照、查询用户最新快照、批量删除动作、插入会话与消息。

```xml
<select id="selectLatestByUserId" resultType="com.xiaou.ai.domain.growthcoach.AiGrowthCoachSnapshot">
  SELECT * FROM ai_growth_coach_snapshot
  WHERE user_id = #{userId}
  ORDER BY id DESC
  LIMIT 1
</select>
```

**Step 4: Run test to verify it passes**

Run: `mvn -pl xiaou-ai -am -Dtest=AiGrowthCoachSnapshotMapperTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add xiaou-ai/src/main/java/com/xiaou/ai/mapper/growthcoach xiaou-ai/src/main/resources/mapper/growthcoach xiaou-ai/src/test/java/com/xiaou/ai/mapper/growthcoach/AiGrowthCoachSnapshotMapperTest.java
git commit -m "feat: add ai growth coach mappers"
```

---

### Task 3: 实现多源数据聚合服务

**Files:**
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/dto/growthcoach/AiGrowthCoachSourceBundle.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/service/growthcoach/AiGrowthCoachAggregateService.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/service/growthcoach/impl/AiGrowthCoachAggregateServiceImpl.java`
- Modify: `xiaou-ai/src/main/java/com/xiaou/ai/service/AiInterviewService.java`
- Modify: `xiaou-ai/src/main/java/com/xiaou/ai/service/AiJobBattleService.java`
- Test: `xiaou-ai/src/test/java/com/xiaou/ai/service/growthcoach/AiGrowthCoachAggregateServiceTest.java`

**Step 1: Write the failing test**

```java
@Test
void shouldAggregateInterviewPlanAndCareerLoopSignals() {
    AiGrowthCoachSourceBundle bundle = aggregateService.buildSourceBundle(1L, "hybrid");
    assertNotNull(bundle.getInterviewStats());
    assertNotNull(bundle.getPlanStats());
    assertNotNull(bundle.getCareerLoopSnapshot());
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -pl xiaou-ai -am -Dtest=AiGrowthCoachAggregateServiceTest test`
Expected: FAIL

**Step 3: Write minimal implementation**

- 为刷题、闪卡、计划、学习资产、模拟面试、岗位匹配、求职闭环建立聚合适配器。
- 一期允许部分来源缺失，但必须输出统一 bundle。

```java
public AiGrowthCoachSourceBundle buildSourceBundle(Long userId, String scene) {
    return AiGrowthCoachSourceBundle.builder()
            .interviewStats(loadInterviewStats(userId))
            .planStats(loadPlanStats(userId))
            .careerLoopSnapshot(loadCareerLoop(userId))
            .jobMatchSummary(loadJobMatch(userId))
            .build();
}
```

**Step 4: Run test to verify it passes**

Run: `mvn -pl xiaou-ai -am -Dtest=AiGrowthCoachAggregateServiceTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add xiaou-ai/src/main/java/com/xiaou/ai/dto/growthcoach/AiGrowthCoachSourceBundle.java xiaou-ai/src/main/java/com/xiaou/ai/service/growthcoach xiaou-ai/src/test/java/com/xiaou/ai/service/growthcoach/AiGrowthCoachAggregateServiceTest.java
git commit -m "feat: aggregate ai growth coach source signals"
```

---

### Task 4: 实现快照生成与动作排序服务

**Files:**
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/dto/growthcoach/request/AiGrowthCoachRefreshRequest.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/dto/growthcoach/response/AiGrowthCoachSummaryResponse.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/dto/growthcoach/response/AiGrowthCoachSnapshotDetailResponse.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/service/growthcoach/AiGrowthCoachSnapshotService.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/service/growthcoach/impl/AiGrowthCoachSnapshotServiceImpl.java`
- Modify: `xiaou-ai/src/main/java/com/xiaou/ai/util/CozeResponseParser.java`
- Test: `xiaou-ai/src/test/java/com/xiaou/ai/service/growthcoach/AiGrowthCoachSnapshotServiceTest.java`

**Step 1: Write the failing test**

```java
@Test
void refreshShouldCreateSnapshotAndPrioritizedActions() {
    AiGrowthCoachSummaryResponse response = snapshotService.refresh(1L, "hybrid", true);
    assertEquals("READY", response.getStatus());
    assertFalse(response.getTopActions().isEmpty());
    assertTrue(response.getTopActions().get(0).getPriority() <= response.getTopActions().get(1).getPriority());
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -pl xiaou-ai -am -Dtest=AiGrowthCoachSnapshotServiceTest test`
Expected: FAIL

**Step 3: Write minimal implementation**

- 先用规则计算学习分、求职分、执行分和总分。
- 调用 `GROWTH_COACH_SNAPSHOT` 工作流生成结构化诊断快照。
- 生成 P0/P1/P2 动作清单。
- 保存快照与动作。

```java
private List<AiGrowthCoachAction> buildActions(AiGrowthCoachSourceBundle bundle) {
    return List.of(
        createAction("补齐高频薄弱点", "去刷题", "P0", "/interview"),
        createAction("完成一次模拟面试", "去训练", "P1", "/mock-interview")
    );
}
```

**Step 4: Run test to verify it passes**

Run: `mvn -pl xiaou-ai -am -Dtest=AiGrowthCoachSnapshotServiceTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add xiaou-ai/src/main/java/com/xiaou/ai/dto/growthcoach/request xiaou-ai/src/main/java/com/xiaou/ai/dto/growthcoach/response xiaou-ai/src/main/java/com/xiaou/ai/service/growthcoach/AiGrowthCoachSnapshotService.java xiaou-ai/src/main/java/com/xiaou/ai/service/growthcoach/impl/AiGrowthCoachSnapshotServiceImpl.java xiaou-ai/src/main/java/com/xiaou/ai/util/CozeResponseParser.java xiaou-ai/src/test/java/com/xiaou/ai/service/growthcoach/AiGrowthCoachSnapshotServiceTest.java
git commit -m "feat: generate ai growth coach snapshots"
```

---

### Task 5: 实现追问式 AI 对话服务

**Files:**
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/dto/growthcoach/request/AiGrowthCoachChatRequest.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/dto/growthcoach/response/AiGrowthCoachChatResponse.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/service/growthcoach/AiGrowthCoachChatService.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/service/growthcoach/impl/AiGrowthCoachChatServiceImpl.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/util/AiGrowthCoachPromptBuilder.java`
- Test: `xiaou-ai/src/test/java/com/xiaou/ai/service/growthcoach/AiGrowthCoachChatServiceTest.java`

**Step 1: Write the failing test**

```java
@Test
void chatShouldAnswerBasedOnSnapshotInsteadOfGenericText() {
    AiGrowthCoachChatResponse response = chatService.chat(1L, request);
    assertTrue(response.getAnswer().contains("当前诊断"));
    assertFalse(response.getReferences().isEmpty());
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -pl xiaou-ai -am -Dtest=AiGrowthCoachChatServiceTest test`
Expected: FAIL

**Step 3: Write minimal implementation**

- 根据快照和追问内容拼装 prompt。
- 优先调用 `GROWTH_COACH_CHAT` 工作流，失败时回退到模板化解释。
- 回答必须带引用来源摘要与推荐动作。
- 一期允许使用模板化兜底，不要求完全依赖远程 AI 服务。

```java
String answer = "当前诊断显示你在 Redis 与系统设计上风险更高，建议先完成 P0 动作。";
response.setReferences(List.of("模拟面试报告", "岗位匹配结果"));
```

**Step 4: Run test to verify it passes**

Run: `mvn -pl xiaou-ai -am -Dtest=AiGrowthCoachChatServiceTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add xiaou-ai/src/main/java/com/xiaou/ai/dto/growthcoach/request/AiGrowthCoachChatRequest.java xiaou-ai/src/main/java/com/xiaou/ai/dto/growthcoach/response/AiGrowthCoachChatResponse.java xiaou-ai/src/main/java/com/xiaou/ai/service/growthcoach/AiGrowthCoachChatService.java xiaou-ai/src/main/java/com/xiaou/ai/service/growthcoach/impl/AiGrowthCoachChatServiceImpl.java xiaou-ai/src/main/java/com/xiaou/ai/util/AiGrowthCoachPromptBuilder.java xiaou-ai/src/test/java/com/xiaou/ai/service/growthcoach/AiGrowthCoachChatServiceTest.java
git commit -m "feat: add ai growth coach chat"
```

---

### Task 6: 暴露用户端与管理端后端接口

**Files:**
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/controller/user/UserAiGrowthCoachController.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/controller/admin/AdminAiGrowthCoachController.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/dto/growthcoach/response/AiGrowthCoachAdminStatisticsResponse.java`
- Test: `xiaou-ai/src/test/java/com/xiaou/ai/controller/growthcoach/UserAiGrowthCoachControllerTest.java`
- Test: `xiaou-ai/src/test/java/com/xiaou/ai/controller/growthcoach/AdminAiGrowthCoachControllerTest.java`

**Step 1: Write the failing test**

```java
@Test
void userSummaryEndpointShouldRequireUserLogin() throws Exception {
    mockMvc.perform(get("/user/ai-growth-coach/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(701));
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -pl xiaou-ai -am -Dtest=UserAiGrowthCoachControllerTest,AdminAiGrowthCoachControllerTest test`
Expected: FAIL

**Step 3: Write minimal implementation**

- 用户端接口：
  - `GET /user/ai-growth-coach/summary`
  - `GET /user/ai-growth-coach/snapshots/{id}`
  - `POST /user/ai-growth-coach/refresh`
  - `POST /user/ai-growth-coach/chat`
  - `POST /user/ai-growth-coach/actions/{id}/status`
- 管理端接口：
  - `GET /admin/ai-growth-coach/statistics`
  - `GET /admin/ai-growth-coach/config`
  - `PUT /admin/ai-growth-coach/config`

**Step 4: Run test to verify it passes**

Run: `mvn -pl xiaou-ai -am -Dtest=UserAiGrowthCoachControllerTest,AdminAiGrowthCoachControllerTest test`
Expected: PASS

**Step 5: Commit**

```bash
git add xiaou-ai/src/main/java/com/xiaou/ai/controller/user/UserAiGrowthCoachController.java xiaou-ai/src/main/java/com/xiaou/ai/controller/admin/AdminAiGrowthCoachController.java xiaou-ai/src/main/java/com/xiaou/ai/dto/growthcoach/response/AiGrowthCoachAdminStatisticsResponse.java xiaou-ai/src/test/java/com/xiaou/ai/controller/growthcoach
git commit -m "feat: expose ai growth coach apis"
```

---

### Task 7: 用户端接入学习驾驶舱完整面板

**Files:**
- Create: `vue3-user-front/src/api/aiGrowthCoach.js`
- Create: `vue3-user-front/src/components/ai-growth-coach/CoachOverviewPanel.vue`
- Create: `vue3-user-front/src/components/ai-growth-coach/CoachActionList.vue`
- Create: `vue3-user-front/src/components/ai-growth-coach/CoachChatDrawer.vue`
- Modify: `vue3-user-front/src/views/learning-cockpit/Index.vue`
- Test: 前端构建验证 `vue3-user-front npm run build`

**Step 1: Write the failing test**

前端不额外引入测试框架，本任务以构建失败作为失败信号。

**Step 2: Run build to verify missing imports fail**

Run: `cd vue3-user-front && npm run build`
Expected: FAIL，提示 `aiGrowthCoach.js` 或组件不存在。

**Step 3: Write minimal implementation**

- 新增用户端 API 封装。
- 在学习驾驶舱新增 `AI成长教练` Tab。
- 接入摘要区、动作区、追问式对话抽屉。

```js
export function getGrowthCoachSummary(scene = 'hybrid') {
  return request.get('/user/ai-growth-coach/summary', { scene })
}
```

**Step 4: Run build to verify it passes**

Run: `cd vue3-user-front && npm run build`
Expected: BUILD SUCCESS

**Step 5: Commit**

```bash
git add vue3-user-front/src/api/aiGrowthCoach.js vue3-user-front/src/components/ai-growth-coach vue3-user-front/src/views/learning-cockpit/Index.vue
git commit -m "feat: add ai growth coach to learning cockpit"
```

---

### Task 8: 用户端接入求职闭环摘要与继续追问

**Files:**
- Modify: `vue3-user-front/src/views/career-loop/Index.vue`
- Modify: `vue3-user-front/src/components/ai-growth-coach/CoachChatDrawer.vue`
- Modify: `vue3-user-front/src/api/aiGrowthCoach.js`
- Test: 前端构建验证 `vue3-user-front npm run build`

**Step 1: Write the failing test**

前端仍以构建作为最小验证。

**Step 2: Run build to verify current code fails or lacks symbols**

Run: `cd vue3-user-front && npm run build`
Expected: FAIL，直到闭环页完成摘要卡接入。

**Step 3: Write minimal implementation**

- 在求职闭环中新增 AI 教练摘要卡。
- 新增“继续追问”按钮，复用学习驾驶舱同一对话抽屉。
- 增加跳转到岗位匹配、模拟面试、学习驾驶舱的承接动作。

```vue
<el-card shadow="never" class="panel-card">
  <template #header>
    <div class="panel-title">AI 教练摘要</div>
  </template>
</el-card>
```

**Step 4: Run build to verify it passes**

Run: `cd vue3-user-front && npm run build`
Expected: BUILD SUCCESS

**Step 5: Commit**

```bash
git add vue3-user-front/src/views/career-loop/Index.vue vue3-user-front/src/components/ai-growth-coach/CoachChatDrawer.vue vue3-user-front/src/api/aiGrowthCoach.js
git commit -m "feat: add ai growth coach summary to career loop"
```

---

### Task 9: 管理端接入统计与配置页面

**Files:**
- Create: `vue3-admin-front/src/api/aiGrowthCoach.js`
- Create: `vue3-admin-front/src/views/ai-growth-coach/index.vue`
- Modify: `vue3-admin-front/src/router/index.js`
- Modify: `vue3-admin-front/src/layout/index.vue`
- Test: 前端构建验证 `vue3-admin-front npm run build`

**Step 1: Write the failing test**

管理端同样以构建作为最小验证。

**Step 2: Run build to verify missing page/api fail**

Run: `cd vue3-admin-front && npm run build`
Expected: FAIL

**Step 3: Write minimal implementation**

- 新增管理端统计和配置页面。
- 展示生成次数、追问次数、动作点击率、失败率。
- 支持读取/更新基础配置。

```js
export function getAiGrowthCoachStatistics() {
  return request.get('/admin/ai-growth-coach/statistics')
}
```

**Step 4: Run build to verify it passes**

Run: `cd vue3-admin-front && npm run build`
Expected: BUILD SUCCESS

**Step 5: Commit**

```bash
git add vue3-admin-front/src/api/aiGrowthCoach.js vue3-admin-front/src/views/ai-growth-coach/index.vue vue3-admin-front/src/router/index.js vue3-admin-front/src/layout/index.vue
git commit -m "feat: add ai growth coach admin console"
```

---

### Task 10: 联调、验证与文档收口

**Files:**
- Modify: `README.md`
- Modify: `docs/PRD/AI成长教练模块PRD-v1.0.0.md`
- Modify: `docs/plans/2026-03-14-ai-growth-coach-design.md`
- Modify: `docs/coze/AI成长教练工作流配置指南.md`
- Test: `xiaou-ai/src/test/java/com/xiaou/ai/**`

**Step 1: Run targeted backend tests**

Run: `mvn -pl xiaou-ai -am "-Dtest=AiGrowthCoachSchemaSqlTest,AiGrowthCoachSnapshotMapperTest,AiGrowthCoachAggregateServiceTest,AiGrowthCoachSnapshotServiceTest,AiGrowthCoachChatServiceTest,UserAiGrowthCoachControllerTest,AdminAiGrowthCoachControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
Expected: PASS

**Step 2: Run backend package build**

Run: `mvn -pl xiaou-application -am -DskipTests package`
Expected: BUILD SUCCESS

**Step 3: Run frontend builds**

Run: `cd vue3-user-front && npm run build`
Expected: BUILD SUCCESS

Run: `cd vue3-admin-front && npm run build`
Expected: BUILD SUCCESS

**Step 4: Update docs**

- README 增补 AI 成长教练能力说明。
- PRD 根据真实落地细节修正一期边界。
- Coze 指南回填真实工作流 ID、输入输出和调试注意事项。

**Step 5: Commit**

```bash
git add README.md docs/PRD/AI成长教练模块PRD-v1.0.0.md docs/plans/2026-03-14-ai-growth-coach-design.md docs/coze/AI成长教练工作流配置指南.md
git commit -m "docs: finalize ai growth coach docs and verification"
```

---

## 时间压缩重排补充设计

### 目标

- 支持用户围绕当前教练快照做“如果我这周只有 N 分钟/小时怎么办”的预算模拟。
- 输出一版临时重排后的行动清单，不覆盖当前快照、不生成新快照、不污染统计口径。
- 继续保持“规则优先 + Coze 工作流增强 + 本地兜底”的模式。

### 交互形态

- 入口放在 `CoachWorkspace` 的行动清单区域。
- 用户输入一个时间预算后，触发“时间压缩重排”。
- 前端展示一份临时结果：保留动作、延后动作、重排摘要、推荐追问。
- 用户可以继续点击“去执行”，但原始快照与原始行动清单保持不变。

### 后端设计

- 新增用户端接口：`POST /user/ai-growth-coach/replan`
- 请求体最小字段：
  - `snapshotId`
  - `scene`
  - `availableMinutes`
- 响应字段：
  - `snapshotId`
  - `scene`
  - `availableMinutes`
  - `originalTotalMinutes`
  - `summary`
  - `fallbackOnly`
  - `actions`
  - `deferredActions`
  - `suggestedQuestions`

### 执行策略

- 先读取当前快照与动作列表，做用户归属校验。
- 规则层先按 `priority + estimatedMinutes + targetRoute` 生成一版压缩结果：
  - 优先保留 `P0`
  - 再按时长塞入 `P1`
  - 超出预算的动作归入 `deferredActions`
- 再调用 `GROWTH_COACH_ACTION_REPLAN` 工作流润色摘要、压缩理由和建议问题。
- 如果 Coze 失败，则返回规则结果并标记 `fallbackOnly = true`。

### 边界

- 本期不生成新快照，不写入数据库。
- 本期不增加“保存为新快照”或“应用重排覆盖原清单”能力。
- 本期只支持围绕单个 `snapshotId` 做重排，不做多快照对比。

---

## 时间压缩重排统计补充设计

### 目标

- 为“时间压缩重排”建立独立统计口径，支持后台查看用户是否在频繁使用预算模拟。
- 统计必须区分“快照生成”“追问对话”“动作完成”和“重排预览”，避免混淆业务含义。
- 保持用户侧体验不变，不新增前台设置页，也不把重排日志暴露给普通用户。

### 数据模型

- 新增表：`ai_growth_coach_replan_log`
- 记录一次重排预览的最小字段：
  - `user_id`
  - `snapshot_id`
  - `scene_scope`
  - `available_minutes`
  - `original_total_minutes`
  - `selected_count`
  - `deferred_count`
  - `fallback_only`
  - `create_time`

### 统计口径

- `totalReplans`：重排预览总次数。
- `todayReplans`：今日重排预览次数。
- `replanFallbackRate`：重排预览中走本地兜底的占比。
- `avgCompressionRate`：平均压缩率，计算口径为 `1 - available_minutes / original_total_minutes`，按每次预览聚合后取平均。

### 后端流程

- 用户调用 `POST /user/ai-growth-coach/replan` 成功返回前，落一条重排日志。
- 即使 Coze 工作流失败，只要规则引擎产出了预览结果，也要记日志。
- 管理端 `GET /admin/ai-growth-coach/statistics` 增加重排维度字段，不新增单独统计接口。

### 前端展示

- 管理端统计页新增一组“时间压缩重排”指标卡。
- 一期只展示聚合结果，不做日志列表、筛选和导出。

### 边界

- 本期不做按用户、场景、时间区间的重排明细查询。
- 本期不把重排行为写入会话消息，也不通过聊天消息数反推重排次数。
- 本期不把重排数据并入失败案例列表，因为规则兜底不是失败。
