---
artifact: test-report
work_id: WORK-004
work: 知识库
status: partial
evidence: required
created: 2026-10-10
updated: 2026-10-10
---

# 知识库验证报告

## 验证环境

- 后端：JDK 25 / Spring Boot 4.1.1；MyBatis 手写 mapper；MySQL 8.0.46（在自有服务器上，经本机 `127.0.0.1:3307` 的 SSH 隧道访问），验证库 `paideia_test`。
- 测试库口令从被忽略的 `backend/config/application-local.yml` 读入环境变量，**未回显、未进入任何被跟踪的文件**。
- 前端：pnpm 12.10.1 workspace；Playwright chromium；e2e 用一个手工启动的后端（各应用的 `reuseExistingServer: true` 复用它），后端启动实测 8.4 秒。
- 验证基准：`f0edc79`（测试节奏与上下文合并）。本轮新增的 `V952` 与两个 e2e spec 在验证时尚未提交。

## 验证结果

| 检查项 | 命令 | 退出码 | 结果 | 证据 |
| --- | --- | --- | --- | --- |
| AC-001 迁移与启动 | `cd backend && mvn -B verify` | 0 | passed | `testing/logs/backend-verify.txt`（含 `Successfully applied … version "3 - create knowledge tables"`） |
| AC-002 只读浏览与 md 渲染 | `pnpm --filter @paideia/app exec playwright test` | 0 | passed | `testing/logs/e2e-app.txt`（11 passed） |
| AC-003 中文检索、不返回草稿 | 同上 | 0 | passed | `testing/logs/e2e-app.txt` |
| AC-004 关系与邻域 | `mvn -B verify` 的关系用例 + 学习者端 e2e | 0 | passed | `testing/logs/backend-verify.txt`、`e2e-app.txt` |
| AC-005 管理端录入并发布 | `pnpm --filter @paideia/admin exec playwright test` | 0 | passed | `testing/logs/e2e-admin.txt`（7 passed） |
| AC-006 三态鉴权 | `mvn -B verify` 的 `KnowledgeAdminIntegrationTest` | 0 | passed | `testing/logs/backend-verify.txt` |
| AC-007 构建、边界检查、桌面产物不含管理端 | `pnpm -r typecheck`、`pnpm -r test`、`pnpm -r --filter '!@paideia/desktop' build`、产物文案检索 | 0 | passed | `testing/logs/build-apps.txt`、`testing/logs/artifact-exclusion.txt` |
| AC-008 上传与免鉴权读取 | `mvn -B verify` 的 `KnowledgeFileIntegrationTest` | 0 | passed | `testing/logs/backend-verify.txt` |
| AC-009 正文导航（目录／上下篇／反向链接） | 学习者端 e2e | 0 | passed | `testing/logs/e2e-app.txt` |
| AC-010 分类树展开与筛选 | 学习者端 e2e | 0 | passed | `testing/logs/e2e-app.txt` |
| AC-011 草稿预览 | 管理端 e2e + `mvn -B verify`（对学习者 403） | 0 | passed | `testing/logs/e2e-admin.txt`、`backend-verify.txt` |
| AC-012 自评可用 | 学习者端 e2e（刷新后仍在）+ `mvn -B verify` | 0 | passed | `testing/logs/e2e-app.txt`、`backend-verify.txt` |
| AC-013 自评隔离 | `mvn -B verify` 的 `KnowledgeSelfAssessmentIntegrationTest` | 0 | passed | `testing/logs/backend-verify.txt` |
| 反向验证：角色规则 + 自评过滤 | 临时改坏两处后 `mvn -B verify -Dtest='Knowledge*IntegrationTest'` | **1（预期）** | **咬住** | `testing/logs/reverse-verification-backend.txt`（一次运行报出 3 条失败，逐条可归因） |
| 反向验证：Tailwind `@source` 覆盖 | 删掉 `packages/knowledge/src` 那行后 `pnpm --filter @paideia/ui test` | **1（预期）** | **咬住** | `testing/logs/reverse-verification-source.txt`（直接点名 `packages\knowledge\src`） |

