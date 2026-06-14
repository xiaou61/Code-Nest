import { defineConfig, DefaultTheme } from 'vitepress'

type SidebarItem = DefaultTheme.SidebarItem

const guideGettingStarted: SidebarItem[] = [
  { text: '快速开始', link: '/guide/quick-start' },
  { text: '本地开发', link: '/guide/local-dev' },
  { text: '本地完整启动剧本', link: '/guide/startup-playbook' },
  { text: '常见问题（FAQ）', link: '/guide/faq' }
]

const guideLearningPaths: SidebarItem[] = [
  { text: '学习路线', link: '/guide/learning-paths' },
  { text: '按模块学习路线', link: '/guide/module-learning-paths' },
  { text: '模块学习任务模板', link: '/guide/module-learning-workbook' },
  { text: '模块学习评审表', link: '/guide/module-learning-rubric' },
  { text: '模块结业案例', link: '/guide/module-learning-capstone' }
]

const guideOnboarding: SidebarItem[] = [
  { text: '带教执行剧本', link: '/guide/onboarding-execution-playbook' },
  { text: '带教交付模板包', link: '/guide/onboarding-deliverable-templates' },
  { text: '角色上手任务包', link: '/guide/onboarding-tasks' },
  { text: '首个真实任务接入', link: '/guide/first-real-task' },
  { text: '首单任务题库', link: '/guide/first-task-backlog' }
]

const guideModuleOwnership: SidebarItem[] = [
  { text: '模块接手升级路线', link: '/guide/module-ownership-ladder' },
  { text: '模块接手检查清单', link: '/guide/module-takeover-checklist' },
  { text: '模块接手样例包', link: '/guide/module-takeover-examples' },
  { text: '模块值守与回归手册', link: '/guide/module-owner-playbook' }
]

const guideDevelopment: SidebarItem[] = [
  { text: '功能开发流程', link: '/guide/feature-development' },
  { text: '常见二开场景', link: '/guide/extension-scenarios' },
  { text: '测试与回归', link: '/guide/testing-regression' },
  { text: '开发者规范', link: '/guide/developer-standards' }
]

const guideRelease: SidebarItem[] = [
  { text: 'Git Log 版更新记录', link: '/guide/git-log-release-notes' },
  { text: '生产 CI/CD 自动发布', link: '/guide/ci-cd-production' },
  { text: '版本公告与交接模板', link: '/guide/version-release-handoff-template' },
  { text: '版本交付实战样例', link: '/guide/version-release-worked-examples' },
  { text: '发布前验证', link: '/guide/release-verification' }
]

const guideSecurity: SidebarItem[] = [
  { text: '权限与安全边界', link: '/guide/security-boundaries' },
  { text: '独立部署', link: '/guide/deploy' },
  { text: '文档维护规范', link: '/guide/documentation-maintenance' }
]

const guideApi: SidebarItem[] = [
  { text: 'API 认证指南', link: '/guide/api-auth' },
  { text: '数据库迁移指南', link: '/guide/database-migration' }
]

const guideCommunity: SidebarItem[] = [
  { text: '贡献指南', link: '/guide/contributing' },
  { text: '行为准则', link: '/guide/code-of-conduct' },
  { text: '安全策略', link: '/guide/security-policy' },
  { text: '发布流程', link: '/guide/release-process' },
  { text: '变更日志', link: '/guide/changelog' }
]

const architecture = [
  { text: '架构总览', link: '/architecture/overview' },
  { text: '后端模块', link: '/architecture/backend-modules' },
  { text: '前端应用', link: '/architecture/frontend-apps' },
  { text: '数据库与脚本', link: '/architecture/database' }
]

const modulesCore: SidebarItem[] = [
  { text: '模块总览', link: '/modules/' },
  { text: '公共底座', link: '/modules/common' },
  { text: '工具类详解', link: '/modules/utils' },
  { text: '鉴权与用户体系', link: '/modules/auth' },
  { text: '用户账户与个人中心', link: '/modules/user-account' },
  { text: 'AI Runtime', link: '/modules/ai-runtime' }
]

const modulesLearning: SidebarItem[] = [
  { text: '题库与成长闭环', link: '/modules/interview-and-growth' },
  { text: '面试题库', link: '/modules/interview' },
  { text: '模拟面试与求职作战台', link: '/modules/mock-interview-job-battle' },
  { text: '学习资产', link: '/modules/learning-assets' },
  { text: '闪卡', link: '/modules/flashcard' },
  { text: '计划与学习小组', link: '/modules/plan-team' },
  { text: '知识图谱', link: '/modules/knowledge' },
  { text: 'SQL 优化工作台', link: '/modules/sql-optimizer' },
  { text: 'OJ 判题系统', link: '/modules/oj' }
]

