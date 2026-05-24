# WebSocket 协议

IM 聊天室使用 Spring WebSocket，服务端入口位于 `xiaou-chat/src/main/java/com/xiaou/chat/config/WebSocketConfig.java`，消息处理器位于 `ChatWebSocketHandler.java`。

## 连接地址

```text
ws://<host>/api/ws/chat?ticket=<one-time-ticket>
```

注意：因为后端 `server.servlet.context-path=/api`，所以 WebSocket 端点实际路径为 `/api/ws/chat`。前端开发代理需正确转发 WebSocket 升级请求。

## 连接流程

```text
客户端                              服务端
  │                                  │
  │  POST /api/user/chat/ws-ticket   │
  │ ──────────────────────────────>  │
  │                                  │ ChatWebSocketTicketService
  │                                  │ 生成 ticket → Redis (TTL 60s)
  │  <────────────────────────────── │
  │  { ticket: "abc123" }           │
  │                                  │
  │  WS /api/ws/chat?ticket=abc123  │
  │ ──────────────────────────────>  │
  │                                  │ SaTokenWebSocketInterceptor
  │                                  │ 1. 校验 ticket (Redis 读取+删除)
  │                                  │ 2. 校验用户登录态 (StpUserUtil)
  │                                  │ 3. 写入 userId/username 到 session
  │                                  │
  │  <────────────────────────────── │
  │  CONNECT event                   │
  │                                  │
  │  HEARTBEAT (定时)                │
  │ ──────────────────────────────>  │
  │  <────────────────────────────── │
  │  PONG                            │
```

连接前，用户端先调用 `POST /api/user/chat/ws-ticket` 换取一次性票据。票据由 `ChatWebSocketTicketService` 写入 Redis，TTL 为 60 秒，握手时被 `SaTokenWebSocketInterceptor` 消费，消费后立即删除。校验成功后，服务端会把 `userId`、`username` 写入 WebSocket session attributes。

## 消息格式

```json
{
  "type": "MESSAGE",
  "data": {}
}
```

| 字段 | 说明 |
| --- | --- |
| `type` | 事件类型，取值见下表 |
| `data` | 事件负载，按类型变化 |

## 服务端事件

| type | 方向 | data |
| --- | --- | --- |
| `CONNECT` | 服务端→客户端 | `{ "message": "连接成功", "userId": 1, "username": "demo" }` |
| `MESSAGE` | 双向 | 普通聊天消息。客户端发送时是消息请求，服务端广播时是消息响应 |
| `MESSAGE_ACK` | 服务端→发送者 | 消息保存成功后的确认，用于把本地临时消息更新为真实消息 |
| `SYSTEM` | 服务端→客户端 | `{ "content": "公告内容" }` |
| `ONLINE_COUNT` | 服务端→客户端 | `{ "count": 10 }` |
| `USER_JOIN` | 服务端→客户端 | `{ "userId": 1, "username": "demo" }` |
| `USER_LEAVE` | 服务端→客户端 | `{ "userId": 1, "username": "demo" }` |
| `MESSAGE_RECALL` | 服务端→客户端 | `{ "messageId": 100 }` |
| `MESSAGE_DELETE` | 服务端→客户端 | `{ "messageId": 100 }` |
| `KICK_OUT` | 服务端→客户端 | `{ "message": "您已被管理员踢出聊天室" }` |
| `HEARTBEAT` | 客户端→服务端 | 心跳包，服务端更新在线状态 |
| `PONG` | 服务端→客户端 | `{ "timestamp": 1710000000000 }` |
| `TYPING` | 客户端→服务端→其他客户端 | `{ "userId": 1, "nickname": "demo" }` |
| `ERROR` | 服务端→客户端 | `{ "code": "RATE_LIMITED", "message": "发送太快了，请稍后再试", "tempId": "temp_xxx", "retryAfterSeconds": 10 }` |

### ERROR 子类型

| code | 含义 | 客户端处理建议 |
|------|------|---------------|
| `RATE_LIMITED` | 发送频率超限 | 根据 `retryAfterSeconds` 延迟重试 |
| `TOKEN_EXPIRED` |票据过期 | 重新获取 ticket 并重连 |
| `UNAUTHORIZED` | 未登录 | 跳转登录页 |
| `BANNED` | 被禁言 | 显示禁言提示，禁止发送 |
| `ROOM_NOT_FOUND` | 房间不存在 | 提示错误并离开聊天页 |

