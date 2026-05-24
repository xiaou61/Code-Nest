# IM 聊天室

IM 聊天室提供官方聊天室、实时消息、在线状态、图片消息、回复、撤回、禁言、踢人和公告能力。v2.1.2 之后，聊天室重点补齐了 WebSocket 一次性票据、心跳响应、输入中广播、消息限流和服务端消息校验，读源码时要把“连接安全”和“消息可靠性”放在一起看。

## 推荐学习顺序

聊天模块建议按“连接 -> 消息 -> 在线态 -> 管理动作 -> 生产化风险”阅读：

1. 先看一次性票据，理解为什么前端要先调用 `/user/chat/ws-ticket`，再连接 `/ws/chat?ticket=...`。
2. 再看消息发送、限流和 `MESSAGE_ACK`，理解前端临时消息如何替换成正式消息，失败消息如何回写。
3. 接着看 `HEARTBEAT`、`PONG`、Redis 在线态和超时清理，理解在线人数为什么不能只看数据库。
4. 然后看 `TYPING`、撤回、禁言、踢出和公告，理解用户动作和管理动作如何广播。
5. 最后看多实例广播和生产配置，区分当前单实例已落地能力和集群部署要补的能力。

## 主要入口

| 端 | 入口 | 说明 |
| --- | --- | --- |
| 用户端页面 | `/chat` | 聊天主界面，包含历史消息、在线用户、文本/图片发送和撤回 |
| 用户端 API | `/user/chat/**` | 历史消息、在线人数、在线用户、撤回 |
| WebSocket 票据 | `POST /user/chat/ws-ticket` | 用户登录态换取 60 秒一次性票据 |
| WebSocket | `/ws/chat?ticket=<ticket>` | 握手消费一次性票据，不再把长期 Token 放到 URL |
| 管理端页面 | `/chat/messages`、`/chat/users` | 消息审计、删除、在线用户、禁言、踢出、公告 |
| 管理端 API | `/admin/chat/**` | 管理员操作入口 |
| 后端模块 | `xiaou-chat` | WebSocket、消息、在线态、禁言和定时任务 |

## 源码地图

| 文件 | 职责 |
| --- | --- |
| `xiaou-chat/src/main/java/com/xiaou/chat/config/WebSocketConfig.java` | 注册 `/ws/chat`，绑定握手拦截器和处理器 |
| `xiaou-chat/src/main/java/com/xiaou/chat/websocket/SaTokenWebSocketInterceptor.java` | 从 query string 读取一次性 `ticket`，消费票据后写入用户信息 |
| `xiaou-chat/src/main/java/com/xiaou/chat/service/ChatWebSocketTicketService.java` | 生成和消费 60 秒一次性 WebSocket 握手票据 |
| `xiaou-chat/src/main/java/com/xiaou/chat/websocket/ChatWebSocketHandler.java` | 连接、心跳、PONG、输入中、消息广播、系统消息、踢人、撤回/删除通知 |
| `xiaou-chat/src/main/java/com/xiaou/chat/service/ChatRateLimitService.java` | 消息和输入中事件的 Redis 固定窗口限流 |
| `xiaou-chat/src/main/java/com/xiaou/chat/service/impl/ChatMessageServiceImpl.java` | 消息入库、历史查询、撤回、删除、公告 |
| `xiaou-chat/src/main/java/com/xiaou/chat/service/impl/ChatOnlineUserServiceImpl.java` | Redis 在线态、心跳、踢出、超时清理 |
| `vue3-user-front/src/views/chat/Index.vue` | 用户端聊天室完整交互 |

## 当前连接流程

