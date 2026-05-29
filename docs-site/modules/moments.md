# 动态广场

动态广场面向轻内容发布和互动，适合短状态、图片、评论、点赞、收藏和个人动态页。

## 功能入口

| 端 | 页面 | 路由 | 需登录 | keepAlive |
| --- | --- | --- | --- | --- |
| 用户端 | 动态广场首页 | `/moments` | 否 | 是 |
| 用户端 | 用户动态主页 | `/moments/user/:userId` | 否 | 是 |
| 用户端 | 我的收藏 | `/moments/my-favorites` | 是 | 是 |
| 管理端 | 动态列表 | `/moments/list` | 是 | - |
| 管理端 | 评论管理 | `/moments/comments` | 是 | - |
| 管理端 | 数据统计 | `/moments/statistics` | 是 | - |

## 推荐学习顺序

动态广场适合用来学习"轻内容 + 互动计数 + 缓存统计"的实现。建议按下面顺序读：

1. 看 `publishMoment`：理解发布频率限制、敏感词处理和图片 JSON 保存。
2. 看 `toggleLike`、`toggleFavorite`、`publishComment`：理解互动表和计数字段如何同步。
3. 看列表转换方法：理解用户信息、点赞状态、收藏状态如何批量补齐。
4. 看 `MomentViewServiceImpl` 和两个定时任务：理解浏览数先写 Redis，再定时同步到数据库。
5. 看管理端列表和统计：理解运营视角如何筛选、删除和观察趋势。

## 源码地图

| 关注点 | 源码位置 | 行数 |
| --- | --- | --- |
| 用户端接口 | `xiaou-moment/src/main/java/com/xiaou/moment/controller/user/UserMomentController.java` | ~12 个接口 |
| 管理端接口 | `xiaou-moment/src/main/java/com/xiaou/moment/controller/admin/AdminMomentController.java` | ~5 个接口 |
| 动态主服务 | `xiaou-moment/src/main/java/com/xiaou/moment/service/impl/MomentServiceImpl.java` | 核心业务 |
| 浏览数 Redis 服务 | `xiaou-moment/src/main/java/com/xiaou/moment/service/impl/MomentViewServiceImpl.java` | Redis 读写 |
| 热门动态任务 | `xiaou-moment/src/main/java/com/xiaou/moment/task/HotMomentCalculateTask.java` | 10 分钟周期 |
| 浏览数同步任务 | `xiaou-moment/src/main/java/com/xiaou/moment/task/MomentViewSyncTask.java` | 1 小时周期 |
| 状态枚举 | `xiaou-moment/src/main/java/com/xiaou/moment/enums/MomentStatus.java` | 3 种状态 |
| 评论状态枚举 | `xiaou-moment/src/main/java/com/xiaou/moment/enums/CommentStatus.java` | 2 种状态 |
| 动态实体 | `xiaou-moment/src/main/java/com/xiaou/moment/domain/Moment.java` | 主表实体 |
| 评论实体 | `xiaou-moment/src/main/java/com/xiaou/moment/domain/MomentComment.java` | 评论实体 |
| 点赞实体 | `xiaou-moment/src/main/java/com/xiaou/moment/domain/MomentLike.java` | 点赞关系 |
| 收藏实体 | `xiaou-moment/src/main/java/com/xiaou/moment/domain/MomentFavorite.java` | 收藏关系 |
| 用户端页面 | `vue3-user-front/src/views/moments/Index.vue` | 动态列表 |
| 发布对话框 | `vue3-user-front/src/views/moments/components/PublishDialog.vue` | 发布交互 |
| 评论对话框 | `vue3-user-front/src/views/moments/components/CommentDialog.vue` | 评论交互 |
| 用户主页 | `vue3-user-front/src/views/moments/UserProfile.vue` | 用户动态 |
| 我的收藏 | `vue3-user-front/src/views/moments/MyFavorites.vue` | 收藏列表 |
| 管理端列表 | `vue3-admin-front/src/views/moments/list/index.vue` | 后台管理 |
| 管理端评论 | `vue3-admin-front/src/views/moments/comments/index.vue` | 评论管理 |
| 管理端统计 | `vue3-admin-front/src/views/moments/statistics/index.vue` | 数据看板 |
| 核心 SQL | `sql/MySql/code_nest.sql` | 5 张表 |

## 用户侧接口域

