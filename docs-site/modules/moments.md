# 动态广场

动态广场面向轻内容发布和互动，适合短状态、图片、评论、点赞、收藏和个人动态页。

## 功能入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/moments`、`/moments/user/:userId`、`/moments/my-favorites` |
| 管理端 | `/moments/list`、`/moments/comments`、`/moments/statistics` |
| 后端模块 | `xiaou-moment` |

## 推荐学习顺序

动态广场适合用来学习“轻内容 + 互动计数 + 缓存统计”的实现。建议按下面顺序读：

1. 看 `publishMoment`：理解发布频率限制、敏感词处理和图片 JSON 保存。
2. 看 `toggleLike`、`toggleFavorite`、`publishComment`：理解互动表和计数字段如何同步。
3. 看列表转换方法：理解用户信息、点赞状态、收藏状态如何批量补齐。
4. 看 `MomentViewServiceImpl` 和两个定时任务：理解浏览数先写 Redis，再定时同步到数据库。
5. 看管理端列表和统计：理解运营视角如何筛选、删除和观察趋势。

## 源码地图

| 关注点 | 源码位置 |
| --- | --- |
| 用户端接口 | `xiaou-moment/src/main/java/com/xiaou/moment/controller/user/UserMomentController.java` |
| 管理端接口 | `xiaou-moment/src/main/java/com/xiaou/moment/controller/admin/AdminMomentController.java` |
| 动态主服务 | `xiaou-moment/src/main/java/com/xiaou/moment/service/impl/MomentServiceImpl.java` |
| 浏览数 Redis 服务 | `xiaou-moment/src/main/java/com/xiaou/moment/service/impl/MomentViewServiceImpl.java` |
| 热门动态任务 | `xiaou-moment/src/main/java/com/xiaou/moment/task/HotMomentCalculateTask.java` |
| 浏览数同步任务 | `xiaou-moment/src/main/java/com/xiaou/moment/task/MomentViewSyncTask.java` |
| 状态枚举 | `xiaou-moment/src/main/java/com/xiaou/moment/enums` |
| 核心 SQL | `sql/MySql/code_nest.sql` 的 `moments`、`moment_comments`、`moment_likes`、`moment_favorites`、`moment_views` |

## 用户侧接口域

| 接口域 | 能力 |
| --- | --- |
| `/user/moments/publish` | 发布动态 |
| `/user/moments/list` | 动态列表 |
| `/user/moments/{id}` | 删除动态 |
| `/user/moments/{momentId}/like` | 点赞或取消点赞 |
| `/user/moments/comment` | 发表评论 |
| `/user/moments/comments` | 评论列表 |
| `/user/moments/hot` | 热门动态 |
| `/user/moments/search` | 搜索动态 |
| `/user/moments/user-list` | 用户动态列表 |
| `/user/moments/user-info` | 动态用户信息 |
| `/user/moments/{momentId}/favorite` | 收藏或取消收藏 |
| `/user/moments/my-favorites` | 我的收藏 |

## 管理侧接口域

| 接口域 | 能力 |
| --- | --- |
| `/admin/moments/list` | 动态列表 |
| `/admin/moments/batch-delete` | 批量删除 |
| `/admin/moments/comments/list` | 评论列表 |
| `/admin/moments/comments/{id}` | 删除评论 |
| `/admin/moments/statistics` | 数据统计 |

## 开发注意

- 动态和评论都要接入敏感词风控。
- 图片内容要走文件存储权限。
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

图片以 JSON 数组保存在动态记录中，列表响应会反序列化为数组。

## 发布流程拆解

用户发布动态时，后端做了几件事：

1. `StpUserUtil.checkLogin()` 校验登录。
2. `checkPublishFrequency` 查询最近 5 分钟发文数量，超过 3 条就拒绝。
3. 调用 `SensitiveWordUtils.checkText(content, "moment", null, currentUserId)`。
4. 如果敏感词策略不允许发布，返回“内容包含敏感词，禁止发布”。
5. 写入动态内容、图片 JSON、点赞数 0、评论数 0、状态 `NORMAL`。

评论发布也会走敏感词，但模块名是 `moment_comment`，并且成功后会增加评论数。点赞、评论、收藏都会尝试通过通知中心提醒动态作者；通知失败只记录日志，不回滚主操作。

## 互动规则

| 动作 | 规则 |
| --- | --- |
| 点赞 | 已点赞再次点击会取消点赞，并同步增减 `likeCount` |
| 收藏 | 已收藏再次点击会取消收藏，并同步增减 `favoriteCount` |
| 评论 | 评论前校验动态存在和敏感词，成功后增加评论数 |
| 删除评论 | 评论作者本人或动态作者可以删除；管理端也可以删除 |
| 删除动态 | 用户只能删除自己的动态；管理端支持批量删除 |

点赞、评论和收藏会尝试通知动态作者，通知失败只写日志。

## 热门和浏览

动态列表、搜索、用户动态和收藏页都会记录浏览。热门动态定时任务 `HotMomentCalculateTask` 每 10 分钟计算一次，Redis Sorted Set Key 为 `moment:hot:list`，TTL 为 10 分钟。

热度公式：

```text
likeCount * 2 + commentCount * 3 + viewCount * 0.1
```

用户端列表会批量查询用户信息、点赞状态和收藏状态，避免明显的 N+1 查询。

浏览数不是每次直接写数据库。`MomentViewServiceImpl` 会使用两个 Redis Key：

| Key | 说明 |
| --- | --- |
| `moment:view:{momentId}` | 动态待同步浏览增量 |
| `moment:view:user:{userId}:{momentId}` | 用户 5 分钟去重标记 |

`MomentViewSyncTask` 每小时执行一次，把 Redis 增量同步到数据库。同步成功后会把对应 Redis 计数设为 0。这个设计能减少高频列表访问对数据库的写压力。

## 数据表理解

| 表 | 说明 |
| --- | --- |
| `moments` | 动态主表，保存内容、图片 JSON、互动计数和状态 |
| `moment_comments` | 评论表，当前是动态下的评论列表 |
| `moment_likes` | 点赞关系表 |
| `moment_favorites` | 收藏关系表 |
| `moment_views` | 浏览记录表，当前主要配合浏览统计能力 |

## 常见坑

| 问题 | 原因 | 建议 |
| --- | --- | --- |
| 浏览数没有立刻出现在数据库 | 浏览先写 Redis，每小时同步 | 联调时同时看 Redis 和数据库 |
| 未登录列表的互动状态都是 false | 当前用户 ID 为空 | 前端不要把 false 当作接口异常 |
| 管理端敏感词筛选不准确 | `hasSensitiveWord` 当前是预留参数 | 需要接入敏感词日志后再做真实筛选 |
| 热门列表为空 | 最近 24 小时无可计算动态或任务未运行 | 先检查 `moment:hot:list` 和定时任务日志 |
| 评论数出现负数风险 | 重复删除或并发删除会影响计数 | 删除前确认状态，后续可加幂等保护 |

## 验证点

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
