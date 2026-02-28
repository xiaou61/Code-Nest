# OJ 赛事系统（周赛/挑战赛）Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在现有 OJ 判题能力上新增完整赛事闭环（赛事创建、报名、参赛、榜单、管理端运营），支持用户参加周赛/挑战赛并在统一榜单中竞争。

**Architecture:** 采用“赛事域扩展 + 复用现有判题链路”方案。后端在 `xiaou-oj` 增加赛事实体、报名关系、榜单计算服务，并在提交记录中引入 `contestId` 以复用既有判题服务；前端分为用户端参赛页面与管理端赛事运营页面，和现有 OJ 路由/菜单无缝衔接。

**Tech Stack:** Spring Boot 3 + MyBatis XML + Sa-Token + Vue3 + Element Plus + Pinia + Node `node:test` + Maven 多模块构建。

---

## 约束与范围

- 不引入 mock，所有赛事数据均来自真实接口。
- 不改现有 `/oj/submit` 判题核心流程，只做可选赛事上下文扩展。
- MVP 采用 ACM 榜单规则：`解题数 DESC`，`罚时 ASC`，`最后一次 AC 时间 ASC`。
- 本期不做 Rating/ELO（放到 Phase 2）。

## 执行规范（强制）

- 研发流程按 `@test-driven-development` 执行：先写失败测试，再写最小实现，再回归。
- 完成声明前按 `@verification-before-completion` 执行构建与测试验证。
- 每个任务结束做一次小提交，避免大批量混改。

---

### Task 1: 建立赛事规则测试基座

**Files:**
- Modify: `xiaou-oj/pom.xml`
- Create: `xiaou-oj/src/main/java/com/xiaou/oj/contest/ContestRuleValidator.java`
- Test: `xiaou-oj/src/test/java/com/xiaou/oj/contest/ContestRuleValidatorTest.java`

**Step 1: 写失败测试（时间窗口 + 状态校验）**

```java
@Test
void should_reject_submit_when_contest_not_running() {
    OjContest contest = new OjContest().setStatus(1) // upcoming
        .setStartTime(LocalDateTime.now().plusHours(1))
        .setEndTime(LocalDateTime.now().plusHours(3));
    assertThrows(BusinessException.class,
        () -> ContestRuleValidator.checkCanSubmit(contest, LocalDateTime.now()));
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-oj -am -Dtest=ContestRuleValidatorTest test`
Expected: FAIL，提示 `ContestRuleValidator` 或 `checkCanSubmit` 未实现。

**Step 3: 写最小实现**

```java
public static void checkCanSubmit(OjContest contest, LocalDateTime now) {
    if (contest == null || contest.getStatus() == null || contest.getStatus() != 2) {
        throw new BusinessException("赛事未开始");
    }
    if (now.isBefore(contest.getStartTime()) || now.isAfter(contest.getEndTime())) {
        throw new BusinessException("不在赛事提交时间窗口内");
    }
}
```

**Step 4: 重跑测试确认通过**

Run: `mvn -pl xiaou-oj -am -Dtest=ContestRuleValidatorTest test`
Expected: PASS。

**Step 5: Commit**

```bash
git add xiaou-oj/pom.xml xiaou-oj/src/main/java/com/xiaou/oj/contest/ContestRuleValidator.java xiaou-oj/src/test/java/com/xiaou/oj/contest/ContestRuleValidatorTest.java
git commit -m "test(oj): add contest rule validator with TDD"
```

---

### Task 2: 落库脚本与提交记录扩展

**Files:**
- Create: `sql/v1.8.1/oj_contest.sql`
- Modify: `xiaou-oj/src/main/java/com/xiaou/oj/domain/OjSubmission.java`
- Modify: `xiaou-oj/src/main/java/com/xiaou/oj/mapper/OjSubmissionMapper.java`
- Modify: `xiaou-oj/src/main/java/com/xiaou/oj/mapper/OjSubmissionMapper.xml`
- Test: `xiaou-oj/src/test/java/com/xiaou/oj/sql/OjContestSchemaSqlTest.java`

