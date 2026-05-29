# 社区与内容矩阵

社区与内容矩阵让用户生产内容、沉淀经验、交流作品，并把高价值内容转化为学习资产。它包含社区帖子、动态、博客、代码工坊和学习资产，不是单纯的"发帖系统"，而是一条从内容生产到学习沉淀的链路。

## 推荐学习顺序

内容矩阵建议按"发布入口 -> 互动运营 -> 学习转化 -> 风控审核"的顺序读：

1. 先读社区帖子，理解分类、标签、评论树、收藏和 AI 摘要。
2. 再读动态广场，理解短内容、频率限制、浏览去重和热门任务。
3. 接着读博客和 CodePen，理解长文、作品、付费 Fork 和积分联动。
4. 然后读学习资产，理解内容如何转成闪卡、计划、知识节点或题目候选。
5. 最后读敏感词风控，理解所有用户输入发布前为什么要经过统一策略。

## 源码地图

| 能力 | 源码入口 | Controller 数量 |
| --- | --- | --- |
| 社区帖子 | `xiaou-community/src/main/java/com/xiaou/community` | 4（用户 + 管理） |
| 动态广场 | `xiaou-moment/src/main/java/com/xiaou/moment` | 2（用户 + 管理） |
| 博客 | `xiaou-blog/src/main/java/com/xiaou/blog` | 2（用户 + 管理） |
| 代码工坊 | `xiaou-codepen/src/main/java/com/xiaou/codepen` | 4（用户 + 管理 + 标签 + 文件夹） |
| 学习资产 | `xiaou-learning-asset/src/main/java/com/xiaou/learningasset` | 2（用户 + 管理） |
| 敏感词风控 | `xiaou-sensitive/src/main/java/com/xiaou/sensitive` | 2（用户检测 + 管理端） |
| 用户端内容路由 | `vue3-user-front/src/router/index.js` | - |
| 管理端内容路由 | `vue3-admin-front/src/router/index.js` | - |

## 功能地图

| 功能 | 用户端入口 | 管理端入口 | 后端模块 | 深入文档 |
| --- | --- | --- | --- | --- |
| 社区帖子 | `/community` | `/community/posts` | `xiaou-community` | [社区帖子](/modules/community) |
| 社区分类和标签 | 社区筛选 | `/community/categories`、`/community/tags` | `xiaou-community` | [社区帖子](/modules/community) |
| 社区用户和评论 | 用户主页、评论区 | `/community/users`、`/community/comments` | `xiaou-community` | [社区帖子](/modules/community) |
| 动态广场 | `/moments` | `/moments/list` | `xiaou-moment` | [动态广场](/modules/moments) |
| 博客 | `/blog` | `/blog/articles` | `xiaou-blog` | [博客](/modules/blog) |
| 代码工坊 | `/codepen` | `/codepen/pens` | `xiaou-codepen` | [代码工坊](/modules/codepen) |
| 学习资产 | `/learning-assets` | `/learning-assets/review` | `xiaou-learning-asset` | [学习资产](/modules/learning-assets) |
| 敏感词风控 | 发布入口内置 | `/sensitive/*` | `xiaou-sensitive` | [敏感词风控](/modules/sensitive) |

## 内容类型怎么区分

| 内容 | 适合承载 | 特点 | 敏感词模块名 |
| --- | --- | --- | --- |
| 社区帖子 | 技术讨论、问题求助、经验复盘 | 有分类、标签、评论、收藏、AI 摘要 | `community_post`、`community_comment` |
| 动态 | 轻量状态、碎片记录、短内容互动 | 发布频率限制更明显，热门榜按互动刷新 | `moment`、`moment_comment` |
| 博客 | 长文、体系化总结、系列文章 | 有个人博客主页、开通成本、发布成本 | `blog_article` |
| CodePen | 前端作品、代码片段、模板 | 可在线编辑、发布、Fork、付费 Fork | 无直接调用（HTML/CSS/JS 内容） |
| 学习资产 | 可复习材料、练习计划、知识节点候选 | 来自内容转化，强调审核和二次学习 | 无直接调用 |

