import test from 'node:test'
import assert from 'node:assert/strict'
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { resolve, relative } from 'node:path'

const projectRoot = resolve(import.meta.dirname, '..')
const srcRoot = resolve(projectRoot, 'src')
const routerSource = readFileSync(resolve(srcRoot, 'router/index.js'), 'utf8')
const navigationSource = readFileSync(resolve(srcRoot, 'config/navigation.js'), 'utf8')

function collectSourceFiles(dir) {
  return readdirSync(dir).flatMap((entry) => {
    const fullPath = resolve(dir, entry)
    const stats = statSync(fullPath)

    if (stats.isDirectory()) {
      return collectSourceFiles(fullPath)
    }

    return /\.(vue|js|ts)$/.test(entry) ? [fullPath] : []
  })
}

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

function extractNavigationConfigPaths(source) {
  return [...source.matchAll(/\bpath:\s*'([^']+)'/g)]
    .map((match) => match[1])
    .filter((path) => path.startsWith('/'))
}

function extractLiteralRouterPushes(filePath) {
  const source = readFileSync(filePath, 'utf8')
  const relativePath = relative(projectRoot, filePath).replaceAll('\\', '/')
  const matches = [
    ...source.matchAll(/\b(?:router|\$router)\.(?:push|replace)\(\s*['"]([^'"]+)['"]/g),
    ...source.matchAll(/\bnavigate\(\s*['"]([^'"]+)['"]/g)
  ]

  return matches
    .map((match) => match[1])
    .filter((path) => path.startsWith('/'))
    .map((path) => ({ path, source: relativePath }))
}

test('user static navigation paths should exist in the router table', () => {
  const routerPaths = extractRouterPaths(routerSource)
  const candidates = [
    ...extractNavigationConfigPaths(navigationSource).map((path) => ({
      path,
      source: 'src/config/navigation.js'
    })),
    ...collectSourceFiles(srcRoot).flatMap(extractLiteralRouterPushes)
  ]

  const missingPaths = candidates
    .filter(({ path }) => !routerPaths.has(normalizePath(path)))
    .map(({ path, source }) => `${source} -> ${path}`)
    .sort()

  assert.deepEqual(missingPaths, [])
})
