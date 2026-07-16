import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { ElLoading } from 'element-plus'
import 'element-plus/es/components/loading/style/css'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'nprogress/nprogress.css'

import App from './App.vue'
import router from './router'
import './styles/index.scss'
import './styles/markdown.scss'
import { useTheme } from '@/design-system/composables/useTheme'

// 预加载面试数据
import { useInterviewStore } from '@/stores/interview'

const app = createApp(App)
const pinia = createPinia()
const { initializeTheme } = useTheme()

initializeTheme()
app.use(pinia)
app.use(router)
app.directive('loading', ElLoading.directive)

app.mount('#app')

// 预加载面试数据
router.isReady().then(() => {
  const interviewStore = useInterviewStore()
  const preload = () => {
    const currentPath = router.currentRoute.value.path
    if (currentPath.startsWith('/interview') || currentPath === '/') {
      interviewStore.preloadData()
    }
  }

  if (typeof window !== 'undefined' && typeof window.requestIdleCallback === 'function') {
    window.requestIdleCallback(preload, { timeout: 1500 })
    return
  }

  setTimeout(preload, 600)
})
