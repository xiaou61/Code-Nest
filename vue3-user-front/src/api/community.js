import request from '@/utils/request'

export const communityApi = {
  // 获取帖子列表
  getPostList(params) {
    return request.post('/community/posts/list', params)
  },

  // 获取帖子详情
  getPostDetail(id) {
    return request.get(`/community/posts/${id}`)
  },

  // 创建帖子
  createPost(data) {
    return request.post('/community/posts', data)
  },

  // 点赞帖子
  likePost(id) {
    return request.post(`/community/posts/${id}/like`)
  },

  // 取消点赞帖子
  unlikePost(id) {
    return request.delete(`/community/posts/${id}/like`)
  },

  // 收藏帖子
  collectPost(id) {
    return request.post(`/community/posts/${id}/collect`)
  },

  // 取消收藏帖子
  uncollectPost(id) {
    return request.delete(`/community/posts/${id}/collect`)
  },

  // 获取帖子评论
  getPostComments(id, params) {
    return request.post(`/community/posts/${id}/comments`, params)
  },

  // 创建评论
  createComment(postId, data) {
    return request.post(`/community/posts/${postId}/comments/create`, data)
  },

  // 点赞评论
  likeComment(id) {
    return request.post(`/community/comments/${id}/like`)
  },

  // 获取我的帖子
  getMyPosts(params) {
    return request.post('/community/user/posts', params)
  },

  // 获取我的收藏
  getMyCollections(params) {
    return request.post('/community/user/collections', params)
  },

  // 别名，与后端方法名保持一致
  getUserPosts(params) {
    return this.getMyPosts(params)
  },

  // 别名，与后端方法名保持一致  
  getUserCollections(params) {
    return this.getMyCollections(params)
  },

  // 获取启用的分类列表
  getEnabledCategories() {
    return request.get('/community/categories')
  },

  // 社区初始化
  init() {
    return request.get('/community/init')
  },

  // ==================== v1.1.0 新增接口 ====================

  // 获取热门帖子
  getHotPosts(limit = 5) {
    return request.get('/community/posts/hot', { limit })
  },

  // 获取启用的标签列表
  getTags() {
    return request.get('/community/tags')
  },

  // 获取热门标签
  getHotTags(limit = 10) {
    return request.get('/community/tags/hot', { limit })
  },

  // 获取标签下的帖子
  getTagPosts(tagId, params) {
    return request.post(`/community/tags/${tagId}/posts`, params)
  },

  // 回复评论
  replyComment(commentId, data) {
    return request.post(`/community/comments/${commentId}/reply`, data)
  },

  // 获取评论的回复列表
  getCommentReplies(commentId, params) {
    return request.post(`/community/comments/${commentId}/replies`, params)
  },

  // 获取用户主页信息
  getUserProfile(userId) {
    return request.get(`/community/users/${userId}/profile`)
  },

  // 获取用户帖子列表
  getUserPostList(userId, params) {
    return request.post(`/community/users/${userId}/posts`, params)
  },

  // 生成AI摘要
  generateSummary(postId, forceRefresh = false) {
    return request.post(`/community/posts/${postId}/generate-summary`, null, {
      params: { forceRefresh }
    })
  },

  // 获取帖子摘要
  getPostSummary(postId) {
    return request.get(`/community/posts/${postId}/summary`)
  },

  // 获取热门搜索词
  getHotKeywords(limit = 10) {
    return request.get('/community/hot-keywords', { limit })
  }
} 
