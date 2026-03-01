import request from '@/utils/request'

// ==================== 敏感词管理 ====================
// 分页查询敏感词列表
export function listSensitiveWords(data) {
  return request.post('/admin/sensitive/words/list', data)
}

// 根据ID查询敏感词
export function getSensitiveWordById(id) {
  return request.post(`/admin/sensitive/words/${id}`)
}

// 新增敏感词
export function addSensitiveWord(data) {
  return request.post('/admin/sensitive/words', data)
}

// 更新敏感词
export function updateSensitiveWord(data) {
  return request.post('/admin/sensitive/words/update', data)
}

// 删除敏感词
export function deleteSensitiveWord(id) {
  return request.post(`/admin/sensitive/words/delete/${id}`)
}

// 批量删除敏感词
export function deleteSensitiveWords(ids) {
  return request.post('/admin/sensitive/words/delete/batch', ids)
}

// 批量导入敏感词
export function importSensitiveWords(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/sensitive/words/import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 预览导入敏感词
export function previewSensitiveWordsImport(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/sensitive/words/preview-import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 确认导入敏感词
export function confirmSensitiveWordsImport(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/sensitive/words/confirm-import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 导出敏感词（CSV内容）
export function exportSensitiveWords(data) {
  return request.post('/admin/sensitive/words/export', data)
}

// 刷新敏感词库
export function refreshWordLibrary() {
  return request.post('/admin/sensitive/refresh')
}

// 查询敏感词分类列表
export function listSensitiveCategories() {
  return request.post('/admin/sensitive/categories')
}

// ==================== 白名单管理 ====================
// 分页查询白名单列表
export function listWhitelist(data) {
  return request.post('/sensitive/whitelist/list', data)
}

// 根据ID查询白名单
export function getWhitelistById(id) {
  return request.get(`/sensitive/whitelist/${id}`)
}

// 新增白名单
export function addWhitelist(data) {
  return request.post('/sensitive/whitelist/add', data)
}

// 更新白名单
export function updateWhitelist(data) {
  return request.post('/sensitive/whitelist/update', data)
}

// 删除白名单
export function deleteWhitelist(id) {
  return request.post(`/sensitive/whitelist/delete/${id}`)
}

// 批量删除白名单
export function deleteWhitelistBatch(ids) {
  return request.post('/sensitive/whitelist/delete/batch', ids)
}

// 批量导入白名单
export function importWhitelist(data) {
  return request.post('/sensitive/whitelist/import', data)
}

// 刷新白名单缓存
export function refreshWhitelistCache() {
  return request.post('/sensitive/whitelist/refresh')
}

// ==================== 策略管理 ====================
// 分页查询策略列表
export function listStrategy(data) {
  return request.post('/sensitive/strategy/list', data)
}

// 根据ID查询策略
export function getStrategyById(id) {
  return request.get(`/sensitive/strategy/${id}`)
}

// 根据模块和等级获取策略
export function getStrategy(module, level) {
  return request.get('/sensitive/strategy/get', { params: { module, level } })
}

// 更新策略
export function updateStrategy(data) {
  return request.post('/sensitive/strategy/update', data)
}

// 重置策略
export function resetStrategy(id) {
  return request.post(`/sensitive/strategy/reset/${id}`)
}

// 刷新策略缓存
export function refreshStrategyCache() {
  return request.post('/sensitive/strategy/refresh')
}

// ==================== 统计分析 ====================
// 获取统计总览
export function getStatisticsOverview(data) {
  return request.post('/sensitive/statistics/overview', data)
}

// 获取命中趋势
export function getHitTrend(data) {
  return request.post('/sensitive/statistics/trend', data)
}

// 获取热门敏感词
export function getHotWords(data) {
  return request.post('/sensitive/statistics/hot-words', data)
}

// 获取分类分布
export function getCategoryDistribution(data) {
  return request.post('/sensitive/statistics/category-distribution', data)
}

// 获取模块分布
export function getModuleDistribution(data) {
  return request.post('/sensitive/statistics/module-distribution', data)
}

// 导出统计报表
export function exportStatisticsReport(data) {
  return request.post('/sensitive/statistics/export', data)
}

// ==================== 词库来源管理 ====================
// 查询词库来源列表
export function listSources(data) {
  return request.post('/sensitive/source/list', data)
}

// 获取词库来源详情
export function getSourceById(id) {
  return request.get(`/sensitive/source/${id}`)
}

// 新增词库来源
export function addSource(data) {
  return request.post('/sensitive/source/add', data)
}

// 更新词库来源
export function updateSource(data) {
  return request.post('/sensitive/source/update', data)
}

// 删除词库来源
export function deleteSource(id) {
  return request.post(`/sensitive/source/delete/${id}`)
}

// 测试连接
export function testSourceConnection(id) {
  return request.post(`/sensitive/source/test-connection/${id}`)
}

// 手动同步词库
export function syncSource(id) {
  return request.post(`/sensitive/source/sync/${id}`)
}

// ==================== 版本管理 ====================
// 查询版本历史列表
export function listVersions(data) {
  return request.post('/sensitive/version/list', data)
}

// 获取版本详情
export function getVersionById(id) {
  return request.get(`/sensitive/version/${id}`)
}

// 回滚到指定版本
export function rollbackVersion(id, data) {
  return request.post(`/sensitive/version/rollback/${id}`, data)
}

// 获取最新版本号
export function getLatestVersion() {
  return request.get('/sensitive/version/latest')
}

// ==================== 同音字管理 ====================
// 查询同音字列表
export function listHomophones(data) {
  return request.post('/sensitive/homophone/list', data)
}

// 获取同音字详情
export function getHomophoneById(id) {
  return request.get(`/sensitive/homophone/${id}`)
}

// 新增同音字
export function addHomophone(data) {
  return request.post('/sensitive/homophone/add', data)
}

// 批量新增同音字
export function batchAddHomophones(data) {
  return request.post('/sensitive/homophone/batch-add', data)
}

// 更新同音字
export function updateHomophone(data) {
  return request.post('/sensitive/homophone/update', data)
}

// 删除同音字
export function deleteHomophone(id) {
  return request.post(`/sensitive/homophone/delete/${id}`)
}

// 刷新同音字缓存
export function refreshHomophoneCache() {
  return request.post('/sensitive/homophone/refresh')
}

// ==================== 形似字管理 ====================
// 查询形似字列表
export function listSimilarChars(data) {
  return request.post('/sensitive/similar-char/list', data)
}

// 获取形似字详情
export function getSimilarCharById(id) {
  return request.get(`/sensitive/similar-char/${id}`)
}

// 新增形似字
export function addSimilarChar(data) {
  return request.post('/sensitive/similar-char/add', data)
}

// 批量新增形似字
export function batchAddSimilarChars(data) {
  return request.post('/sensitive/similar-char/batch-add', data)
}

// 更新形似字
export function updateSimilarChar(data) {
  return request.post('/sensitive/similar-char/update', data)
}

// 删除形似字
export function deleteSimilarChar(id) {
  return request.post(`/sensitive/similar-char/delete/${id}`)
}

// 刷新形似字缓存
export function refreshSimilarCharCache() {
  return request.post('/sensitive/similar-char/refresh')
} 
