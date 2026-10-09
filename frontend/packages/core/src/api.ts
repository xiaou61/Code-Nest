export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
  traceId: string | null
}

/** 后端错误契约的客户端表示：保留错误码与追踪标识，便于把界面报错和日志对上。 */
export class ApiError extends Error {
  readonly code: number
  readonly traceId: string | null

  constructor(code: number, message: string, traceId: string | null) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.traceId = traceId
  }
}

export interface ApiClientOptions {
  /** 为空表示同源（开发期由 Vite 代理转发）。 */
  baseUrl?: string
  getToken?: () => string | null
  fetchImpl?: typeof fetch
}

export interface ApiClient {
  /** 取原始响应体，用于非统一结构的端点（例如 Actuator 健康检查）。 */
  request<T>(path: string, init?: RequestInit): Promise<T>
  /** 取统一结构的 data 字段；code 非 0 或 HTTP 失败时抛 ApiError。 */
  requestEnvelope<T>(path: string, init?: RequestInit): Promise<T>
}

const SUCCESS_CODE = 0

function isEnvelope(value: unknown): value is ApiEnvelope<unknown> {
  return typeof value === 'object' && value !== null && 'code' in value && 'message' in value
}

export function createApiClient(options: ApiClientOptions = {}): ApiClient {
  const baseUrl = options.baseUrl ?? ''
  const doFetch = options.fetchImpl ?? globalThis.fetch

  async function send(path: string, init: RequestInit = {}): Promise<Response> {
    const headers = new Headers(init.headers)
    headers.set('Accept', 'application/json')
    const token = options.getToken?.() ?? null
    if (token !== null && token !== '') {
      headers.set('Authorization', `Bearer ${token}`)
    }
    return doFetch(`${baseUrl}${path}`, { ...init, headers })
  }

  function parse(text: string): unknown {
    if (text === '') {
      return null
    }
    try {
      return JSON.parse(text) as unknown
    } catch {
      return undefined
    }
  }

  async function request<T>(path: string, init?: RequestInit): Promise<T> {
    const response = await send(path, init)
    const body = parse(await response.text())
    if (!response.ok) {
      if (isEnvelope(body)) {
        throw new ApiError(body.code, body.message, body.traceId)
      }
      throw new ApiError(response.status, `请求失败：HTTP ${response.status}`, null)
    }
    if (body === undefined) {
      throw new ApiError(response.status, '响应不是合法 JSON', null)
    }
    return body as T
  }

  async function requestEnvelope<T>(path: string, init?: RequestInit): Promise<T> {
    const envelope = await request<unknown>(path, init)
    if (!isEnvelope(envelope)) {
      throw new ApiError(-1, '响应不符合统一结构', null)
    }
    if (envelope.code !== SUCCESS_CODE) {
      throw new ApiError(envelope.code, envelope.message, envelope.traceId)
    }
    return envelope.data as T
  }

  return { request, requestEnvelope }
}
