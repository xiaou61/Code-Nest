import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { createCoreRoutes } from './routes/core'
import { createLearningRoutes } from './routes/learning'
import { createCommunityRoutes } from './routes/community'
import { createOperationsRoutes } from './routes/operations'
import { createSystemRoutes } from './routes/system'
import { createFallbackRoutes } from './routes/fallback'

const Layout = () => import('@/layout/index.vue')

const routes = [
  ...createCoreRoutes(Layout),
  ...createLearningRoutes(Layout),
  ...createCommunityRoutes(Layout),
  ...createOperationsRoutes(Layout),
  ...createSystemRoutes(Layout),
  ...createFallbackRoutes(Layout)
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  // 设置页面标题
  if (to.meta?.title) {
    document.title = `${to.meta.title} - Code Nest 管理后台`
  }
  
  // 检查是否需要登录（除了登录页，其他页面都需要登录）
  const requiresAuth = to.meta?.requiresAuth !== false // 默认需要登录
  
  if (requiresAuth && to.path !== '/login') {
    // 检查是否已登录
    if (!userStore.token || !userStore.isLoggedIn) {
      ElMessage.warning('请先登录')
      next({
        path: '/login',
        query: { redirect: to.fullPath } // 保存目标路径，登录后跳转回去
      })
      return
    }
  }
  
  // 如果已登录，访问登录页面时跳转到首页
  if (to.path === '/login' && userStore.token && userStore.isLoggedIn) {
    next('/')
    return
  }
  
  next()
})

export default router 