## REST 辅助接口

| 能力 | 路由前缀 | 说明 |
| --- | --- | --- |
| 用户侧票据、消息历史、在线人数、在线用户、撤回、图片上传 | `/api/user/chat` | WebSocket 之外的查询和操作 |
| 管理侧消息删除、批量删除、禁言、踢人、系统公告 | `/api/admin/chat` | 管理动作会通过 WebSocket 广播删除、踢出或系统消息 |

### 核心接口列表

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/chat/ws-ticket` | POST | 获取一次性 WebSocket 票据 |
| `/api/user/chat/history` | GET | 分页获取聊天历史 |
| `/api/user/chat/online-count` | GET | 在线人数 |
| `/api/user/chat/online-users` | GET | 在线用户列表 |
| `/api/user/chat/recall` | POST | 撤回消息 |
| `/api/user/chat/upload-image` | POST | 上传聊天图片 |
| `/api/admin/chat/delete` | DELETE | 删除消息 |
| `/api/admin/chat/ban` | POST | 禁言用户 |
| `/api/admin/chat/kick` | POST | 踢出用户 |
| `/api/admin/chat/announce` | POST | 发送系统公告 |

## 服务端架构

```text
xiaou-chat/src/main/java/com/xiaou/chat/
├── config/
│   └── WebSocketConfig.java          ← @EnableWebSocket + 端点注册
├── websocket/
│   ├── ChatWebSocketHandler.java     ← 消息处理核心 (TextWebSocketHandler)
│   ├── SaTokenWebSocketInterceptor.java  ← 票据校验 + 登录态校验
│   └── WebSocketMessage.java         ← 消息数据类 (type + data)
├── service/
│   ├── ChatWebSocketTicketService.java  ← 票据生成/校验 (Redis TTL 60s)
│   └── ChatMessageService.java      ← 消息持久化/撤回/广播
├── controller/
│   ├── user/ChatUserController.java  ← 用户端 REST API
│   └── admin/ChatAdminController.java ← 管理端 REST API
├── dto/                              ← 请求/响应 DTO
├── domain/                           ← 实体类
└── mapper/                           ← MyBatis Mapper
```

### 会话管理

当前使用**本地内存**管理 WebSocket 会话：

```java
// ChatWebSocketHandler 内部
private static final ConcurrentHashMap<String, WebSocketSession> SESSIONS;
```

| 特征 | 说明 |
|------|------|
| 存储 | `ConcurrentHashMap<sessionId, WebSocketSession>` |
| 广播 | 遍历所有 SESSIONS 发送消息 |
| 节点 | 单节点适用，集群需改造 |

**集群部署注意**：当前 SESSIONS 是本地 HashMap，集群部署需要改为 Redis Pub/Sub 或 Sticky Session。

### 限流机制

| 事件类型 | 限流规则 | 实现 |
|----------|----------|------|
| MESSAGE | 10 秒 8 条 | Redis 固定窗口 |
| TYPING | 10 秒 12 次 | Redis 固定窗口 |

超限时服务端返回 `ERROR` 事件，`code=RATE_LIMITED`。

## 在线状态

| 存储 | 说明 |
| --- | --- |
| `chat_online_users` 表 | 记录 session、用户、房间、IP、设备、连接时间和最后心跳 |
| Redis `chat:online:heartbeat` | `sessionId -> timestamp`，用于快速判断在线状态 |

### 心跳与断连清理

```text
客户端定时 → HEARTBEAT (每 30 秒)
服务端响应 → PONG + 更新 Redis heartbeat timestamp

超时检测（定时任务）:
  1. 比较每个 session 的最后心跳时间
  2. 超过阈值（如 90 秒）的标记为离线
  3. 广播 USER_LEAVE 事件
  4. 删除 session + 清理数据库记录
