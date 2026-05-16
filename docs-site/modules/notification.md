# 通知中心

通知中心负责站内消息、系统公告、模板消息、批量发送和阅读状态。它不是一个孤立模块：注册欢迎、资料变更、社区互动、审核结果、系统公告都可以通过它统一落库和展示。

## 功能入口

| 端 | 页面或接口 | 说明 |
| --- | --- | --- |
| 用户端 | `/notification` | 查看通知、未读数、标记已读、删除 |
| 管理端 | `/notification` | 公告、批量消息、模板、统计 |
| 后端模块 | `xiaou-notification` | 用户侧和管理侧通知接口 |
| 公共服务 | `xiaou-common` | `NotificationUtil`、实体、Mapper、异步服务 |

## 读源码先看哪里

| 位置 | 作用 |
| --- | --- |
| `xiaou-notification/src/main/java/com/xiaou/notification/controller/NotificationController.java` | 用户侧通知接口 |
| `xiaou-notification/src/main/java/com/xiaou/notification/controller/AdminNotificationController.java` | 管理侧通知接口 |
| `xiaou-notification/src/main/java/com/xiaou/notification/service/NotificationUserService.java` | 用户侧列表、未读、已读、删除 |
| `xiaou-notification/src/main/java/com/xiaou/notification/service/NotificationAdminService.java` | 后台统计、批量发送、模板 |
| `xiaou-common/src/main/java/com/xiaou/common/service/NotificationService.java` | 通知发送、已读、删除基础服务 |
| `xiaou-common/src/main/java/com/xiaou/common/utils/NotificationUtil.java` | 业务模块调用的工具入口 |
| `xiaou-common/src/main/java/com/xiaou/common/config/NotificationAsyncConfig.java` | 通知异步线程池 |

## 用户侧接口

| 接口 | 能力 |
| --- | --- |
| `POST /notification/list` | 分页查询当前用户消息 |
| `GET /notification/unread-count` | 获取未读数量 |
| `GET /notification/{id}` | 获取消息详情，未读时自动标记已读 |
| `POST /notification/mark-read` | 单条、批量或全部标记已读 |
| `POST /notification/delete` | 删除当前用户消息 |
| `POST /notification/mark-all-read` | 全部已读 |

用户身份来自 `StpUserUtil.getLoginIdAsLong()`，所以前端不需要传用户 ID。用户只能看自己的个人消息，公告类消息通过阅读记录判断是否已读。

## 管理侧接口

| 接口 | 能力 |
| --- | --- |
| `POST /admin/notification/announcement` | 发送全站公告 |
| `POST /admin/notification/statistics` | 通知统计 |
| `POST /admin/notification/list` | 后台查看通知列表 |
| `POST /admin/notification/batch-send` | 批量发送个人消息 |
| `POST /admin/notification/delete/{id}` | 管理员删除通知 |
| `GET /admin/notification/templates` | 模板列表 |
| `POST /admin/notification/templates` | 新增模板 |
| `PUT /admin/notification/templates/{id}` | 更新模板 |
| `DELETE /admin/notification/templates/{id}` | 删除模板 |

后台批量发送会调用 `NotificationUtil.sendBatchMessage`，底层异步批量插入通知表。

## 通知类型

| 类型 | 含义 | 常见场景 |
| --- | --- | --- |
| `PERSONAL` | 个人消息 | 后台批量消息 |
| `ANNOUNCEMENT` | 系统公告 | 全站公告 |
| `COMMUNITY_INTERACTION` | 社区互动 | 点赞、评论、收藏 |
| `INTERVIEW_REMINDER` | 面试题提醒 | 题库更新、收藏提醒 |
| `SYSTEM` | 系统通知 | 注册欢迎、资料变更、密码修改 |
| `AUDIT_RESULT` | 审核结果 | 学习资产、内容审核 |
| `ACTIVITY_NOTIFICATION` | 活动通知 | 抽奖、活动 |
| `TEMPLATE` | 模板消息 | 用模板变量生成标题和内容 |

## 状态、优先级和来源

