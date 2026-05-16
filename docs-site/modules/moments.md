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

