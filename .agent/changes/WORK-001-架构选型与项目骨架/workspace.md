---
base_commit: 39cf6d1
---

# WORK-001 工作区归因

基准提交为 `39cf6d1`（TASK-006）。本表登记该提交之后、本次提交之前的工作区改动。

## 工作区归因

| 路径 | 归属 | 说明 |
| --- | --- | --- |
| frontend/apps/public/ | current_work | TASK-007：新增公开页 surface（两遍构建 + 预渲染脚本 + 复用 packages/ui 的页面） |
| frontend/pnpm-lock.yaml | current_work | TASK-007：新增公开页工作区成员后的锁文件更新 |
| .agent/changes/WORK-001-架构选型与项目骨架/testing/ | current_work | TASK-008：测试计划、验证报告（状态 partial，带结构化证据矩阵）与四个证据日志 |
| .agent/changes/WORK-001-架构选型与项目骨架/tasks.md | current_work | TASK-007 状态改为 done；TASK-008 状态改为 done 并记录结果 |
| .agent/rules/always.md | current_work | TASK-008：把占位的构建与检查命令替换为实测命令，并写明运行前置条件 |
| .agent/INDEX.md | current_work | TASK-008：登记后端 6 项与前端 8 项模块的位置与测试文件 |
| .agent/history/updates.md | current_work | 追加本轮更新记录 |

## 未纳入版本控制

- `backend/config/application-local.yml`（数据库口令与本地签名密钥，被忽略）
- `frontend/apps/desktop/renderer/`、`frontend/apps/desktop/release/`、`frontend/apps/public/dist/`、`frontend/apps/public/dist-ssr/`、各 `node_modules/`、后端 `target/`、`test-results/`

## 备注

- 本次提交后 WORK-001 **仍未完成**：AC-007 的 Windows 安装包未产出（electron-builder 需从 GitHub 拉取 NSIS 与签名辅助二进制，本机对 github.com 持续超时；已尝试三次，含 npmmirror 镜像与 `--dir` 模式），对应的任务 TASK-006 记为 `blocked`。
- 验证证据已落盘：`testing/logs/backend-verify.txt`、`frontend-checks.txt`、`frontend-surfaces.txt`、`runtime-and-desktop.txt`。
- 并发写入提醒：`.agent/` 下另有会话在写入；修改共享工件前先重新读取。
