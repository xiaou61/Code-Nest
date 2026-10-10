import { auth } from './auth'

/**
 * 页面使用的数据客户端。
 *
 * <p>由认证接线装配：带 `Authorization` 头，遇到 401 会刷新一次并重放原请求；
 * 令牌读取与刷新的实现都在 `./auth`，页面**不直接碰令牌**。
 */
export const api = auth.api
