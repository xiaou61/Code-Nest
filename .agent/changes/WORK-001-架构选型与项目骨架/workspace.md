---
base_commit: 85bb6562758d7e05e7f68d52a980576cd891f937
---

# WORK-001 工作区归因

基准提交为 `85bb656`（用户提交：`chore: 初始化 Paideia 项目脚手架`）。本表登记该提交之后工作区中所有未提交改动，避免恢复时把谁的改动认错。

## 工作区归因

| 路径 | 归属 | 说明 |
| --- | --- | --- |
| .gitignore | current_work | 本轮新增：公开仓库的敏感文件忽略规则（.env、*.local、.secrets/、私钥、构建产物） |
| .agent/changes/WORK-001-架构选型与项目骨架/proposal.md | current_work | 新增：架构提案（仓库结构、后端通用模块集合与依赖方向、Maven 粒度两方案、前端 workspace、端到端技术链路、交付六片） |
| AGENTS.md | current_work | 本轮修订：新增「保密规则（公开仓库）」章节；说明凭据误提交只能靠轮换补救，并建议启用 GitHub Secret scanning 与 Push protection |
| .agent/references/technology-options.md | current_work | 新增：通用能力候选清单 + D-01..D-15 技术选项登记表（备选与优缺点，不含结论）+ 选项间客观约束关系 |
| .agent/changes/WORK-001-架构选型与项目骨架/requirements.md | current_work | 本轮修订：记录四轮用户裁决（D-01..D-08、D-10..D-14），暂缓项标记，REQ-003/007、AC-005/007 与假设随之修订 |
| .agent/rules/always.md | current_work | 本轮修订：写入已裁决技术栈与选型、公开仓库保密约束、RabbitMQ 与 outbox 中继约束；仍为 draft 待确认 |
| .agent/history/updates.md | current_work | 本轮追加更新记录 |

## 已移除

| 路径 | 处理 | 原因 |
| --- | --- | --- |
| .agent/changes/WORK-001-架构选型与项目骨架/proposal.md | 已删除（内容重构进 technology-options.md） | 该文件在需求未批准、技术选型未裁决时就写下了架构结论并划分了业务模块；业务模块划分依赖需求，属越权，故撤下 |

## 保密与凭据

- 本仓库为公开仓库。用户曾在于会话中提供服务器 root 凭据，**未写入任何文件、未提交、未记入更新历史**。
- 已核查：使用 `git grep` 扫描已跟踪文件，未发现服务器地址或凭据泄露。
- 已核查：`.gitignore` 对 `.env`、`.env.local`、`application-local.yml`、`.secrets/server.env`、`src-tauri/target/**` 均生效。
- 建议由用户轮换该凭据并使用 SSH 密钥与普通用户；后续如需 Agent 访问服务器，凭据由用户放入被忽略的本地文件后由 Agent 读取，不再经会话传递。

## 备注

- 以上改动全部由 `WORK-001` 架构选型工作产生，没有用户既有或来源不明的改动。
- 工作区改动**未提交**：本地提交需用户明确授权。
- 远端 `origin` 指向公开仓库（地址见 `.git/config`，不写入本项目工件）；当前分支 `master` 未设置上游跟踪，**未执行任何推送**。
- **未创建任何源码**：`backend/`、`frontend/`、`deploy/` 尚未落地，仓库当前仍只有文档与工件。
