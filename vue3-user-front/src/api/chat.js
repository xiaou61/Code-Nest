import request from '@/utils/request'

/**
 * 聊天室相关API
 */

// 获取历史消息
export function getChatHistory(data) {
  return request.post('/user/chat/history', data)
}

// 获取在线人数
export function getOnlineCount() {
  return request.post('/user/chat/online-count')
}

// 获取在线用户列表
export function getOnlineUsers() {
  return request.post('/user/chat/online-users')
}

// 撤回消息
export function recallMessage(data) {
  return request.post('/user/chat/message/recall', data)
}

// 获取 WebSocket 一次性握手票据
export function getWebSocketTicket() {
  return request.post('/user/chat/ws-ticket')
}

// 上传聊天图片
export function uploadChatImage(file, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('moduleName', 'chat')
  formData.append('businessType', 'message')
  
  return request.post('/file/upload/single', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percent)
      }
    }
  })
}
