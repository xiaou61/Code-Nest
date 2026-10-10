import type { ApiClient } from '@paideia/core'

/**
 * 认证端点的调用。路径与后端 `AuthController` 一一对应。
 *
 * <p>用既有的 `ApiClient`（`requestEnvelope`）而不是裸 fetch：统一响应结构、错误码与
 * 追踪标识的解析都在那里，认证接口没有理由另搞一套。
 */

export type AuthRole = 'admin' | 'learner'

export interface AuthUser {
  id: string
  username: string
  email: string
  role: AuthRole
}

export interface AuthTokens {
  accessToken: string
  accessExpiresAt: string
  refreshToken: string
  refreshExpiresAt: string
  user: AuthUser
}

export interface CaptchaChallenge {
  captchaId: string
  imageBase64: string
  expiresAt: string
}

const JSON_HEADERS = { 'Content-Type': 'application/json' }

function post(body: unknown): RequestInit {
  return { method: 'POST', headers: JSON_HEADERS, body: JSON.stringify(body) }
}

export interface AuthApi {
  captcha(): Promise<CaptchaChallenge>
  sendEmailCode(email: string, captchaId: string, captchaAnswer: string): Promise<void>
  register(input: {
    username: string
    email: string
    password: string
    code: string
  }): Promise<AuthTokens>
  login(identifier: string, password: string): Promise<AuthTokens>
  refresh(refreshToken: string): Promise<AuthTokens>
  logout(refreshToken: string): Promise<void>
}

export function createAuthApi(client: ApiClient): AuthApi {
  return {
    captcha: () => client.requestEnvelope<CaptchaChallenge>('/api/v1/auth/captcha', { method: 'POST' }),

    sendEmailCode: (email, captchaId, captchaAnswer) =>
      client.requestEnvelope<void>(
        '/api/v1/auth/email-code',
        post({ email, captchaId, captchaAnswer }),
      ),

    register: (input) => client.requestEnvelope<AuthTokens>('/api/v1/auth/register', post(input)),

    login: (identifier, password) =>
      client.requestEnvelope<AuthTokens>('/api/v1/auth/login', post({ identifier, password })),

    refresh: (refreshToken) =>
      client.requestEnvelope<AuthTokens>('/api/v1/auth/refresh', post({ refreshToken })),

    logout: (refreshToken) =>
      client.requestEnvelope<void>('/api/v1/auth/logout', post({ refreshToken })),
  }
}
