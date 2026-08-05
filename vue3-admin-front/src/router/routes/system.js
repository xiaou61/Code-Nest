export const createSystemRoutes = (Layout) => [
  {
    path: '/system',
    component: Layout,
    redirect: '/system/version',
    meta: { title: '系统管理' },
    children: [
      {
        path: 'ai-config',
        name: 'AiConfig',
        component: () => import('@/views/system/ai-config/index.vue'),
        meta: { title: 'AI 配置与观测' }
      },
      {
        path: 'version',
        name: 'VersionManagement',
        component: () => import('@/views/system/version/index.vue'),
        meta: { title: '版本管理' }
      },
      {
        path: 'ai-governance',
        name: 'AiGovernance',
        component: () => import('@/views/system/ai-governance/index.vue'),
        meta: { title: 'AI质量治理' }
      }
    ]
  }
]
