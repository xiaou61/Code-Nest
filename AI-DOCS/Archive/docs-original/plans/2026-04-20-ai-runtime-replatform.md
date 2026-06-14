# AI Runtime Replatform Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将项目中的 Coze AI 运行链路彻底替换为 `LangChain4j + LangGraph4j + LlamaIndex` 架构，并保持现有业务 API 与核心 DTO 基本稳定。

**Architecture:** 保留 `xiaou-ai` 作为统一 AI 场景门面，在 `xiaou-common` 中收敛基础配置与安全约束，在 `xiaou-ai` 中拆分模型层、图编排层、RAG 接入层和场景层。复杂链路使用 `LangGraph4j`，单步结构化场景优先使用 `LangChain4j`，知识库能力通过独立 `LlamaIndex` 服务接入。

**Tech Stack:** Java 17, Spring Boot 3.4.4, Maven Multi-Module, LangChain4j, LangGraph4j, LlamaIndex, Redis, Micrometer, JUnit 5

---

### Task 1: 清理 Coze 公共依赖与不安全配置

**Files:**
- Modify: `pom.xml`
- Modify: `xiaou-common/pom.xml`
- Modify: `xiaou-application/src/main/resources/application.yml`
- Delete: `xiaou-common/src/main/java/com/xiaou/common/config/CozeConfig.java`
- Delete: `xiaou-common/src/main/java/com/xiaou/common/enums/CozeWorkflowEnum.java`
- Delete: `xiaou-common/src/main/java/com/xiaou/common/utils/CozeUtils.java`
- Delete: `xiaou-application/src/test/java/CozeTest.java`
- Test: `mvn -pl xiaou-common,xiaou-ai,xiaou-application -am test -DskipITs`

**Step 1: 写失败验证**

Run: `rg -n "Coze|coze-api|CozeUtils|CozeWorkflowEnum" xiaou-common xiaou-ai xiaou-application`

Expected: 能看到当前遗留引用，作为清理基线。

**Step 2: 清理公共依赖**

- 从 `xiaou-common/pom.xml` 删除 Coze SDK。
- 从 `application.yml` 删除 `coze:` 配置段与明文密钥。

**Step 3: 删除旧公共代码**

- 删除 `CozeConfig`
- 删除 `CozeWorkflowEnum`
- 删除 `CozeUtils`
- 删除 `CozeTest`

**Step 4: 运行验证**

Run: `rg -n "Coze|coze-api|CozeUtils|CozeWorkflowEnum" xiaou-common xiaou-application`

Expected: 公共层与应用测试层不再出现 Coze 主引用。

**Step 5: 提交**

```bash
git add pom.xml xiaou-common/pom.xml xiaou-application/src/main/resources/application.yml xiaou-common/src/main/java xiaou-application/src/test/java
git commit -m "refactor: remove coze runtime foundation"
```

### Task 2: 建立统一 AI 配置与基础异常模型

**Files:**
- Create: `xiaou-common/src/main/java/com/xiaou/common/config/AiProperties.java`
- Create: `xiaou-common/src/main/java/com/xiaou/common/exception/ai/AiConfigurationException.java`
- Create: `xiaou-common/src/main/java/com/xiaou/common/exception/ai/AiInvocationException.java`
- Create: `xiaou-common/src/main/java/com/xiaou/common/exception/ai/AiRetrievalException.java`
- Create: `xiaou-common/src/main/java/com/xiaou/common/exception/ai/AiStructuredOutputException.java`
- Modify: `xiaou-application/src/main/resources/application.yml`
- Test: `xiaou-common/src/test/java/com/xiaou/common/config/AiPropertiesTest.java`

**Step 1: 写失败测试**

新增 `AiPropertiesTest`，验证：

- 默认 provider 正常装配
- 缺失 API Key 时按配置可识别
- RAG endpoint 可读取

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-common -Dtest=AiPropertiesTest test`

Expected: FAIL，提示配置类不存在。

**Step 3: 写最小实现**

- 增加新的 `xiaou.ai` 配置树
- 增加 AI 相关基础异常

**Step 4: 运行测试确认通过**

Run: `mvn -pl xiaou-common -Dtest=AiPropertiesTest test`

Expected: PASS

**Step 5: 提交**

```bash
git add xiaou-common/src/main/java/com/xiaou/common/config/AiProperties.java xiaou-common/src/main/java/com/xiaou/common/exception/ai xiaou-common/src/test/java/com/xiaou/common/config/AiPropertiesTest.java xiaou-application/src/main/resources/application.yml
git commit -m "feat: add unified ai configuration"
```

### Task 3: 为 `xiaou-ai` 引入 LangChain4j 与基础模型工厂

**Files:**
- Modify: `xiaou-ai/pom.xml`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/config/LangChain4jConfig.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/client/AiModelFactory.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/client/AiEmbeddingFactory.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/support/AiExecutionSupport.java`
- Test: `xiaou-ai/src/test/java/com/xiaou/ai/client/AiModelFactoryTest.java`