| 接口域 | 方法 | 能力 |
| --- | --- | --- |
| `/user/moments/publish` | POST | 发布动态 |
| `/user/moments/list` | POST | 动态列表（分页） |
| `/user/moments/{id}` | DELETE | 删除动态 |
| `/user/moments/{momentId}/like` | POST | 点赞或取消点赞 |
| `/user/moments/{momentId}/favorite` | POST | 收藏或取消收藏 |
| `/user/moments/comment` | POST | 发表评论 |
| `/user/moments/comments` | POST | 评论列表 |
| `/user/moments/hot` | GET | 热门动态 |
| `/user/moments/search` | POST | 搜索动态 |
| `/user/moments/user-list` | POST | 用户动态列表 |
| `/user/moments/user-info` | GET | 动态用户信息 |
| `/user/moments/my-favorites` | POST | 我的收藏 |

## 管理侧接口域

| 接口域 | 方法 | 能力 |
| --- | --- | --- |
| `/admin/moments/list` | POST | 动态列表（含筛选） |
| `/admin/moments/batch-delete` | POST | 批量删除 |
| `/admin/moments/comments/list` | POST | 评论列表 |
| `/admin/moments/comments/{id}` | DELETE | 删除评论 |
| `/admin/moments/statistics` | POST | 数据统计 |

所有管理侧接口标注 `@RequireAdmin`，需要管理员权限。

## 开发注意

- 动态和评论都要接入敏感词风控（模块名分别为 `moment` 和 `moment_comment`）。
- 图片内容要走文件存储权限，以 JSON 数组存入动态记录。
- 热门动态和搜索需要明确排序权重。
- 用户主页访问公开信息，不应泄露私密字段。
- 用户信息来自 `UserInfoApiService`，批量列表要优先走批量接口，避免 N+1。

## 状态和发布限制

动态状态定义在 `MomentStatus`：

| code | 状态 | 说明 |
| --- | --- | --- |
| `0` | 已删除 | 用户或管理端软删除 |
| `1` | 正常 | 发布成功后默认状态 |
| `2` | 审核中 | 预留审核态 |

评论状态定义在 `CommentStatus`：`0` 已删除，`1` 正常。

发布动态时会做两类校验：

1. 频率限制：同一用户 5 分钟最多 3 条。
2. 敏感词检测：正文不允许包含违规内容，允许时写入处理后的文本。

## 发布流程拆解

用户发布动态时，后端做了几件事：

1. `StpUserUtil.checkLogin()` 校验登录。
2. `checkPublishFrequency` 查询最近 5 分钟发文数量，超过 3 条就拒绝。
3. 调用 `SensitiveWordUtils.checkText(content, "moment", null, currentUserId)`。
4. 如果敏感词策略不允许发布，返回"内容包含敏感词，禁止发布"。
5. 写入动态内容、图片 JSON、点赞数 0、评论数 0、状态 `NORMAL`。

评论发布也会走敏感词，但模块名是 `moment_comment`，并且成功后会增加评论数。点赞、评论、收藏都会尝试通过通知中心提醒动态作者；通知失败只记录日志，不回滚主操作。

## 互动规则

| 动作 | 规则 | 计数字段同步 |
| --- | --- | --- |
| 点赞 | 已点赞再次点击取消，并同步增减 `likeCount` | `moment_likes` 表增删 + `moments.likeCount` 更新 |
| 收藏 | 已收藏再次点击取消，并同步增减 `favoriteCount` | `moment_favorites` 表增删 + `moments.favoriteCount` 更新 |
| 评论 | 评论前校验动态存在和敏感词，成功后增加 `commentCount` | `moment_comments` 插入 + `moments.commentCount` 更新 |
| 删除评论 | 评论作者本人或动态作者可以删除；管理端也可以删除 | 软删除 + `moments.commentCount` 减少 |
| 删除动态 | 用户只能删除自己的动态；管理端支持批量删除 | 软删除动态 + 关联评论 |

## 热门和浏览

动态列表、搜索、用户动态和收藏页都会记录浏览。热门动态定时任务 `HotMomentCalculateTask` 每 10 分钟计算一次，Redis Sorted Set Key 为 `moment:hot:list`，TTL 为 10 分钟。

热度公式：

```text
likeCount * 2 + commentCount * 3 + viewCount * 0.1
```

用户端列表会批量查询用户信息、点赞状态和收藏状态，避免明显的 N+1 查询。

