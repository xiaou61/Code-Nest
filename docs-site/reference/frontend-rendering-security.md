# 前端渲染安全

Code Nest 里很多页面会展示用户或管理员录入的内容，例如社区帖子、博客正文、面试题答案、闪卡内容、OJ 题面、通知正文、聊天文本、JSON 高亮和文本 Diff。只要页面用了 `v-html`，就要明确这段 HTML 是从哪里来、有没有转义、有没有净化。

本页是新增富文本或 Markdown 展示时的安全检查入口。结论先记住一句：能不用 `v-html` 就不用；必须用时，只允许走统一渲染工具或显式转义后的 HTML。

## 源码入口

| 位置 | 作用 |
| --- | --- |
| `vue3-user-front/src/utils/markdown.js` | 用户端 Markdown 渲染、代码高亮和 DOMPurify 净化 |
| `vue3-admin-front/src/utils/markdown.js` | 管理端 Markdown 渲染、代码高亮和 DOMPurify 净化 |
| `vue3-user-front/src/views/dev-tools/JsonTool.vue` | JSON 高亮，手动转义后拼接 `span` |
| `vue3-user-front/src/views/dev-tools/TextDiff.vue` | Diff 展示，手动转义后拼接差异片段 |
| `vue3-admin-front/src/views/system/ai-config/index.vue` | RAG 命中高亮，先 `escapeHtml` 再插入 `mark` |

## v-html 使用审计

### 用户端（16 个文件）

| 文件 | 渲染方式 | 安全检查 |
| --- | --- | --- |
| `community/PostDetail.vue` | `renderMarkdown` | Markdown 正文，走统一链路 |
| `community/CreatePost.vue` | 编辑器预览 | 走 `renderMarkdown` |
| `blog/ArticleDetail.vue` | `renderMarkdown` | Markdown 正文 |
| `interview/QuestionDetail.vue` | `renderMarkdown` | 题目答案展示 |
| `interview/RandomQuestions.vue` | `renderMarkdown` | 随机题目展示 |
| `flashcard/FlashCard.vue` | `renderMarkdown` 或 `escapeHtml` | 按 `contentType` 分支 |
| `flashcard/DeckDetail.vue` | `escapeHtml + sanitizeHtml` | 纯文本换行展示 |
| `oj/ProblemDetail.vue` | `renderMarkdown` | OJ 题面 Markdown |
| `notification/index.vue` | `escapeHtml + sanitizeHtml` | 通知正文纯文本 |
| `chat/Index.vue` | `escapeHtml + sanitizeHtml` | 聊天文本 |
| `team/DiscussionList.vue` | `escapeHtml + sanitizeHtml` | 小组讨论文本 |
| `moyu-tools/DailyContent.vue` | `escapeHtml + sanitizeHtml` | 摸鱼内容文本 |
| `moyu-tools/bug-store.vue` | `escapeHtml + sanitizeHtml` | Bug 商店文本 |
| `dev-tools/JsonTool.vue` | 手动转义 + 拼接 `span` | JSON 语法高亮 |
| `dev-tools/TextDiff.vue` | 手动转义 + 拼接 `span` | Diff 片段高亮 |
| `lottery/index.vue` | 静态文本或转义 | 抽奖展示 |

### 管理端（3 个文件）

| 文件 | 渲染方式 | 安全检查 |
| --- | --- | --- |
| `community/posts/index.vue` | `renderMarkdown` | 帖子预览 |
| `interview/questions/index.vue` | `renderMarkdown` | 题目答案预览 |
| `system/ai-config/index.vue` | `escapeHtml` + `mark` 插入 | RAG 命中高亮 |

## 统一 Markdown 渲染链路

用户端和管理端都有同名工具：`@/utils/markdown`。它的主流程是：

1. `MarkdownIt` 解析 Markdown。
2. `highlight.js/lib/core` 只注册常用语言，避免整包导入。
3. 链接统一加 `target="_blank"` 和 `rel="noopener noreferrer"`。
4. 渲染结果进入 `sanitizeHtml`。
5. `sanitizeHtml` 调用 DOMPurify，禁止 `script`、`style`、`iframe`、`object`、`embed`，并禁止常见事件属性和内联样式。

典型调用方式：

```js
import { renderMarkdown } from '@/utils/markdown'

const html = renderMarkdown(content)
```

这些页面都应该走这条链路：

| 场景 | 例子 |
| --- | --- |
| 社区、博客、题库、OJ 正文 | `renderMarkdown(content)` 后再 `v-html` |
| 闪卡 Markdown 内容 | `contentType` 为 Markdown 时使用 `renderMarkdown` |
| 管理端 Markdown 预览 | 题库答案、社区帖子详情等预览 |

