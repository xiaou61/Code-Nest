import request from '@/utils/request'

/**
 * 聊天室管理相关API
 */

// 获取消息列表（管理端）
export function getAdminMessageList(data) {
  return request.post('/admin/chat/messages/list', data)
}

// 删除消息
export function deleteMessage(id) {
  return request.delete(`/admin/chat/messages/${id}`)
}

// 批量删除消息
export function batchDeleteMessages(data) {
  return request.post('/admin/chat/messages/batch-delete', data)
}

// 获取在线用户列表（管理端）
export function getAdminOnlineUsers() {
  return request.post('/admin/chat/users/online')
}

// 获取用户当前生效禁言记录（管理端）
export function getActiveChatUserBan(userId) {
  return request.post('/admin/chat/users/ban/active', userId)
}

// 踢出用户
export function kickUser(userId) {
  return request.post('/admin/chat/users/kick', userId)
}

// 禁言用户
export function banUser(data) {
  return request.post('/admin/chat/users/ban', data)
}

// 解除禁言
export function unbanUser(userId) {
  return request.post('/admin/chat/users/unban', userId)
}

// 发送系统公告
export function sendAnnouncement(data) {
  return request.post('/admin/chat/announcement', data)
}
