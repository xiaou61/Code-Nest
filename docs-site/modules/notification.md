# 通知中心

通知中心负责站内消息、公告、批量发送、模板和阅读状态。

## 功能入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/notification` |
| 管理端 | `/notification` |
| 后端模块 | `xiaou-notification` |

## 用户侧接口域

| 接口 | 能力 |
| --- | --- |
| `/notification/list` | 消息列表 |
| `/notification/unread-count` | 未读数量 |
| `/notification/{id}` | 消息详情 |
| `/notification/mark-read` | 标记已读 |
| `/notification/delete` | 删除消息 |
| `/notification/mark-all-read` | 全部已读 |

## 管理侧接口域

| 接口 | 能力 |
| --- | --- |
| `/admin/notification/announcement` | 发送公告 |
| `/admin/notification/statistics` | 通知统计 |
| `/admin/notification/list` | 通知列表 |
| `/admin/notification/batch-send` | 批量发送 |
| `/admin/notification/delete/{id}` | 删除通知 |
| `/admin/notification/templates` | 模板列表、新增、更新、删除 |

## 联动场景

- 审核结果通知。
- 聊天室公告。
- 积分变动。
- 计划和小组任务提醒。
- 系统版本发布。
- AI 回归或治理风险提醒。

## 开发注意

- 批量发送要考虑异步化和失败重试。
- 未读数量要避免频繁全表统计。
- 模板变量要做转义。
- 删除可以考虑用户侧软删除和管理侧审计。