一句话判断：要讨论就发社区，要记录状态就发动态，要写长文就用博客，要展示代码作品就用 CodePen，要复用成学习材料就进学习资产。

## 内容发布链路详解

虽然各模块状态值不同，但生命周期可以统一理解为以下链路：

### 社区帖子发布链

`CommunityPostServiceImpl.createPost()` 的完整校验和写入流程：

```text
1. checkUserBanStatus()           → 禁言检查
2. StpUserUtil.checkLogin()       → 登录态校验
3. categoryId → getById → status  → 分类是否启用
4. insert community_post           → 写入帖子（status=1 已发布）
5. incrementPostCount()           → 更新用户发帖数统计
6. updatePostCount(categoryId, 1) → 更新分类下帖子数量
7. tagIds(最多5个) → 验证标签启用 → batchInsert → updatePostCount
```

**注意**：帖子发布目前不经过敏感词检测，内容安全由编辑器和展示层处理。但评论发布会经过 `SensitiveWordUtils.checkText()`。

### 评论发布链

`CommunityCommentServiceImpl.createComment()` 流程：

```text
1. checkUserBanStatus()                   → 禁言检查
2. StpUserUtil.checkLogin()               → 登录态校验
3. selectById(postId)                     → 帖子是否存在
4. SensitiveWordUtils.checkText(content,  → 敏感词检测
     "community", postId, currentUserId)
5. !sensitiveResult.getAllowed()          → 包含高风险词则拒绝
6. content = sensitiveResult.getProcessedText()  → 使用处理后文本
7. insert community_comment               → 写入评论（status=1）
8. updateCommentCount(postId, 1)          → 更新帖子评论数
9. incrementCommentCount()                → 更新用户评论数统计
10. NotificationUtil.sendCommunityMessage() → 通知帖子作者或被回复用户
```

**关键**：回复评论也走 `SensitiveWordUtils.checkText()`，但模块名同样是 `"community"`。

### 互动链路

| 操作 | Service 方法 | 写入 | 更新统计 | 通知 |
|------|-------------|------|---------|------|
| 点赞帖子 | `likePost()` | `community_post_like` | likeCount+1, 用户点赞数+1 | 通知帖子作者 |
| 取消点赞 | `unlikePost()` | 删除 like 记录 | likeCount-1, 用户点赞数-1 | 无 |
| 收藏帖子 | `collectPost()` | `community_post_collect` | collectCount+1, 用户收藏数+1 | 通知帖子作者 |
| 取消收藏 | `uncollectPost()` | 删除 collect 记录 | collectCount-1, 用户收藏数-1 | 无 |
| 点赞评论 | `likeComment()` | `community_comment_like` | likeCount+1, 用户点赞数+1 | 通知评论作者 |
| 回复评论 | `replyComment()` | `community_comment` | replyCount+1, commentCount+1, 用户评论数+1 | 通知被回复用户 |

所有互动操作都先调用 `checkUserBanStatus()` 防止禁言用户继续互动。

### 评论结构

评论采用两级树结构：

```text
一级评论 (parentId = 0)
  └─ 二级回复 (parentId = 一级评论 ID)
      └─ replyToId = 被回复的二级评论 ID
      └─ replyToUserId = 被回复用户 ID
      └─ replyToUserName = 被回复用户名
```

`convertToResponseWithReplies()` 会为每条一级评论预加载最多 2 条回复，并标记 `hasMoreReplies`。

### 热度公式源码

`CommunityPostServiceImpl.convertToResponse()` 中热度计算：

```text
hotScore = likeCount × 3.0 + commentCount × 5.0 + collectCount × 8.0 + viewCount × 0.1
```

动态广场热度公式在 `xiaou-moment` 中：

```text
hotScore = like × 2 + comment × 3 + view × 0.1
```

### 缓存策略

