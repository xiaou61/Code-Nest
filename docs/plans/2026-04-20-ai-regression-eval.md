# AI Regression Eval

## 目标

为统一 AI 运行时补一层“固定黄金样例回归”，避免后续在以下动作中出现隐性质量回退：

- 修改 Prompt 模板
- 调整 Prompt 版本
- 更换中转站模型
- 修改结构化输出解析
- 调整 LangGraph 节点编排

## 当前落地

位置：

- 运行时服务：`xiaou-ai/src/main/java/com/xiaou/ai/regression/`
- 后台接口：`xiaou-system/src/main/java/com/xiaou/system/controller/AiConfigController.java`
- 管理后台页面：`vue3-admin-front/src/views/system/ai-config/index.vue`
- 样例文件：`xiaou-ai/src/main/resources/ai-evals/scene-regression-cases.json`
- 测试入口：`xiaou-ai/src/test/java/com/xiaou/ai/eval/AiSceneRegressionEvalTest.java`

当前已经同时支持两种执行方式：

1. 测试侧通过 JUnit 跑黄金样例回归，适合 CI 和本地开发校验
2. 运行时通过 `AiRegressionService` 在后台直接执行，适合管理员在改 Prompt、换模型、调图编排后做快速人工复核

此外，后台现在会保存“最近一次回归结果”以及“最近几次回归历史”：

- 进程内始终保留最近一次执行结果，并维护最近 20 次回归历史
- 若 Redis 可用且 AI 运行观测持久化已启用，则会额外写入 Redis
- 管理后台刷新后仍可恢复最近一次执行时间、通过/失败数量和单用例结果
- 管理后台支持直接查看最近历史中的任意一次执行详情，便于比对不同模型、Prompt 或编排改动后的回归表现
- 管理后台会按最近窗口聚合场景健康状态，优先展示最近状态退化的场景，帮助快速定位哪条链路开始不稳定
- 场景健康展开区会继续给出“高频失败用例 Top5”和“高频失败原因 Top5”，便于快速判断是单点 case 退化，还是同类错误在多个 case 上反复出现
- 单用例回归结果会额外记录 `modelName / graphName / promptIds`，便于在后台直接判断当前失败命中了哪条运行链路
- 场景健康展开区会进一步给出“退化模型 Top5 / 高影响图编排 Top5 / 高影响 Prompt Top5”，帮助快速判断质量回退更像是模型波动、Prompt 变更还是图编排链路退化

当前已覆盖：

1. 社区摘要
2. 模拟面试评价
3. 模拟面试总结
4. 模拟面试题生成
5. 模拟面试追问生成
6. 求职作战台 JD 解析
7. 求职作战台简历匹配
8. 求职作战台行动计划
9. 求职作战台面试复盘
10. 求职作战台单岗位综合分析
11. SQL 分析
12. SQL 分析 + 重写组合流程
13. SQL 重写
14. SQL 收益对比

同时覆盖了两类异常路径：

- 统一 AI 直接降级
- 模型返回非法 JSON 后走本地规则兜底

## 设计原则

1. 用例放资源文件，不把黄金样例硬编码在测试类里。
2. 测试断言聚焦“结构稳定性与关键语义”，不过度绑定完整文案。
3. 样例优先覆盖用户可见核心结果字段，例如：
   - fallback
   - score
   - summary
   - keywords
   - dailyTasks
   - suggestions
4. 新增场景时优先加成功样例，再补异常样例。

## 运行方式

后台手动执行：

- 拉取用例目录：`GET /admin/ai/config/regression/cases`
- 拉取最近一次结果：`GET /admin/ai/config/regression/latest`
- 拉取最近历史结果：`GET /admin/ai/config/regression/history?limit=10`
- 拉取场景健康聚合：`GET /admin/ai/config/regression/scenario-health?limit=10`
- 执行回归：`POST /admin/ai/config/regression/run`
- 管理后台入口：`系统管理 -> AI 配置与观测 -> AI 黄金样例回归`

测试执行：

```bash
mvn -pl xiaou-ai -am "-Dtest=AiSceneRegressionEvalTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

CI 工作流：

- `.github/workflows/ai-regression.yml`
- 触发范围：`xiaou-ai`、`xiaou-common`、根 `pom.xml` 以及 workflow 自身改动
- 失败时会上传 surefire 报告，便于排查具体断言

## 扩展约定

新增样例时只需要：

1. 在 `scene-regression-cases.json` 中追加 case
2. 如果是新场景，补对应场景执行器，并纳入 `AiRegressionService` / `AiSceneRegressionEvalTest`
3. 在后台页面确认新 case 可以被拉取并可单条执行

对于多步图编排场景，还可以使用：

- `responses`: 按调用顺序消费的模型响应列表
- `fallbackSequence`: 按调用顺序决定是否直接走 fallback 的布尔列表

## 后续建议

下一阶段可以继续补：

1. 把评测结果继续接入 CI，变成合并前必跑回归项
2. 若后续新增多节点图，优先沿用 `responses + fallbackSequence` 机制补 fixture
3. 视需要把历史记录从“最近 20 次”继续扩展成可检索、可筛选的长期留存方案
4. 进一步按模型、Prompt 版本或图编排版本补时间趋势和版本对比，支持更细粒度定位质量回退来源
