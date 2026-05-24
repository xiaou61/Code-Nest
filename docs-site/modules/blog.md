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

需要注意：标签合并**已经实现了文章标签替换**。`mergeTags` 会遍历所有使用源标签的文章，将 JSON 数组中的源标签替换为目标标签（同时去重），然后更新目标标签的 `useCount`，最后物理删除源标签。

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
| 标签合并后文章标签已替换 | `mergeTags` 会遍历并替换所有文章中的源标签 | 合并是不可逆操作，执行前请确认目标标签正确 |

---

## 博客系统深度拆解

以下内容来自对 xiaou-blog 模块全部源码的逐行阅读，覆盖 4 个 ServiceImpl、2 个 Controller、5 个 Domain、2 个 Enum、2 个 Mapper XML。

### 一、文章生命周期状态机

#### 1.1 文章状态流转

```
                    ┌──────────────┐
                    │   DRAFT(0)   │ ← 初始创建
                    │   草稿        │
                    └──────┬───────┘
                           │ publish()
                    ┌──────▼───────┐
              ┌─────│ PUBLISHED(1) │─────┐
              │     │   已发布      │     │
              │     └──────────────┘     │
         unpublish()                toRecycle()
              │                           │
     ┌────────▼────────┐        ┌────────▼────────┐
     │   DRAFT(0)       │        │  RECYCLE(2)      │
     │   草稿(回到)     │        │   回收站          │
     └─────────────────┘        └───────┬──────────┘
                                         │
                                ┌────────┴────────┐
                           restore()         delete()
                                │                 │
                     ┌──────────▼──┐     ┌────────▼────────┐
                     │ PUBLISHED(1)│     │  物理删除 DELETE │
                     │ 恢复发布     │     │  (不可逆)        │
                     └─────────────┘     └─────────────────┘
```

**源码位置**：`BlogArticleServiceImpl.java` — `publishArticle` / `unpublishArticle` / `moveToRecycle` / `restoreFromRecycle` / `deleteArticle`

**关键发现**：`unpublish` 将文章从 PUBLISHED 变为 DRAFT，但 **不更新 publishedAt**。如果文章再次发布，publishedAt 还是原来的时间，这可能影响按发布时间排序的结果。

#### 1.2 文章类型体系

**源码**：`enums/ArticleType.java`

| code | 枚举名 | 说明 | 特殊逻辑 |
| --- | --- | --- | --- |
| 1 | ORIGINAL | 原创 | 无 |
| 2 | REPOST | 转载 | **必填** originalUrl |
| 3 | TRANSLATE | 翻译 | **必填** originalUrl |

**校验逻辑**（`createArticle` / `updateArticle`）：

```
if (type == REPOST || type == TRANSLATE) && (originalUrl == null || originalUrl.isBlank()):
    throw "转载/翻译文章必须填写原文链接"
```

#### 1.3 文章创建完整流程

```
createArticle(request):
  1. 参数校验: title 非空(≤100字), content 非空, type 合法
  2. type=REPOST/TRANSLATE 时校验 originalUrl
  3. 构建 BlogArticle 对象
     - summary: 请求传入 → 使用; 未传入 → 截取 content 前 200 字符
     - wordCount: content.length()（字符数，非字数）
     - categoryId: 可选，不为空时校验分类是否存在
     - tags: 请求传入的标签名列表，序列化为 JSON 数组字符串
     - status: DRAFT(0)
     - viewCount: 0
     - likeCount: 0
     - commentCount: 0
  4. INSERT blog_article
  5. 处理标签关联:
     for each tag in request.tags:
       ├─ 标签存在(by name): useCount++, INSERT blog_article_tag (articleId, tagId)
       └─ 标签不存在: INSERT blog_tag (name, useCount=1), INSERT blog_article_tag
  6. 返回 articleId
```

**关键发现 1**：`wordCount` 使用 `content.length()` 计算字符数，不是真正的"字数"。对于 Markdown 内容，代码块、链接标记等标记符号也会被计入。

**关键发现 2**：文章标签同时存储在两个地方——`blog_article.tags`（JSON 字符串）和 `blog_article_tag`（关联表）。两份数据需要保持同步。

### 二、双标签存储架构

#### 2.1 为什么双存？

| 存储位置 | 格式 | 用途 | 查询方式 |
| --- | --- | --- | --- |
| `blog_article.tags` | `["Java","Spring"]` | 快速展示文章标签列表 | 直接读取，无需 JOIN |
| `blog_article_tag` | `(articleId, tagId)` | 标签维度的文章检索 | JOIN 查询 |

**同步逻辑**：所有标签变更操作（创建、更新、合并）都同时维护两处数据。

