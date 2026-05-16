# 代码工坊

代码工坊提供在线代码作品创作、发布、Fork、收藏、点赞、评论、模板和标签能力。

## 功能入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/codepen`、`/codepen/editor`、`/codepen/editor/:id`、`/codepen/my`、`/codepen/:id` |
| 管理端 | `/codepen/pens`、`/codepen/templates`、`/codepen/tags`、`/codepen/statistics` |
| 后端模块 | `xiaou-codepen` |

## 推荐学习顺序

代码工坊适合用来学习“在线作品平台”的后端设计。建议按下面顺序读：

1. 先看 `createOrSave`，理解草稿、发布和首次发布奖励。
2. 再看 `getDetail`，理解免费/付费作品的源码可见性。
3. 接着看 `forkPen`，理解积分扣除、作者收益、作品复制和交易记录。
4. 然后看点赞、收藏、评论、文件夹，理解作品周边互动。
5. 最后看管理端模板和标签，理解如何运营作品库。

## 源码地图

| 关注点 | 源码位置 |
| --- | --- |
| 用户端接口 | `xiaou-codepen/src/main/java/com/xiaou/codepen/controller/user/CodePenUserController.java` |
| 管理端接口 | `xiaou-codepen/src/main/java/com/xiaou/codepen/controller/admin/CodePenAdminController.java` |
| 作品主流程 | `xiaou-codepen/src/main/java/com/xiaou/codepen/service/impl/CodePenServiceImpl.java` |
| 评论流程 | `xiaou-codepen/src/main/java/com/xiaou/codepen/service/impl/CodePenCommentServiceImpl.java` |
| 文件夹流程 | `xiaou-codepen/src/main/java/com/xiaou/codepen/service/impl/CodePenFolderServiceImpl.java` |
| 标签流程 | `xiaou-codepen/src/main/java/com/xiaou/codepen/service/impl/CodePenTagServiceImpl.java` |
| 常量和状态 | `xiaou-codepen/src/main/java/com/xiaou/codepen/constant/CodePenConstants.java` |
| 配置项 | `xiaou-codepen/src/main/java/com/xiaou/codepen/config/CodePenProperties.java` |
| 核心 SQL | `sql/MySql/code_nest.sql`、`sql/v1.6.0/codepen.sql` |

## 用户侧接口域

`/user/code-pen` 覆盖：

- 创建、保存、更新、删除作品。
- Fork、检查 Fork 价格、收益统计。
- 我的作品、草稿、作品广场、推荐、热门、搜索。
- 按标签、分类、用户查询。
- 点赞、收藏、浏览。
- 评论、删除评论、评论列表。
- 文件夹创建、更新、删除、列表和内容。
- 模板、模板详情、标签、热门标签。

## 管理侧接口域

`/admin/code-pen` 覆盖：

- 作品列表、详情、状态更新、推荐、取消推荐、删除。
- 模板创建、更新、列表、删除。
- 标签创建、更新、删除、合并、列表。
- 评论列表、隐藏、删除。
- 统计数据。

## 开发注意

- 在线代码展示要隔离执行环境。
- 用户提交的 HTML/CSS/JS 需要防 XSS。
- Fork 涉及积分或收益时要保证流水一致。
- 作品发布和草稿保存是不同语义。

## 源码位置和配置

源码目录是 `xiaou-codepen`，用户端接口前缀是 `/user/code-pen`，管理端接口前缀是 `/admin/code-pen`。配置前缀是 `codepen`：

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `shareBaseUrl` | `https://code-nest.com/pen/` | 发布后返回的分享链接前缀 |
| `createRewardPoints` | `10` | 草稿首次发布或新作品直接发布的奖励积分 |

## 作品状态和限制

状态常量在 `CodePenConstants`：

| code | 状态 | 说明 |
| --- | --- | --- |
| `0` | 草稿 | 保存或 Fork 后默认状态 |
| `1` | 已发布 | 广场可见，首次发布奖励积分 |
| `2` | 已下架 | 后台运营下架 |
| `3` | 已删除 | 用户或后台删除 |

创建或保存时会校验：

- 标题不能为空且不超过 100 字符。
- HTML、CSS、JS 至少填写一种。
- Fork 价格范围为 0 到 1000 积分。
- 标签建议最多 5 个，前后端都要保持一致。

相关枚举常量还包括：

| 常量 | 值 | 说明 |
| --- | --- | --- |
| `Visibility.PRIVATE` | `0` | 私密作品 |
| `Visibility.PUBLIC` | `1` | 公开作品 |
| `FreeStatus.PAID` | `0` | 付费 Fork 后可看源码 |
| `FreeStatus.FREE` | `1` | 免费可看源码 |
| `TransactionType.FREE_FORK` | `0` | 免费 Fork 交易 |
| `TransactionType.PAID_FORK` | `1` | 付费 Fork 交易 |
| `CommentStatus.NORMAL` | `1` | 正常评论 |
| `CommentStatus.HIDDEN` | `2` | 被隐藏评论 |
| `CommentStatus.DELETED` | `3` | 已删除评论 |

