import request from '@/utils/request'

/**
 * 朋友圈相关API
 */

// 发布动态
export function publishMoment(data) {
  return request.post('/user/moments/publish', data)
}

// 获取动态列表
export function getMomentList(data) {
  return request.post('/user/moments/list', data)
}

// 删除动态
export function deleteMoment(id) {
  return request.delete(`/user/moments/${id}`)
}

// 点赞/取消点赞
export function toggleLike(momentId) {
  return request.post(`/user/moments/${momentId}/like`)
}

// 发布评论
export function publishComment(data) {
  return request.post('/user/moments/comment', data)
}

// 删除评论
export function deleteComment(id) {
  return request.delete(`/user/moments/comments/${id}`)
}

// 获取动态的完整评论列表
export function getMomentComments(data) {
  return request.post('/user/moments/comments', data)
}

// ========== v1.1.0 新增接口 ==========

// 获取热门动态
export function getHotMoments(data, config = {}) {
  return request.post('/user/moments/hot', data, config)
}

// 搜索动态
export function searchMoments(data) {
  return request.post('/user/moments/search', data)
}

// 获取用户个人动态列表
export function getUserMomentList(data) {
  return request.post('/user/moments/user-list', data)
}

// 获取用户动态信息
export function getUserMomentInfo(data) {
  return request.post('/user/moments/user-info', data)
}

// 收藏/取消收藏
export function toggleFavorite(momentId) {
  return request.post(`/user/moments/${momentId}/favorite`)
}

// 获取我的收藏列表
export function getMyFavorites(data) {
  return request.post('/user/moments/my-favorites', data)
}
