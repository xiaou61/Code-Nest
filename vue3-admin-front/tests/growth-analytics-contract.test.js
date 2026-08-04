import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const projectRoot = resolve(import.meta.dirname, '..')

function source(...segments) {
  return readFileSync(resolve(projectRoot, ...segments), 'utf8')
}

test('admin dashboard exposes the persisted growth analytics overview', () => {
  const api = source('src', 'api', 'dashboard.js')
  const dashboard = source('src', 'views', 'dashboard', 'index.vue')
  const controller = source(
    '..',
    'xiaou-application',
    'src',
    'main',
    'java',
    'com',
    'xiaou',
    'web',
    'growthcoach',
    'controller',
    'AdminGrowthAnalyticsController.java'
  )

  assert.match(api, /\/admin\/growth-analytics\/overview/)
  assert.match(dashboard, /Growth Analytics/)
  assert.match(dashboard, /verifiedGrowthUsers/)
  assert.match(dashboard, /primaryActionStartRate/)
  assert.match(dashboard, /planAdoptionRate/)
  assert.match(controller, /@RequireAdmin/)
  assert.match(controller, /@GetMapping\("\/overview"\)/)
})