1. 用户登录后，前端确认用户 store 中存在登录态。
2. 前端调用 `POST /user/chat/ws-ticket`，后端通过当前登录用户生成一次性票据。
3. `ChatWebSocketTicketService` 生成 32 字节随机值，写入 Redis：`xiaou:chat:ws-ticket:{ticket} -> userId`，TTL 为 60 秒。
4. 前端建立连接，等价于：`new WebSocket(WS_URL + '/ws/chat?ticket=' + encodeURIComponent(ticket))`。
5. `SaTokenWebSocketInterceptor` 从 URL query 中解析 `ticket`，调用 `consumeTicket` 读取并删除 Redis 票据。
6. 票据有效时，拦截器把 `userId`、`username` 放入 WebSocket session attributes。
7. `ChatWebSocketHandler.afterConnectionEstablished` 将 session 放入进程内 `SESSIONS`。
8. 服务端调用 `ChatOnlineUserService.userOnline` 写入 Redis 在线态和数据库在线记录。
9. 服务端向当前用户发送 `CONNECT`，向所有用户广播 `ONLINE_COUNT` 和 `USER_JOIN`。

当前实现里 `WebSocketConfig` 使用 `setAllowedOriginPatterns`，默认允许本地开发地址，并支持通过 `xiaou.cors.allowed-origin-patterns` 扩展。生产部署时应显式配置业务域名，避免回退到过宽的来源策略。

## WebSocket 事件

事件常量定义在 `WebSocketMessage.MessageType`。

| 事件 | 方向 | 当前状态 | 说明 |
| --- | --- | --- | --- |
| `CONNECT` | 服务端到客户端 | 已实现 | 握手成功，返回用户信息 |
| `MESSAGE` | 双向 | 已实现 | 客户端发送消息；服务端广播已入库消息 |
| `MESSAGE_ACK` | 服务端到发送者 | 已实现 | 返回正式消息数据，用于替换前端乐观消息 |
| `SYSTEM` | 服务端到客户端 | 已实现 | 系统公告 |
| `ONLINE_COUNT` | 服务端到客户端 | 已实现 | 在线人数变化 |
| `USER_JOIN` | 服务端到客户端 | 已实现 | 用户加入聊天室 |
| `USER_LEAVE` | 服务端到客户端 | 已实现 | 用户离开聊天室 |
| `MESSAGE_RECALL` | 服务端到客户端 | 已实现 | 用户撤回消息后广播 |
| `MESSAGE_DELETE` | 服务端到客户端 | 已实现 | 管理员删除消息后广播 |
| `KICK_OUT` | 服务端到客户端 | 已实现 | 管理员踢出用户 |
| `HEARTBEAT` | 客户端到服务端 | 已实现 | 服务端更新 Redis 心跳时间 |
| `PONG` | 服务端到客户端 | 已实现 | 服务端响应心跳，前端用于计算延迟和调整心跳间隔 |
| `TYPING` | 客户端到服务端，服务端广播 | 已实现 | 输入中事件，服务端限流后广播给其他在线用户 |
| `ERROR` | 服务端到客户端 | 已实现 | 消息处理失败、限流或消息校验失败 |

教学理解：`MESSAGE_ACK` 是聊天室最关键的事件之一。前端发送消息时会先生成 `tempId` 并显示“发送中”，服务端入库后返回正式消息对象。后续撤回必须使用正式 `id`，如果只保留临时 ID，撤回接口会找不到数据库记录。

## 消息发送流程

文本消息：

1. 前端校验输入不为空。
2. 前端生成 `tempId`，把本地消息插入消息列表，状态为 `sending`。
3. 前端通过 WebSocket 发送：

```json
{
  "type": "MESSAGE",
  "data": {
    "messageType": 1,
    "content": "你好",
    "tempId": "temp_xxx",
    "replyToId": null
  }
}
```

4. 服务端读取当前用户、IP、User-Agent。
5. `ChatRateLimitService.tryAcquireMessage` 执行固定窗口限流，默认每个用户 10 秒最多 8 条消息。
6. `ChatMessageServiceImpl.sendMessage` 获取官方聊天室，检查用户是否被禁言，并校验消息类型、文本长度和图片 URL。
7. 服务端写入 `chat_messages`，再查出带用户信息的完整消息。
8. 服务端向其他在线用户广播 `MESSAGE`。
9. 服务端向发送者发送 `MESSAGE_ACK`。
10. 前端收到 ACK 后，把本地消息状态改为 `sent`，并更新为正式消息 ID。

如果命中限流或消息校验失败，服务端发送 `ERROR`，`data.code` 可能是 `RATE_LIMITED` 或 `MESSAGE_REJECTED`，并尽量携带 `tempId`。前端会把对应本地乐观消息标记为 `failed`，展示错误提示，用户可以修改后重发。

