# 开发者工具

开发者工具是用户端的轻量工具箱，当前全部在浏览器内完成，不依赖后端接口。它的定位不是"保存用户代码的平台"，而是"打开即用、少打扰、尽量不上传输入内容"的日常辅助工具。

三个工具全部不需要登录（`requiresAuth: false`），并且都开启了 keepAlive 缓存（`keepAlive: true`），切换页面时不会丢失输入内容。

## 推荐学习顺序

开发者工具适合用来学习"纯前端工具页"的写法：

1. 先看路由和首页卡片，理解工具如何挂到 `/dev-tools/*`。
2. 再看公共导航 `DevToolsNav`，理解工具页之间如何保持一致入口。
3. 接着看 JSON 工具，学习浏览器本地解析、历史记录和文件下载。
4. 然后看文本比对，学习前端如何处理用户输入、转义和差异展示。
5. 最后看翻译工具，理解 iframe 嵌入外部页面和隐私边界。

## 功能定位

| 工具 | 路由 | 路由名 | 需登录 | keepAlive | 主要能力 | 是否调用后端 |
| --- | --- | --- | --- | --- | --- | --- |
| 工具首页 | `/dev-tools` | `DevTools` | 否 | 是 | 展示工具卡片并跳转 | 否 |
| JSON 工具 | `/dev-tools/json` | `JsonTool` | 否 | 是 | 格式化、压缩、校验、语法高亮、统计、历史记录 | 否 |
| 文本比对 | `/dev-tools/text-diff` | `TextDiff` | 否 | 是 | 行级/词级/字符级比对、忽略空白、忽略大小写、并排/统一视图、导出 HTML | 否 |
| 聚合翻译 | `/dev-tools/translation` | `Translation` | 否 | 是 | 输入文本后嵌入百度翻译 iframe | 否 |

## 源码地图

| 层级 | 文件 | 行数 | 说明 |
| --- | --- | --- | --- |
| 路由定义 | `vue3-user-front/src/router/index.js` | ~580-620 | 4 条 `/dev-tools/*` 路由 |
| 首页 | `vue3-user-front/src/views/dev-tools/index.vue` | ~375 | 工具卡片网格布局 + 渐变背景 |
| 工具导航 | `vue3-user-front/src/components/DevToolsNav.vue` | ~188 | 品牌标识 + 工具链接 + 返回按钮 |
| JSON 工具 | `vue3-user-front/src/views/dev-tools/JsonTool.vue` | ~500+ | 格式化、压缩、校验、高亮、统计、历史、文件上传 |
| 文本比对 | `vue3-user-front/src/views/dev-tools/TextDiff.vue` | ~580+ | 三种比对模式、LCS 算法、并排/统一视图、HTML 导出 |
| 聚合翻译 | `vue3-user-front/src/views/dev-tools/Translation.vue` | ~200+ | iframe 嵌入百度翻译 + 新窗口打开 |

## 读源码时先抓住三件事

1. 工具页没有后端模块，路由都在用户前端。
2. 用户输入默认留在浏览器内，复制、粘贴、下载使用 `navigator.clipboard`、`FileReader`、`Blob`、`URL.createObjectURL` 等浏览器 API。
3. 如果以后新增 AI 解释、云端翻译、历史同步等功能，才需要新增后端接口，并且文档要说明"输入会被发送到服务端或模型"。

## 首页实现

首页 `index.vue` 是一个纯展示页：

- 使用渐变背景 `linear-gradient(135deg, #667eea, #764ba2)`，配合毛玻璃卡片效果。
- 三个工具卡片以 CSS Grid 排列（`grid-template-columns: repeat(auto-fit, minmax(350px, 1fr))`），自适应屏幕宽度。
- 每个卡片包含：工具图标、徽章标签（热门/实用/强大）、功能标签、跳转按钮。
- 卡片 hover 效果：`translateY(-10px)` + 渐变色彩叠加。
- 响应式设计：768px 以下单列排列，480px 以下缩小标题和卡片。

