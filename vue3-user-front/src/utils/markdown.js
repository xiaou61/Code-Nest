import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import bash from 'highlight.js/lib/languages/bash'
import css from 'highlight.js/lib/languages/css'
import java from 'highlight.js/lib/languages/java'
import javascript from 'highlight.js/lib/languages/javascript'
import json from 'highlight.js/lib/languages/json'
import markdown from 'highlight.js/lib/languages/markdown'
import python from 'highlight.js/lib/languages/python'
import shell from 'highlight.js/lib/languages/shell'
import sql from 'highlight.js/lib/languages/sql'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
import 'highlight.js/styles/github.css' // 代码高亮主题

const languages = {
  bash,
  css,
  java,
  javascript,
  json,
  markdown,
  python,
  shell,
  sql,
  typescript,
  xml,
}

Object.entries(languages).forEach(([name, language]) => {
  hljs.registerLanguage(name, language)
})

// 配置markdown-it
const md = new MarkdownIt({
  html: true,        // 启用HTML标签
  linkify: true,     // 自动识别链接
  typographer: true, // 启用一些语言中性的替换和引号的美化
  breaks: true,      // 将换行符转换为<br>
  highlight: function (str, lang) {
    // 代码高亮处理
    if (lang && hljs.getLanguage(lang)) {
      try {
        const result = hljs.highlight(str, { language: lang }).value
        return `<pre class="hljs"><code class="language-${lang}">${result}</code></pre>`
      } catch (__) {
        const escaped = md.utils.escapeHtml(str)
        return `<pre class="hljs"><code>${escaped}</code></pre>`
      }
    }

    // 默认处理（没有指定语言或语言不支持）
    const escaped = md.utils.escapeHtml(str)
    return `<pre class="hljs"><code>${escaped}</code></pre>`
  }
})

// 自定义渲染规则
md.renderer.rules.heading_open = function (tokens, idx, _options, _env, _renderer) {
  const token = tokens[idx]
  const level = token.tag.slice(1) // h1 -> 1, h2 -> 2, etc.
  return `<${token.tag} class="markdown-heading markdown-h${level}">`
}

md.renderer.rules.paragraph_open = function () {
  return '<p class="markdown-paragraph">'
}

md.renderer.rules.blockquote_open = function () {
  return '<blockquote class="markdown-blockquote">'
}

md.renderer.rules.bullet_list_open = function () {
  return '<ul class="markdown-list">'
}

md.renderer.rules.ordered_list_open = function () {
  return '<ol class="markdown-list markdown-ordered-list">'
}

md.renderer.rules.list_item_open = function () {
  return '<li class="markdown-list-item">'
}

md.renderer.rules.table_open = function () {
  return '<div class="markdown-table-wrapper"><table class="markdown-table">'
}

md.renderer.rules.table_close = function () {
  return '</table></div>'
}

// 自定义链接渲染（添加target="_blank"）
const defaultLinkRender = md.renderer.rules.link_open || function(tokens, idx, options, env, renderer) {
  return renderer.renderToken(tokens, idx, options)
}

md.renderer.rules.link_open = function (tokens, idx, options, env, renderer) {
  const aIndex = tokens[idx].attrIndex('target')

  if (aIndex < 0) {
    tokens[idx].attrPush(['target', '_blank'])
    tokens[idx].attrPush(['rel', 'noopener noreferrer'])
  } else {
    tokens[idx].attrs[aIndex][1] = '_blank'
  }

  return defaultLinkRender(tokens, idx, options, env, renderer)
}

/**
 * 渲染Markdown内容
 * @param {string} content - Markdown内容
 * @returns {string} - 渲染后的HTML
 */
export function renderMarkdown(content) {
  if (!content || typeof content !== 'string') {
    return ''
  }

  try {
    return md.render(content)
  } catch (error) {
    console.error('Markdown渲染失败:', error)
    // 回退到简单的文本处理
    return content.replace(/\n/g, '<br>')
  }
}

export default {
  renderMarkdown
}
