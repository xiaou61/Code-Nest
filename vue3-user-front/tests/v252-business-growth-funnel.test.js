import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const projectRoot = resolve(import.meta.dirname, '..')

function source(...segments) {
  return readFileSync(resolve(projectRoot, ...segments), 'utf8')
}

test('home consumes the global briefing and records an idempotent primary-action funnel', () => {
  const home = source('src', 'views', 'HomeRevamp.vue')
  const api = source('src', 'api', 'growthCoach.js')
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

  assert.match(homeService, /GrowthCoachBriefingService/)
  assert.match(homeService, /growthCoachBriefingService\.getForUser/)
  assert.doesNotMatch(homeService, /growthCoachApplicationService\.getTodayAction/)
  assert.match(api, /\/user\/growth-coach\/journey-events/)
  assert.match(home, /PRIMARY_ACTION_SHOWN/)
  assert.match(home, /PRIMARY_ACTION_STARTED/)
  assert.match(home, /growthAction: todayAction\.value\.actionType/)
  assert.doesNotMatch(home, /prefillMessage: todayAction\.value\.prefillMessage/)
})

test('career applications expose and submit the three owned preparation sources', () => {
  const view = source('src', 'views', 'career-loop', 'Index.vue')
  const request = source(
    '..',
    'xiaou-mock-interview',
    'src',
    'main',
    'java',
    'com',
    'xiaou',
    'mockinterview',
    'dto',
    'request',
    'CareerApplicationUpsertRequest.java'
  )
  const service = source(
    '..',
    'xiaou-mock-interview',
    'src',
    'main',
    'java',
    'com',
    'xiaou',
    'mockinterview',
    'service',
    'impl',
    'CareerApplicationServiceImpl.java'
  )

  for (const field of ['matchRecordId', 'planRecordId', 'mockInterviewSessionId']) {
    assert.match(view, new RegExp(field))
    assert.match(request, new RegExp(field))
  }
  assert.match(view, /getMatchEngineHistory/)
  assert.match(view, /getPlanHistory/)
  assert.match(view, /mockInterviewApi\.getHistory/)
  assert.match(service, /validateSourceOwnership/)
  assert.match(service, /selectByIdAndUserId/)
})

test('v2.5.2 migration defines the unique journey event and application source links', () => {
  const migration = source('..', 'sql', 'v2.5.2', 'business_growth_funnel.sql')

  assert.match(migration, /CREATE TABLE IF NOT EXISTS `growth_journey_event`/)
  assert.match(migration, /uk_growth_journey_user_event_tracking/)
  assert.match(migration, /ADD COLUMN `match_record_id`/)
  assert.match(migration, /ADD COLUMN `plan_record_id`/)
  assert.match(migration, /ADD COLUMN `mock_interview_session_id`/)
})

test('growth journey contract carries versioned client context and bounded event values', () => {
  const api = source('src', 'api', 'growthCoach.js')
  const request = source(
    '..',
    'xiaou-application',
    'src',
    'main',
    'java',
    'com',
    'xiaou',
    'web',
    'growthcoach',
    'dto',
    'GrowthJourneyEventRequest.java'
  )
  const catalog = source(
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
    'GrowthJourneyEventCatalog.java'
  )

  assert.match(api, /schemaVersion/)
  assert.match(api, /clientVersion/)
  assert.match(api, /entryPage/)
  assert.match(request, /PRIMARY_ACTION_COMPLETED/)
  assert.match(request, /OUTCOME_RECORDED/)
  assert.match(catalog, /ACTION_TYPES/)
  assert.match(catalog, /SOURCES/)
})
