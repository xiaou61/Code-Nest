import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'nprogress/nprogress.css'

import App from './App.vue'
import router from './router'
import './styles/index.scss'
import './styles/markdown.scss'

// 预加载面试数据
import { useInterviewStore } from '@/stores/interview'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

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
