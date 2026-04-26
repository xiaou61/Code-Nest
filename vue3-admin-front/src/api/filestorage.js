import request from '@/utils/request'

// 存储配置相关API
export const storageAPI = {
  // 获取存储配置列表
  getStorageConfigs(params) {
    return request.get('/admin/storage/configs', { params })
  },

  // 根据ID获取存储配置
  getStorageConfig(id) {
    return request.get(`/admin/storage/config/${id}`)
  },

  // 创建存储配置
  createStorageConfig(data) {
    return request.post('/admin/storage/config', data)
  },

  // 更新存储配置
  updateStorageConfig(id, data) {
    return request.put(`/admin/storage/config/${id}`, data)
  },

  // 删除存储配置
  deleteStorageConfig(id) {
    return request.delete(`/admin/storage/config/${id}`)
  },

  // 测试存储配置
  testStorageConfig(id) {
    return request.post(`/admin/storage/config/${id}/test`)
  },

  // 启用/禁用存储配置
  toggleStorageConfig(id, isEnabled) {
    return request.put(`/admin/storage/config/${id}/enable`, null, {
      params: { isEnabled }
    })
  },

  // 设置默认存储配置
  setDefaultStorageConfig(id) {
    return request.put(`/admin/storage/config/${id}/default`)
  },

  // 获取支持的存储类型
  getSupportedStorageTypes() {
    return request.get('/admin/storage/types')
  }
}

// 文件管理相关API
export const fileAPI = {
  // 获取文件列表
  getFileList(params) {
    return request.get('/admin/file/list', { params })
  },

  // 物理删除文件
  forceDeleteFile(id) {
    return request.delete(`/admin/file/${id}/force`)
  },

  // 移动文件到其他存储
  moveFile(id, targetStorageId) {
    return request.put(`/admin/file/${id}/move`, null, {
      params: { targetStorageId }
    })
  },

  // 获取文件统计信息
  getFileStatistics() {
    return request.get('/admin/file/statistics')
  },

  // 获取存储使用情况
  getStorageUsage() {
    return request.get('/admin/file/storage-usage')
  },

  // === 公共接口功能 ===
  
  // 单文件上传
  uploadSingle(file, moduleName, businessType = 'default') {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('moduleName', moduleName)
    formData.append('businessType', businessType)
    
    return request.post('/file/upload/single', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  // 批量文件上传
  uploadBatch(files, moduleName, businessType = 'default') {
    const formData = new FormData()
    files.forEach(file => {
      formData.append('files', file)
    })
    formData.append('moduleName', moduleName)
    formData.append('businessType', businessType)
    
    return request.post('/file/upload/batch', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  // 下载文件
  downloadFile(id) {
    return request.get(`/file/download/${id}`, {}, {
      responseType: 'blob'
    })
  },

  // 获取文件信息
  getFileInfo(id) {
    return request.get(`/file/info/${id}`)
  },

  // 获取文件访问URL
  getFileUrl(id, expireHours) {
    return request.get(`/file/url/${id}`, {
      params: expireHours ? { expireHours } : {}
    })
  },

  // 批量获取文件URL
  getFileUrls(fileIds) {
    return request.post('/file/urls', fileIds)
  },

  // 逻辑删除文件
  deleteFile(id, moduleName) {
    return request.delete(`/file/${id}`, {
      params: { moduleName }
    })
  },

  // 查询公共文件列表
  getPublicFileList(moduleName, businessType, pageNum = 1, pageSize = 10) {
    return request.get('/file/list', {
      params: { moduleName, businessType, pageNum, pageSize }
    })
  },

  // 检查文件是否存在
  checkFilesExists(fileIds) {
    return request.post('/file/exists', fileIds)
  }
}

// 文件迁移相关API
export const migrationAPI = {
  // 创建迁移任务
  createMigrationTask(data) {
    return request.post('/admin/file/migrate', data)
  },

  // 获取迁移任务详情
  getMigrationTask(id) {
    return request.get(`/admin/file/migration/${id}`)
  },

  // 获取迁移任务列表
  getMigrationTasks(params) {
    return request.get('/admin/file/migrations', { params })
  },

  // 执行迁移任务
  executeMigration(id) {
    return request.post(`/admin/file/migration/${id}/execute`)
  },

  // 停止迁移任务
  stopMigration(id) {
    return request.put(`/admin/file/migration/${id}/stop`)
  },

  // 删除迁移任务
  deleteMigrationTask(id) {
    return request.delete(`/admin/file/migration/${id}`)
  },

  // 获取迁移进度
  getMigrationProgress(id) {
    return request.get(`/admin/file/migration/${id}/progress`)
  }
}

// 系统设置相关API
export const systemAPI = {
  // 获取系统设置
  getSystemSettings() {
    return request.get('/admin/system/settings')
  },

  // 更新系统设置
  updateSystemSettings(data) {
    return request.put('/admin/system/settings', data)
  },

  // 获取允许的文件类型
  getFileTypes() {
    return request.get('/admin/system/file-types')
  },

  // 更新文件类型白名单
  updateFileTypes(data) {
    return request.put('/admin/system/file-types', data)
  },

  // 获取系统配置摘要
  getSystemSummary() {
    return request.get('/admin/system/summary')
  }
}
