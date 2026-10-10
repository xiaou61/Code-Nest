import { RequireAuth } from '@paideia/auth'
import { createHashRouter } from 'react-router'

import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'

/**
 * 使用 Hash 路由：桌面壳从自定义协议加载页面，基于浏览器历史的路径路由在那种来源下会失效。
 * Hash 路由让同一套路由同时服务 Web 与桌面端，不必为两端维护两份路由配置。
 *
 * <p>学习者端**整体要求登录**：内容页统一包在 `RequireAuth` 里，登录页与注册页是仅有的两个
 * 不需要登录的路由。公开页不在本应用里——它是独立的 apps/public。
 *
 * <p>这条守卫只做界面分流，**不是授权边界**：真正的拦截在后端每个接口上。
 */
export const router = createHashRouter([
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  {
    path: '/',
    element: (
      <RequireAuth>
        <HomePage />
      </RequireAuth>
    ),
  },
])
