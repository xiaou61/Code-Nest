# 求职闭环中台（V1）Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 新增“求职闭环中台”，将 Job Battle、计划执行、模拟面试、复盘统一到一个可追踪状态机与动作清单中。  

**Architecture:** 采用中心编排式方案，在 `xiaou-mock-interview` 新增 `career-loop` 领域（会话、阶段日志、动作、快照），通过事件驱动推进状态机；用户端新增 `/career-loop` 页面统一展示闭环状态、时间线和下一步动作。既有业务模块保持职责不变，只做最小接入。  

**Tech Stack:** Spring Boot 3 + MyBatis XML + Sa-Token + Vue3 + Element Plus + Pinia + Node test + Maven 多模块构建。  

---

## 约束与执行规范

- 严格按 `@test-driven-development` 执行：先写失败测试，再写最小实现，再回归。
- 每个任务完成后做小提交，避免大批量混改。
- 完成前按 `@verification-before-completion` 做构建和关键链路验证。
- V1 阶段到 `REVIEWED`，不在本期实现 Offer 跟踪系统。

---

### Task 1: 建立闭环中台数据模型与 SQL 脚本

**Files:**
- Create: `sql/v1.8.3/career_loop.sql`
- Create: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/domain/CareerLoopSession.java`
- Create: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/domain/CareerLoopStageLog.java`
- Create: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/domain/CareerLoopAction.java`
- Create: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/domain/CareerLoopSnapshot.java`
- Create: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/enums/CareerLoopStageEnum.java`
- Test: `xiaou-mock-interview/src/test/java/com/xiaou/mockinterview/sql/CareerLoopSchemaSqlTest.java`

**Step 1: 写失败测试（校验 SQL 关键 DDL）**

```java
@Test
void career_loop_sql_should_define_required_tables() throws Exception {
    String sql = Files.readString(Path.of("sql/v1.8.3/career_loop.sql"));
    assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `career_loop_session`"));
    assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `career_loop_stage_log`"));
    assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `career_loop_action`"));
    assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `career_loop_snapshot`"));
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-mock-interview -am -Dtest=CareerLoopSchemaSqlTest test`  
Expected: FAIL（脚本或测试类不存在）。

**Step 3: 写最小实现**

```sql
CREATE TABLE IF NOT EXISTS `career_loop_session` (...);
CREATE TABLE IF NOT EXISTS `career_loop_stage_log` (...);
CREATE TABLE IF NOT EXISTS `career_loop_action` (...);
CREATE TABLE IF NOT EXISTS `career_loop_snapshot` (...);
```

**Step 4: 重跑测试并编译**

Run: `mvn -pl xiaou-mock-interview -am -Dtest=CareerLoopSchemaSqlTest test`  
Expected: PASS  

Run: `mvn -pl xiaou-mock-interview -am -DskipTests compile`  
Expected: BUILD SUCCESS

**Step 5: Commit**

```bash
git add sql/v1.8.3/career_loop.sql xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/domain xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/enums/CareerLoopStageEnum.java xiaou-mock-interview/src/test/java/com/xiaou/mockinterview/sql/CareerLoopSchemaSqlTest.java
git commit -m "feat(career-loop): add schema and core domain models"
```

---

### Task 2: 实现状态机规则（纯业务层）

**Files:**
- Create: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/CareerLoopStateMachine.java`
- Test: `xiaou-mock-interview/src/test/java/com/xiaou/mockinterview/service/CareerLoopStateMachineTest.java`

**Step 1: 写失败测试（非法回退与重复事件幂等）**

```java
@Test
void should_reject_backward_transition() {
    assertThrows(BusinessException.class, () ->
        machine.next(CareerLoopStageEnum.PLAN_READY, CareerLoopStageEnum.JD_PARSED));
}

@Test
void should_be_idempotent_for_same_stage() {
    assertEquals(CareerLoopStageEnum.PLAN_READY,
        machine.next(CareerLoopStageEnum.PLAN_READY, CareerLoopStageEnum.PLAN_READY));
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-mock-interview -am -Dtest=CareerLoopStateMachineTest test`  
Expected: FAIL

**Step 3: 写最小实现**

```java
public CareerLoopStageEnum next(CareerLoopStageEnum current, CareerLoopStageEnum target) {
    if (current == target) return current;
    if (target.getOrder() < current.getOrder()) throw new BusinessException("不允许回退阶段");
    return target;
}
```

**Step 4: 重跑测试**

Run: `mvn -pl xiaou-mock-interview -am -Dtest=CareerLoopStateMachineTest test`  
Expected: PASS

**Step 5: Commit**

```bash
git add xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/CareerLoopStateMachine.java xiaou-mock-interview/src/test/java/com/xiaou/mockinterview/service/CareerLoopStateMachineTest.java
git commit -m "feat(career-loop): add transition state machine with tests"
```

---

### Task 3: 持久层与服务层骨架（会话/时间线/动作）

**Files:**
- Create: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/mapper/CareerLoopSessionMapper.java`
- Create: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/mapper/CareerLoopStageLogMapper.java`
- Create: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/mapper/CareerLoopActionMapper.java`
- Create: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/mapper/CareerLoopSnapshotMapper.java`
- Create: `xiaou-mock-interview/src/main/resources/mapper/CareerLoopSessionMapper.xml`
- Create: `xiaou-mock-interview/src/main/resources/mapper/CareerLoopStageLogMapper.xml`
- Create: `xiaou-mock-interview/src/main/resources/mapper/CareerLoopActionMapper.xml`
- Create: `xiaou-mock-interview/src/main/resources/mapper/CareerLoopSnapshotMapper.xml`
- Create: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/CareerLoopService.java`
- Create: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/CareerLoopServiceImpl.java`
- Create: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/dto/request/CareerLoopStartRequest.java`
- Create: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/dto/request/CareerLoopEventRequest.java`
- Create: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/dto/response/CareerLoopCurrentResponse.java`
- Test: `xiaou-mock-interview/src/test/java/com/xiaou/mockinterview/service/impl/CareerLoopServiceImplTest.java`

**Step 1: 写失败测试（首次启动会话）**

```java
@Test
void start_should_create_active_session() {
    CareerLoopSession session = service.start(1001L, new CareerLoopStartRequest().setTargetRole("Java后端"));
    assertNotNull(session.getId());
    assertEquals(CareerLoopStageEnum.INIT.name(), session.getCurrentStage());
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-mock-interview -am -Dtest=CareerLoopServiceImplTest test`  
Expected: FAIL

**Step 3: 写最小实现**

```java
@Transactional
public CareerLoopSession start(Long userId, CareerLoopStartRequest request) {
    CareerLoopSession active = sessionMapper.selectActiveByUserId(userId);
    if (active != null) return active;
    CareerLoopSession created = new CareerLoopSession()
        .setUserId(userId)
        .setCurrentStage(CareerLoopStageEnum.INIT.name())
        .setStatus("active");
    sessionMapper.insert(created);
    return created;
}
```

**Step 4: 重跑测试 + 编译**

Run: `mvn -pl xiaou-mock-interview -am -Dtest=CareerLoopServiceImplTest test`  
Expected: PASS  

Run: `mvn -pl xiaou-mock-interview -am -DskipTests compile`  
Expected: BUILD SUCCESS

**Step 5: Commit**

```bash
git add xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/mapper xiaou-mock-interview/src/main/resources/mapper/CareerLoop*.xml xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/CareerLoopService.java xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/CareerLoopServiceImpl.java xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/dto/request/CareerLoopStartRequest.java xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/dto/request/CareerLoopEventRequest.java xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/dto/response/CareerLoopCurrentResponse.java xiaou-mock-interview/src/test/java/com/xiaou/mockinterview/service/impl/CareerLoopServiceImplTest.java
git commit -m "feat(career-loop): add persistence and core service"
```

---

### Task 4: 暴露用户端 API（start/current/timeline/actions/sync/event）

**Files:**
- Create: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/controller/CareerLoopController.java`
- Modify: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/CareerLoopService.java`
- Modify: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/CareerLoopServiceImpl.java`
- Test: `xiaou-mock-interview/src/test/java/com/xiaou/mockinterview/controller/CareerLoopControllerWebTest.java`

**Step 1: 写失败 Web 测试**

```java
@WebMvcTest(CareerLoopController.class)
class CareerLoopControllerWebTest {
    @Test
    void should_expose_current_endpoint() throws Exception {
        mockMvc.perform(get("/user/career-loop/current"))
            .andExpect(status().isOk());
    }
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-mock-interview -am -Dtest=CareerLoopControllerWebTest test`  
Expected: FAIL

**Step 3: 写最小实现**

```java
@RestController
@RequestMapping("/user/career-loop")
public class CareerLoopController {
    @GetMapping("/current")
    public Result<CareerLoopCurrentResponse> current() { ... }
}
```

**Step 4: 重跑测试**

Run: `mvn -pl xiaou-mock-interview -am -Dtest=CareerLoopControllerWebTest test`  
Expected: PASS

**Step 5: Commit**

```bash
git add xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/controller/CareerLoopController.java xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/CareerLoopService.java xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/CareerLoopServiceImpl.java xiaou-mock-interview/src/test/java/com/xiaou/mockinterview/controller/CareerLoopControllerWebTest.java
git commit -m "feat(career-loop): expose user APIs for loop center"
```

---

### Task 5: 接入 Job Battle 事件推进（同模块自动联动）

**Files:**
- Modify: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/JobBattleService.java`
- Modify: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/JobBattleServiceImpl.java`
- Modify: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/controller/JobBattleController.java`
- Test: `xiaou-mock-interview/src/test/java/com/xiaou/mockinterview/service/impl/JobBattleCareerLoopIntegrationTest.java`

**Step 1: 写失败测试（生成计划后推进到 PLAN_READY）**

```java
@Test
void generate_plan_should_push_loop_to_plan_ready() {
    jobBattleService.generatePlan(userId, request);
    verify(careerLoopService).onEvent(eq(userId), argThat(e -> "PLAN_READY".equals(e.getTargetStage())));
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-mock-interview -am -Dtest=JobBattleCareerLoopIntegrationTest test`  
Expected: FAIL

**Step 3: 写最小实现**

```java
careerLoopService.onEvent(userId, new CareerLoopEventRequest()
    .setSource("job_battle")
    .setTargetStage("PLAN_READY")
    .setRefId(recordId));
```

**Step 4: 重跑测试**

Run: `mvn -pl xiaou-mock-interview -am -Dtest=JobBattleCareerLoopIntegrationTest test`  
Expected: PASS

**Step 5: Commit**

```bash
git add xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/JobBattleService.java xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/JobBattleServiceImpl.java xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/controller/JobBattleController.java xiaou-mock-interview/src/test/java/com/xiaou/mockinterview/service/impl/JobBattleCareerLoopIntegrationTest.java
git commit -m "feat(career-loop): integrate job battle stage events"
```

---

### Task 6: 接入模拟面试与复盘事件推进

**Files:**
- Modify: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/MockInterviewService.java`
- Modify: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/MockInterviewServiceImpl.java`
- Modify: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/controller/MockInterviewController.java`
- Test: `xiaou-mock-interview/src/test/java/com/xiaou/mockinterview/service/impl/MockInterviewCareerLoopIntegrationTest.java`

**Step 1: 写失败测试（完成面试后推进 INTERVIEW_DONE）**

```java
@Test
void complete_interview_should_push_loop_to_interview_done() {
    service.submitAnswer(userId, request);
    verify(careerLoopService).onEvent(eq(userId), argThat(e -> "INTERVIEW_DONE".equals(e.getTargetStage())));
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-mock-interview -am -Dtest=MockInterviewCareerLoopIntegrationTest test`  
Expected: FAIL

**Step 3: 写最小实现**

```java
careerLoopService.onEvent(userId, new CareerLoopEventRequest()
    .setSource("mock_interview")
    .setTargetStage("INTERVIEW_DONE")
    .setRefId(sessionId));
```

**Step 4: 重跑测试**

Run: `mvn -pl xiaou-mock-interview -am -Dtest=MockInterviewCareerLoopIntegrationTest test`  
Expected: PASS

**Step 5: Commit**

```bash
git add xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/MockInterviewService.java xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/MockInterviewServiceImpl.java xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/controller/MockInterviewController.java xiaou-mock-interview/src/test/java/com/xiaou/mockinterview/service/impl/MockInterviewCareerLoopIntegrationTest.java
git commit -m "feat(career-loop): integrate mock interview stage events"
```

---

### Task 7: 用户端新增闭环中台页面与路由

**Files:**
- Create: `vue3-user-front/src/api/careerLoop.js`
- Create: `vue3-user-front/src/views/career-loop/Index.vue`
- Modify: `vue3-user-front/src/router/index.js`
- Modify: `vue3-user-front/src/App.vue`
- Test: `vue3-user-front/tests/career-loop-adapter.test.js`

**Step 1: 写失败测试（前端适配器状态映射）**

```js
test('mapStageLabel should map PLAN_READY to 待执行计划', () => {
  assert.equal(mapStageLabel('PLAN_READY'), '待执行计划')
})
```

**Step 2: 运行测试确认失败**

Run: `cd vue3-user-front && node --test tests/career-loop-adapter.test.js`  
Expected: FAIL

**Step 3: 写最小实现 + 页面接入**

```js
export const careerLoopApi = {
  getCurrent: () => request.get('/user/career-loop/current'),
  getTimeline: () => request.get('/user/career-loop/timeline'),
  getActions: () => request.get('/user/career-loop/actions')
}
```

**Step 4: 重跑测试与构建**

Run: `cd vue3-user-front && node --test tests/career-loop-adapter.test.js`  
Expected: PASS  

Run: `cd vue3-user-front && npm run build`  
Expected: 构建成功

**Step 5: Commit**

```bash
git add vue3-user-front/src/api/careerLoop.js vue3-user-front/src/views/career-loop/Index.vue vue3-user-front/src/router/index.js vue3-user-front/src/App.vue vue3-user-front/tests/career-loop-adapter.test.js
git commit -m "feat(user): add career loop center page and routes"
```

---

### Task 8: Job Battle 页面接入闭环入口与手动同步

**Files:**
- Modify: `vue3-user-front/src/views/job-battle/Index.vue`
- Modify: `vue3-user-front/src/api/careerLoop.js`

**Step 1: 写失败交互测试（或最小手工回归脚本）**

```bash
# 手工验证脚本（记录预期）
# 1. 生成计划后点击“同步到闭环”应提示成功
# 2. 点击“查看闭环进度”跳转 /career-loop
```

**Step 2: 运行验证确认当前不满足**

Run: 手工验证 `http://localhost:3001/job-battle`  
Expected: 无闭环入口或不能同步

**Step 3: 写最小实现**

```js
await careerLoopApi.sync()
router.push('/career-loop')
```

**Step 4: 重跑前端构建**

Run: `cd vue3-user-front && npm run build`  
Expected: 构建成功

**Step 5: Commit**

```bash
git add vue3-user-front/src/views/job-battle/Index.vue vue3-user-front/src/api/careerLoop.js
git commit -m "feat(job-battle): link to career loop center and sync action"
```

---

### Task 9: 全链路验收与文档更新

**Files:**
- Modify: `README.md`
- Modify: `docs/plans/2026-03-01-career-loop-platform-design.md`
- Modify: `docs/plans/2026-03-01-career-loop-platform.md`（如执行中调整）

**Step 1: 后端测试与编译验收**

Run: `mvn -pl xiaou-mock-interview -am test`  
Expected: PASS  

Run: `mvn -pl xiaou-application -am -DskipTests compile`  
Expected: BUILD SUCCESS

**Step 2: 前端构建验收**

Run: `cd vue3-user-front && npm run build`  
Expected: SUCCESS（允许非阻塞 warning）

**Step 3: 手工回归清单**

- Job Battle 生成计划后，中台可看到阶段推进到 `PLAN_READY`
- 完成模拟面试后，可推进到 `INTERVIEW_DONE`
- 完成复盘后，可推进到 `REVIEWED`
- 闭环页动作清单可查看、可完成、可跳转
- 模块异常时闭环页不白屏，显示“待同步”

**Step 4: 文档补齐**

- README 增加“求职闭环中台”简介、入口和 API 概览
- 记录 v1.8.x 新增能力与迁移脚本执行说明

**Step 5: Commit**

```bash
git add README.md docs/plans/2026-03-01-career-loop-platform-design.md docs/plans/2026-03-01-career-loop-platform.md
git commit -m "docs(career-loop): add rollout notes and verification checklist"
```

---

## 执行顺序建议

1. Task 1（数据模型与 SQL）
2. Task 2（状态机）
3. Task 3（服务层）
4. Task 4（API）
5. Task 5（Job Battle 联动）
6. Task 6（模拟面试联动）
7. Task 7（中台前端页面）
8. Task 8（Job Battle UI 接入）
9. Task 9（验收与文档）

## 里程碑建议

- Day 1：Task 1-3  
- Day 2：Task 4-6  
- Day 3：Task 7-9

