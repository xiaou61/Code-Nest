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

function extractRouterPaths(source) {
  return new Set(
    [...source.matchAll(/\bpath:\s*'([^']+)'/g)]
      .map((match) => match[1])
      .filter((path) => path.startsWith('/'))
      .map(normalizePath)
  )
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

function extractDemoActivePaths(source) {
  return [...source.matchAll(/\bactive-(?:full-)?path="([^"]+)"/g)]
    .map((match) => match[1])
    .filter((path) => path.startsWith('/'))
    .map(normalizePath)
}

test('user design-system demo navigation paths should exist in the router table', () => {
  const routerPaths = extractRouterPaths(routerSource)
  const demoPaths = [
    ...extractDemoPaths(componentsSource),
    ...extractDemoActivePaths(componentsSource)
  ]
  const missingPaths = [...new Set(demoPaths)]
    .filter((path) => !routerPaths.has(path))
    .sort()

  assert.deepEqual(missingPaths, [])
})
