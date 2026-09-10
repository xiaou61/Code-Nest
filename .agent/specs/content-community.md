---
artifact: spec
status: active
scope: 内容社区相关模块
---

# 内容社区

覆盖 Code-Nest 中 6 个"内容与互动"业务模块：`xiaou-community`、`xiaou-moment`、`xiaou-blog`、`xiaou-codepen`、`xiaou-chat`、`xiaou-notification`。
六个模块均由 `xiaou-application/pom.xml` 聚合（第 94/108/115/155/162/169 行），统一返回 `Result<T>`，用户身份取 `com.xiaou.common.satoken.StpUserUtil`。
本文只记录阅读到的真实事实，判断不了的写"未确认"。

## 模块总览

| 模块 | 职责一句话 | 关键入口（包/类/文件） |
| --- | --- | --- |
| xiaou-community | 论坛式社区：帖子/评论/点赞/收藏/标签/分类/用户封禁/AI 摘要/热门榜 | `community/controller/{admin,pub}`、`service/impl/CommunityPostServiceImpl|CommunityCommentServiceImpl`、`task/CommunityHotPostTask` |
| xiaou-moment | 朋友圈式动态：发布/点赞/评论/收藏/热门/统计 | `moment/controller/{admin,user}`、`service/impl/MomentServiceImpl`、`task/HotMomentCalculateTask|MomentViewSyncTask` |
| xiaou-blog | 个人博客：开通博客、文章发布（积分）、分类/标签、管理端审核置顶 | `blog/controller/{admin,user}`、`service/impl/BlogArticleServiceImpl|BlogConfigServiceImpl` |
| xiaou-codepen | 代码共享器：在线代码作品、Fork（付费/免费）、收藏夹、标签、评论 | `codepen/controller/{admin,user}`、`service/impl/CodePenServiceImpl` |
| xiaou-chat | 单聊天室 IM：WebSocket 实时消息、在线用户、禁言/踢人/撤回/公告 | `chat/websocket/ChatWebSocketHandler`、`chat/config/WebSocketConfig`、`chat/service/impl/ChatMessageServiceImpl` |
| xiaou-notification | 站内消息：发布抽象、个人消息/系统公告、已读记录、模板、管理端统计 | `notification/api/NotificationPublisher|NotificationCommand`、`service/NotificationPublisherImpl|NotificationService` |

## 模块详情

### xiaou-community（xiaou-community）

- **职责与边界**：13 个 Controller（5 admin + 8 pub）、8 个 Service（8 impl，共 16 文件）、9 个 Mapper。承担帖子、评论（含二级回复）、帖子/评论点赞、帖子收藏、标签、分类、社区用户封禁状态、AI 摘要、Redis 热门榜。是否与其他模块重复实现互动逻辑见"跨模块观察"。
- **核心领域对象**：`CommunityPost`、`CommunityComment`、`CommunityPostLike`、`CommunityPostCollect`、`CommunityCommentLike`、`CommunityTag`、`CommunityPostTag`、`CommunityCategory`、`CommunityUserStatus`；配置 `config/CommunityProperties`（cache/hot/ai 三段）。
- **对外接口**（公共前缀 `/community/**`，管理端 `/admin/community/**`）：

| 端 | 方法与路径 |
| --- | --- |
| 公共 | POST `/community/posts/list`、GET `/community/posts/{id}`、POST `/community/posts`、POST/DELETE `/community/posts/{id}/like`、POST/DELETE `/community/posts/{id}/collect`、GET `/community/posts/hot` |
| 公共 | POST `/community/posts/{postId}/comments`、POST `/community/posts/{postId}/comments/create`、POST/DELETE `/community/comments/{id}/like`、POST `/community/comments/{id}/reply`、POST `/community/comments/{id}/replies` |
| 公共 | POST `/community/posts/{id}/generate-summary`、GET `/community/posts/{id}/summary`、GET `/community/categories`、POST `/community/categories/list`、GET `/community/tags`、GET `/community/tags/hot`、POST `/community/tags/{id}/posts` |
| 公共 | GET `/community/init`、GET `/community/user/status`、GET `/community/hot-keywords`、GET `/community/users/{userId}/profile`、POST `/community/users/{userId}/posts`、POST `/community/user/{collections,comments,posts}` |
| 管理 | `/admin/community/posts`（list/get/top/disable/delete）、`/admin/community/comments`、`/admin/community/users`（含 ban/unban、发帖评论记录）、`/admin/community/categories`、`/admin/community/tags` |