```

## 消息存储

| 字段 | 说明 |
|------|------|
| `chat_messages.id` | 消息 ID |
| `chat_messages.room_id` | 房间 ID |
| `chat_messages.sender_id` | 发送者 ID |
| `chat_messages.content` | 消息内容 |
| `chat_messages.type` | TEXT / IMAGE / SYSTEM |
| `chat_messages.is_recalled` | 撤回标记 |
| `chat_messages.created_time` | 发送时间 |

消息历史通过 REST `/api/user/chat/history` 分页查询，不通过 WebSocket 传输。

## 客户端行为

用户端页面位于 `vue3-user-front/src/views/chat/Index.vue`。

1. 页面进入时读取用户登录态，先请求 `/api/user/chat/ws-ticket`。
2. 拿到票据后连接 `/api/ws/chat?ticket=...`。
3. 发送普通消息使用 `type=MESSAGE`，发送输入中状态使用 `type=TYPING`。
4. 心跳定时发送 `type=HEARTBEAT`，收到 `PONG` 后更新延迟和心跳状态。
5. 收到 `MESSAGE_ACK` 后更新本地消息状态（乐观更新→确认）。
6. 收到 `MESSAGE_RECALL`、`MESSAGE_DELETE` 后更新消息列表。
7. 收到 `ERROR` 后根据 `tempId` 标记本地乐观消息失败。
8. 收到 `KICK_OUT` 后断开连接并提示用户。

### 乐观消息机制

```text
用户点击发送
  → 本地立即显示消息 (tempId: "temp_xxx")
  → WebSocket 发送 MESSAGE 事件
  → 服务端返回 MESSAGE_ACK (realId: 100)
  → 本地替换 tempId 为 realId

如果服务端返回 ERROR (RATE_LIMITED):
  → 本地标记消息发送失败
  → 根据 retryAfterSeconds 决定是否重试
```

## 注意事项

1. WebSocket 票据只能使用一次，超过 60 秒或被消费后需要重新申请。
2. 管理端删除和踢人必须同时走 REST 和 WebSocket 通知，否则用户端状态会延迟。
3. `MESSAGE` 和 `TYPING` 都有 Redis 固定窗口限流，默认消息为 10 秒 8 条，输入中为 10 秒 12 次。
4. 集群部署时需要关注 WebSocket session 的节点内存态，目前 `SESSIONS` 是本地 `ConcurrentHashMap`。
5. 聊天图片上传走 REST `/api/user/chat/upload-image`，不走 WebSocket。
6. 消息撤回仅限发送者本人，2 分钟内可撤回。

## 验证清单

验证聊天室协议时，至少覆盖：

1. 未携带 ticket、ticket 过期或重复使用时，握手被拒绝。
2. 正常连接前先拿到 `/api/user/chat/ws-ticket`，连接后收到 `CONNECT`，在线人数随连接变化。
3. 发送 `MESSAGE` 后，发送者收到 `MESSAGE_ACK`，其他在线用户收到广播消息。
4. 心跳能收到 `PONG` 并更新在线状态，超时用户会被清理。
5. 输入框持续输入时，其他客户端能收到 `TYPING`。
6. 高频发送消息时，服务端返回带 `RATE_LIMITED` 的 `ERROR`。
7. 撤回、删除、踢出、系统公告能通过 REST 动作触发 WebSocket 通知。
8. 禁言用户发送消息时收到 `BANNED` 错误。
9. 连接断开后自动重连并重新获取票据。

## 源码导航

| 文件 | 说明 |
|------|------|
| `xiaou-chat/.../config/WebSocketConfig.java` | WebSocket 端点注册 (`/ws/chat`) |
| `xiaou-chat/.../websocket/ChatWebSocketHandler.java` | 消息处理核心 (TextWebSocketHandler) |
| `xiaou-chat/.../websocket/SaTokenWebSocketInterceptor.java` | 票据校验 + 登录态校验 |
| `xiaou-chat/.../websocket/WebSocketMessage.java` | 消息数据类 (type + data) |
| `xiaou-chat/.../service/ChatWebSocketTicketService.java` | 票据生成/校验 (Redis TTL 60s) |
| `xiaou-chat/.../controller/user/ChatUserController.java` | 用户端 REST API (`/api/user/chat`) |
| `xiaou-chat/.../controller/admin/ChatAdminController.java` | 管理端 REST API (`/api/admin/chat`) |
| `vue3-user-front/src/views/chat/Index.vue` | 前端聊天页面 |
