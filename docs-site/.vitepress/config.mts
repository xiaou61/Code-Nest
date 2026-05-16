import { defineConfig } from 'vitepress'

const guide = [
  { text: '快速开始', link: '/guide/quick-start' },
  { text: '本地开发', link: '/guide/local-dev' },
  { text: '独立部署', link: '/guide/deploy' },
  { text: '文档维护规范', link: '/guide/documentation-maintenance' }
]

const architecture = [
  { text: '架构总览', link: '/architecture/overview' },
  { text: '后端模块', link: '/architecture/backend-modules' },
  { text: '前端应用', link: '/architecture/frontend-apps' },
  { text: '数据库与脚本', link: '/architecture/database' }
]

const modules = [
  { text: '模块总览', link: '/modules/' },
  { text: '鉴权与用户体系', link: '/modules/auth' },
  { text: '用户账户与个人中心', link: '/modules/user-account' },
  { text: 'AI Runtime', link: '/modules/ai-runtime' },
  { text: '题库与成长闭环', link: '/modules/interview-and-growth' },
  { text: '面试题库', link: '/modules/interview' },
  { text: '模拟面试与求职作战台', link: '/modules/mock-interview-job-battle' },
  { text: '学习资产', link: '/modules/learning-assets' },
  { text: '闪卡', link: '/modules/flashcard' },
  { text: '计划与学习小组', link: '/modules/plan-team' },
  { text: '知识图谱', link: '/modules/knowledge' },
  { text: 'SQL 优化工作台', link: '/modules/sql-optimizer' },
  { text: 'OJ 判题系统', link: '/modules/oj' },
  { text: '社区与内容矩阵', link: '/modules/community-content' },
  { text: '社区帖子', link: '/modules/community' },
  { text: '动态广场', link: '/modules/moments' },
  { text: '博客', link: '/modules/blog' },
  { text: '代码工坊', link: '/modules/codepen' },
  { text: 'IM 聊天室', link: '/modules/chat' },
  { text: '简历系统', link: '/modules/resume' },
  { text: '积分与抽奖', link: '/modules/points' },
  { text: '文件存储', link: '/modules/file-storage' },
  { text: '通知中心', link: '/modules/notification' },
  { text: '敏感词风控', link: '/modules/sensitive' },
  { text: '工具、摸鱼与版本', link: '/modules/tools-moyu-version' },
  { text: '开发者工具', link: '/modules/dev-tools' },
  { text: '摸鱼工具', link: '/modules/moyu' },
  { text: '版本历史', link: '/modules/version-history' },
  { text: '仪表盘与日志', link: '/modules/dashboard-logs' },
  { text: '系统运营后台', link: '/modules/system-ops' }
]

const operations = [
  { text: 'Docker 与服务部署', link: '/operations/docker' },
  { text: '监控与观测', link: '/operations/monitoring' },
  { text: '常见问题排查', link: '/operations/troubleshooting' }
]

const reference = [
  { text: 'API 路由索引', link: '/reference/api-routes' },
  { text: '前端路由索引', link: '/reference/frontend-routes' },
  { text: '数据表索引', link: '/reference/database-tables' },
  { text: '源码地图', link: '/reference/source-map' },
  { text: '响应体与错误码', link: '/reference/response-errors' },
  { text: 'WebSocket 协议', link: '/reference/websocket' },
  { text: 'AI Schema 与治理', link: '/reference/ai-schemas' },
  { text: '全功能覆盖矩阵', link: '/reference/feature-coverage' }
]

const manuals = [
  { text: '用户端操作手册', link: '/manuals/user-operations' },
  { text: '管理端操作手册', link: '/manuals/admin-operations' },
  { text: '核心链路教程', link: '/manuals/core-workflows' },
  { text: '验证记录与已知问题', link: '/manuals/verified-scenarios' }
]

export default defineConfig({
  lang: 'zh-CN',
  title: 'Code Nest 文档',
  description: 'Code Nest v2.2.0 独立文档站',
  base: process.env.VITEPRESS_BASE || '/',
  cleanUrls: true,
  lastUpdated: true,
  head: [
    ['meta', { name: 'theme-color', content: '#2563eb' }]
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
      { text: 'v2.2.0 计划', link: '/roadmap/v2.2.0-docs-plan' }
    ],
    sidebar: {
      '/guide/': [{ text: '使用指南', items: guide }],
      '/architecture/': [{ text: '系统架构', items: architecture }],
      '/modules/': [{ text: '功能模块', items: modules }],
      '/reference/': [{ text: '参考索引', items: reference }],
      '/manuals/': [{ text: '操作手册', items: manuals }],
      '/operations/': [{ text: '部署运维', items: operations }],
      '/roadmap/': [
        {
          text: '文档路线图',
          items: [{ text: 'v2.2.0 文档计划', link: '/roadmap/v2.2.0-docs-plan' }]
        }
      ]
    },
    search: {
      provider: 'local'
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
      message: 'Code Nest v2.2.0 documentation module.',
      copyright: 'MIT Licensed.'
    }
  }
})