- **数据表**（由 mapper XML grep 得到）：`community_post`、`community_comment`、`community_post_like`、`community_post_collect`、`community_comment_like`、`community_tag`、`community_post_tag`、`community_category`、`community_user_status`。
- **关键流程**：
  - 发帖 `createPost`：`checkUserBanStatus()` → 分类名校验（status!=1 抛错）→ insert → `incrementPostCount` → 分类计数 +1 → 标签最多取 5 个、仅 status==1 的标签 `batchInsert` 并逐个 `updatePostCount(+1)`（`CommunityPostServiceImpl` L202-276）。
  - 点赞/收藏：先查唯一记录，存在即抛 `BusinessException("已经点赞过了"/"已经收藏过了")`；成功后 `updateLikeCount/updateCollectCount(±1)` + 用户计数；点赞与收藏均向作者发 `NotificationCommand.community(...)`，自身操作不发（L280-446）。
  - 评论：`SensitiveWordUtils.checkText(content,"community",postId,userId)`，`allowed=false` 抛错，否则写回 `processedText`；一级评论通知帖子作者、回复通知父评论作者（`CommunityCommentServiceImpl` L111-201）；回复统一挂在一级评论下并 `updateReplyCount`（L314-401）。
  - 详情 `getPostDetail`：先走 `CommunityCacheService.getCachedPost`，未命中回源并 `cachePost`，随后 `incrementViewCount`（L184-198）。
  - 热度：`CommunityHotPostTask`（cron `0 */10 * * * ?`）→ `CommunityHotPostServiceImpl.refreshHotPosts` 写 Redisson `RScoredSortedSet` key `community:hot:posts`；分数 = 赞×3 + 评×5 + 藏×8 + 浏览×0.1 − 时间衰减（L148-164）。`getPostDetail` 侧另有一份不含时间衰减的同公式计算（`CommunityPostServiceImpl` L514-516），两处公式不一致。
- **依赖关系**：pom 依赖 `xiaou-common-core/web/security/persistence/cache`、`xiaou-common`、`xiaou-notification`、`xiaou-ai`。未依赖 `xiaou-sensitive-api`，敏感词走 common 的 `SensitiveWordUtils`。
- **约束与注意事项**：`/community/**` 不在 `SaTokenConfig` 的全局拦截规则内（该文件只有 `/auth/**`+`/admin/**` 与 `/user/**` 两条 `SaRouter.match`），写操作靠 service 内 `StpUserUtil.checkLogin()`；`unlikePost/uncollectPost` 未调用 `checkUserBanStatus()`，而被封禁校验只出现在点赞/收藏/发帖/评论路径上。

### xiaou-moment（xiaou-moment）

- **职责与边界**：2 个 Controller（`/user/moments`、`/admin/moments`）、`MomentService`+`MomentViewService` 两个接口与实现、4 个 Mapper；动态发布、评论、点赞、收藏、热门、搜索、统计、浏览数同步。动态发布有独立限频 `checkPublishFrequency`（`MomentServiceImpl` L434，实现未读）。
- **核心领域对象**：`Moment`、`MomentComment`、`MomentLike`、`MomentFavorite`；枚举 `MomentStatus`、`CommentStatus`；DTO 侧 `MomentListResponse`、`DailyStatistics` 等。
- **对外接口**：

| 端 | 方法与路径 |
| --- | --- |
| 用户 | POST `/user/moments/publish`、POST `/user/moments/list`、DELETE `/user/moments/{id}`、POST `/user/moments/{momentId}/like`、POST `/user/moments/{momentId}/favorite` |
| 用户 | POST `/user/moments/comment`、DELETE `/user/moments/comments/{id}`、POST `/user/moments/comments`、POST `/user/moments/my-favorites` |
| 用户 | POST `/user/moments/hot`、POST `/user/moments/search`、POST `/user/moments/user-list`、POST `/user/moments/user-info` |
| 管理 | POST `/admin/moments/list`、POST `/admin/moments/batch-delete`、POST `/admin/moments/comments/list`、DELETE `/admin/moments/comments/{id}`、POST `/admin/moments/statistics`（均 `@RequireAdmin`） |

