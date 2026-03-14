# AI成长教练时间压缩重排统计 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 AI成长教练补齐“时间压缩重排”的独立日志与后台统计，帮助运营判断预算模拟能力的使用情况与效果。

**Architecture:** 在 `xiaou-application/com.xiaou.aigrowth` 领域内新增 `replan log` 数据表、Domain、Mapper 和统计聚合逻辑。用户侧 `replan` 接口在返回预览结果前记录一次日志，管理端继续复用 `/admin/ai-growth-coach/statistics`，只扩展响应字段和前端展示，不新增独立页面。

**Tech Stack:** Spring Boot 3、MyBatis XML、Maven 多模块、Vue3、Element Plus、Vite。

---

### Task 1: 为重排日志与统计字段写失败测试

**Files:**
- Modify: `xiaou-application/src/test/java/com/xiaou/aigrowth/sql/AiGrowthCoachSchemaSqlTest.java`
- Modify: `xiaou-application/src/test/java/com/xiaou/aigrowth/service/AiGrowthCoachServiceImplTest.java`

**Step 1: Write the failing test**

- 在 SQL 测试中断言 `ai_growth_coach_replan_log` 存在。
- 在服务测试中新增：
  - `replanShouldPersistPreviewLogWhenReturningResult`
  - `getStatisticsShouldIncludeReplanMetrics`

**Step 2: Run test to verify it fails**

Run: `cmd /c "mvn -pl xiaou-application -am -Dtest=AiGrowthCoachSchemaSqlTest,AiGrowthCoachServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test"`
Expected: FAIL，提示新表或新统计字段缺失。

### Task 2: 实现后端重排日志与统计聚合

**Files:**
- Modify: `sql/v1.8.5/ai_growth_coach.sql`
- Create: `xiaou-application/src/main/java/com/xiaou/aigrowth/domain/AiGrowthCoachReplanLog.java`
- Create: `xiaou-application/src/main/java/com/xiaou/aigrowth/mapper/AiGrowthCoachReplanLogMapper.java`
- Create: `xiaou-application/src/main/resources/mapper/aigrowth/AiGrowthCoachReplanLogMapper.xml`
- Modify: `xiaou-application/src/main/java/com/xiaou/aigrowth/dto/response/AiGrowthCoachStatisticsResponse.java`
- Modify: `xiaou-application/src/main/java/com/xiaou/aigrowth/service/impl/AiGrowthCoachServiceImpl.java`

**Step 1: Implement minimal schema and mapper**

- 新增重排日志表。
- 提供最小统计查询：
  - `countAll`
  - `countToday`
  - `countFallback`
  - `avgCompressionRate`
- 支持 `insert`

**Step 2: 在 replan 流程中记录日志**

- 在 `replan` 成功生成规则结果后插入日志。
- 记录 `userId`、`snapshotId`、`sceneScope`、`availableMinutes`、`originalTotalMinutes`、`selectedCount`、`deferredCount`、`fallbackOnly`。

**Step 3: 扩展统计响应**

- `totalReplans`
- `todayReplans`
- `replanFallbackRate`
- `avgCompressionRate`

**Step 4: Run tests**

Run: `cmd /c "mvn -pl xiaou-application -am -Dtest=AiGrowthCoachSchemaSqlTest,AiGrowthCoachServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test"`
Expected: PASS

### Task 3: 接入管理端统计页展示

**Files:**
- Modify: `vue3-admin-front/src/views/ai-growth-coach/statistics/index.vue`

**Step 1: 展示新增指标**

- 在现有统计页新增“重排总次数”“今日重排次数”“重排兜底率”“平均压缩率”。
- 沿用当前卡片样式，不新增图表。

**Step 2: Run frontend build**

Run: `cmd /c "npm run build"` in `vue3-admin-front`
Expected: PASS

### Task 4: 全量验证

**Files:**
- Modify: `docs/plans/2026-03-14-ai-growth-coach-design.md`

**Step 1: Run backend tests**

Run: `cmd /c "mvn -pl xiaou-application -am -Dtest=AiGrowthCoachSchemaSqlTest,AiGrowthCoachServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test"`
Expected: PASS

**Step 2: Run admin frontend build**

Run: `cmd /c "npm run build"` in `vue3-admin-front`
Expected: PASS

**Step 3: 收口说明**

- 记录本次扩展仅统计“重排预览”，不统计明细列表。