**Step 1: 写失败测试（校验 SQL 脚本关键 DDL）**

```java
@Test
void contest_sql_should_define_required_tables() {
    String sql = Files.readString(Path.of("sql/v1.8.1/oj_contest.sql"));
    assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `oj_contest`"));
    assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `oj_contest_problem`"));
    assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `oj_contest_participant`"));
    assertTrue(sql.contains("ALTER TABLE `oj_submission` ADD COLUMN `contest_id`"));
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-oj -am -Dtest=OjContestSchemaSqlTest test`
Expected: FAIL（脚本不存在）。

**Step 3: 写 SQL 与提交记录字段扩展**

- 新增赛事主表、赛事题目关联表、赛事参赛者表。
- `oj_submission` 增加 `contest_id BIGINT NULL` 与索引 `idx_contest_id`。
- `OjSubmission` 增加 `contestId` 字段；Mapper insert/select/selectPage 补齐映射。

**Step 4: 重跑测试与编译验证**

Run: `mvn -pl xiaou-oj -am -Dtest=OjContestSchemaSqlTest test`
Expected: PASS。

Run: `mvn -pl xiaou-oj -am -DskipTests compile`
Expected: BUILD SUCCESS。

**Step 5: Commit**

```bash
git add sql/v1.8.1/oj_contest.sql xiaou-oj/src/main/java/com/xiaou/oj/domain/OjSubmission.java xiaou-oj/src/main/java/com/xiaou/oj/mapper/OjSubmissionMapper.java xiaou-oj/src/main/java/com/xiaou/oj/mapper/OjSubmissionMapper.xml xiaou-oj/src/test/java/com/xiaou/oj/sql/OjContestSchemaSqlTest.java
git commit -m "feat(oj): add contest schema and submission contest field"
```

---

### Task 3: 赛事管理端服务（创建/更新/上下线/题目编排）

**Files:**
- Create: `xiaou-oj/src/main/java/com/xiaou/oj/domain/OjContest.java`
- Create: `xiaou-oj/src/main/java/com/xiaou/oj/domain/OjContestProblem.java`
- Create: `xiaou-oj/src/main/java/com/xiaou/oj/dto/contest/ContestSaveRequest.java`
- Create: `xiaou-oj/src/main/java/com/xiaou/oj/dto/contest/ContestQueryRequest.java`
- Create: `xiaou-oj/src/main/java/com/xiaou/oj/mapper/OjContestMapper.java`
- Create: `xiaou-oj/src/main/java/com/xiaou/oj/mapper/OjContestMapper.xml`
- Create: `xiaou-oj/src/main/java/com/xiaou/oj/mapper/OjContestProblemMapper.java`
- Create: `xiaou-oj/src/main/java/com/xiaou/oj/mapper/OjContestProblemMapper.xml`
- Create: `xiaou-oj/src/main/java/com/xiaou/oj/service/OjContestService.java`
- Create: `xiaou-oj/src/main/java/com/xiaou/oj/service/impl/OjContestServiceImpl.java`
- Test: `xiaou-oj/src/test/java/com/xiaou/oj/service/impl/OjContestServiceImplTest.java`

**Step 1: 写失败测试（创建赛事时的时间合法性与状态流转）**

```java
@Test
void create_contest_should_reject_when_end_before_start() {
    ContestSaveRequest req = new ContestSaveRequest();
    req.setStartTime(LocalDateTime.now().plusDays(1));
    req.setEndTime(LocalDateTime.now());
    assertThrows(BusinessException.class, () -> service.createContest(1L, req));
}
```

**Step 2: 跑测试确认失败**

Run: `mvn -pl xiaou-oj -am -Dtest=OjContestServiceImplTest test`
Expected: FAIL。