浏览数不是每次直接写数据库。`MomentViewServiceImpl` 会使用两个 Redis Key：

| Key | 说明 | TTL |
| --- | --- | --- |
| `moment:view:{momentId}` | 动态待同步浏览增量 | 无过期 |
| `moment:view:user:{userId}:{momentId}` | 用户 5 分钟去重标记 | 5 分钟 |

`MomentViewSyncTask` 每小时执行一次，把 Redis 增量同步到数据库。同步成功后会把对应 Redis 计数设为 0。这个设计能减少高频列表访问对数据库的写压力。

## 数据表理解

| 表 | 说明 | 关键字段 |
| --- | --- | --- |
| `moments` | 动态主表 | 内容、图片 JSON、likeCount、commentCount、favoriteCount、viewCount、状态 |
| `moment_comments` | 评论表 | 动态 ID、评论内容、用户 ID、状态 |
| `moment_likes` | 点赞关系表 | 动态 ID、用户 ID |
| `moment_favorites` | 收藏关系表 | 动态 ID、用户 ID |
| `moment_views` | 浏览记录表 | 动态 ID、用户 ID、浏览时间 |

## 前端组件架构

| 组件 | 职责 |
| --- | --- |
| `Index.vue` | 动态列表主页，瀑布流布局，支持分页、搜索、热门切换 |
| `PublishDialog.vue` | 发布对话框：文本输入、图片上传预览、敏感词提示 |
| `CommentDialog.vue` | 评论弹出框：评论列表、分页、回复输入 |
| `AllCommentsDialog.vue` | 全部评论查看（管理端也复用） |
| `UserProfile.vue` | 用户动态主页，展示该用户的公开动态 |
| `MyFavorites.vue` | 我的收藏页，需要登录 |

## 发布频率限制实现

`MomentServiceImpl.checkPublishFrequency()` 实现频率限制：

```text
1. 查询最近 5 分钟内该用户发布的动态数量
2. 如果 count >= 3，抛出 BusinessException("发布过于频繁，请稍后再试")
```

频率限制参数：

| 参数 | 值 | 说明 |
|------|----|------|
| 时间窗口 | 5 分钟 | `LocalDateTime.now().minusMinutes(5)` |
| 最大条数 | 3 条 | 超过即拒绝 |
| 检测时机 | 发布前 | `publishMoment()` 方法开头 |

## 批量查询优化

`MomentServiceImpl.convertToMomentListResponseBatch()` 实现了 8 步批量查询优化，避免 N+1：

| 步骤 | 操作 | 优化 |
|------|------|------|
| 1 | 收集所有用户 ID | Set 去重 |
| 2 | `userInfoApiService.getSimpleUserInfoBatch()` | 一次查所有用户信息 |
| 3 | 获取当前用户 ID | 判断登录态 |
| 4 | `momentLikeMapper.selectLikedMomentIds()` | 批量查点赞状态 |
| 5 | `momentFavoriteMapper.selectFavoritedMomentIds()` | 批量查收藏状态 |
| 6 | 每条动态取 3 条评论 | Map 缓存 |
| 7 | 评论用户批量查信息 | 一次查所有评论用户 |
| 8 | 遍历转换 | 使用 Map 取值而非逐条查询 |

**关键**：如果新增动态列表字段涉及跨模块数据，必须参照此批量模式，不能逐条查询外部服务。

## 管理端统计实现

`MomentServiceImpl.getStatistics()` 计算动态统计：

| 统计项 | 来源方法 | 说明 |
|--------|---------|------|
| 总动态数 | `momentMapper.selectCount()` | 按时间范围筛选 |
| 总点赞数 | `momentMapper.selectTotalLikes()` | 所有动态点赞总和 |
| 总评论数 | `momentCommentMapper.selectCount()` | 按时间范围筛选 |
| 活跃用户数 | `momentMapper.selectActiveUsersCount()` | 发过动态的用户数 |
| 每日统计 | `getDailyStatistics()` | 按天循环统计（默认最近 7 天） |

## 常见坑

