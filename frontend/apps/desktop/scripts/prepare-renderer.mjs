/**
 * 把 SPA 产物复制到桌面应用目录内。
 *
 * 打包器只会打包应用目录下的文件，而 SPA 产物在 ../../app/dist，
 * 因此先拷成同级的 renderer/ 再打包。用脚本而不是构建插件，是为了少一个依赖。
 */
import { cp, mkdir, rm } from 'node:fs/promises'
import { existsSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const here = path.dirname(fileURLToPath(import.meta.url))
const source = path.resolve(here, '..', '..', 'app', 'dist')
const target = path.resolve(here, '..', 'renderer')

if (!existsSync(path.join(source, 'index.html'))) {
  throw new Error(`找不到 SPA 产物：${source}。请先执行 pnpm --filter @paideia/app build`)
}

await rm(target, { recursive: true, force: true })
await mkdir(target, { recursive: true })
await cp(source, target, { recursive: true })
console.log(`渲染产物已就位：${target}`)
