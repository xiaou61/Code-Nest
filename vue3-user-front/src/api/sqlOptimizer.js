import request from '@/utils/request'

/**
 * SQL优化工作台 API
 */
export const sqlOptimizerApi = {
  analyzeWorkbench(data) {
    return request.post('/user/sql-optimizer/workbench/analyze', data)
  },

  rewriteWorkbench(data) {
    return request.post('/user/sql-optimizer/workbench/rewrite', data)
  },

  batchAnalyzeWorkbench(data) {
    return request.post('/user/sql-optimizer/workbench/batch-analyze', data)
  },

  compareWorkbench(data) {
    return request.post('/user/sql-optimizer/workbench/compare', data)
  },

  getWorkbenchCases(params = {}) {
    return request.get('/user/sql-optimizer/workbench/cases', params)
  },

  getWorkbenchCaseDetail(id) {
    return request.get(`/user/sql-optimizer/workbench/cases/${id}`)
  },

  analyze(data) {
    return request.post('/user/sql-optimizer/analyze', data)
  },

  getHistory(params = {}) {
    return request.get('/user/sql-optimizer/history', params)
  },

  getDetail(id) {
    return request.get(`/user/sql-optimizer/${id}`)
  },

  toggleFavorite(id) {
    return request.post(`/user/sql-optimizer/favorite/${id}`)
  },

  deleteHistory(id) {
    return request.delete(`/user/sql-optimizer/history/${id}`)
  }
}

export default sqlOptimizerApi
