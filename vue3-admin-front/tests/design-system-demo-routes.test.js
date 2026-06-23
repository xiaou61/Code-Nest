import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const projectRoot = resolve(import.meta.dirname, '..')
const componentsSource = readFileSync(
  resolve(projectRoot, 'src/views/design-system/Components.vue'),
  'utf8'
)
const routerSource = readFileSync(resolve(projectRoot, 'src/router/index.js'), 'utf8')

function normalizePath(path) {
  const pathOnly = path.split(/[?#]/)[0]
  if (pathOnly === '/') return '/'
  return pathOnly.replace(/\/$/, '')
}

function extractRouteEntries(source) {
  return [...source.matchAll(/\bpath:\s*'([^']+)'/g)].map((match) => match[1])
}

function extractRouterPaths(source) {
  const routeEntries = extractRouteEntries(source)
  const absolutePaths = new Set()

  for (let index = 0; index < routeEntries.length; index += 1) {
    const path = routeEntries[index]
    if (path.startsWith('/')) {
      absolutePaths.add(normalizePath(path))
      continue
    }

    for (let parentIndex = index - 1; parentIndex >= 0; parentIndex -= 1) {
      const parentPath = routeEntries[parentIndex]
      if (parentPath.startsWith('/')) {
        absolutePaths.add(normalizePath(`${parentPath.replace(/\/$/, '')}/${path}`))
        break
      }
    }
  }

  return absolutePaths
}

function extractDemoPaths(source) {
  const setupStart = source.indexOf('<script setup')
  const setupEnd = source.indexOf('</script>', setupStart)
  assert.notEqual(setupStart, -1)
  assert.notEqual(setupEnd, -1)

  return [...source.slice(setupStart, setupEnd).matchAll(/\bpath:\s*'([^']+)'/g)]
    .map((match) => match[1])
    .filter((path) => path.startsWith('/'))
    .map(normalizePath)
}

test('admin design-system demo navigation paths should exist in the router table', () => {
  const routerPaths = extractRouterPaths(routerSource)
  const missingPaths = [...new Set(extractDemoPaths(componentsSource))]
    .filter((path) => !routerPaths.has(path))
    .sort()

  assert.deepEqual(missingPaths, [])
})