**Step 1: 写失败测试**

验证：

- Spring 可装配 chat model 工厂
- 缺失关键配置时能抛出 `AiConfigurationException`

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-ai -Dtest=AiModelFactoryTest test`

Expected: FAIL，提示工厂类不存在。

**Step 3: 实现最小模型工厂**

- 在 `pom.xml` 增加 LangChain4j 依赖
- 建立 provider 到具体 model 的创建逻辑
- 统一超时、重试、日志包装

**Step 4: 运行测试确认通过**

Run: `mvn -pl xiaou-ai -Dtest=AiModelFactoryTest test`

Expected: PASS

**Step 5: 提交**

```bash
git add xiaou-ai/pom.xml xiaou-ai/src/main/java/com/xiaou/ai/config xiaou-ai/src/main/java/com/xiaou/ai/client xiaou-ai/src/main/java/com/xiaou/ai/support xiaou-ai/src/test/java/com/xiaou/ai/client/AiModelFactoryTest.java
git commit -m "feat: add langchain4j model infrastructure"
```

### Task 4: 建立统一结构化输出与 Prompt 模板层

**Files:**
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/prompt/...`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/structured/...`
- Replace: `xiaou-ai/src/main/java/com/xiaou/ai/util/CozeResponseParser.java`
- Test: `xiaou-ai/src/test/java/com/xiaou/ai/structured/StructuredOutputTest.java`

**Step 1: 写失败测试**

验证：

- 面试评价输出可映射为 `AnswerEvaluationResult`
- SQL 诊断输出可映射为 `SqlAnalyzeResult`
- 无效 JSON / 无效结构时抛出统一异常

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-ai -Dtest=StructuredOutputTest test`

Expected: FAIL

**Step 3: 实现结构化层**

- 不再依赖 `CozeResponseParser`
- 使用 LangChain4j 的结构化输出能力或统一 JSON schema 解析

**Step 4: 运行测试确认通过**

Run: `mvn -pl xiaou-ai -Dtest=StructuredOutputTest test`

Expected: PASS

**Step 5: 提交**

```bash
git add xiaou-ai/src/main/java/com/xiaou/ai/prompt xiaou-ai/src/main/java/com/xiaou/ai/structured xiaou-ai/src/test/java/com/xiaou/ai/structured/StructuredOutputTest.java
git commit -m "feat: add ai prompt and structured output layer"
```

### Task 5: 搭建 LlamaIndex 检索网关

**Files:**
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/rag/LlamaIndexClient.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/rag/LlamaIndexRetrieveRequest.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/rag/LlamaIndexRetrieveResponse.java`
- Create: `xiaou-ai/src/test/java/com/xiaou/ai/rag/LlamaIndexClientTest.java`
- Create: `llamaindex-service/README.md`
- Create: `llamaindex-service/` 基础脚手架文件

**Step 1: 写失败测试**

验证：

- Java 端可正确调用检索接口
- 远端返回异常时抛出 `AiRetrievalException`

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-ai -Dtest=LlamaIndexClientTest test`

Expected: FAIL

**Step 3: 实现最小检索网关**

- 定义统一检索请求/响应契约
- 增加独立服务目录说明
- Java 侧只负责网关，不直接耦合 LlamaIndex 内部实现

**Step 4: 运行测试确认通过**

Run: `mvn -pl xiaou-ai -Dtest=LlamaIndexClientTest test`

Expected: PASS

**Step 5: 提交**

```bash
git add xiaou-ai/src/main/java/com/xiaou/ai/rag xiaou-ai/src/test/java/com/xiaou/ai/rag/LlamaIndexClientTest.java llamaindex-service
git commit -m "feat: add llamaindex retrieval gateway"
```

### Task 6: 用 LangChain4j 迁移社区摘要场景

**Files:**
- Modify: `xiaou-ai/src/main/java/com/xiaou/ai/service/impl/AiCommunityServiceImpl.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/scene/community/...`
- Test: `xiaou-ai/src/test/java/com/xiaou/ai/service/impl/AiCommunityServiceImplTest.java`
- Test: `xiaou-community/src/test/java/com/xiaou/community/service/impl/CommunityAiSummaryServiceImplTest.java`

