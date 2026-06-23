import request from '@/utils/request'

/**
 * 知识图谱管理API
 */

// 获取知识图谱列表
export function getKnowledgeMapList(params) {
  return request.post('/admin/knowledge/maps/list', params)
}

// 根据ID获取知识图谱详情
export function getKnowledgeMapById(id) {
  return request.get(`/admin/knowledge/maps/${id}`)
}

// 创建知识图谱
export function createKnowledgeMap(data) {
  return request.post('/admin/knowledge/maps', data)
}

// 更新知识图谱
export function updateKnowledgeMap(id, data) {
  return request.put(`/admin/knowledge/maps/${id}`, data)
}

// 发布知识图谱
export function publishKnowledgeMap(id) {
  return request.post(`/admin/knowledge/maps/${id}/publish`)
}

// 隐藏知识图谱
export function hideKnowledgeMap(id) {
  return request.post(`/admin/knowledge/maps/${id}/hide`)
}

// 删除知识图谱
export function deleteKnowledgeMap(id) {
  return request.delete(`/admin/knowledge/maps/${id}`)
}

// 批量删除知识图谱
export function deleteBatchKnowledgeMaps(ids) {
  return request.delete('/admin/knowledge/maps/batch', { data: ids })
}

/**
 * 知识节点管理API
 */

// 获取图谱节点树
export function getKnowledgeNodeTree(mapId) {
  return request.get(`/admin/knowledge/maps/${mapId}/nodes`)
}

// 根据ID获取知识节点详情
export function getKnowledgeNodeById(id) {
  return request.get(`/admin/knowledge/nodes/${id}`)
}

// 创建知识节点
export function createKnowledgeNode(mapId, data) {
  return request.post(`/admin/knowledge/maps/${mapId}/nodes`, data)
}

// 更新知识节点
export function updateKnowledgeNode(id, data) {
  return request.put(`/admin/knowledge/nodes/${id}`, data)
}

// 批量更新节点排序
export function sortKnowledgeNodes(mapId, data) {
  return request.put(`/admin/knowledge/maps/${mapId}/nodes/sort`, data)
}

// 删除知识节点
export function deleteKnowledgeNode(id) {
  return request.delete(`/admin/knowledge/nodes/${id}`)
}

// 搜索节点
export function searchKnowledgeNodes(mapId, keyword) {
  return request.get(`/admin/knowledge/maps/${mapId}/nodes/search`, { keyword })
}
