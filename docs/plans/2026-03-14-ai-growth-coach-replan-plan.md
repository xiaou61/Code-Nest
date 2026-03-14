# AI成长教练时间压缩重排 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 AI成长教练补齐“时间压缩重排”能力，让用户基于当前快照预算模拟一版临时行动清单。

**Architecture:** 保持当前 `xiaou-application/com.xiaou.aigrowth` 作为业务承载层，继续复用 `xiaou-ai` 中的 Coze 工作流适配。后端新增一个不落库的 `replan` 接口，规则层先产出最小可用结果，再由 `GROWTH_COACH_ACTION_REPLAN` 生成更自然的摘要和推荐追问；前端在 `CoachWorkspace` 内以弹层方式承载时间预算输入和结果展示。

**Tech Stack:** Spring Boot 3、MyBatis、Maven 多模块、Vue3、Element Plus、Vite。

---

### Task 1: 为时间压缩重排写失败测试

**Files:**
- Modify: `xiaou-application/src/test/java/com/xiaou/aigrowth/service/AiGrowthCoachServiceImplTest.java`

**Step 1: Write the failing test**

- 增加 `replanShouldReturnCompressedActionsWhenWorkflowUnavailable`
- 增加 `replanShouldRejectForeignSnapshot`

**Step 2: Run test to verify it fails**

Run: `cmd /c "mvn -pl xiaou-application -am -Dtest=AiGrowthCoachServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test"`
Expected: FAIL，提示 `replan` 方法或相关 DTO 不存在。

**Step 3: Write minimal implementation**

- 仅写到让测试开始编译的最小接口和 DTO。

**Step 4: Run test to verify it still fails for expected reason**

Run: `cmd /c "mvn -pl xiaou-application -am -Dtest=AiGrowthCoachServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test"`
Expected: FAIL，且失败点落在行为断言上，而不是编译错误。

### Task 2: 接通后端 replan 能力

**Files:**
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/dto/growthcoach/GrowthCoachActionReplanRequest.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/dto/growthcoach/GrowthCoachActionReplanResult.java`
- Modify: `xiaou-ai/src/main/java/com/xiaou/ai/service/AiGrowthCoachWorkflowService.java`
- Modify: `xiaou-ai/src/main/java/com/xiaou/ai/service/impl/AiGrowthCoachWorkflowServiceImpl.java`
- Create: `xiaou-application/src/main/java/com/xiaou/aigrowth/dto/request/AiGrowthCoachReplanRequest.java`
- Create: `xiaou-application/src/main/java/com/xiaou/aigrowth/dto/response/AiGrowthCoachReplanResponse.java`
- Modify: `xiaou-application/src/main/java/com/xiaou/aigrowth/service/AiGrowthCoachService.java`
- Modify: `xiaou-application/src/main/java/com/xiaou/aigrowth/controller/user/UserAiGrowthCoachController.java`
- Modify: `xiaou-application/src/main/java/com/xiaou/aigrowth/service/impl/AiGrowthCoachRuleEngine.java`
- Modify: `xiaou-application/src/main/java/com/xiaou/aigrowth/service/impl/AiGrowthCoachServiceImpl.java`

**Step 1: Implement minimal DTOs and service method**

- `replan(Long userId, AiGrowthCoachReplanRequest request)`
- 请求字段：`snapshotId`、`scene`、`availableMinutes`

**Step 2: 实现规则重排**

- 读取当前快照和动作列表
- 按 `P0 -> P1 -> P2` 和 `estimatedMinutes` 做预算裁剪
- 输出 `actions`、`deferredActions`、`summary`

**Step 3: 接入 Coze 工作流增强**

- 调用 `GROWTH_COACH_ACTION_REPLAN`
- 若失败则保留规则摘要并标记 `fallbackOnly`

**Step 4: Run tests**

Run: `cmd /c "mvn -pl xiaou-application -am -Dtest=AiGrowthCoachServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test"`
Expected: PASS

### Task 3: 接入用户端 UI

**Files:**
- Modify: `vue3-user-front/src/api/aiGrowthCoach.js`
- Modify: `vue3-user-front/src/components/ai-growth-coach/CoachWorkspace.vue`

**Step 1: 增加前端 API**

- 新增 `replan(data)` 调 `POST /user/ai-growth-coach/replan`

**Step 2: 增加时间压缩重排交互**

- 在行动清单区加按钮
- 用 `el-dialog` 或 `el-drawer` 输入可用分钟数
- 展示重排摘要、保留动作、延后动作

**Step 3: Run frontend build**

Run: `cmd /c "npm run build"` in `vue3-user-front`
Expected: PASS

### Task 4: 全量验证

**Files:**
- Modify: `docs/plans/2026-03-14-ai-growth-coach-design.md`

**Step 1: Run backend tests**

Run: `cmd /c "mvn -pl xiaou-application -am -Dtest=AiGrowthCoachSchemaSqlTest,AiGrowthCoachServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test"`
Expected: PASS

**Step 2: Run frontend build**

Run: `cmd /c "npm run build"` in `vue3-user-front`
Expected: PASS

**Step 3: 收口说明**

- 记录本次能力为“临时预览重排，不落库”。