点击卡片时调用 `router.push('/dev-tools/&lt;toolName&gt;')`，跳转到具体工具页。

## JSON 工具

JSON 工具的核心是三步：

1. 读取输入框文本。
2. 使用 `JSON.parse` 解析。
3. 根据动作使用 `JSON.stringify(parsed, null, 2)` 格式化，或 `JSON.stringify(parsed)` 压缩。

它额外做了几件对学习者很有用的事：

| 能力 | 源码表现 | 说明 |
| --- | --- | --- |
| 语法校验 | 捕获 `JSON.parse` 异常 | 错误会显示为 `JSON 语法错误: ...` |
| 结构统计 | `analyzeJson()` | 统计类型（Object/Array）、层级深度、键数量 |
| 语法高亮 | `highlightJson()` | 用正则匹配键、字符串、数字、布尔、null，输出 HTML 高亮 span |
| 历史记录 | `localStorage.setItem('json-tool-history', ...)` | 最多保留 10 条本地历史，包含操作类型（format/compress）和时间戳 |
| 文件导入 | `el-upload` + `FileReader` | 从本地 .json/.txt 文件读入文本，不上传到服务器 |
| 下载结果 | `Blob` | 生成 `formatted.json` 文件下载 |
| 示例数据 | `loadExample()` | 提供预置 JSON 供新手体验 |
| 字符计数 | `inputText.length` | 实时显示输入字符数 |
| 粘贴 | `navigator.clipboard.readText()` | 从剪贴板粘贴 |

### JSON 统计信息

`analyzeJson(parsed)` 会计算并展示：

| 统计项 | 来源函数 | 示例输出 |
| --- | --- | --- |
| 类型 | `getType(value)` | `Object` 或 `Array` |
| 键数量 | `countKeys(obj)` | 递归统计所有层级的 key |
| 层级深度 | `getDepth(obj)` | 递归计算最大嵌套层数 |
| 大小 | `formatBytes()` | 输入/输出字节数 |

### JSON 高亮实现

`highlightJson()` 用一条正则把 JSON 文本中不同类型的值标记为不同颜色：

- `json-key`：带冒号的字符串（键名）
- `json-string`：不带冒号的字符串（值）
- `json-number`：数字
- `json-boolean`：true/false
- `json-null`：null

所有匹配结果在展示前走 `escapeHtml()` 转义，防止把 JSON 里的特殊字符当作页面代码执行。

学习建议：如果你要新增"JSON 转 TypeScript 类型"，可以复用当前流程：先解析 JSON，再递归分析字段类型，最后在浏览器本地生成文本结果。不要一上来就接后端。

## 文本比对

文本比对页的关键状态：

| 状态 | 类型 | 作用 |
| --- | --- | --- |
| `leftText`、`rightText` | `ref` | 左右文本输入 |
| `diffMode` | `ref` | `chars`、`words`、`lines` 比对粒度 |
| `ignoreWhitespace` | `ref` | 是否忽略空白字符 |
| `ignoreCase` | `ref` | 是否忽略大小写 |
| `diffResult` | `ref` | 比对结果数组 |
| `currentDiffIndex` | `ref` | 当前跳转到第几个差异 |
| `hasDiff` | `computed` | 是否有比对结果 |

核心流程：

1. `performDiff` 先调用 `processText` 处理忽略空白、忽略大小写。
2. `splitText` 根据 `diffMode` 把文本切成行、词或字符。
3. 字符级比对会走 `computeLCS`，先求最长公共子序列，再生成新增/删除/相同片段。
4. 行级比对使用顺序比较，输出 added、removed、equal 等片段。
5. 展示时通过 `formatDiffForSideBySide` 或 `formatDiffForUnified` 生成 HTML。
6. 所有用户文本进入 HTML 前要走 `escapeHtml`，避免把用户输入当成页面代码执行。

### LCS 算法细节

`computeLCS(arr1, arr2)` 实现了经典的动态规划最长公共子序列：

