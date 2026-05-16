# 博客

博客模块用于长文创作、个人主页展示、分类标签管理和后台运营。

## 功能入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/blog`、`/blog/editor`、`/blog/editor/:id`、`/blog/:userId`、`/blog/:userId/article/:articleId` |
| 管理端 | `/blog/articles`、`/blog/categories`、`/blog/tags` |
| 后端模块 | `xiaou-blog` |

## 用户侧接口域

| 接口 | 能力 |
| --- | --- |
| `/user/blog/open` | 开通博客 |
| `/user/blog/check-status` | 检查博客状态 |
| `/user/blog/config/{userId}` | 博客配置 |
| `/user/blog/config/update` | 更新配置 |
| `/user/blog/article/create` | 创建文章 |
| `/user/blog/article/publish` | 发布文章 |
| `/user/blog/article/update/{id}` | 更新文章 |
| `/user/blog/article/{id}` | 文章详情或删除 |
| `/user/blog/article/list` | 公开文章列表 |
| `/user/blog/article/my-list` | 我的文章 |
| `/user/blog/article/draft-list` | 草稿列表 |
| `/user/blog/article/by-category` | 按分类查文章 |
| `/user/blog/categories` | 分类 |
| `/user/blog/tags`、`/tags/hot` | 标签和热门标签 |

## 管理侧接口域

| 接口 | 能力 |
| --- | --- |
| `/admin/blog/statistics` | 博客统计 |
| `/admin/blog/article/list` | 文章列表 |
| `/admin/blog/article/top`、`/cancel-top` | 置顶和取消置顶 |
| `/admin/blog/article/update-status` | 更新文章状态 |
| `/admin/blog/category/*` | 分类管理 |
| `/admin/blog/tag/*` | 标签管理和合并 |

## 文档深化点

- 草稿、发布、下架状态。
- Markdown 渲染和 DOMPurify 净化。
- 博客主页配置字段。
- 文章与学习资产转化关系。