图片消息：

1. 前端先校验文件是图片且不超过 5MB。
2. 前端通过 `/file/upload/single` 上传，参数为 `moduleName=chat`、`businessType=message`。
3. 上传成功后，WebSocket 发送 `messageType=2` 和 `imageUrl`。
4. 后端消息服务把图片 URL 写入 `chat_messages.image_url`。

回复消息：

如果 `replyToId` 有值，服务端会查询被回复消息，并写入：

| 字段 | 说明 |
| --- | --- |
| `reply_to_id` | 被回复消息 ID |
| `reply_to_user` | 被回复消息发送者昵称 |
| `reply_to_content` | 被回复内容摘要，最长 50 字；图片显示为 `[图片]` |

## 消息类型

当前服务代码按这些数值使用消息类型：

| 值 | 类型 | 说明 |
| --- | --- | --- |
| `1` | 文本 | 普通聊天文本 |
| `2` | 图片 | 图片消息，内容主要在 `image_url` |
| `3` | 系统消息 | 管理员公告或系统广播 |
| `4` | 前端系统提示 | 前端本地用于用户加入/离开提示，不一定入库 |

## 在线态存储

在线用户由 `ChatOnlineUserServiceImpl` 同步维护 Redis 和数据库。

| Redis Key | 类型 | 内容 |
| --- | --- | --- |
| `chat:online:users` | Hash | `sessionId -> userInfo JSON` |
| `chat:online:heartbeat` | Hash | `sessionId -> timestamp` |
| `chat:room:{roomId}:users` | Set | 房间内所有 `sessionId` |

上线时会先清理同一用户在同一房间的旧会话，避免一个用户重复在线。心跳只更新 Redis，不写数据库。超时阈值是 90 秒，`ChatScheduledTask.cleanTimeoutUsers` 每 30 秒扫描一次并清理 Redis 与数据库残留。

客户端发送 `HEARTBEAT` 后，服务端会立即返回 `PONG` 并带上 `timestamp`。用户端用 PONG 计算最近延迟，网络较好时拉长心跳间隔，网络较差时缩短心跳间隔；连续丢失超过阈值后会主动重连。

输入中状态使用 `TYPING`。前端输入时发送事件，服务端通过 `ChatRateLimitService.tryAcquireTyping` 做限流，默认每 10 秒最多 12 次，然后广播给除发送者之外的在线用户。

## 撤回、删除和禁言

| 动作 | 接口 | 规则 |
| --- | --- | --- |
| 查看历史 | `POST /user/chat/history` | 基于 `lastMessageId` 和 `pageSize` 向上翻页，返回后服务端反转为旧消息在前 |
| 在线人数 | `POST /user/chat/online-count` | 读取官方聊天室 Redis set 大小 |
| 在线用户 | `POST /user/chat/online-users` | 读取 Redis 在线用户信息，同时清理孤儿 session |
| 用户撤回 | `POST /user/chat/message/recall` | 只能撤回自己的消息，发送 120 秒内可撤回 |
| 管理删除 | `DELETE /admin/chat/messages/{id}` | 物理删除消息并广播 `MESSAGE_DELETE` |
| 批量删除 | `POST /admin/chat/messages/batch-delete` | 批量物理删除 |
| 踢出用户 | `POST /admin/chat/users/kick` | 清理在线态，发送 `KICK_OUT`，关闭 WebSocket |
| 禁言用户 | `POST /admin/chat/users/ban` | 写入 `chat_user_bans`，发送消息前检查 |
| 解除禁言 | `POST /admin/chat/users/unban` | 将禁言记录置为失效 |
| 系统公告 | `POST /admin/chat/announcement` | 写系统消息并广播 `SYSTEM` |

禁言支持临时和永久。`banDuration > 0` 时按秒计算结束时间；`banDuration = 0` 时视为永久禁言。过期禁言由定时任务每分钟自动解除。

## 核心数据表

