const STAGE_ORDER = {
  INIT: 0,
  JD_PARSED: 1,
  RESUME_MATCHED: 2,
  PLAN_READY: 3,
  PLAN_EXECUTING: 4,
  INTERVIEW_DONE: 5,
  REVIEWED: 6,
  OFFER_TRACKING: 7
}

const MAX_STAGE_ORDER = Math.max(...Object.values(STAGE_ORDER))

const STAGE_LABEL = {
  INIT: '初始化',
  JD_PARSED: '已完成 JD 解析',
  RESUME_MATCHED: '已完成简历匹配',
  PLAN_READY: '计划已生成',
  PLAN_EXECUTING: '计划执行中',
  INTERVIEW_DONE: '面试已完成',
  REVIEWED: '复盘已完成',
  OFFER_TRACKING: '投递与 Offer 跟踪'
}

const ACTION_STATUS_TAG = {
  todo: 'warning',
  doing: 'primary',
  done: 'success'
}

const ACTION_STATUS_LABEL = {
  todo: '待处理',
  doing: '进行中',
  done: '已完成'
}

function toNumber(value, fallback = 0) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : fallback
}

export function mapStageLabel(stage) {
  return STAGE_LABEL[stage] || '未知阶段'
}

export function mapStageOrder(stage) {
  return STAGE_ORDER[stage] ?? 0
}

export function mapStagePercent(stage) {
  const order = mapStageOrder(stage)
  return Math.min(100, Math.round((order / MAX_STAGE_ORDER) * 100))
}

export function mapActionStatusTag(status) {
  return ACTION_STATUS_TAG[status] || 'info'
}

export function mapActionStatusLabel(status) {
  return ACTION_STATUS_LABEL[status] || '未知'
}

export function adaptCareerLoopCurrent(data = {}) {
  const session = data.session || {}
  const snapshot = data.snapshot || {}
  const actions = Array.isArray(data.actions) ? data.actions : []
  const riskFlags = Array.isArray(data.riskFlags) ? data.riskFlags : []
  const nextSuggestions = Array.isArray(data.nextSuggestions) ? data.nextSuggestions : []

  return {
    session: {
      ...session,
      currentStage: session.currentStage || 'INIT',
      currentStageLabel: mapStageLabel(session.currentStage || 'INIT'),
      stagePercent: mapStagePercent(session.currentStage || 'INIT'),
      healthScore: toNumber(session.healthScore, 60)
    },
    snapshot: {
      ...snapshot,
      planProgress: toNumber(snapshot.planProgress),
      mockCount: toNumber(snapshot.mockCount),
      latestMockScore: snapshot.latestMockScore == null ? null : toNumber(snapshot.latestMockScore),
      reviewCount: toNumber(snapshot.reviewCount)
    },
    actions,
    riskFlags,
    nextSuggestions
  }
}