const modulesContent: SidebarItem[] = [
  { text: '社区与内容矩阵', link: '/modules/community-content' },
  { text: '社区帖子', link: '/modules/community' },
  { text: '动态广场', link: '/modules/moments' },
  { text: '博客', link: '/modules/blog' },
  { text: '代码工坊', link: '/modules/codepen' },
  { text: 'IM 聊天室', link: '/modules/chat' },
  { text: '简历系统', link: '/modules/resume' }
]

const modulesPlatform: SidebarItem[] = [
  { text: '积分与抽奖', link: '/modules/points' },
  { text: '文件存储', link: '/modules/file-storage' },
  { text: '通知中心', link: '/modules/notification' },
  { text: '敏感词风控', link: '/modules/sensitive' }
]

const modulesTools: SidebarItem[] = [
  { text: '工具、摸鱼与版本', link: '/modules/tools-moyu-version' },
  { text: '开发者工具', link: '/modules/dev-tools' },
  { text: '摸鱼工具', link: '/modules/moyu' },
  { text: '版本历史', link: '/modules/version-history' },
  { text: '仪表盘与日志', link: '/modules/dashboard-logs' },
  { text: '系统运营后台', link: '/modules/system-ops' }
]

const operations = [
  { text: 'Docker 与服务部署', link: '/operations/docker' },
  { text: '环境变量总表', link: '/operations/env-vars' },
  { text: '监控与观测', link: '/operations/monitoring' },
  { text: '告警 Runbook', link: '/operations/alert-runbooks' },
  { text: '事故响应', link: '/operations/incident-response' },
  { text: '问题定位流程', link: '/operations/diagnosis-flow' },
  { text: '常见问题排查', link: '/operations/troubleshooting' }
]

const referenceApi: SidebarItem[] = [
  { text: '术语表', link: '/reference/glossary' },
  { text: 'API 路由索引', link: '/reference/api-routes' },
  { text: 'API 调用示例', link: '/reference/api-examples' },
  { text: '前端路由索引', link: '/reference/frontend-routes' },
  { text: '响应体与错误码', link: '/reference/response-errors' },
  { text: 'WebSocket 协议', link: '/reference/websocket' }
]

const referenceData: SidebarItem[] = [
  { text: '数据表索引', link: '/reference/database-tables' },
  { text: '数据库字段阅读指南', link: '/reference/database-field-guide' },
  { text: '源码地图', link: '/reference/source-map' },
  { text: '模块依赖地图', link: '/reference/module-dependencies' }
]

const referenceSystem: SidebarItem[] = [
  { text: 'AI Schema 与治理', link: '/reference/ai-schemas' },
  { text: '权限注解与角色边界索引', link: '/reference/permission-boundaries' },
  { text: '异常路径与失败态索引', link: '/reference/failure-paths' },
  { text: '事件、通知与回流索引', link: '/reference/event-backflow-index' },
  { text: '统计、排行与计数口径索引', link: '/reference/statistics-ranking-counts' },
  { text: '幂等、回滚与补偿索引', link: '/reference/idempotency-rollbacks-compensation' },
  { text: '前端渲染安全', link: '/reference/frontend-rendering-security' },
  { text: '模块状态机与生命周期索引', link: '/reference/module-state-machines' }
]

const referenceQuality: SidebarItem[] = [
  { text: '模块最小回归矩阵', link: '/reference/module-regression-matrix' },
  { text: '全功能覆盖矩阵', link: '/reference/feature-coverage' },
  { text: '文档同步基线', link: '/reference/docs-sync-baseline' }
]

const manuals = [
  { text: '用户端操作手册', link: '/manuals/user-operations' },
  { text: '管理端操作手册', link: '/manuals/admin-operations' },
  { text: '端到端业务链路图', link: '/manuals/business-flow-map' },
  { text: '核心链路教程', link: '/manuals/core-workflows' },
  { text: '验证记录与已知问题', link: '/manuals/verified-scenarios' }
]

