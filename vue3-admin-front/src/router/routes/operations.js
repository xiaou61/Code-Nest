export const createOperationsRoutes = (Layout) => [
  {
    path: '/logs',
    component: Layout,
    redirect: '/logs/login',
    meta: { title: '日志管理' },
    children: [
      {
        path: 'login',
        name: 'LoginLogs',
        component: () => import('@/views/logs/login/index.vue'),
        meta: { title: '登录日志' }
      },
      {
        path: 'operation',
        name: 'OperationLogs',
        component: () => import('@/views/logs/operation/index.vue'),
        meta: { title: '操作日志' }
      }
    ]
  },
  {
    path: '/sre',
    component: Layout,
    redirect: '/sre/incidents',
    meta: { title: 'SRE 运维中心' },
    children: [
      {
        path: 'incidents',
        name: 'SreIncidents',
        component: () => import('@/views/sre/incidents/index.vue'),
        meta: { title: '事故工作台' }
      }
    ]
  },
  {
    path: '/sensitive',
    component: Layout,
    redirect: '/sensitive/words',
    meta: { title: '敏感词管理' },
    children: [
      {
        path: 'words',
        name: 'SensitiveWords',
        component: () => import('@/views/sensitive/words/index.vue'),
        meta: { title: '词库管理' }
      },
      {
        path: 'whitelist',
        name: 'SensitiveWhitelist',
        component: () => import('@/views/sensitive/whitelist/index.vue'),
        meta: { title: '白名单管理' }
      },
      {
        path: 'strategy',
        name: 'SensitiveStrategy',
        component: () => import('@/views/sensitive/strategy/index.vue'),
        meta: { title: '策略配置' }
      },
      {
        path: 'statistics',
        name: 'SensitiveStatistics',
        component: () => import('@/views/sensitive/statistics/index.vue'),
        meta: { title: '统计分析' }
      },
      {
        path: 'source',
        name: 'SensitiveSource',
        component: () => import('@/views/sensitive/source/index.vue'),
        meta: { title: '词库来源' }
      },
      {
        path: 'version',
        name: 'SensitiveVersion',
        component: () => import('@/views/sensitive/version/index.vue'),
        meta: { title: '版本历史' }
      },
      {
        path: 'config',
        name: 'SensitiveConfig',
        component: () => import('@/views/sensitive/config/index.vue'),
        meta: { title: '配置管理' }
      }
    ]
  },
  {
    path: '/filestorage',
    component: Layout,
    redirect: '/filestorage/storage-config',
    meta: { title: '文件存储管理' },
    children: [
      {
        path: 'storage-config',
        name: 'StorageConfig',
        component: () => import('@/views/filestorage/storage-config/index.vue'),
        meta: { title: '存储配置' }
      },
      {
        path: 'file-management',
        name: 'FileManagement',
        component: () => import('@/views/filestorage/file-management/index.vue'),
        meta: { title: '文件管理' }
      },
      {
        path: 'migration',
        name: 'FileMigration',
        component: () => import('@/views/filestorage/migration/index.vue'),
        meta: { title: '文件迁移' }
      },
      {
        path: 'system-settings',
        name: 'SystemSettings',
        component: () => import('@/views/filestorage/system-settings/index.vue'),
        meta: { title: '系统设置' }
      }
    ]
  },
  {
    path: '/points',
    component: Layout,
    redirect: '/points/index',
    meta: { title: '积分管理' },
    children: [
      {
        path: 'index',
        name: 'PointsOverview',
        component: () => import('@/views/points/index.vue'),
        meta: { title: '积分概览' }
      },
      {
        path: 'users',
        name: 'PointsUsers',
        component: () => import('@/views/points/users.vue'),
        meta: { title: '积分排行' }
      },
      {
        path: 'details',
        name: 'PointsDetails',
        component: () => import('@/views/points/details.vue'),
        meta: { title: '积分明细' }
      },
      {
        path: 'grant',
        name: 'PointsGrant',
        component: () => import('@/views/points/grant.vue'),
        meta: { title: '积分发放' }
      }
    ]
  },
  {
    path: '/lottery',
    component: Layout,
    children: [
      {
        path: '',
        name: 'LotteryManagement',
        component: () => import('@/views/lottery/index.vue'),
        meta: { title: '抽奖管理' }
      }
    ]
  }
]
