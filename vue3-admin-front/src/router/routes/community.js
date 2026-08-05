export const createCommunityRoutes = (Layout) => [
  {
    path: '/community',
    component: Layout,
    redirect: '/community/categories',
    meta: { title: '社区管理' },
    children: [
      {
        path: 'categories',
        name: 'CommunityCategories',
        component: () => import('@/views/community/categories/index.vue'),
        meta: { title: '分类管理' }
      },
      {
        path: 'tags',
        name: 'CommunityTags',
        component: () => import('@/views/community/tags/index.vue'),
        meta: { title: '标签管理' }
      },
      {
        path: 'posts',
        name: 'CommunityPosts',
        component: () => import('@/views/community/posts/index.vue'),
        meta: { title: '帖子管理' }
      },
      {
        path: 'comments',
        name: 'CommunityComments',
        component: () => import('@/views/community/comments/index.vue'),
        meta: { title: '评论管理' }
      },
      {
        path: 'users',
        name: 'CommunityUsers',
        component: () => import('@/views/community/users/index.vue'),
        meta: { title: '用户管理' }
      }
    ]
  },
  {
    path: '/moments',
    component: Layout,
    redirect: '/moments/list',
    meta: { title: '朋友圈管理' },
    children: [
      {
        path: 'list',
        name: 'MomentsList',
        component: () => import('@/views/moments/list/index.vue'),
        meta: { title: '动态管理' }
      },
      {
        path: 'comments',
        name: 'MomentsComments',
        component: () => import('@/views/moments/comments/index.vue'),
        meta: { title: '评论管理' }
      },
      {
        path: 'statistics',
        name: 'MomentsStatistics',
        component: () => import('@/views/moments/statistics/index.vue'),
        meta: { title: '数据统计' }
      }
    ]
  },
  {
    path: '/chat',
    component: Layout,
    redirect: '/chat/messages',
    meta: { title: '聊天室管理' },
    children: [
      {
        path: 'messages',
        name: 'ChatMessages',
        component: () => import('@/views/chat/messages/index.vue'),
        meta: { title: '消息管理' }
      },
      {
        path: 'users',
        name: 'ChatUsers',
        component: () => import('@/views/chat/users/index.vue'),
        meta: { title: '在线用户' }
      }
    ]
  },
  {
    path: '/notification',
    component: Layout,
    children: [
      {
        path: '',
        name: 'Notification',
        component: () => import('@/views/notification/index.vue'),
        meta: { title: '通知管理' }
      }
    ]
  },
  {
    path: '/codepen',
    component: Layout,
    redirect: '/codepen/pens',
    meta: { title: '代码共享器管理' },
    children: [
      {
        path: 'pens',
        name: 'CodePenManagement',
        component: () => import('@/views/codepen/pens/index.vue'),
        meta: { title: '作品管理' }
      },
      {
        path: 'templates',
        name: 'CodePenTemplates',
        component: () => import('@/views/codepen/templates/index.vue'),
        meta: { title: '模板管理' }
      },
      {
        path: 'tags',
        name: 'CodePenTags',
        component: () => import('@/views/codepen/tags/index.vue'),
        meta: { title: '标签管理' }
      },
      {
        path: 'statistics',
        name: 'CodePenStatistics',
        component: () => import('@/views/codepen/statistics/index.vue'),
        meta: { title: '数据统计' }
      }
    ]
  },
  {
    path: '/moyu',
    component: Layout,
    redirect: '/moyu/calendar-events',
    meta: { title: '摸鱼工具管理' },
    children: [
      {
        path: 'calendar-events',
        name: 'MoyuCalendarEvents',
        component: () => import('@/views/moyu/calendar-events/index.vue'),
        meta: { title: '日历事件管理' }
      },
      {
        path: 'daily-content',
        name: 'MoyuDailyContent',
        component: () => import('@/views/moyu/daily-content/index.vue'),
        meta: { title: '每日内容管理' }
      },
      {
        path: 'statistics',
        name: 'MoyuStatistics',
        component: () => import('@/views/moyu/statistics/index.vue'),
        meta: { title: '统计分析' }
      },
      {
        path: 'bug-store',
        name: 'MoyuBugStore',
        component: () => import('@/views/moyu/bug-store/index.vue'),
        meta: { title: 'Bug商店管理' }
      }
    ]
  },
  {
    path: '/blog',
    component: Layout,
    redirect: '/blog/articles',
    meta: { title: '博客管理' },
    children: [
      {
        path: 'articles',
        name: 'BlogArticles',
        component: () => import('@/views/blog/articles/index.vue'),
        meta: { title: '文章管理' }
      },
      {
        path: 'categories',
        name: 'BlogCategories',
        component: () => import('@/views/blog/categories/index.vue'),
        meta: { title: '分类管理' }
      },
      {
        path: 'tags',
        name: 'BlogTags',
        component: () => import('@/views/blog/tags/index.vue'),
        meta: { title: '标签管理' }
      }
    ]
  }
]
