export const learningRoutes = [
  {
    path: '/interview',
    name: 'Interview',
    component: () => import('@/views/interview/Index.vue'),
    meta: {
      title: '面试题库',
      requiresAuth: true,
      keepAlive: true // 开启页面缓存
    }
  },
  {
    path: '/interview/random',
    name: 'RandomQuestions',
    component: () => import('@/views/interview/RandomQuestions.vue'),
    meta: {
      title: '随机抽题',
      requiresAuth: true,
      keepAlive: false // 随机抽题页面不缓存，确保每次都是新的状态
    }
  },
  {
    path: '/interview/question-sets/:id',
    name: 'QuestionSetDetail',
    component: () => import('@/views/interview/QuestionSetDetail.vue'),
    meta: {
      title: '题单详情',
      requiresAuth: true,
      keepAlive: true // 开启页面缓存
    }
  },
  {
    path: '/interview/questions/:setId/:questionId',
    name: 'QuestionDetail',
    component: () => import('@/views/interview/QuestionDetail.vue'),
    meta: {
      title: '题目学习',
      requiresAuth: true,
      keepAlive: false // 题目页面不缓存，确保学习进度正确
    }
  },
  {
    path: '/interview/favorites',
    name: 'MyFavorites',
    component: () => import('@/views/interview/Favorites.vue'),
    meta: {
      title: '我的收藏',
      requiresAuth: true,
      keepAlive: true // 开启页面缓存
    }
  },
  {
    path: '/interview/review',
    name: 'InterviewReview',
    component: () => import('@/views/interview/Review.vue'),
    meta: {
      title: '智能复习',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/oj',
    name: 'OJ',
    component: () => import('@/views/oj/Index.vue'),
    meta: {
      title: '在线判题',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/oj/contests',
    name: 'OjContests',
    component: () => import('@/views/oj/Contests.vue'),
    meta: {
      title: '赛事中心',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/oj/contests/:id',
    name: 'OjContestDetail',
    component: () => import('@/views/oj/ContestDetail.vue'),
    meta: {
      title: '赛事详情',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/oj/problem/:id',
    name: 'OjProblemDetail',
    component: () => import('@/views/oj/ProblemDetail.vue'),
    meta: {
      title: '题目详情',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/oj/submission/:id',
    name: 'OjSubmissionDetail',
    component: () => import('@/views/oj/SubmissionDetail.vue'),
    meta: {
      title: '提交详情',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/oj/my-submissions',
    name: 'OjMySubmissions',
    component: () => import('@/views/oj/MySubmissions.vue'),
    meta: {
      title: '我的提交',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/oj/statistics',
    name: 'OjStatistics',
    component: () => import('@/views/oj/Statistics.vue'),
    meta: {
      title: '做题统计',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/oj/playground',
    name: 'OjPlayground',
    component: () => import('@/views/oj/Playground.vue'),
    meta: {
      title: '练习场',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/oj/ranking',
    name: 'OjRanking',
    component: () => import('@/views/oj/Ranking.vue'),
    meta: {
      title: '排行榜',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/learning-cockpit',
    name: 'LearningCockpit',
    component: () => import('@/views/learning-cockpit/Index.vue'),
    meta: {
      title: '学习成长驾驶舱 2.0',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/growth-capabilities',
    name: 'GrowthCapabilities',
    component: () => import('@/views/growth-capabilities/Index.vue'),
    meta: {
      title: '成长能力图谱',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/learning-assets',
    name: 'LearningAssets',
    component: () => import('@/views/learning-assets/Index.vue'),
    meta: {
      title: '我的学习资产',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/knowledge',
    name: 'Knowledge',
    component: () => import('@/views/knowledge/Index.vue'),
    meta: {
      title: '知识图谱',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/knowledge/maps/:id',
    name: 'KnowledgeMapViewer',
    component: () => import('@/views/knowledge/MapViewer.vue'),
    props: true,
    meta: {
      title: '知识图谱学习',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/flashcard',
    name: 'Flashcard',
    component: () => import('@/views/flashcard/Index.vue'),
    meta: {
      title: '闪卡记忆',
      requiresAuth: false,
      keepAlive: true
    }
  },
  {
    path: '/flashcard/deck/:id',
    name: 'FlashcardDeckDetail',
    component: () => import('@/views/flashcard/DeckDetail.vue'),
    meta: {
      title: '卡组详情',
      requiresAuth: false,
      keepAlive: false
    }
  },
  {
    path: '/flashcard/study',
    name: 'FlashcardStudy',
    component: () => import('@/views/flashcard/Study.vue'),
    meta: {
      title: '闪卡学习',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/flashcard/study/:deckId',
    name: 'FlashcardDeckStudy',
    component: () => import('@/views/flashcard/Study.vue'),
    meta: {
      title: '卡组学习',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/flashcard/my',
    name: 'MyFlashcardDecks',
    component: () => import('@/views/flashcard/MyDecks.vue'),
    meta: {
      title: '我的卡组',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/flashcard/deck/create',
    name: 'CreateFlashcardDeck',
    component: () => import('@/views/flashcard/DeckEditor.vue'),
    meta: {
      title: '创建卡组',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/flashcard/deck/:id/edit',
    name: 'EditFlashcardDeck',
    component: () => import('@/views/flashcard/DeckEditor.vue'),
    meta: {
      title: '编辑卡组',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/flashcard/deck/:deckId/cards',
    name: 'FlashcardCards',
    component: () => import('@/views/flashcard/CardEditor.vue'),
    meta: {
      title: '管理闪卡',
      requiresAuth: true,
      keepAlive: false
    }
  }
]
