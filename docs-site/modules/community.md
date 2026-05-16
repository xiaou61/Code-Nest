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