- **数据表**：`moments`、`moment_comments`、`moment_likes`、`moment_favorites`。
- **关键流程**：
  - 点赞/收藏为 toggle 语义：已存在则删除并递减计数、返回 false；不存在则 `insertIfAbsent`（XML 为 `INSERT IGNORE`），插入成功才 `incrementLikeCount/incrementFavoriteCount` 并返回 true（L133-185、L694-747）。重复请求不抛异常，与 community/codepen 语义不同。
  - 评论：`SensitiveWordUtils.checkText(content,"moment_comment",momentId,userId)`，不允许即抛错，写入 `processedText`；随后通知动态作者（L189-242）。
  - 通知统一使用 `NotificationCommand.personal(...)`（L172、L230、L734），因此 `sourceModule` 为 null，不带社区类来源标记。
  - 计数：`moments` 的 XML 递减语句带 `AND like_count > 0` / `AND comment_count > 0` / `AND favorite_count > 0` 保护（`MomentMapper.xml` L107/115/206）。
  - 定时任务：`HotMomentCalculateTask` cron `0 */10 * * * ?`、`MomentViewSyncTask` cron `0 0 * * * ?`（后者与 `MomentViewService` 的实现细节未读）。
- **依赖关系**：pom 依赖 `xiaou-common-*`、`xiaou-common`、`xiaou-notification`、`xiaou-user-api`、`xiaou-sensitive-api`。读到的 `MomentServiceImpl` 敏感词走 common 的 `SensitiveWordUtils`；pom 中 `xiaou-sensitive-api` 是否另有使用未确认。
- **约束与注意事项**：`/user/moments/**` 命中 `SaTokenConfig` 的 `/user/**` 拦截；service 内仍以 `getLoginIdAsLong()`+null 判断兜底。表名不一致（`moments` 复数无前缀 vs `moment_comments`）。

### xiaou-blog（xiaou-blog）

- **职责与边界**：2 个 Controller（`/user/blog`、`/admin/blog`）、4 个 Service+实现、4 个 Mapper；开通博客、文章草稿/发布/更新/删除、分类、标签（含合并）、管理端置顶与状态审核。无点赞/收藏/评论能力，也无站内通知接入。
- **核心领域对象**：`BlogArticle`、`BlogConfig`、`BlogCategory`、`BlogTag`。
- **对外接口**：

| 端 | 方法与路径 |
| --- | --- |
| 用户 | POST `/user/blog/open`、GET `/user/blog/check-status`、GET `/user/blog/config/{userId}`、POST `/user/blog/config/update` |
| 用户 | POST `/user/blog/article/create`、`/article/publish`、`/article/update/{id}`、DELETE `/article/{id}`、GET `/article/{id}`、POST `/article/list`、`/article/my-list`、`/article/draft-list`、`/article/by-category` |
| 用户 | GET `/user/blog/categories`、GET `/user/blog/tags`、GET `/user/blog/tags/hot` |
| 管理 | GET `/admin/blog/statistics`、POST `/article/list`、`/article/top`、`/article/cancel-top`、`/article/update-status`、DELETE `/article/{id}`、分类 CRUD、标签 list/merge/delete（均 `@RequireAdmin`） |

- **数据表**：`blog_article`、`blog_config`、`blog_category`、`blog_tag`。
- **关键流程**：
  - 发文章 `publishArticle`（`BlogArticleServiceImpl` L79 起）：`sensitiveCheckService.containsSensitiveWords(title|content,"blog")` 审核 → 校验余额 `pointsBalanceMapper.selectByUserId` 不足 `PUBLISH_ARTICLE_POINTS` 抛错 → `subtractPoints` → 写 `UserPointsDetail`（`PointsType.ADMIN_GRANT`，`pointsChange = -PUBLISH_ARTICLE_POINTS`）→ 返回 `pointsRemaining`。控制层注释标注发布扣 20 积分、开通博客扣 50 积分（`BlogUserController` L39/L89 Javadoc）。
  - 管理端状态值由 `@Min(0)@Max(3)` 约束（`BlogAdminController` L96）。
