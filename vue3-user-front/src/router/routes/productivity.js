export const productivityRoutes = [
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: {
      title: '个人中心',
      requiresAuth: true
    }
  },
  {
    path: '/points',
    name: 'Points',
    component: () => import('@/views/points/Index.vue'),
    meta: {
      title: '我的积分',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/plan',
    name: 'PlanCheckin',
    component: () => import('@/views/plan/Index.vue'),
    meta: {
      title: '计划打卡',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/team',
    name: 'TeamSquare',
    component: () => import('@/views/team/Index.vue'),
    meta: {
      title: '小组广场',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/team/create',
    name: 'TeamCreate',
    component: () => import('@/views/team/Create.vue'),
    meta: {
      title: '创建小组',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/team/my',
    name: 'MyTeams',
    component: () => import('@/views/team/My.vue'),
    meta: {
      title: '我的小组',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/team/:id',
    name: 'TeamDetail',
    component: () => import('@/views/team/Detail.vue'),
    meta: {
      title: '小组详情',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/team/:id/edit',
    name: 'TeamEdit',
    component: () => import('@/views/team/Create.vue'),
    meta: {
      title: '编辑小组',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/lottery',
    name: 'Lottery',
    component: () => import('@/views/lottery/index.vue'),
    meta: {
      title: '幸运抽奖',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/version-history',
    name: 'VersionHistory',
    component: () => import('@/views/version/index.vue'),
    meta: {
      title: '版本更新历史',
      requiresAuth: false,
      keepAlive: true
    }
  },
  {
    path: '/dev-tools',
    name: 'DevTools',
    component: () => import('@/views/dev-tools/index.vue'),
    meta: {
      title: '程序员工具',
      requiresAuth: false,
      keepAlive: true
    }
  },
  {
    path: '/dev-tools/json',
    name: 'JsonTool',
    component: () => import('@/views/dev-tools/JsonTool.vue'),
    meta: {
      title: 'JSON工具',
      requiresAuth: false,
      keepAlive: true
    }
  },
  {
    path: '/dev-tools/text-diff',
    name: 'TextDiff',
    component: () => import('@/views/dev-tools/TextDiff.vue'),
    meta: {
      title: '文本比对',
      requiresAuth: false,
      keepAlive: true
    }
  },
  {
    path: '/dev-tools/translation',
    name: 'Translation',
    component: () => import('@/views/dev-tools/Translation.vue'),
    meta: {
      title: '聚合翻译',
      requiresAuth: false,
      keepAlive: true
    }
  },
  {
    path: '/moyu-tools',
    name: 'MoyuTools',
    component: () => import('@/views/moyu-tools/index.vue'),
    meta: {
      title: '摸鱼工具',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/moyu-tools/hot-topics',
    name: 'HotTopics',
    component: () => import('@/views/moyu-tools/HotTopics.vue'),
    meta: {
      title: '今日热榜',
      requiresAuth: false,
      keepAlive: true
    }
  },
  {
    path: '/moyu-tools/salary-calculator',
    name: 'SalaryCalculator',
    component: () => import('@/views/moyu-tools/SalaryCalculator.vue'),
    meta: {
      title: '时薪计算器',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/moyu-tools/calendar',
    name: 'DeveloperCalendar',
    component: () => import('@/views/moyu-tools/DeveloperCalendar.vue'),
    meta: {
      title: '程序员日历',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/moyu-tools/daily-content',
    name: 'DailyContent',
    component: () => import('@/views/moyu-tools/DailyContent.vue'),
    meta: {
      title: '每日内容',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/moyu-tools/bug-store',
    name: 'BugStore',
    component: () => import('@/views/moyu-tools/bug-store.vue'),
    meta: {
      title: 'Bug商店',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/codepen',
    name: 'CodePenSquare',
    component: () => import('@/views/codepen/Index.vue'),
    meta: {
      title: '代码广场',
      requiresAuth: false,
      keepAlive: true
    }
  },
  {
    path: '/codepen/editor',
    name: 'CodePenEditor',
    component: () => import('@/views/codepen/Editor.vue'),
    meta: {
      title: '代码编辑器',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/codepen/editor/:id',
    name: 'CodePenEditorEdit',
    component: () => import('@/views/codepen/Editor.vue'),
    meta: {
      title: '编辑作品',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/codepen/my',
    name: 'MyCodePens',
    component: () => import('@/views/codepen/MyPens.vue'),
    meta: {
      title: '我的作品',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/codepen/:id',
    name: 'CodePenDetail',
    component: () => import('@/views/codepen/Detail.vue'),
    meta: {
      title: '作品详情',
      requiresAuth: false,
      keepAlive: false
    }
  }
]
