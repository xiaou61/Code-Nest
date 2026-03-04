import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

// 路由配置
const routes = [
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
  // ============ 在线判题 ============
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
  // ============ 模拟面试 ============
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
  },
  {
    path: '/community',
    name: 'Community',
    component: () => import('@/views/community/Index.vue'),
    meta: {
      title: '技术社区',
      requiresAuth: true,
      keepAlive: true // 开启页面缓存
    }
  },
  {
    path: '/community/posts/:id',
    name: 'PostDetail',
    component: () => import('@/views/community/PostDetail.vue'),
    meta: {
      title: '帖子详情',
      requiresAuth: true,
      keepAlive: false // 帖子详情不缓存，确保数据实时
    }
  },
  {
    path: '/community/collections',
    name: 'MyCollections',
    component: () => import('@/views/community/Collections.vue'),
    meta: {
      title: '我的收藏',
      requiresAuth: true,
      keepAlive: true // 开启页面缓存
    }
  },
  {
    path: '/community/my-posts',
    name: 'MyPosts',
    component: () => import('@/views/community/MyPosts.vue'),
    meta: {
      title: '我的帖子',
      requiresAuth: true,
      keepAlive: true // 开启页面缓存
    }
  },
  {
    path: '/community/create',
    name: 'CreatePost',
    component: () => import('@/views/community/CreatePost.vue'),
    meta: {
      title: '创作帖子',
      requiresAuth: true,
      keepAlive: false // 不缓存，确保每次都是新的编辑状态
    }
  },
  {
    path: '/community/users/:userId',
    name: 'CommunityUserProfile',
    component: () => import('@/views/community/UserProfile.vue'),
    meta: {
      title: '用户主页',
      requiresAuth: true,
      keepAlive: false // 不缓存，确保数据实时
    }
  },
  {
    path: '/notification',
    name: 'Notification',
    component: () => import('@/views/notification/index.vue'),
    meta: {
      title: '通知中心',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/moments',
    name: 'Moments',
    component: () => import('@/views/moments/Index.vue'),
    meta: {
      title: '朋友圈',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/moments/user/:userId',
    name: 'MomentUserProfile',
    component: () => import('@/views/moments/UserProfile.vue'),
    meta: {
      title: '用户主页',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/moments/my-favorites',
    name: 'MomentFavorites',
    component: () => import('@/views/moments/MyFavorites.vue'),
    meta: {
      title: '我的收藏',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('@/views/chat/Index.vue'),
    meta: {
      title: '聊天室',
      requiresAuth: true,
      keepAlive: false
    }
  },
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
  // ============ 学习小组 ============
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
    path: '/blog',
    name: 'MyBlog',
    component: () => import('@/views/blog/Index.vue'),
    meta: {
      title: '我的博客',
      requiresAuth: true,
      keepAlive: true
    }
  },
  {
    path: '/blog/editor',
    name: 'BlogEditor',
    component: () => import('@/views/blog/Editor.vue'),
    meta: {
      title: '写文章',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/blog/editor/:id',
    name: 'BlogEditorEdit',
    component: () => import('@/views/blog/Editor.vue'),
    meta: {
      title: '编辑文章',
      requiresAuth: true,
      keepAlive: false
    }
  },
  {
    path: '/blog/:userId',
    name: 'BlogHome',
    component: () => import('@/views/blog/BlogHome.vue'),
    meta: {
      title: '博客主页',
      requiresAuth: false,
      keepAlive: true
    }
  },
  {
    path: '/blog/:userId/article/:articleId',
    name: 'ArticleDetail',
    component: () => import('@/views/blog/ArticleDetail.vue'),
    meta: {
      title: '文章详情',
      requiresAuth: false,
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
  },
  // ============ 闪卡记忆系统 ============
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
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: {
      title: '页面不存在'
    }
  }
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
