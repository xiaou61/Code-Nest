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
3. 内容里不要放未转义的用户输入；如果前端需要保留换行并使用 `v-html`，按 [前端渲染安全](/reference/frontend-rendering-security) 先转义再净化。
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

如果你想把"一个动作成功后应该回到哪里"整体串起来，可以继续看 [事件、通知与回流索引](/reference/event-backflow-index)。这页会把通知中心和其他回流类型放到同一张总表里。

---

## 通知模块深度拆解

> 以下内容基于 `xiaou-notification` + `xiaou-common` 通知相关全部源码（2 个 ServiceImpl、2 个 Controller、5 个 Domain、4 个枚举、1 个 Mapper XML、1 个 AsyncConfig、1 个 CacheUtil、7 个 DTO）逐行拆解。

### 一、双层服务架构

通知模块采用"公共底座 + 业务模块"双层架构：

```
xiaou-common (公共底座)
  ├── NotificationService     — 基础 CRUD：发送、已读、删除
  ├── NotificationUtil        — 静态工具入口，所有业务模块调用
  ├── NotificationCacheUtil   — Redis 未读数缓存
  ├── NotificationAsyncConfig — 线程池配置
  ├── NotificationMapper      — 数据访问
  └── Domain/Enum             — 实体和枚举定义

xiaou-notification (业务模块)
  ├── NotificationUserService  — 用户侧：列表、未读数、详情、已读、删除
  ├── NotificationAdminService — 管理侧：统计、公告、批量发送、模板 CRUD
  ├── NotificationController   — 用户端 REST 接口
  └── AdminNotificationController — 管理端 REST 接口
```

**关键设计**：`NotificationUtil` 使用**静态方法 + @Autowired setter**注入 Spring Bean。这意味着所有业务模块只需 `NotificationUtil.sendSystemMessage(userId, title, content)` 一行代码，无需注入任何 Bean。

```java
// NotificationUtil 的注入模式
private static NotificationService notificationService;

@Autowired
public void setNotificationService(NotificationService notificationService) {
    NotificationUtil.notificationService = notificationService;
}
```

**问题**：静态方法 + Spring 注入存在时序风险。如果 Spring 容器初始化未完成时业务代码调用 `NotificationUtil`，会抛 `NullPointerException`。正常 Spring Boot 启动流程中不会出现这个问题，但在单元测试或手动调用时需注意。

### 二、公告与个人消息的阅读状态双轨机制

这是通知模块最核心的设计差异：

#### 2.1 个人消息已读

```
个人消息已读:
  UPDATE notification
  SET status = 'READ', read_time = NOW(), updated_time = NOW()
  WHERE id = #{id}
    AND (receiver_id = #{userId} OR receiver_id IS NULL)
    AND status = 'UNREAD'
```

- 直接修改主表 `status` 字段
- 因为个人消息只有一个接收者，修改主表不影响其他人

#### 2.2 公告已读

```
公告已读:
  1. 检查 readRecordMapper.selectByUserAndNotification(userId, messageId)
     → 已有记录 → return true（幂等）
  2. 无记录 → INSERT notification_user_read_record
     (userId, notificationId, readTime=NOW(), createdTime=NOW())
```

- 公告主表 `status` 保持 `UNREAD` 不变
- 每个"读过"的用户都有一条 `notification_user_read_record`
- 查询时用 LEFT JOIN 判断"是否已读"

#### 2.3 双轨查询 SQL（核心）

`selectByUserIdWithReadRecord` 是最关键的 SQL：

```sql
SELECT n.*,
       CASE
         WHEN n.receiver_id IS NULL AND r.id IS NOT NULL THEN 'read'
         WHEN n.receiver_id IS NULL AND r.id IS NULL THEN 'UNREAD'
         ELSE n.status
       END as status,
       CASE
         WHEN n.receiver_id IS NULL AND r.id IS NOT NULL THEN r.read_time
         ELSE n.read_time
       END as read_time
FROM notification n
LEFT JOIN notification_user_read_record r
  ON n.id = r.notification_id AND r.user_id = #{userId}
WHERE n.status != 'DELETED'
  AND (n.receiver_id = #{userId} OR n.receiver_id IS NULL)
```

**关键发现**：公告的已读状态通过 CASE 表达式**动态计算**，而不是存储在主表中。这意味着：
- 同一条公告，用户 A 看到 `UNREAD`，用户 B 看到 `read`
- 公告主表永远不会变成 `READ` 状态

#### 2.4 未读数统计 SQL