- **依赖关系**：pom 依赖 `xiaou-common-core/web/security/persistence`、`xiaou-user-api`、`xiaou-points`、`xiaou-sensitive-api`；**不依赖 `xiaou-notification`**，因此博客无任何站内消息产出。
- **约束与注意事项**：积分逻辑直接调用 `xiaou-points` 的 Mapper（`UserPointsBalanceMapper`/`UserPointsDetailMapper`）读写积分表，跨模块直连数据访问；是否应改走 points 对外 API 未确认。

### xiaou-codepen（xiaou-codepen）

- **职责与边界**：2 个 Controller（`/user/code-pen` 约 40 个端点、`/admin/code-pen`）、4 个 Service+实现、7 个 Mapper；代码作品 CRUD、Fork（免费/付费）、点赞、收藏与收藏夹、评论、标签、模板、推荐位、收益统计。
- **核心领域对象**：`CodePen`、`CodePenComment`、`CodePenCollect`、`CodePenFolder`、`CodePenForkTransaction`、`CodePenLike`、`CodePenTag`；`config/CodePenProperties`、`constant/CodePenConstants`。
- **对外接口**（节选，方法名即语义）：用户端 POST `/user/code-pen/create|save|update|fork|list|search|by-tag|by-category|by-user|like|unlike|collect|uncollect|view|comment|comment-list|folder/*|my-list|draft-list|check-fork-price|income-stats|recommend-list`，GET `/user/code-pen/{id}|hot|templates|template/{id}|tags|tags/hot`，DELETE `/{id}` 与 `/comment/{id}`、`/folder/{id}`；管理端 `/admin/code-pen/list|{id}|update-status|recommend|cancel-recommend|template/*|tag/*|comment/*|statistics`（均 `@RequireAdmin`）。
- **数据表**：`code_pen`、`code_pen_comment`、`code_pen_collect`、`code_pen_folder`、`code_pen_fork_transaction`、`code_pen_like`、`code_pen_tag`。
- **关键流程**：
  - 点赞 `like`：已存在则抛 `BusinessException("已经点赞过了")`，否则 insert + `incrementLikeCount`；`unlike` 不做存在性校验直接 delete+`decrementLikeCount`（L451-475）。收藏同理（L479-504）。**无通知产出**。
  - Fork：`forkPen` + `checkForkPrice` + `ensureUserPointsBalance`，返回 `ForkResponse(pointsRemaining)`；依赖 `xiaou-points`（细节未读）。
  - 代码可见性：`canViewCode`/`getPurchasedOriginalPenIds` 决定付费作品是否展示源码（L711-741）。
- **依赖关系**：pom 依赖 `xiaou-common-core/web/security/persistence`、`xiaou-user-api`、`xiaou-sensitive-api`、`xiaou-points`；**不依赖 `xiaou-notification`**；无定时任务；未读到敏感词调用（pom 声明的 `xiaou-sensitive-api` 实际用途未确认）。
- **约束与注意事项**：`CodePenAdminController` 直接注入 `CodePenMapper` 执行 `updateStatus/setRecommend/cancelRecommend/deleteById/insert`（L71-130、L151-185），绕过 service 层与事务边界；管理端 `getDetail` 强制 `setCanViewCode(true)`。

### xiaou-chat（xiaou-chat）