**Step 3: 实现最小服务与 Mapper**

```java
@Transactional
public Long createContest(Long adminId, ContestSaveRequest req) {
    validateContestWindow(req);
    OjContest contest = convert(req);
    contest.setStatus(0); // draft
    contest.setCreatedBy(adminId);
    contestMapper.insert(contest);
    contestProblemMapper.replaceProblems(contest.getId(), req.getProblemIds());
    return contest.getId();
}
```

**Step 4: 测试通过 + 编译**

Run: `mvn -pl xiaou-oj -am -Dtest=OjContestServiceImplTest test`
Expected: PASS。

Run: `mvn -pl xiaou-oj -am -DskipTests compile`
Expected: BUILD SUCCESS。

**Step 5: Commit**

```bash
git add xiaou-oj/src/main/java/com/xiaou/oj/domain/OjContest.java xiaou-oj/src/main/java/com/xiaou/oj/domain/OjContestProblem.java xiaou-oj/src/main/java/com/xiaou/oj/dto/contest/ContestSaveRequest.java xiaou-oj/src/main/java/com/xiaou/oj/dto/contest/ContestQueryRequest.java xiaou-oj/src/main/java/com/xiaou/oj/mapper/OjContestMapper.java xiaou-oj/src/main/java/com/xiaou/oj/mapper/OjContestMapper.xml xiaou-oj/src/main/java/com/xiaou/oj/mapper/OjContestProblemMapper.java xiaou-oj/src/main/java/com/xiaou/oj/mapper/OjContestProblemMapper.xml xiaou-oj/src/main/java/com/xiaou/oj/service/OjContestService.java xiaou-oj/src/main/java/com/xiaou/oj/service/impl/OjContestServiceImpl.java xiaou-oj/src/test/java/com/xiaou/oj/service/impl/OjContestServiceImplTest.java
git commit -m "feat(oj): implement admin contest service and persistence"
```

---

### Task 4: 用户报名与参赛资格校验

**Files:**
- Create: `xiaou-oj/src/main/java/com/xiaou/oj/domain/OjContestParticipant.java`
- Create: `xiaou-oj/src/main/java/com/xiaou/oj/mapper/OjContestParticipantMapper.java`
- Create: `xiaou-oj/src/main/java/com/xiaou/oj/mapper/OjContestParticipantMapper.xml`
- Modify: `xiaou-oj/src/main/java/com/xiaou/oj/dto/SubmitCodeRequest.java`
- Modify: `xiaou-oj/src/main/java/com/xiaou/oj/dto/SubmissionQueryRequest.java`
- Modify: `xiaou-oj/src/main/java/com/xiaou/oj/service/impl/OjSubmissionServiceImpl.java`
- Test: `xiaou-oj/src/test/java/com/xiaou/oj/service/impl/OjSubmissionServiceContestTest.java`

**Step 1: 写失败测试（赛事未报名不得提交）**

```java
@Test
void submit_should_fail_when_user_not_joined_contest() {
    SubmitCodeRequest req = new SubmitCodeRequest();
    req.setContestId(101L);
    req.setProblemId(1L);
    req.setLanguage("java");
    req.setCode("class Main{} ");
    assertThrows(BusinessException.class, () -> service.submitCode(1001L, req));
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-oj -am -Dtest=OjSubmissionServiceContestTest test`
Expected: FAIL。

**Step 3: 最小实现（报名 + 提交校验）**

- `SubmitCodeRequest` 增加 `contestId`（可空）。
- `submitCode` 中：若 `contestId != null`，依次校验赛事状态、时间窗口、用户是否报名、题目是否属于赛事。
- 插入 `oj_submission` 时写入 `contestId`。

**Step 4: 测试通过**

Run: `mvn -pl xiaou-oj -am -Dtest=OjSubmissionServiceContestTest test`
Expected: PASS。