注意：当前 `MarkdownIt` 开启了 `html: true`，所以原始 HTML 会先被 Markdown 解析，再由 DOMPurify 净化。以后如果想允许更多标签或属性，应先评估 XSS 风险，再调整 `sanitizeOptions`。

## 纯文本进入 v-html

有些内容不是 Markdown，只是为了保留换行或插入少量标签才使用 `v-html`。这类场景必须先转义用户文本，再把换行替换成 `br`，最后再净化。

推荐模式：

```js
import { sanitizeHtml } from '@/utils/markdown'

const escapeHtml = (text) => {
  const div = document.createElement('div')
  div.textContent = text || ''
  return div.innerHTML
}

const html = sanitizeHtml(escapeHtml(content).replace(/\n/g, '<br>'))
```

已采用类似模式的页面包括通知详情、学习小组讨论、摸鱼内容、Bug 商店、闪卡纯文本和纯代码展示。

## 高亮和 Diff 片段

JSON 高亮、文本 Diff、RAG 命中高亮这类功能会拼接 `span` 或 `mark`。它们不是 Markdown，不能直接把原文塞进模板字符串。

正确顺序是：

1. 先对用户文本或模型返回文本执行 `escapeHtml`。
2. 再把已经转义的文本切片。
3. 最后只拼接受控标签，例如 `span class="..."` 或 `mark`。

`vue3-admin-front/src/views/system/ai-config/index.vue` 的 `renderHighlightedText` 是一个好例子：它先转义正文和关键词，再用安全后的关键词生成正则，最终只插入 `mark`。

## v-html 审计清单

新增或修改 `v-html` 时按这张表判断：

| 内容来源 | 推荐处理 |
| --- | --- |
| Markdown 正文 | `renderMarkdown(content)` |
| 普通用户文本 | `escapeHtml(content)` 后再 `sanitizeHtml` |
| 后端模板内容 | 如果模板含 HTML，限制可用标签；如果只是文本，先转义 |
| AI 输出或 RAG 片段 | 默认当作不可信文本，先转义再高亮 |
| JSON、Diff、搜索高亮 | 先转义原文，再拼受控标签 |
| 图片 URL、链接 URL | 校验协议和来源，不允许 `javascript:` 这类伪协议 |

## 新页面接入规则

1. 默认使用 `{{ text }}` 或组件属性展示文本。
2. 只有需要 Markdown、换行、代码高亮或差异片段时才使用 `v-html`。
3. 使用 Markdown 时统一从 `@/utils/markdown` 引入 `renderMarkdown`。
4. 使用纯文本 HTML 时必须有 `escapeHtml`。
5. 新增允许标签或属性时，同步检查用户端和管理端两个 `utils/markdown.js`。
6. 更新相关模块页的"安全边界"或"常见坑"。

## 常见坑

| 问题 | 原因 | 建议 |
| --- | --- | --- |
| `v-html="content"` 直接展示用户输入 | 没有转义和净化 | 改成 `renderMarkdown` 或 `escapeHtml + sanitizeHtml` |
| 高亮函数只转义正文，没转义关键词 | 关键词进入正则或 HTML 拼接 | 正文和关键词都转义，正则再用 `escapeRegExp` |
| 后端已经做了敏感词，就以为前端安全 | 敏感词不是 HTML 净化 | 敏感词、权限、XSS 是三层不同防线 |
| Markdown 中的 HTML 被显示出来或被清掉 | DOMPurify 白名单限制 | 先确认是否真的需要该标签，再评估是否放开 |
| 外链打开后能控制原页面 | 缺少 `rel="noopener noreferrer"` | 统一用 `renderMarkdown` 的链接渲染规则 |
| 用户端 `markdown.js` 放开了标签但管理端没同步 | 两端是独立副本 | 任何白名单变更必须两端同时改 |

## 验证清单

| 场景 | 预期 |
| --- | --- |
| Markdown 输入 `script` 标签 | 页面不执行脚本 |
| Markdown 输入图片事件属性 | `onerror` 等事件属性被移除 |
| 普通文本输入 `b` 标签 | 如果按纯文本展示，应显示文本语义，不执行 HTML |
| JSON 或 Diff 输入 HTML 片段 | 高亮仍正常，但标签不被浏览器执行 |
| 外链 Markdown | 新窗口打开，并带 `noopener noreferrer` |
| 新增 `v-html` | 能在源码中看到 `renderMarkdown`、`sanitizeHtml` 或显式 `escapeHtml` |
| 用户端和管理端同时新增标签 | 两端的 `sanitizeOptions` 保持一致 |