- **职责与边界**：2 个 Controller（`/user/chat`、`/admin/chat`）、5 个 Service 接口+4 个实现、票据/限流为无接口 `@Service`、4 个 Mapper、`websocket` 包 3 类、1 个定时任务类。
- **WebSocket 结论（已确认）**：pom 引入 `spring-boot-starter-websocket`（L51-54）；`WebSocketConfig` 上 `@EnableWebSocket`，注册 `ChatWebSocketHandler` 到 **`/ws/chat`**，加 `SaTokenWebSocketInterceptor`，允许来源读配置 `xiaou.cors.allowed-origin-patterns`。认证方式不是 Sa-Token 头，而是**一次性握手票据**：`POST /user/chat/ws-ticket` → `ChatWebSocketTicketService.createTicket`（32 字节 `SecureRandom`、Base64URL、TTL 60s、key `xiaou:chat:ws-ticket:`、`cacheStore.take()` 单次消费）；拦截器从 query `ticket` 取票据换 `userId/username`，失败返回 false。
- **核心领域对象**：`ChatMessage`、`ChatOnlineUser`、`ChatRoom`、`ChatUserBan`；`WebSocketMessage`（CONNECT/USER_JOIN/HEARTBEAT/PONG/TYPING/MESSAGE/MESSAGE_ACK/ERROR 等类型）。
- **对外接口**：

| 端 | 方法与路径 |
| --- | --- |
| 用户 | POST `/user/chat/ws-ticket`、`/history`、`/online-count`、`/online-users`、`/message/recall` |
| 管理 | POST `/admin/chat/messages/list`、DELETE `/admin/chat/messages/{id}`、POST `/admin/chat/messages/batch-delete`、POST `/admin/chat/users/online`、`/users/kick`、`/users/ban`、`/users/ban/active`、`/users/unban`、`/announcement`（均 `@RequireAdmin`） |
| WS | `/ws/chat?ticket=...` |

- **数据表**：`chat_messages`、`chat_online_users`、`chat_rooms`、`chat_user_bans`。
- **关键流程**：
  - 消息链路：WS `handleTextMessage` 分流 HEARTBEAT/TYPING/MESSAGE → `ChatRateLimitService.tryAcquireMessage` → `ChatMessageService.sendMessage`（禁言校验 `chatUserBanService.isUserBanned`、消息类型 1 文本/2 图片、长度与图片 URL 白名单校验、回复摘要截断 50 字）→ 广播给其他会话 + 给发送方回 `MESSAGE_ACK`（`ChatWebSocketHandler` L93-181）。
  - 限流：`xiaou.chat.rate-limit.*`，默认 8 条/10 秒、typing 12/10 秒；缓存不可用时按可用性策略**放行**并打 warn（`ChatRateLimitService` L49-66）。
  - 撤回：仅本人、`RECALL_TIME_LIMIT=120` 秒内，软删 `is_deleted=1`；`convertToResponse` 用同一 120 秒规则设置 `canRecall`。
  - 公告：`sendAnnouncement` 以 `userId=0`、`messageType=3` 落库，再经 WS `sendSystemMessage` 广播（`ChatAdminController` L156-165）。
  - 在线状态：内存 `static final Map<String, WebSocketSession> SESSIONS` + 数据表 `chat_online_users` 双写；新连接会 `closeExistingSessionsForUser` 踢掉旧会话。
  - 定时任务 `ChatScheduledTask`：cron `0 * * * * ?` 自动解除过期禁言、`*/30 * * * * ?` 清理超时在线用户。
- **依赖关系**：pom 依赖 `xiaou-common-core/web/security/persistence/cache` + websocket；**不依赖 `xiaou-notification`、`xiaou-sensitive-api`**，消息内容无敏感词过滤调用（读到的 `ChatMessageServiceImpl` 中只有格式与长度校验）。
- **约束与注意事项**：会话表为 JVM 静态 Map，进程内单机状态；多实例部署时跨节点广播/踢人失效（是否已限制单实例部署未确认）。用户端历史/在线接口用 `chatRoomService.getOfficialRoom()`，即当前只有单一官方聊天室。

### xiaou-notification（xiaou-notification）

