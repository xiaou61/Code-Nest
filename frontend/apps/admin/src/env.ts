/**
 * 后端基址。默认为空字符串，表示同源——开发期由 Vite 代理转发，避免跨域配置。
 * 与学习者端一致：管理端不在桌面壳里跑，所以没有桌面构建变量那一套。
 */
export const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? ''