`CommunityCacheServiceImpl` 管理社区缓存：

| 缓存项 | Redis Key | TTL | 更新触发 |
|--------|-----------|-----|---------|
| 帖子详情 | `community:post:{id}` | 默认 | 帖子下架/删除时 evict |
| 搜索关键词 | `community:search:keywords` | 默认 | 列表查询时异步记录 |
| 热门帖子 | `community:hot:list` | 默认 | `CommunityHotPostTask` 定时刷新 |

`CommunityHotPostTask` 是定时任务，定期从数据库重算热门帖子并写入 Redis Sorted Set。

## 状态速查

| 模块 | 关键状态 | 状态值 |
| --- | --- | --- |
| 社区帖子 | 帖子状态 | `0` 草稿、`1` 已发布、`2` 下线、`3` 删除 |
| 社区帖子 | 分类/标签启用状态 | `0` 禁用、`1` 启用 |
| 社区帖子 | 社区用户禁言状态 | `0` 正常、`1` 禁言 |
| 动态 | 动态状态 | `0` 删除、`1` 正常、`2` 审核中 |
| 博客文章 | 文章状态 | `0` 草稿、`1` 已发布、`2` 下线、`3` 删除 |
| CodePen 作品 | 作品状态 | `0` 草稿、`1` 已发布、`2` 下线、`3` 删除 |
| 学习资产记录 | 记录状态 | 候选/已发布/已拒绝 |
| 敏感词策略 | 动作 | `replace`、`reject`、`warn` |

详细状态和表结构请进入对应模块页查看。

## 互动与热度

社区和动态都依赖互动数据，但口径不同：

| 模块 | 热度口径 | 刷新方式 |
| --- | --- | --- |
| 社区帖子 | `like * 3 + comment * 5 + collect * 8 + view * 0.1` | Redis 缓存 + 定时刷新 |
| 动态广场 | `like * 2 + comment * 3 + view * 0.1` | `moment:hot:list` Sorted Set，10 分钟 TTL |
| CodePen | 关注浏览、点赞、收藏、Fork、收益 | 实时计数 |
| 博客 | 关注文章浏览、分类标签、开通/发布积分成本 | 实时计数 |

热门内容不是实时每次都重算。社区、动态等模块都有缓存或定时任务，调试时要同时看数据库数据和 Redis/任务刷新情况。

## 内容转学习资产

学习资产是内容矩阵最重要的连接点。它把"看完就过去"的内容变成"以后还能复习和练习"的材料。

典型来源：

| 来源 | 可能转化为 | 转化方式 |
| --- | --- | --- |
| 社区帖子 | 知识节点候选、闪卡、练习计划 | 用户点击转化按钮 |
| 博客文章 | 闪卡、知识节点候选、练习计划 | 用户点击转化按钮 |
| CodePen 作品 | 练习计划、作品复盘材料 | 用户点击转化按钮 |
| 模拟面试报告 | 闪卡、薄弱项练习、知识节点候选 | 系统自动生成候选 |

转化流程：

1. 用户在内容详情页点击转化。
2. 前端提交来源类型、来源 ID、目标类型和摘要偏好。
3. 后端读取原始内容。
4. AI 或规则生成候选材料。
5. 对于可直接发布的目标，立即生成目标资产。
6. 对于需要审核的目标，进入管理端审核列表。
7. 审核通过后，写入闪卡、练习计划、知识图谱或题库相关表。

## 风控和审核

内容矩阵的风险主要来自用户输入，所以发布链路要关注：

