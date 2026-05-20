# 版本历史

这页建议分成两个视角来读：

1. **Git 演进视角**：项目真实是怎么从 `v1.8.x`、`v2.0.x` 一路演进到 `v2.2.0` 的。
2. **产品内版本墙视角**：后台 `/system/version` 和用户端 `/version-history` 这套功能本身是怎么实现、怎么发布、怎么维护的。

如果你只是想给用户写一条版本公告，看下面的“产品内版本墙”部分就够了。  
如果你想回答“这个仓库最近几版到底做了什么、哪些是大版本、哪些只是补强批次”，先看下面的 Git 演进视角。

## 推荐学习顺序

1. 先看“版本线怎么看”，建立整个仓库的演进脑图。
2. 再看 `v2.0.0` 到 `v2.2.0` 的关键版本节点，理解这条主线是怎么从 AI Runtime 重构一路走到工程质量与文档治理的。
3. 然后看“当前状态怎么判断”，理解 tag、分支、README 版本段和文档基线之间的关系。
4. 最后再看产品内版本墙的接口、表结构、状态流转和写作规范。

## 先看结论

如果只用一句话概括这条版本线，可以这样理解：

| 阶段 | 关键词 | 主要变化 |
| --- | --- | --- |
| `v1.8.1` ~ `v1.8.2` | 学习闭环与求职闭环 | 学习资产、Career Loop、内容到学习资产沉淀 |
| `v2.0.0` | AI Runtime 重构 | 移除 Coze，统一到 `LangChain4j + LangGraph4j + LlamaIndex` |
| `v2.0.1` | 重构后的功能补齐 | AI 治理总览、OJ 赛事增强、文件预览、积分导出 |
| `v2.1.0` | Growth Intelligence | 用户成长驾驶舱 + 管理端 AI 治理中心 |
| `v2.1.1` ~ `v2.1.2` | 安全与聊天室稳定性 | 文件权限、WebSocket 票据、限流、失败态闭环 |
| `v2.2.0` | 工程质量与文档治理 | 异常出口、参数校验、Mapper SQL、前端规范、独立 VitePress 文档站 |

## 版本线怎么看

这个仓库的“版本记录”不只存在于一个地方，而是分散在四类证据里：

| 来源 | 用途 | 适合回答什么问题 |
| --- | --- | --- |
| Git tag | 正式切过哪些版本点 | “仓库明确打过哪些 tag” |
| Git commit / PR merge | 某个版本批次真实是怎么落地的 | “这一版实际改了什么” |
| README 更新日志 | 面向读者的版本摘要 | “这一版对外怎么描述” |
| 产品内版本墙 | 面向用户的产品公告 | “用户在前台能看到什么” |

当前看到的实际情况是：

- 正式 tag 一直打到 `V2.0.0`
- `v2.0.1`、`v2.1.0`、`v2.1.2` 之后更多通过 **release commit / PR merge / README 版本段** 来表达
- `v2.2.0` 则明显已经进入 **工程质量 + 文档治理批次持续推进** 的状态

所以更稳的读法不是只看 tag，而是：

**tag 看“官方锚点”，git log 看“真实演进过程”，README 看“对外摘要”。**

## Git 演进视角

下面这部分按照真实提交来整理，不是按 PRD 想象出来的时间线。

### `v2.2.0` 当前工作线

这是当前仓库最明显的主线，但它和 `v2.0.0` 不一样，**不是一个单点 release commit 就结束**，而是一串持续推进的治理批次。

关键提交：

| 日期 | 提交 | 含义 |
| --- | --- | --- |
| 2026-04-26 | `857eacf` `chore: harden code quality for v2.2.0` | 明确把版本主题收敛到工程质量治理 |
| 2026-05-02 | `9551452` `chore: continue v2.2.0 mapper hardening` | 继续推进 Mapper / SQL 级别硬化 |
| 2026-05-16 | `0dd1819` `docs: add standalone VitePress site` | 独立文档站起点 |
| 2026-05-16 ~ 2026-05-20 | 一系列 `docs:` 提交 | 持续补模块文档、索引、运维、回归、交接、事故响应等 |

这一版真正的关键词不是“新增某个业务功能”，而是：

1. 统一异常出口和参数校验。
2. 收敛 Mapper SQL 和类型安全。
3. 补齐前端构建、Lint、样板治理。
4. 建立独立 VitePress 文档站和维护闭环。

对应资料：

- [README 更新日志](D:/onenodes/githubprojectstart/Code-Nest/README.md)
- [v2.2.0 工程质量计划](D:/onenodes/githubprojectstart/Code-Nest/docs/plans/2026-04-26-v2.2.0-code-quality-hardening.md)
- [v2.2.0 文档计划](/roadmap/v2.2.0-docs-plan)
- [文档同步基线](/reference/docs-sync-baseline)

