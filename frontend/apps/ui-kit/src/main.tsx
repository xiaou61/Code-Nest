import '@paideia/ui/styles.css'

import { createWebPlatform } from '@paideia/platform-web'
import { ThemeProvider, Toaster } from '@paideia/ui'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

import { UiKitPage } from './pages/UiKitPage'

/**
 * 组合根。不引路由、不接后端：这一页只有一个主题。
 * 平台能力只需要缓存 —— 主题偏好的持久化走 Platform.cache，与其它应用同一条路径。
 */
const platform = createWebPlatform()

const container = document.getElementById('root')
if (container === null) {
  throw new Error('找不到挂载节点 #root')
}

createRoot(container).render(
  <StrictMode>
    <ThemeProvider storage={platform.cache}>
      <UiKitPage />
      <Toaster />
    </ThemeProvider>
  </StrictMode>,
)
