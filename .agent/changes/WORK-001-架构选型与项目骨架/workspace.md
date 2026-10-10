---
base_commit: 5181245
---

# WORK-001 工作区归因

基准提交为 `5181245`。本工作项已完成，本表只用于满足归因完整性检查：表内改动都不属于它。

## 工作区归因

| 路径 | 归属 | 说明 |
| --- | --- | --- |
| .agent/changes/WORK-001-架构选型与项目骨架/workspace.md | current_work | 属 workspac 的归因表，随 HEAD 前移重写 |
| .agent/changes/WORK-002-前端组件化与管理端拆分/workspace.md | current_work | 属 workspac 的归因表，随 HEAD 前移重写 |
| .agent/changes/WORK-003-账号与认证/testing/evidence/ | current_work | WORK-003 验收证据截图 |
| .agent/changes/WORK-003-账号与认证/testing/evidence/auth-admin-login-after.png | current_work | WORK-003 验收证据截图 |
| .agent/changes/WORK-003-账号与认证/testing/evidence/auth-login-after.png | current_work | WORK-003 验收证据截图 |
| .agent/changes/WORK-003-账号与认证/testing/evidence/auth-login-before.png | current_work | WORK-003 验收证据截图 |
| .agent/changes/WORK-003-账号与认证/testing/evidence/auth-register-after.png | current_work | WORK-003 验收证据截图 |
| .agent/changes/WORK-003-账号与认证/workspace.md | current_work | 属 workspac 的归因表，随 HEAD 前移重写 |
| .agent/changes/WORK-004-知识库/ | current_work | WORK-004：知识库需求与下游草稿（用户暂停待批） |
| .agent/changes/WORK-004-知识库/design.md | current_work | WORK-004：知识库需求与下游草稿（用户暂停待批） |
| .agent/changes/WORK-004-知识库/proposal.md | current_work | WORK-004：知识库需求与下游草稿（用户暂停待批） |
| .agent/changes/WORK-004-知识库/requirements.md | current_work | WORK-004：知识库需求与下游草稿（用户暂停待批） |
| .agent/changes/WORK-004-知识库/tasks.md | current_work | WORK-004：知识库需求与下游草稿（用户暂停待批） |
| .agent/changes/WORK-004-知识库/testing/plan.md | current_work | WORK-004：知识库需求与下游草稿（用户暂停待批） |
| .agent/changes/WORK-004-知识库/workspace.md | current_work | 属 workspac 的归因表，随 HEAD 前移重写 |
| .agent/memory.md | current_work | 项目长期记忆 |
| .agent/history/updates.md | current_work | 项目更新历史（approve 每次会追加签署记录，因此它常处于未提交状态） |
| .agent/notes/ | current_work | 在途改动 |
| .agent/notes/deferred-scope.md | current_work | 在途改动 |
| backend/paideia-security/src/main/java/io/github/xiaou61/security/AuthProperties.java | current_work | WORK-003 在途：AuthPort 带角色与令牌寿命默认值 |
| backend/paideia-security/src/test/java/io/github/xiaou61/security/JwtAuthServiceTest.java | current_work | WORK-003 在途：AuthPort 带角色与令牌寿命默认值 |
| frontend/apps/admin/src/pages/AdminHomePage.tsx | current_work | WORK-003 在途：认证接线、登录/注册视图与登录门 |
| frontend/apps/app/src/pages/LoginPage.tsx | current_work | WORK-003 在途：认证接线、登录/注册视图与登录门 |
| frontend/apps/app/src/pages/RegisterPage.tsx | current_work | WORK-003 在途：认证接线、登录/注册视图与登录门 |
| frontend/packages/auth/src/auth-layout.tsx | current_work | WORK-003 在途：共享认证包 |
| frontend/packages/auth/src/index.ts | current_work | WORK-003 在途：共享认证包 |
| frontend/packages/auth/src/login-form.tsx | current_work | WORK-003 在途：共享认证包 |
| frontend/packages/auth/src/register-form.tsx | current_work | WORK-003 在途：共享认证包 |
| frontend/packages/ui/src/styles/globals.css | current_work | WORK-003 在途：补 auth 包的 @source 与覆盖检查 |
| frontend/packages/ui/src/styles/source-coverage.test.ts | current_work | WORK-003 在途：补 auth 包的 @source 与覆盖检查 |

## 说明

- 本表基准随 `5181245`（全库审查必修缺陷的修复）推进；上一版基准 `8ac11d9` 已被两笔提交取代。
- 提交时按说明列分开归属：不属于本工作项的路径不得在本工作项下提交。
- 并发写入提醒：`.agent/` 下另有会话在写入；修改共享工件前先重新读取。
