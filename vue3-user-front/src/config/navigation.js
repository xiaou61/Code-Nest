import {
  ArrowDown,
  Bell,
  Calendar,
  ChatDotRound,
  Coffee,
  Connection,
  Cpu,
  DataAnalysis,
  Document,
  EditPen,
  HomeFilled,
  Message,
  Mic,
  Monitor,
  Picture,
  Postcard,
  Promotion,
  Reading,
  Tools,
  Trophy,
  UserFilled
} from '@element-plus/icons-vue'

export const primaryNavItems = [
  {
    path: '/',
    label: '今天',
    icon: HomeFilled,
    matchPrefixes: ['/']
  },
  {
    path: '/learning-cockpit',
    label: '学习',
    icon: DataAnalysis,
    matchPrefixes: [
      '/learning-cockpit',
      '/learning-assets',
      '/plan',
      '/flashcard',
      '/knowledge',
      '/interview',
      '/oj',
      '/sql-optimizer'
    ]
  },
  {
    path: '/career-loop',
    label: '求职',
    icon: Trophy,
    matchPrefixes: ['/career-loop', '/job-battle', '/job-match-engine', '/mock-interview', '/resume']
  },
  {
    path: '/community',
    label: '社区',
    icon: ChatDotRound,
    matchPrefixes: ['/community', '/moments', '/chat', '/team']
  }
]

export const learningMenuGroups = [
  {
    title: '学习工作台',
    items: [
      { path: '/learning-cockpit', label: '成长驾驶舱', desc: '成长分、能力雷达与今日任务闭环', icon: DataAnalysis, matchPrefixes: ['/learning-cockpit', '/growth-autopilot'] },
      { path: '/interview', label: '面试题库', desc: '题单学习与进度追踪', icon: Document, matchPrefixes: ['/interview'] },
      { path: '/learning-assets', label: '我的学习资产', desc: '管理闪卡、计划和候选资产', icon: Postcard, matchPrefixes: ['/learning-assets'] },
      { path: '/plan', label: '计划打卡', desc: '每日计划执行与复盘', icon: Calendar, matchPrefixes: ['/plan'] },
      { path: '/flashcard', label: '闪卡记忆', desc: '间隔复习强化长期记忆', icon: Postcard, matchPrefixes: ['/flashcard'] },
      { path: '/knowledge', label: '知识图谱', desc: '可视化构建知识体系', icon: Connection, matchPrefixes: ['/knowledge'] }
    ]
  },
  {
    title: '求职准备',
    items: [
      { path: '/career-loop', label: '求职闭环中台', desc: '统一追踪求职阶段与下一步行动', icon: DataAnalysis, matchPrefixes: ['/career-loop'] },
      { path: '/mock-interview', label: 'AI 模拟面试', desc: '真实问答与评分反馈', icon: Mic, matchPrefixes: ['/mock-interview'] },
      { path: '/job-battle', label: '求职作战台', desc: 'JD 解析到复盘的闭环训练', icon: Trophy, matchPrefixes: ['/job-battle'] },
      { path: '/job-match-engine', label: '岗位匹配引擎 2.0', desc: '多岗位并行评估与优先级排序', icon: Trophy, matchPrefixes: ['/job-match-engine'] },
      { path: '/resume', label: '简历工坊', desc: '多模板编辑与导出', icon: EditPen, matchPrefixes: ['/resume'] }
    ]
  }
]

