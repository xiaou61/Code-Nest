# 社区帖子

社区模块承载帖子、评论、分类、标签、收藏、点赞、用户主页和 AI 摘要。

## 功能入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/community`、`/community/posts/:id`、`/community/collections`、`/community/my-posts`、`/community/create`、`/community/users/:userId` |
| 管理端 | `/community/categories`、`/community/tags`、`/community/posts`、`/community/comments`、`/community/users` |
| 后端模块 | `xiaou-community` |

## 推荐学习顺序

社区模块建议按“内容模型 -> 互动模型 -> 运营模型”的顺序读：

1. 先看分类和标签，理解帖子为什么需要 `categoryId` 和最多 5 个标签。
2. 再看 `CommunityPostServiceImpl.createPost`，读懂发帖、敏感词、计数和标签关系。
3. 接着看点赞、收藏和评论，理解关系表如何防重复。
4. 然后看用户社区状态，理解封禁为什么独立于基础用户表。
5. 最后看 AI 摘要和缓存，学习一个可选 AI 能力如何接入内容详情页。

## 源码地图

| 目标 | 文件 |
| --- | --- |
| 页面初始化 | `xiaou-community/src/main/java/com/xiaou/community/controller/pub/CommunityPublicController.java` |
| 帖子接口 | `xiaou-community/src/main/java/com/xiaou/community/controller/pub/CommunityPostController.java` |
| 评论接口 | `xiaou-community/src/main/java/com/xiaou/community/controller/pub/CommunityCommentController.java` |
| 用户主页 | `xiaou-community/src/main/java/com/xiaou/community/controller/pub/CommunityUserProfileController.java` |
| AI 摘要接口 | `xiaou-community/src/main/java/com/xiaou/community/controller/pub/CommunityAiController.java` |
| 帖子服务 | `xiaou-community/src/main/java/com/xiaou/community/service/impl/CommunityPostServiceImpl.java` |
| 评论服务 | `xiaou-community/src/main/java/com/xiaou/community/service/impl/CommunityCommentServiceImpl.java` |
| 用户状态服务 | `xiaou-community/src/main/java/com/xiaou/community/service/impl/CommunityUserStatusServiceImpl.java` |
| 摘要服务 | `xiaou-community/src/main/java/com/xiaou/community/service/impl/CommunityAiSummaryServiceImpl.java` |
| 社区配置 | `xiaou-community/src/main/java/com/xiaou/community/config/CommunityProperties.java` |

## 用户侧接口域

| 接口域 | 能力 |
| --- | --- |
| `/community/init` | 页面初始化数据 |
| `/community/categories` | 分类列表 |
| `/community/tags` | 标签、热门标签、按标签查帖子 |
| `/community/posts` | 帖子列表、详情、创建、点赞、收藏、热门帖子 |
| `/community/posts/{id}/generate-summary` | AI 生成摘要 |
| `/community/posts/{id}/summary` | 获取摘要 |
| `/community/posts/{postId}/comments` | 评论列表 |
| `/community/posts/{postId}/comments/create` | 创建评论 |
| `/community/comments/{id}` | 评论点赞、回复和回复列表 |
| `/community/user` | 我的收藏、评论、帖子 |
| `/community/users/{userId}/profile` | 用户主页 |

## 管理侧接口域

| 接口域 | 能力 |
| --- | --- |
| `/admin/community/categories` | 分类增删改查和状态 |
| `/admin/community/tags` | 标签增删改查 |
| `/admin/community/posts` | 帖子列表、详情、置顶、取消置顶、禁用、删除 |
| `/admin/community/comments` | 评论列表、详情、删除 |
| `/admin/community/users` | 社区用户、封禁、解封、用户帖子和评论 |

## 风控点

- 发帖、评论、回复需要敏感词检测。
- Markdown 和富文本展示要净化，前端统一参考 [前端渲染安全](/reference/frontend-rendering-security)。
- 删除、禁用和封禁要保留审计信息。
- AI 摘要需要失败兜底，不能阻塞主链路。

## 一次发帖如何完成

发帖入口是 `/community/posts`，主流程可以拆成：

| 步骤 | 说明 |
| --- | --- |
| 登录和封禁检查 | 读取当前用户，并调用社区用户状态服务判断是否被封禁 |
| 分类校验 | 如果传了分类，分类必须存在且 `status = 1` |
| 敏感词处理 | 对标题和正文做社区模块敏感词检测 |
| 写帖子主表 | `community_post.status` 默认写为 `1` |
| 处理标签 | 最多取前 5 个标签，只关联存在且启用的标签 |
| 更新计数 | 更新用户发帖数、分类帖子数、标签帖子数 |
| 清理缓存 | 后续列表和详情通过缓存服务重新回填 |

这个流程里最容易忽略的是“标签会被截断到 5 个”。如果前端传了 8 个标签，后端不会报错，而是只保存前 5 个可用标签。

## 帖子和互动规则

帖子主链路在 `CommunityPostServiceImpl`：

| 动作 | 规则 |
| --- | --- |
| 创建帖子 | 登录用户可创建；如果指定分类，分类必须存在且启用；最多关联 5 个启用标签 |
| 帖子详情 | 先读缓存，未命中查库并回填缓存；详情访问会增加浏览数 |
| 点赞 | 同一用户同一帖子只能点赞一次；点赞后增加帖子点赞数和用户点赞统计 |
| 收藏 | 同一用户同一帖子只能收藏一次；收藏后增加帖子收藏数和用户收藏统计 |
| 删除 | 管理端软删除后清帖子缓存，并减少作者发帖数 |
| 下架 | 管理端将帖子置为不可见并清缓存 |
| 置顶 | 管理端按小时计算置顶过期时间 |

