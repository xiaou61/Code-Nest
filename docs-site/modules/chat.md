# IM 聊天室

IM 聊天室提供官方聊天室、实时消息、在线状态、图片消息、回复、撤回、禁言、踢人和公告能力。读这篇文档时要特别注意：聊天模块有一部分前端交互已经写得比后端协议更“超前”，所以本文会区分“当前已落地”和“需要验证/补齐”的部分。

## 推荐学习顺序

聊天模块建议按“连接 -> 消息 -> 在线态 -> 管理动作 -> 生产化风险”阅读：

1. 先看 WebSocket 握手，理解当前为什么是 `/ws/chat?token=<token>`。
2. 再看消息发送和 `MESSAGE_ACK`，理解前端临时消息如何替换成正式消息。
3. 接着看 Redis 在线态和心跳清理，理解在线人数为什么不能只看数据库。
4. 然后看撤回、禁言、踢出和公告，理解用户动作和管理动作如何广播。
5. 最后看协议差异和下一步建议，区分已落地能力和前端预留能力。

## 主要入口

| 端 | 入口 | 说明 |
| --- | --- | --- |
| 用户端页面 | `/chat` | 聊天主界面，包含历史消息、在线用户、文本/图片发送和撤回 |
| 用户端 API | `/user/chat/**` | 历史消息、在线人数、在线用户、撤回 |
| WebSocket | `/ws/chat?token=<token>` | 当前源码使用 URL query token 完成握手 |
| 管理端页面 | `/chat/messages`、`/chat/users` | 消息审计、删除、在线用户、禁言、踢出、公告 |
| 管理端 API | `/admin/chat/**` | 管理员操作入口 |
| 后端模块 | `xiaou-chat` | WebSocket、消息、在线态、禁言和定时任务 |

## 源码地图

| 文件 | 职责 |
| --- | --- |
| `xiaou-chat/src/main/java/com/xiaou/chat/config/WebSocketConfig.java` | 注册 `/ws/chat`，绑定握手拦截器和处理器 |
| `xiaou-chat/src/main/java/com/xiaou/chat/websocket/SaTokenWebSocketInterceptor.java` | 从 query string 读取 token，并用 Sa-Token 解析用户 |
| `xiaou-chat/src/main/java/com/xiaou/chat/websocket/ChatWebSocketHandler.java` | 连接、心跳、消息广播、系统消息、踢人、撤回/删除通知 |
| `xiaou-chat/src/main/java/com/xiaou/chat/service/impl/ChatMessageServiceImpl.java` | 消息入库、历史查询、撤回、删除、公告 |
| `xiaou-chat/src/main/java/com/xiaou/chat/service/impl/ChatOnlineUserServiceImpl.java` | Redis 在线态、心跳、踢出、超时清理 |
| `vue3-user-front/src/views/chat/Index.vue` | 用户端聊天室完整交互 |

## 当前连接流程

1. 用户登录后，前端从用户 store 中读取 Sa-Token。
2. 前端建立连接：`new WebSocket(`${WS_URL}/ws/chat?token=${token}`)`。
3. `SaTokenWebSocketInterceptor` 从 URL query 中解析 `token`。
4. 拦截器调用 `StpUserUtil.stpLogic.getLoginIdByToken(token)` 校验登录态。
5. 握手成功后，把 `userId`、`token`、`username` 放入 WebSocket session attributes。
6. `ChatWebSocketHandler.afterConnectionEstablished` 将 session 放入进程内 `SESSIONS`。
7. 服务端调用 `ChatOnlineUserService.userOnline` 写入 Redis 在线态和数据库在线记录。
8. 服务端向当前用户发送 `CONNECT`，向所有用户广播 `ONLINE_COUNT` 和 `USER_JOIN`。

