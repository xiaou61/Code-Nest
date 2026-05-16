# 博客

博客模块用于长文创作、个人主页展示、分类标签管理和后台运营。

## 功能入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/blog`、`/blog/editor`、`/blog/editor/:id`、`/blog/:userId`、`/blog/:userId/article/:articleId` |
| 管理端 | `/blog/articles`、`/blog/categories`、`/blog/tags` |
| 后端模块 | `xiaou-blog` |

## 推荐学习顺序

博客模块适合用来学习“内容发布 + 积分门槛 + 分类标签运营”。建议按下面顺序读：

1. 先读 `BlogConfigServiceImpl.openBlog`，理解为什么博客需要先开通。
2. 再读 `BlogArticleServiceImpl.createArticle` 和 `publishArticle`，区分草稿和发布。
3. 接着看分类、标签服务，理解后台运营和内容检索的关系。
4. 最后读管理端置顶、下架、删除能力，理解运营如何干预内容生命周期。

## 源码地图

| 关注点 | 源码位置 |
| --- | --- |
| 用户端接口 | `xiaou-blog/src/main/java/com/xiaou/blog/controller/user/BlogUserController.java` |
| 管理端接口 | `xiaou-blog/src/main/java/com/xiaou/blog/controller/admin/BlogAdminController.java` |
| 博客开通和主页配置 | `xiaou-blog/src/main/java/com/xiaou/blog/service/impl/BlogConfigServiceImpl.java` |
| 文章创建、发布、详情、列表 | `xiaou-blog/src/main/java/com/xiaou/blog/service/impl/BlogArticleServiceImpl.java` |
| 分类管理 | `xiaou-blog/src/main/java/com/xiaou/blog/service/impl/BlogCategoryServiceImpl.java` |
| 标签管理 | `xiaou-blog/src/main/java/com/xiaou/blog/service/impl/BlogTagServiceImpl.java` |
| 核心 SQL | `sql/MySql/code_nest.sql` 的 `blog_config`、`blog_article`、`blog_category`、`blog_tag` |

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

开通流程可以理解为：

1. 查询 `blog_config`，如果用户已经开通过，直接拒绝。
2. 查询 `user_points_balance`，积分不足时返回当前积分和所需积分。
3. 扣除 50 积分并写 `user_points_detail`。
4. 读取用户昵称和头像，生成默认博客主页配置。
5. 写入 `blog_config`，返回博客 ID、用户 ID、剩余积分和创建时间。

当前积分流水类型复用 `ADMIN_GRANT`，但描述字段会写“开通博客”或“发布博客文章”。如果后续要做更细的积分报表，建议增加专门的博客消费类型。

## 文章状态

文章状态在 `BlogArticleServiceImpl` 中使用：

| code | 状态 | 进入方式 |
| --- | --- | --- |
| `0` | 草稿 | `/article/create` 创建 |
| `1` | 已发布 | `/article/publish` 发布 |
| `2` | 已下架 | 管理端更新状态 |
| `3` | 已删除 | 作者删除或管理端更新状态 |

公开列表只查询已发布文章。草稿和已删除文章只有作者本人可以查看，否则返回“文章不存在或已下架”。

## 从草稿到发布

创建文章和发布文章不是同一件事：

| 动作 | 状态变化 | 是否扣积分 | 典型入口 |
| --- | --- | --- | --- |
| 创建草稿 | 新文章进入 `0` 草稿 | 否 | 编辑器自动保存或手动保存 |
| 草稿首次发布 | `0` -> `1` | 是，扣 20 分 | 发布按钮 |
| 新文章直接发布 | 新文章进入 `1` | 是，扣 20 分 | 发布按钮 |
| 已发布文章再次编辑 | 仍为 `1` | 否 | 更新文章 |

发布成功后会更新三类计数：博客文章总数、分类文章数量、标签使用次数。文章摘要为空时，后端会取正文前 200 个字符作为摘要。

## 发布校验

发布和更新都会执行：

- 用户必须已开通博客。
- 标题不能为空且不超过 200 字符。
- 正文不能为空且不少于 50 字符。
- 必须选择分类。
- 标签最多 5 个。
- 标题和正文都走敏感词服务。

如果摘要为空，系统取正文前 200 字符作为摘要。标签以 JSON 数组保存，返回时反序列化。

## 分类和标签运营

分类是更稳定的栏目，标签是更灵活的索引：

| 能力 | 规则 |
| --- | --- |
| 创建分类 | 分类名不能重复，初始状态为 1 |
| 更新分类 | 改名时会重新检查重名 |
| 删除分类 | 如果 `articleCount > 0`，禁止删除 |
| 热门标签 | 按使用次数读取 |
| 删除标签 | 如果 `useCount > 0`，禁止删除 |
| 合并标签 | 当前会合并使用次数并删除源标签 |

需要注意：标签合并里有一个实现备注，当前还没有真正把 `blog_article.tags` 中的源标签替换为目标标签。运营执行合并前要先评估历史文章标签是否需要额外迁移。

## 博客主页配置

博客配置包含博客名、描述、头像、封面、公告、个人标签、社交链接和公开状态。`personalTags` 和 `socialLinks` 以 JSON 保存，读取时转换为数组或 Map。

博客配置是个人主页展示的核心。`isPublic = 1` 时可以公开访问，`status = 1` 表示博客配置正常。更新配置时，后端只更新请求中非空字段，因此前端可以做局部保存。

## 数据表理解

| 表 | 说明 |
| --- | --- |
| `blog_config` | 用户博客主页配置、开通成本、公开状态、文章总数 |
| `blog_article` | 文章标题、正文、摘要、分类、标签 JSON、状态和置顶信息 |
| `blog_category` | 分类名称、图标、描述、排序、文章数量 |
| `blog_tag` | 标签名称、描述、使用次数 |

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

置顶文章可以传 `duration`，单位是天；大于 0 时后端会计算 `topExpireTime`。取消置顶会把 `isTop` 改为 0，并清空置顶过期时间。

## 常见坑

| 问题 | 原因 | 建议 |
| --- | --- | --- |
| 未开通就创建文章 | `BlogConfig` 不存在 | 先调用 `/user/blog/check-status` |
| 草稿保存不扣积分 | 扣积分只发生在首次发布 | 前端保存按钮和发布按钮要区分文案 |
| 标签合并后历史文章没变 | 当前只合并标签使用次数 | 需要补充文章 JSON 标签迁移 |
| 分类删除失败 | 分类下还有文章 | 先迁移或下架相关文章 |
| 非作者访问草稿 | 服务会隐藏草稿和删除态 | 调试时用作者账号访问 |

## 验证点

| 场景 | 预期 |
| --- | --- |
| 未开通博客创建文章 | 返回请先开通博客 |
| 积分不足开通或发布 | 返回当前积分和所需积分 |
| 文章正文少于 50 字 | 返回内容长度错误 |
| 非作者查看草稿或已删除文章 | 返回不存在或已下架 |
| 标签超过 5 个 | 返回标签数量错误 |
| 已发布文章再次编辑 | 不再次扣发布积分 |
| 分类下存在文章时删除分类 | 返回无法删除 |
| 使用中的标签被删除 | 返回无法删除 |
| 置顶文章设置时长 | 写入置顶状态和过期时间 |
