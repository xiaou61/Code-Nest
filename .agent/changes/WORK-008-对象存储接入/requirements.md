---
artifact: requirements
work_id: WORK-008
work: 对象存储接入
status: draft
mode: strict
mode_reason: 引入一个新的外部基础设施组件（S3 兼容对象存储）并改变附件读写路径与部署形态，涉及部署与数据（既有附件）；但底层 FileStorage 端口已存在，业务代码不受影响
workflow: full
depends_on: []
related_to: [WORK-004]
created: 2026-10-10
updated: 2026-10-10
---

# 对象存储接入需求

2026-10-10 登记。**用户决定：本期不实现**，仅建项留待后续。原话：「暂时不需要 这个先记录成一个work 我们后面再实现，你先记录上」。本工件是登记与范围草稿，**未经用户批准**，保持 `draft`。

## 编号说明

- Skill 工具当时给出的下一个可用号是 `WORK-005`，但 `WORK-005/006/007` 已被 `WORK-004` 需求文末与 `.agent/notes/deferred-scope.md` **在文字上保留**给「AI 精简与悬浮助手／Agent 运行时与记忆／个人笔记与只读分享」。
- 为不与那三个保留号相撞，本项取 **WORK-008**。若用户希望在编号上让那三项顺延、本项占 005，改一个目录名与 frontmatter 即可。

## 已确认事实（读源码与外部核实，2026-10-10）

- 附件存储现状 = **本地磁盘**，收在 `FileStorage` 端口（`backend/paideia-knowledge/.../internal/file/`）之后，唯一实现 `LocalFileStorage`：写 `paideia.knowledge.upload-dir`，文件名 = UUID，先写 `.part` 再 `ATOMIC_MOVE`。
- 读端点 `/files/{uuid}` **免鉴权**（全项目唯一），**UUID 不可枚举即凭据**。
- 天花板已写进代码与 `always.md` D-15：**单实例本地盘**；多实例部署必须换共享存储——端口就是为此留的。对象存储属 **D-15 暂缓**，重审触发 =「需要多实例共享或多副本」。
- **MinIO 社区版已停维护**：官方仓库 README 顶栏「THIS REPOSITORY IS NO LONGER MAINTAINED」，社区版仅发源码、不再提供预编译二进制，转向付费 AIStor。
- 用户 2026-10-10 明确点到的候选是 **RustFS**（Rust／Apache-2.0／S3 兼容／很新）；同类备选 **SeaweedFS**（Go／Apache-2.0／单二进制 `weed`）。

## 用户决定

1. **本期不实现**，先登记为工作项，后续再做。
2. 该能力被用户定性为**一个可扩展点**——即「每个外部能力后面挡一个端口、实现可换」的既有模式，**不是**通用插件运行时；后者与 D-02 Spring Modulith 的构建期边界相冲突，本项不做。

## 目标（草案）

- 让附件存储可换成 S3 兼容对象存储，接在既有 `FileStorage` 端口之后，**业务代码不改**。
- 保持免鉴权读端点的前提：**id 不可枚举**。

## 非目标（草案）

- 不引入通用插件运行时／SPI 热加载／插件注册表。
- 不在本项做多实例部署本身（本项只解决其前置件：共享存储）。

## 待确认问题

- 触发时机：立即做，还是等「多实例共享／多副本」真实出现（D-15 的触发条件）？
- 选型：RustFS，还是 SeaweedFS（或其它）。
- 既有本地附件是迁移，还是只对新上传生效。
- 服务暴露方式：服务器无 Docker、仅 8081 公网可达，对象存储端口须只绑回环。

## 关联工作项

- `related_to: WORK-004`（知识库）——`FileStorage` 端口与附件读写路径由它建立，本项是其后继演进，共享同一数据（既有附件）与同一读端点契约。不设 `depends_on`：本项不依赖 WORK-004 的未完成结果。
