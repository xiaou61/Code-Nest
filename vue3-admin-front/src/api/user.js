import request from '@/utils/request'

export const userApi = {
  // 分页查询用户列表
  getUserList(params) {
    return request.get('/admin/user/list', params)
  },
  
  // 获取所有用户（不分页）
  getAllUsers() {
    return request.get('/admin/user/all')
  },
  
  // 根据ID获取用户详情
  getUserInfo(userId) {
    return request.get(`/admin/user/${userId}`)
  },
  
  // 创建新用户
  createUser(data) {
    return request.post('/admin/user/create', data)
  },
  
  // 更新用户信息
  updateUser(userId, data) {
    return request.put(`/admin/user/${userId}`, data)
  },
  
  // 删除用户（逻辑删除）
  deleteUser(userId) {
    return request.delete(`/admin/user/${userId}`)
  },
  
  // 批量删除用户（逻辑删除）
  deleteUsers(userIds) {
    return request.delete('/admin/user/batch', { data: userIds })
  },
  
  // 启用/禁用用户
  updateUserStatus(userId, status) {
    return request.put(`/admin/user/${userId}/status`, null, {
      params: { status }
    })
  },
  
  // 重置用户密码
  resetPassword(userId, newPassword) {
    return request.put(`/admin/user/${userId}/reset-password`, null, {
      params: { newPassword }
    })
  },
  
  // 获取用户统计信息
  getUserStatistics() {
    return request.get('/admin/user/statistics')
  }
}
