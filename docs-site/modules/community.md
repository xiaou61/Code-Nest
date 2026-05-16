# 社区帖子

社区模块承载帖子、评论、分类、标签、收藏、点赞、用户主页和 AI 摘要。

## 功能入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/community`、`/community/posts/:id`、`/community/collections`、`/community/my-posts`、`/community/create`、`/community/users/:userId` |
| 管理端 | `/community/categories`、`/community/tags`、`/community/posts`、`/community/comments`、`/community/users` |
| 后端模块 | `xiaou-community` |

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
- Markdown 和富文本展示要净化。
- 删除、禁用和封禁要保留审计信息。
- AI 摘要需要失败兜底，不能阻塞主链路。

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

## 验证点

| 场景 | 预期 |
| --- | --- |
| 禁用分类下发帖 | 返回“所选分类已被禁用” |
| 重复点赞或收藏 | 返回已经点赞/收藏过 |
| 评论含敏感词 | 禁止发布或使用处理后的文本 |
| 被封禁用户互动 | 返回封禁原因 |
| AI 未启用生成摘要 | 返回“AI功能未启用” |
