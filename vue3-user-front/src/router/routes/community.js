export const communityRoutes = [
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
  }
]
