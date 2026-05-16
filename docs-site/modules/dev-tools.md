# 开发者工具

开发者工具是用户端的轻量工具箱，当前全部在浏览器内完成，不依赖后端接口。它的定位不是“保存用户代码的平台”，而是“打开即用、少打扰、尽量不上传输入内容”的日常辅助工具。

## 推荐学习顺序

开发者工具适合用来学习“纯前端工具页”的写法：

1. 先看路由和首页卡片，理解工具如何挂到 `/dev-tools/*`。
2. 再看公共导航 `DevToolsNav`，理解工具页之间如何保持一致入口。
3. 接着看 JSON 工具，学习浏览器本地解析、历史记录和文件下载。
4. 然后看文本比对，学习前端如何处理用户输入、转义和差异展示。
5. 最后看翻译工具，理解“跳外部服务”和“自己调用后端/AI”之间的隐私边界。

## 功能定位

| 工具 | 用户入口 | 主要能力 | 是否调用后端 |
| --- | --- | --- | --- |
| 工具首页 | `/dev-tools` | 展示工具卡片并跳转 | 否 |
| JSON 工具 | `/dev-tools/json` | 格式化、压缩、校验、语法高亮、统计、历史记录 | 否 |
| 文本比对 | `/dev-tools/text-diff` | 行级/字符级比对、忽略空白、忽略大小写、并排/统一视图、导出 HTML | 否 |
| 聚合翻译 | `/dev-tools/translation` | 粘贴文本、加载示例、打开百度翻译页面 | 否 |

## 源码地图

| 层级 | 文件 |
| --- | --- |
| 路由 | `vue3-user-front/src/router/index.js` |
| 首页 | `vue3-user-front/src/views/dev-tools/index.vue` |
| 工具导航 | `vue3-user-front/src/components/DevToolsNav.vue` |
| JSON 工具 | `vue3-user-front/src/views/dev-tools/JsonTool.vue` |
| 文本比对 | `vue3-user-front/src/views/dev-tools/TextDiff.vue` |
| 聚合翻译 | `vue3-user-front/src/views/dev-tools/Translation.vue` |

## 读源码时先抓住三件事

1. 工具页没有后端模块，路由都在用户前端。
2. 用户输入默认留在浏览器内，复制、粘贴、下载使用 `navigator.clipboard`、`FileReader`、`Blob`、`URL.createObjectURL` 等浏览器 API。
3. 如果以后新增 AI 解释、云端翻译、历史同步等功能，才需要新增后端接口，并且文档要说明“输入会被发送到服务端或模型”。

## JSON 工具

JSON 工具的核心是三步：

1. 读取输入框文本。
2. 使用 `JSON.parse` 解析。
3. 根据动作使用 `JSON.stringify(parsed, null, 2)` 格式化，或 `JSON.stringify(parsed)` 压缩。

它额外做了几件对学习者很有用的事：

| 能力 | 源码表现 | 说明 |
| --- | --- | --- |
| 语法校验 | 捕获 `JSON.parse` 异常 | 错误会显示为 `JSON 语法错误: ...` |
| 结构统计 | `analyzeJson` | 统计类型、层级、键数量等，帮助理解输入结构 |
| 语法高亮 | `highlightJson` | 输出 HTML 高亮，展示前要注意转义 |
| 历史记录 | `localStorage.setItem('json-tool-history', ...)` | 最多保留 10 条本地历史 |
| 文件导入 | `FileReader` | 从本地文件读入文本，不上传 |
| 下载结果 | `Blob` | 生成 `formatted.json` |

学习建议：如果你要新增“JSON 转 TypeScript 类型”，可以复用当前流程：先解析 JSON，再递归分析字段类型，最后在浏览器本地生成文本结果。不要一上来就接后端。

## 文本比对

文本比对页的关键状态：

