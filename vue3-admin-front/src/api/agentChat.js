import request from '@/utils/request'

export const agentChatApi = {
  sendMessage(data, config = {}) {
    return request.post('/admin/agent/chat', data, config)
  }
}

export default agentChatApi