**潜在不一致风险**：如果只通过 SQL 直接修改 `blog_article_tag` 而不更新 `blog_article.tags`，两处数据会不一致。

#### 2.2 标签合并流程详解

**源码**：`BlogTagServiceImpl.mergeTags`

```
mergeTags(sourceTagId, targetTagId):
  1. 校验: sourceTag ≠ targetTag, 两个标签都存在
  2. 查询使用源标签的所有文章: blog_article_tag WHERE tagId = sourceTagId
  3. 遍历这些文章:
     a. 更新 blog_article.tags JSON:
        - 移除 sourceTag.name
        - 如果 targetTag.name 不在列表中 → 追加
        - 去重
     b. 删除旧关联: DELETE blog_article_tag WHERE articleId=? AND tagId=sourceTagId
     c. 添加新关联: INSERT IGNORE blog_article_tag (articleId, targetTagId)
  4. 更新目标标签 useCount: SELECT COUNT FROM blog_article_tag WHERE tagId=targetTagId
  5. 物理删除源标签: DELETE blog_tag WHERE id=sourceTagId
```

**注意**：合并是不可逆的。源标签被物理删除后无法恢复。

### 三、文章查询与分页

#### 3.1 用户端查询（BlogUserController）

**源码**：`BlogUserController.listArticles` + `BlogArticleServiceImpl.listArticles`

查询参数（`ArticleListRequest`）：

| 参数 | 类型 | 说明 | 默认值 |
| --- | --- | --- | --- |
| pageNum | int | 页码 | 1 |
| pageSize | int | 每页数量 | 10 |
| categoryId | Long | 分类筛选 | null（全部） |
| tagId | Long | 标签筛选 | null（全部） |
| keyword | String | 标题关键词 | null |

**查询逻辑**（Mapper XML `BlogArticleMapper.xml`）：

```sql
SELECT a.*, c.name as categoryName
FROM blog_article a
LEFT JOIN blog_category c ON a.category_id = c.id
WHERE a.status = 1                           -- 只查已发布
  AND a.is_deleted = 0                       -- 未删除
  <if test="categoryId != null">
    AND a.category_id = #{categoryId}
  </if>
  <if test="tagId != null">
    AND EXISTS (
      SELECT 1 FROM blog_article_tag at
      WHERE at.article_id = a.id AND at.tag_id = #{tagId}
    )
  </if>
  <if test="keyword != null and keyword != ''">
    AND a.title LIKE CONCAT('%', #{keyword}, '%')
  </if>
ORDER BY a.is_top DESC, a.published_at DESC  -- 置顶优先，发布时间倒序
```

**置顶逻辑**：`isTop` 字段（Boolean），置顶文章始终排在最前面。

#### 3.2 管理端查询（BlogAdminController）

**源码**：`BlogAdminController.listArticles` + `BlogArticleServiceImpl.adminListArticles`

查询参数（`AdminArticleListRequest`）：

| 参数 | 类型 | 说明 | 默认值 |
| --- | --- | --- | --- |
| pageNum | int | 页码 | 1 |
| pageSize | int | 每页数量 | 10 |
| status | Integer | 状态筛选 | null（全部） |
| categoryId | Long | 分类筛选 | null |
| keyword | String | 标题关键词 | null |
| startTime | String | 开始时间 | null |
| endTime | String | 结束时间 | null |

**差异**：管理端可查看所有状态的文章（包括草稿和回收站），且支持时间范围筛选。

#### 3.3 文章详情访问

```
用户端 getArticleDetail(articleId):
  1. 查询文章（status=1 且 is_deleted=0）
  2. 不存在 → throw "文章不存在"
  3. viewCount++ (UPDATE)
  4. 构建响应: article + categoryName + tags(JSON解析) + prevArticle + nextArticle

管理端 getArticleDetail(articleId):
  1. 查询文章（无状态限制）
  2. 不更新 viewCount
  3. 构建响应: article + categoryName + tags(JSON解析)
```

**上下篇导航**：用户端查询同一分类下按 `publishedAt` 排序的相邻文章：

```sql
-- 上一篇: 同分类中 publishedAt < 当前文章 的最新一篇
SELECT * FROM blog_article
WHERE category_id = #{categoryId}
  AND published_at < #{currentPublishedAt}
  AND status = 1 AND is_deleted = 0
ORDER BY published_at DESC LIMIT 1

-- 下一篇: 同分类中 publishedAt > 当前文章 的最早一篇
SELECT * FROM blog_article
WHERE category_id = #{categoryId}
  AND published_at > #{currentPublishedAt}
  AND status = 1 AND is_deleted = 0
ORDER BY published_at ASC LIMIT 1
```

**注意**：如果文章没有分类（categoryId=null），上下篇查询结果为空。

### 四、分类管理

#### 4.1 分类 CRUD