| 状态 | 作用 |
| --- | --- |
| `leftText`、`rightText` | 左右文本输入 |
| `diffMode` | `lines`、`words`、`chars` 等比对粒度 |
| `ignoreWhitespace` | 是否忽略空白字符 |
| `ignoreCase` | 是否忽略大小写 |
| `viewMode` | 并排视图或统一视图 |
| `diffResult` | 比对结果数组 |
| `currentDiffIndex` | 当前跳转到第几个差异 |

核心流程：

1. `performDiff` 先调用 `processText` 处理忽略空白、忽略大小写。
2. `splitText` 根据 `diffMode` 把文本切成行、词或字符。
3. 字符级比对会走 `computeLCS`，先求最长公共子序列，再生成新增/删除/相同片段。
4. 行级比对使用顺序比较，输出 added、removed、equal 等片段。
5. 展示时通过 `formatDiffForSideBySide` 或 `formatDiffForUnified` 生成 HTML。
6. 所有用户文本进入 HTML 前要走 `escapeHtml`，避免把用户输入当成页面代码执行。

这里最值得学的是“前端工具也要做安全处理”。虽然文本不上传，但结果区域用了 `v-html`，所以转义是必须的。

## 聚合翻译

翻译工具当前不是机器翻译 API，而是把用户输入编码后打开百度翻译：

```text
https://fanyi.baidu.com/#auto/zh/{encodedText}
```

这意味着它的边界非常清楚：

| 项 | 当前行为 |
| --- | --- |
| 输入保存 | 不保存 |
| 后端调用 | 无 |
| 翻译结果 | 由外部翻译页面提供 |
| 隐私风险 | 点击打开外部翻译后，文本会进入外部服务 |

如果以后要接入 AI 翻译，建议新增显式提示，比如“发送到模型翻译”，并在后端增加审计、限流和错误兜底。

## 添加新工具的推荐步骤

1. 在 `vue3-user-front/src/router/index.js` 新增 `/dev-tools/<tool-name>`。
2. 在 `vue3-user-front/src/views/dev-tools/` 新建页面组件。
3. 在 `vue3-user-front/src/components/DevToolsNav.vue` 增加导航项。
4. 在 `dev-tools/index.vue` 增加首页工具卡片。
5. 如果工具纯前端完成，优先使用浏览器 API。
6. 如果工具会调用后端或外部服务，在页面上明确提示输入数据会离开本机。

## 隐私和安全边界

| 场景 | 建议 |
| --- | --- |
| JSON、Diff、编码转换 | 默认本地处理 |
| AI 解释、云端翻译、SQL 分析 | 明确提示会发送到服务端或模型 |
| 使用 `v-html` 展示结果 | 先转义用户输入 |
| 保存历史 | 优先本地 `localStorage`，并提供清空入口 |
| 大文本处理 | 增加大小限制、加载态或 Web Worker |
| 文件导入 | 只读取文本，不自动上传 |

## 验证清单

- 访问 `/dev-tools`，三个工具卡片都能跳转。
- JSON 输入合法内容时能格式化、压缩、复制、下载。
- JSON 输入错误内容时能显示清晰错误，不崩页面。
- 文本比对能在行级和字符级得到不同结果。
- Diff 导出 HTML 后，用户输入中的 `<script>` 不能作为脚本执行。
- 翻译页点击外部翻译时，URL 中的文本经过编码。
- 刷新页面后，JSON 历史记录仍在本机可见，清空历史后 `localStorage` 被更新。

## 常见坑

| 问题 | 原因 | 处理 |
| --- | --- | --- |
| 明明是纯前端工具，却出现接口 404 | 新工具误用了请求封装 | 先判断能否浏览器内完成 |
| Diff 结果区域出现异常 HTML | 用户输入未转义就进入 `v-html` | 使用 `escapeHtml` 或渲染纯文本节点 |
| JSON 历史记录太大 | `localStorage` 空间有限 | 限制条数和单条长度 |
| 粘贴失败 | 浏览器剪贴板权限受限 | 保留手动输入方式 |
| 翻译工具被误解为内置翻译模型 | 当前只是打开外部页面 | 页面和文档都要说明边界 |
