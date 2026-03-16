# 科技热点速览模块 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 Code Nest 落地一个可运行的“科技热点速览”第一版，支持国内外 RSS 科技新闻聚合、文章列表与详情、飞书 / 钉钉订阅接入、基础后台管理与定时刷新。

**Architecture:** 在 `xiaou-application` 下新增 `com.xiaou.techbriefing` 领域，采用 `Controller + Service + MyBatis XML + Scheduled Task` 结构。抓取层首版仅支持 RSS/Feed 源，入库时同步完成规则摘要和可选 AI 摘要 / 翻译兜底；订阅层统一抽象为 `FEISHU / DINGTALK` Webhook 渠道，并通过独立客户端处理测试消息发送与签名。

**Tech Stack:** Spring Boot 3、MyBatis XML、Hutool、Maven 多模块、Vue3、Element Plus、Vite。

---

### Task 1: 为科技热点核心能力写失败测试

**Files:**
- Create: `xiaou-application/src/test/java/com/xiaou/techbriefing/sql/TechBriefingSchemaSqlTest.java`
- Create: `xiaou-application/src/test/java/com/xiaou/techbriefing/service/TechBriefingServiceImplTest.java`

**Step 1: Write the failing test**

- 在 SQL 测试中断言以下核心表存在：
  - `tech_briefing_source`
  - `tech_briefing_article`
  - `tech_briefing_article_content`
  - `tech_briefing_article_ai`
  - `tech_briefing_subscription`
  - `tech_briefing_fetch_log`
- 在服务测试中覆盖以下行为：
  - `listArticlesShouldReturnPagedCards`
  - `refreshSourcesShouldPersistParsedFeedItems`
  - `createSubscriptionShouldValidateAndMaskWebhook`
  - `testSubscriptionShouldSendChannelMessage`

**Step 2: Run test to verify it fails**

Run: `cmd /c "mvn -pl xiaou-application -am -Dtest=TechBriefingSchemaSqlTest,TechBriefingServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test"`
Expected: FAIL，提示表、Service 或订阅发送能力尚未实现。

### Task 2: 实现后端科技热点领域与飞书 / 钉钉渠道能力

**Files:**
- Create: `sql/v1.8.5/tech_briefing.sql`
- Create: `xiaou-application/src/main/java/com/xiaou/techbriefing/domain/*.java`
- Create: `xiaou-application/src/main/java/com/xiaou/techbriefing/dto/request/*.java`
- Create: `xiaou-application/src/main/java/com/xiaou/techbriefing/dto/response/*.java`
- Create: `xiaou-application/src/main/java/com/xiaou/techbriefing/mapper/*.java`
- Create: `xiaou-application/src/main/resources/mapper/techbriefing/*.xml`
- Create: `xiaou-application/src/main/java/com/xiaou/techbriefing/service/TechBriefingService.java`
- Create: `xiaou-application/src/main/java/com/xiaou/techbriefing/service/impl/TechBriefingServiceImpl.java`
- Create: `xiaou-application/src/main/java/com/xiaou/techbriefing/service/impl/TechBriefingWebhookClient.java`
- Create: `xiaou-application/src/main/java/com/xiaou/techbriefing/service/impl/TechBriefingRssClient.java`
- Create: `xiaou-application/src/main/java/com/xiaou/techbriefing/controller/user/UserTechBriefingController.java`
- Create: `xiaou-application/src/main/java/com/xiaou/techbriefing/controller/admin/AdminTechBriefingController.java`
- Create: `xiaou-application/src/main/java/com/xiaou/techbriefing/task/TechBriefingTask.java`
- Modify: `xiaou-common/src/main/java/com/xiaou/common/enums/CozeWorkflowEnum.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/dto/techbriefing/*.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/service/AiTechBriefingService.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/service/impl/AiTechBriefingServiceImpl.java`

**Step 1: Implement minimal schema and seed sources**

