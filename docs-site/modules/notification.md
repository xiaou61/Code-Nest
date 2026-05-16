# 通知中心

通知中心负责站内消息、系统公告、模板消息、批量发送和阅读状态。它不是一个孤立模块：注册欢迎、资料变更、社区互动、审核结果、系统公告都可以通过它统一落库和展示。

## 功能入口

| 端 | 页面或接口 | 说明 |
| --- | --- | --- |
| 用户端 | `/notification` | 查看通知、未读数、标记已读、删除 |
| 管理端 | `/notification` | 公告、批量消息、模板、统计 |
| 后端模块 | `xiaou-notification` | 用户侧和管理侧通知接口 |
| 公共服务 | `xiaou-common` | `NotificationUtil`、实体、Mapper、异步服务 |

## 推荐学习顺序

通知中心要先分清“谁发、谁读、怎么读”：

1. 先读 `NotificationUtil`，理解业务模块为什么不直接操作通知表。
2. 再读 `NotificationService`，理解个人消息、公告、模板消息的基础落库方式。
3. 接着读 `NotificationUserService`，学习用户侧未读数、详情自动已读和删除。
4. 然后读 `NotificationAdminService`，理解公告、批量发送、模板和统计。
5. 最后读 `notification_user_read_record`，这是公告未读数最容易出错的地方。

## 源码地图

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

## 一次通知如何送达

| 步骤 | 说明 |
| --- | --- |
| 业务模块触发 | 例如注册欢迎、资料变更、社区互动或审核结果 |
| 调用工具类 | 优先调用 `NotificationUtil`，不要散落地直接写 Mapper |
| 参数兜底 | 非法类型退回 `PERSONAL`，非法优先级退回 `LOW` |
| 写通知表 | 个人消息写 `receiver_id`，公告 `receiver_id` 为空 |
| 异步发送 | 批量或异步方法进入通知线程池 |
| 用户读取 | 用户列表和未读数通过当前登录用户 ID 查询 |
| 标记已读 | 个人消息更新主表，公告写阅读记录 |

通知发送失败通常不应该阻断主业务。例如注册欢迎通知失败，不应该导致注册失败；社区点赞通知失败，也不应该导致点赞失败。

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

## 个人消息和公告的读写差异

| 动作 | 个人消息 | 公告 |
| --- | --- | --- |
| 创建 | 一条消息对应一个接收用户 | 一条消息面向全站用户 |
| 接收人 | `receiver_id = userId` | `receiver_id = null` |
| 是否已读 | 更新 `notification.status` | 写 `notification_user_read_record` |
| 删除 | 当前用户删除自己的消息 | 管理员删除公告主记录 |
| 未读统计 | 查个人未读消息 | 查公告减去当前用户阅读记录 |

如果未来要做“部门公告”或“分组公告”，不能直接复用全站公告语义，需要扩展接收范围表，否则未读数会混在一起。

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

## 排查路径

| 现象 | 优先检查 |
| --- | --- |
| 用户未读数偏大 | 公告阅读记录是否缺失 |
| 用户未读数偏小 | 个人消息是否被错误更新为全局已读 |
| 批量消息延迟 | 异步线程池是否正常，批量插入是否异常 |
| 模板消息文案不对 | 模板 `code` 是否存在，变量名是否匹配 `{key}` |
| 社区互动通知缺失 | 目标用户 ID 是否为空，发送异常是否被业务日志吞掉 |
| 公告详情打开后别人也已读 | 错误更新了公告主表状态，没有写阅读记录 |

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

## 验证清单

| 场景 | 预期 |
| --- | --- |
| 管理端发送公告 | `notification.receiver_id` 为空，用户端可见 |
| 用户读取公告详情 | 写入 `notification_user_read_record` |
| 管理端批量发送个人消息 | 每个目标用户都有个人消息 |
| 用户读取个人消息详情 | 主表状态变为 `READ` |
| 用户全部已读 | 当前用户的个人消息和公告阅读记录都正确处理 |
| 模板变量替换 | `{username}` 等变量被替换为传入参数 |
| 传空 `receiverId` 发个人消息 | 放弃发送并记录警告，不写脏数据 |
