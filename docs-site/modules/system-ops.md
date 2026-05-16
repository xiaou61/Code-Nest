# 系统运营后台

系统运营后台覆盖管理端配置、日志、敏感词、通知、AI 配置和平台治理能力。

## 功能入口

| 功能 | 管理端入口 | 后端模块 |
| --- | --- | --- |
| 用户管理 | `/user` | `xiaou-system`、`xiaou-user` |
| 登录日志 | `/logs/login` | `xiaou-system` |
| 操作日志 | `/logs/operation` | `xiaou-system` |
| 通知管理 | `/notification` | `xiaou-notification` |
| 敏感词 | `/sensitive/*` | `xiaou-sensitive` |
| AI 配置 | `/system/ai-config` | `xiaou-system`、`xiaou-ai` |
| AI 治理 | `/system/ai-governance` | `xiaou-ai` |
| 版本管理 | `/system/version` | `xiaou-version` |
| 文件管理 | `/filestorage/*` | `xiaou-filestorage` |

## 敏感词风控

敏感词模块包括词库、白名单、策略、统计、来源、版本和配置。它是社区、动态、博客、聊天等内容型模块的基础风控能力。

后续文档需要写清：

- AC 自动机匹配链路。
- 变形词预处理。
- 词库热更新。
- 白名单和策略优先级。
- 命中日志和统计口径。

## 日志与审计

登录日志和操作日志用于排查账号行为、后台操作和安全问题。新增管理端功能时，应考虑是否需要记录关键操作日志。

## 通知

通知模块负责系统通知、私信和消息状态。它与社区互动、审核结果、学习任务、积分事件等功能存在联动空间。