**Step 5: Commit**

```bash
git add xiaou-oj/src/main/java/com/xiaou/oj/domain/OjContestParticipant.java xiaou-oj/src/main/java/com/xiaou/oj/mapper/OjContestParticipantMapper.java xiaou-oj/src/main/java/com/xiaou/oj/mapper/OjContestParticipantMapper.xml xiaou-oj/src/main/java/com/xiaou/oj/dto/SubmitCodeRequest.java xiaou-oj/src/main/java/com/xiaou/oj/dto/SubmissionQueryRequest.java xiaou-oj/src/main/java/com/xiaou/oj/service/impl/OjSubmissionServiceImpl.java xiaou-oj/src/test/java/com/xiaou/oj/service/impl/OjSubmissionServiceContestTest.java
git commit -m "feat(oj): add contest participant validation for submissions"
```

---

### Task 5: 榜单计算服务（ACM 规则）

**Files:**
- Create: `xiaou-oj/src/main/java/com/xiaou/oj/dto/contest/ContestRankingItem.java`
- Create: `xiaou-oj/src/main/java/com/xiaou/oj/service/OjContestRankingService.java`
- Create: `xiaou-oj/src/main/java/com/xiaou/oj/service/impl/OjContestRankingServiceImpl.java`
- Create: `xiaou-oj/src/main/java/com/xiaou/oj/contest/ContestRankingCalculator.java`
- Test: `xiaou-oj/src/test/java/com/xiaou/oj/contest/ContestRankingCalculatorTest.java`

**Step 1: 写失败测试（解题数、罚时、并列规则）**

```java
@Test
void ranking_should_sort_by_solved_then_penalty_then_lastAcTime() {
    List<ContestRankingItem> items = calculator.calculate(contest, submissions, participants);
    assertEquals(1002L, items.get(0).getUserId());
    assertEquals(2, items.get(0).getSolvedCount());
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-oj -am -Dtest=ContestRankingCalculatorTest test`
Expected: FAIL。

**Step 3: 实现最小计算逻辑**

```java
// 每题只计首次AC；AC前每次WA罚时20分钟
penalty += acMinutes + wrongBeforeAc * 20;
```

**Step 4: 测试通过**

Run: `mvn -pl xiaou-oj -am -Dtest=ContestRankingCalculatorTest test`
Expected: PASS。

**Step 5: Commit**

```bash
git add xiaou-oj/src/main/java/com/xiaou/oj/dto/contest/ContestRankingItem.java xiaou-oj/src/main/java/com/xiaou/oj/service/OjContestRankingService.java xiaou-oj/src/main/java/com/xiaou/oj/service/impl/OjContestRankingServiceImpl.java xiaou-oj/src/main/java/com/xiaou/oj/contest/ContestRankingCalculator.java xiaou-oj/src/test/java/com/xiaou/oj/contest/ContestRankingCalculatorTest.java
git commit -m "feat(oj): implement contest ranking calculator"
```

---

### Task 6: 暴露赛事 API（用户端 + 管理端）

**Files:**
- Create: `xiaou-oj/src/main/java/com/xiaou/oj/controller/admin/OjContestController.java`
- Create: `xiaou-oj/src/main/java/com/xiaou/oj/controller/pub/OjContestPublicController.java`
- Modify: `xiaou-oj/src/main/java/com/xiaou/oj/controller/pub/OjSubmissionController.java`
- Test: `xiaou-oj/src/test/java/com/xiaou/oj/controller/OjContestControllerWebTest.java`

**Step 1: 写失败的 Web 层测试（核心路由）**

```java
@WebMvcTest(OjContestPublicController.class)
class OjContestControllerWebTest {
    @Test
    void should_expose_contest_ranking_endpoint() throws Exception {
        mockMvc.perform(get("/oj/contests/1/ranking")).andExpect(status().isOk());
    }
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-oj -am -Dtest=OjContestControllerWebTest test`
Expected: FAIL。