### `v2.1.2` 聊天室防刷增强

这一版的 release 信号很清晰：

| 日期 | 提交 | 含义 |
| --- | --- | --- |
| 2026-05-02 | `282bdc4` `feat: release v2.1.2 security hardening` | 正式 release 提交 |
| 2026-05-02 | `30676d2` `feat: optimize frontend UX and bundle loading` | 同批次前端体验和包加载优化 |
| 2026-05-02 | `b999e59` `Merge pull request #49 from xiaou61/v2.1.2` | 版本分支合入主线 |

这一版的重点不是大架构变更，而是把聊天室和安全链路补稳：

- 聊天消息限流
- `TYPING` 事件降噪
- 消息校验
- 前端失败态闭环

对应资料：

- [README v2.1.2 段落](D:/onenodes/githubprojectstart/Code-Nest/README.md)
- [v2.1.2 审计记录](D:/onenodes/githubprojectstart/Code-Nest/docs/code-nest-v2.1.2-optimization-audit-2026-05-02.md)

### `v2.1.1` 安全加固稳定批次

这个版本在 README 里是明确存在的，但 Git 历史里**没有找到单独以 `v2.1.1` 命名的 release commit**。更像是 `v2.1.0` 和 `v2.1.2` 之间的一次稳定化发布批次。

可以确认的事实：

- README 有完整的 `v2.1.1` 更新段
- 功能主题集中在文件权限、WebSocket 票据、CORS 与 HTML 净化
- 但 Git log 中没有像 `v2.1.0`、`v2.1.2` 那样显式命名的 release 节点

这意味着：

- **对用户和产品来说，`v2.1.1` 是一个正式版本语义**
- **对仓库演进来说，它更像一段稳定性补强批次，而不是强锚点 release commit**

这是一个很典型的仓库现实：**版本语义比 tag/commit 更细，但 Git 锚点没有完全一一对应。**

### `v2.1.0` Growth Intelligence

这一版是 `v2.0.0` 之后最明确的一次“主题升级”：

| 日期 | 提交 | 含义 |
| --- | --- | --- |
| 2026-04-26 | `7695d44` `feat: release v2.1.0 growth intelligence` | 正式 release 提交 |
| 2026-04-26 | `56d1637` `Merge pull request #48 from xiaou61/v2.1.0` | 版本分支合入 |

关键词：

- 学习成长驾驶舱 2.1
- 成长分 / 能力雷达 / 今日任务
- 管理端 AI Runtime 质量中心

这说明项目在 `v2.0.0` 完成 AI 底座重构后，开始往“用户成长闭环 + AI 治理闭环”继续上层建设。

对应资料：

- [README v2.1.0 段落](D:/onenodes/githubprojectstart/Code-Nest/README.md)
- [v2.1.0 Growth Intelligence 设计](D:/onenodes/githubprojectstart/Code-Nest/docs/plans/2026-04-26-v2.1.0-growth-intelligence-design.md)
- [v2.1.0 Growth Intelligence 实施计划](D:/onenodes/githubprojectstart/Code-Nest/docs/plans/2026-04-26-v2.1.0-growth-intelligence.md)

### `v2.0.1` 重构后功能补齐

这一版的 Git 证据也比较完整：

| 日期 | 提交 | 含义 |
| --- | --- | --- |
| 2026-04-26 | `2f6dcac` `chore: bump version to v2.0.1` | 版本号切换 |
| 2026-04-26 | `be1f5ab` `feat: add v2.0.1 upgrade features` | 主要功能补齐 |
| 2026-04-26 | `40751ad` `docs: update v2.0.1 release notes` | 更新版本说明 |
| 2026-04-26 | `737cdcc` `Merge pull request #47 from xiaou61/v2.0.1` | 分支合入 |

这版更像是 `v2.0.0` AI Runtime 重构完成后的补强与稳定化：

- AI 治理总览
- 求职闭环二期
- OJ 赛事增强
- 文件预览和积分导出

### `v2.0.0` AI Runtime 全面重构

这是目前 Git 里最标准的一次“大版本锚点”：

| 日期 | 提交 | 含义 |
| --- | --- | --- |
| 2026-04-22 | `d9deeb2` `refactor: release ai runtime replatform v2.0.0` | 真正的重构主提交 |
| 2026-04-25 | `1a23763` `chore: bump version to v2.0.0` | 版本号提升 |
| 2026-04-25 | `e92abec` `Merge pull request #46 from xiaou61/v2.0.0` | 分支合入主线 |
| 2026-04-25 | tag `V2.0.0` | 当前可见的最新正式 tag 锚点 |

这版的意义非常大：

