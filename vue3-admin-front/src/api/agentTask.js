import request from '@/utils/request'

function taskPath(taskId) {
  const encodedTaskId = encodeURIComponent(String(taskId ?? '').trim())
  return `/admin/agent/tasks/${encodedTaskId}`
}

export const agentTaskApi = {
  createTask(data, config = {}) {
    return request.post('/admin/agent/tasks', data, config)
  },

  listTasks(params = {}, config = {}) {
    return request.get('/admin/agent/tasks', params, config)
  },

  getTask(taskId, config = {}) {
    return request.get(`${taskPath(taskId)}`, {}, config)
  },

  getEvents(taskId, params = {}, config = {}) {
    return request.get(`${taskPath(taskId)}/events`, params, config)
  },

  pauseTask(taskId, data = {}, config = {}) {
    return request.post(`${taskPath(taskId)}/pause`, data, config)
  },

  resumeTask(taskId, config = {}) {
    return request.post(`${taskPath(taskId)}/resume`, {}, config)
  },

  submitInput(taskId, data, config = {}) {
    return request.post(`${taskPath(taskId)}/input`, data, config)
  },

  confirmTask(taskId, data, config = {}) {
    return request.post(`${taskPath(taskId)}/confirm`, data, config)
  },

  cancelTask(taskId, data = {}, config = {}) {
    return request.post(`${taskPath(taskId)}/cancel`, data, config)
  },
}

export default agentTaskApi