数量：**后端 124 条测试 0 失败、0 错误、0 跳过**（knowledge 22、app 58、account 13、security 11、persistence 4、platform 7、web 9），实测 91 秒；**前端单测 63 条**（core 14、ui 17、knowledge 15、auth 13、platform-desktop 4），`pnpm -r typecheck` 9 个包全过；**e2e 23 条**（学习者端 11、管理端 7、展览页 5）。

## 反向验证的逐条结论

三项咬住，一项**没有咬住**——后者比咬住更重要，照实记：

1. **角色路径规则**：注释掉 `hasRole('ADMIN')` 后，`learnerIsForbiddenOnAdminEndpoints` 与 `uploadRequiresAdmin` 都由"期望 403"变成"实际 200"。顺序或存在性写错都会被这条抓住。
2. **自评隔离**：去掉查询里的 `user_id` 条件后，`markingIsVisibleOnlyToTheMarkerOnBothReadPaths` 失败，证据里直接打出**用户 B 读到了用户 A 的标记**（`[{"entryId":1,"title":"什么是依赖注入","level":"understood",…}]`）。这正是"能标记、能看到自己的"这类断言在串号时照样通过的反面。
3. **Tailwind `@source`**：删掉那行后 `source-coverage` 失败并点名缺登记的目录。
4. **`WITH PARSER ngram`：没有咬住。** 摘掉解析器后，`chineseKeywordSearchHitsThroughFulltextIndex` **仍然通过**——说明"中文能搜到"这条断言**对解析器不敏感**，而设计里"不加 ngram 中文基本不召回"的说法**没有得到本次实验支持**。索引已还原、探针迁移已删除。这不是"没做"，是"做了但结论与预期相反"，因此列入下面的剩余风险而不是当作已验证。

## 失败与未验证项

1. **桌面安装包（electron-builder）未构建**。`pnpm -w build:desktop` 未执行：实测一轮接近 20 分钟（`apps/desktop` 的 build 会下载 Electron 再打包），按本轮新定的「测试节奏」它属于最外环（发布前）。**目标性质已用更便宜的方式证明**：桌面壳打包的正是 `apps/app/dist`（见 `apps/desktop/package.json` 的 build 脚本），而管理端独有文案「知识库管理」在学习者端产物里出现 **0** 次、在管理端产物里 **1** 次、学习者端独有文案「我标记过的内容」出现 **1** 次（证明检索方法有效）——因此管理端不可能进入桌面产物。见 `testing/logs/artifact-exclusion.txt`。**报告因这一条停在 `partial`**：AC-007 的措辞含"桌面产物"，我不在没跑打包的情况下宣布它已完全验证。
2. 未做性能与容量测试、未做并发写冲突验证（不在本项范围）。

## 剩余风险

- **`WITH PARSER ngram` 的必要性待重新评估**：反向验证未咬住（见上）。当前索引带 ngram，功能正常，因此**不构成立即风险**；但它意味着设计里的这条理由没有证据，后续若要简化 schema 或换检索方案，需要先弄清楚"什么查询在什么分词下会失效"。
- **附件读取端点免鉴权**（用户在 2026-10-10 明确裁决接受）：未发布条目引用的图片，URL 一旦泄露即可被读。缓解是不可枚举的 UUID 与不提供目录列举。触发重评：出现他人的真实内容或对外网开放时。
- **附件目录默认落系统临时目录**并告警（与设计里"未配置即启动失败"不同，理由是本地与测试环境都没有该变量）。生产必须显式配置 `paideia.knowledge.upload-dir`。
- **孤儿附件不清理**：删条目不删文件，本版不做定时清理，也不做容量告警。
- **自评只有两个状态、不参与任何排程**：它是学习者模型的第一块砖，不是掌握度推断。
- **证据文件在本机**：`testing/logs/` 被 `.gitignore` 忽略（与 WORK-001/002/003 一致），换一台机器看不到这些日志。
