# IM 聊天室

IM 聊天室提供实时消息、在线状态、心跳、输入中、图片消息、撤回、禁言、踢人和公告能力。

## 主要入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/chat` |
| 管理端 | `/chat/messages`、`/chat/users` |
| 后端 | `xiaou-chat` |

## 实时链路

1. 用户登录后获取聊天室连接凭据。
2. 前端建立 WebSocket 连接。
3. 服务端校验握手凭据并绑定用户会话。
4. 用户发送消息，服务端校验类型、长度、图片 URL 和限流。
5. 合法消息入库并广播。
6. 管理端可查看消息和在线用户，并执行禁言、踢人、公告等操作。

## WebSocket 事件

事件常量定义在 `xiaou-chat/src/main/java/com/xiaou/chat/websocket/WebSocketMessage.java`。

| 事件 | 方向 | 说明 |
| --- | --- | --- |
| `CONNECT` | 服务端到客户端 | 握手成功，返回用户信息 |
| `MESSAGE` | 双向 | 客户端发送聊天消息；服务端广播已入库消息 |
| `MESSAGE_ACK` | 服务端到发送者 | 返回正式消息 ID，用于替换前端乐观消息 |
| `SYSTEM` | 服务端到客户端 | 系统公告 |
| `ONLINE_COUNT` | 服务端到客户端 | 在线人数变化 |
| `USER_JOIN` | 服务端到客户端 | 用户加入聊天室 |
| `USER_LEAVE` | 服务端到客户端 | 用户离开聊天室 |
| `MESSAGE_RECALL` | 服务端到客户端 | 用户撤回消息 |
| `MESSAGE_DELETE` | 服务端到客户端 | 管理员删除消息 |
| `KICK_OUT` | 服务端到客户端 | 管理员踢出用户 |
| `HEARTBEAT` | 客户端到服务端 | 更新在线心跳 |
| `ERROR` | 服务端到客户端 | 消息处理失败 |

## 在线态存储

在线用户由 `ChatOnlineUserServiceImpl` 同步维护 Redis 和数据库。

| Redis Key | 类型 | 内容 |
| --- | --- | --- |
| `chat:online:users` | Hash | `sessionId -> userInfo JSON` |
| `chat:online:heartbeat` | Hash | `sessionId -> timestamp` |
| `chat:room:{roomId}:users` | Set | 房间内所有 `sessionId` |

心跳超时时间为 90 秒。用户上线时会清理同一用户在同一房间的旧会话，避免重复在线；下线、踢出和超时清理都会删除 Redis 在线态，并尝试清理数据库记录。

## 消息撤回和管理动作

| 动作 | 接口 | 规则 |
| --- | --- | --- |
| 用户撤回 | `/user/chat/message/recall` | 只能撤回自己的消息，发送 2 分钟内可撤回 |
| 管理员删除 | `/admin/chat/messages/{id}` | 软删除消息并广播 `MESSAGE_DELETE` |
| 批量删除 | `/admin/chat/messages/batch-delete` | 批量软删除消息 |
| 踢出用户 | `/admin/chat/users/kick` | 删除在线态并关闭 WebSocket |
| 禁言用户 | `/admin/chat/users/ban` | 发送消息前检查禁言状态 |
| 系统公告 | `/admin/chat/announcement` | 写入系统消息并广播 |

前端乐观消息要以 `MESSAGE_ACK` 返回的正式 `messageId` 为准；如果只保留临时 ID，后续撤回会找不到真实消息。

## v2.1.1 之后的安全强化

- WebSocket 握手不再把长期 Token 放进 URL。
- 使用短时一次性票据完成握手。
- CORS 白名单配置化。
- 聊天消息和 Markdown/v-html 渲染强化净化。

## v2.1.2 之后的运行保护

- 文本和图片消息做服务端校验。
- 高频消息进入 Redis 固定窗口限流。
- 输入中事件也纳入降噪。
- 前端乐观消息可以标记失败并展示错误。

## 后续补齐

- Redis Pub/Sub 多节点广播。
- 图片上传与文件权限关系。
- 聊天室压测和限流参数建议。