- **职责与边界**：2 个 Controller、3 个 Service（`NotificationService` 内部服务、`NotificationAdminService`、`NotificationUserService`）+ `NotificationPublisherImpl`、4 个 Mapper、`api` 包（对外发布抽象）、`config/NotificationAsyncConfig`、4 个枚举。
- **投递渠道（已确认）**：**仅站内（数据库表 `notification`）**。pom 无 mail/SMS/MQ/websocket 依赖，只有 `xiaou-common-*` 与 `xiaou-user-api`。链路：`NotificationPublisher.publish` → `NotificationPublisherImpl.toNotification`（senderId 固定 0）→ `NotificationService.sendNotification` 同步 `insert`；批量走 `publishBatchAsync` → `sendBatchNotifications`（`@Async("notificationExecutor")` + `batchInsert`）。无重试、无失败落表、无推送通道。
- **核心领域对象**：`Notification`（含 receiverId、type、priority、status、sourceModule、sourceId）、`NotificationConfig`、`NotificationTemplate`、`NotificationUserReadRecord`；枚举 `NotificationTypeEnum`（PERSONAL/SYSTEM/COMMUNITY_INTERACTION/INTERVIEW_REMINDER/ANNOUNCEMENT）、`NotificationPriorityEnum`（LOW…）、`NotificationStatusEnum`（UNREAD/READ，READ 以字符串 `"READ"` 比较）、`NotificationSourceEnum`（system/community/interview/user/filestorage/monitor/growth_coach）。
- **对外接口**：

| 端 | 方法与路径 |
| --- | --- |
| 用户 | POST `/notification/list`、GET `/notification/unread-count`、GET `/notification/{id}`、POST `/notification/mark-read`、`/notification/delete`、`/notification/mark-all-read` |
| 管理 | POST `/admin/notification/announcement`、`/statistics`、`/list`、`/batch-send`、`/delete/{id}`、GET+POST `/templates`、PUT+DELETE `/templates/{id}`（均 `@RequireAdmin`） |

- **数据表**：`notification`、`notification_config`、`notification_template`、`notification_user_read_record`。
- **关键流程**：
  - 公告广播模型：`NotificationCommand.announcement(title,content,priority)` 的 `receiverId=null`；用户读公告不更新消息行，而是向 `notification_user_read_record` 插入 `(userId, notificationId, readTime)`；个人消息才 `notificationMapper.markAsRead(messageId,userId)`（`NotificationService` L108-201）。未读数走 `countUnreadWithReadRecord`，查询走 `selectByUserIdWithReadRecord`——即公告已读状态按用户维度单独计算。
  - 批量发送：`NotificationAdminService.batchSendMessage` 先对每个 `receiverId` 调 `requireExistingUser`，再 `publishBatchAsync`（L125-153）；`publishAnnouncement` 直接 publish（L155-158）。
  - 触发来源（全仓 grep）：`xiaou-community` 4 处（帖子点赞、帖子收藏、评论创建/回复、评论点赞，均 `NotificationCommand.community`）；`xiaou-moment` 3 处（点赞/评论/收藏，`NotificationCommand.personal`）；`xiaou-user` 3 处 system；`xiaou-interview` 1 处 interview；`xiaou-application`(growth-coach)、`xiaou-learning-asset`、`xiaou-plan`(PlanRemindScheduler) 各 1 处。blog/codepen/chat 无调用点。
- **依赖关系**：被 `xiaou-community`、`xiaou-moment` 直接依赖；自身依赖 `xiaou-user-api`（`requireExistingUser`、用户信息）。
- **约束与注意事项**：用户端路径前缀是 `/notification/**`，既不在 `/user/**`，也不在 `SaTokenConfig` 的全局拦截规则内（该文件只 match `/auth/**`、`/admin/**`、`/user/**`），登录态完全取决于 `NotificationUserService` 内部实现（**未确认**其是否调用 `StpUserUtil.checkLogin()`）。

## 跨模块观察

1. **评论/点赞/收藏没有统一抽象，是 3 套并行实现**。全仓 grep `interface .*(Interaction|Like|Collect|Favorite)` 在 `xiaou-common*` 中零命中，命中的全是模块内 Mapper/Service（如 `CommunityPostLikeMapper`、`MomentLikeMapper`、`CodePenLikeMapper`）。语义还不一致：

| 维度 | community | moment | codepen |
| --- | --- | --- | --- |
| 点赞接口形态 | `like`/`unlike` 两个方法，返回 `Result<Void>` | 单个 `toggleLike` 返回 `Boolean` | `like`/`unlike` 两个方法返回 `boolean` |
| 重复点赞 | 抛 `已经点赞过了` | `INSERT IGNORE` 幂等，返回 true | 抛 `已经点赞过了` |
| 计数下限保护 | `updateLikeCount(±1)`，无下限 | XML `AND like_count > 0` | `decrementLikeCount`，未确认 |
| 通知 | 有（community） | 有（personal） | 无 |
| 表 | `community_post_like`/`community_comment_like`/`community_post_collect` | `moment_likes`/`moment_favorites` | `code_pen_like`/`code_pen_collect` |