**源码**：`BlogCategoryServiceImpl`

| 操作 | 方法 | 说明 |
| --- | --- | --- |
| 创建 | `createCategory` | name 唯一校验 + INSERT |
| 更新 | `updateCategory` | `CategoryUpdateRequest` 支持修改 name、description、sortOrder |
| 删除 | `deleteCategory` | 检查是否有文章使用，有 → throw，无 → 物理删除 |
| 列表 | `listCategories` | 返回所有分类，按 sortOrder 排序 |

**删除保护**：如果分类下有文章（`SELECT COUNT FROM blog_article WHERE category_id=? AND is_deleted=0`），拒绝删除。

**`CategoryUpdateRequest` 字段**：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| name | String | 分类名 |
| description | String | 分类描述 |
| sortOrder | Integer | 排序权重（越小越靠前） |

### 五、配置管理

**源码**：`BlogConfigServiceImpl` + `BlogConfig` Domain

`blog_config` 表存储博客全局配置，key-value 结构：

| configKey | 说明 | 示例值 |
| --- | --- | --- |
| blog_name | 博客名称 | "CodeNest Blog" |
| blog_description | 博客描述 | "技术分享平台" |
| posts_per_page | 每页文章数 | "10" |
| allow_comment | 是否允许评论 | "true" |
| review_comment | 评论是否审核 | "false" |

**配置读取逻辑**：启动时全量加载到内存 Map，后续读取直接从 Map 获取，修改时同时更新 DB 和 Map。

### 六、深度发现与坑点

#### 6.1 已确认的代码 Bug / 问题

| 编号 | 问题 | 位置 | 影响 |
| --- | --- | --- | --- |
| BUG-1 | `unpublish` 不重置 `publishedAt` | `BlogArticleServiceImpl.unpublishArticle` | 重新发布后发布时间是旧的，排序可能不符合预期 |
| BUG-2 | `wordCount` 用 `content.length()` | `BlogArticleServiceImpl.createArticle` | Markdown 标记符号被计入，统计不准 |
| BUG-3 | 无分类文章无上下篇导航 | `BlogArticleServiceImpl.getPrevNextArticles` | categoryId=null 时返回空，无替代方案 |
| BUG-4 | 标签双存无事务保证 | `BlogArticleServiceImpl.createArticle` | tags JSON 和 article_tag 表在不同步骤写入，中间失败会导致不一致 |
| BUG-5 | 标签 JSON 和关联表可被 SQL 绕过 | 架构层面 | 直接操作 `blog_article_tag` 表不会同步 `blog_article.tags` |

#### 6.2 设计层面的潜在风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | 标签合并不可逆 | 源标签物理删除后无法恢复，建议改为软删除 |
| RISK-2 | 分类删除检查不含回收站文章 | `deleteCategory` 只查 `is_deleted=0` 的文章 | 回收站中的文章恢复后可能指向已删除的分类 |
| RISK-3 | 配置无缓存过期机制 | 启动时加载到内存，如果 DB 被直接修改，需要重启应用 |
| RISK-4 | 文章内容无版本管理 | 更新直接覆盖，无法回滚到历史版本 |

#### 6.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | 双标签存储 | JSON 快速展示 + 关联表灵活查询，读写分离优化 |
| H-2 | 分类删除保护 | 有关联文章时拒绝删除，防止悬空引用 |
| H-3 | 上下篇导航 | 用户端自动计算同分类上下篇，提升阅读体验 |
| H-4 | 文章软删除 | is_deleted 标记而非物理删除，可恢复 |

#### 6.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| 文章 CRUD | `BlogArticleServiceImpl.java` — 全部文章操作入口 |
| 标签管理 | `BlogTagServiceImpl.java` — 标签增删改查+合并 |
| 分类管理 | `BlogCategoryServiceImpl.java` — 分类 CRUD + 删除保护 |
| 配置管理 | `BlogConfigServiceImpl.java` — key-value 配置读写 |
| 用户端 API | `BlogUserController.java` — 已发布文章访问 |
| 管理端 API | `BlogAdminController.java` — 全状态文章管理 |
| 文章查询 SQL | `BlogArticleMapper.xml` — 分页+筛选+上下篇 |
| 标签查询 SQL | `BlogTagMapper.xml` — 标签统计+关联查询 |
| 文章状态枚举 | `ArticleStatus.java` — DRAFT/PUBLISHED/RECYCLE |
| 文章类型枚举 | `ArticleType.java` — ORIGINAL/REPOST/TRANSLATE |
| 分类删除失败 | 分类下还有文章 | 先迁移或下架相关文章 |
| 非作者访问草稿 | 服务会隐藏草稿和删除态 | 调试时用作者账号访问 |

## 验证清单

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