| 表 | 说明 |
| --- | --- |
| `chat_rooms` | 聊天室，当前主要使用官方聊天室 |
| `chat_messages` | 消息记录，包含文本、图片、回复、删除状态、IP 和设备信息 |
| `chat_online_users` | 在线用户持久化记录，Redis 是实时主存储 |
| `chat_user_bans` | 禁言记录 |
| `chat_sensitive_word` | 聊天敏感词历史表，当前更完整的敏感词能力在敏感词模块 |

## 验证清单

1. 用户登录后进入 `/chat`，确认前端先请求 `/user/chat/ws-ticket`，再连接 `/ws/chat?ticket=...`。
2. 发送文本消息，观察前端状态从 `sending` 变为 `sent`。
3. 开两个浏览器账号，确认一个账号发送消息后，另一个账号收到 `MESSAGE`。
4. 上传小于 5MB 的图片，确认文件先进入文件存储，再发送图片消息。
5. 连续快速发送超过 8 条消息，确认服务端返回 `RATE_LIMITED`，本地消息变成失败态。
6. 发送空文本、超长文本或非法图片 URL，确认服务端返回 `MESSAGE_REJECTED`。
7. 发送后 2 分钟内撤回，确认双方都删除该消息。
8. 超过 2 分钟后撤回，确认接口返回业务错误。
9. 管理端禁言用户，再用该用户发送消息，确认服务端拒绝。
10. 管理端踢出用户，确认用户端收到 `KICK_OUT` 并断开。
11. 停止心跳或关闭页面，等待 90 秒以上，确认在线用户被清理。
12. 多节点部署前，确认是否需要 Redis Pub/Sub 或消息队列做跨实例广播。

## 常见坑

| 问题 | 原因 | 处理 |
| --- | --- | --- |
| 撤回失败 | 前端仍拿 `tempId` 调接口 | 必须用 `MESSAGE_ACK` 返回的正式 `id` |
| 在线人数不准 | Redis 中有孤儿 session 或心跳超时未清理 | 检查 `chat:online:*` key 和定时任务 |
| 握手失败 | 没先取票据、票据超过 60 秒、票据已被消费 | 重新调用 `/user/chat/ws-ticket` 后再连接 |
| 前端显示心跳超时 | `PONG` 没按时返回或网络抖动过大 | 检查 WebSocket 连接、代理超时和服务端日志 |
| 输入中状态频繁丢失 | `TYPING` 被服务端限流 | 检查 `xiaou.chat.rate-limit.typing-*` 配置 |
| 消息发送失败 | 命中限流、被禁言、消息超长或图片 URL 不合法 | 查看 `ERROR.data.code` 和 `ERROR.data.message` |
| 生产环境跨域失败 | `xiaou.cors.allowed-origin-patterns` 未包含前端域名 | 在生产配置中显式加入 HTTPS 域名 |
| 多实例消息丢失 | `SESSIONS` 是单进程内存 Map | 多实例部署需要 Redis Pub/Sub 或消息中间件 |

## 下一步建议

聊天模块已经补齐单实例聊天室的核心生产保护：一次性握手票据、心跳响应、输入中限流、消息限流和服务端消息校验。下一步如果要支撑多实例或更大规模在线用户，重点是把广播从进程内 `SESSIONS` 扩展到 Redis Pub/Sub 或消息队列，并补充更细的运营监控指标。

---

## 聊天系统深度拆解

以下内容来自对 xiaou-chat 模块全部源码的逐行阅读，覆盖 WebSocket 处理器、4 个 ServiceImpl、2 个 Controller、4 个 Domain、1 个 Interceptor、1 个 ScheduledTask、1 个 RateLimitService。

### 一、WebSocket 连接生命周期详解

#### 1.1 一次性票据机制

**源码**：`ChatWebSocketTicketService` + `SaTokenWebSocketInterceptor`