```sql
SELECT COUNT(*)
FROM notification n
LEFT JOIN notification_user_read_record r
  ON n.id = r.notification_id AND r.user_id = #{userId}
WHERE n.status != 'DELETED'
  AND (
    (n.receiver_id = #{userId} AND n.status = 'UNREAD')   -- 个人未读
    OR
    (n.receiver_id IS NULL AND r.id IS NULL)              -- 公告未读（没阅读记录）
  )
```

**坑点**：这个 LEFT JOIN 在通知量大时性能不佳——每次查未读数都要 JOIN 阅读记录表。如果公告数量很多（几千条），每个用户都要做一次 LEFT JOIN + 过滤。

### 三、全部标记已读的实现缺陷

**源码**：`NotificationUserService.markAllAsRead()`

```java
public boolean markAllAsRead() {
    Long userId = StpUserUtil.getLoginIdAsLong();
    List<Notification> unreadMessages = notificationService.getUserMessages(
        userId, NotificationStatusEnum.UNREAD.getCode(), null
    );
    if (unreadMessages != null && !unreadMessages.isEmpty()) {
        List<Long> messageIds = unreadMessages.stream()
            .map(Notification::getId).toList();
        return notificationService.batchMarkAsRead(messageIds, userId);
    }
    return true;
}
```

**关键问题**：

1. `getUserMessages` 调用的是 `selectByUserIdWithReadRecord`，但**传入 `status = "UNREAD"`**。这个 SQL 的 WHERE 条件里用 CASE 表达式计算状态，然后在 IF 条件里用 `#{status}` 过滤 CASE 结果。问题在于 CASE 返回的是小写 `'read'`（不是 `'READ'`），而传入的是大写 `'UNREAD'`。这会导致**公告的已读记录无法被正确过滤**——因为 CASE 返回的 `'read'` 不等于 `'UNREAD'`，所以已读公告不会出现在结果里，但**未读公告也不会被全部标记已读**，因为 CASE 返回 `'UNREAD'`（大写），确实匹配。

2. 更严重的是：`getUserMessages` 返回**全量未读消息**（没有 LIMIT），如果某用户有几千条未读消息，全部加载到内存再提取 ID 列表，可能造成内存压力。注释写着"限制1000条以免性能问题"但代码里**没有实现这个限制**。

3. `batchMarkAsRead` 内部对每个 messageId 逐一查询 `notificationMapper.selectById`，然后判断是个人消息还是公告分别处理。这是典型的 **N+1 查询**——先拿全部 ID，再逐条查详情。

### 四、管理员删除实现

**源码**：`NotificationAdminService.deleteMessage()`

```java
public boolean deleteMessage(Long messageId) {
    Notification notification = notificationMapper.selectById(messageId);
    if (notification != null) {
        int result = notificationMapper.deleteMessage(messageId, notification.getReceiverId());
        return result > 0;
    }
    return false;
}
```

**关键发现**：管理员删除使用 `deleteMessage` SQL，但这条 SQL 的 WHERE 条件是 `WHERE id = #{id} AND receiver_id = #{userId}`。管理员传的 `userId` 是 `notification.getReceiverId()`，也就是消息接收者。

对于**公告**（`receiverId = null`），`WHERE id = #{id} AND receiver_id = null` 在 SQL 中应该是 `WHERE id = #{id} AND receiver_id IS NULL`，但 MyBatis 参数 `#{userId}` 为 null 时，生成的 SQL 是 `receiver_id = null`，MySQL 中 `= null` 不等于 `IS NULL`。这意味着**管理员可能无法删除公告**。

对于**个人消息**（`receiverId = 具体用户ID`），管理员传的是接收者 ID，所以可以正常删除。

### 五、模板消息双重降级机制

**源码**：`NotificationUtil.sendTemplateMessage()`

```
sendTemplateMessage(receiverId, templateCode, params):
  try:
    1. notificationTemplateMapper.selectByCode(templateCode)
    2. 如果找到模板 → processTemplate(titleTemplate, params) + processTemplate(contentTemplate, params)
    3. sendPersonalMessage(receiverId, title, content, "TEMPLATE")
  catch Exception:
    1. 使用硬编码备用模板 getTemplateTitle(templateCode) + getTemplateContent(templateCode)
    2. processTemplate(备用模板, params)
    3. sendPersonalMessage(receiverId, title, content, "TEMPLATE")
```

**关键发现**：模板消息有**双层降级**：
1. 数据库没有模板 → 使用硬编码备用模板
2. 数据库查询异常 → 也使用硬编码备用模板

硬编码备用模板列表：

