# 动态广场

动态广场面向轻内容发布和互动，适合短状态、图片、评论、点赞、收藏和个人动态页。

## 功能入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/moments`、`/moments/user/:userId`、`/moments/my-favorites` |
| 管理端 | `/moments/list`、`/moments/comments`、`/moments/statistics` |
| 后端模块 | `xiaou-moment` |

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

## 验证点

| 场景 | 预期 |
| --- | --- |
| 5 分钟内发布第 4 条动态 | 返回发布过于频繁 |
| 非作者删除动态 | 返回无权限 |
| 评论者或动态作者删除评论 | 删除成功并减少评论数 |
| 动态列表未登录访问 | 点赞、收藏、删除权限均为 false |
| 热门任务失败 | 只记录错误，不影响主链路 |
