import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs'
import path from 'node:path'

import { describe, expect, it } from 'vitest'

/**
 * 扫描路径覆盖检查。
 *
 * <p>`globals.css` 用 `@import "tailwindcss" source(none)` 关掉了自动探测，改为显式登记
 * 每个含类名的源码目录（原因见该文件顶部注释：pnpm 把 workspace 包软链在 node_modules 下，
 * 自动探测会跳过它）。
 *
 * <p><b>漏登记的失败是静默的</b>：凡是别处也用过的类名仍然会生成，只有本包独有的那些
 * 会消失——表现为"布局类不生效但没有任何报错"。这个坑已经踩过一次（新加的
 * `packages/auth` 没登记，`justify-end` 这类只在那儿出现的类名一直没生成）。
 * 所以这里把它变成会失败的检查，而不是靠人记住。
 */

const CSS_PATH = path.join(process.cwd(), 'src', 'styles', 'globals.css')

function workspaceRoot(): string {
  let directory = path.resolve(process.cwd())
  for (let depth = 0; depth < 6; depth += 1) {
    if (existsSync(path.join(directory, 'pnpm-workspace.yaml'))) {
      return directory
    }
    directory = path.dirname(directory)
  }
  throw new Error('向上找不到 pnpm-workspace.yaml，无法定位工作区根目录')
}

/** 有渲染组件（.tsx）的源码目录才需要被扫描；纯 TS 的包不写类名。 */
function sourceRootsWithComponents(root: string): string[] {
  const roots: string[] = []
  for (const group of ['packages', 'apps']) {
    const groupDir = path.join(root, group)
    if (!existsSync(groupDir)) {
      continue
    }
    for (const entry of readdirSync(groupDir, { withFileTypes: true })) {
      if (!entry.isDirectory()) {
        continue
      }
      const source = path.join(groupDir, entry.name, 'src')
      if (existsSync(source) && statSync(source).isDirectory() && hasTsx(source)) {
        roots.push(source)
      }
    }
  }
  return roots
}

function hasTsx(directory: string): boolean {
  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    if (entry.name === 'node_modules' || entry.name === 'dist') {
      continue
    }
    const full = path.join(directory, entry.name)
    if (entry.isDirectory() && hasTsx(full)) {
      return true
    }
    if (entry.isFile() && entry.name.endsWith('.tsx')) {
      return true
    }
  }
  return false
}

/** 把 @source 的写法（可能是 glob）折算成它覆盖的起点目录。 */
function coveredRoot(cssDirectory: string, entry: string): string {
  const withoutGlob = entry.replace(/\*\*.*$/, '').replace(/\*.*$/, '')
  return path.resolve(cssDirectory, withoutGlob.replace(/[/\\]+$/, ''))
}

describe('Tailwind 扫描路径覆盖', () => {
  it('每个含组件的源码目录都在 globals.css 的 @source 里', () => {
    const root = workspaceRoot()
    const cssPath = CSS_PATH
    const css = readFileSync(cssPath, 'utf8')
    const cssDirectory = path.dirname(cssPath)

    const entries = [...css.matchAll(/@source\s+'([^']+)'/g)].map((match) => match[1] ?? '')
    expect(entries.length, 'globals.css 里没有任何 @source，检查失去意义').toBeGreaterThan(0)
    const covered = entries.map((entry) => coveredRoot(cssDirectory, entry))

    const uncovered = sourceRootsWithComponents(root).filter((source) => {
      const normalized = path.resolve(source)
      return !covered.some(
        (base) => normalized === base || normalized.startsWith(base + path.sep),
      )
    })

    expect(
      uncovered.map((item) => path.relative(root, item)),
      '这些目录含 .tsx 但没被 globals.css 的 @source 登记：它们独有的 Tailwind 类名会静默不生成',
    ).toEqual([])
  })
})
