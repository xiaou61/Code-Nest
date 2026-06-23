import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import bash from 'highlight.js/lib/languages/bash'
import c from 'highlight.js/lib/languages/c'
import cpp from 'highlight.js/lib/languages/cpp'
import css from 'highlight.js/lib/languages/css'
import go from 'highlight.js/lib/languages/go'
import java from 'highlight.js/lib/languages/java'
import javascript from 'highlight.js/lib/languages/javascript'
import json from 'highlight.js/lib/languages/json'
import markdown from 'highlight.js/lib/languages/markdown'
import plaintext from 'highlight.js/lib/languages/plaintext'
import python from 'highlight.js/lib/languages/python'
import sql from 'highlight.js/lib/languages/sql'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
import DOMPurify from 'dompurify'
import 'highlight.js/styles/github.css'

const supportedLanguages = {
  bash,
  shell: bash,
  sh: bash,
  c,
  cpp,
  'c++': cpp,
  css,
  go,
  golang: go,
  java,
  javascript,
  js: javascript,
  json,
  markdown,
  md: markdown,
  plaintext,
  text: plaintext,
  python,
  py: python,
  sql,
  typescript,
  ts: typescript,
  xml,
  html: xml,
  vue: xml
}

Object.entries(supportedLanguages).forEach(([name, language]) => {
  if (!hljs.getLanguage(name)) {
    hljs.registerLanguage(name, language)
  }
})

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  breaks: true,
  highlight(str, lang) {
    const normalizedLang = lang ? lang.toLowerCase() : ''

    if (normalizedLang && hljs.getLanguage(normalizedLang)) {
      try {
        const result = hljs.highlight(str, { language: normalizedLang }).value
        return `<pre class="hljs"><code class="language-${normalizedLang}">${result}</code></pre>`
      } catch (error) {
        console.warn('代码高亮失败，已回退为纯文本:', error)
      }
    }

    const escaped = md.utils.escapeHtml(str)
    return `<pre class="hljs"><code>${escaped}</code></pre>`
  }
})

const sanitizeOptions = {
  USE_PROFILES: { html: true },
  ADD_ATTR: ['target', 'rel'],
  FORBID_TAGS: ['script', 'style', 'iframe', 'object', 'embed'],
  FORBID_ATTR: ['onerror', 'onload', 'onclick', 'onmouseover', 'style']
}

export function sanitizeHtml(html) {
  if (!html || typeof html !== 'string') {
    return ''
  }

  return DOMPurify.sanitize(html, sanitizeOptions)
}

md.renderer.rules.heading_open = function (tokens, idx) {
  const token = tokens[idx]
  const level = token.tag.slice(1)
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

const defaultLinkRender = md.renderer.rules.link_open || function (tokens, idx, options, env, renderer) {
  return renderer.renderToken(tokens, idx, options)
}

md.renderer.rules.link_open = function (tokens, idx, options, env, renderer) {
  const targetIndex = tokens[idx].attrIndex('target')
  const relIndex = tokens[idx].attrIndex('rel')

  if (targetIndex < 0) {
    tokens[idx].attrPush(['target', '_blank'])
  } else {
    tokens[idx].attrs[targetIndex][1] = '_blank'
  }

  if (relIndex < 0) {
    tokens[idx].attrPush(['rel', 'noopener noreferrer'])
  } else {
    tokens[idx].attrs[relIndex][1] = 'noopener noreferrer'
  }

  return defaultLinkRender(tokens, idx, options, env, renderer)
}

export function renderMarkdown(content) {
  if (!content || typeof content !== 'string') {
    return ''
  }

  try {
    return sanitizeHtml(md.render(content))
  } catch (error) {
    console.error('Markdown渲染失败:', error)
    return sanitizeHtml(md.utils.escapeHtml(content).replace(/\n/g, '<br>'))
  }
}

export default {
  renderMarkdown,
  sanitizeHtml
}
