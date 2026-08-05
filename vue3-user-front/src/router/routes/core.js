export const coreRoutes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeRevamp.vue'),
    meta: {
      title: '首页',
      requiresAuth: true
    }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Auth.vue'),
    meta: {
      title: '登录',
      requiresAuth: false
    }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Auth.vue'),
    meta: {
      title: '注册',
      requiresAuth: false
    }
  },
  {
    path: '/onboarding',
    name: 'Onboarding',
    component: () => import('@/views/onboarding/Index.vue'),
    meta: {
      title: '制定第一周',
      requiresAuth: true
    }
  },
  {
    path: '/design-system/components',
    name: 'DesignSystemComponents',
    component: () => import('@/views/design-system/Components.vue'),
    meta: {
      title: 'Design System 组件验收',
      requiresAuth: false
    }
  }
]
