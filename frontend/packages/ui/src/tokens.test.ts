import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs'
import path from 'node:path'

import { describe, expect, it } from 'vitest'

/**
 * 令牌单一来源检查。
 *
 * <p>颜色值只允许出现在 `packages/ui/src/styles/globals.css`。应用里一旦出现硬编码颜色，
 * 深浅两套主题里必有一套是错的——而那种错误通常要等用户在深色模式下打开页面才发现。
 *
 * <p>覆盖范围是全部前端应用与共享包，不只是"看起来像 UI"的那几个文件：颜色泄漏最容易
 * 发生在某个临时页面或占位组件里，而不是主流程上。
 */

const FORBIDDEN: { name: string; pattern: RegExp }[] = [
  { name: '十六进制颜色', pattern: /#[0-9a-fA-F]{3,8}\b/g },
  { name: '原始颜色函数', pattern: /\b(?:oklch|oklab|rgba?|hsla?|lch|lab)\(\s*[0-9.]/g },
  {
    name: '调色板颜色类',
    pattern:
      /(?:^|[\s"'`])(?:bg|text|border|ring|fill|stroke|from|via|to|outline|divide|placeholder|decoration|caret|shadow)-(?:black|white|slate|gray|zinc|neutral|stone|red|orange|amber|yellow|lime|green|emerald|teal|cyan|sky|blue|indigo|violet|purple|fuchsia|pink|rose)(?:\/\d+)?/g,
  },
]

/** 令牌定义文件本身与测试代码不受这条规则约束。 */
function isExempt(relative: string): boolean {
  return (
    relative.includes(`${path.sep}styles${path.sep}`) ||
    /\.test\.(?:ts|tsx)$/.test(relative) ||
    relative.endsWith('node_modules')
  )
}

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

function scannedRoots(root: string): string[] {
  const roots = [path.join(root, 'packages', 'ui', 'src')]
  const appsDirectory = path.join(root, 'apps')
  for (const entry of readdirSync(appsDirectory, { withFileTypes: true })) {
    if (entry.isDirectory()) {
      const source = path.join(appsDirectory, entry.name, 'src')
      if (existsSync(source) && statSync(source).isDirectory()) {
        roots.push(source)
      }
    }
  }
  return roots
}

describe('令牌单一来源', () => {
  it('应用与共享包里没有硬编码颜色', () => {
    const root = workspaceRoot()
    const offenders: string[] = []
    let scanned = 0

    for (const directory of scannedRoots(root)) {
      for (const file of sourceFiles(directory)) {
        const relative = path.relative(root, file)
        if (isExempt(relative)) {
          continue
        }
        scanned += 1
        const content = readFileSync(file, 'utf8')
        for (const { name, pattern } of FORBIDDEN) {
          for (const match of content.matchAll(pattern)) {
            offenders.push(`${relative} 出现${name}：${match[0].trim()}`)
          }
        }
      }
    }

    // 扫描范围自身也要有下限，否则目录改名会让这条检查变成"扫了 0 个文件、通过"
    expect(scanned, '扫描到的源文件数量异常，检查可能没有真正生效').toBeGreaterThan(10)
    expect(offenders, '颜色必须来自令牌，请看 packages/ui/src/styles/globals.css').toEqual([])
  })
})
