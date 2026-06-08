import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'nprogress/nprogress.css'

import App from './App.vue'
import router from './router'
import './styles/index.scss'
import './styles/markdown.scss'
import { useTheme } from '@/design-system/composables/useTheme'

const app = createApp(App)

const pinia = createPinia()
const { initializeTheme } = useTheme()

initializeTheme()
app.use(pinia)
app.use(router)
app.use(ElementPlus)

app.mount('#app')
