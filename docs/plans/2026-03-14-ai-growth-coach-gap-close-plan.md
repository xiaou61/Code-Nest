# AI成长教练缺口收口 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 补齐 AI成长教练一期真实缺口，让当前实现从“主链路可用”提升到“功能可交付”。

**Architecture:** 保持现有实现主体位于 `xiaou-application/com.xiaou.aigrowth`，继续复用 `xiaou-ai` 里的 Coze 工作流调用层。优先补齐配置生效、推荐资源输出、失败案例观测，以及管理端可见入口，不额外扩张到行动重排工作流。

**Tech Stack:** Spring Boot 3、MyBatis XML、Maven 多模块、Vue3、Element Plus、Vite。

---

### Task 1: 让场景开关配置真正生效

**Files:**
- Modify: `xiaou-application/src/test/java/com/xiaou/aigrowth/service/AiGrowthCoachServiceImplTest.java`
- Modify: `xiaou-application/src/main/java/com/xiaou/aigrowth/service/impl/AiGrowthCoachServiceImpl.java`

**Step 1: Write the failing test**

- 为 `scene_enabled_learning/career/hybrid` 增加关闭场景时的断言。

**Step 2: Run test to verify it fails**

Run: `mvn -pl xiaou-application -am "-Dtest=AiGrowthCoachServiceImplTest" test`
Expected: FAIL，当前未读取场景开关。

**Step 3: Write minimal implementation**

- 在 `getSummary` / `refresh` 前校验场景是否启用。
- 关闭时抛出明确业务异常。

**Step 4: Run test to verify it passes**

Run: `mvn -pl xiaou-application -am "-Dtest=AiGrowthCoachServiceImplTest" test`
Expected: PASS

### Task 2: 补齐推荐资源输出

**Files:**
- Modify: `xiaou-application/src/test/java/com/xiaou/aigrowth/service/AiGrowthCoachServiceImplTest.java`
- Modify: `xiaou-application/src/main/java/com/xiaou/aigrowth/dto/response/AiGrowthCoachSnapshotDetailResponse.java`
- Create: `xiaou-application/src/main/java/com/xiaou/aigrowth/dto/response/AiGrowthCoachResourceResponse.java`
- Modify: `xiaou-application/src/main/java/com/xiaou/aigrowth/service/impl/AiGrowthCoachServiceImpl.java`
- Modify: `vue3-user-front/src/components/ai-growth-coach/CoachWorkspace.vue`

**Step 1: Write the failing test**

- 断言刷新后的详情里包含推荐资源列表。

**Step 2: Run test to verify it fails**

Run: `mvn -pl xiaou-application -am "-Dtest=AiGrowthCoachServiceImplTest" test`
Expected: FAIL

**Step 3: Write minimal implementation**

- 后端根据学习资产、博客、社区、CodePen 生成资源候选列表。
- 快照详情响应新增 `resources`。
- 用户端在教练组件中渲染推荐资源区。

**Step 4: Run test to verify it passes**

Run: `mvn -pl xiaou-application -am "-Dtest=AiGrowthCoachServiceImplTest" test`
Expected: PASS

### Task 3: 补齐失败案例观测接口与管理端页面

**Files:**
- Modify: `xiaou-application/src/test/java/com/xiaou/aigrowth/service/AiGrowthCoachServiceImplTest.java`
- Modify: `xiaou-application/src/main/java/com/xiaou/aigrowth/service/AiGrowthCoachService.java`
- Create: `xiaou-application/src/main/java/com/xiaou/aigrowth/dto/response/AiGrowthCoachFailureResponse.java`
- Modify: `xiaou-application/src/main/java/com/xiaou/aigrowth/mapper/AiGrowthCoachSnapshotMapper.java`
- Modify: `xiaou-application/src/main/resources/mapper/aigrowth/AiGrowthCoachSnapshotMapper.xml`
- Modify: `xiaou-application/src/main/java/com/xiaou/aigrowth/service/impl/AiGrowthCoachServiceImpl.java`
- Modify: `xiaou-application/src/main/java/com/xiaou/aigrowth/controller/admin/AdminAiGrowthCoachController.java`
- Modify: `vue3-admin-front/src/api/aiGrowthCoach.js`
- Create: `vue3-admin-front/src/views/ai-growth-coach/failures/index.vue`
- Modify: `vue3-admin-front/src/router/index.js`
- Modify: `vue3-admin-front/src/layout/index.vue`

**Step 1: Write the failing test**

- 断言服务层可以返回失败快照列表。

**Step 2: Run test to verify it fails**

Run: `mvn -pl xiaou-application -am "-Dtest=AiGrowthCoachServiceImplTest" test`
Expected: FAIL

**Step 3: Write minimal implementation**

- Mapper 增加失败快照列表查询。
- 管理端接口新增 `/admin/ai-growth-coach/failures`。
- 管理端新增失败案例列表页，并在侧边菜单挂出入口。

**Step 4: Run test to verify it passes**

Run: `mvn -pl xiaou-application -am "-Dtest=AiGrowthCoachServiceImplTest" test`
Expected: PASS

### Task 4: 完整验证

**Files:**
- Modify: `docs/plans/2026-03-14-ai-growth-coach-design.md`

**Step 1: Run backend tests**

Run: `cmd /c "mvn -pl xiaou-application -am -Dtest=AiGrowthCoachSchemaSqlTest,AiGrowthCoachServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test"`
Expected: PASS

**Step 2: Run frontend builds**

Run: `cd vue3-user-front && npm run build`
Expected: PASS

Run: `cd vue3-admin-front && npm run build`
Expected: PASS

**Step 3: Update design note**

- 在设计文档中补充已收口项。
