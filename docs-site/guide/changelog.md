# 变更日志

本项目遵循面向读者的变更记录方式，按版本归档重要功能、修复、工程治理、兼容性变化和迁移提示。

格式参考 [Keep a Changelog](https://keepachangelog.com/) 的结构，并结合本项目实际使用 `vX.Y.Z` 版本号。

完整变更日志请查看 GitHub 仓库：[CHANGELOG.md](https://github.com/xiaou61/Code-Nest/blob/main/CHANGELOG.md)

---

## 版本概览

| 版本 | 日期 | 主要变更 |
|------|------|---------|
| v2.2.2 | 2026-05-27 | GitHub 协作治理文档、文档站完善 |
| v2.2.1 | 2026-05-24 | 学习小组前后端字段对齐 |
| v2.1.2 | - | 聊天室消息限流和输入中事件降噪 |
| v2.1.1 | - | WebSocket 票据机制、CORS 配置化 |
| v2.1.0 | - | AI 学习成长驾驶舱、AI Runtime 治理中心 |
| v2.0.0 | - | AI 基础设施切换到 LangChain4j/LangGraph4j/LlamaIndex |

---

## v2.2.2 (2026-05-27)

### Added

- 新增 GitHub 协作治理文档：贡献指南、安全策略、行为准则、发布流程、Issue 模板和 PR 模板
- 新增文档站发布交接、环境变量、验证场景、模块手册和维护规范资料
- 新增 `.gitattributes`，统一跨平台文本换行策略

### Changed

- 规范 Pull Request、Issue、版本发布和文档同步流程，降低多模块协作时的遗漏风险
- 统一后端 Maven、管理端前端、用户端前端、文档站和 README 示例版本到 `v2.2.2`
- 修正 docs-site GitHub Actions 版本分支触发规则

### Fixed

- 对齐学习小组任务、打卡、讨论、成员、统计等前后端字段契约
- 清理本地生成脚本、构建日志和 Windows 保留名残留文件污染风险

---

## v2.2.1 (2026-05-24)

### Added

- 新增小组统计值 DTO，用于统一趋势、排行和聚合统计返回结构
- 新增申请列表中的小组头像字段
- 新增小组列表待审核申请数量聚合字段

### Changed

- 优化学习小组列表、详情、成员、排行榜、统计等接口响应字段兼容性
- 优化小组统计、排行、打卡、讨论和成员服务的数据聚合方式
- 优化前端操作成功后的任务、打卡和讨论列表刷新逻辑

### Fixed

- 对齐学习小组任务、打卡、讨论、成员、统计等前后端字段契约
- 修复小组任务创建和编辑时的任务类型、重复规则、目标值、日期范围等提交参数
- 修复打卡弹窗的任务选择、补卡日期、图片数组、内容字段和接口参数
- 修复小组列表排序参数与后端枚举不一致的问题
- 补齐讨论点赞记录模型、Mapper 和 SQL 表结构

---

## v2.1.2

### Added

- 新增聊天室消息限流和输入中事件降噪
- 新增前端失败态提示，便于用户识别限流或校验失败

### Fixed

- 强化文本、图片消息校验，避免非法消息进入数据库

---

## v2.1.1

### Changed

- 收紧文件接口访问权限
- WebSocket 握手切换为短期票据机制
- HTTP 与 WebSocket CORS 来源配置化

### Fixed

- 补齐聊天室 PONG 心跳和 TYPING 输入中事件链路
- 对 Markdown 和高风险 HTML 渲染场景接入净化或转义

---

## v2.1.0

### Added

- 新增 AI 学习成长驾驶舱
- 新增 AI Runtime 质量与治理中心
- 新增 Prompt、Schema、RAG、Regression 等治理视图

---

## v2.0.0

### Changed

- 移除历史 Coze 链路
- AI 基础设施统一切换到 LangChain4j、LangGraph4j 和 LlamaIndex 分层架构

### Added

- 新增 AI Prompt 模板化、版本化和可观测治理能力
- 新增知识库 sidecar 和 RAG 检索调试链路

---

## 相关文档

| 文档 | 说明 |
| --- | --- |
| [版本公告与交接模板](/guide/version-release-handoff-template) | 版本发布交接模板 |
| [版本交付实战样例](/guide/version-release-worked-examples) | 版本发布实战示例 |
| [Git Log 版更新记录](/guide/git-log-release-notes) | Git 提交记录 |
| [版本历史](/modules/version-history) | 用户端版本历史展示 |
