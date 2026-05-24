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

### CodePenTagCreateRequest 字段

| 字段 | 类型 | 校验 | 说明 |
| --- | --- | --- | --- |
| `name` | String | @NotBlank, @Size(max=50) | 标签名称 |
| `color` | String | @Pattern(^#[0-9a-fA-F]{6}$) | 标签颜色，十六进制色值 |
| `description` | String | @Size(max=200) | 标签描述 |

### CodePenTagUpdateRequest 字段

| 字段 | 类型 | 校验 | 说明 |
| --- | --- | --- | --- |
| `name` | String | @Size(max=50) | 标签名称 |
| `color` | String | @Pattern(^#[0-9a-fA-F]{6}$) | 标签颜色 |
| `description` | String | @Size(max=200) | 标签描述 |

### FolderCreateRequest 字段

| 字段 | 类型 | 校验 | 说明 |
| --- | --- | --- | --- |
| `folderName` | String | @NotBlank, @Size(max=100) | 文件夹名称 |
| `parentId` | Long | @Positive | 父文件夹 ID，空表示根级 |
| `description` | String | @Size(max=500) | 文件夹描述 |
| `icon` | String | @Size(max=50) | 图标标识 |

### FolderUpdateRequest 字段

| 字段 | 类型 | 校验 | 说明 |
| --- | --- | --- | --- |
| `folderName` | String | @Size(max=100) | 文件夹名称 |
| `parentId` | Long | @Positive | 父文件夹 ID |
| `description` | String | @Size(max=500) | 文件夹描述 |
| `icon` | String | @Size(max=50) | 图标标识 |

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

---

## 代码工坊深度拆解

以下内容来自对 xiaou-codepen 模块全部源码的逐行阅读，覆盖 4 个 ServiceImpl、2 个 Controller、7 个 Domain、7 个 Mapper、2 个 Mapper XML。

### 一、Fork 经济体系详解

#### 1.1 付费 Fork 完整流程

**源码**：`CodePenServiceImpl.forkPen`（`service/impl/CodePenServiceImpl.java:338-447`）

```
forkPen(request, userId):
  1. 查询原作品
  2. 判断 isFree / forkPrice
  3. if 付费且 price > 0:
     a. 查询 Fork 用户余额 → 不足则 throw
     b. 扣减 Fork 用户积分: pointsBalanceMapper.subtractPoints(userId, forkPrice)
        → affected=0: throw "积分扣除失败"（并发扣减保护）
     c. 确保原作者积分账户存在: ensureUserPointsBalance(originalAuthorId)
     d. 增加原作者积分: pointsBalanceMapper.addPoints(originalAuthorId, forkPrice)
     e. 更新原作品收益: codePenMapper.updateIncome(penId, forkPrice)
     f. 记录 Fork 用户积分明细 (pointsChange=-forkPrice)
     g. 记录原作者积分明细 (pointsChange=+forkPrice)
  4. 复制作品为草稿 (BeanUtil.copyProperties + 重置所有计数)
  5. INSERT 新作品
  6. 原作品 forkCount++
  7. INSERT fork_transaction
  8. 返回 ForkResponse
```

**积分类型复用问题**：Fork 扣减和收益的积分明细都使用 `PointsType.ADMIN_GRANT.getCode()`（值为 1），而不是专用的 CodePen 类型。这意味着从积分明细无法区分"管理员发放"和"CodePen Fork 交易"。

#### 1.2 源码可见性判定算法

**源码**：`CodePenServiceImpl.canViewCode`

```
canViewCode(codePen, currentUserId, purchasedOriginalPenIds):
  if isFree == 1:         return true    // 免费作品，所有人可看
  if userId == currentUserId: return true    // 作者可看
  if penId in purchasedOriginalPenIds: return true  // 已 Fork 用户可看
  return false
```

**已购买集合获取**（`getPurchasedOriginalPenIds`）：

```
1. 过滤出列表中的付费且非自己的作品 ID
2. 查询 fork_transaction 表: SELECT original_pen_id WHERE fork_user_id = ? AND original_pen_id IN (?)
3. 返回已购买的原始作品 ID 集合
```

**关键发现**：`purchasedOriginalPenIds` 查询的是 `fork_transaction` 表，不是 `code_pen` 表的 `forkedFrom` 字段。这意味着只要有过 Fork 交易记录（包括免费 Fork），就能查看源码。免费 Fork 也会写入交易记录（`transactionType=0`），所以免费 Fork 后再查看付费原作品也能看到源码——这是一个逻辑漏洞。

#### 1.3 积分并发安全

`subtractPoints` 和 `addPoints` 使用 SQL 原子操作：

```sql
UPDATE user_points_balance SET total_points = total_points - #{points} WHERE user_id = #{userId}
UPDATE user_points_balance SET total_points = total_points + #{points} WHERE user_id = #{userId}
```

扣减返回 `affected=0` 时抛出异常，说明余额不足或用户不存在（MySQL 不更新则返回 0 行）。

**但缺少余额非负约束**：SQL 中没有 `AND total_points >= #{points}` 条件，如果并发两次扣减，可能导致余额变为负数。

### 二、作品创建与发布积分奖励

**源码**：`CodePenServiceImpl.createOrSave`

```
首次发布判定:
  - 已有作品: 旧状态==0(草稿) && 新状态==1(已发布) → isNewPublish=true
  - 新作品: 新状态==1(已发布) → isNewPublish=true

首次发布奖励:
  1. ensureUserPointsBalance(userId)  // 确保积分账户存在
  2. pointsBalanceMapper.addPoints(userId, createRewardPoints)  // 默认 10 积分
  3. 记录积分明细 (pointsType=ADMIN_GRANT, description="创建代码作品")
  4. 设置 publishTime
  5. 返回 CodePenCreateResponse (penId, status, pointsAdded, pointsTotal, shareUrl)
```

**重复发布保护**：只有从草稿→已发布才触发奖励。已发布作品更新不会重复奖励。

### 三、互动功能详解

#### 3.1 点赞/取消点赞

```
like(penId, userId):
  if 已点赞 → throw "已经点赞过了"
  INSERT code_pen_like
  codePen.likeCount++

unlike(penId, userId):
  DELETE code_pen_like WHERE penId=? AND userId=?
  codePen.likeCount--    // 无条件递减，即使记录不存在也会递减
```

**关键发现**：`unlike` 不检查点赞记录是否存在，直接删除+递减。如果用户未点赞就调用取消点赞，`delete` 返回 0 行但 `decrementLikeCount` 仍然执行，导致 `likeCount` 变成负数。

#### 3.2 收藏/取消收藏

同点赞逻辑，`uncollect` 也不检查记录是否存在，直接删除+递减。

#### 3.3 评论系统

**源码**：`CodePenCommentServiceImpl`

| 操作 | 方法 | 说明 |
| --- | --- | --- |
| 创建评论 | `createComment` | 校验内容(≤500字) + INSERT + commentCount++ |
| 删除评论 | `deleteComment` | 只能删自己的 + 物理删除 + commentCount-- |
| 管理员隐藏 | `hideComment` | status 设为 2（隐藏），**不递减 commentCount** |
| 管理员删除 | `adminDeleteComment` | 物理删除 + commentCount-- |
| 查询列表 | `getCommentList` | 只查 status=1 的评论，批量获取用户信息避免 N+1 |

**关键发现**：管理员隐藏评论后 `commentCount` 不变，但 `getCommentList` 只查 `status=1`，导致 `commentCount` 与实际可见评论数不一致。

### 四、文件夹与收藏

**源码**：`CodePenFolderServiceImpl`

| 操作 | 校验 | 说明 |
| --- | --- | --- |
| 创建 | name 非空(≤100字) | 自动设置 collectCount=0 |
| 更新 | 归属校验 | 只能更新自己的文件夹 |
| 删除 | 归属校验 | 物理删除，**不清理关联的收藏记录** |
| 查看内容 | 归属校验 | 返回文件夹内的 penId 列表 |

**关键发现**：删除文件夹不清理 `code_pen_collect` 中 `folderId` 指向该文件夹的收藏记录。删除后这些收藏记录变成孤儿数据，`folderId` 指向不存在的文件夹。

### 五、标签合并

**源码**：`CodePenTagServiceImpl.mergeTags`

```
mergeTags(sourceId, targetId):
  1. 校验两个标签都存在
  2. tagMapper.mergeTags(sourceId, targetId)  // SQL: 将源标签使用次数加到目标标签
  3. tagMapper.deleteById(sourceId)            // 物理删除源标签
```

**与 Blog 模块的差异**：CodePen 的标签合并不更新作品的 `tags` JSON 字段，只合并使用次数。Blog 模块的标签合并会遍历文章替换 JSON 中的标签名。

### 六、深度发现与坑点

#### 6.1 已确认的代码问题

| 编号 | 问题 | 位置 | 影响 |
| --- | --- | --- | --- |
| BUG-1 | unlike/uncollect 无条件递减计数 | `CodePenServiceImpl.unlike/unlike` | 计数可能变负 |
| BUG-2 | 隐藏评论不递减 commentCount | `CodePenCommentServiceImpl.hideComment` | commentCount 与可见数不一致 |
| BUG-3 | 删除文件夹不清理收藏记录 | `CodePenFolderServiceImpl.deleteFolder` | 孤儿收藏数据 |
| BUG-4 | 积分扣减无余额非负约束 | `pointsBalanceMapper.subtractPoints` | 并发扣减可能导致负余额 |
| BUG-5 | 积分类型复用 ADMIN_GRANT | `CodePenServiceImpl.forkPen` | 无法区分 Fork 交易和管理员发放 |
| BUG-6 | 标签合并不更新作品 tags JSON | `CodePenTagServiceImpl.mergeTags` | 作品仍显示已删除的源标签 |

#### 6.2 设计层面的潜在风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | 免费 Fork 可查看付费原作品源码 | 免费 Fork 也写交易记录，`canViewCode` 查交易记录判定 |
| RISK-2 | 作品物理删除 | 删除后无法恢复，点赞/收藏/评论的计数可能不一致 |
| RISK-3 | 模板使用系统用户 ID=1 | 如果 ID=1 的真实用户存在，模板会出现在该用户的作品列表中 |
| RISK-4 | Fork 标题硬编码追加 " (Fork)" | 多次 Fork 会导致标题越来越长 |

#### 6.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | 批量查询避免 N+1 | 列表批量查点赞/收藏状态，评论批量查用户信息 |
| H-2 | Fork 经济闭环 | 扣减+收益+交易记录+收益统计，完整闭环 |
| H-3 | 源码可见性控制 | 付费作品非作者/Fork用户不返回代码，保护知识产权 |
| H-4 | 首次发布奖励 | 草稿→发布才奖励，防止重复刷积分 |
| H-5 | 列表不返回代码 | 分页查询省略 HTML/CSS/JS，减少传输量 |

#### 6.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| 作品 CRUD + Fork | `CodePenServiceImpl.java` — 全部核心逻辑 |
| 评论管理 | `CodePenCommentServiceImpl.java` — 创建/删除/隐藏 |
| 文件夹管理 | `CodePenFolderServiceImpl.java` — 收藏夹 CRUD |
| 标签管理 | `CodePenTagServiceImpl.java` — 标签 CRUD + 合并 |
| 用户端 API | `CodePenUserController.java` — 30+ 端点 |
| 管理端 API | `CodePenAdminController.java` — 模板/标签/评论管理 |
| 常量定义 | `CodePenConstants.java` — 状态/可见性/交易类型 |
| 配置项 | `CodePenProperties.java` — shareBaseUrl/createRewardPoints |