当前实现里 `WebSocketConfig` 使用 `setAllowedOrigins("*")`。如果部署到公网，建议改为配置化白名单，并评估 query token 在代理日志、浏览器历史和监控链路中的暴露风险。

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
| `HEARTBEAT` | 客户端到服务端 | 已实现 | 服务端只更新 Redis 心跳时间 |
| `ERROR` | 服务端到客户端 | 已实现 | 消息处理失败 |
| `PONG` | 服务端到客户端 | 前端有处理，后端未发送 | 当前前端会等待/计算延迟，但后端没有对应事件 |
| `TYPING` | 双向或广播 | 前端有处理，后端未注册 | 当前后端常量和 handler 未实现输入中广播 |

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
5. `ChatMessageServiceImpl.sendMessage` 获取官方聊天室，检查用户是否被禁言。
6. 服务端写入 `chat_messages`，再查出带用户信息的完整消息。
7. 服务端向其他在线用户广播 `MESSAGE`。
8. 服务端向发送者发送 `MESSAGE_ACK`。
9. 前端收到 ACK 后，把本地消息状态改为 `sent`，并更新为正式消息 ID。

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

## 撤回、删除和禁言

| 动作 | 接口 | 规则 |
| --- | --- | --- |
| 查看历史 | `POST /user/chat/history` | 基于 `lastMessageId` 和 `pageSize` 向上翻页，返回后服务端反转为旧消息在前 |
| 在线人数 | `POST /user/chat/online-count` | 读取官方聊天室 Redis set 大小 |
| 在线用户 | `POST /user/chat/online-users` | 读取 Redis 在线用户信息，同时清理孤儿 session |
| 用户撤回 | `POST /user/chat/message/recall` | 只能撤回自己的消息，发送 120 秒内可撤回 |
| 管理删除 | `DELETE /admin/chat/messages/{id}` | 软删除消息并广播 `MESSAGE_DELETE` |
| 批量删除 | `POST /admin/chat/messages/batch-delete` | 批量软删除 |
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

1. 用户登录后进入 `/chat`，确认 WebSocket 地址为 `/ws/chat?token=...`。
2. 发送文本消息，观察前端状态从 `sending` 变为 `sent`。
3. 开两个浏览器账号，确认一个账号发送消息后，另一个账号收到 `MESSAGE`。
4. 上传小于 5MB 的图片，确认文件先进入文件存储，再发送图片消息。
5. 发送后 2 分钟内撤回，确认双方都删除该消息。
6. 超过 2 分钟后撤回，确认接口返回业务错误。
7. 管理端禁言用户，再用该用户发送消息，确认服务端拒绝。
8. 管理端踢出用户，确认用户端收到 `KICK_OUT` 并断开。
9. 停止心跳或关闭页面，等待 90 秒以上，确认在线用户被清理。
10. 多节点部署前，确认是否需要 Redis Pub/Sub 或消息队列做跨实例广播。

## 常见坑

| 问题 | 原因 | 处理 |
| --- | --- | --- |
| 撤回失败 | 前端仍拿 `tempId` 调接口 | 必须用 `MESSAGE_ACK` 返回的正式 `id` |
| 在线人数不准 | Redis 中有孤儿 session 或心跳超时未清理 | 检查 `chat:online:*` key 和定时任务 |
| 前端显示心跳超时 | 前端期待 `PONG`，但后端当前只更新心跳 | 要么补后端 `PONG`，要么调整前端心跳判断 |
| 输入中状态无效 | 前端有 `TYPING` 处理，后端未广播 | 补 WebSocket 事件常量和 handler 分支 |
| 生产环境 token 暴露风险 | 当前 token 放在 URL query | 改为短时一次性票据或 WebSocket 子协议/握手头方案 |
| 多实例消息丢失 | `SESSIONS` 是单进程内存 Map | 多实例部署需要 Redis Pub/Sub 或消息中间件 |

## 下一步建议

聊天模块已经能支撑单实例实时聊天室，但生产化还需要补三件事：第一，握手凭证从长期 token 改成短时票据；第二，服务端补 `PONG`、`TYPING`、消息频率限制和内容长度校验；第三，多节点部署时把广播从进程内 Map 扩展到 Redis Pub/Sub 或消息队列。