| templateCode | 标题模板 | 内容模板 |
| --- | --- | --- |
| `WELCOME` | 欢迎加入{platform} | 亲爱的{username}，欢迎加入我们的平台！ |
| `COMMUNITY_LIKE` | 您的帖子收到点赞 | 您的帖子《{postTitle}》收到了{likerName}的点赞 |
| `COMMUNITY_COMMENT` | 您的帖子收到评论 | 您的帖子《{postTitle}》收到了{commenterName}的评论 |
| `INTERVIEW_FAVORITE` | 收藏提醒 | 您收藏的面试题《{questionTitle}》已更新 |
| `SYSTEM_MAINTENANCE` | 系统维护通知 | 系统将于{maintenanceTime}进行维护，预计耗时{duration} |

模板变量使用简单的 `String.replace`，格式是 `{key}`。不是 SpEL、不是 FreeMarker，是最简单的字符串替换。如果模板中有 `{key}` 但参数里没有对应 key，`{key}` 会被替换为空字符串（因为 `entry.getValue() != null ? entry.getValue().toString() : ""`）。

### 六、异步线程池配置

**源码**：`NotificationAsyncConfig`

| 参数 | 值 | 说明 |
| --- | --- | --- |
| 核心线程数 | 5 | 日常并发 |
| 最大线程数 | 20 | 高峰扩容 |
| 队列容量 | 100 | 缓冲任务 |
| 线程名前缀 | `notification-` | 日志辨识 |
| KeepAlive | 60s | 扩容线程空闲回收 |
| 拒绝策略 | CallerRunsPolicy | 队列满时由调用线程执行 |
| 等待关闭 | true + 30s | 优雅停机 |

**关键发现**：`CallerRunsPolicy` 意味着如果线程池和队列都满了，发送通知的请求会由**调用方线程**（通常是业务线程）执行。这是合理的——通知不应该丢失，宁可慢一点也不能不发。但如果通知量持续很高，业务线程被阻塞可能导致主流程超时。

### 七、未读数缓存策略

**源码**：`NotificationCacheUtil`

| 维度 | Redis Key | TTL | 说明 |
| --- | --- | --- | --- |
| 未读数 | `notification:unread:{userId}` | 30 分钟 | 减少数据库查询 |

缓存读写流程：

```
getUnreadCount(userId):
  1. Redis GET notification:unread:{userId}
  2. 缓存命中 → 返回
  3. 缓存未命中 → DB 查询 countUnreadWithReadRecord
  4. Redis SET notification:unread:{userId}, TTL=30min
  5. 返回

sendPersonalMessage 成功后:
  → Redis DEL notification:unread:{userId}（清缓存）
```

**关键发现**：
1. 只有 `sendPersonalMessage` 成功后才清缓存。公告发送成功后**不清缓存**——因为公告是全站的，无法逐用户清缓存。
2. `markAsRead` 和 `deleteMessage` 后**不清缓存**——这意味着用户标记已读后，缓存的未读数不会立即更新，要等 30 分钟 TTL 过期后才会重新查 DB。
3. 缓存清除失败不影响主流程（catch 后静默处理）。

### 八、用户偏好配置被硬禁用

**源码**：`NotificationUtil.isUserAcceptType()`

```java
private static boolean isUserAcceptType(Long userId, String type) {
    // 强制所有用户接受所有类型的消息
    return true;
}
```

`NotificationConfigMapper` 有完整的配置 CRUD（按用户和类型查询是否启用），但 `isUserAcceptType` 直接返回 `true`。这意味着 `notification_config` 表的数据**完全不被使用**——用户无法真正关闭某种类型的通知。

注释说"现已强制所有用户接受所有类型的消息"，说明这是一个有意的产品决策，但代码层面保留了 `NotificationConfigMapper` 的完整接口，后续如果要恢复用户偏好功能，只需把 `isUserAcceptType` 改为查 DB 即可。

### 九、消息详情自动已读

**源码**：`NotificationUserService.getMessageDetail()`

```java
public Notification getMessageDetail(Long messageId) {
    Notification notification = notificationService.getMessageById(messageId);
    if (notification != null) {
        Long userId = StpUserUtil.getLoginIdAsLong();
        if (notification.getReceiverId() == null || notification.getReceiverId().equals(userId)) {
            if (NotificationStatusEnum.UNREAD.getCode().equals(notification.getStatus())) {
                notificationService.markAsRead(messageId, userId);
            }
            return notification;
        }
    }
    return null;
}
```

**关键发现**：
1. 查看详情时自动标记已读——这是"读即标记"模式，用户无法"看了但不标记已读"
2. 权限检查只看 `receiverId == null`（公告）或 `receiverId == userId`（个人消息），不看消息类型
3. 对于公告，`notification.getStatus()` 永远是 `UNREAD`（公告主表不改状态），所以每次查看公告详情都会触发 `markAsRead` → 写阅读记录
4. 返回的 `notification` 对象里的 `status` 字段仍然是主表值（`UNREAD`），没有用 CASE 表达式计算"该用户视角的已读状态"——前端可能拿到公告 `status = "UNREAD"` 但实际已读

