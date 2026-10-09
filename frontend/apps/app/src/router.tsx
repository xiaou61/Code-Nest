import { createHashRouter } from 'react-router'

import { HomePage } from './pages/HomePage'

/**
 * 使用 Hash 路由：桌面壳从自定义协议加载页面，基于浏览器历史的路径路由在那种来源下会失效。
 * Hash 路由让同一套路由同时服务 Web 与桌面端，不必为两端维护两份路由配置。
 */
export const router = createHashRouter([{ path: '/', element: <HomePage /> }])
