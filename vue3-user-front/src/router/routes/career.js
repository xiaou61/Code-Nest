export const careerRoutes = [
  {
    path: '/mock-interview',
    name: 'MockInterview',
    component: () => import('@/views/mock-interview/Index.vue'),
    meta: {
      title: 'AI模拟面试',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/mock-interview/config',
    name: 'MockInterviewConfig',
    component: () => import('@/views/mock-interview/Config.vue'),
    meta: {
      title: '面试配置',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/mock-interview/session',
    name: 'MockInterviewSession',
    component: () => import('@/views/mock-interview/Interview.vue'),
    meta: {
      title: '面试进行中',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/mock-interview/report',
    name: 'MockInterviewReport',
    component: () => import('@/views/mock-interview/Report.vue'),
    meta: {
      title: '面试报告',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/mock-interview/history',
    name: 'MockInterviewHistory',
    component: () => import('@/views/mock-interview/History.vue'),
    meta: {
      title: '面试历史',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/job-battle',
    name: 'JobBattle',
    component: () => import('@/views/job-battle/Index.vue'),
    meta: {
      title: '求职作战台',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/job-match-engine',
    name: 'JobMatchEngine',
    component: () => import('@/views/job-battle/MatchEngine.vue'),
    meta: {
      title: '岗位匹配引擎 2.0',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/career-loop',
    name: 'CareerLoop',
    component: () => import('@/views/career-loop/Index.vue'),
    meta: {
      title: '求职闭环中台',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/sql-optimizer/workbench',
    name: 'SqlOptimizerWorkbench',
    component: () => import('@/views/sql-optimizer/Workbench.vue'),
    meta: {
      title: 'SQL优化工作台 2.0',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/growth-autopilot',
    name: 'GrowthAutopilot',
    redirect: (to) => ({
      path: '/learning-cockpit',
      query: {
        ...to.query,
        tab: 'autopilot'
      }
    }),
    meta: {
      title: '成长闭环自动驾驶',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/resume',
    name: 'ResumeHome',
    component: () => import('@/views/resume/MyResumes.vue'),
    meta: {
      title: '简历管理',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/resume/templates',
    name: 'ResumeTemplates',
    component: () => import('@/views/resume/TemplateCenter.vue'),
    meta: {
      title: '简历模板',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/resume/editor',
    name: 'ResumeCreate',
    component: () => import('@/views/resume/Editor.vue'),
    meta: {
      title: '创建简历',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/resume/editor/:id',
    name: 'ResumeEdit',
    component: () => import('@/views/resume/Editor.vue'),
    meta: {
      title: '编辑简历',
      requiresAuth: true,
      keepAlive: false
    }
  }
]
