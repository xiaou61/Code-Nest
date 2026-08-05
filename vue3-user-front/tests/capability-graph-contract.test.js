import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { readRouterSource } from './helpers/router-source.js'

const projectRoot = resolve(import.meta.dirname, '..')

function source(...segments) {
  return readFileSync(resolve(projectRoot, ...segments), 'utf8')
}

test('capability graph exposes a read-only evidence-backed user contract', () => {
  const api = source('src', 'api', 'growthCoach.js')
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
    'UserGrowthCoachController.java'
  )
  const service = source(
    '..',
    'xiaou-application',
    'src',
    'main',
    'java',
    'com',
    'xiaou',
    'web',
    'growthcoach',
    'service',
    'GrowthCapabilityGraphService.java'
  )

  assert.match(api, /getCapabilityGraph\(\)/)
  assert.match(api, /\/user\/growth-coach\/capability-graph/)
  assert.match(controller, /@GetMapping\("\/capability-graph"\)/)
  assert.match(controller, /GrowthCapabilityGraphResponse/)
  assert.match(service, /只读投影/)
  assert.match(service, /GrowthEvidenceQueryService/)
  assert.match(service, /GrowthSkillInsightService/)
  assert.doesNotMatch(service, /GrowthCoachApplicationService/)
})

test('capability graph has a discoverable user route and navigation entry', () => {
  const router = readRouterSource(projectRoot)
  const navigation = source('src', 'config', 'navigation.js')
  const view = source('src', 'views', 'growth-capabilities', 'Index.vue')

  assert.match(router, /path: '\/growth-capabilities'/)
  assert.match(router, /name: 'GrowthCapabilities'/)
  assert.match(navigation, /path: '\/growth-capabilities'/)
  assert.match(navigation, /能力图谱/)
  assert.match(view, /title="成长能力图谱"/)
  assert.match(view, /graph\.nodes/)
  assert.match(view, /graph\.gaps/)
  assert.match(view, /只基于已落库成长证据计算/)
})
