import request from '@/utils/request'

export const logApi = {
  // ============ 登录日志 ============
  
  // 获取登录日志（分页）
  getLoginLogs(params) {
    return request.post('/log/login', params)
  },
  
  // 获取登录日志详情
  getLoginLogById(id) {
    return request.get(`/log/login/${id}`)
  },
  
  // 清空登录日志
  clearLoginLogs() {
    return request.delete('/log/login')
  },
  
  // ============ 操作日志 ============
  
  // 获取操作日志（分页）
  getOperationLogs(params) {
    return request.post('/log/operation', params)
  },
  
  // 获取操作日志详情
  getOperationLogById(id) {
    return request.get(`/log/operation/${id}`)
  },
  
  // 批量删除操作日志
  deleteOperationLogs(ids) {
    return request.delete('/log/operation', { data: ids })
  },
  
  // 清空操作日志
  clearOperationLogs() {
    return request.delete('/log/operation/all')
  },
  
  // 清理过期操作日志
  cleanOperationLogs(days) {
    return request.delete(`/log/operation/clean/${days}`)
  },
}
