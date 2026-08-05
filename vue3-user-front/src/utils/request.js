import axios from 'axios'
import {
  classifyApiFailure,
  unwrapApiResponse
} from '@code-nest/api-contract'
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
      
      // Sa-Token 自动续签机制
      // Sa-Token 在后端配置了 activity-timeout（30分钟无操作过期）和 timeout（7天总过期时间）
      // 每次请求都会自动续签，无需前端手动刷新 Token
      // 如果需要主动刷新，可以调用 /user/auth/refresh 接口
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

function showRequestError(message, config) {
  if (!config?.silent) {
    ElMessage.error(message)
  }
}

function handleApiError(error, config) {
  if (error.kind === 'authentication') {
    handleTokenError(error.message)
    return
  }

  showRequestError(error.message, config)
  if (error.code === 704) {
    handleLogout()
  }
}

// 响应拦截器
// 注意：Sa-Token 已经在后端配置了持久化机制
// activity-timeout: -1（不启用活动超时）
// timeout: 7天总过期时间
// 前端无需手动刷新 Token，Sa-Token 会自动处理
service.interceptors.response.use(
  (response) => {
    NProgress.done()

    try {
      return unwrapApiResponse(response.data, { httpStatus: response.status })
    } catch (error) {
      const apiError = classifyApiFailure(error)
      handleApiError(apiError, response.config)
      return Promise.reject(apiError)
    }
  },
  (error) => {
    NProgress.done()
    console.error('响应错误:', error)

    const apiError = classifyApiFailure(error)
    handleApiError(apiError, error.config)
    return Promise.reject(apiError)
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