1. 构建 DP 表：`dp[i][j]` 表示 `arr1[0..i-1]` 和 `arr2[0..j-1]` 的 LCS 长度。
2. 回溯：从 `dp[m][n]` 开始，相同字符加入 LCS，不同时沿最大值方向回溯。
3. 生成差异：基于 LCS 把两个数组标记为 equal、added、removed 片段。

### 并排与统一视图

| 视图模式 | 实现函数 | 特点 |
| --- | --- | --- |
| 并排视图 | `formatDiffForSideBySide` | 左右两列，每列只显示属于本侧的片段 |
| 统一视图 | `formatDiffForUnified` | 单列展示，新增行带 + 标记，删除行带 - 标记 |

### 导出功能

`exportDiff()` 把比对结果生成 HTML 文件：

- 使用 `Blob` 创建文件。
- 通过 `URL.createObjectURL` 生成下载链接。
- 导出文件包含 CSS 样式，确保离线也能正确显示差异。

### 文件上传

比对页两边输入框都支持文件上传：

- 接受格式：`.txt`、`.md`、`.js`、`.css`、`.html`、`.json`、`.xml`。
- 上传只读取文本内容，不发送到服务器。

这里最值得学的是"前端工具也要做安全处理"。虽然文本不上传，但结果区域用了 `v-html`，所以转义是必须的。新增类似工具时，先按 [前端渲染安全](/reference/frontend-rendering-security) 的审计清单确认内容来源和转义方式。

## 聚合翻译

翻译工具当前不是机器翻译 API，而是通过 iframe 嵌入百度翻译页面：

### 实现方式