```
前端: POST /user/chat/ws-ticket
  ↓
ChatWebSocketTicketService.createTicket(userId):
  1. 生成 32 字节随机 token (SecureRandom)
  2. Redis SET xiaou:chat:ws-ticket:{token} → userId, TTL=60s
  3. 返回 token 给前端

前端: new WebSocket("/ws/chat?ticket=" + token)
  ↓
SaTokenWebSocketInterceptor.beforeHandshake():
  1. 从 URI query 解析 ticket 参数
  2. ChatWebSocketTicketService.consumeTicket(token):
     a. Redis GET xiaou:chat:ws-ticket:{token}
     b. 不存在 → 拒绝握手
     c. 存在 → Redis DEL (一次性消费), 返回 userId
  3. 查询用户昵称
  4. session.attributes.put("userId", userId)
  5. session.attributes.put("username", username)
  6. 返回 true → 握手继续
```

**安全设计**：票据一次性消费，60 秒 TTL，即使 token 泄露也只能使用一次。避免了在 WebSocket URL 中传递长期 Token 的风险（URL 会被浏览器历史、服务器日志记录）。

#### 1.2 连接建立完整流程

**源码**：`ChatWebSocketHandler.afterConnectionEstablished`

```
afterConnectionEstablished(session):
  1. 从 session.attributes 取 userId、username
  2. userId 为空 → session.close()（防御性检查）
  3. SESSIONS.put(sessionId, session)   // ConcurrentHashMap
  4. 获取 IP、User-Agent
  5. chatOnlineUserService.userOnline(userId, roomId, sessionId, ip, ua)
     ├─ cleanUserOldSessions(userId, roomId)  // 先清理旧会话
     ├─ Redis 写入用户信息、心跳、房间集合
     └─ DB INSERT chat_online_users (try-catch，失败不影响连接)
  6. 发送 CONNECT 给当前用户
  7. 广播 ONLINE_COUNT
  8. 广播 USER_JOIN
```

**关键发现**：`cleanUserOldSessions` 在用户上线时先清理旧会话。这意味着同一用户只能有一个活跃连接——新连接会踢掉旧连接的在线记录。

#### 1.3 SESSIONS 存储结构

**源码**：`ChatWebSocketHandler` 第 43 行

```java
private static final Map<String, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();
```

- Key: `session.getId()` (Spring WebSocket 自动生成的唯一 ID)
- Value: `WebSocketSession` 对象

**单实例限制**：`SESSIONS` 是进程内 Map，不支持多实例部署。多实例需要替换为 Redis Pub/Sub + 本地 Session 映射。

### 二、消息发送完整链路

#### 2.1 消息处理流程

**源码**：`ChatWebSocketHandler.handleTextMessage`

```
handleTextMessage(session, message):
  1. 解析 payload → WebSocketMessage
  2. type == HEARTBEAT → 更新心跳 + 返回 PONG
  3. type == TYPING → 限流后广播
  4. type == MESSAGE → handleChatMessage()
     ├─ 解析 ChatMessageRequest
     ├─ ChatRateLimitService.tryAcquireMessage(userId)
     │   └─ 限流 → 发送 ERROR(RATE_LIMITED)
     ├─ ChatMessageService.sendMessage(request, userId, ip, ua)
     │   ├─ 校验消息类型和内容
     │   ├─ 检查禁言状态
     │   ├─ 处理回复消息
     │   └─ INSERT chat_messages + SELECT 完整消息
     ├─ 广播 MESSAGE 给其他在线用户（排除发送者）
     └─ 发送 MESSAGE_ACK 给发送者（携带正式消息ID）
```

#### 2.2 消息限流算法

**源码**：`ChatRateLimitService`

| 限流维度 | Redis Key | 窗口 | 阈值 | 说明 |
| --- | --- | --- | --- | --- |
| 消息发送 | `chat:ratelimit:message:{userId}` | 10 秒 | 8 条 | 每用户每 10 秒最多 8 条消息 |
| 输入中事件 | `chat:ratelimit:typing:{userId}` | 10 秒 | 12 次 | 每用户每 10 秒最多 12 次 TYPING 事件 |

实现方式：Redis 固定窗口计数器（INCR + EXPIRE）。

**限流返回对象** `RateLimitResult`：
- `allowed`: boolean — 是否允许
- `message`: String — 拒绝原因
- `retryAfterSeconds`: int — 建议重试等待时间

#### 2.3 消息校验规则

**源码**：`ChatMessageServiceImpl.validateMessageRequest`

