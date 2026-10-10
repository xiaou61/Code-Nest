import { AuthProvider, useAuth } from '@paideia/auth'
import { ThemeProvider } from '@paideia/ui'
import '@paideia/ui/styles.css'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode, useEffect } from 'react'
import { createRoot } from 'react-dom/client'

import { auth, platform } from './auth'
import { AdminHomePage } from './pages/AdminHomePage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
      refetchOnWindowFocus: false,
    },
  },
})

/** 把认证上下文的刷新实现注册给带令牌的 fetch（原因见 apps/app/src/auth.ts 的说明）。 */
function RefreshBridge() {
  const { refreshSession } = useAuth()
  useEffect(() => {
    auth.registerRefresh(refreshSession)
  }, [refreshSession])
  return null
}

const container = document.getElementById('root')
if (container === null) {
  throw new Error('找不到挂载节点 #root')
}

createRoot(container).render(
  <StrictMode>
    <ThemeProvider storage={platform.cache}>
      <QueryClientProvider client={queryClient}>
        <AuthProvider tokens={auth.tokens} api={auth.authApi}>
          <RefreshBridge />
          <AdminHomePage />
        </AuthProvider>
      </QueryClientProvider>
    </ThemeProvider>
  </StrictMode>,
)
