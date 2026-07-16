import request from '@/utils/request'

/**
 * 用户端知识图谱API
 */

// 获取已发布的知识图谱列表
export function getPublishedKnowledgeMaps(params, config = {}) {
  return request.post('/pub/knowledge/maps/list', params, config)
}

// 根据ID获取知识图谱详情
export function getKnowledgeMapById(id) {
  return request.get(`/pub/knowledge/maps/${id}`)
}

// 获取图谱节点树
export function getKnowledgeNodeTree(mapId) {
  return request.get(`/pub/knowledge/maps/${mapId}/nodes`)
}

// 搜索节点
export function searchKnowledgeNodes(mapId, keyword) {
  return request.get(`/pub/knowledge/maps/${mapId}/nodes/search`, { keyword })
}

// 记录节点查看
export function recordNodeView(nodeId) {
  return request.post(`/pub/knowledge/maps/nodes/${nodeId}/view`)
}
