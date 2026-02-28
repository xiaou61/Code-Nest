import test from 'node:test'
import assert from 'node:assert/strict'

import {
  adaptContestItem,
  adaptContestList,
  adaptContestRanking
} from '../src/utils/oj-contest-adapter.js'

test('adaptContestItem 兼容字段并规范化类型', () => {
  const contest = adaptContestItem({
    contestId: 7,
    name: '周末冲刺赛',
    type: 2,
    status: '2',
    beginTime: '2026-02-23T20:00:00',
    finishTime: '2026-02-23T22:00:00',
    joinCount: 88
  })

  assert.equal(contest.id, 7)
  assert.equal(contest.title, '周末冲刺赛')
  assert.equal(contest.contestType, 'challenge')
  assert.equal(contest.status, 2)
  assert.equal(contest.startTime, '2026-02-23 20:00:00')
  assert.equal(contest.endTime, '2026-02-23 22:00:00')
  assert.equal(contest.participantCount, 88)
})

test('adaptContestList 兼容 records/list 和 total', () => {
  const result = adaptContestList({
    list: [
      { id: 11, title: '第一场', contestType: 'weekly', status: 1 },
      { id: 12, title: '第二场', contestType: 'challenge', status: 2 }
    ],
    total: 18
  })

  assert.equal(result.total, 18)
  assert.equal(result.records.length, 2)
  assert.equal(result.records[0].id, 11)
  assert.equal(result.records[1].contestType, 'challenge')
})

test('adaptContestRanking 按 solved/penalty/lastAcTime 排序并输出展示字段', () => {
  const rows = adaptContestRanking([
    { userId: 2, nickname: 'B', solvedCount: 2, penalty: 140, lastAcTime: '2026-02-23 10:20:00' },
    { userId: 1, nickname: 'A', solvedCount: 2, penalty: 120, lastAcTime: '2026-02-23 10:30:00' },
    { userId: 3, nickname: 'C', solvedCount: 1, penalty: 80, lastAcTime: '2026-02-23 09:30:00' }
  ])

  assert.equal(rows[0].userId, 1)
  assert.equal(rows[0].rank, 1)
  assert.equal(rows[0].solvedText, '2 题')
  assert.equal(rows[0].penaltyText, '120 分钟')

  assert.equal(rows[1].userId, 2)
  assert.equal(rows[1].rank, 2)
  assert.equal(rows[2].rank, 3)
})

test('adaptContestRanking 对并列保留相同名次', () => {
  const rows = adaptContestRanking([
    { userId: 10, solvedCount: 3, penalty: 180, lastAcTime: '2026-02-23 11:00:00' },
    { userId: 11, solvedCount: 3, penalty: 180, lastAcTime: '2026-02-23 11:00:00' },
    { userId: 12, solvedCount: 2, penalty: 120, lastAcTime: '2026-02-23 10:00:00' }
  ])

  assert.equal(rows[0].rank, 1)
  assert.equal(rows[1].rank, 1)
  assert.equal(rows[2].rank, 3)
})
