import { createWebPlatform } from '@paideia/platform-web'
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

const container = document.getElementById('root')
if (container === null) {
  throw new Error('找不到挂载节点 #root')
}

createRoot(container).render(
  <StrictMode>
    <PlatformProvider platform={createWebPlatform()}>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </PlatformProvider>
  </StrictMode>,
)
