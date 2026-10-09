import '@paideia/ui/styles.css'

import { createWebPlatform } from '@paideia/platform-web'
import { ThemeProvider } from '@paideia/ui'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

import { AdminHomePage } from './pages/AdminHomePage'

/**
 * 组合根。管理端只有一屏，因此不引路由——真出现第二屏时再加，
 * 现在装一个只服务单页的路由是没人用的脚手架。
 */
const platform = createWebPlatform()

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
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
    <ThemeProvider storage={platform.cache}>
      <QueryClientProvider client={queryClient}>
        <AdminHomePage />
      </QueryClientProvider>
    </ThemeProvider>
  </StrictMode>,
)