帖子热度分在响应组装时按 `likeCount * 3 + commentCount * 5 + collectCount * 8 + viewCount * 0.1` 计算。

## 缓存和热门帖子

社区配置在 `CommunityProperties`：

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `cache.hotPostsTtl` | 600 秒 | 热门帖子缓存 |
| `cache.postDetailTtl` | 1800 秒 | 帖子详情缓存 |
| `cache.userInfoTtl` | 3600 秒 | 用户信息缓存 |
| `cache.tagsTtl` | 86400 秒 | 标签缓存 |
| `hot.timeRangeHours` | 72 小时 | 热门统计时间窗口 |
| `hot.minScore` | 30 | 入选热门的最低分 |
| `hot.limit` | 10 | 默认热门数量 |
| `hot.refreshIntervalMinutes` | 10 分钟 | 热门刷新间隔 |

读详情时是缓存优先，未命中再查数据库并回填；同时会增加浏览数。排查“列表数据不刷新”时，要先判断是缓存 TTL 未过期，还是写操作没有清对应缓存。

## 评论树

评论主链路在 `CommunityCommentServiceImpl`：

| 动作 | 规则 |
| --- | --- |
| 一级评论 | 评论前检查用户封禁和敏感词；创建后增加帖子评论数和用户评论数 |
| 回复评论 | 所有回复都挂到一级评论下，保留 `replyToId`、`replyToUserId`、`replyToUserName` |
| 评论列表 | 一级评论分页返回，每条评论默认带最多 2 条回复 |
| 评论点赞 | 同一用户同一评论只能点赞一次 |
| 删除评论 | 管理端删除后减少用户评论数 |

发评论、回复、点赞和收藏都会尝试发站内通知，通知失败只记录日志，不阻断主链路。

## 用户社区状态

用户状态在 `community_user_status` 中维护，服务入口为 `CommunityUserStatusServiceImpl`：

| 字段 | 用途 |
| --- | --- |
| `postCount` | 用户发帖数 |
| `commentCount` | 用户评论数 |
| `likeCount` | 用户点赞行为统计 |
| `collectCount` | 用户收藏行为统计 |
| `isBanned` | 是否封禁 |
| `banReason`、`banExpireTime` | 封禁原因和过期时间 |

被封禁用户在发帖、评论、回复、点赞和收藏前会被拦截；封禁过期后访问时会自动解封。

## AI 摘要

摘要服务是 `CommunityAiSummaryServiceImpl`，配置前缀是 `community.ai`：

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | `false` | 未启用时生成摘要接口直接返回业务错误 |
| `minContentLength` | `500` | 自动摘要的最小正文长度，当前预留 |
| `summaryCacheTtl` | `2592000` 秒 | 摘要缓存 TTL |

摘要缓存 Key 为 `community:post:summary:{postId}`。生成时优先读缓存，再读数据库；强制刷新会调用 `AiCommunityService.generatePostSummary`，成功后写回帖子表、清帖子详情缓存并写摘要缓存。

## 核心数据表

| 表 | 作用 | 排查时重点看 |
| --- | --- | --- |
| `community_post` | 帖子主表 | `status`、分类、浏览/点赞/评论/收藏数、摘要字段 |
| `community_category` | 分类 | `status`、帖子数、排序 |
| `community_tag` | 标签 | `status`、`post_count`、颜色和排序 |
| `community_post_tag` | 帖子标签关系 | 标签筛选异常时检查 |
| `community_post_like` | 帖子点赞关系 | 防重复点赞 |
| `community_post_collect` | 帖子收藏关系 | 防重复收藏 |
| `community_comment` | 评论和回复 | `parent_id`、`reply_to_id`、`status` |
| `community_comment_like` | 评论点赞关系 | 防重复点赞 |
| `community_user_status` | 社区用户状态 | 发帖数、评论数、封禁原因和过期时间 |

## 常见坑

| 问题 | 常见原因 | 排查方式 |
| --- | --- | --- |
| 发帖提示分类禁用 | 分类不存在或 `status != 1` | 查 `community_category` |
| 前端传了很多标签但只保存部分 | 后端最多取前 5 个，并过滤禁用标签 | 查 `community_post_tag` |
| 重复点赞/收藏失败 | 关系表已经存在记录 | 查 `community_post_like` 或 `community_post_collect` |
| 回复显示层级不对 | 所有回复都挂到一级评论下，目标用户靠 `replyTo*` 字段表达 | 看 `parent_id` 和 `reply_to_id` |
| 封禁过期后仍不能操作 | 过期自动解封依赖访问时检查 | 重新触发用户状态检查并查 `ban_expire_time` |
| AI 摘要生成失败 | `community.ai.enabled=false` 或 AI 服务异常 | 先看配置，再看 `community:post:summary:{postId}` 缓存 |

## 验证清单

| 场景 | 预期 |
| --- | --- |
| 禁用分类下发帖 | 返回“所选分类已被禁用” |
| 重复点赞或收藏 | 返回已经点赞/收藏过 |
| 评论含敏感词 | 禁止发布或使用处理后的文本 |
| 被封禁用户互动 | 返回封禁原因 |
| AI 未启用生成摘要 | 返回“AI功能未启用” |
| 帖子带 8 个标签 | 最多只保存前 5 个启用标签 |
| 一级评论带多条回复 | 列表默认只带前 2 条，并标记是否还有更多 |
