import { createWebPlatform } from '@paideia/platform-web'
import { createDesktopPlatform } from '@paideia/platform-desktop'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router'

import { PlatformProvider } from './platform'
import { router } from './router'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // 骨架期不做失败重试：连不上后端时希望立刻看到原因，而不是等几次重试
      retry: false,
      refetchOnWindowFocus: false,
    },
  },
})

/**
 * 组合根：只有这里知道宿主是什么，其余代码只依赖 Platform 端口。
 *
 * 两种实现都在此静态引入——platform-desktop 不 import Electron 的任何东西，
 * 它只读 preload 注入的 window.paideiaDesktop，因此打进 Web 产物也无害且不会被调用。
 */
const platform = import.meta.env.MODE === 'desktop' ? createDesktopPlatform() : createWebPlatform()

const container = document.getElementById('root')
if (container === null) {
  throw new Error('找不到挂载节点 #root')
}

createRoot(container).render(
  <StrictMode>
    <PlatformProvider platform={platform}>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </PlatformProvider>
  </StrictMode>,
)
