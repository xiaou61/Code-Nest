---
base_commit: 7e83ba4
---

# WORK-001 工作区归因

基准提交为 `7e83ba4`（TASK-004）。本表登记该提交之后、本次提交之前的工作区改动。

## 工作区归因

| 路径 | 归属 | 说明 |
| --- | --- | --- |
| backend/paideia-security/ | current_work | TASK-005：新增认证授权模块（AuthPort、JwtAuthService、AuthProperties、CurrentUser、SecurityConfiguration、AuthController、MeController、包说明与单测） |
| backend/paideia-app/src/test/ | current_work | TASK-005：授权隔离端到端测试、测试夹具控制器、测试 profile 配置 |
| backend/pom.xml | current_work | TASK-005：登记 paideia-security 模块与依赖管理 |
| backend/paideia-app/pom.xml | current_work | TASK-005：加入安全模块、测试切片模块与持久层 test-jar |
| backend/paideia-persistence/pom.xml | current_work | TASK-005：构建 test-jar 以共享测试夹具 |
| backend/paideia-persistence/src/test/java/io/github/xiaou61/persistence/ExampleItemMapper.java | current_work | TASK-005：加 `@Mapper` 使其可被应用自动扫描 |
| backend/paideia-web/src/main/java/io/github/xiaou61/web/ApiResponseBodyAdvice.java | current_work | TASK-005：修复已包装响应的 traceId 恒为 null 的缺陷 |
| backend/paideia-web/src/test/java/io/github/xiaou61/web/WebPipelineTest.java | current_work | TASK-005：为上述修复加防回归断言 |
| frontend/apps/app/src/pages/HomePage.tsx | current_work | TASK-005：新增「当前身份」面板，走真实受保护接口 |
| frontend/apps/app/e2e/home.spec.ts | current_work | TASK-005：新增受保护接口在浏览器中被拒的用例 |
| frontend/apps/app/playwright.config.ts | current_work | TASK-005：后端改以 local profile 与 backend 工作目录启动 |
| .agent/changes/WORK-001-架构选型与项目骨架/tasks.md | current_work | TASK-005 状态与验证结果 |
| .agent/changes/WORK-001-架构选型与项目骨架/design.md | current_work | TASK-005 实施期确认（实现类名、密钥缺失行为、本地配置位置、放行规则、test-jar、traceId 修复） |
| .agent/history/updates.md | current_work | 追加更新记录 |

## 未纳入版本控制

`backend/config/application-local.yml`（含数据库口令与本地签名密钥，被 `.gitignore` 忽略）、`backend/paideia-app/target/`、`frontend/**/node_modules/`、`frontend/apps/app/dist/`、`test-results/`。

## 备注

- 提交前验证（带 `PAIDEIA_TEST_DB_PASSWORD`）：`mvn -B verify` BUILD SUCCESS（platform 7、web 9、persistence 4、security 8、app 7 个测试）；`pnpm -r typecheck` 通过；`pnpm test:e2e` 3 个用例通过。
- 后端启动现需数据源，本地以 `--spring.profiles.active=local` 运行；e2e 亦依赖 3307 隧道。
- 并发写入提醒：`.agent/` 下另有会话在写入；修改共享工件前先重新读取。