export const practiceMenuGroups = [
  {
    title: '学习训练',
    items: [
      { path: '/interview', label: '面试题库', desc: '按题单完成面试训练', icon: Document, matchPrefixes: ['/interview'] },
      { path: '/flashcard', label: '闪卡记忆', desc: '间隔复习强化长期记忆', icon: Postcard, matchPrefixes: ['/flashcard'] },
      { path: '/plan', label: '计划打卡', desc: '执行今日学习计划', icon: Calendar, matchPrefixes: ['/plan'] },
      { path: '/knowledge', label: '知识图谱', desc: '整理知识体系与关联', icon: Connection, matchPrefixes: ['/knowledge'] }
    ]
  },
  {
    title: '实战与工具',
    items: [
      { path: '/oj', label: '在线判题', desc: '算法刷题与多语言判题', icon: Monitor, matchPrefixes: ['/oj', '/oj/problem/', '/oj/submission/', '/oj/my-submissions', '/oj/statistics', '/oj/ranking'] },
      { path: '/oj/contests', label: '赛事中心', desc: '周赛挑战与实时榜单', icon: Trophy, matchPrefixes: ['/oj/contests'] },
      { path: '/oj/playground', label: '练习场', desc: '独立运行调试代码片段', icon: Cpu, matchPrefixes: ['/oj/playground'] },
      { path: '/sql-optimizer/workbench', label: 'SQL 优化工作台', desc: '执行计划诊断与优化收益对比', icon: Cpu, matchPrefixes: ['/sql-optimizer'] },
      { path: '/dev-tools', label: '程序员工具', desc: 'JSON、Diff、翻译等常用工具', icon: Tools, matchPrefixes: ['/dev-tools'] }
    ]
  }
]

export const creationMenuItems = [
  { path: '/codepen', label: '代码共享器', desc: '在线编码与作品发布', icon: Promotion, matchPrefixes: ['/codepen'] },
  { path: '/blog', label: '我的博客', desc: '持续写作与技术表达', icon: Reading, matchPrefixes: ['/blog'] }
]

export const leisureMenuItems = [
  { path: '/moyu-tools', label: '摸鱼工具', desc: '热榜、日历和趣味工具', icon: Coffee, matchPrefixes: ['/moyu-tools'] },
  { path: '/lottery', label: '幸运抽奖', desc: '积分抽奖与活动奖励', icon: Trophy, matchPrefixes: ['/lottery'] },
  { path: '/version-history', label: '版本历史', desc: '平台版本与功能迭代', icon: DataAnalysis, matchPrefixes: ['/version-history'] }
]

export const moreMenuItems = [
  { path: '/moments', label: '朋友圈', desc: '浏览动态与学习分享', icon: Picture, matchPrefixes: ['/moments'] },
  { path: '/chat', label: '聊天室', desc: '实时交流技术问题', icon: Message, matchPrefixes: ['/chat'] },
  { path: '/team', label: '学习小组', desc: '组队监督与共学成长', icon: UserFilled, matchPrefixes: ['/team'] },
  { path: '/points', label: '积分中心', desc: '签到、积分和排行榜', icon: Trophy, matchPrefixes: ['/points'] },
  { path: '/notification', label: '通知中心', desc: '查看系统消息和提醒', icon: Bell, matchPrefixes: ['/notification'] },
  { path: '/profile', label: '个人中心', desc: '账号设置与信息维护', icon: UserFilled, matchPrefixes: ['/profile'] },
  ...leisureMenuItems
]

export const desktopDropdowns = [
  {
    key: 'practice',
    label: '练习与工具',
    icon: Tools,
    arrowIcon: ArrowDown,
    groups: practiceMenuGroups
  },
  {
    key: 'creation',
    label: '创作',
    icon: Tools,
    arrowIcon: ArrowDown,
    items: creationMenuItems
  },
  {
    key: 'more',
    label: '更多',
    icon: Coffee,
    arrowIcon: ArrowDown,
    items: moreMenuItems
  }
]

export const commandSections = [
  {
    key: 'main',
    title: '常用场景',
    items: [
      { path: '/', label: '今天', desc: '查看今日成长与热门内容', icon: HomeFilled },
      { path: '/learning-cockpit', label: '成长驾驶舱', desc: '聚合学习、计划与成长指标', icon: DataAnalysis },
      { path: '/career-loop', label: '求职闭环中台', desc: '按下一步行动推进求职准备', icon: Trophy },
      { path: '/community', label: '技术社区', desc: '看讨论、发帖子、追热门', icon: ChatDotRound }
    ]
  },
  {
    key: 'learning',
    title: '学习与训练',
    items: learningMenuGroups.flatMap((group) => group.items)
  },
  {
    key: 'creation',
    title: '创作与输出',
    items: creationMenuItems
  },
  {
    key: 'more',
    title: '更多与协作',
    items: moreMenuItems
  }
]

export const flattenCommandItems = (sections) => sections.flatMap((section) => section.items)
