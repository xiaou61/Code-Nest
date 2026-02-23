import test from 'node:test'
import assert from 'node:assert/strict'

import {
  adaptPlanStats,
  adaptDailyProblem,
  adaptHeroMetrics,
  adaptHotFeed,
  adaptVersionFeed
} from '../src/utils/home-data-adapter.js'

test('adaptPlanStats 支持后端新字段并计算今日完成率', () => {
  const result = adaptPlanStats({
    activePlanCount: 5,
    totalCheckinCount: 128,
    todayCompletedCount: 3,
    todayPendingCount: 1
  })

  assert.equal(result.activeCount, 5)
  assert.equal(result.totalCheckins, 128)
  assert.equal(result.todayCompleted, 3)
  assert.equal(result.todayPending, 1)
  assert.equal(result.todayCompletionRate, 75)
})

test('adaptPlanStats 兼容旧字段命名', () => {
  const result = adaptPlanStats({
    totalPlans: 4,
    totalCheckins: 90,
    todayCheckins: 2,
    todayPending: 2
  })

  assert.equal(result.activeCount, 4)
  assert.equal(result.totalCheckins, 90)
  assert.equal(result.todayCompleted, 2)
  assert.equal(result.todayPending, 2)
  assert.equal(result.todayCompletionRate, 50)
})

test('adaptDailyProblem 计算通过率并抽取标签', () => {
  const result = adaptDailyProblem({
    id: 2001,
    title: '两数之和',
    difficulty: 'easy',
    acceptedCount: 25,
    submitCount: 50,
    tags: [{ tagName: '数组' }, { tagName: '哈希表' }]
  })

  assert.equal(result.id, 2001)
  assert.equal(result.title, '两数之和')
  assert.equal(result.acceptanceRate, 50)
  assert.deepEqual(result.tags, ['数组', '哈希表'])
})

test('adaptHeroMetrics 聚合首屏核心数字', () => {
  const result = adaptHeroMetrics({
    learnedTotal: 320,
    knowledgeTotal: 12,
    onlineCount: 86,
    hotPosts: [{ id: 1 }, { id: 2 }, { id: 3 }],
    hotMoments: [{ id: 11 }, { id: 12 }],
    planStats: {
      todayCompletedCount: 3,
      todayPendingCount: 1
    }
  })

  assert.equal(result.learnedCount, 320)
  assert.equal(result.knowledgeCount, 12)
  assert.equal(result.onlineCount, 86)
  assert.equal(result.hotTopicCount, 5)
  assert.equal(result.todayTaskCompletionRate, 75)
})

test('adaptHotFeed 输出统一卡片模型并限制数量', () => {
  const result = adaptHotFeed({
    posts: [
      { id: 1, title: 'A', likeCount: 8, commentCount: 2 },
      { id: 2, title: 'B', likeCount: 6, commentCount: 1 },
      { id: 3, title: 'C', likeCount: 5, commentCount: 1 }
    ],
    moments: [
      { id: 11, content: 'M1', likeCount: 9, commentCount: 3 },
      { id: 12, content: 'M2', likeCount: 7, commentCount: 2 },
      { id: 13, content: 'M3', likeCount: 4, commentCount: 1 }
    ],
    limitPerType: 2
  })

  assert.equal(result.posts.length, 2)
  assert.equal(result.moments.length, 2)
  assert.equal(result.posts[0].routePath, '/community/posts/1')
  assert.equal(result.moments[0].routePath, '/moments')
})

test('adaptVersionFeed 标准化版本播报条目', () => {
  const result = adaptVersionFeed([
    {
      id: 1,
      versionNumber: 'v1.8.0',
      title: '首页改版',
      updateTypeName: '功能更新',
      releaseTime: '2026-02-20 10:00:00',
      description: '优化首页体验'
    }
  ])

  assert.equal(result.length, 1)
  assert.equal(result[0].version, 'v1.8.0')
  assert.equal(result[0].title, '首页改版')
  assert.equal(result[0].typeName, '功能更新')
  assert.equal(result[0].dateText, '2026-02-20')
})
