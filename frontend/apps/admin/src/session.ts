import { createSessionReader } from '@paideia/core'

import { getToken } from './api'

/**
 * 会话来源。用与 API 客户端同一个 `getToken`，因此接入认证时不需要改这里。
 *
 * <p>注意 `createSessionReader` 解析令牌载荷但**不校验签名**：它只用于界面分流，
 * 不是授权边界。后端必须自己拦越权请求。
 */
export const sessionReader = createSessionReader({ getToken })
