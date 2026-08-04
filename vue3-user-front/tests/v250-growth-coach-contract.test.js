import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const projectRoot = resolve(import.meta.dirname, '..')

function source(...segments) {
  return readFileSync(resolve(projectRoot, ...segments), 'utf8')
}

test('growth coach client and controller expose preview-confirm endpoints without a chat endpoint', () => {
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

  assert.match(api, /\/user\/growth-coach\/plan-adjustments\/preview/)
  assert.match(api, /\/user\/growth-coach\/evidence/)
  assert.match(api, /confirmPlanAdjustment/)
  assert.match(api, /cancelPlanAdjustment/)
  assert.match(controller, /@PostMapping\("\/plan-adjustments\/preview"\)/)
  assert.match(controller, /@GetMapping\("\/evidence"\)/)
  assert.match(controller, /@PostMapping\("\/plan-adjustments\/\{runId\}\/confirm"\)/)
  assert.doesNotMatch(api, /\/user\/ai\/chat/)
})

test('growth autopilot keeps the AI interaction in a preview-confirm dialog', () => {
  const panel = source('src', 'views', 'growth-autopilot', 'GrowthAutopilotPanel.vue')

  assert.match(panel, /title="调整本周计划"/)
  assert.match(panel, /growthCoachApi\.previewPlanAdjustment/)
  assert.match(panel, /growthCoachApi\.confirmPlanAdjustment/)
  assert.match(panel, /previewHash/)
  assert.doesNotMatch(panel, /\/user\/ai\/chat/)
})

test('home presents one growth coach action from the overview contract', () => {
  const home = source('src', 'views', 'HomeRevamp.vue')
  const homeService = source(
    '..',
    'xiaou-application',
    'src',
    'main',
    'java',
    'com',
    'xiaou',
    'web',
    'home',
    'service',
    'UserHomeOverviewService.java'
  )

  assert.match(homeService, /response\.setTodayAction/)
  assert.match(home, /todayAction\.available/)
  assert.match(home, /todayAction\.reason/)
  assert.match(home, /todayAction(?:\.value)?\.startRoute/)
  assert.match(home, /todayAction\.evidenceRefs/)
  assert.doesNotMatch(home, /today-secondary-action/)
})
