import request from '@/utils/request'

/**
 * 管理后台 AI 配置 API
 */

export function getAiRuntimeConfig() {
  return request.get('/admin/ai/config/runtime')
}

export function getAiSchemaCatalog() {
  return request.get('/admin/ai/config/schema-catalog')
}

export function getAiRegressionCases() {
  return request.get('/admin/ai/config/regression/cases')
}

export function getAiRegressionLatestRun() {
  return request.get('/admin/ai/config/regression/latest')
}

export function getAiRegressionHistory(params = {}) {
  return request.get('/admin/ai/config/regression/history', params)
}

export function runAiRegression(data = {}) {
  return request.post('/admin/ai/config/regression/run', data)
}

export function debugAiPrompt(data) {
  return request.post('/admin/ai/config/prompt-debug', data)
}

export function debugAiRag(data) {
  return request.post('/admin/ai/config/rag-debug', data)
}

export function getAiRagServiceHealth() {
  return request.get('/admin/ai/config/rag-service/health')
}

export function getAiRagServiceDocuments(params = {}) {
  return request.get('/admin/ai/config/rag-service/documents', params)
}

export function exportAiRagDocuments(params = {}) {
  return request.get('/admin/ai/config/rag-service/documents/export', params)
}

export function importAiRagSampleDocuments(data = {}) {
  return request.post('/admin/ai/config/rag-service/sample-import', data)
}

export function importAiRagDocuments(data = {}) {
  return request.post('/admin/ai/config/rag-service/documents/import', data)
}

export function deleteAiRagDocument(params = {}) {
  return request.delete('/admin/ai/config/rag-service/documents', params)
}

export function batchDeleteAiRagDocuments(data = {}) {
  return request.post('/admin/ai/config/rag-service/documents/batch-delete', data)
}

export function getAiRuntimeMetrics(params = {}) {
  return request.get('/admin/ai/config/metrics', params)
}

export function clearAiRuntimeMetrics() {
  return request.delete('/admin/ai/config/metrics')
}

export function testAiConfig(data) {
  return request.post('/admin/ai/config/test', data)
}

export const aiConfigApi = {
  getAiRuntimeConfig,
  getAiSchemaCatalog,
  getAiRegressionCases,
  getAiRegressionLatestRun,
  getAiRegressionHistory,
  runAiRegression,
  debugAiPrompt,
  debugAiRag,
  getAiRagServiceHealth,
  getAiRagServiceDocuments,
  exportAiRagDocuments,
  importAiRagSampleDocuments,
  importAiRagDocuments,
  deleteAiRagDocument,
  batchDeleteAiRagDocuments,
  getAiRuntimeMetrics,
  clearAiRuntimeMetrics,
  testAiConfig
}
