# 学习资产转化引擎 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 新增“学习资产转化引擎”功能，打通博客、社区帖子、CodePen、模拟面试报告到闪卡、练习清单、知识节点候选、面试题草稿的转化链路。

**Architecture:** 采用独立业务模块 `xiaou-learning-asset` 承担来源快照、转化记录、候选资产、发布日志与审核编排；内容源依赖既有业务模块服务获取详情，资产发布则通过既有 `xiaou-flashcard`、`xiaou-plan`、`xiaou-knowledge`、`xiaou-interview` 的服务接口落地。AI 侧先做可离线的规则化 MVP 生成器，保留后续接入 `xiaou-ai` 工作流的扩展点。

**Tech Stack:** Spring Boot 3 + MyBatis XML + Sa-Token + Maven 多模块 + Vue3 + Element Plus + Vite。

---

## 约束与执行规范

- 严格按 `@test-driven-development` 执行，后端先写失败测试再写实现。
- 新增接口时遵循 `@security-review`：校验来源权限、避免越权读取与越权发布。
- 实现完成前按 `@verification-before-completion` 跑后端编译、用户端构建、管理端构建。
- 前端仓库当前没有单元测试基建，本次以前端构建验证为主，不额外引入测试框架。

---

### Task 1: 新增学习资产模块与 SQL 增量脚本

**Files:**
- Create: `sql/v1.8.4/learning_asset.sql`
- Create: `xiaou-learning-asset/pom.xml`
- Create: `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/domain/LearningAssetRecord.java`
- Create: `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/domain/LearningAssetCandidate.java`
- Create: `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/domain/LearningAssetPublishLog.java`
- Create: `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/enums/*.java`
- Modify: `pom.xml`
- Modify: `xiaou-application/pom.xml`
- Test: `xiaou-learning-asset/src/test/java/com/xiaou/learningasset/sql/LearningAssetSchemaSqlTest.java`

**Step 1: 写失败测试**

```java
@Test
void learningAssetSqlShouldDefineCoreTables() throws Exception {
    String sql = Files.readString(Path.of("../sql/v1.8.4/learning_asset.sql"));
    assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `learning_asset_record`"));
    assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `learning_asset_candidate`"));
    assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `learning_asset_publish_log`"));
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-learning-asset -am -Dtest=LearningAssetSchemaSqlTest test`
Expected: FAIL

**Step 3: 写最小实现**

- 建立新模块骨架与枚举/领域对象
- 补齐 `pom.xml` 和 `xiaou-application/pom.xml` 依赖
- 写 SQL 表结构脚本

**Step 4: 重跑测试并编译**

Run: `mvn -pl xiaou-learning-asset -am -Dtest=LearningAssetSchemaSqlTest test`
Expected: PASS

Run: `mvn -pl xiaou-learning-asset -am -DskipTests compile`
Expected: BUILD SUCCESS

---

### Task 2: 实现来源快照与规则化转化引擎

**Files:**
- Create: `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/service/LearningAssetSourceService.java`
- Create: `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/service/LearningAssetTransformEngine.java`
- Create: `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/service/impl/LearningAssetSourceServiceImpl.java`
- Create: `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/service/impl/LearningAssetTransformEngineImpl.java`
- Create: `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/dto/source/*.java`
- Test: `xiaou-learning-asset/src/test/java/com/xiaou/learningasset/service/LearningAssetTransformEngineTest.java`

**Step 1: 写失败测试**

```java
@Test
void shouldGenerateFlashcardAndPracticeCandidatesFromBlogSource() {
    LearningAssetSourceSnapshot snapshot = LearningAssetSourceSnapshot.builder()
            .sourceType("blog")
            .title("Redis 缓存击穿实战")
            .content("缓存击穿是热点 key 在过期瞬间大量并发请求打到数据库...")
            .tags(List.of("Redis", "缓存"))
            .build();

    TransformResult result = engine.transform(snapshot, TransformMode.STUDY,
            List.of(TargetAssetType.FLASHCARD, TargetAssetType.PRACTICE_PLAN));

    assertFalse(result.getCandidates().isEmpty());
    assertTrue(result.getCandidates().stream().anyMatch(it -> it.getAssetType() == TargetAssetType.FLASHCARD));
    assertTrue(result.getCandidates().stream().anyMatch(it -> it.getAssetType() == TargetAssetType.PRACTICE_PLAN));
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-learning-asset -am -Dtest=LearningAssetTransformEngineTest test`
Expected: FAIL

