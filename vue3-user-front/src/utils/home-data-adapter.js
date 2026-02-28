const DIFFICULTY_TEXT_MAP = {
  easy: '简单',
  medium: '中等',
  hard: '困难'
}

function toNumber(value, fallback = 0) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : fallback
}

function toDateText(input) {
  if (!input) {
    return '--'
  }

  const date = new Date(input)
  if (Number.isNaN(date.getTime())) {
    return '--'
  }

  const year = String(date.getFullYear())
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function truncateText(text, maxLength = 44) {
  const value = typeof text === 'string' ? text.trim() : ''
  if (!value) {
    return ''
  }
  if (value.length <= maxLength) {
    return value
  }
  return `${value.slice(0, maxLength)}...`
}

function pickTagText(tag) {
  if (!tag || typeof tag !== 'object') {
    return ''
  }
  return tag.tagName || tag.name || tag.tag || ''
}

export function adaptPlanStats(source = {}) {
  const activeCount = toNumber(source.activePlanCount ?? source.totalPlans)
  const totalCheckins = toNumber(source.totalCheckinCount ?? source.totalCheckins)
  const todayCompleted = toNumber(source.todayCompletedCount ?? source.todayCheckins)
  const todayPending = toNumber(source.todayPendingCount ?? source.todayPending)
  const todayTaskCount = todayCompleted + todayPending
  const todayCompletionRate = todayTaskCount > 0
    ? Math.round((todayCompleted / todayTaskCount) * 100)
    : 0

  return {
    activeCount,
    totalCheckins,
    todayCompleted,
    todayPending,
    todayCompletionRate,
    maxStreak: toNumber(source.maxStreak),
    weekCheckinCount: toNumber(source.weekCheckinCount),
    monthCheckinCount: toNumber(source.monthCheckinCount)
  }
}

export function adaptMockStats(source = {}) {
  const completionRate = toNumber(source.completionRate)
  const avgScore = toNumber(source.avgScore)

  return {
    totalInterviews: toNumber(source.totalInterviews),
    completedInterviews: toNumber(source.completedInterviews),
    avgScore: Number(avgScore.toFixed(1)),
    highestScore: toNumber(source.highestScore),
    interviewStreak: toNumber(source.interviewStreak),
    completionRate: Number(completionRate.toFixed(1))
  }
}

export function adaptDailyProblem(problem = {}) {
  const acceptedCount = toNumber(problem.acceptedCount)
  const submitCount = toNumber(problem.submitCount)
  const acceptanceRate = submitCount > 0 ? Math.round((acceptedCount / submitCount) * 100) : 0
  const difficulty = typeof problem.difficulty === 'string'
    ? problem.difficulty.toLowerCase()
    : 'easy'

  return {
    id: problem.id || null,
    title: problem.title || '今日挑战正在准备中',
    difficulty,
    difficultyText: DIFFICULTY_TEXT_MAP[difficulty] || '未知',
    acceptanceRate,
    acceptedCount,
    submitCount,
    tags: Array.isArray(problem.tags) ? problem.tags.map(pickTagText).filter(Boolean).slice(0, 4) : [],
    routePath: problem.id ? `/oj/problem/${problem.id}` : '/oj'
  }
}

function adaptHotPostItem(item = {}) {
  return {
    id: item.id || null,
    title: truncateText(item.title || '未命名帖子', 38),
    authorName: item.authorName || '匿名用户',
    likeCount: toNumber(item.likeCount),
    commentCount: toNumber(item.commentCount),
    routePath: item.id ? `/community/posts/${item.id}` : '/community'
  }
}

function adaptHotMomentItem(item = {}) {
  return {
    id: item.id || null,
    title: truncateText(item.content || '暂无动态内容', 52),
    authorName: item.userNickname || '社区用户',
    likeCount: toNumber(item.likeCount),
    commentCount: toNumber(item.commentCount),
    routePath: '/moments'
  }
}

export function adaptHotFeed({ posts = [], moments = [], limitPerType = 3 } = {}) {
  const safeLimit = Math.max(1, toNumber(limitPerType, 3))
  const safePosts = Array.isArray(posts) ? posts : []
  const safeMoments = Array.isArray(moments) ? moments : []

  return {
    posts: safePosts.slice(0, safeLimit).map(adaptHotPostItem),
    moments: safeMoments.slice(0, safeLimit).map(adaptHotMomentItem)
  }
}

export function adaptVersionFeed(list = [], limit = 4) {
  const safeList = Array.isArray(list) ? list : []
  const safeLimit = Math.max(1, toNumber(limit, 4))

  return safeList.slice(0, safeLimit).map((item) => ({
    id: item.id || null,
    version: item.versionNumber || '--',
    title: item.title || '版本更新',
    typeName: item.updateTypeName || '常规更新',
    description: item.description || '',
    dateText: toDateText(item.releaseTime || item.createdTime),
    routePath: '/version-history'
  }))
}

export function adaptHeroMetrics({
  learnedTotal = 0,
  knowledgeTotal = 0,
  onlineCount = 0,
  hotPosts = [],
  hotMoments = [],
  planStats = {}
} = {}) {
  const normalizedPlan = adaptPlanStats(planStats)
  const postCount = Array.isArray(hotPosts) ? hotPosts.length : 0
  const momentCount = Array.isArray(hotMoments) ? hotMoments.length : 0

  return {
    learnedCount: toNumber(learnedTotal),
    knowledgeCount: toNumber(knowledgeTotal),
    onlineCount: toNumber(onlineCount),
    hotTopicCount: postCount + momentCount,
    todayTaskCompletionRate: normalizedPlan.todayCompletionRate
  }
}