| 项 | 实现 |
| --- | --- |
| iframe URL | `getBaiduUrl()` 生成 \`https://fanyi.baidu.com/mtpe-individual/transText?query=&lt;encodedText&gt;&lang=auto2zh\` |
| 文本编码 | `encodeURIComponent(inputText)` |
| 新窗口打开 | `openBaiduTranslation()` 用 `window.open(url, '_blank')` |
| iframe 加载回调 | `onFrameLoad(platform)` 记录 iframe 加载完成日志 |
| 空状态 | 没有输入时显示提示信息和百度翻译图标 |

### 隐私边界

| 项 | 当前行为 |
| --- | --- |
| 输入保存 | 不保存在本地或后端 |
| 后端调用 | 无 |
| 翻译结果 | 由百度翻译 iframe 提供，Code Nest 不截取结果 |
| 隐私风险 | 点击新窗口打开后，文本会进入百度翻译的外部服务 |
| iframe 安全 | iframe 由百度域名提供，Code Nest 前端无法访问 iframe 内部内容（跨域限制） |

如果以后要接入 AI 翻译，建议新增显式提示，比如"发送到模型翻译"，并在后端增加审计、限流和错误兜底。

## 公共导航 DevToolsNav

`DevToolsNav.vue` 是三个工具页共享的顶部导航：

| 结构 | 说明 |
| --- | --- |
| 品牌区 | 工具图标 + "程序员工具"文字 |
| 导航链接 | 3 个 `router-link`：JSON 工具、文本比对、聚合翻译 |
| 返回按钮 | "返回工具箱" 按钮，跳转到 `/dev-tools` |

导航使用 `$route.path === tool.path` 判断当前高亮。

响应式设计：768px 以下导航链接变为竖排（图标 + 文字），480px 以下隐藏文字只显示图标。

## 添加新工具的推荐步骤

1. 在 `vue3-user-front/src/router/index.js` 新增 `/dev-tools/&lt;tool-name&gt;` 路由，设置 `requiresAuth: false`、`keepAlive: true`。
2. 在 `vue3-user-front/src/views/dev-tools/` 新建页面组件。
3. 在 `vue3-user-front/src/components/DevToolsNav.vue` 的 `tools` 数组增加导航项。
4. 在 `dev-tools/index.vue` 增加首页工具卡片。
5. 如果工具纯前端完成，优先使用浏览器 API（`navigator.clipboard`、`Blob`、`FileReader`）。
6. 如果工具会调用后端或外部服务，在页面上明确提示输入数据会离开本机。
7. 如果工具使用 `v-html` 展示结果，必须对用户输入做 `escapeHtml` 转义。

## 隐私和安全边界

| 场景 | 建议 |
| --- | --- |
| JSON、Diff、编码转换 | 默认本地处理 |
| AI 解释、云端翻译、SQL 分析 | 明确提示会发送到服务端或模型 |
| 使用 `v-html` 展示结果 | 先 `escapeHtml` 转义用户输入 |
| 保存历史 | 优先本地 `localStorage`，并提供清空入口 |
| 大文本处理 | 增加大小限制、加载态或 Web Worker |
| 文件导入 | 只读取文本，不自动上传 |
| iframe 嵌入外部页面 | 注意跨域安全，不能访问 iframe 内部 DOM |

## 验证清单

- 访问 `/dev-tools`，三个工具卡片都能跳转。
- JSON 输入合法内容时能格式化、压缩、复制、下载。
- JSON 输入错误内容时能显示清晰错误，不崩页面。
- JSON 统计信息正确显示类型、键数量、层级深度。
- JSON 历史记录能保存最多 10 条，刷新后不丢失，清空后 localStorage 被更新。
- 文本比对能在行级、词级和字符级得到不同结果。
- 文本比对忽略空白和忽略大小写选项生效。
- 文本比对导出 HTML 后，用户输入中的脚本标签不能作为脚本执行。
- 翻译页输入文本后 iframe 加载百度翻译。
- 翻译页新窗口打开按钮能正确跳转。
- 刷新页面后输入内容不丢失（keepAlive 生效）。
- 未登录用户可以正常使用所有工具。

## 常见坑

| 问题 | 原因 | 处理 |
| --- | --- | --- |
| 明明是纯前端工具，却出现接口 404 | 新工具误用了请求封装 | 先判断能否浏览器内完成 |
| Diff 结果区域出现异常 HTML | 用户输入未转义就进入 `v-html` | 使用 `escapeHtml` 或渲染纯文本节点 |
| JSON 历史记录太大 | `localStorage` 空间有限（一般 5MB） | 限制条数和单条长度 |
| 粘贴失败 | 浏览器剪贴板权限受限（HTTPS 环境） | 保留手动输入方式 |
| 翻译工具被误解为内置翻译模型 | 当前只是 iframe 嵌入百度翻译 | 页面和文档都要说明边界 |
| 翻译 iframe 空白 | 百度翻译域名被浏览器 CSP 或防火墙拦截 | 检查网络环境和 Content-Security-Policy |
| 文件上传读不到内容 | `FileReader` 异步读取，需要 await | 检查 `before-upload` 回调是否正确处理 |
| keepAlive 不生效 | `App.vue` 缺少 `&lt;KeepAlive&gt;` 包裹 | 检查 [前端路由索引](/reference/frontend-routes) 的 keepAlive 策略说明 |

## 文档维护提醒

新增开发者工具时至少同步四处：

1. 本页功能定位表和源码地图。
2. [前端路由索引](/reference/frontend-routes) 的路由表。
3. [模块最小回归矩阵](/reference/module-regression-matrix) 的开发者工具行。
4. [工具、摸鱼与版本](/modules/tools-moyu-version) 的功能地图。


## 相关模块

| 模块 | 关系 | 说明 |
| --- | --- | --- |
| [公共底座](/modules/common) | 强依赖 | 开发者工具模块依赖公共底座的统一响应和异常处理 |
| [鉴权与用户体系](/modules/auth) | 强依赖 | 部分工具需要用户登录态 |
| [用户账户与个人中心](/modules/user-account) | 间接依赖 | 用户工具使用记录依赖用户信息 |
| [工具、摸鱼与版本](/modules/tools-moyu-version) | 强依赖 | 开发者工具与工具模块紧密关联 |
| [系统运营后台](/modules/system-ops) | 间接依赖 | 工具管理可能在管理端 |
