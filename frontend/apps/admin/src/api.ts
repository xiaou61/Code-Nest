import { auth } from './auth'

/**
 * 页面使用的数据客户端，由认证接线装配：带 `Authorization` 头，遇到 401 刷新一次后重放。
 * 页面不直接碰令牌。
 */
export const api = auth.api
