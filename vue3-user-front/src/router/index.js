import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { coreRoutes } from './routes/core'
import { learningRoutes } from './routes/learning'
import { careerRoutes } from './routes/career'
import { communityRoutes } from './routes/community'
import { productivityRoutes } from './routes/productivity'
import { fallbackRoutes } from './routes/fallback'

// 路由配置
const routes = [
  ...coreRoutes,
  ...learningRoutes,
  ...careerRoutes,
  ...communityRoutes,
  ...productivityRoutes,
  ...fallbackRoutes
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  
  // 设置页面标题
  document.title = `${to.meta.title || '用户端'} - Code Nest`
  
  // 检查是否需要登录
  if (to.meta.requiresAuth) {
    if (!userStore.token || !userStore.isLoggedIn) {
      ElMessage.warning('请先登录')
      next('/login')
      return
    }
  }
  
  // 如果已登录，访问登录或注册页面时跳转到首页
  if ((to.path === '/login' || to.path === '/register') && userStore.token && userStore.isLoggedIn) {
    next('/')
    return
  }
  
  next()
})

export default router 
