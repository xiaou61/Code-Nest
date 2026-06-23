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
  'signal',
  'timeout',
  'transformRequest',
  'transformResponse',
  'validateStatus',
  'withCredentials',
  'xsrfCookieName',
  'xsrfHeaderName'
])

function isPlainObject(value) {
  return Object.prototype.toString.call(value) === '[object Object]'
}

function hasAxiosConfigKey(value) {
  return isPlainObject(value) && Object.keys(value).some((key) => CONFIG_KEYS.has(key))
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
