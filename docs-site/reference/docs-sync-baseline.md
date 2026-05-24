<script setup>
import baseline from '../.vitepress/cache/docs-sync-baseline.json'
</script>

# 文档同步基线

这页专门用来回答一个很实际的问题：

**当前这套文档，究竟同步到了哪一个 Git 提交。**

如果没有这页，后面很容易出现两种误判：

1. 文档看起来很完整，但其实只覆盖到几次提交之前。
2. 大家口头说"已经同步了"，却说不清到底同步到了哪个 commit。

## 先说结论

**不要只记"git log 第几个"。**

更稳的做法是同时记录：

| 字段 | 为什么需要 |
| --- | --- |
| 分支名 | 同一个仓库不同分支的文档状态可能完全不同 |
| 提交 SHA | 最稳定的唯一标识 |
| 提交时间 | 方便判断是不是最新一批 |
| 提交标题 | 方便人快速读懂这次同步的大致内容 |
| `git rev-list --count HEAD` | 可以作为"第几个提交"的辅助序号，但不能单独依赖 |

其中真正可靠的是 **SHA**，不是"第几个"。

## 当前同步基线

下面这组信息表示：**当前文档站在本次 dev/build 启动前自动读取到的 Git 基线。**

- 同步分支：`{{ baseline.branch }}`
- 跟踪分支：`{{ baseline.upstream || '未配置' }}`
- 提交短 SHA：`{{ baseline.shortSha }}`
- 提交完整 SHA：`{{ baseline.fullSha }}`
- Git 提交序号（辅助值）：`{{ baseline.commitCount }}`
- 提交时间：`{{ baseline.commitTime }}`
- 提交标题：`{{ baseline.subject }}`
- 工作区状态：`{{ baseline.workingTree }}`
- 基线生成时间：`{{ baseline.generatedAt }}`

> {{ baseline.note }}

## 这页该怎么判断"文档是不是落后了"

最简单的判断方式就是对比当前仓库 `HEAD`：

```powershell
git rev-parse HEAD
git rev-list --count HEAD
git log -1 --date=iso --pretty=format:"%H%n%h%n%cd%n%s"
```

如果你跑出来的结果和本页不一致，有三种常见情况：

| 情景 | SHA 对比 | 建议 |
| --- | --- | --- |
| 完全一致 | 相同 | 文档和代码基线一致，可以放心使用 |
| 代码有新提交 | 不同（代码更多） | 文档可能滞后，需要补同步 |
| 分支不同 | 分支名不同 | 先确认你看的到底是不是同一条分支的文档 |

## 为什么"第几个提交"不能单独当基线

因为它只是一个辅助阅读值，不是稳定 ID。

举个例子：

1. 你今天看到是第 `225` 个提交。
2. 明天有人 `rebase`、整理历史、切到别的分支，序号可能变化。
3. 但 `1ad6202756d76f10ab18f5d7ae1d440a698a6b59` 这个 SHA 只指向这一条提交。

所以更稳的规则应该是：

- **对机器**：看 SHA
- **对人**：看标题、时间和辅助序号

## 现在怎么刷新

这页已经不再推荐手工改 SHA 了，而是改成了脚本自动刷新。

当前规则是：

1. `npm run dev` 前自动刷新一次基线。
2. `npm run build` 前自动刷新一次基线。
3. `npm run preview` 前自动刷新一次基线。

如果你只是想单独刷新这页的数据，也可以手动执行：

```powershell
cd docs-site
npm run sync:baseline
```

这样做的好处是：

| 好处 | 说明 |
| --- | --- |
| 不用手工改 SHA | 少一个容易忘的动作 |
| dev/build 都会刷新 | 本地看效果和构建产物都会更接近实时基线 |
| 能显示工作区是否 dirty | 不会把"当前 HEAD"误当成"当前所有改动都已提交" |

## 基线脚本工作原理

`sync:baseline` 脚本（`scripts/generate-docs-sync-baseline.mjs`）在每次 dev/build/preview 前通过 npm `predev`/`prebuild`/`prepreview` 钩子自动执行。

它做的事情：

1. 用 `git rev-parse HEAD` 获取当前 SHA。
2. 用 `git rev-parse --abbrev-ref HEAD` 获取分支名。
3. 用 `git rev-parse --abbrev-ref @{upstream}` 获取跟踪分支。
4. 用 `git rev-list --count HEAD` 获取提交序号。
5. 用 `git log -1` 获取提交时间和标题。
6. 用 `git status --porcelain` 判断工作区是否 dirty。
7. 写入 `docs-site/.vitepress/cache/docs-sync-baseline.json`。
8. 本页通过 `import baseline` 读取这个 JSON 并展示。

如果脚本失败（例如不在 Git 仓库中），构建也会失败。

## 还需要手工维护什么

虽然基线值本身已经自动刷新，但这不代表所有文档同步动作都自动化了。

你仍然需要手工维护的，是：

1. 当前阶段路线图里"这一批到底补了什么"；当前主线是 [v2.2.1 文档计划](/roadmap/v2.2.1-docs-plan)。
2. [验证记录与已知问题](/manuals/verified-scenarios) 里真实补测和已知问题结论。
3. [文档维护规范](/guide/documentation-maintenance) 里新增的同步规则。

也就是说：

- **这页负责自动对齐 Git 基线**
- **其他页面负责人工总结"这一批内容意味着什么"**

## 和现有文档体系怎么配合

这页不是替代：

- [文档维护规范](/guide/documentation-maintenance)
- [验证记录与已知问题](/manuals/verified-scenarios)
- [v2.2.1 文档计划](/roadmap/v2.2.1-docs-plan)

它的职责更单一：

**只负责标记"当前文档同步基线在哪里"。**

你可以把它理解成这套文档站的"对齐刻度线"。

## 常见问题

| 问题 | 原因 | 修复 |
| --- | --- | --- |
| 基线数据总是同一个 SHA | 没有执行 `npm run build` 或 `npm run dev`，JSON 缓存未更新 | 手动 `npm run sync:baseline` |
| `workingTree` 显示 dirty | 有未提交的文件改动 | 提交代码后再刷新 |
| 构建失败提示找不到 JSON | 首次构建前没有执行 `sync:baseline` | `npm ci` 后自动执行，或手动跑一次 |
| 分支名显示 HEAD | 处于 detached HEAD 状态 | 切到一个实际分支 |
