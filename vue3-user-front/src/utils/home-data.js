import { onBeforeUnmount, reactive, ref } from 'vue'
import { homeApi } from '@/api/home'

const REFRESH_INTERVAL = 60_000
const HOME_REQUEST_CONFIG = { silent: true }

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

function normalizeDailyProblem(source = {}) {
  const difficulty = typeof source.difficulty === 'string' ? source.difficulty.toLowerCase() : 'easy'
  const acceptedCount = toNumber(source.acceptedCount)
  const submitCount = toNumber(source.submitCount)
  const difficultyText = {
    easy: '简单',
    medium: '中等',
    hard: '困难'
  }[difficulty] || '未知'

  return {
    ...source,
    difficulty,
    difficultyText,
    acceptedCount,
    submitCount,
    acceptanceRate: submitCount > 0 ? Math.round((acceptedCount / submitCount) * 100) : 0,
    tags: Array.isArray(source.tags) ? source.tags : []
  }
}

function createHomeData() {
  return {
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
      plan: {
        activeCount: 0,
        totalCheckins: 0,
        todayCompleted: 0,
        todayPending: 0,
        todayCompletionRate: 0,
        maxStreak: 0,
        weekCheckinCount: 0,
        monthCheckinCount: 0
      },
      mockInterview: {
        totalInterviews: 0,
        completedInterviews: 0,
        avgScore: 0,
        highestScore: 0,
        interviewStreak: 0,
        completionRate: 0
      },
      points: {
        totalPoints: 0,
        balanceYuan: '0.00',
        continuousDays: 0,
        todayCheckedIn: false,
        todayPoints: 0
      }
    },
    challenge: {
      dailyProblem: {
        id: null,
        title: '今日挑战正在准备中',
        difficulty: 'easy',
        acceptedCount: 0,
        submitCount: 0,
        tags: [],
        routePath: '/oj'
      }
    },
    versions: []
  }
}

export function useHomeData() {
  const loading = ref(true)
  const refreshAt = ref('')
  let refreshTimer = null
  let visibilityHandlerBound = false

  const moduleState = reactive({
    hero: createModuleState(),
    hot: createModuleState(),
    growth: createModuleState(),
    challenge: createModuleState(),
    version: createModuleState()
  })

  const homeData = reactive(createHomeData())

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

  const resetModuleState = () => {
    Object.keys(moduleState).forEach((moduleName) => {
      moduleState[moduleName].loading = true
      moduleState[moduleName].available = false
      moduleState[moduleName].message = '加载中'
    })
  }

  const applyOverview = (overview = {}) => {
    const hero = overview?.heroMetrics || {}
    const hotFeed = overview?.hotFeed || {}
    const growth = overview?.growth || {}
    const challenge = overview?.challenge || {}
    const sections = overview?.sections || {}

    Object.assign(homeData.heroMetrics, {
      learnedCount: toNumber(hero.learnedCount),
      knowledgeCount: toNumber(hero.knowledgeCount),
      onlineCount: toNumber(hero.onlineCount),
      hotTopicCount: toNumber(hero.hotTopicCount),
      todayTaskCompletionRate: toNumber(hero.todayTaskCompletionRate)
    })

    homeData.hotFeed.posts = Array.isArray(hotFeed.posts) ? hotFeed.posts : []
    homeData.hotFeed.moments = Array.isArray(hotFeed.moments) ? hotFeed.moments : []

    Object.assign(homeData.growth.plan, growth.plan || {})
    Object.assign(homeData.growth.mockInterview, growth.mockInterview || {})
    Object.assign(homeData.growth.points, growth.points || {})
    Object.assign(homeData.challenge.dailyProblem, normalizeDailyProblem(challenge.dailyProblem || {}))
    homeData.versions = Array.isArray(overview.versions) ? overview.versions : []

    Object.keys(moduleState).forEach((moduleName) => {
      const section = sections[moduleName]
      if (section?.available) {
        setModuleSuccess(moduleName)
        return
      }
      setModuleError(moduleName, section?.message || '数据暂不可用')
    })
    updateRefreshAt()
  }

  const loadOverview = async () => {
    const overview = await homeApi.getOverview(HOME_REQUEST_CONFIG)
    applyOverview(overview || {})
  }

  const loadAllData = async ({ silent = false } = {}) => {
    if (!silent) {
      loading.value = true
      resetModuleState()
    }

    try {
      await loadOverview()
    } catch {
      Object.keys(moduleState).forEach((moduleName) => {
        setModuleError(moduleName, '数据暂不可用')
      })
    } finally {
      loading.value = false
    }
  }

  const refreshRealtimeData = async () => {
    if (typeof document !== 'undefined' && document.hidden) {
      return
    }
    await loadOverview()
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
