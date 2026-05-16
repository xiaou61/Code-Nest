# WebSocket 协议

IM 聊天室使用 Spring WebSocket，服务端入口位于 `xiaou-chat/src/main/java/com/xiaou/chat/config/WebSocketConfig.java`，消息处理器位于 `ChatWebSocketHandler.java`。

## 连接地址

```text
ws://<host>/ws/chat?token=<sa-token>
```

握手阶段由 `SaTokenWebSocketInterceptor` 校验 Token。校验成功后，服务端会把 `userId`、`username` 写入 WebSocket session attributes。

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
| `CONNECT` | 服务端到客户端 | `{ "message": "连接成功", "userId": 1, "username": "demo" }` |
| `MESSAGE` | 双向 | 普通聊天消息。客户端发送时是消息请求，服务端广播时是消息响应 |
| `MESSAGE_ACK` | 服务端到发送者 | 消息保存成功后的确认，用于把本地临时消息更新为真实消息 |
| `SYSTEM` | 服务端到客户端 | `{ "content": "公告内容" }` |
| `ONLINE_COUNT` | 服务端到客户端 | `{ "count": 10 }` |
| `USER_JOIN` | 服务端到客户端 | `{ "userId": 1, "username": "demo" }` |
| `USER_LEAVE` | 服务端到客户端 | `{ "userId": 1, "username": "demo" }` |
| `MESSAGE_RECALL` | 服务端到客户端 | `{ "messageId": 100 }` |
| `MESSAGE_DELETE` | 服务端到客户端 | `{ "messageId": 100 }` |
| `KICK_OUT` | 服务端到客户端 | `{ "message": "您已被管理员踢出聊天室" }` |
| `HEARTBEAT` | 客户端到服务端 | 心跳包，服务端更新在线状态 |
| `ERROR` | 服务端到客户端 | `{ "message": "消息处理失败: ..." }` |

## REST 辅助接口

| 能力 | 路由前缀 | 说明 |
| --- | --- | --- |
| 用户侧消息历史、在线人数、在线用户、撤回、图片上传 | `/user/chat` | WebSocket 之外的查询和操作 |
| 管理侧消息删除、批量删除、禁言、踢人、系统公告 | `/admin/chat` | 管理动作会通过 WebSocket 广播删除、踢出或系统消息 |

## 在线状态

| 存储 | 说明 |
| --- | --- |
| `chat_online_users` | 记录 session、用户、房间、IP、设备、连接时间和最后心跳 |
| Redis `chat:online:heartbeat` | `sessionId -> timestamp`，用于快速判断在线状态 |

## 客户端行为

用户端页面位于 `vue3-user-front/src/views/chat/Index.vue`。

1. 页面进入时读取用户 Token 并连接 `/ws/chat`。
2. 发送普通消息使用 `type=MESSAGE`。
3. 心跳定时发送 `type=HEARTBEAT`。
4. 收到 `MESSAGE_ACK` 后更新本地消息状态。
5. 收到 `MESSAGE_RECALL`、`MESSAGE_DELETE` 后更新消息列表。
6. 收到 `KICK_OUT` 后断开连接并提示用户。

## 注意事项

1. 目前 `typingUsers` 是前端输入状态 UI，服务端协议未登记 `TYPING` 事件；如果要做多人输入中提示，需要新增事件并更新本页。
2. 管理端删除和踢人必须同时走 REST 和 WebSocket 通知，否则用户端状态会延迟。
3. 集群部署时需要关注 WebSocket session 的节点内存态，目前 `SESSIONS` 是本地 `ConcurrentHashMap`。

## 验证清单

验证聊天室协议时，至少覆盖：

1. 未携带 Token 或 Token 失效时，握手被拒绝。
2. 正常连接后收到 `CONNECT`，在线人数随连接变化。
3. 发送 `MESSAGE` 后，发送者收到 `MESSAGE_ACK`，其他在线用户收到广播消息。
4. 心跳能更新在线状态，超时用户会被清理。
5. 撤回、删除、踢出、系统公告能通过 REST 动作触发 WebSocket 通知。
6. 前端存在但服务端未登记的事件，不应写进正式协议表。
