import { existsSync, readFileSync, readdirSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const docsRoot = path.resolve(__dirname, '..')
const configPath = path.resolve(docsRoot, '.vitepress', 'config.mts')

function collectMarkdown(directory) {
  const files = []
  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    if (entry.name === 'node_modules' || entry.name === '.vitepress') {
      continue
    }
    const fullPath = path.join(directory, entry.name)
    if (entry.isDirectory()) {
      files.push(...collectMarkdown(fullPath))
    } else if (entry.isFile() && entry.name.endsWith('.md')) {
      files.push(fullPath)
    }
  }
  return files
}

function routeFor(filePath) {
  const relative = path.relative(docsRoot, filePath).replaceAll('\\', '/')
  if (relative === 'index.md') {
    return '/'
  }
  if (relative.endsWith('/index.md')) {
    return `/${relative.slice(0, -'/index.md'.length)}`
  }
  return `/${relative.slice(0, -'.md'.length)}`
}

const errors = []
const warnings = []
const markdownFiles = collectMarkdown(docsRoot)
const routes = new Set(markdownFiles.map(routeFor))
const config = readFileSync(configPath, 'utf8')
const configuredLinks = new Set(
  [...config.matchAll(/link:\s*'([^']+)'/g)]
    .map(match => match[1].split('#')[0].replace(/\/$/, '') || '/')
)

for (const route of routes) {
  if (route === '/' || route === '/404') {
    continue
  }
  if (!configuredLinks.has(route)) {
    errors.push(`页面未加入 VitePress 导航: ${route}`)
  }
}

const requiredIndexes = [
  'guide/index.md',
  'architecture/index.md',
  'modules/index.md',
  'api/index.md',
  'operations/index.md',
  'manuals/index.md',
  'reference/index.md',
  'roadmap/index.md'
]

for (const relativePath of requiredIndexes) {
  if (!existsSync(path.resolve(docsRoot, relativePath))) {
    errors.push(`缺少分类总览页: ${relativePath}`)
  }
}

const staleFacts = [
  { pattern: /v2\.3\.1 项目文档/, message: '首页仍使用 v2.3.1 标题' },
  { pattern: /24 个[^。\n]{0,24}Maven 子模块/, message: '仍存在旧的 24 模块统计' },
  { pattern: /142 张表/, message: '仍存在旧的 142 张表统计' },
  { pattern: /STOMP 协议|STOMP \+ WebSocket|WebSocket \+ STOMP/, message: '聊天室仍被错误描述为 STOMP' },
  { pattern: /Spring AI \+ LangGraph4j|BeanOutputConverter/, message: 'AI Runtime 仍被描述为旧 Spring AI 实现' }
]

for (const filePath of markdownFiles) {
  const content = readFileSync(filePath, 'utf8')
  const relative = path.relative(docsRoot, filePath).replaceAll('\\', '/')
  for (const stale of staleFacts) {
    if (stale.pattern.test(content)) {
      errors.push(`${stale.message}: ${relative}`)
    }
  }
  const lines = content.split(/\r?\n/).length
  if (lines > 1200) {
    warnings.push(`超长页面 (${lines} 行): ${relative}`)
  }
}

if (!config.includes('Code Nest v2.5.0 工程文档中心')) {
  errors.push('VitePress description 未标记 v2.5.0 文档中心')
}

console.log(`[docs-audit] pages=${markdownFiles.length}, configuredLinks=${configuredLinks.size}`)
for (const warning of warnings) {
  console.warn(`[docs-audit] warning: ${warning}`)
}
for (const error of errors) {
  console.error(`[docs-audit] error: ${error}`)
}

if (errors.length > 0) {
  process.exit(1)
}

console.log('[docs-audit] passed')
