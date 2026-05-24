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

---

## 深度拆解

### 一、发帖流程深度分析

`CommunityPostServiceImpl.createPost` 完整流程：

```text
1. checkUserBanStatus → 被封禁则抛原因
2. StpUserUtil.checkLogin + getLoginIdAsLong
3. categoryId 非空 → 校验分类存在且 status=1
4. 构建 CommunityPost:
   ├─ authorName = SaTokenUserUtil.getCurrentUserUsername("用户"+id)
   ├─ 四计数全部=0, isTop=0, status=1
5. insert → result<=0 抛"发布失败"
6. incrementPostCount(authorId)
7. categoryId != null → updatePostCount(categoryId, 1)
8. 处理标签:
   ├─ tagIds.size() > 5 → subList(0, 5) 静默截断
   ├─ 逐个 selectById(tagId) → tag != null && status==1 才关联
   ├─ batchInsert(postTags)
   └─ 逐个 updatePostCount(tagId, 1) 增量
```

**标签关联 N+1**：每验证一个标签 = 1 次 SELECT，创建后每更新一个标签帖子数 = 1 次 UPDATE。5 标签 = 5 SELECT + 5 UPDATE = 10 次额外 SQL。

**authorName 非规范化**：`authorName` 冗余在帖子表中，用户改名后帖子表不更新——与 OJ 评论系统同样的反模式。

### 二、点赞/收藏幂等性深度分析

**点赞** `likePost`：

```text
1. checkUserBanStatus
2. selectByPostIdAndUserId(postId, userId) → 已存在 → 抛"已经点赞过了"
3. insert(CommunityPostLike) → userName 冗余
4. updateLikeCount(postId, 1) → 增量
5. incrementLikeCount(userId) → 用户统计
6. 通知帖子作者 (catch 不阻断)
```

**取消点赞** `unlikePost`：与点赞对称，delete + updateLikeCount(-1) + decrementLikeCount。

**收藏** `collectPost/uncollectPost`：与点赞结构完全对称。

**并发安全**：`selectByPostIdAndUserId` + `insert` 不是原子操作——并发点赞可能突破 exists 检查导致重复插入。**没有数据库唯一约束**（`post_id, user_id`）保护。所有模块的点赞/收藏都存在这个问题。

### 三、评论与回复深度分析

**一级评论** `createComment`：

```text
1. checkUserBanStatus
2. 帖子存在校验
3. 敏感词检测 → SensitiveWordUtils.checkText → 违规则抛"包含违规内容"
4. 敏感词替换 → 使用 processedText
5. parentId=null → 一级评论
6. updateCommentCount(postId, 1) → 帖子计数+1
7. incrementCommentCount(userId) → 用户统计+1
8. 通知帖子作者 (catch 不阻断)
```

**回复评论** `replyComment`：

```text
1. checkUserBanStatus
2. 被回复评论存在校验
3. 敏感词检测+替换
4. 关键: parentId = parentComment.parentId == 0 ? commentId : parentComment.parentId
   → 所有回复都挂在一级评论下（扁平化二级结构）
5. replyToId = commentId (被回复的评论ID)
6. replyToUserId = request.replyToUserId
7. replyToUserName: 先尝试从 CommunityUserStatus 获取，失败则 fallback "用户"+id
8. updateReplyCount(parentCommentId, 1) → 一级评论的 replyCount+1
9. updateCommentCount(postId, 1) → 帖子评论数+1
10. incrementCommentCount(userId)
11. 通知被回复用户 (catch 不阻断)
```

**评论列表** `getPostComments`：一级评论分页，每条默认带 2 条回复（`selectRepliesByCommentId(id, 2)`），标记 `hasMoreReplies = replyCount > 2`。

**N+1 问题**：列表中每条一级评论多 2 次 SELECT（replies + isLiked 检查）。一页 10 条评论 = 10×(1+2+1) = 40 次 SQL。

### 四、热度公式深度分析

`convertToResponse` 中计算：

```text
hotScore = likeCount * 3.0 + commentCount * 5.0 + collectCount * 8.0 + viewCount * 0.1
```

| 行为 | 权重 | 推导 |
| --- | --- | --- |
| 收藏 | 8.0 | 最高权重，表示深度认可 |
| 评论 | 5.0 | 次高，表示互动参与 |
| 点赞 | 3.0 | 最低互动，轻量认可 |
| 浏览 | 0.1 | 几乎不贡献热度 |

**没有时间衰减**：一个一年前的热门帖子和一个新帖子的热度公式完全相同。长时间后，旧帖子的累积 viewCount 会使其持续排在前面。

**热门帖子定时任务** `CommunityHotPostTask`：按 `community.hot.refreshIntervalMinutes`（默认10分钟）定时刷新，计算72小时内热度分 >= 30 的前10条帖子，缓存到 `community:hot:posts`。

### 五、缓存策略深度分析

`CommunityCacheServiceImpl` 提供三级缓存：

| 缓存 Key | TTL | 用途 |
| --- | --- | --- |
| `community:post:detail:{id}` | 1800s | 帖子详情 |
| `community:hot:posts` | 600s | 热门帖子列表 |
| `community:user:info:{id}` | 3600s | 用户信息 |
| `community:tags:enabled` | 86400s | 启用标签列表 |
| `community:post:summary:{id}` | 2592000s | AI 摘要 |
| `community:search:keywords` | - | 搜索关键词记录 |

