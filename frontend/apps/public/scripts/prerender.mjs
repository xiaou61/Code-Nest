/**
 * 把 React 的输出写进构建好的 index.html。
 *
 * 用「客户端构建 + SSR 构建 + 注入」三小步，而不是引入预渲染插件：
 * 需要的能力只有 renderToString 一次调用，多一个插件就多一份需要跟随的配置。
 */
import { readFile, rm, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const here = path.dirname(fileURLToPath(import.meta.url))
const appDir = path.resolve(here, '..')
const distDir = path.join(appDir, 'dist')
const ssrEntry = path.join(appDir, 'dist-ssr', 'entry-server.js')
const MARKER = '<!--prerender-outlet-->'

const { render } = await import(pathToFileURL(ssrEntry).href)
const markup = render()

const indexFile = path.join(distDir, 'index.html')
const template = await readFile(indexFile, 'utf8')

if (!template.includes(MARKER)) {
  throw new Error(`index.html 里找不到标记 ${MARKER}，预渲染无处注入`)
}

await writeFile(indexFile, template.replace(MARKER, markup), 'utf8')
await rm(path.join(appDir, 'dist-ssr'), { recursive: true, force: true })

console.log(`预渲染完成：向 index.html 注入 ${markup.length} 个字符`)