**Step 3: 写最小实现**

- 适配博客、社区、CodePen、面试报告四类来源快照
- 基于标题、标签、摘要、正文切分规则生成候选资产
- 对空内容、太短内容、无权限来源做保护

**Step 4: 重跑测试**

Run: `mvn -pl xiaou-learning-asset -am -Dtest=LearningAssetTransformEngineTest test`
Expected: PASS

---

### Task 3: 持久层与用户端核心服务

**Files:**
- Create: `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/mapper/LearningAssetRecordMapper.java`
- Create: `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/mapper/LearningAssetCandidateMapper.java`
- Create: `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/mapper/LearningAssetPublishLogMapper.java`
- Create: `xiaou-learning-asset/src/main/resources/mapper/learningasset/*.xml`
- Create: `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/service/LearningAssetService.java`
- Create: `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/service/impl/LearningAssetServiceImpl.java`
- Create: `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/dto/request/*.java`
- Create: `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/dto/response/*.java`
- Test: `xiaou-learning-asset/src/test/java/com/xiaou/learningasset/service/LearningAssetServiceImplTest.java`

**Step 1: 写失败测试**

```java
@Test
void convertShouldCreateRecordAndCandidates() {
    ConvertRequest request = new ConvertRequest();
    request.setSourceType("blog");
    request.setSourceId(1L);
    request.setTransformMode("study");
    request.setTargetTypes(List.of("flashcard", "practice_plan"));

    RecordDetailResponse response = service.convert(1001L, request);

    assertNotNull(response.getRecordId());
    assertEquals("待确认", response.getStatusText());
    assertFalse(response.getCandidates().isEmpty());
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-learning-asset -am -Dtest=LearningAssetServiceImplTest test`
Expected: FAIL

**Step 3: 写最小实现**

- 持久化转化记录和候选资产
- 支持记录详情、记录列表、候选更新、重试
- 候选内容采用 JSON 字符串存储

**Step 4: 重跑测试 + 编译**

Run: `mvn -pl xiaou-learning-asset -am -Dtest=LearningAssetServiceImplTest test`
Expected: PASS

Run: `mvn -pl xiaou-learning-asset -am -DskipTests compile`
Expected: BUILD SUCCESS

---

### Task 4: 实现发布编排与管理员审核流

**Files:**
- Modify: `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/service/LearningAssetService.java`
- Modify: `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/service/impl/LearningAssetServiceImpl.java`
- Create: `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/service/LearningAssetPublishService.java`
- Create: `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/service/impl/LearningAssetPublishServiceImpl.java`
- Test: `xiaou-learning-asset/src/test/java/com/xiaou/learningasset/service/LearningAssetPublishServiceTest.java`

**Step 1: 写失败测试**

```java
@Test
void publishShouldCreateDeckAndSubmitReviewCandidates() {
    PublishResult result = publishService.publish(userId, recordId, List.of(candidateId1, candidateId2));
    assertTrue(result.getPublishedCount() > 0);
    assertTrue(result.getReviewingCount() > 0);
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-learning-asset -am -Dtest=LearningAssetPublishServiceTest test`
Expected: FAIL

**Step 3: 写最小实现**

- 闪卡候选：创建卡组并批量创建闪卡
- 练习清单候选：转换为 `UserPlan`
- 知识节点候选：状态改为 `REVIEWING`
- 面试题草稿候选：状态改为 `REVIEWING`
- 管理员审批通过时落地到 `KnowledgeNodeService` / `InterviewQuestionService`

**Step 4: 重跑测试**

Run: `mvn -pl xiaou-learning-asset -am -Dtest=LearningAssetPublishServiceTest test`
Expected: PASS

---

### Task 5: 暴露用户端与管理端 API

