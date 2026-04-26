const CONTEST_TYPE_LABEL_MAP = {
  weekly: '周赛',
  challenge: '挑战赛'
}

const CONTEST_STATUS_LABEL_MAP = {
  0: '草稿',
  1: '即将开始',
  2: '进行中',
  3: '已结束'
}

const CONTEST_STATUS_TAG_MAP = {
  0: 'info',
  1: 'warning',
  2: 'success',
  3: 'info'
}

function toNumber(value, fallback = 0) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : fallback
}

function normalizeDateTime(value) {
  if (!value) return ''
  if (typeof value !== 'string') return String(value)
  return value.replace('T', ' ').slice(0, 19)
}

function normalizeContestType(value) {
  if (value === 1 || value === '1' || value === 'weekly') return 'weekly'
  if (value === 2 || value === '2' || value === 'challenge') return 'challenge'
  return 'weekly'
}

function normalizeContestStatus(value) {
  const normalized = toNumber(value, 0)
  return [0, 1, 2, 3].includes(normalized) ? normalized : 0
}

function toTimestamp(value) {
  if (!value) return Number.MAX_SAFE_INTEGER
  const time = new Date(value).getTime()
  return Number.isNaN(time) ? Number.MAX_SAFE_INTEGER : time
}

export function getContestTypeLabel(type) {
  return CONTEST_TYPE_LABEL_MAP[type] || '赛事'
}

export function getContestStatusLabel(status) {
  return CONTEST_STATUS_LABEL_MAP[status] || '未知状态'
}

export function getContestStatusTag(status) {
  return CONTEST_STATUS_TAG_MAP[status] || 'info'
}

export function adaptContestItem(item = {}) {
  return {
    id: item.id ?? item.contestId ?? null,
    title: item.title || item.name || '未命名赛事',
    description: item.description || '',
    contestType: normalizeContestType(item.contestType || item.type),
    status: normalizeContestStatus(item.status),
    startTime: normalizeDateTime(item.startTime || item.beginTime),
    endTime: normalizeDateTime(item.endTime || item.finishTime),
    problemCount: toNumber(item.problemCount),
    participantCount: toNumber(item.participantCount ?? item.joinCount),
    problemIds: Array.isArray(item.problemIds) ? item.problemIds : []
  }
}

export function adaptContestList(data) {
  if (Array.isArray(data)) {
    return {
      records: data.map(adaptContestItem),
      total: data.length
    }
  }

  if (!data || typeof data !== 'object') {
    return {
      records: [],
      total: 0
    }
  }

  const rawRecords = Array.isArray(data.records)
    ? data.records
    : Array.isArray(data.list)
      ? data.list
      : []

  return {
    records: rawRecords.map(adaptContestItem),
    total: toNumber(data.total, rawRecords.length)
  }
}

function buildRankingSortKey(item) {
  return `${item.solvedCount}|${item.penalty}|${item.lastAcTime || ''}`
}

export function adaptContestRanking(rows = [], options = {}) {
  const safeRows = Array.isArray(rows) ? rows : []
  const currentUserId = options.currentUserId == null ? null : Number(options.currentUserId)

  const normalized = safeRows.map((item) => ({
    userId: Number(item.userId || 0),
    nickname: item.nickname || item.userName || `用户${item.userId || '-'}`,
    avatar: item.avatar || '',
    solvedCount: toNumber(item.solvedCount),
    penalty: toNumber(item.penalty),
    performanceScore: toNumber(item.performanceScore),
    ratingChange: toNumber(item.ratingChange),
    ratingAfter: toNumber(item.ratingAfter, 1500),
    lastAcTime: normalizeDateTime(item.lastAcTime)
  }))

  normalized.sort((a, b) => {
    if (b.solvedCount !== a.solvedCount) return b.solvedCount - a.solvedCount
    if (a.penalty !== b.penalty) return a.penalty - b.penalty
    const timeDiff = toTimestamp(a.lastAcTime) - toTimestamp(b.lastAcTime)
    if (timeDiff !== 0) return timeDiff
    return a.userId - b.userId
  })

  let previousSortKey = ''
  let previousRank = 0

  return normalized.map((item, index) => {
    const sortKey = buildRankingSortKey(item)
    const rank = sortKey === previousSortKey ? previousRank : index + 1
    previousSortKey = sortKey
    previousRank = rank

    return {
      ...item,
      rank,
      solvedText: `${item.solvedCount} 题`,
      penaltyText: `${item.penalty} 分钟`,
      performanceText: `${item.performanceScore} 分`,
      ratingChangeText: item.ratingChange > 0 ? `+${item.ratingChange}` : String(item.ratingChange),
      ratingChangeType: item.ratingChange > 0 ? 'success' : item.ratingChange < 0 ? 'danger' : 'info',
      ratingAfterText: item.ratingAfter || '--',
      lastAcText: item.lastAcTime || '--',
      isCurrentUser: currentUserId != null && item.userId === currentUserId
    }
  })
}
