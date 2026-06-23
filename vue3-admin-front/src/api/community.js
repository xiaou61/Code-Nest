import request from '@/utils/request'

// 获取帖子列表
export function getPostList(params) {
  return request.post('/admin/community/posts/list', params)
}

// 获取帖子详情
export function getPostDetail(id) {
  return request.get(`/admin/community/posts/${id}`)
}

// 删除帖子
export function deletePost(id) {
  return request.delete(`/admin/community/posts/${id}`)
}

// 置顶帖子
export function topPost(id, data) {
  return request.put(`/admin/community/posts/${id}/top`, data)
}

// 取消置顶
export function cancelTop(id) {
  return request.delete(`/admin/community/posts/${id}/top`)
}

// 下架帖子  
export function disablePost(id) {
  return request.put(`/admin/community/posts/${id}/disable`)
}

// 获取帖子详情
export function getPostById(id) {
  return request.get(`/admin/community/posts/${id}`)
}

// 获取所有启用的分类
export function getEnabledCategories() {
  return request.get('/community/categories')
}

// 获取评论列表
export function getCommentList(params) {
  return request.post('/admin/community/comments/list', params)
}

// 获取评论详情
export function getCommentById(id) {
  return request.get(`/admin/community/comments/${id}`)
}

// 删除评论
export function deleteComment(id) {
  return request.delete(`/admin/community/comments/${id}`)
}

// 获取用户状态列表
export function getUserStatusList(params) {
  return request.post('/admin/community/users/list', params)
}

// 封禁用户
export function banUser(id, data) {
  return request.put(`/admin/community/users/${id}/ban`, data)
}

// 解封用户
export function unbanUser(id) {
  return request.delete(`/admin/community/users/${id}/ban`)
}

// 获取用户发帖记录
export function getUserPosts(userId) {
  return request.get(`/admin/community/users/${userId}/posts`)
}

// 获取用户评论记录
export function getUserComments(userId) {
  return request.get(`/admin/community/users/${userId}/comments`)
}

// ============ 分类管理接口（新增）============

// 获取分类列表
export function getCategoryList(params) {
  return request.post('/admin/community/categories/list', params)
}

// 获取分类详情
export function getCategoryDetail(id) {
  return request.get(`/admin/community/categories/${id}`)
}

// 创建分类
export function createCategory(data) {
  return request.post('/admin/community/categories', data)
}

// 更新分类
export function updateCategory(id, data) {
  return request.put(`/admin/community/categories/${id}`, data)
}

// 删除分类
export function deleteCategory(id) {
  return request.delete(`/admin/community/categories/${id}`)
}

// 切换分类状态
export function toggleCategoryStatus(id) {
  return request.patch(`/admin/community/categories/${id}/status`)
}

// ============ 标签管理接口（v1.1.0 新增）============

// 获取标签列表
export function getTagList(params) {
  return request.post('/admin/community/tags/list', params)
}

// 获取标签详情
export function getTagDetail(id) {
  return request.get(`/admin/community/tags/${id}`)
}

// 创建标签
export function createTag(data) {
  return request.post('/admin/community/tags', data)
}

// 更新标签
export function updateTag(id, data) {
  return request.put(`/admin/community/tags/${id}`, data)
}

// 删除标签
export function deleteTag(id) {
  return request.delete(`/admin/community/tags/${id}`)
}

// 切换标签状态
export function toggleTagStatus(id) {
  return request.patch(`/admin/community/tags/${id}/status`)
}

export const communityApi = {
  // 帖子相关
  getPostList,
  getPostDetail,
  getPostById,
  deletePost,
  topPost,
  cancelTop,
  disablePost,
  getEnabledCategories,
  
  // 评论相关
  getCommentList,
  getCommentById,
  deleteComment,
  
  // 用户相关
  getUserStatusList,
  banUser,
  unbanUser,
  getUserPosts,
  getUserComments,
  
  // 分类相关
  getCategoryList,
  getCategoryDetail,
  createCategory,
  updateCategory,
  deleteCategory,
  toggleCategoryStatus,
  
  // 标签相关
  getTagList,
  getTagDetail,
  createTag,
  updateTag,
  deleteTag,
  toggleTagStatus
} 
