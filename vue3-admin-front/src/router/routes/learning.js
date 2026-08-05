export const createLearningRoutes = (Layout) => [
  {
    path: '/interview',
    component: Layout,
    redirect: '/interview/categories',
    meta: { title: '面试题目管理' },
    children: [
      {
        path: 'categories',
        name: 'InterviewCategories',
        component: () => import('@/views/interview/categories/index.vue'),
        meta: { title: '分类管理' }
      },
      {
        path: 'question-sets',
        name: 'QuestionSets',
        component: () => import('@/views/interview/question-sets/index.vue'),
        meta: { title: '题单管理' }
      },
      {
        path: 'questions',
        name: 'Questions',
        component: () => import('@/views/interview/questions/index.vue'),
        meta: { title: '题目管理' }
      }
    ]
  },
  {
    path: '/mock-interview',
    component: Layout,
    redirect: '/mock-interview/sessions',
    meta: { title: '模拟面试运营' },
    children: [
      {
        path: 'sessions',
        name: 'MockInterviewSessions',
        component: () => import('@/views/mock-interview/sessions/index.vue'),
        meta: { title: '面试会话' }
      },
      {
        path: 'directions',
        name: 'MockInterviewDirections',
        component: () => import('@/views/mock-interview/directions/index.vue'),
        meta: { title: '方向配置' }
      }
    ]
  },
  {
    path: '/oj',
    component: Layout,
    redirect: '/oj/problems',
    meta: { title: 'OJ 判题管理' },
    children: [
      {
        path: 'problems',
        name: 'OjProblems',
        component: () => import('@/views/oj/problems/index.vue'),
        meta: { title: '题目管理' }
      },
      {
        path: 'problems/create',
        name: 'OjProblemCreate',
        component: () => import('@/views/oj/problems/edit.vue'),
        meta: { title: '新增题目' }
      },
      {
        path: 'problems/:id/edit',
        name: 'OjProblemEdit',
        component: () => import('@/views/oj/problems/edit.vue'),
        meta: { title: '编辑题目' },
        props: true
      },
      {
        path: 'contests',
        name: 'OjContests',
        component: () => import('@/views/oj/contests/index.vue'),
        meta: { title: '赛事管理' }
      },
      {
        path: 'contests/create',
        name: 'OjContestCreate',
        component: () => import('@/views/oj/contests/edit.vue'),
        meta: { title: '新增赛事' }
      },
      {
        path: 'contests/:id/edit',
        name: 'OjContestEdit',
        component: () => import('@/views/oj/contests/edit.vue'),
        meta: { title: '编辑赛事' },
        props: true
      },
      {
        path: 'tags',
        name: 'OjTags',
        component: () => import('@/views/oj/tags/index.vue'),
        meta: { title: '标签管理' }
      }
    ]
  },
  {
    path: '/resume',
    component: Layout,
    redirect: '/resume/templates',
    meta: { title: '简历中心' },
    children: [
      {
        path: 'templates',
        name: 'ResumeTemplates',
        component: () => import('@/views/resume/templates/index.vue'),
        meta: { title: '模板管理' }
      },
      {
        path: 'analytics',
        name: 'ResumeAnalytics',
        component: () => import('@/views/resume/analytics/index.vue'),
        meta: { title: '数据总览' }
      },
      {
        path: 'reports',
        name: 'ResumeReports',
        component: () => import('@/views/resume/reports/index.vue'),
        meta: { title: '健康巡检' }
      }
    ]
  },
  {
    path: '/knowledge',
    component: Layout,
    redirect: '/knowledge/maps',
    meta: { title: '知识图谱管理' },
    children: [
      {
        path: 'maps',
        name: 'KnowledgeMaps',
        component: () => import('@/views/knowledge/maps/index.vue'),
        meta: { title: '图谱管理' }
      },
      {
        path: 'maps/:id/edit',
        name: 'KnowledgeMapEdit',
        component: () => import('@/views/knowledge/maps/edit.vue'),
        meta: { title: '编辑图谱' },
        props: true
      }
    ]
  },
  {
    path: '/learning-assets',
    component: Layout,
    redirect: '/learning-assets/review',
    meta: { title: '学习资产管理' },
    children: [
      {
        path: 'review',
        name: 'LearningAssetsReview',
        component: () => import('@/views/learning-assets/review/index.vue'),
          meta: { title: '学习资产审核台' }
      },
      {
        path: 'statistics',
        name: 'LearningAssetsStatistics',
        component: () => import('@/views/learning-assets/statistics/index.vue'),
        meta: { title: '学习资产统计' }
      }
    ]
  }
]
