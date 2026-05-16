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

## 开通和发布成本

博客开通在 `BlogConfigServiceImpl.openBlog`：

| 动作 | 积分 | 说明 |
| --- | --- | --- |
| 开通博客 | 50 | 用户未开通时扣除，生成博客配置 |
| 首次发布文章 | 20 | 新文章或草稿首次发布时扣除 |

开通成功后会创建博客配置，默认公开，博客名使用用户昵称。重复开通会被拒绝。

## 文章状态

文章状态在 `BlogArticleServiceImpl` 中使用：

| code | 状态 | 进入方式 |
| --- | --- | --- |
| `0` | 草稿 | `/article/create` 创建 |
| `1` | 已发布 | `/article/publish` 发布 |
| `2` | 已下架 | 管理端更新状态 |
| `3` | 已删除 | 作者删除或管理端更新状态 |

公开列表只查询已发布文章。草稿和已删除文章只有作者本人可以查看，否则返回“文章不存在或已下架”。

## 发布校验

发布和更新都会执行：

- 用户必须已开通博客。
- 标题不能为空且不超过 200 字符。
- 正文不能为空且不少于 50 字符。
- 必须选择分类。
- 标签最多 5 个。
- 标题和正文都走敏感词服务。

如果摘要为空，系统取正文前 200 字符作为摘要。标签以 JSON 数组保存，返回时反序列化。

## 博客主页配置

博客配置包含博客名、描述、头像、封面、公告、个人标签、社交链接和公开状态。`personalTags` 和 `socialLinks` 以 JSON 保存，读取时转换为数组或 Map。

## 后台运营

管理端可以：

- 查看博客总数、文章总数和活跃博主数。
- 查询文章列表。
- 置顶文章并设置过期时间。
- 取消置顶。
- 更新文章状态。
- 删除文章。
- 管理分类。
- 合并或删除标签。

## 验证点

| 场景 | 预期 |
| --- | --- |
| 未开通博客创建文章 | 返回请先开通博客 |
| 积分不足开通或发布 | 返回当前积分和所需积分 |
| 文章正文少于 50 字 | 返回内容长度错误 |
| 非作者查看草稿或已删除文章 | 返回不存在或已下架 |
| 标签超过 5 个 | 返回标签数量错误 |
