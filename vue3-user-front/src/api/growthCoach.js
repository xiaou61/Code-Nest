import request from '@/utils/request'

/**
 * AI 成长教练受控计划调整 API。
 */
export const growthCoachApi = {
  getBriefing() {
    return request.get('/user/growth-coach/briefing')
  },

  recordJourneyEvent(data = {}) {
    const entryPage = data.entryPage || (typeof window !== 'undefined' ? window.location.pathname : '/home')
    return request.post('/user/growth-coach/journey-events', {
      ...data,
      schemaVersion: data.schemaVersion || '1',
      clientVersion: data.clientVersion || import.meta.env.VITE_APP_VERSION || '2.5.5',
      entryPage
    }, { silent: true })
  },

  getCodeArtifacts() {
    return request.get('/user/growth-coach/code-artifacts')
  },

  getGithubConnection() {
    return request.get('/user/growth-coach/github-connection')
  },

  authorizeGithubConnection() {
    return request.post('/user/growth-coach/github-connection/authorize')
  },

  unlinkGithubConnection() {
    return request.delete('/user/growth-coach/github-connection')
  },

  previewCodeArtifact(data = {}) {
    return request.post('/user/growth-coach/code-artifacts/preview', data)
  },

  attachCodeArtifact(data = {}) {
    return request.post('/user/growth-coach/code-artifacts', data)
  },

  deleteCodeArtifact(artifactId) {
    return request.delete(`/user/growth-coach/code-artifacts/${artifactId}`)
  },

  reviewCodePen(data = {}) {
    return request.post('/user/growth-coach/code-reviews', data)
  },

  getLatestCodeReview(penId) {
    return request.get('/user/growth-coach/code-reviews/latest', { params: { penId } })
  },

  deleteCodeReview(reviewId) {
    return request.delete(`/user/growth-coach/code-reviews/${reviewId}`)
  },

  getTodayAction() {
    return request.get('/user/growth-coach/today-action')
  },

  getEvidence(limit = 10) {
    return request.get('/user/growth-coach/evidence', { params: { limit } })
  },

  getEvidenceProfile() {
    return request.get('/user/growth-coach/evidence-profile')
  },

  getSkillInsights() {
    return request.get('/user/growth-coach/skill-insights')
  },

  getCapabilityGraph() {
    return request.get('/user/growth-coach/capability-graph')
  },

  getCareerNextAction() {
    return request.get('/user/growth-coach/career-next-action')
  },

  getApplicationOutcomes() {
    return request.get('/user/growth-coach/application-outcomes')
  },

  getJobBattleGap() {
    return request.get('/user/growth-coach/job-battle-gap')
  },

  getJobPreparationLoop() {
    return request.get('/user/growth-coach/job-preparation-loop')
  },

  getJobMarketSignal() {
    return request.get('/user/growth-coach/job-market-signal')
  },

  getWeeklyReview() {
    return request.get('/user/growth-coach/weekly-review')
  },

  completeCareerNextAction(actionId) {
    return request.post(`/user/growth-coach/career-next-action/${actionId}/complete`)
  },

  previewPlanAdjustment(data = {}) {
    return request.post('/user/growth-coach/plan-adjustments/preview', data)
  },

  confirmPlanAdjustment(runId, data = {}) {
    return request.post(`/user/growth-coach/plan-adjustments/${runId}/confirm`, data)
  },

  cancelPlanAdjustment(runId) {
    return request.post(`/user/growth-coach/plan-adjustments/${runId}/cancel`)
  },

  getPlanAdjustment(runId) {
    return request.get(`/user/growth-coach/plan-adjustments/${runId}`)
  }
}

export default growthCoachApi
