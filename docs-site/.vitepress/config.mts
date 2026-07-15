import { defineConfig, type DefaultTheme } from 'vitepress'

type SidebarItem = DefaultTheme.SidebarItem

const guideSidebar: SidebarItem[] = [
  { text: '指南总览', link: '/guide/' },
  {
    text: '上手与环境',
    items: [
      { text: '快速开始', link: '/guide/quick-start' },
      { text: '本地开发', link: '/guide/local-dev' },
      { text: '本地完整启动剧本', link: '/guide/startup-playbook' },
      { text: '常见问题（FAQ）', link: '/guide/faq' }
    ]
  },
  {
    text: '学习与带教',
    items: [
      { text: '学习路线', link: '/guide/learning-paths' },
      { text: '按模块学习路线', link: '/guide/module-learning-paths' },
      { text: '模块学习任务模板', link: '/guide/module-learning-workbook' },
      { text: '模块学习评审表', link: '/guide/module-learning-rubric' },
      { text: '模块结业案例', link: '/guide/module-learning-capstone' },
      { text: '带教执行剧本', link: '/guide/onboarding-execution-playbook' },
      { text: '带教交付模板包', link: '/guide/onboarding-deliverable-templates' },
      { text: '角色上手任务包', link: '/guide/onboarding-tasks' },
      { text: '首个真实任务接入', link: '/guide/first-real-task' },
      { text: '首单任务题库', link: '/guide/first-task-backlog' }
    ]
  },
  {
    text: '模块接手',
    items: [
      { text: '模块接手升级路线', link: '/guide/module-ownership-ladder' },
      { text: '模块接手检查清单', link: '/guide/module-takeover-checklist' },
      { text: '模块接手样例包', link: '/guide/module-takeover-examples' },
      { text: '模块值守与回归手册', link: '/guide/module-owner-playbook' }
    ]
  },
  {
    text: '开发与质量',
    items: [
      { text: '功能开发流程', link: '/guide/feature-development' },
      { text: '常见二开场景', link: '/guide/extension-scenarios' },
      { text: '测试与回归', link: '/guide/testing-regression' },
      { text: '开发者规范', link: '/guide/developer-standards' },
      { text: '权限与安全边界', link: '/guide/security-boundaries' },
      { text: 'API 认证指南', link: '/guide/api-auth' },
      { text: '数据库迁移指南', link: '/guide/database-migration' }
    ]
  },
  {
    text: '发布与部署',
    items: [
      { text: '生产 CI/CD 自动发布', link: '/guide/ci-cd-production' },
      { text: '版本公告与交接模板', link: '/guide/version-release-handoff-template' },
      { text: '版本交付实战样例', link: '/guide/version-release-worked-examples' },
      { text: '发布前验证', link: '/guide/release-verification' },
      { text: '独立部署', link: '/guide/deploy' },
      { text: '发布流程', link: '/guide/release-process' },
      { text: 'Git Log 版更新记录', link: '/guide/git-log-release-notes' },
      { text: '变更日志', link: '/guide/changelog' }
    ]
  },
  {
    text: '协作与治理',
    items: [
      { text: '文档维护规范', link: '/guide/documentation-maintenance' },
      { text: '贡献指南', link: '/guide/contributing' },
      { text: '行为准则', link: '/guide/code-of-conduct' },
      { text: '安全策略', link: '/guide/security-policy' }
    ]
  }
]

const architectureSidebar: SidebarItem[] = [
  { text: '架构导航', link: '/architecture/' },
  { text: '整体架构', link: '/architecture/overview' },
  { text: '后端模块', link: '/architecture/backend-modules' },
  { text: '前端应用', link: '/architecture/frontend-apps' },
  { text: '数据库与脚本', link: '/architecture/database' }
]