**Step 3: 实现控制器接口**

- 管理端：`/admin/oj/contests`（增删改查、编排题目、状态变更）。
- 用户端：`/oj/contests/list`、`/oj/contests/{id}`、`/oj/contests/{id}/join`、`/oj/contests/{id}/ranking`。

**Step 4: 重跑测试与模块编译**

Run: `mvn -pl xiaou-oj -am -Dtest=OjContestControllerWebTest test`
Expected: PASS。

Run: `mvn -pl xiaou-application -am -DskipTests compile`
Expected: BUILD SUCCESS。

**Step 5: Commit**

```bash
git add xiaou-oj/src/main/java/com/xiaou/oj/controller/admin/OjContestController.java xiaou-oj/src/main/java/com/xiaou/oj/controller/pub/OjContestPublicController.java xiaou-oj/src/main/java/com/xiaou/oj/controller/pub/OjSubmissionController.java xiaou-oj/src/test/java/com/xiaou/oj/controller/OjContestControllerWebTest.java
git commit -m "feat(oj): expose contest APIs for admin and user"
```

---

### Task 7: 用户端参赛页面（列表/详情/榜单）

**Files:**
- Modify: `vue3-user-front/src/api/oj.js`
- Modify: `vue3-user-front/src/router/index.js`
- Modify: `vue3-user-front/src/views/oj/Index.vue`
- Modify: `vue3-user-front/src/App.vue`
- Create: `vue3-user-front/src/views/oj/Contests.vue`
- Create: `vue3-user-front/src/views/oj/ContestDetail.vue`
- Create: `vue3-user-front/src/utils/oj-contest-adapter.js`
- Test: `vue3-user-front/tests/oj-contest-adapter.test.js`

**Step 1: 写失败测试（榜单适配器排序与字段兼容）**

```js
test('adaptContestRanking 按 solved/penalty/rank 输出', () => {
  const rows = adaptContestRanking([{ userId: 1, solvedCount: 2, penalty: 120 }])
  assert.equal(rows[0].rank, 1)
  assert.equal(rows[0].solvedText, '2 题')
})
```

**Step 2: 运行测试确认失败**

Run: `cd vue3-user-front && node --test tests/oj-contest-adapter.test.js`
Expected: FAIL。

**Step 3: 最小实现 + 页面接入**

- `oj.js` 新增赛事 API：`getContestList`/`getContestDetail`/`joinContest`/`getContestRanking`。
- 新增 `Contests.vue` 与 `ContestDetail.vue`。
- 在 `Index.vue` 快捷入口加入“赛事中心”。
- 在 `App.vue` 学习菜单加入赛事入口。

**Step 4: 重跑测试与构建**

Run: `cd vue3-user-front && node --test tests/oj-contest-adapter.test.js`
Expected: PASS。

Run: `cd vue3-user-front && npm run build`
Expected: 构建成功。

**Step 5: Commit**

```bash
git add vue3-user-front/src/api/oj.js vue3-user-front/src/router/index.js vue3-user-front/src/views/oj/Index.vue vue3-user-front/src/App.vue vue3-user-front/src/views/oj/Contests.vue vue3-user-front/src/views/oj/ContestDetail.vue vue3-user-front/src/utils/oj-contest-adapter.js vue3-user-front/tests/oj-contest-adapter.test.js
git commit -m "feat(user-oj): add contest center pages and API integration"
```

---

### Task 8: 管理端赛事运营页面

**Files:**
- Modify: `vue3-admin-front/src/api/oj.js`
- Modify: `vue3-admin-front/src/router/index.js`
- Modify: `vue3-admin-front/src/layout/index.vue`
- Create: `vue3-admin-front/src/views/oj/contests/index.vue`
- Create: `vue3-admin-front/src/views/oj/contests/edit.vue`

**Step 1: 先加路由与菜单占位（确保可访问）**