- 彻底移除 Coze 链路
- 统一到 `LangChain4j + LangGraph4j + LlamaIndex`
- 引入 Prompt / Schema / RAG / 回归 / 治理等工程化能力

从仓库演进角度看，`v2.0.0` 是当前主产品线的真正分水岭。

### `v1.8.1` / `v1.8.2`

`v1.8.x` 这一段还属于 `v2.0.0` 前的业务能力持续扩展阶段。

能确认的锚点：

| 日期 | 提交 | 含义 |
| --- | --- | --- |
| 2026-03-01 | `187c005` `docs(release): update README for v1.8.1 and bump maven revision` | `v1.8.1` 版本说明与版本号同步 |
| 2026-03-01 | `e2e83dc` `Merge pull request #44 from xiaou61/v1.8.1` | `v1.8.1` 合入 |
| 2026-03-14 | `6e80a9d` `Merge pull request #45 from xiaou61/v1.8.2` | `v1.8.2` 合入 |

结合 README 和现有文档，这一段主要对应：

- 学习资产转化引擎
- 求职闭环中台 / Career Loop
- 内容向学习资产沉淀

### 更早期版本

仓库里还能看到更早的 tag：

`V1.0.0`、`V1.1.1`、`V1.2.0`、`V1.2.1`、`V1.3.0`、`V1.4.0`、`V1.5.0`、`V1.6.0`、`V1.6.1`、`V1.6.3`、`V1.7.0`、`V1.7.1`、`V1.8.0`

这说明项目在 `v1.x` 阶段就已经持续发布，但当前文档站更详细整理的是 `v1.8.x` 之后的现代主线，因为这部分和现有代码、AI Runtime、文档治理关系最强。

## 当前状态怎么判断

现在如果有人问你：“项目当前到底在哪个版本状态？”更稳的回答方式是：

| 问题 | 看哪里 |
| --- | --- |
| 仓库当前工作线是什么 | `pom.xml`、前端 `package.json`、README 版本 badge |
| 文档和代码同步到哪一批 | [文档同步基线](/reference/docs-sync-baseline) |
| 当前大版本主题是什么 | README 的最新版本段 + [v2.2.0 文档计划](/roadmap/v2.2.0-docs-plan) |
| 产品内给用户展示什么版本公告 | `/version-history` 和后台 `/system/version` |

当前仓库的状态更接近：

- **代码主线版本**：`v2.2.0`
- **最近清晰 release 锚点**：`v2.1.2`
- **最近正式 tag 锚点**：`V2.0.0`
- **当前文档治理主线**：`v2.2.0` 文档中心持续补强

## 产品内版本墙视角

上面讲的是仓库版本怎么演进。下面这部分讲的是“产品内版本历史模块本身怎么实现”。

## 功能入口

| 端 | 页面 | 说明 |
| --- | --- | --- |
| 用户端 | `/version-history` | 查看时间线、搜索版本、打开版本详情 |
| 管理端 | `/system/version` | 创建、编辑、发布、隐藏、取消发布、批量操作 |
| 后端用户接口 | `/version` | 公开版本历史 |
| 后端管理接口 | `/admin/version` | 后台版本管理 |

## 源码地图

| 层级 | 文件 |
| --- | --- |
| 用户 API | `vue3-user-front/src/api/version.js` |
| 管理页面 | `vue3-admin-front/src/views/system/version/index.vue` |
| 管理 API | `vue3-admin-front/src/api/version.js` |
| 公开 Controller | `xiaou-version/src/main/java/com/xiaou/version/controller/pub/VersionHistoryController.java` |
| 管理 Controller | `xiaou-version/src/main/java/com/xiaou/version/controller/admin/VersionHistoryAdminController.java` |
| Service | `xiaou-version/src/main/java/com/xiaou/version/service/impl/VersionHistoryServiceImpl.java` |
| 实体 | `xiaou-version/src/main/java/com/xiaou/version/domain/VersionHistory.java` |

## 数据模型

核心表是 `version_history`：

| 字段 | 说明 |
| --- | --- |
| `version_number` | 版本号，例如 `v2.2.0` |
| `title` | 更新标题 |
| `update_type` | 更新类型 |
| `description` | 版本简要描述 |
| `prd_url` | PRD 或详细文档链接 |
| `release_time` | 发布时间 |
| `status` | 发布状态 |
| `sort_order` | 排序权重，数字越大越靠前 |
| `view_count` | 浏览次数 |
| `is_featured` | 是否重点推荐 |
| `deleted` | 逻辑删除标记 |

更新类型：

| 值 | 含义 |
| --- | --- |
| `1` | 重大更新 |
| `2` | 功能更新 |
| `3` | 修复更新 |
| `4` | 其他 |

