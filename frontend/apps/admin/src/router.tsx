import { createHashRouter } from 'react-router'

import { AdminHomePage } from './pages/AdminHomePage'
import { KnowledgeAdminPage } from './pages/KnowledgeAdminPage'
import { RequireAdmin } from './require-admin'

/**
 * 管理端路由。
 *
 * <p>用 Hash 路由是为了与学习者端保持一致（同一套约定、同一份经验）；管理端只在 Web 上跑，
 * 所以这里不是被桌面壳逼的，而是"两端不要各有一套路由习惯"。
 *
 * <p>每个页面都包在 `RequireAdmin` 里，门只有一处。
 */
export const router = createHashRouter([
  {
    path: '/',
    element: (
      <RequireAdmin>
        <AdminHomePage />
      </RequireAdmin>
    ),
  },
  {
    path: '/knowledge',
    element: (
      <RequireAdmin>
        <KnowledgeAdminPage />
      </RequireAdmin>
    ),
  },
])
