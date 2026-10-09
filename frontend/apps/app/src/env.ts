/**
 * 后端基址。默认为空字符串，表示同源——开发期由 Vite 代理转发，避免跨域配置。
 * 桌面端没有代理，构建时通过 VITE_API_BASE_URL 注入后端地址。
 */
export const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? ''