const modulesSidebar: SidebarItem[] = [
  { text: '模块总览', link: '/modules/' },
  {
    text: '核心与公共',
    items: [
      { text: '公共底座', link: '/modules/common' },
      { text: '工具与基础设施', link: '/modules/utils' },
      { text: '鉴权与用户体系', link: '/modules/auth' },
      { text: '用户账户与个人中心', link: '/modules/user-account' },
      { text: 'AI Runtime', link: '/modules/ai-runtime' }
    ]
  },
  {
    text: '学习成长',
    items: [
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
  },
  {
    text: '内容与社交',
    items: [
      { text: '社区与内容矩阵', link: '/modules/community-content' },
      { text: '社区帖子', link: '/modules/community' },
      { text: '动态广场', link: '/modules/moments' },
      { text: '博客', link: '/modules/blog' },
      { text: '代码工坊', link: '/modules/codepen' },
      { text: 'IM 聊天室', link: '/modules/chat' },
      { text: '简历系统', link: '/modules/resume' }
    ]
  },
  {
    text: '平台能力',
    items: [
      { text: '积分与抽奖', link: '/modules/points' },
      { text: '文件存储', link: '/modules/file-storage' },
      { text: '通知中心', link: '/modules/notification' },
      { text: '敏感词风控', link: '/modules/sensitive' }
    ]
  },
  {
    text: '工具与运营',
    items: [
      { text: '工具、摸鱼与版本', link: '/modules/tools-moyu-version' },
      { text: '开发者工具', link: '/modules/dev-tools' },
      { text: '摸鱼工具', link: '/modules/moyu' },
      { text: '版本历史', link: '/modules/version-history' },
      { text: '仪表盘与日志', link: '/modules/dashboard-logs' },
      { text: '系统运营后台', link: '/modules/system-ops' }
    ]
  }
]

const apiSidebar: SidebarItem[] = [
  { text: 'API 导航', link: '/api/' },
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
]

const operationsSidebar: SidebarItem[] = [
  { text: '运维导航', link: '/operations/' },
  {
    text: '部署与配置',
    items: [
      { text: 'Docker 与服务部署', link: '/operations/docker' },
      { text: '环境变量总表', link: '/operations/env-vars' }
    ]
  },
  {
    text: '观测与响应',
    items: [
      { text: '监控与观测', link: '/operations/monitoring' },
      { text: '告警 Runbook', link: '/operations/alert-runbooks' },
      { text: '事故响应', link: '/operations/incident-response' },
      { text: '问题定位流程', link: '/operations/diagnosis-flow' },
      { text: '常见问题排查', link: '/operations/troubleshooting' }
    ]
  },
  {
    text: '验证记录',
    items: [
      { text: '线上接口业务正确性测试（2026-06-18）', link: '/operations/online-api-business-correctness-2026-06-18' }
    ]
  }
]

const referenceSidebar: SidebarItem[] = [
  { text: '参考总览', link: '/reference/' },
  {
    text: 'API 与协议',
    items: [
      { text: '术语表', link: '/reference/glossary' },
      { text: 'API 路由索引', link: '/reference/api-routes' },
      { text: 'API 调用示例', link: '/reference/api-examples' },
      { text: '前端路由索引', link: '/reference/frontend-routes' },
      { text: '响应体与错误码', link: '/reference/response-errors' },
      { text: 'WebSocket 协议', link: '/reference/websocket' }
    ]
  },
  {
    text: '数据与源码',
    items: [
      { text: '数据表索引', link: '/reference/database-tables' },
      { text: '数据库字段阅读指南', link: '/reference/database-field-guide' },
      { text: '环境变量索引', link: '/reference/env-vars' },
      { text: '源码地图', link: '/reference/source-map' },
      { text: '模块依赖地图', link: '/reference/module-dependencies' }
    ]
  },
  {
    text: '系统设计',
    items: [
      { text: 'AI Schema 与治理', link: '/reference/ai-schemas' },
      { text: '权限注解与角色边界', link: '/reference/permission-boundaries' },
      { text: '异常路径与失败态', link: '/reference/failure-paths' },
      { text: '事件、通知与回流', link: '/reference/event-backflow-index' },
      { text: '统计、排行与计数口径', link: '/reference/statistics-ranking-counts' },
      { text: '幂等、回滚与补偿', link: '/reference/idempotency-rollbacks-compensation' },
      { text: '前端渲染安全', link: '/reference/frontend-rendering-security' },
      { text: '模块状态机与生命周期', link: '/reference/module-state-machines' }
    ]
  },
  {
    text: '质量保障',
    items: [
      { text: '模块最小回归矩阵', link: '/reference/module-regression-matrix' },
      { text: '全功能覆盖矩阵', link: '/reference/feature-coverage' },
      { text: '文档同步基线', link: '/reference/docs-sync-baseline' }
    ]
  }
]

const manualsSidebar: SidebarItem[] = [
  { text: '手册导航', link: '/manuals/' },
  { text: '用户端操作手册', link: '/manuals/user-operations' },
  { text: '管理端操作手册', link: '/manuals/admin-operations' },
  { text: '端到端业务链路图', link: '/manuals/business-flow-map' },
  { text: '核心链路教程', link: '/manuals/core-workflows' },
  { text: '验证记录与已知问题', link: '/manuals/verified-scenarios' }
]

const roadmapSidebar: SidebarItem[] = [
  { text: '版本与历史', link: '/roadmap/' },
  { text: 'v2.2.1 文档计划（历史）', link: '/roadmap/v2.2.1-docs-plan' },
  { text: 'v2.2.0 文档计划（历史）', link: '/roadmap/v2.2.0-docs-plan' }
]

const base = process.env.VITEPRESS_BASE || '/'
const sitemapHostname = process.env.SITEMAP_HOSTNAME

export default defineConfig({
  lang: 'zh-CN',
  title: 'Code Nest 文档',
  description: 'Code Nest v2.4.1 工程文档中心：架构、模块、API、操作手册与运维参考。',
  base,
  cleanUrls: true,
  lastUpdated: true,
  sitemap: sitemapHostname ? { hostname: sitemapHostname } : undefined,
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
    ['meta', { name: 'theme-color', content: '#0f766e' }],
    ['link', { rel: 'icon', type: 'image/svg+xml', href: `${base}code-nest-mark.svg` }],
    ['meta', { name: 'author', content: 'Code Nest Team' }],
    ['meta', { property: 'og:type', content: 'website' }],
    ['meta', { property: 'og:site_name', content: 'Code Nest 文档' }],
    ['meta', { property: 'og:title', content: 'Code Nest 工程文档中心' }],
    ['meta', { property: 'og:description', content: '覆盖 28 个 Maven 子模块、双前端、API、部署与运维的工程手册。' }],
    ['meta', { name: 'twitter:card', content: 'summary' }],
    ['meta', { name: 'twitter:title', content: 'Code Nest 工程文档中心' }],
    ['meta', { name: 'twitter:description', content: '按角色、任务和系统边界组织的 Code Nest 文档。' }]
  ],
  themeConfig: {
    logo: '/code-nest-mark.svg',
    nav: [
      { text: '开始', link: '/guide/' },
      { text: '架构', link: '/architecture/' },
      { text: '模块', link: '/modules/' },
      { text: 'API', link: '/api/' },
      { text: '运维', link: '/operations/' },
      {
        text: '资料',
        items: [
          { text: '操作手册', link: '/manuals/' },
          { text: '参考索引', link: '/reference/' },
          { text: '版本与历史', link: '/roadmap/' }
        ]
      }
    ],
    sidebar: {
      '/guide/': guideSidebar,
      '/architecture/': architectureSidebar,
      '/modules/': modulesSidebar,
      '/api/': apiSidebar,
      '/operations/': operationsSidebar,
      '/reference/': referenceSidebar,
      '/manuals/': manualsSidebar,
      '/roadmap/': roadmapSidebar
    },
    search: {
      provider: 'local'
    },
    socialLinks: [
      { icon: 'github', link: 'https://github.com/xiaou61/Code-Nest' }
    ],
    externalLinkIcon: true,
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
    sidebarMenuLabel: '文档目录',
    returnToTopLabel: '返回顶部',
    darkModeSwitchLabel: '外观',
    lightModeSwitchTitle: '切换到浅色模式',
    darkModeSwitchTitle: '切换到深色模式',
    skipToContentLabel: '跳到正文',
    footer: {
      message: 'Code Nest v2.4.1 engineering documentation.',
      copyright: 'MIT Licensed.'
    }
  }
})
