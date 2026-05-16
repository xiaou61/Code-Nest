import { defineConfig } from 'vitepress'

const guide = [
  { text: '快速开始', link: '/guide/quick-start' },
  { text: '本地开发', link: '/guide/local-dev' },
  { text: '独立部署', link: '/guide/deploy' }
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
  { text: 'AI Runtime', link: '/modules/ai-runtime' },
  { text: '题库与成长闭环', link: '/modules/interview-and-growth' },
  { text: 'OJ 判题系统', link: '/modules/oj' },
  { text: '社区与内容矩阵', link: '/modules/community-content' },
  { text: 'IM 聊天室', link: '/modules/chat' },
  { text: '简历系统', link: '/modules/resume' },
  { text: '积分与抽奖', link: '/modules/points' },
  { text: '文件存储', link: '/modules/file-storage' },
  { text: '工具、摸鱼与版本', link: '/modules/tools-moyu-version' },
  { text: '系统运营后台', link: '/modules/system-ops' }
]

const operations = [
  { text: 'Docker 与服务部署', link: '/operations/docker' },
  { text: '监控与观测', link: '/operations/monitoring' },
  { text: '常见问题排查', link: '/operations/troubleshooting' }
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
      { text: '运维', link: '/operations/docker' },
      { text: 'v2.2.0 计划', link: '/roadmap/v2.2.0-docs-plan' }
    ],
    sidebar: {
      '/guide/': [{ text: '使用指南', items: guide }],
      '/architecture/': [{ text: '系统架构', items: architecture }],
      '/modules/': [{ text: '功能模块', items: modules }],
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