| 问题 | 原因 | 建议 |
| --- | --- | --- |
| 浏览数没有立刻出现在数据库 | 浏览先写 Redis，每小时同步 | 联调时同时看 Redis 和数据库 |
| 未登录列表的互动状态都是 false | 当前用户 ID 为空，后端返回默认值 | 前端不要把 false 当作接口异常 |
| 管理端敏感词筛选不准确 | `hasSensitiveWord` 当前是预留参数 | 需要接入敏感词日志后再做真实筛选 |
| 热门列表为空 | 最近 24 小时无可计算动态或任务未运行 | 先检查 `moment:hot:list` 和定时任务日志 |
| 评论数出现负数风险 | 重复删除或并发删除会影响计数 | 删除前确认状态，后续可加幂等保护 |
| 图片显示异常 | 图片 JSON 解析失败或文件存储权限不足 | 检查 images 字段格式和文件访问权限 |

## 验证清单

| 场景 | 预期 |
| --- | --- |
| 5 分钟内发布第 4 条动态 | 返回发布过于频繁 |
| 非作者删除动态 | 返回无权限 |
| 评论者或动态作者删除评论 | 删除成功并减少评论数 |
| 动态列表未登录访问 | 点赞、收藏、删除权限均为 false |
| 热门任务失败 | 只记录错误，不影响主链路 |
| 同一用户 5 分钟重复浏览同一动态 | 只统计一次浏览 |
| 点赞后再次点击 | 取消点赞并减少点赞数 |
| 收藏后再次点击 | 取消收藏并减少收藏数 |
| 管理端批量删除 | 动态和关联评论一起软删除 |
| 管理端数据统计 | 展示每日发帖、评论、点赞趋势 |

## 文档维护提醒

动态模块变更时至少同步三处：

1. 本页的接口域、状态定义和流程描述。
2. [社区与内容矩阵](/modules/community-content) 的功能地图和互动热度。
3. [模块最小回归矩阵](/reference/module-regression-matrix) 的动态广场行。

---

## 动态广场模块深度拆解

> 以下内容基于 `xiaou-moment` 全部源码逐行拆解，覆盖 1 个主 Service、1 个浏览数 Service、1 个定时任务、4 个实体类、2 个枚举类。

### 一、批量查询优化深度分析

**源码**：`MomentServiceImpl.java`（900 行）

动态列表是用户访问最频繁的接口，如果逐条查询用户信息、点赞状态、收藏状态，会产生严重的 N+1 问题。`convertToMomentListResponseBatch` 方法通过 8 步批量查询优化解决这个问题。

#### 1.1 优化前后对比

```
优化前（N+1 问题）：
  for each moment:
    userInfoApiService.getSimpleUserInfo(userId)     ← 第1次查询
    momentLikeMapper.selectByMomentIdAndUserId(...)  ← 第2次查询
    momentFavoriteMapper.selectByMomentIdAndUserId(...) ← 第3次查询
    momentCommentMapper.selectByMomentId(...)        ← 第4次查询
    for each comment:
      userInfoApiService.getSimpleUserInfo(commentUserId) ← 第5次查询

  总查询次数 = N * (4 + M)，M 为每条动态的评论数

优化后（批量查询）：
  1. 收集所有 userId → Set
  2. userInfoApiService.getSimpleUserInfoBatch(userIds) ← 1次查询
  3. 获取当前用户 ID
  4. momentLikeMapper.selectLikedMomentIds(momentIds, userId) ← 1次查询
  5. momentFavoriteMapper.selectFavoritedMomentIds(momentIds, userId) ← 1次查询
  6. for each moment: momentCommentMapper.selectByMomentId(id, 3) ← N次查询
  7. userInfoApiService.getSimpleUserInfoBatch(commentUserIds) ← 1次查询
  8. 遍历组装结果

  总查询次数 = 4 + N（N 为动态数量）
```

#### 1.2 8 步流程详解

