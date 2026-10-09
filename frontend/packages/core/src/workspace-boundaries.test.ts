import { readdirSync, readFileSync, statSync } from 'node:fs'
import path from 'node:path'
import { describe, expect, it } from 'vitest'

/**
 * 共享包不得引用桌面壳专有 API。
 *
 * <p>这条约束是 Web 与桌面能共用同一份业务代码的前提：一旦 packages/core 或 packages/ui
 * 直接 import 了 Electron 或 Tauri 的东西，那份代码就只能在桌面端跑，Web 端会直接崩。
 * 检查放在 core 里只是因为这里已经有测试运行器，覆盖范围是整个工作区的共享包。
 */

const SHARED_PACKAGES = ['core', 'ui']
const DESKTOP_ONLY_IMPORTS = ['electron', '@tauri-apps/']

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
  it.each(SHARED_PACKAGES)('packages/%s 不引用任何桌面壳专有 API', (name) => {
    const root = workspaceRoot()
    const files = sourceFiles(path.join(root, 'packages', name, 'src'))
    const offenders: string[] = []

    for (const file of files) {
      const content = readFileSync(file, 'utf8')
      for (const forbidden of DESKTOP_ONLY_IMPORTS) {
        // 只看 import/require 语句，避免把注释或字符串里提到的名字也算成违规
        const pattern = new RegExp(`(?:from|require\\()\\s*['"]${forbidden}`, 'g')
        if (pattern.test(content)) {
          offenders.push(`${path.relative(root, file)} → ${forbidden}`)
        }
      }
    }

    expect(offenders, '共享包出现了桌面壳专有引用').toEqual([])
  })
})
