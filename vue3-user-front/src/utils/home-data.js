import { onBeforeUnmount, reactive, ref } from 'vue'
import { interviewApi } from '@/api/interview'
import { getPublishedKnowledgeMaps } from '@/api/knowledge'
import { getOnlineCount } from '@/api/chat'
import { communityApi } from '@/api/community'
import { getHotMoments } from '@/api/moment'
import { ojApi } from '@/api/oj'
import { mockInterviewApi } from '@/api/mockInterview'
import { planApi } from '@/api/plan'
import { pointsApi } from '@/api/points'
import { versionApi } from '@/api/version'
import {
  adaptDailyProblem,
  adaptHeroMetrics,
  adaptHotFeed,
  adaptMockStats,
  adaptPlanStats,
  adaptVersionFeed
} from '@/utils/home-data-adapter'

const REFRESH_INTERVAL = 60_000

function createModuleState() {
  return {
    loading: true,
    available: false,
    message: '加载中'
  }
}

function toNumber(value, fallback = 0) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : fallback
}

function parseResult(result, fallbackValue) {
  if (result.status === 'fulfilled') {
    return {
      ok: true,
      value: result.value ?? fallbackValue
    }
  }
  return {
    ok: false,
    value: fallbackValue
  }
}

export function useHomeData() {
  const loading = ref(true)
  const refreshAt = ref('')
  let refreshTimer = null
  let visibilityHandlerBound = false
  const rawCache = reactive({
    hotPosts: [],
    hotMoments: [],
    planStats: {}
  })

  const moduleState = reactive({
    hero: createModuleState(),
    hot: createModuleState(),
    growth: createModuleState(),
    challenge: createModuleState(),
    version: createModuleState()
  })

  const homeData = reactive({
    heroMetrics: {
      learnedCount: 0,
      knowledgeCount: 0,
      onlineCount: 0,
      hotTopicCount: 0,
      todayTaskCompletionRate: 0
    },
    hotFeed: {
      posts: [],
      moments: []
    },
    growth: {
      plan: adaptPlanStats(),
      mockInterview: adaptMockStats(),
      points: {
        totalPoints: 0,
        balanceYuan: '0.00',
        continuousDays: 0,
        todayCheckedIn: false,
        todayPoints: 0
      }
    },
    challenge: {
      dailyProblem: adaptDailyProblem()
    },
    versions: []
  })

  const updateRefreshAt = () => {
    refreshAt.value = new Date().toLocaleTimeString('zh-CN', {
      hour12: false
    })
  }

  const setModuleSuccess = (moduleName) => {
    moduleState[moduleName].loading = false
    moduleState[moduleName].available = true
    moduleState[moduleName].message = ''
  }

  const setModuleError = (moduleName, message = '暂不可用') => {
    moduleState[moduleName].loading = false
    moduleState[moduleName].available = false
    moduleState[moduleName].message = message
  }

  const loadAllData = async ({ silent = false } = {}) => {
    if (!silent) {
      loading.value = true
      Object.keys(moduleState).forEach((moduleName) => {
        moduleState[moduleName].loading = true
        moduleState[moduleName].available = false
        moduleState[moduleName].message = '加载中'
      })
    }

    const [
      learnedResult,
      knowledgeResult,
      onlineResult,
      hotPostsResult,
      hotMomentsResult,
      dailyProblemResult,
      mockStatsResult,
      planStatsResult,
      pointsResult,
      versionsResult
    ] = await Promise.allSettled([
      interviewApi.getTotalLearned(),
      getPublishedKnowledgeMaps({ pageNum: 1, pageSize: 1 }),
      getOnlineCount(),
      communityApi.getHotPosts(6),
      getHotMoments({ limit: 6 }),
      ojApi.getDailyProblem(),
      mockInterviewApi.getStats(),
      planApi.getStatsOverview(),
      pointsApi.getPointsBalance(),
      versionApi.getLatestVersions(6)
    ])

    const learned = parseResult(learnedResult, 0)
    const knowledge = parseResult(knowledgeResult, { total: 0 })
    const online = parseResult(onlineResult, 0)
    const hotPosts = parseResult(hotPostsResult, [])
    const hotMoments = parseResult(hotMomentsResult, [])
    const dailyProblem = parseResult(dailyProblemResult, null)
    const mockStats = parseResult(mockStatsResult, {})
    const planStats = parseResult(planStatsResult, {})
    const points = parseResult(pointsResult, {})
    const versions = parseResult(versionsResult, [])

    rawCache.hotPosts = hotPosts.ok && Array.isArray(hotPosts.value) ? hotPosts.value : []
    rawCache.hotMoments = hotMoments.ok && Array.isArray(hotMoments.value) ? hotMoments.value : []
    rawCache.planStats = planStats.ok ? (planStats.value || {}) : {}

    homeData.growth.plan = adaptPlanStats(rawCache.planStats)
    homeData.growth.mockInterview = adaptMockStats(mockStats.value)
    homeData.growth.points = {
      totalPoints: toNumber(points.value.totalPoints),
      balanceYuan: points.value.balanceYuan || '0.00',
      continuousDays: toNumber(points.value.continuousDays),
      todayCheckedIn: Boolean(points.value.todayCheckedIn),
      todayPoints: toNumber(points.value.todayPoints)
    }

    homeData.hotFeed = adaptHotFeed({
      posts: rawCache.hotPosts,
      moments: rawCache.hotMoments,
      limitPerType: 4
    })

    homeData.challenge.dailyProblem = adaptDailyProblem(dailyProblem.value || {})
    homeData.versions = adaptVersionFeed(versions.value, 5)

    homeData.heroMetrics = adaptHeroMetrics({
      learnedTotal: learned.value,
      knowledgeTotal: knowledge.value?.total,
      onlineCount: online.value,
      hotPosts: rawCache.hotPosts,
      hotMoments: rawCache.hotMoments,
      planStats: rawCache.planStats
    })

    const heroHasAnySource = learned.ok || knowledge.ok || online.ok || planStats.ok
    const hotHasAnySource = hotPosts.ok || hotMoments.ok
    const growthHasAnySource = planStats.ok || mockStats.ok || points.ok
    const challengeHasAnySource = dailyProblem.ok || mockStats.ok
    const versionHasAnySource = versions.ok

    heroHasAnySource ? setModuleSuccess('hero') : setModuleError('hero')
    hotHasAnySource ? setModuleSuccess('hot') : setModuleError('hot')
    growthHasAnySource ? setModuleSuccess('growth') : setModuleError('growth')
    challengeHasAnySource ? setModuleSuccess('challenge') : setModuleError('challenge')
    versionHasAnySource ? setModuleSuccess('version') : setModuleError('version')

    updateRefreshAt()
    loading.value = false
  }

  const refreshRealtimeData = async () => {
    if (typeof document !== 'undefined' && document.hidden) {
      return
    }

    const [onlineResult, hotPostsResult, hotMomentsResult, planStatsResult] = await Promise.allSettled([
      getOnlineCount(),
      communityApi.getHotPosts(6),
      getHotMoments({ limit: 6 }),
      planApi.getStatsOverview()
    ])

    const online = parseResult(onlineResult, homeData.heroMetrics.onlineCount)
    const hotPosts = parseResult(hotPostsResult, rawCache.hotPosts)
    const hotMoments = parseResult(hotMomentsResult, rawCache.hotMoments)
    const planStats = parseResult(planStatsResult, rawCache.planStats)

    if (hotPosts.ok && Array.isArray(hotPosts.value)) {
      rawCache.hotPosts = hotPosts.value
    }
    if (hotMoments.ok && Array.isArray(hotMoments.value)) {
      rawCache.hotMoments = hotMoments.value
    }
    if (planStats.ok) {
      rawCache.planStats = planStats.value || {}
    }

    homeData.hotFeed = adaptHotFeed({
      posts: rawCache.hotPosts,
      moments: rawCache.hotMoments,
      limitPerType: 4
    })
    homeData.growth.plan = adaptPlanStats(rawCache.planStats)

    homeData.heroMetrics = adaptHeroMetrics({
      learnedTotal: homeData.heroMetrics.learnedCount,
      knowledgeTotal: homeData.heroMetrics.knowledgeCount,
      onlineCount: online.value,
      hotPosts: rawCache.hotPosts,
      hotMoments: rawCache.hotMoments,
      planStats: rawCache.planStats
    })

    updateRefreshAt()
  }

  const startAutoRefresh = () => {
    if (refreshTimer) {
      clearInterval(refreshTimer)
    }
    refreshTimer = setInterval(() => {
      refreshRealtimeData().catch(() => {})
    }, REFRESH_INTERVAL)

    if (!visibilityHandlerBound && typeof document !== 'undefined') {
      document.addEventListener('visibilitychange', handleVisibilityChange)
      visibilityHandlerBound = true
    }
  }

  const stopAutoRefresh = () => {
    if (!refreshTimer) {
      if (visibilityHandlerBound && typeof document !== 'undefined') {
        document.removeEventListener('visibilitychange', handleVisibilityChange)
        visibilityHandlerBound = false
      }
      return
    }
    clearInterval(refreshTimer)
    refreshTimer = null

    if (visibilityHandlerBound && typeof document !== 'undefined') {
      document.removeEventListener('visibilitychange', handleVisibilityChange)
      visibilityHandlerBound = false
    }
  }

  const handleVisibilityChange = () => {
    if (document.hidden) {
      return
    }
    refreshRealtimeData().catch(() => {})
  }

  onBeforeUnmount(() => {
    stopAutoRefresh()
  })

  return {
    loading,
    refreshAt,
    moduleState,
    homeData,
    loadAllData,
    refreshRealtimeData,
    startAutoRefresh,
    stopAutoRefresh
  }
}