**Step 1: 写失败测试**

覆盖：

- 正常摘要生成
- fallback 场景
- 关键词为空时的稳定返回

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-ai,xiaou-community -am -Dtest=AiCommunityServiceImplTest,CommunityAiSummaryServiceImplTest test`

Expected: FAIL

**Step 3: 实现最小迁移**

- 使用 `LangChain4j AiService` 直接输出摘要 DTO
- 不引入 `LangGraph4j`

**Step 4: 运行测试确认通过**

Run: `mvn -pl xiaou-ai,xiaou-community -am -Dtest=AiCommunityServiceImplTest,CommunityAiSummaryServiceImplTest test`

Expected: PASS

**Step 5: 提交**

```bash
git add xiaou-ai/src/main/java/com/xiaou/ai/service/impl/AiCommunityServiceImpl.java xiaou-ai/src/main/java/com/xiaou/ai/scene/community xiaou-ai/src/test/java/com/xiaou/ai/service/impl/AiCommunityServiceImplTest.java xiaou-community/src/test/java/com/xiaou/community/service/impl/CommunityAiSummaryServiceImplTest.java
git commit -m "refactor: migrate community summary to langchain4j"
```

### Task 7: 用 LangGraph4j 迁移模拟面试场景

**Files:**
- Modify: `xiaou-ai/src/main/java/com/xiaou/ai/service/impl/AiInterviewServiceImpl.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/graph/interview/...`
- Modify: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/AIInterviewerServiceImpl.java`
- Modify: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/QuestionSelectorServiceImpl.java`
- Test: `xiaou-ai/src/test/java/com/xiaou/ai/graph/interview/InterviewGraphTest.java`
- Test: `xiaou-mock-interview/src/test/java/com/xiaou/mockinterview/service/impl/AIInterviewerServiceImplTest.java`

**Step 1: 写失败测试**

覆盖：

- 出题
- 回答评价
- 追问决策
- 总结生成
- fallback 分支

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-ai,xiaou-mock-interview -am -Dtest=InterviewGraphTest,AIInterviewerServiceImplTest test`

Expected: FAIL

**Step 3: 写最小图编排实现**

- 定义面试状态对象
- 定义核心节点与条件分支
- 场景 service 对图执行结果做 DTO 转换

**Step 4: 运行测试确认通过**

Run: `mvn -pl xiaou-ai,xiaou-mock-interview -am -Dtest=InterviewGraphTest,AIInterviewerServiceImplTest test`

Expected: PASS

**Step 5: 提交**

```bash
git add xiaou-ai/src/main/java/com/xiaou/ai/service/impl/AiInterviewServiceImpl.java xiaou-ai/src/main/java/com/xiaou/ai/graph/interview xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/AIInterviewerServiceImpl.java xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/QuestionSelectorServiceImpl.java xiaou-ai/src/test/java/com/xiaou/ai/graph/interview/InterviewGraphTest.java xiaou-mock-interview/src/test/java/com/xiaou/mockinterview/service/impl/AIInterviewerServiceImplTest.java
git commit -m "refactor: migrate mock interview to langgraph4j"
```

### Task 8: 用 LangGraph4j 迁移 SQL 优化场景

**Files:**
- Modify: `xiaou-ai/src/main/java/com/xiaou/ai/service/impl/AiSqlOptimizeServiceImpl.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/graph/sql/...`
- Modify: `xiaou-sql-optimizer/src/main/java/com/xiaou/sqloptimizer/service/impl/SqlOptimizerServiceImpl.java`
- Test: `xiaou-ai/src/test/java/com/xiaou/ai/graph/sql/SqlOptimizeGraphTest.java`
- Test: `xiaou-sql-optimizer/src/test/java/com/xiaou/sqloptimizer/service/impl/SqlOptimizerServiceImplTest.java`

**Step 1: 写失败测试**

覆盖：

