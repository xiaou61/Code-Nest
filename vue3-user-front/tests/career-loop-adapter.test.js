import test from 'node:test'
import assert from 'node:assert/strict'

import {
  adaptCareerLoopCurrent,
  mapActionStatusLabel,
  mapActionStatusTag,
  mapStageLabel,
  mapStagePercent
} from '../src/utils/career-loop-adapter.js'

test('mapStageLabel should map PLAN_READY correctly', () => {
  assert.equal(mapStageLabel('PLAN_READY'), '计划已生成')
})

test('mapStagePercent should map REVIEWED to 100', () => {
  assert.equal(mapStagePercent('REVIEWED'), 100)
})

test('mapActionStatus should map done to success/已完成', () => {
  assert.equal(mapActionStatusTag('done'), 'success')
  assert.equal(mapActionStatusLabel('done'), '已完成')
})

test('adaptCareerLoopCurrent should normalize numeric fields', () => {
  const result = adaptCareerLoopCurrent({
    session: { currentStage: 'PLAN_EXECUTING', healthScore: '88' },
    snapshot: { planProgress: '30', mockCount: '2', latestMockScore: '76', reviewCount: '1' }
  })

  assert.equal(result.session.currentStageLabel, '计划执行中')
  assert.equal(result.session.stagePercent, 67)
  assert.equal(result.session.healthScore, 88)
  assert.equal(result.snapshot.planProgress, 30)
  assert.equal(result.snapshot.mockCount, 2)
  assert.equal(result.snapshot.latestMockScore, 76)
  assert.equal(result.snapshot.reviewCount, 1)
})