| 消息类型 | 校验项 | 限制 |
| --- | --- | --- |
| 文本(1) | content 非空非 blank | 必须 |
| 文本(1) | content.length() | ≤ `maxTextLength`（默认 1000） |
| 图片(2) | imageUrl 非空非 blank | 必须 |
| 图片(2) | imageUrl.length() | ≤ `maxImageUrlLength`（默认 1024） |
| 图片(2) | imageUrl 协议白名单 | http://, https://, /api/files/, api/files/ |
| 图片(2) | content (图片说明) | 可选，≤ maxTextLength |
| 其他 | messageType | 只支持 1 和 2 |

**图片 URL 白名单**（`isAllowedImageUrl`）：

```java
return normalized.startsWith("http://")
    || normalized.startsWith("https://")
    || normalized.startsWith("/api/files/")
    || normalized.startsWith("api/files/");
```

**关键发现**：图片 URL 允许任何 `http://` 或 `https://` 开头的 URL，包括外部域名的图片。这可能有安全风险（SSRF、恶意图片链接等）。

#### 2.4 消息撤回机制

**源码**：`ChatMessageServiceImpl.recallMessage`

```
recallMessage(messageId, userId):
  1. 查询消息
  2. 消息不存在 → throw
  3. !message.userId.equals(userId) → throw "只能撤回自己的消息"
  4. (now - createTime) > 120秒 → throw "消息发送超过2分钟"
  5. chatMessageMapper.deleteById(messageId)  // 物理删除！
  6. chatWebSocketHandler.sendRecallNotification(messageId)  // 广播撤回通知
```

**关键发现**：撤回使用的是 `deleteById`（**物理删除**），不是软删除。撤回后消息从数据库完全消失。这意味着：
1. 撤回后的消息无法恢复
2. 如果撤回通知广播失败（用户不在线），已撤回的消息在对方客户端仍然显示，但数据库已无记录
3. 管理员删除也是物理删除（`deleteById`），与文档中描述的"软删除"不一致

### 三、在线态 Redis 架构

#### 3.1 Redis 数据结构

**源码**：`ChatOnlineUserServiceImpl`

| Redis Key | 类型 | 字段/值 | TTL | 用途 |
| --- | --- | --- | --- | --- |
| `chat:online:users` | Hash | `sessionId → userInfo JSON` | 无 | 用户详细信息 |
| `chat:online:heartbeat` | Hash | `sessionId → timestamp` | 无 | 心跳时间戳 |
| `chat:room:{roomId}:users` | Set | `sessionId` | 无 | 房间内在线用户集合 |

**无 TTL 风险**：这三个 Redis Key 都没有设置 TTL，过期清理完全依赖 `ChatScheduledTask.cleanTimeoutUsers` 定时任务。如果定时任务停止运行，Redis 中会积累大量僵尸数据。

#### 3.2 心跳机制

```
客户端: 每 30 秒发送 HEARTBEAT
  ↓
ChatWebSocketHandler: 更新 Redis heartbeat key
  ↓
服务端: 返回 PONG { timestamp: 当前毫秒时间戳 }
  ↓
客户端: 计算延迟 = 接收时间戳 - PONG.timestamp
  ├─ 延迟 < 500ms: 维持 30 秒心跳间隔
  ├─ 延迟 500-2000ms: 缩短到 15 秒
  └─ 延迟 > 2000ms: 缩短到 10 秒

连续 3 次 PONG 超时 → 主动重连
```

#### 3.3 超时清理定时任务

**源码**：`ChatScheduledTask`

| 任务 | Cron | 说明 |
| --- | --- | --- |
| `cleanTimeoutUsers` | `0/30 * * * * ?` | 每 30 秒清理超时用户（90 秒无心跳） |
| `autoUnbanExpired` | `0 * * * * ?` | 每分钟自动解除过期禁言 |

**清理逻辑**：

```
cleanTimeoutUsers():
  1. 遍历 Redis heartbeat map
  2. 心跳时间 < (now - 90s) → 标记为超时
  3. 对每个超时 session: userOffline(sessionId)
     ├─ Redis: 删除用户信息 + 心跳 + 房间集合
     └─ DB: DELETE chat_online_users WHERE session_id=?
  4. DB: DELETE chat_online_users WHERE last_heartbeat_time < (now - 90s)
```

