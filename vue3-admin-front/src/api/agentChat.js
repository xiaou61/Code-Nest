import request from '@/utils/request'

export const agentChatApi = {
  sendMessage(data) {
    return request.post('/admin/agent/chat', data)
  }
}

export default agentChatApi