2. **通知接入面很窄**：只有 community、moment 两个模块向 `NotificationPublisher` 发消息，blog/codepen/chat 连依赖都没有；通知模块自身只有站内落库，前端拿未读数只能轮询 `GET /notification/unread-count`（chat 虽已有 WebSocket，但与 notification 无任何耦合）。
3. **敏感词两套机制并存**：community/moment 用 common 的 `SensitiveWordUtils.checkText(text, 模块字符串, 业务ID, userId)`（模块名硬编码为字面量 `"community"`、`"moment_comment"`）；blog 用 `xiaou-sensitive-api` 的 `SensitiveCheckService.containsSensitiveWords(text,"blog")`；codepen/chat 读到的代码里没有检测调用。
4. **鉴权分层不一致**：`SaTokenConfig` 只覆盖 `/auth/**`、`/admin/**`、`/user/**`，而 `/community/**` 与 `/notification/**` 都靠业务层自证登录，且 community 只有部分写方法调用 `checkUserBanStatus()`，`unlikePost/uncollectPost` 没有封禁校验。
5. **计数正确性风险**：社区侧点赞/收藏计数为无保护 `±1`，moment 侧有 `>0` 保护；热度公式在 `CommunityHotPostServiceImpl` 与 `CommunityPostServiceImpl.convertToResponse` 各写一份且不一致（后者无时间衰减）。
6. **跨模块直连数据层**：blog、codepen 依赖 `xiaou-points` 并直接使用其 `UserPointsBalanceMapper`/`UserPointsDetailMapper`，积分扣减逻辑写在博客/代码共享模块内，与 `.agent/rules/always.md`「业务逻辑落在对应业务模块」的精神相冲突（points 是否提供对外 API 未确认）。
7. **管理端越层**：`CodePenAdminController` 直接调用 Mapper 写库，绕过 service 事务与校验，与 `always.md` 的分层意图不符。
8. **与 `always.md` 的表述冲突**：rules 把 `/community/**` 归入"公共接口"，但该前缀下存在必须登录的写操作（发帖/评论/点赞/收藏），实际是"路径公共分区 + 方法级鉴权"；`/notification/**` 未被 rules 的任何路由分区覆盖，建议在规则中补录。
9. **定时与异步落点**：community `0 */10 * * * ?`、moment `0 */10 * * * ?` 与 `0 0 * * * ?`、chat `0 * * * * ?` 与 `*/30 * * * * ?`；异步仅 notification 的 `@Async("notificationExecutor")`（`NotificationAsyncConfig` 上 `@EnableAsync`），全局 `@EnableAsync`/`@EnableScheduling` 在 `xiaou-bootstrap` 主类。

## 未确认

- `MomentViewServiceImpl`、`MomentViewSyncTask`、`HotMomentCalculateTask` 的实现细节（`MomentServiceImpl` 的 `checkPublishFrequency`、统计与批量转换方法）未读全文。
- `BlogConfigServiceImpl.openBlog` 的 50 积分实现细节（仅由 `BlogUserController` Javadoc 得知）；`CodePenServiceImpl` 的 `forkPen`/积分流程未读全文。
- `NotificationUserService` 是否在内部校验登录态（决定 `/notification/**` 的实际安全性）。
- `ChatOnlineUserServiceImpl`、`ChatWebSocketHandler` 211 行之后（踢人/断线/广播实现）未读。
- `xiaou-points` 是否对外提供积分 API（影响第 6 条结论的严重度）。
- `xiaou-sensitive-api` 在 codepen 与 moment 中的实际使用点；chat/blog 是否有其他位置的敏感词调用。
- 各表是否已存在于 `sql/` 基线与增量脚本（未读 sql 目录）。
- Mapper XML 只用于提取表名与关键语句，未逐行审阅其 SQL 正确性。