| 风险 | 处理 | 涉及模块 |
| --- | --- | --- |
| 敏感词 | 通过敏感词模块做预处理、AC 匹配、白名单和策略动作 | `xiaou-sensitive` |
| 用户禁言 | 社区用户状态或聊天室禁言会阻止对应发布/发送 | `xiaou-community`、`xiaou-chat` |
| 高频发布 | 动态模块限制 5 分钟 3 条 | `xiaou-moment` |
| 分类/标签下线 | 社区发帖会校验分类和标签是否启用 | `xiaou-community` |
| 评论层级膨胀 | 社区评论固定为两级树 | `xiaou-community` |
| AI 摘要失败 | 社区摘要有开关和缓存，失败不能阻断帖子展示 | `xiaou-community` |
| 付费 Fork | CodePen 需要积分扣减、作者收益和作品复制在事务内一致 | `xiaou-codepen` |

## 内容类型积分规则

各内容类型与积分的联动方式不同：

| 内容类型 | 积分消耗 | 积分获得 | 说明 |
|---------|---------|---------|------|
| 社区帖子 | 无 | 无直接积分 | 发帖不消耗也不获得积分 |
| 社区评论 | 无 | 无直接积分 | 评论不消耗也不获得积分 |
| 动态 | 无 | 无直接积分 | 发布动态无积分联动 |
| 博客开通 | 消耗积分 | — | 开通博客主页需要积分成本 |
| 博客发布 | 消耗积分 | — | 发布文章需要积分成本 |
| CodePen 发布 | 无 | 无直接积分 | 发布作品无积分联动 |
| CodePen Fork | 消耗积分 | 作者获得收益 | 付费 Fork：买家扣减 → 作者收益 → 作品复制，事务内一致 |

**关键约束**：CodePen 付费 Fork 涉及三方积分变动，必须在 `@Transactional` 内完成，任一环节失败则全部回滚。

## 社区接口清单

### 用户端

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/community/posts/list` | 分页查询帖子列表 |
| GET | `/community/posts/{id}` | 帖子详情（浏览量 +1） |
| POST | `/community/posts` | 创建帖子 |
| POST | `/community/posts/{id}/like` | 点赞帖子 |
| DELETE | `/community/posts/{id}/like` | 取消点赞 |
| POST | `/community/posts/{id}/collect` | 收藏帖子 |
| DELETE | `/community/posts/{id}/collect` | 取消收藏 |
| GET | `/community/posts/hot` | 获取热门帖子 |
| POST | `/community/posts/{postId}/comments` | 获取帖子评论列表 |
| POST | `/community/posts/{postId}/comments/create` | 发表评论 |
| POST | `/community/comments/{id}/like` | 点赞评论 |
| DELETE | `/community/comments/{id}/like` | 取消点赞评论 |
| POST | `/community/comments/{id}/reply` | 回复评论 |
| POST | `/community/comments/{id}/replies` | 获取评论回复列表 |

### 管理端

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/community/posts` | 管理端帖子列表 |
| PUT | `/admin/community/posts/{id}/top` | 置顶帖子 |
| PUT | `/admin/community/posts/{id}/cancel-top` | 取消置顶 |
| PUT | `/admin/community/posts/{id}/disable` | 下架帖子 |
| DELETE | `/admin/community/posts/{id}` | 删除帖子 |
| GET | `/admin/community/comments` | 管理端评论列表 |
| DELETE | `/admin/community/comments/{id}` | 删除评论 |
| GET | `/admin/community/categories` | 分类列表 |
| POST | `/admin/community/categories` | 创建分类 |
| PUT | `/admin/community/categories/{id}` | 更新分类 |
| GET | `/admin/community/tags` | 标签列表 |
| POST | `/admin/community/tags` | 创建标签 |
| PUT | `/admin/community/tags/{id}` | 更新标签 |

## 管理端运营视角

运营同学通常按这个顺序排查内容问题：

1. 内容是否存在，状态是否正常。
2. 作者是否被禁言、封禁或删除。
3. 分类、标签、模板是否启用。
4. 敏感词日志里是否有命中（`sensitive_check_log` 表）。
5. 评论或互动是否被软删除。
6. 热度任务是否刷新。
7. 学习资产转化记录是否进入审核。
8. 如果涉及积分，检查积分流水和事务日志。

## 核心表按域分组