发布状态：

| 值 | 含义 | 用户端是否可见 |
| --- | --- | --- |
| `0` | 草稿 | 否 |
| `1` | 已发布 | 是 |
| `2` | 已隐藏 | 否 |

## 用户侧接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/version/timeline` | `POST` | 分页查看已发布版本 |
| `/version/{id}` | `GET` | 查看版本详情 |
| `/version/view` | `POST` | 增加浏览次数 |
| `/version/search` | `POST` | 搜索已发布版本 |
| `/version/latest` | `GET` | 获取最新若干版本，默认 5 条 |

用户侧只读已发布版本。草稿和隐藏版本不能泄露到前台。

## 管理侧接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/admin/version/list` | `POST` | 管理端分页查询 |
| `/admin/version/{id}` | `GET` | 版本详情 |
| `/admin/version/create` | `POST` | 创建版本 |
| `/admin/version/update` | `PUT` | 更新版本 |
| `/admin/version/delete` | `DELETE` | 逻辑删除 |
| `/admin/version/publish` | `POST` | 发布 |
| `/admin/version/hide` | `POST` | 隐藏 |
| `/admin/version/unpublish` | `POST` | 取消发布，回到草稿 |
| `/admin/version/batch/publish` | `POST` | 批量发布 |
| `/admin/version/batch/hide` | `POST` | 批量隐藏 |
| `/admin/version/batch/delete` | `POST` | 批量逻辑删除 |
| `/admin/version/check-version/{versionNumber}` | `GET` | 检查版本号是否重复 |

## 创建和发布流程

1. 管理员在 `/system/version` 填写版本号、标题、类型、说明、PRD 链接和发布时间。
2. 后端检查版本号是否已存在。唯一约束是 `version_number + deleted`。
3. 发布时间必须能按 `yyyy-MM-dd HH:mm:ss` 解析。
4. 创建记录时写入创建人、初始浏览次数 0。
5. 草稿状态下用户端不可见。
6. 点击发布后，状态改为 `1`。
7. 用户端时间线和最新版本接口开始返回该记录。
8. 用户查看详情后，可调用 `/version/view` 增加浏览次数。

## 状态流转

```text
草稿(0) -> 已发布(1)
已发布(1) -> 已隐藏(2)
已隐藏(2) -> 已发布(1)
已发布(1) -> 草稿(0)
任意状态 -> 逻辑删除(deleted = 1)
```

## 一条好版本记录应该包含什么

| 内容 | 说明 |
| --- | --- |
| 版本号 | 和 Git / 部署 / 文档口径尽量一致 |
| 发布时间 | 方便和发版、告警、事故时间线对齐 |
| 一句话标题 | 让用户马上知道主题 |
| 简要描述 | 不要写“优化若干问题”这种空描述 |
| 重点功能 | 用户能感知的变化 |
| 修复项 | 稳定性、兼容性、权限、性能修复 |
| 升级注意事项 | 是否需要执行 SQL、清缓存、重启依赖 |
| 链接 | 可指向 PRD、设计文档、VitePress 页面 |

## 和文档站的关系

版本墙和文档站最好这样分工：

| 产品版本历史 | VitePress |
| --- | --- |
| 面向最终用户和管理员 | 面向开发者、维护者、学习者 |
| 讲“这一版更新了什么” | 讲“这一版背后改了哪些模块、为什么这么改、怎么维护” |
| 允许简洁摘要 | 需要保留源码、流程、验证和风险上下文 |
| 更接近发布公告 | 更接近长期知识库 |

所以现在这页改成 Git 演进视角，其实是在补一个空白：

**让维护者不只会写版本公告，还能读懂版本主线。**

## 验证清单

- `README` 里的大版本段和本页的主线描述不要互相矛盾。
- 版本号、时间、提交语义尽量以真实 Git 记录为准。
- 如果某个版本在 README 存在但 Git 没有显式 release commit，要明确写出这一点，不要假装它有单独锚点。
- 新发版本时，产品内版本墙、README 更新段和本页三者至少要同步其中两处，避免完全漂移。

## 常见误区

| 误区 | 问题 | 更稳的做法 |
| --- | --- | --- |
| 只看 tag 不看 commit | 很多后续版本没有继续打 tag | tag 看锚点，commit 看真实演进 |
| 只看 README 不看 Git | README 是摘要，不是完整证据 | 用 README 定主题，再用 git log 定批次 |
| 把产品版本墙当仓库演进史 | 用户公告粒度太粗 | 产品墙讲“用户看到什么”，文档页讲“仓库怎么演进” |
| 没有显式 release commit 也硬写成强锚点 | 会和真实历史对不上 | 明确标注“这是稳定化批次，不是单独 release commit” |