- 诊断
- 重写
- 对比
- fallback

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-ai,xiaou-sql-optimizer -am -Dtest=SqlOptimizeGraphTest,SqlOptimizerServiceImplTest test`

Expected: FAIL

**Step 3: 实现最小图编排**

- 构建 SQL 状态对象
- 拆出 analyze / rewrite / compare / riskCheck 节点
- 让现有工作台 service 保持外部响应不变

**Step 4: 运行测试确认通过**

Run: `mvn -pl xiaou-ai,xiaou-sql-optimizer -am -Dtest=SqlOptimizeGraphTest,SqlOptimizerServiceImplTest test`

Expected: PASS

**Step 5: 提交**

```bash
git add xiaou-ai/src/main/java/com/xiaou/ai/service/impl/AiSqlOptimizeServiceImpl.java xiaou-ai/src/main/java/com/xiaou/ai/graph/sql xiaou-sql-optimizer/src/main/java/com/xiaou/sqloptimizer/service/impl/SqlOptimizerServiceImpl.java xiaou-ai/src/test/java/com/xiaou/ai/graph/sql/SqlOptimizeGraphTest.java xiaou-sql-optimizer/src/test/java/com/xiaou/sqloptimizer/service/impl/SqlOptimizerServiceImplTest.java
git commit -m "refactor: migrate sql optimizer to langgraph4j"
```

### Task 9: 用 LangGraph4j 迁移求职作战台场景

**Files:**
- Modify: `xiaou-ai/src/main/java/com/xiaou/ai/service/impl/AiJobBattleServiceImpl.java`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/graph/jobbattle/...`
- Modify: `xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/JobBattleServiceImpl.java`
- Test: `xiaou-ai/src/test/java/com/xiaou/ai/graph/jobbattle/JobBattleGraphTest.java`
- Test: `xiaou-mock-interview/src/test/java/com/xiaou/mockinterview/service/impl/JobBattleServiceImplTest.java`

**Step 1: 写失败测试**

覆盖：

- JD 解析
- 简历匹配
- 计划生成
- 面试复盘
- graph fallback

**Step 2: 运行测试确认失败**

Run: `mvn -pl xiaou-ai,xiaou-mock-interview -am -Dtest=JobBattleGraphTest,JobBattleServiceImplTest test`

Expected: FAIL

**Step 3: 实现最小图编排**

- 抽象作战台状态对象
- 拆分多阶段节点
- 场景 service 继续对外返回现有 DTO

**Step 4: 运行测试确认通过**

Run: `mvn -pl xiaou-ai,xiaou-mock-interview -am -Dtest=JobBattleGraphTest,JobBattleServiceImplTest test`

Expected: PASS

**Step 5: 提交**

```bash
git add xiaou-ai/src/main/java/com/xiaou/ai/service/impl/AiJobBattleServiceImpl.java xiaou-ai/src/main/java/com/xiaou/ai/graph/jobbattle xiaou-mock-interview/src/main/java/com/xiaou/mockinterview/service/impl/JobBattleServiceImpl.java xiaou-ai/src/test/java/com/xiaou/ai/graph/jobbattle/JobBattleGraphTest.java xiaou-mock-interview/src/test/java/com/xiaou/mockinterview/service/impl/JobBattleServiceImplTest.java
git commit -m "refactor: migrate job battle to langgraph4j"
```

### Task 10: 清理旧文档、补齐观测与全量验证

**Files:**
- Modify: `README.md`
- Modify: `docs/archive/coze/*`
- Modify: `docs/plans/2026-04-20-ai-runtime-replatform-design.md`
- Modify: `docs/plans/2026-04-20-ai-runtime-replatform.md`
- Create: `docs/plans/2026-04-20-ai-prompt-governance.md`
- Create: `xiaou-ai/src/main/java/com/xiaou/ai/metrics/...`
- Create: `xiaou-ai/src/test/java/com/xiaou/ai/prompt/...`
- Test: `mvn -pl xiaou-ai,xiaou-community,xiaou-mock-interview,xiaou-sql-optimizer -am test`

**Step 1: 补齐观测**

- 增加调用耗时、fallback 计数、结构化失败数
- 增加 `prompt_key`、`prompt_version` 标签

**Step 2: 清理旧文档**

- README 改为新 AI 架构说明
- 废弃或迁移 `docs/coze` 下旧说明
- 补充 Prompt 治理规范文档，明确模板、变量、版本、测试与观测约束

**Step 3: 运行回归**

Run: `mvn -pl xiaou-ai,xiaou-community,xiaou-mock-interview,xiaou-sql-optimizer -am test`

Expected: 关键模块测试通过。

**Step 4: 完整检索清理**

Run: `rg -n "Coze|coze-api|CozeUtils|CozeWorkflowEnum" .`

Expected: 仅允许出现在历史计划或归档文档中；运行代码不再引用。

**Step 5: 提交**

```bash
git add README.md docs/archive/coze docs/plans xiaou-ai/src/main/java/com/xiaou/ai/metrics xiaou-ai/src/test/java/com/xiaou/ai/prompt
git commit -m "chore: finish ai replatform cleanup and observability"
```