| 维度 | 可选值 |
| --- | --- |
| 状态 | `UNREAD`、`READ`、`DELETED` |
| 优先级 | `LOW`、`MEDIUM`、`HIGH` |
| 来源模块 | `system`、`community`、`interview`、`user`、`filestorage`、`monitor` |

个人消息的 `receiver_id` 是用户 ID。公告的 `receiver_id` 为空，表示所有用户都能看到。

## 公告为什么要有阅读记录

个人消息可以直接把 `notification.status` 从 `UNREAD` 改成 `READ`，因为它只属于一个人。

公告不行。公告只有一条记录，但所有用户都能看到。如果直接把公告状态改成已读，其他用户也会被“顺手已读”。所以项目用 `notification_user_read_record` 记录“某个用户读过某条公告”。

| 消息类型 | 已读实现 |
| --- | --- |
| 个人消息 | 更新 `notification.status` 和 `read_time` |
| 全站公告 | 插入 `notification_user_read_record` |

这也是未读数查询要走 `countUnreadWithReadRecord` 的原因：它必须同时考虑个人消息状态和公告阅读记录。

## 发送方式

业务模块通常不直接操作 Mapper，而是调用 `NotificationUtil`：

| 方法 | 说明 |
| --- | --- |
| `sendAnnouncement` | 发送全站公告 |
| `sendPersonalMessage` | 发送个人消息 |
| `sendBatchMessage` | 批量发送 |
| `sendTemplateMessage` | 模板消息 |
| `sendMessageAsync` | 异步发送 |
| `sendCommunityMessage` | 社区互动消息 |
| `sendInterviewMessage` | 面试题相关消息 |
| `sendSystemMessage` | 系统通知 |
| `getUnreadCount` | 获取未读数，优先读缓存 |

工具类会做一些兜底，例如消息类型不合法时退回 `PERSONAL`，优先级不合法时退回 `LOW`，接收者为空时放弃发送个人消息并记录警告。

## 模板消息

模板表是 `notification_template`，核心字段：

| 字段 | 说明 |
| --- | --- |
| `code` | 模板编码，唯一 |
| `name` | 模板名称 |
| `title_template` | 标题模板 |
| `content_template` | 内容模板 |
| `is_enabled` | 是否启用 |

模板变量使用 `{key}` 形式替换。例如参数里有 `username = "小欧"`，模板中的 `{username}` 会被替换为“小欧”。如果数据库没有对应模板，`NotificationUtil` 会使用内置备用模板。

## 统计口径

后台统计在 `NotificationAdminService.getStatistics`：

| 指标 | 说明 |
| --- | --- |
| `todayTotal` | 今日发送总数 |
| `monthTotal` | 本月发送总数 |
| `announcementCount` | 公告数量 |
| `personalCount` | 个人消息数量 |
| `communityCount` | 社区互动数量 |
| `systemCount` | 系统消息数量 |
| `unreadTotal` | 全站未读总数 |

如果请求带了开始和结束时间，类型统计会按时间范围计算，`todayTotal` 和 `monthTotal` 会置为 0，避免语义混乱。

## 核心数据表

| 表 | 说明 |
| --- | --- |
| `notification` | 通知主表 |
| `notification_template` | 模板表 |
| `notification_config` | 通知配置表 |
| `notification_user_read_record` | 公告阅读记录表 |

## 常见坑

| 问题 | 原因 | 建议 |
| --- | --- | --- |
| 公告未读数不对 | 没有考虑阅读记录表 | 查 `notification_user_read_record` |
| 批量发送没有立刻显示 | 批量发送走异步线程池 | 查异步线程池和应用日志 |
| 模板变量原样展示 | 参数 key 和 `{key}` 不一致 | 对齐模板变量名 |
| 用户没收到社区通知 | `receiverId` 为空会被保护性忽略 | 发送前确认目标用户 ID |
| 未读数缓存不刷新 | 发送后缓存清理失败不会阻断主流程 | 可手动清理 Redis 中未读缓存 |

## 新业务接入建议

1. 先确定通知类型和来源模块。
2. 个人消息必须有 `receiverId`。
3. 内容里不要放未转义的用户输入。
4. 主流程不要依赖通知发送成功，通知失败应只影响消息展示。
5. 可复用的消息优先建模板，避免文案散落在业务代码里。