- 新增 `/oj/contests`、`/oj/contests/create`、`/oj/contests/:id/edit`。
- 侧边栏 OJ 子菜单加入“赛事管理”。

**Step 2: 新增 API 封装**

```js
getContestList(data) { return request.post('/admin/oj/contests/list', data) }
createContest(data) { return request.post('/admin/oj/contests', data) }
updateContest(id, data) { return request.put(`/admin/oj/contests/${id}`, data) }
```

**Step 3: 实现列表页与编辑页最小闭环**

- 列表：查询、状态切换、删除、跳转编辑。
- 编辑：基本信息、题目编排、保存。

**Step 4: 构建验证**

Run: `cd vue3-admin-front && npm run build`
Expected: 构建成功。

**Step 5: Commit**

```bash
git add vue3-admin-front/src/api/oj.js vue3-admin-front/src/router/index.js vue3-admin-front/src/layout/index.vue vue3-admin-front/src/views/oj/contests/index.vue vue3-admin-front/src/views/oj/contests/edit.vue
git commit -m "feat(admin-oj): add contest management pages"
```

---

### Task 9: 联调回归与文档补齐

**Files:**
- Modify: `README.md`
- Create: `docs/PRD/OJ赛事系统PRD-v1.0.0.md`
- Modify: `docs/plans/2026-02-23-oj-contest-system.md`（如执行中有调整）

**Step 1: 后端测试与编译全验收**

Run: `mvn -pl xiaou-oj -am test`
Expected: 单测通过。

Run: `mvn -pl xiaou-application -am -DskipTests package`
Expected: BUILD SUCCESS。

**Step 2: 前端双端构建验收**

Run: `cd vue3-user-front && npm run build`
Expected: success。

Run: `cd vue3-admin-front && npm run build`
Expected: success。

**Step 3: 手工联调清单**

- 管理端创建赛事 -> 编排题目 -> 发布。
- 用户端可看到赛事 -> 报名 -> 参赛提交。
- 榜单实时更新且排序规则正确。
- 非参赛者提交赛事题目被正确拦截。

**Step 4: Commit**

```bash
git add README.md docs/PRD/OJ赛事系统PRD-v1.0.0.md
git commit -m "docs(oj): add contest feature docs and verification notes"
```

---

## 执行顺序建议

1. Task 1（规则测试基座）
2. Task 2（SQL与提交记录扩展）
3. Task 3（管理端服务）
4. Task 4（报名与提交校验）
5. Task 5（榜单计算）
6. Task 6（赛事 API）
7. Task 7（用户端页面）
8. Task 8（管理端页面）
9. Task 9（联调与文档）

## 里程碑（建议）

- Day 1: Task 1 + Task 2 + Task 3
- Day 2: Task 4 + Task 5 + Task 6
- Day 3: Task 7 + Task 8
- Day 4: Task 9 + 缺陷修复

## Phase 2（可选增强）

- 赛事 Rating/ELO（新增 `oj_contest_rating`、赛后批量结算）。
- 冻结榜单（封榜时间点后仅赛后揭榜）。
- 队伍赛（多人组队与队伍榜单）。

---

## 执行记录（2026-02-23）

- 已完成：Task 8（管理端赛事运营页面）
  - `vue3-admin-front/src/api/oj.js` 新增赛事管理接口封装
  - `vue3-admin-front/src/router/index.js` 新增赛事管理路由
  - `vue3-admin-front/src/layout/index.vue` 新增侧边栏入口
  - `vue3-admin-front/src/views/oj/contests/index.vue`、`edit.vue` 完成最小闭环
- 已完成验证：
  - `cd vue3-admin-front && npm run build`
  - `cd vue3-user-front && npm run build`
  - `mvn -pl xiaou-oj -am test`
- 待继续：
  - Task 9 的后端赛事功能全链路验收（依赖赛事后端模块完整接入）