### 十、统计口径差异

**源码**：`NotificationAdminService.getStatistics()`

有两种统计模式：

| 模式 | 触发条件 | 计算方式 |
| --- | --- | --- |
| 默认统计 | `startTime` 和 `endTime` 都为 null | `countTodayMessages` + `countMonthMessages` + `countByType`（全量） |
| 时间范围统计 | `startTime` 和 `endTime` 都有值 | `countByTimeRangeAndType`（按范围） |

**关键发现**：
1. 默认统计的 `announcementCount` 是**全量**统计（不限时间），不是"今日公告数"。这意味着 `todayTotal` 是今日发送数，但 `announcementCount` 是历史上所有公告数——口径不一致。
2. 时间范围统计时，`todayTotal` 和 `monthTotal` 置为 0，避免混淆。
3. `unreadTotal` 不受时间范围限制——始终是全站所有未读消息数。
4. `countByType` 和 `countByTimeRangeAndType` 的统计不区分个人消息的 `READ`/`UNREAD` 状态——只要没 `DELETED` 就计入。

### 十一、深度发现与坑点

#### 11.1 已确认的代码问题

| 编号 | 问题 | 位置 | 影响 |
| --- | --- | --- | --- |
| BUG-1 | 全部标记已读无数量限制 | `NotificationUserService.markAllAsRead` | 大量未读消息时内存压力 |
| BUG-2 | 全部标记已读 N+1 查询 | `NotificationService.batchMarkAsRead` | 逐条 selectById 判断消息类型 |
| BUG-3 | 公告 CASE 表达式返回小写 'read' | `NotificationMapper.xml:182` | 与传入大写 'UNREAD' 过滤条件不一致 |
| BUG-4 | 管理员删除公告可能失败 | `NotificationAdminService.deleteMessage` | `receiver_id = null` vs `IS NULL` |
| BUG-5 | 详情返回公告主表 status=UNREAD | `NotificationUserService.getMessageDetail` | 前端看到已读公告仍显示 UNREAD |
| BUG-6 | markAsRead/delete 后不清缓存 | `NotificationUserService` | 缓存未读数 30 分钟后才更新 |

#### 11.2 设计层面的潜在风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | 未读数 LEFT JOIN 性能 | 公告量大时，每次查未读数都 JOIN 阅读记录表 |
| RISK-2 | NotificationUtil 静态注入时序 | Spring 容器初始化前调用可能 NPE |
| RISK-3 | CallerRunsPolicy 阻塞业务线程 | 通知量持续高时业务线程可能被占 |
| RISK-4 | 公告无 TTL 清理机制 | 公告永远存在，`notification` 表无限增长 |
| RISK-5 | 阅读记录无 TTL 清理机制 | `notification_user_read_record` 无限增长 |
| RISK-6 | 模板变量简单字符串替换 | 不支持条件逻辑、循环等高级模板功能 |

#### 11.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | 公告阅读记录双轨 | 公告不改主表状态，用阅读记录表实现多人独立已读 |
| H-2 | 静态工具类入口 | 业务模块一行代码发通知，无需注入 Bean |
| H-3 | 模板双重降级 | DB 查不到或异常时，自动使用硬编码备用模板 |
| H-4 | 参数兜底 | 非法类型退回 PERSONAL，非法优先级退回 LOW，空 receiverId 放弃发送 |
| H-5 | CallerRunsPolicy | 队列满时由调用线程执行，保证通知不丢失 |
| H-6 | 未读数 Redis 缓存 | 30 分钟 TTL，减少频繁 LEFT JOIN 查询 |
| H-7 | 通知不影响主流程 | 所有发送方法都 try-catch，失败只记录日志 |

#### 11.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| 静态工具入口 | `NotificationUtil.java` — 所有 sendXxx 方法 + 模板降级 |
| 基础 CRUD | `NotificationService.java` — 发送、已读、删除 |
| 公告阅读记录 | `NotificationMapper.xml:163-208` — LEFT JOIN + CASE 表达式 |
| 用户侧接口 | `NotificationUserService.java` — 列表、未读数、详情自动已读 |
| 管理侧接口 | `NotificationAdminService.java` — 统计、公告、批量、模板 |
| 线程池配置 | `NotificationAsyncConfig.java` — 5/20/100/CallerRunsPolicy |
| 缓存策略 | `NotificationCacheUtil.java` — Redis 30min TTL |