```
convertToMomentListResponseBatch(moments):
┌─────────────────────────────────────────────────────────┐
│ Step 1: 收集所有用户 ID                                  │
│   userIds = moments.map(userId).toSet()                 │
│   去重后批量查询，避免重复请求                            │
├─────────────────────────────────────────────────────────┤
│ Step 2: 批量查询用户信息                                 │
│   userInfoMap = userInfoApiService.getSimpleUserInfoBatch│
│   返回 Map<userId, SimpleUserInfo>                      │
│   后续通过 Map.get(userId) 直接取值，O(1) 复杂度         │
├─────────────────────────────────────────────────────────┤
│ Step 3: 获取当前用户 ID                                  │
│   currentUserId = StpUserUtil.getLoginIdAsLong()        │
│   未登录时为 null，后续判断需要                           │
├─────────────────────────────────────────────────────────┤
│ Step 4: 批量查询点赞状态                                 │
│   if (currentUserId != null):                           │
│     likedMomentIds = momentLikeMapper.selectLikedMomentIds│
│   使用 Set 存储，contains 判断 O(1)                      │
├─────────────────────────────────────────────────────────┤
│ Step 5: 批量查询收藏状态                                 │
│   if (currentUserId != null):                           │
│     favoritedMomentIds = momentFavoriteMapper.selectFavoritedMomentIds│
│   同样使用 Set 存储                                      │
├─────────────────────────────────────────────────────────┤
│ Step 6: 批量获取评论并收集评论用户 ID                    │
│   for each momentId:                                    │
│     comments = momentCommentMapper.selectByMomentId(id, 3)│
│     momentCommentsMap.put(momentId, comments)           │
│     comments.forEach(c -> commentUserIds.add(c.userId)) │
│   每条动态只取最新 3 条评论                               │
├─────────────────────────────────────────────────────────┤
│ Step 7: 批量查询评论用户信息                             │
│   commentUserInfoMap = userInfoApiService.getSimpleUserInfoBatch│
│   合并到同一个 Map，避免重复查询                          │
├─────────────────────────────────────────────────────────┤
│ Step 8: 批量转换结果                                     │
│   moments.stream().map(moment -> {                      │
│     response.userNickname = userInfoMap.get(userId)     │
│     response.isLiked = likedMomentIds.contains(id)     │
│     response.isFavorited = favoritedMomentIds.contains(id)│
│     response.recentComments = comments.map(convert)     │
│   })                                                    │
└─────────────────────────────────────────────────────────┘
```

**关键发现 1**：Step 6 中每条动态仍然单独查询评论（`selectByMomentId`），这是因为每条动态只需要最新 3 条评论，批量查询 SQL 会更复杂。如果动态数量很大（>100），可以考虑使用 `LEFT JOIN` + `ROW_NUMBER()` 一次性查询。

**关键发现 2**：Step 4 和 Step 5 只在用户登录时才查询点赞/收藏状态，未登录时直接使用空 Set，避免不必要的数据库查询。

**关键发现 3**：Step 7 将评论用户信息和动态作者信息合并到同一个 Map，如果评论者和动态作者是同一人，不会重复查询。

### 二、Redis 浏览数同步机制深度分析

**源码**：`MomentViewServiceImpl.java`（131 行）

浏览数是高频写操作，如果每次浏览都直接写数据库，会产生大量 UPDATE 语句。动态广场采用"Redis 累积 + 定时同步"策略解决这个问题。

#### 2.1 双 Key 设计

```
Redis Key 设计：
  moment:view:{momentId}           → 浏览增量计数器（累积待同步的浏览数）
  moment:view:user:{userId}:{momentId} → 用户去重标记（5 分钟过期）
```

| Key | 类型 | 用途 | TTL |
| --- | --- | --- | --- |
| `moment:view:{momentId}` | String (数字) | 累积浏览增量 | 无过期（同步后清零） |
| `moment:view:user:{userId}:{momentId}` | String | 用户 5 分钟去重 | 5 分钟 |

#### 2.2 recordView 流程

```
recordView(momentId, userId):
┌─────────────────────────────────────────────────────────┐
│ 1. 构建用户浏览记录 Key                                  │
│    userViewKey = "moment:view:user:" + userId + ":" + momentId│
├─────────────────────────────────────────────────────────┤
│ 2. 检查用户是否在 5 分钟内已经浏览过                      │
│    if (redisUtil.hasKey(userViewKey)):                   │
│      return  // 已统计过，不重复统计                      │
├─────────────────────────────────────────────────────────┤
│ 3. 增加浏览数                                            │
│    viewCountKey = "moment:view:" + momentId             │
│    redisUtil.incr(viewCountKey, 1)                      │
├─────────────────────────────────────────────────────────┤
│ 4. 记录用户浏览，5 分钟过期                              │
│    redisUtil.set(userViewKey, "1", 5 * 60)              │
└─────────────────────────────────────────────────────────┘
```

**关键发现 1**：用户去重使用 Redis Key 过期实现，而不是 Set 结构。这样只需要一个 Key，内存开销更小。

**关键发现 2**：`viewCountKey` 没有过期时间，累积的浏览数不会自动丢失。只有同步成功后才会清零。