评论长度限制为 1 到 500 字，作品描述最多 500 字。

## 从保存到发布

`createOrSave` 同时处理新建和更新：

1. 校验标题、代码内容和 Fork 价格。
2. 如果传了 `id`，读取作品并校验作者；如果没传 `id`，创建新作品。
3. 当旧状态是草稿、新状态是已发布，或新作品直接发布时，视为首次发布。
4. 首次发布会确保积分账户存在，增加 `createRewardPoints` 积分，并写入积分明细。
5. 保存作品，发布时返回分享链接、奖励积分和当前积分。

这意味着“保存草稿”和“发布作品”要在前端按钮上区分清楚。草稿不会奖励积分，只有首次发布才奖励。

## Fork 和源码可见性

作品支持免费和付费 Fork：

| 字段 | 说明 |
| --- | --- |
| `isFree = 1` | 免费作品，所有用户可查看源码并 Fork |
| `isFree = 0` | 付费作品，非作者必须 Fork 后才能查看源码 |
| `forkPrice` | 付费 Fork 价格 |
| `forkedFrom` | Fork 作品来源 |
| `totalIncome` | 原作品累计收益 |

付费 Fork 会在同一事务中扣除 Fork 用户积分、增加原作者积分、更新原作品收益、复制作品为草稿、增加原作品 Fork 数，并写入 `code_pen_fork_transaction`。积分明细当前复用 `ADMIN_GRANT` 类型，后续可单独增加 CodePen 收益类型。

源码可见性规则在 `getDetail`：

| 访问者 | 免费作品 | 付费作品 |
| --- | --- | --- |
| 作者本人 | 可看源码 | 可看源码 |
| 已 Fork 用户 | 可看源码 | 可看源码 |
| 普通访客 | 可看源码 | 不返回 HTML/CSS/JS，并提示 Fork 后可查看 |

Fork 后生成的新作品默认是草稿，标题会追加 ` (Fork)`，点赞、评论、收藏、浏览、收益都会重置为 0。

## 互动和列表性能

作品列表不返回 HTML/CSS/JS 代码，只返回展示字段和互动状态。列表会批量查询用户点赞和收藏状态，避免逐条查库。

互动规则：

- 已点赞再次点赞会报错；取消点赞会递减计数。
- 已收藏再次收藏会报错；取消收藏会递减计数。
- 浏览单独通过 `/view` 增加浏览数。
- 评论有正常、隐藏、删除三种状态，管理端可隐藏或删除。

收藏可以放入文件夹。`CodePenFolderServiceImpl` 负责文件夹创建、更新、删除和查询文件夹内作品 ID；收藏记录通过 `folderId` 关联文件夹。

## 数据表理解

| 表 | 说明 |
| --- | --- |
| `code_pen` | 作品主表，保存源码、外部资源、标签、状态、收益和计数 |
| `code_pen_like` | 点赞关系 |
| `code_pen_collect` | 收藏关系，可带 `folder_id` |
| `code_pen_folder` | 用户收藏文件夹 |
| `code_pen_comment` | 评论和回复 |
| `code_pen_tag` | 标签 |
| `code_pen_fork_transaction` | Fork 交易记录 |

## 管理端能力

管理端可以更新作品状态、推荐/取消推荐、删除作品、维护模板、维护标签、合并标签、隐藏或删除评论，并查看统计。

模板本质上也是 `code_pen` 记录，管理端创建模板时会把 `isTemplate` 设为 1、状态设为已发布、公开且免费，作者使用系统用户 ID `1`。用户端通过模板列表读取这些作品，用作编辑器起始代码。

## 常见坑

| 问题 | 原因 | 建议 |
| --- | --- | --- |
| 付费作品详情没有源码 | 当前用户不是作者，也没有 Fork 交易 | 先调用检查 Fork 价格，再执行 Fork |
| 作品保存成功但没奖励积分 | 只是草稿保存或已发布后的更新 | 只有首次发布奖励 |
| 免费 Fork 也会写交易记录 | 用于记录 Fork 关系和源码可见性 | 不要只靠积分流水判断是否 Fork |
| 标签超过 5 个 | 常量限制 `MAX_TAGS_COUNT = 5` | 前后端都要校验 |
| 模板出现在普通作品列表 | 模板也是作品记录 | 查询广场时要确认 mapper 是否排除模板 |

## 验证清单

| 场景 | 预期 |
| --- | --- |
| 空代码保存 | 返回至少需要一种代码 |
| Fork 价格小于 0 或大于 1000 | 返回价格范围错误 |
| 付费作品未 Fork 查看详情 | 不返回源码，显示 Fork 后可查看 |
| 付费 Fork 积分不足 | 返回当前积分和所需积分 |
| 草稿首次发布 | 增加 `createRewardPoints` 积分并返回分享链接 |
| 免费 Fork | 复制作品为草稿，写免费交易，不扣积分 |
| 已点赞再次点赞 | 返回已经点赞过了 |
| 评论被管理端隐藏 | 用户列表不应展示隐藏评论 |
| 收藏到文件夹 | 收藏关系写入 `folder_id` |
