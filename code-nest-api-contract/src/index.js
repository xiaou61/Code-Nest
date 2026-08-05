const API_ERROR_MESSAGES = Object.freeze({
  authentication: '登录已过期，请重新登录',
  authorization: '权限不足',
  validation: '请求参数错误',
  'not-found': '请求的资源不存在',
  conflict: '当前状态不允许该操作',
  'rate-limit': '请求过于频繁，请稍后再试',
  timeout: '请求超时，请稍后重试',
  unavailable: '服务暂时不可用，请稍后重试',
  server: '服务器内部错误',
  network: '网络连接异常，请检查网络',
  business: '请求失败',
  unknown: '请求失败'
})

const CONFIG_KEYS = new Set([
  'adapter',
  'auth',
  'baseURL',
  'cancelToken',
  'data',
  'headers',
  'maxBodyLength',
  'maxContentLength',
  'onDownloadProgress',
  'onUploadProgress',
  'params',
  'paramsSerializer',
  'responseEncoding',
  'responseType',
  'silent',
  'signal',
  'timeout',
  'transformRequest',
  'transformResponse',
  'validateStatus',
  'withCredentials',
  'xsrfCookieName',
  'xsrfHeaderName'
])

export class ApiError extends Error {
  constructor(message, options = {}) {
    super(message || API_ERROR_MESSAGES.unknown)
    this.name = 'ApiError'
    this.kind = options.kind || 'unknown'
    this.code = Number.isInteger(options.code) ? options.code : null
    this.httpStatus = Number.isInteger(options.httpStatus) ? options.httpStatus : null
    this.cause = options.cause
  }
}

export function isApiResponse(value) {
  return isPlainObject(value) && Number.isInteger(value.code)
}

export function isSuccessfulHttpStatus(status) {
  return Number.isInteger(status) && status >= 200 && status < 300
}

export function unwrapApiResponse(body, options = {}) {
  const httpStatus = Number.isInteger(options.httpStatus) ? options.httpStatus : null

  if (isApiResponse(body)) {
    if (body.code === 200 && (httpStatus === null || isSuccessfulHttpStatus(httpStatus))) {
      return body.data
    }

    throw apiErrorFrom(body.code, httpStatus, body.message)
  }

  if (httpStatus !== null && !isSuccessfulHttpStatus(httpStatus)) {
    throw apiErrorFrom(null, httpStatus, null)
  }

  return body
}

export function classifyApiFailure(error) {
  if (error instanceof ApiError) {
    return error
  }

  const status = Number.isInteger(error?.response?.status) ? error.response.status : null
  const body = error?.response?.data
  if (status !== null) {
    const code = isApiResponse(body) ? body.code : null
    const message = isApiResponse(body) ? body.message : body?.message
    return apiErrorFrom(code, status, message, error)
  }

  if (error?.code === 'ECONNABORTED' || error?.code === 'ETIMEDOUT') {
    return new ApiError(API_ERROR_MESSAGES.timeout, { kind: 'timeout', cause: error })
  }

  if (error?.code === 'ERR_NETWORK') {
    return new ApiError(API_ERROR_MESSAGES.network, { kind: 'network', cause: error })
  }

  if (error instanceof Error) {
    return new ApiError(error.message, { kind: 'unknown', cause: error })
  }

  return new ApiError(API_ERROR_MESSAGES.unknown, { kind: 'unknown', cause: error })
}

export function getApiErrorMessage(error, fallback = '') {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}

export function normalizeQueryRequestConfig(input = {}, config = {}) {
  if (hasAxiosConfigKey(input)) {
    return { ...input, ...config }
  }

  return {
    ...config,
    params: input
  }
}

export function normalizeBodyRequestConfig(config = {}) {
  if (hasAxiosConfigKey(config)) {
    return config
  }

  return {
    params: config
  }
}

function apiErrorFrom(code, httpStatus, message, cause) {
  const kind = classifyErrorKind(code, httpStatus)
  return new ApiError(message || API_ERROR_MESSAGES[kind], {
    kind,
    code,
    httpStatus,
    cause
  })
}

function classifyErrorKind(code, httpStatus) {
  if (code === 705) return 'business'
  if ([701, 702].includes(code) || httpStatus === 401) return 'authentication'
  if ([703, 704].includes(code) || httpStatus === 403) return 'authorization'
  if ([400, 601].includes(code) || httpStatus === 400) return 'validation'
  if ([404, 602, 803].includes(code) || httpStatus === 404) return 'not-found'
  if ([409, 603, 604].includes(code) || httpStatus === 409) return 'conflict'
  if (code === 429 || httpStatus === 429) return 'rate-limit'
  if (code === 408 || httpStatus === 408) return 'timeout'
  if ([502, 503, 504].includes(httpStatus)) return 'unavailable'
  if ((httpStatus !== null && httpStatus >= 500) || code === 500) return 'server'
  if (code !== null) return 'business'
  return 'unknown'
}

function isPlainObject(value) {
  return Object.prototype.toString.call(value) === '[object Object]'
}

function hasAxiosConfigKey(value) {
  return isPlainObject(value) && Object.keys(value).some((key) => CONFIG_KEYS.has(key))
}
