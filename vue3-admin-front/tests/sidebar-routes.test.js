import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const projectRoot = resolve(import.meta.dirname, '..')
const layoutSource = readFileSync(resolve(projectRoot, 'src/layout/index.vue'), 'utf8')
const routerSource = readFileSync(resolve(projectRoot, 'src/router/index.js'), 'utf8')

function extractSidebarPaths(source) {
  const sidebarStart = source.indexOf('const sidebarItems')
  const sidebarEnd = source.indexOf('// 智能图标推断函数')
  assert.notEqual(sidebarStart, -1)
  assert.notEqual(sidebarEnd, -1)

  const sidebarBlock = source.slice(sidebarStart, sidebarEnd)
  return [...sidebarBlock.matchAll(/\bpath:\s*'([^']+)'/g)]
    .map((match) => match[1])
    .sort()
}

function extractRouterPaths(source) {
  const routePaths = [...source.matchAll(/\bpath:\s*'([^']+)'/g)].map((match) => match[1])
  const absolutePaths = new Set()

  for (let index = 0; index < routePaths.length; index += 1) {
    const path = routePaths[index]
    if (path.startsWith('/')) {
      absolutePaths.add(path)
      continue
    }

    for (let parentIndex = index - 1; parentIndex >= 0; parentIndex -= 1) {
      const parentPath = routePaths[parentIndex]
      if (parentPath.startsWith('/')) {
        absolutePaths.add(`${parentPath.replace(/\/$/, '')}/${path}`.replace(/\/$/, ''))
        break
      }
    }
  }

  return absolutePaths
}

test('admin sidebar leaf paths should exist in the router table', () => {
  const sidebarPaths = extractSidebarPaths(layoutSource)
  const routerPaths = extractRouterPaths(routerSource)

  const missingPaths = sidebarPaths.filter((path) => !routerPaths.has(path))

  assert.deepEqual(missingPaths, [])
})