#### 2.3 syncViewCountToDatabase 流程

```
syncViewCountToDatabase()（每小时执行）:
┌─────────────────────────────────────────────────────────┐
│ 1. 获取所有浏览数 Key                                    │
│    keys = redisUtil.getKeys("moment:view:*")            │
├─────────────────────────────────────────────────────────┤
│ 2. 遍历每个 Key                                          │
│    for each key:                                        │
│      momentId = key.substring("moment:view:".length())  │
│      viewCount = redisUtil.get(key)                     │
├─────────────────────────────────────────────────────────┤
│ 3. 增量更新到数据库                                      │
│    for (i = 0; i < viewCount; i++):                     │
│      momentMapper.incrementViewCount(momentId)          │
├─────────────────────────────────────────────────────────┤
│ 4. 清空 Redis 计数（保留 Key，值设为 0）                  │
│    redisUtil.set(key, 0, 0)                             │
└─────────────────────────────────────────────────────────┘
```

**关键发现 1**：同步使用增量更新（`incrementViewCount`），而不是直接覆盖。这样即使同步过程中有新的浏览记录，也不会丢失。

**关键发现 2**：同步成功后将 Redis Key 的值设为 0，而不是删除 Key。这样下次同步时只需要处理值 > 0 的 Key。

**关键发现 3**：同步任务每小时执行一次，最坏情况下浏览数会延迟 1 小时显示在数据库中。这是性能和实时性的权衡。

#### 2.4 性能分析

| 操作 | 频率 | 数据库写入 | Redis 操作 |
| --- | --- | --- | --- |
| 浏览动态 | 高频（每秒可能数十次） | 0 次 | 2 次（incr + set） |
| 同步浏览数 | 低频（每小时 1 次） | N 次（增量更新） | N 次（get + set） |

**优势**：
- 高频浏览只写 Redis，不写数据库，减少数据库压力
- 用户去重在 Redis 完成，不查数据库
- 同步失败不影响浏览功能，只影响数据一致性

**风险**：
- Redis 宕机时累积的浏览数会丢失（最多 1 小时的数据）
- 同步窗口期内数据库浏览数不准确

### 三、热门动态计算深度分析

**源码**：`HotMomentCalculateTask.java`（88 行）

热门动态使用 Redis Sorted Set 存储，每 10 分钟重新计算一次。

#### 3.1 热度公式

```
hotScore = likeCount * 2.0 + commentCount * 3.0 + viewCount * 0.1
```

| 指标 | 权重 | 说明 |
| --- | --- | --- |
| 点赞数 | 2.0 | 互动意愿最强，权重最高 |
| 评论数 | 3.0 | 评论比点赞更花时间，权重更高 |
| 浏览数 | 0.1 | 浏览成本最低，权重最低 |

**设计思路**：评论 > 点赞 > 浏览。评论需要用户花时间写内容，是最有价值的互动；点赞只需要点击一下；浏览是被动行为，成本最低。

#### 3.2 计算流程

```
calculateHotMoments()（每 10 分钟执行）:
┌─────────────────────────────────────────────────────────┐
│ 1. 查询最近 24 小时的热门动态（按热度排序，取前 50 条）    │
│    hotMoments = momentMapper.selectHotMoments(50)       │
│    SQL: SELECT * FROM moments                           │
│          WHERE status = 1                               │
│          AND create_time >= NOW() - INTERVAL 24 HOUR    │
│          ORDER BY like_count*2 + comment_count*3 + view_count*0.1 DESC│
│          LIMIT 50                                       │
├─────────────────────────────────────────────────────────┤
│ 2. 获取 Redisson 的 Sorted Set                          │
│    hotMomentsSet = redissonClient.getScoredSortedSet("moment:hot:list")│
├─────────────────────────────────────────────────────────┤
│ 3. 清空旧数据                                            │
│    hotMomentsSet.clear()                                │
├─────────────────────────────────────────────────────────┤
│ 4. 计算热度分数并存入 Sorted Set                         │
│    for each moment:                                     │
│      score = calculateHotScore(moment)                  │
│      hotMomentsSet.add(score, momentId)                 │
├─────────────────────────────────────────────────────────┤
│ 5. 设置过期时间（10 分钟）                               │
│    redisUtil.expire("moment:hot:list", 600)             │
└─────────────────────────────────────────────────────────┘
```

