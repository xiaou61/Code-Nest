import { readdirSync, readFileSync, statSync } from 'node:fs'
import path from 'node:path'
import { describe, expect, it } from 'vitest'

/**
 * 共享包的边界检查。
 *
 * <p>两条约束，都是"一套代码同时跑 Web 与桌面、且组件层可复用"的地基：
 *
 * <p>1. 共享包不得引用桌面壳专有 API（Electron / Tauri）。一旦引用，那份代码就只能在
 * 桌面端跑，Web 端会直接崩。
 *
 * <p>2. `packages/ui` 是纯展示层，除桌面壳 API 外还不得引用 `@paideia/core`、
 * `platform-web/desktop` 与 `react-router`。它一旦认识领域状态或路由，就不再是能在任意
 * 宿主里复用的展示层，而变成"必须带着某个特定应用才能用"的东西——设计系统与业务代码
 * 的耦合正是从这种"就 import 一下"开始的。
 *
 * <p>检查放在 core 里只是因为这里已经有测试运行器，覆盖范围是整个工作区的共享包。
 */

const FORBIDDEN_IMPORTS: Record<string, string[]> = {
  core: ['electron', '@tauri-apps/'],
  ui: [
    'electron',
    '@tauri-apps/',
    '@paideia/core',
    '@paideia/platform-web',
    '@paideia/platform-desktop',
    'react-router',
  ],
}

const DESKTOP_ONLY = new Set(['electron', '@tauri-apps/'])

/** 去掉注释再扫：否则注释里提到"不要 import 'electron'"会被算成违规。 */
function stripComments(source: string): string {
  return source.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/[^\n]*/g, '')
}

/**
 * 匹配导入语句里的依赖名。三个分支缺一不可：
 * `from 'x'`（具名导入）、`import 'x'`（纯副作用导入）、`import('x')`（动态导入）。
 *
 * <p>**纯副作用导入这一分支是补上的**：原版只认 `from` 与 `require(`，
 * 于是 `import 'electron'` 这种写法能整个绕过检查——而它恰恰是"引入一个模块只为副作用"
 * 的常见写法。用探针复核时发现的。
 */
function importPattern(dependency: string): RegExp {
  return new RegExp(`(?:from|import\\s*\\(?|require\\s*\\()\\s*['"]${dependency}`, 'g')
}

function workspaceRoot(): string {
  let directory = path.resolve(process.cwd())
  for (let depth = 0; depth < 6; depth += 1) {
    try {
      statSync(path.join(directory, 'pnpm-workspace.yaml'))
      return directory
    } catch {
      directory = path.dirname(directory)
    }
  }
  throw new Error('向上找不到 pnpm-workspace.yaml，无法定位工作区根目录')
}

function sourceFiles(directory: string): string[] {
  const found: string[] = []
  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    if (entry.name === 'node_modules' || entry.name === 'dist') {
      continue
    }
    const full = path.join(directory, entry.name)
    if (entry.isDirectory()) {
      found.push(...sourceFiles(full))
    } else if (/\.(ts|tsx)$/.test(entry.name)) {
      found.push(full)
    }
  }
  return found
}

describe('共享包边界', () => {
  it.each(Object.entries(FORBIDDEN_IMPORTS))(
    'packages/%s 不引用被禁止的依赖',
    (name, forbidden) => {
      const root = workspaceRoot()
      const files = sourceFiles(path.join(root, 'packages', name, 'src'))
      const offenders: string[] = []

      for (const file of files) {
        const content = stripComments(readFileSync(file, 'utf8'))
        for (const dependency of forbidden) {
          if (importPattern(dependency).test(content)) {
            offenders.push(`${path.relative(root, file)} → ${dependency}`)
          }
        }
      }

      const reason = forbidden.every((item) => DESKTOP_ONLY.has(item))
        ? '共享包出现了桌面壳专有引用'
        : '共享包引用了不该认识的依赖'
      expect(offenders, reason).toEqual([])
    },
  )
})