**缓存一致性**：写操作（发帖、点赞、收藏、删除、下架）都会调用 `evictPost(id)` 清除帖子详情缓存。但 **不清理** 热门帖子缓存——热门列表最多延迟 10 分钟更新。

**浏览计数不经过缓存**：`incrementViewCount(id)` 直接操作数据库 `SET view_count = view_count + 1`，然后 **不更新缓存中的 viewCount**。这意味着缓存命中时，返回的 viewCount 可能低于实际值。

### 六、AI 摘要深度分析

`CommunityAiSummaryServiceImpl.generateSummary`：

```text
1. community.ai.enabled == false → 抛"AI功能未启用"
2. 先读摘要缓存 community:post:summary:{postId}
3. 缓存命中 → 返回
4. 数据库查帖子 aiSummary 字段 → 非空 → 返回
5. 调用 aiCommunityService.generatePostSummary(title, content)
6. 成功 → 更新帖子表 aiSummary 字段
7. 清帖子详情缓存 evictPost
8. 写摘要缓存 (TTL=30天)
9. return 摘要
```

**双层缓存**：摘要缓存 + 帖子详情缓存，两个需要分别维护一致性。强制刷新时需要同时清理两处。

### 七、深度发现与坑点

#### 7.1 确认 BUG

| 编号 | BUG | 位置 | 说明 |
| --- | --- | --- | --- |
| BUG-1 | 点赞/收藏无唯一约束 | `likePost/collectPost` | 并发下可重复插入 |
| BUG-2 | 评论列表 N+1 | `convertToResponseWithReplies` | 每条一级评论=3次额外SELECT |
| BUG-3 | 标签关联 N+1 | `createPost` | 每个标签=2次SQL（验证+计数） |
| BUG-4 | authorName 非规范化 | `createPost` | 用户改名后帖子不更新 |
| BUG-5 | 浏览数不回写缓存 | `getPostDetail` | 缓存命中时 viewCount 偏低 |
| BUG-6 | 热度无时间衰减 | `convertToResponse` | 旧帖子累积浏览数持续霸榜 |
| BUG-7 | 删除帖子不清理标签关系 | `deletePost` | community_post_tag 残留 |

#### 7.2 设计风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | 敏感词检测异常后放行 | catch(Exception) 使用原始内容发布 |
| RISK-2 | replyToUserName fallback 硬编码 | "用户"+id 不走统一用户服务 |
| RISK-3 | 热门帖子定时任务无分布式锁 | 多实例部署时重复计算 |
| RISK-4 | 搜索关键词记录 catch 吞异常 | 关键词记录丢失不影响主流程，但排查困难 |

#### 7.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | 扁平化二级评论 | parentId 挂一级，replyToId 定位目标，前端渲染清晰 |
| H-2 | 标签5个上限静默截断 | 不报错但只取前5个，用户体验友好 |
| H-3 | 缓存优先读+写清一致性 | 读详情先缓存再数据库；写操作清缓存 |
| H-4 | 敏感词替换后发布 | 不一刀切禁止，而是替换敏感词后允许发布 |
| H-5 | 双层缓存摘要 | 摘要30天TTL+帖子详情30分钟TTL，按需刷新 |
| H-6 | 封禁过期自动解封 | 访问时检查 banExpireTime，过期自动更新 |
| H-7 | 通知失败不阻断 | 所有点赞/收藏/评论通知全部 catch 不阻断主流程 |
| H-8 | 热门帖子定时刷新 | 后台任务定期计算，避免实时查询性能问题 |
| H-9 | 分类帖子数 COUNT 重算 | `updatePostCount` 用 COUNT 重算避免增量漂移 |
| H-10 | 搜索关键词记录 | 热门搜索词缓存，辅助运营决策 |

#### 7.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| 发帖 | `CommunityPostServiceImpl.java` — createPost + 标签5个限制 |
| 帖子详情 | `CommunityPostServiceImpl.java` — getPostDetail + 缓存优先 |
| 点赞/取消 | `CommunityPostServiceImpl.java` — likePost/unlikePost + 通知 |
| 收藏/取消 | `CommunityPostServiceImpl.java` — collectPost/uncollectPost + 通知 |
| 删除帖子 | `CommunityPostServiceImpl.java` — deletePost + 减计数 |
| 置顶 | `CommunityPostServiceImpl.java` — topPost + DateHelper.addHoursFromNow |
| 下架 | `CommunityPostServiceImpl.java` — disablePost + 清缓存 |
| 热度公式 | `CommunityPostServiceImpl.java` — convertToResponse (3/5/8/0.1权重) |
| 一级评论 | `CommunityCommentServiceImpl.java` — createComment + 敏感词 |
| 回复评论 | `CommunityCommentServiceImpl.java` — replyComment + 扁平化 |
| 评论点赞 | `CommunityCommentServiceImpl.java` — likeComment/unlikeComment |
| 标签 CRUD | `CommunityTagServiceImpl.java` — 名称去重 + 缓存 |
| 缓存服务 | `CommunityCacheServiceImpl.java` — 五级缓存 + TTL |
| 热门任务 | `CommunityHotPostTask.java` — 72小时窗口 + 30分最低 |
| AI 摘要 | `CommunityAiSummaryServiceImpl.java` — 双层缓存 + 强制刷新 |
| 用户封禁 | `CommunityUserStatusServiceImpl.java` — ban/unban + 自动解封 |
| 帖子域 | `CommunityPost.java` — authorName 冗余 + 四计数 + isTop + aiSummary |
| 评论域 | `CommunityComment.java` — parentId + replyToId + replyToUserName |