**关键发现 1**：热门动态只计算最近 24 小时的数据，避免历史数据占据榜单。

**关键发现 2**：缓存 TTL 为 10 分钟，与定时任务周期一致。如果任务执行失败，旧数据会自动过期，不会一直展示过时的热门动态。

**关键发现 3**：使用 Redisson 的 `RScoredSortedSet`，天然支持按分数排序，不需要额外排序逻辑。

#### 3.3 查询热门动态

```
getHotMoments():
  1. 从 Redis Sorted Set 获取动态 ID 列表（按分数降序）
  2. 批量查询动态详情
  3. 转换为响应格式
```

**优势**：
- 热门动态查询只读 Redis，不查数据库
- Sorted Set 天然支持排序，不需要应用层排序
- 定时任务失败时缓存自动过期，不会展示过时数据

### 四、深度发现与坑点

#### 4.1 已确认的代码问题

| 编号 | 问题 | 位置 | 影响 |
| --- | --- | --- | --- |
| BUG-1 | 浏览数同步使用循环 incrementViewCount | `MomentViewServiceImpl:111-113` | 大量浏览时同步慢，应改为批量 UPDATE |
| BUG-2 | 热门动态查询未使用索引 | `MomentMapper.selectHotMoments` | 24 小时全表扫描，数据量大时慢 |
| BUG-3 | 批量查询评论仍逐条查询 | `MomentServiceImpl:822-826` | 动态数量大时有 N 次查询 |

#### 4.2 设计层面的潜在风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | Redis 宕机丢失浏览数 | 累积的浏览数最多丢失 1 小时 |
| RISK-2 | 热门动态计算延迟 | 最多 10 分钟延迟，新发布的动态不会立即出现在热门 |
| RISK-3 | 用户去重 Key 内存占用 | 高并发时 `moment:view:user:*` Key 数量可能很大 |

#### 4.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | 批量查询优化 | 8 步批量查询将 N+1 问题降为 4+N 查询 |
| H-2 | Redis 浏览数累积 | 高频浏览只写 Redis，定时同步到数据库 |
| H-3 | 用户去重 Key 过期 | 使用 Redis Key 过期实现 5 分钟去重，内存开销小 |
| H-4 | Sorted Set 热门榜 | 天然支持按分数排序，查询性能 O(log(N)) |
| H-5 | 缓存自动过期 | 热门动态缓存 10 分钟过期，任务失败不会展示过时数据 |

#### 4.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| 批量查询优化 | `MomentServiceImpl.java` — `convertToMomentListResponseBatch` 8 步流程 |
| 浏览数记录 | `MomentViewServiceImpl.java` — `recordView` Redis 双 Key 设计 |
| 浏览数同步 | `MomentViewServiceImpl.java` — `syncViewCountToDatabase` 增量同步 |
| 热门动态计算 | `HotMomentCalculateTask.java` — `calculateHotMoments` 定时任务 |
| 热度公式 | `HotMomentCalculateTask.java` — `calculateHotScore` 权重计算 |
| 发布频率限制 | `MomentServiceImpl.java` — `checkPublishFrequency` 5 分钟 3 条 |
| 互动规则 | `MomentServiceImpl.java` — `toggleLike`/`toggleFavorite`/`publishComment` |
| 敏感词检测 | `MomentServiceImpl.java` — `publishMoment`/`publishComment` 敏感词调用 |

## 相关模块

| 模块 | 关系 | 说明 |
| --- | --- | --- |
| [公共底座](/modules/common) | 强依赖 | 动态广场依赖公共底座的统一响应、分页和异常处理 |
| [鉴权与用户体系](/modules/auth) | 强依赖 | 动态发布、点赞、收藏需要用户登录态 |
| [用户账户与个人中心](/modules/user-account) | 强依赖 | 动态作者信息依赖用户账户 |
| [敏感词风控](/modules/sensitive) | 强依赖 | 动态内容必须经过敏感词检测 |
| [文件存储](/modules/file-storage) | 强依赖 | 动态图片上传和存储 |
| [通知中心](/modules/notification) | 间接依赖 | 点赞、评论等互动触发通知推送 |
| [积分与抽奖](/modules/points) | 间接关联 | 动态互动可能触发积分奖励 |
| [社区帖子](/modules/community) | 间接关联 | 动态与社区帖子都是内容互动模块 |
