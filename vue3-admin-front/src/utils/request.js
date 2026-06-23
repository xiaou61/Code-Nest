import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'
import NProgress from 'nprogress'
import { normalizeBodyRequestConfig, normalizeQueryRequestConfig } from '@/utils/request-options'

// 创建axios实例
const service = axios.create({
  baseURL: '/api',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
  },
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    NProgress.start()
    
    // 从store中获取token
    const userStore = useUserStore()
    const token = userStore.token
    
    // 如果有token，添加到请求头
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    
    return config
  },
  (error) => {
    NProgress.done()
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 标记是否正在处理token过期，避免重复弹窗
let isHandlingTokenExpired = false

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    NProgress.done()
    
    const { data, status } = response
    
    // HTTP状态码检查
    if (status !== 200) {
      ElMessage.error(`HTTP错误: ${status}`)
      return Promise.reject(new Error(`HTTP Error: ${status}`))
    }
    
    // 业务状态码检查
    if (data.code !== undefined) {
      const { code, message, data: responseData } = data
      
      // 成功响应
      if (code === 200) {
        return responseData
      }
      
      // Token相关错误 - 弹出提示后跳转登录页
      if (code === 701 || code === 702) {
        handleTokenError(message || '登录已过期，请重新登录')
        return Promise.reject(new Error(message))
      }
      
      // 权限不足（业务状态码 703）
      if (code === 703) {
        ElMessage.error(message || '权限不足')
        return Promise.reject(new Error(message || '权限不足'))
      }
      
      // 账户被禁用
      if (code === 704) {
        ElMessage.error(message)
        handleLogout()
        return Promise.reject(new Error(message))
      }
      
      // 其他业务错误
      ElMessage.error(message || '请求失败')
      return Promise.reject(new Error(message || '请求失败'))
    }
    
    return data
  },
  (error) => {
    NProgress.done()
    
    console.error('响应错误:', error)
    
    if (error.response) {
      const { status, data } = error.response
      
      switch (status) {
        case 401:
          handleTokenError('登录已过期，请重新登录')
          break
        case 403:
          ElMessage.error('权限不足')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        case 502:
        case 503:
        case 504:
          ElMessage.error('服务暂时不可用，请稍后重试')
          break
        default:
          ElMessage.error(data?.message || `请求失败 (${status})`)
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else if (error.code === 'ERR_NETWORK') {
      ElMessage.error('网络连接异常，请检查网络')
    } else {
      ElMessage.error('网络连接异常，请检查网络')
    }
    
    return Promise.reject(error)
  }
)

// 处理Token错误 - 弹出提示后跳转登录页
function handleTokenError(message) {
  // 防止重复弹窗
  if (isHandlingTokenExpired) {
    return
  }
  isHandlingTokenExpired = true
  
  ElMessageBox.alert(message, '登录过期', {
    confirmButtonText: '重新登录',
    type: 'warning',
    showClose: false,
    closeOnClickModal: false,
    closeOnPressEscape: false,
  }).then(() => {
    handleLogout()
  }).finally(() => {
    isHandlingTokenExpired = false
  })
}

// 处理登出
function handleLogout() {
  const userStore = useUserStore()
  userStore.logout()
  
  // 如果当前不在登录页，则跳转到登录页
  if (router.currentRoute.value.path !== '/login') {
    router.push('/login')
  }
}

// 封装常用的请求方法
const request = {
  get(url, params = {}, config = {}) {
    return service({
      method: 'GET',
      url,
      ...normalizeQueryRequestConfig(params, config),
    })
  },
  
  post(url, data = {}, config = {}) {
    return service({
      method: 'POST',
      url,
      data,
      ...normalizeBodyRequestConfig(config),
    })
  },
  
  put(url, data = {}, config = {}) {
    return service({
      method: 'PUT',
      url,
      data,
      ...normalizeBodyRequestConfig(config),
    })
  },

  patch(url, data = {}, config = {}) {
    return service({
      method: 'PATCH',
      url,
      data,
      ...normalizeBodyRequestConfig(config),
    })
  },
  
  delete(url, params = {}, config = {}) {
    return service({
      method: 'DELETE',
      url,
      ...normalizeQueryRequestConfig(params, config),
    })
  },
}

export default request