**Files:**
- Create: `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/controller/user/UserLearningAssetController.java`
- Create: `xiaou-learning-asset/src/main/java/com/xiaou/learningasset/controller/admin/AdminLearningAssetController.java`
- Test: `xiaou-learning-asset/src/test/java/com/xiaou/learningasset/controller/UserLearningAssetControllerTest.java`
- Test: `xiaou-learning-asset/src/test/java/com/xiaou/learningasset/controller/AdminLearningAssetControllerTest.java`

**Step 1: 写失败 Web 测试**

```java
@WebMvcTest(UserLearningAssetController.class)
class UserLearningAssetControllerTest {
    @Test
    void shouldExposeConvertEndpoint() throws Exception {
        mockMvc.perform(post("/user/learning-assets/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sourceType\":\"blog\",\"sourceId\":1,\"targetTypes\":[\"flashcard\"]}"))
            .andExpect(status().isOk());
    }
}
```

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-learning-asset -am -Dtest=UserLearningAssetControllerTest,AdminLearningAssetControllerTest test`
Expected: FAIL

**Step 3: 写最小实现**

- 用户端：convert/list/detail/update/confirm/publish/retry
- 管理端：list/detail/approve/reject
- 补充日志注解与权限校验

**Step 4: 重跑测试**

Run: `mvn -pl xiaou-learning-asset -am -Dtest=UserLearningAssetControllerTest,AdminLearningAssetControllerTest test`
Expected: PASS

---

### Task 6: 实现用户端前端入口与“我的学习资产”

**Files:**
- Create: `vue3-user-front/src/api/learningAssets.js`
- Create: `vue3-user-front/src/views/learning-assets/Index.vue`
- Create: `vue3-user-front/src/components/learning-assets/TransformDialog.vue`
- Modify: `vue3-user-front/src/router/index.js`
- Modify: `vue3-user-front/src/views/blog/ArticleDetail.vue`
- Modify: `vue3-user-front/src/views/community/PostDetail.vue`
- Modify: `vue3-user-front/src/views/codepen/Detail.vue`
- Modify: `vue3-user-front/src/views/mock-interview/Report.vue`

**Step 1: 先写最小联调目标**

- 每个详情页可打开统一转化弹窗
- 可发起转化并跳到“我的学习资产”
- “我的学习资产”可查看记录列表与候选资产
- 支持编辑候选标题/标签并触发发布

**Step 2: 实现 API 与页面**

- 统一封装用户端接口
- 复用现有 Element Plus 卡片/对话框风格
- 保持与学习驾驶舱相关页面一致的视觉语言

**Step 3: 本地构建验证**

Run: `cd vue3-user-front && npm run build`
Expected: BUILD SUCCESS

---

### Task 7: 实现管理端审核页面

**Files:**
- Create: `vue3-admin-front/src/api/learningAssets.js`
- Create: `vue3-admin-front/src/views/learning-assets/review/index.vue`
- Modify: `vue3-admin-front/src/router/index.js`

**Step 1: 最小功能**

- 列出 `REVIEWING` 状态候选资产
- 支持查看来源快照、候选内容
- 支持审批通过和驳回

**Step 2: 构建验证**

Run: `cd vue3-admin-front && npm run build`
Expected: BUILD SUCCESS

---

### Task 8: 最终验证与文档同步

**Files:**
- Modify: `README.md`（如需补充新模块或路由）

**Step 1: 后端编译**

Run: `mvn -pl xiaou-application -am clean package -DskipTests`
Expected: BUILD SUCCESS

**Step 2: 后端测试**

Run: `mvn -pl xiaou-learning-asset -am test`
Expected: BUILD SUCCESS

**Step 3: 前端构建**

Run: `cd vue3-user-front && npm run build`
Expected: BUILD SUCCESS

Run: `cd vue3-admin-front && npm run build`
Expected: BUILD SUCCESS

**Step 4: 手工联调重点**

- 博客详情页发起转化
- 社区帖子发起转化并带入 AI 摘要
- 模拟面试报告发起转化
- 发布闪卡卡组成功
- 管理端审批知识节点候选

