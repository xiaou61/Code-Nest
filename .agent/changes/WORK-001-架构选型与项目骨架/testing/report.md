---
artifact: test-report
work_id: WORK-001
work: 架构选型与项目骨架
status: passed
evidence: required
created: 2026-10-09
accepted_by: xiaou61
accepted_at: 2026-10-09 13:43:08 +0800
---

# 架构选型与项目骨架验证报告

## 验证环境

- 本机：Windows，JDK 25.0.4（Temurin）、Maven 3.9.0、Node 24.14.1、pnpm 12.10.1、Electron 44.7.0。
- 后端运行 profile：`local`（数据源指向本机 3307 隧道到服务器 MySQL）。
- 数据库：服务器上的 MySQL 8.0.46，测试库 `paideia_test`（经 SSH 隧道）。
- 前端：React 19.3.0、Vite 8.3.4、Vitest 5.0.3、Playwright 1.64.0。

## 验证结果

| 检查项 | 命令 | 退出码 | 结果 | 证据 |
| --- | --- | --- | --- | --- |
| AC-001 技术选项覆盖与裁决留痕 | 人工检查登记表与已裁决清单 | manual | passed | `.agent/references/technology-options.md`、`.agent/rules/always.md` |
| AC-002 构建与健康检查 | `mvn -B verify`；`java -jar … --spring.profiles.active=local` + `curl /actuator/health` | 0 | passed | `testing/logs/backend-verify.txt`、`testing/logs/runtime-and-desktop.txt` |
| AC-003 浏览器端到端 | `cd frontend && pnpm test:e2e` | 0 | passed | `testing/logs/frontend-surfaces.txt` |
| AC-004 模块边界构建期强制 | `mvn -B verify`（`ModularityTest`） | 0 | passed | `testing/logs/backend-verify.txt`；人为违反见 `tasks.md` TASK-001/TASK-002 |
| AC-005 MySQL 分页与归属过滤 | `mvn -B verify`（`PaginationIntegrationTest`） | 0 | passed | `testing/logs/backend-verify.txt` |
| AC-006 授权隔离与越权拒绝 | `mvn -B verify`（`AuthorizationIsolationTest`）；运行时 401/主体/403 | 0 | passed | `testing/logs/backend-verify.txt`、`testing/logs/runtime-and-desktop.txt` |
| AC-007 桌面端运行时（源码启动，渲染服务、调用后端、CORS） | 渲染服务 200；后端请求计数 4.0→5.0；白名单来源获 ACAO、未列入来源 403 | 0 | passed | `testing/logs/runtime-and-desktop.txt` |
| AC-007 Windows 安装包产出与打包应用运行 | `ELECTRON_MIRROR=… ELECTRON_BUILDER_BINARIES_MIRROR=… pnpm --filter @paideia/desktop exec electron-builder`；随后启动 `release/win-unpacked/Paideia.exe` | 0 | passed | `testing/logs/desktop-packaged.txt`（安装包 111,510,247 字节；请求计数 1.0→2.0；CORS 放行头） |
| AC-008 公开页预渲染与 surface 隔离 | `pnpm --filter @paideia/public build` + 产物交叉检索 | 0 | passed | `testing/logs/frontend-surfaces.txt` |

关键数值：

- 后端 `mvn -B verify`：5 个模块全部 SUCCESS，共 38 个测试（platform 7、web 9、persistence 4、security 8、app 10）。
- 启动耗时 4.332 秒，`/actuator/health` 返回 `{"groups":["liveness","readiness"],"status":"UP"}`。
- 认证链路：无令牌 401；携带令牌时 `/api/v1/me` 返回 `learner-a`，且响应带 `traceId`（此前该字段恒为 null，本次修复）。
- CORS：白名单来源 `http://127.0.0.1:5310` 在 `/actuator/health` 上获得 `Access-Control-Allow-Origin`；未列入来源被拒为 403。
- 桌面端：渲染静态服务返回 200，后端 `/actuator/health` 请求计数由 4.0 增至 5.0，electron 进程 4 个。
- 公开页：预渲染注入 699 个字符正文；桌面端产物不含公开页文案，公开页产物不含主体应用文案（双向对照）。
- 安装包：`Paideia Setup 0.0.1.exe` 111,510,247 字节；打包后的应用启动后渲染服务 200、后端请求计数 1.0→2.0、Padieia 进程 4 个、CORS 放行头存在。**构建必须同时设置 `ELECTRON_MIRROR` 与 `ELECTRON_BUILDER_BINARIES_MIRROR`**，只设后者仍会去 GitHub 拉 Electron 运行时并超时。

## 验收结果

- AC-001 至 AC-006、AC-008：通过。
- AC-007：**部分通过**——运行时行为已验证，Windows 安装包未产出。

## 失败与未验证项

1. **未运行：`frontend/apps/desktop` 的 Playwright Electron 用例**。本环境不允许 node spawn `cmd.exe`（实测 `error=ENOENT`，`C:\Windows\System32\cmd.exe` 文件存在），而 Playwright 在 Windows 上经 cmd.exe 启动 Electron。用例已保留，在普通 shell 或 CI 中应可运行。桌面端的运行时结论由上面的计数与静态服务探测给出。
3. **未运行：RabbitMQ 与 AI 接入相关检查**。本期未创建 `events` 与 `ai` 模块。
4. 未做性能与容量验证。

## 剩余风险

1. **授权隔离的端到端验证建立在测试作用域示例表之上**，不是真实业务实体。第一个带归属的业务功能落地后必须迁到真实实体，否则这条回归会逐渐失去代表性。
2. **前端构建产物未做体积预算与控制**。桌面端打包 Chromium 后安装包会明显偏大（Electron 的既定代价），当前无监控。
3. **`paideia.auth.secret` 缺失时生成一次性随机密钥并告警**。本地起服务很方便，但若有人把它部署到面向他人的环境而不配密钥，重启会让所有令牌失效——告警是唯一的防线，部署前需人工确认。
4. **`POST /api/v1/auth/token` 不校验凭据**，仅靠 profile（dev/local）把守。引入真实登录之前，任何把这两个 profile 用于对外环境的做法都会直接暴露该端点。
5. **多数据库适配已由用户决定不做**，数据访问层只面向 MySQL；将来若换库需重写数据访问层（这是已接受的取舍，不是意外）。
6. 依赖服务器的 SSH 隧道做集成测试，隧道不可用时相关用例静默跳过（有 stderr 提示），存在"本地看着绿、实际没验 SQL"的可能。