export default defineConfig({
  lang: 'zh-CN',
  title: 'Code Nest 文档',
  description: 'Code Nest v2.3.1 文档工作线',
  base: process.env.VITEPRESS_BASE || '/',
  cleanUrls: true,
  lastUpdated: true,
  sitemap: {
    hostname: process.env.SITEMAP_HOSTNAME || 'https://code-nest.example.com' // 替换为实际域名
  },
  markdown: {
    lineNumbers: true
  },
  git: {
    contributor: true
  },
  vite: {
    build: {
      chunkSizeWarningLimit: 1000,
      rollupOptions: {
        output: {
          manualChunks: {
            'vitepress-core': ['vitepress']
          }
        }
      }
    },
    optimizeDeps: {
      include: ['vue', '@vueuse/core']
    }
  },
  head: [
    ['meta', { name: 'theme-color', content: '#2563eb' }],
    ['link', { rel: 'icon', type: 'image/svg+xml', href: '/code-nest-mark.svg' }],
    ['meta', { name: 'author', content: 'Code Nest Team' }],
    ['meta', { property: 'og:type', content: 'website' }],
    ['meta', { property: 'og:site_name', content: 'Code Nest 文档' }],
    ['meta', { property: 'og:title', content: 'Code Nest - 面向开发者成长社区的全量开发与运维手册' }],
    ['meta', { property: 'og:description', content: '后端 15 个模块、前端双端、文档站独立部署的完整技术文档' }],
    ['meta', { name: 'twitter:card', content: 'summary_large_image' }],
    ['meta', { name: 'twitter:title', content: 'Code Nest - 面向开发者成长社区的全量开发与运维手册' }],
    ['meta', { name: 'twitter:description', content: '后端 15 个模块、前端双端、文档站独立部署的完整技术文档' }]
  ],
  themeConfig: {
    logo: '/code-nest-mark.svg',
    nav: [
      { text: '指南', link: '/guide/quick-start' },
      { text: '架构', link: '/architecture/overview' },
      { text: '模块', link: '/modules/' },
      { text: '参考', link: '/reference/api-routes' },
      { text: '手册', link: '/manuals/user-operations' },
      { text: '运维', link: '/operations/docker' },
      { text: 'v2.2.1 计划', link: '/roadmap/v2.2.1-docs-plan' }
    ],
  sidebar: {
    '/guide/': [
      { text: '入门指南', items: guideGettingStarted },
      { text: '学习路线', items: guideLearningPaths },
      { text: '带教与首单', items: guideOnboarding },
      { text: '模块接手', items: guideModuleOwnership },
      { text: '开发流程', items: guideDevelopment },
      { text: '版本发布', items: guideRelease },
      { text: '安全与部署', items: guideSecurity },
      { text: 'API 与数据库', items: guideApi },
      { text: '社区与治理', items: guideCommunity }
    ],
    '/architecture/': [{ text: '系统架构', items: architecture }],
    '/modules/': [
      { text: '核心与公共', items: modulesCore },
      { text: '学习成长', items: modulesLearning },
      { text: '内容与社交', items: modulesContent },
      { text: '平台能力', items: modulesPlatform },
      { text: '工具与运营', items: modulesTools }
    ],
    '/reference/': [
      { text: 'API 与协议', items: referenceApi },
      { text: '数据与源码', items: referenceData },
      { text: '系统设计', items: referenceSystem },
      { text: '质量保障', items: referenceQuality }
    ],
    '/api/': [
      {
        text: '用户端 API',
        items: [
          { text: '账号与系统', link: '/api/account-system' },
          { text: '学习成长', link: '/api/learning-growth' },
          { text: 'OJ 判题', link: '/api/oj' },
          { text: '内容社区', link: '/api/content-community' },
          { text: '平台能力', link: '/api/platform' },
          { text: '工具与运营', link: '/api/tools-operations' }
        ]
      },
      {
        text: '管理端 API',
        items: [
          { text: '积分与抽奖', link: '/api/admin-points-lottery' },
          { text: '内容社区', link: '/api/admin-content-community' },
          { text: '平台能力', link: '/api/admin-platform' },
          { text: 'AI Runtime', link: '/api/admin-ai-runtime' }
        ]
      }
    ],
    '/manuals/': [{ text: '操作手册', items: manuals }],
    '/operations/': [{ text: '部署运维', items: operations }],
    '/roadmap/': [
      {
        text: '文档路线图',
        items: [
          { text: 'v2.2.1 文档计划', link: '/roadmap/v2.2.1-docs-plan' },
          { text: 'v2.2.0 文档计划', link: '/roadmap/v2.2.0-docs-plan' }
        ]
      }
    ]
  },
    search: {
      provider: 'local'
    },
    editLink: {
      pattern: 'https://github.com/xiaou61/Code-Nest/edit/master/docs-site/:path',
      text: '在 GitHub 上编辑此页'
    },
    outline: {
      level: [2, 3],
      label: '本页目录'
    },
    docFooter: {
      prev: '上一页',
      next: '下一页'
    },
    lastUpdated: {
      text: '最后更新',
      formatOptions: {
        dateStyle: 'medium',
        timeStyle: 'short'
      }
    },
    footer: {
      message: 'Code Nest v2.3.1 documentation workline.',
      copyright: 'MIT Licensed.'
    }
  }
})