- 新增 6 张核心表。
- 预置稳定 RSS 内容源：
  - `TechCrunch`
  - `Ars Technica`
  - `VentureBeat`
  - `爱范儿`
  - `InfoQ 中文`

**Step 2: Implement article querying and detail assembly**

- 支持首页分页列表、焦点区、分类列表、文章详情。
- 详情返回中文标题、摘要、AI 速览、中文全文、原文链接、翻译状态。

**Step 3: Implement RSS refresh and fallback summary**

- 支持按来源抓取 RSS。
- 解析标题、链接、发布时间、摘要、正文片段、图片、标签。
- 海外内容优先调用 AI 翻译 / 摘要；失败则走规则降级。

**Step 4: Implement FEISHU / DINGTALK subscription**

- 创建订阅渠道。
- 对 Webhook 地址做格式校验。
- 存储脱敏值与配置摘要。
- 支持测试消息发送。

**Step 5: Run backend tests**

Run: `cmd /c "mvn -pl xiaou-application -am -Dtest=TechBriefingSchemaSqlTest,TechBriefingServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test"`
Expected: PASS

### Task 3: 接入用户端科技热点页面

**Files:**
- Create: `vue3-user-front/src/api/techBriefing.js`
- Create: `vue3-user-front/src/views/tech-briefing/Index.vue`
- Create: `vue3-user-front/src/views/tech-briefing/Detail.vue`
- Modify: `vue3-user-front/src/router/index.js`

**Step 1: 首页实现**

- 顶部产品区
- 今日焦点
- 分类 / 范围 / 时间 / 来源筛选
- 文章卡片流
- 飞书 / 钉钉订阅抽屉或弹层

**Step 2: 详情页实现**

- 中文标题、原文标题、来源信息
- AI 速览卡片
- 中文全文 / 降级提示
- 原文跳转
- 相关主题标签

**Step 3: Run user frontend build**

Run: `cmd /c "npm run build"` in `vue3-user-front`
Expected: PASS

### Task 4: 接入管理端科技热点后台

**Files:**
- Create: `vue3-admin-front/src/api/techBriefing.js`
- Create: `vue3-admin-front/src/views/tech-briefing/source/index.vue`
- Create: `vue3-admin-front/src/views/tech-briefing/article/index.vue`
- Create: `vue3-admin-front/src/views/tech-briefing/subscription/index.vue`
- Create: `vue3-admin-front/src/views/tech-briefing/task/index.vue`
- Modify: `vue3-admin-front/src/router/index.js`
- Modify: `vue3-admin-front/src/layout/index.vue`

**Step 1: 来源管理**

- 查看 / 新增 / 启停来源
- 手动刷新所有来源

**Step 2: 文章管理**

- 查看文章列表
- 置顶 / 下线
- 重试翻译 / 重试摘要

**Step 3: 订阅与任务管理**

- 查看订阅渠道
- 触发测试消息
- 查看任务日志

**Step 4: Run admin frontend build**

Run: `cmd /c "npm run build"` in `vue3-admin-front`
Expected: PASS

### Task 5: 全量验证与收口

**Files:**
- Modify: `docs/PRD/科技热点速览模块PRD-v1.0.0.md`

**Step 1: Run backend verification**

Run: `cmd /c "mvn -pl xiaou-application -am -Dtest=TechBriefingSchemaSqlTest,TechBriefingServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test"`
Expected: PASS

**Step 2: Run frontend verification**

Run: `cmd /c "npm run build"` in `vue3-user-front`
Expected: PASS

Run: `cmd /c "npm run build"` in `vue3-admin-front`
Expected: PASS

**Step 3: Sanity check**

- 验证默认 RSS 源能抓到内容
- 验证飞书 / 钉钉测试消息链路成功或明确返回错误
- 记录一期仍未覆盖的范围：
  - 每日自动推送
  - OAuth 组织接入
  - 个性化推荐