### 四、禁言系统

#### 4.1 禁言状态机

```
         banUser()               unbanUser()
正常 ────────────→ 禁言中 ────────────→ 已解除
                       │                    ↑
                       │  autoUnbanExpired()│
                       └────────────────────┘
                         (banEndTime 到期自动解除)
```

#### 4.2 禁言检查逻辑

**源码**：`ChatUserBanServiceImpl.isUserBanned`

```java
public boolean isUserBanned(Long userId, Long roomId) {
    ChatUserBan ban = chatUserBanMapper.selectActiveByUserId(userId, roomId);
    return ban != null;
}
```

**关键发现**：`selectActiveByUserId` 查询条件是 `status = 1`（生效中），但**不检查 `banEndTime`**。也就是说，即使禁言已经到期，如果定时任务 `autoUnbanExpired` 还没执行（最多 1 分钟延迟），用户仍然会被判定为禁言中。

### 五、深度发现与坑点

#### 5.1 已确认的代码问题

| 编号 | 问题 | 位置 | 影响 |
| --- | --- | --- | --- |
| BUG-1 | 撤回和删除使用物理删除 | `ChatMessageServiceImpl.recallMessage` / `deleteMessage` | 消息不可恢复，审计追溯困难 |
| BUG-2 | 禁言到期检查依赖定时任务 | `ChatUserBanServiceImpl.isUserBanned` | 最多 1 分钟延迟，到期后仍可能被拒 |
| BUG-3 | 图片 URL 协议白名单过于宽松 | `ChatMessageServiceImpl.isAllowedImageUrl` | 允许任意 http/https URL，存在 SSRF 风险 |
| BUG-4 | Redis 在线态数据无 TTL | `ChatOnlineUserServiceImpl` | 定时任务停止则僵尸数据累积 |

#### 5.2 设计层面的潜在风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | SESSIONS 是进程内 Map | 多实例部署时消息无法跨实例广播 |
| RISK-2 | 广播失败静默处理 | `broadcastMessage` 中 IOException 只记日志，不重试 |
| RISK-3 | 数据库写入失败不影响连接 | `userOnline` DB 写入是 try-catch 静默处理，DB 可能缺失上线记录 |
| RISK-4 | 同一用户多设备限制 | `cleanUserOldSessions` 清理旧会话，同一用户只能有一个活跃连接 |

#### 5.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | 一次性票据 | 避免 Token 暴露在 URL 中，安全设计优秀 |
| H-2 | Redis 在线态 + DB 持久化 | 热路径走 Redis，冷数据落 DB |
| H-3 | 心跳自适应 | 前端根据延迟调整心跳间隔，省电省流量 |
| H-4 | 输入中限流 | TYPING 事件独立限流，防止广播风暴 |
| H-5 | 孤儿 Session 清理 | `getOnlineUsers` 时自动清理无用户信息的 sessionId |
| H-6 | 传输异常分级处理 | 区分正常断开（debug 日志）和异常断开（error 日志+堆栈） |

#### 5.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| WebSocket 连接管理 | `ChatWebSocketHandler.java` — 连接建立/消息处理/断开/广播 |
| 握手认证 | `SaTokenWebSocketInterceptor.java` — 票据消费+用户信息注入 |
| 票据生成/消费 | `ChatWebSocketTicketService.java` — Redis 一次性票据 |
| 消息 CRUD | `ChatMessageServiceImpl.java` — 发送/撤回/删除/公告 |
| 在线态管理 | `ChatOnlineUserServiceImpl.java` — Redis 在线+DB 持久化 |
| 禁言管理 | `ChatUserBanServiceImpl.java` — 禁言/解禁/自动解禁 |
| 限流 | `ChatRateLimitService.java` — 消息+输入中限流 |
| 定时任务 | `ChatScheduledTask.java` — 清理超时+自动解禁 |
| WebSocket 配置 | `WebSocketConfig.java` — 端点注册+CORS |
| 消息协议 | `WebSocketMessage.java` — 消息类型常量+数据结构 |
