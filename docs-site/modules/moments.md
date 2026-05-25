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