| 域 | 核心表 | 说明 |
| --- | --- | --- |
| 社区 | `community_post`、`community_comment`、`community_category`、`community_tag`、`community_user_status` | 帖子 CRUD、评论两级树、分类标签筛选、用户禁言 |
| 动态 | `moment`、`moment_comment`、`moment_like`、`moment_favorite`、`moment_views` | 短内容 CRUD、互动计数、浏览去重 |
| 博客 | `blog_article`、`blog_config`、`blog_category`、`blog_tag` | 长文 CRUD、博客配置、开通成本 |
| CodePen | `code_pen`、`code_pen_template`、`code_pen_fork_transaction`、作品互动相关表 | 作品 CRUD、模板、Fork 事务 |
| 学习资产 | 学习资产记录表、候选表、发布目标相关表 | 转化流程、审核状态 |
| 敏感词 | `sensitive_word`、`sensitive_check_log`、`sensitive_strategy`、`sensitive_whitelist` | 词库、检测日志、策略、白名单 |

具体字段和状态请看各模块深度页。

## 验证清单

| 场景 | 预期 |
| --- | --- |
| 发布社区帖子 | 分类/标签合法，帖子可在列表和详情页查看 |
| 评论社区帖子 | 二级评论关系正确，评论数更新 |
| 发布动态 | 频率限制生效，热门任务后可进入热门列表 |
| 开通博客并发布文章 | 积分扣减，文章进入已发布状态 |
| 发布 CodePen 作品 | 作品广场可见，详情页源码可按权限查看 |
| 付费 Fork CodePen | 买家扣积分，作者加收益，新作品为草稿 |
| 内容转学习资产 | 记录生成，直接发布或进入审核 |
| 敏感词命中 | 按模块策略替换、警告或拒绝 |

## 常见坑

| 问题 | 原因 | 处理 |
| --- | --- | --- |
| 内容列表看不到 | 状态不是发布/正常，或分类标签不可用 | 查状态字段和运营配置 |
| 热榜不更新 | 定时任务或 Redis 缓存未刷新 | 查任务日志和缓存 key |
| AI 摘要为空 | AI 摘要开关关闭、内容太短或调用失败 | 查 `community.ai.*` 配置和摘要缓存 |
| 评论结构混乱 | 回复没有归到一级父评论下 | 检查评论 parent/root 处理 |
| 学习资产审核找不到 | 目标类型是直接发布或记录创建失败 | 查学习资产转化记录和候选表 |
| CodePen Fork 积分不对 | 事务内扣减、奖励、复制任一环节失败 | 查 `code_pen_fork_transaction` 和积分明细 |

## 文档维护提醒

内容模块新增功能时，至少同步四处：

1. 对应模块页，写清状态、接口、表结构和异常。
2. [学习资产](/modules/learning-assets)，说明是否能转化为学习材料。
3. [敏感词风控](/modules/sensitive)，说明是否接入内容安全。
4. [全功能覆盖矩阵](/reference/feature-coverage)，补入口、API、表和文档链接。


## 相关模块

| 模块 | 关系 | 说明 |
| --- | --- | --- |
| [公共底座](/modules/common) | 强依赖 | 社区与内容矩阵依赖公共底座的统一响应和异常处理 |
| [鉴权与用户体系](/modules/auth) | 强依赖 | 内容发布和互动需要用户登录态 |
| [社区帖子](/modules/community) | 强依赖 | 社区帖子是内容矩阵的核心模块 |
| [动态广场](/modules/moments) | 强依赖 | 动态广场是内容矩阵的核心模块 |
| [博客](/modules/blog) | 强依赖 | 博客是内容矩阵的核心模块 |
| [代码工坊](/modules/codepen) | 强依赖 | 代码工坊是内容矩阵的核心模块 |
| [敏感词风控](/modules/sensitive) | 强依赖 | 内容发布必须经过敏感词检测 |
| [用户账户与个人中心](/modules/user-account) | 强依赖 | 内容作者信息依赖用户账户 |
