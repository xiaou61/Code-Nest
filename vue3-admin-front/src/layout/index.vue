<template>
  <div class="layout-container">
    <el-container>
      <el-aside :width="sidebarWidth" class="sidebar-shell">
        <CnSidebar
          ref="sidebarRef"
          v-model:search-keyword="searchKeyword"
          :items="sidebarItems"
          :active-path="currentRoute"
          :collapsed="collapsed"
          :search-results="filteredMenuItems"
          @search-input="handleSearchInput"
          @search-enter="handleSearchEnter"
          @search-focus-request="expandAndFocusSearch"
          @result-select="handleMenuSelect"
        />
      </el-aside>
      
      <el-container>
        <!-- 头部 -->
        <el-header height="60px" class="header" :class="{ 'header-elevated': isMainScrolled }">
          <div class="header-left">
            <!-- 折叠按钮 -->
            <el-button
              type="text"
              @click="toggleSidebar"
              class="header-toggle-btn"
            >
              <el-icon>
                <Expand v-if="collapsed" />
                <Fold v-else />
              </el-icon>
            </el-button>
            
            <!-- 面包屑 -->
            <span class="header-title">{{ currentTitle }}</span>
          </div>
          
          <div class="header-right">
            <CnThemeSwitch class="header-theme-switch" />

            <!-- 用户信息 -->
            <div class="user-info">
              <!-- 用户头像 -->
              <el-avatar
                :size="32"
                :src="userStore.avatar"
                class="header-avatar"
              >
                {{ userStore.realName.charAt(0) || userStore.username.charAt(0) }}
              </el-avatar>
              
              <!-- 用户下拉菜单 -->
              <el-dropdown @command="handleUserCommand">
                <span class="username">
                  {{ userStore.realName || userStore.username }}
                  <el-icon><CaretBottom /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="profile">
                      <el-icon><User /></el-icon>
                      个人中心
                    </el-dropdown-item>
                    <el-dropdown-item command="logout" divided>
                      <el-icon><SwitchButton /></el-icon>
                      退出登录
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </el-header>
        
        <!-- 主内容区域 -->
        <el-main class="main-content" @scroll.passive="handleMainScroll">
          <div class="page-container">
            <router-view v-slot="{ Component }">
              <transition name="admin-page-fade" mode="out-in">
                <component :is="Component" />
              </transition>
            </router-view>
          </div>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { 
  Odometer, 
  Document, 
  UserFilled, 
  Operation, 
  User, 
  Avatar,
  Expand, 
  Fold, 
  CaretBottom, 
  SwitchButton,
  Setting,
  Monitor,
  DataAnalysis,
  FolderOpened,
  Collection,
  Edit,
  ChatDotRound,
  ChatLineRound,
  Files,
  PriceTag,
  SetUp,
  Sort,
  Tools,
  Bell,
  Message,
  Picture,
  Warning,
  EditPen,
  Share,
  Coin,
  Trophy,
  Plus,
  Coffee,
  Calendar,
  Reading,
  Check,
  Mic,
  CollectionTag
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { CnSidebar, CnThemeSwitch } from '@/design-system'
import type { CnSidebarItem, CnSidebarSearchResult } from '@/design-system'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 侧边栏折叠状态
const collapsed = ref(false)

// 侧边栏宽度
const sidebarWidth = computed(() => collapsed.value ? '64px' : '200px')

// 当前路由
const currentRoute = computed(() => route.path)

// 当前页面标题
const currentTitle = computed(() => route.meta?.title || '仪表板')

// 搜索相关
const searchKeyword = ref('')
const sidebarRef = ref<InstanceType<typeof CnSidebar> | null>(null)
const isMainScrolled = ref(false)

const handleMainScroll = (event) => {
  isMainScrolled.value = event.target.scrollTop > 8
}

const sidebarIconRegistry: Record<string, Component> = {
  Odometer,
  Document,
  UserFilled,
  Operation,
  User,
  Avatar,
  Setting,
  Monitor,
  DataAnalysis,
  FolderOpened,
  Collection,
  Edit,
  ChatDotRound,
  ChatLineRound,
  Files,
  PriceTag,
  SetUp,
  Sort,
  Tools,
  Bell,
  Message,
  Picture,
  Warning,
  EditPen,
  Share,
  Coin,
  Trophy,
  Plus,
  Coffee,
  Calendar,
  Reading,
  Check,
  Mic,
  CollectionTag
}

const resolveSidebarIcon = (iconName: string) => sidebarIconRegistry[iconName] || Document

const sidebarItems: CnSidebarItem[] = [
  { label: '仪表板', path: '/dashboard', icon: Odometer },
  { label: '用户管理', path: '/user', icon: Avatar },
  {
    label: '面试题目管理',
    index: '/interview',
    icon: Document,
    children: [
      { label: '分类管理', path: '/interview/categories', icon: FolderOpened },
      { label: '题单管理', path: '/interview/question-sets', icon: Collection },
      { label: '题目管理', path: '/interview/questions', icon: Edit }
    ]
  },
  {
    label: '模拟面试运营',
    index: '/mock-interview',
    icon: Mic,
    children: [
      { label: '面试会话', path: '/mock-interview/sessions', icon: DataAnalysis },
      { label: '方向配置', path: '/mock-interview/directions', icon: SetUp }
    ]
  },
  {
    label: 'OJ 判题管理',
    index: '/oj',
    icon: Monitor,
    children: [
      { label: '题目管理', path: '/oj/problems', icon: Edit },
      { label: '赛事管理', path: '/oj/contests', icon: Trophy },
      { label: '标签管理', path: '/oj/tags', icon: PriceTag }
    ]
  },
  {
    label: '简历中心',
    index: '/resume',
    icon: EditPen,
    children: [
      { label: '模板管理', path: '/resume/templates', icon: Document },
      { label: '数据总览', path: '/resume/analytics', icon: DataAnalysis },
      { label: '健康巡检', path: '/resume/reports', icon: Warning }
    ]
  },
  {
    label: '知识图谱管理',
    index: '/knowledge',
    icon: DataAnalysis,
    children: [
      { label: '图谱管理', path: '/knowledge/maps', icon: Share }
    ]
  },
  {
    label: '学习资产管理',
    index: '/learning-assets',
    icon: Collection,
    children: [
      { label: '审核台', path: '/learning-assets/review', icon: Edit },
      { label: '统计分析', path: '/learning-assets/statistics', icon: DataAnalysis }
    ]
  },
  {
    label: '社区管理',
    index: '/community',
    icon: ChatDotRound,
    children: [
      { label: '分类管理', path: '/community/categories', icon: Files },
      { label: '标签管理', path: '/community/tags', icon: PriceTag },
      { label: '帖子管理', path: '/community/posts', icon: Document },
      { label: '评论管理', path: '/community/comments', icon: ChatLineRound },
      { label: '用户管理', path: '/community/users', icon: User }
    ]
  },
  {
    label: '朋友圈管理',
    index: '/moments',
    icon: Picture,
    children: [
      { label: '动态管理', path: '/moments/list', icon: Document },
      { label: '评论管理', path: '/moments/comments', icon: ChatLineRound },
      { label: '数据统计', path: '/moments/statistics', icon: DataAnalysis }
    ]
  },
  {
    label: '聊天室管理',
    index: '/chat',
    icon: Message,
    children: [
      { label: '消息管理', path: '/chat/messages', icon: ChatDotRound },
      { label: '在线用户', path: '/chat/users', icon: User }
    ]
  },
  {
    label: '日志管理',
    index: '/logs',
    icon: Document,
    children: [
      { label: '登录日志', path: '/logs/login', icon: UserFilled },
      { label: '操作日志', path: '/logs/operation', icon: Operation }
    ]
  },
  { label: '通知管理', path: '/notification', icon: Bell },
  {
    label: '敏感词管理',
    index: '/sensitive',
    icon: Warning,
    children: [
      { label: '词库管理', path: '/sensitive/words', icon: EditPen },
      { label: '白名单管理', path: '/sensitive/whitelist', icon: Check },
      { label: '策略配置', path: '/sensitive/strategy', icon: SetUp },
      { label: '统计分析', path: '/sensitive/statistics', icon: DataAnalysis },
      { label: '词库来源', path: '/sensitive/source', icon: FolderOpened },
      { label: '版本历史', path: '/sensitive/version', icon: Document },
      { label: '配置管理', path: '/sensitive/config', icon: Tools }
    ]
  },
  {
    label: '文件存储管理',
    index: '/filestorage',
    icon: FolderOpened,
    children: [
      { label: '存储配置', path: '/filestorage/storage-config', icon: SetUp },
      { label: '文件管理', path: '/filestorage/file-management', icon: Document },
      { label: '文件迁移', path: '/filestorage/migration', icon: Sort },
      { label: '系统设置', path: '/filestorage/system-settings', icon: Tools }
    ]
  },
  {
    label: '代码共享器管理',
    index: '/codepen',
    icon: EditPen,
    children: [
      { label: '作品管理', path: '/codepen/pens', icon: Document },
      { label: '模板管理', path: '/codepen/templates', icon: Files },
      { label: '标签管理', path: '/codepen/tags', icon: CollectionTag },
      { label: '数据统计', path: '/codepen/statistics', icon: DataAnalysis }
    ]
  },
  {
    label: '摸鱼工具管理',
    index: '/moyu',
    icon: Coffee,
    children: [
      { label: '日历事件管理', path: '/moyu/calendar-events', icon: Calendar },
      { label: '每日内容管理', path: '/moyu/daily-content', icon: Document },
      { label: '统计分析', path: '/moyu/statistics', icon: DataAnalysis },
      { label: 'Bug商店管理', path: '/moyu/bug-store', icon: Warning }
    ]
  },
  {
    label: '积分管理',
    index: '/points',
    icon: Coin,
    children: [
      { label: '积分概览', path: '/points/index', icon: DataAnalysis },
      { label: '积分排行', path: '/points/users', icon: Trophy },
      { label: '积分明细', path: '/points/details', icon: Document },
      { label: '积分发放', path: '/points/grant', icon: Plus }
    ]
  },
  { label: '抽奖管理', path: '/lottery', icon: Trophy },
  {
    label: '博客管理',
    index: '/blog',
    icon: Reading,
    children: [
      { label: '文章管理', path: '/blog/articles', icon: Document },
      { label: '分类管理', path: '/blog/categories', icon: FolderOpened },
      { label: '标签管理', path: '/blog/tags', icon: PriceTag }
    ]
  },
  {
    label: '系统管理',
    index: '/system',
    icon: Setting,
    children: [
      { label: '版本管理', path: '/system/version', icon: Document },
      { label: 'AI质量治理', path: '/system/ai-governance', icon: DataAnalysis },
      { label: 'AI 配置与观测', path: '/system/ai-config', icon: SetUp }
    ]
  }
]

// 智能图标推断函数
const getIconByPath = (path, title = '') => {
  // 精确路径匹配（优先级最高）
  const exactIconMap: Record<string, string> = {
    '/dashboard': 'Odometer',
    '/user': 'Avatar',
    '/notification': 'Bell',
    '/system/version': 'Document',
    '/system/ai-governance': 'DataAnalysis',
    '/system/ai-config': 'SetUp'
  }
  
  if (exactIconMap[path]) {
    return resolveSidebarIcon(exactIconMap[path])
  }
  
  // 路径关键词匹配
  const pathKeywords: Record<string, string> = {
    'dashboard': 'Odometer',
    'user': 'Avatar', 
    'interview': 'Document',
    'mock-interview': 'Mic',
    'oj': 'Monitor',
    'contests': 'Trophy',
    'categories': 'FolderOpened',
    'tags': 'PriceTag',
    'question-sets': 'Collection',
    'questions': 'Edit',
    'knowledge': 'DataAnalysis',
    'maps': 'Share',
    'community': 'ChatDotRound',
    'posts': 'Document',
    'comments': 'ChatLineRound',
    'moments': 'Picture',
    'chat': 'Message',
    'messages': 'ChatDotRound',
    'statistics': 'DataAnalysis',
    'logs': 'Document',
    'login': 'UserFilled',
    'operation': 'Operation',
    'notification': 'Bell',
    'sensitive': 'Warning',
    'words': 'EditPen',
    'filestorage': 'FolderOpened',
    'storage-config': 'SetUp',
    'file-management': 'Document',
    'migration': 'Sort',
    'system-settings': 'Tools',
    'moyu': 'Coffee',
    'calendar-events': 'Calendar',
    'daily-content': 'Document',
    'points': 'Coin',
    'grant': 'Plus',
    'lottery': 'Trophy',
    'system': 'Setting',
    'ai-config': 'SetUp',
    'ai': 'SetUp',
    'version': 'Document',
    'ai-governance': 'DataAnalysis',
    'monitor': 'Monitor',
    'sql': 'DataAnalysis',
    'profile': 'User',
    'edit': 'Edit',
    'password': 'EditPen'
  }
  
  // 从路径中提取关键词
  const pathSegments = path.split('/').filter(segment => segment !== '')
  for (const segment of pathSegments) {
    if (pathKeywords[segment]) {
      return resolveSidebarIcon(pathKeywords[segment])
    }
  }
  
  // 标题关键词匹配
  const titleKeywords: Record<string, string> = {
    '仪表板': 'Odometer',
    '用户': 'Avatar',
    '管理': 'Setting',
    '分类': 'FolderOpened',
    '标签': 'PriceTag',
    '题目': 'Edit',
    '模拟面试': 'Mic',
    '赛事': 'Trophy',
    '题单': 'Collection',
    '知识': 'DataAnalysis',
    '图谱': 'Share',
    '社区': 'ChatDotRound',
    '帖子': 'Document',
    '评论': 'ChatLineRound',
    '朋友圈': 'Picture',
    '动态': 'Picture',
    '聊天室': 'Message',
    '消息': 'ChatDotRound',
    '在线': 'User',
    '统计': 'DataAnalysis',
    '日志': 'Document',
    '登录': 'UserFilled',
    '操作': 'Operation',
    '通知': 'Bell',
    '敏感词': 'Warning',
    '文件': 'FolderOpened',
    '存储': 'FolderOpened',
    '配置': 'SetUp',
    '迁移': 'Sort',
    '设置': 'Tools',
    '积分': 'Coin',
    '排行': 'Trophy',
    '发放': 'Plus',
    '明细': 'Document',
    '概览': 'DataAnalysis',
    '抽奖': 'Trophy',
    '系统': 'Setting',
    '版本': 'Document',
    'AI': 'DataAnalysis',
    '治理': 'DataAnalysis',
    '监控': 'Monitor',
    'SQL': 'DataAnalysis',
    '个人': 'User',
    '编辑': 'Edit',
    '修改': 'EditPen',
    '密码': 'EditPen'
  }
  
  // 从标题中匹配关键词
  for (const [keyword, icon] of Object.entries(titleKeywords)) {
    if (title.includes(keyword)) {
      return resolveSidebarIcon(icon)
    }
  }
  
  // 默认图标
  return Document
}

// 智能面包屑生成函数
const generateBreadcrumb = (path, title) => {
  // 特殊路径的面包屑映射（单层级页面）
  const singleLevelPages = {
    '/dashboard': '仪表板',
    '/user': '用户管理',
    '/notification': '通知管理'
  }
  
  if (singleLevelPages[path]) {
    return singleLevelPages[path]
  }
  
  // 路径段到父级模块名称的映射
  const moduleNames = {
    'interview': '面试题目管理',
    'mock-interview': '模拟面试运营',
    'knowledge': '知识图谱管理', 
    'learning-assets': '学习资产管理',
    'community': '社区管理',
    'moments': '朋友圈管理',
    'chat': '聊天室管理',
    'logs': '日志管理',
    'sensitive': '敏感词管理',
    'filestorage': '文件存储管理',
    'codepen': '代码共享器管理',
    'moyu': '摸鱼工具管理',
    'points': '积分管理',
    'system': '系统管理',
    'profile': '个人中心'
  }
  
  // 特殊子模块的映射
  const subModuleNames = {
    'monitor': '系统监控'
  }
  
  // 分解路径构建面包屑
  const pathSegments = path.split('/').filter(segment => segment !== '')
  const breadcrumbParts = []
  
  // 构建层级结构
  for (let i = 0; i < pathSegments.length - 1; i++) {
    const segment = pathSegments[i]
    
    // 检查是否是已知的模块
    if (moduleNames[segment]) {
      breadcrumbParts.push(moduleNames[segment])
    } else if (subModuleNames[segment]) {
      breadcrumbParts.push(subModuleNames[segment])
    }
  }
  
  // 添加当前页面标题
  breadcrumbParts.push(title)
  
  return breadcrumbParts.join(' > ')
}

// 动态生成菜单项
const generateMenuItems = () => {
  const menuItems = []
  
  // 获取所有路由
  const allRoutes = router.getRoutes()
  
  // 过滤并处理路由
  allRoutes.forEach(route => {
    // 跳过特殊路由
    if (route.path === '/login' || 
        route.name === 'NotFound' || 
        route.path === '/:pathMatch(.*)*' ||
        !route.meta?.title) {
      return
    }
    
    // 构建菜单项
    const menuItem = {
      path: route.path,
      title: route.meta.title,
      icon: getIconByPath(route.path, route.meta.title),
      breadcrumb: generateBreadcrumb(route.path, route.meta.title)
    }
    
    menuItems.push(menuItem)
  })
  
  // 去重（防止重复路由）
  const uniqueMenuItems = menuItems.filter((item, index, self) => 
    index === self.findIndex(t => t.path === item.path)
  )
  
  // 排序（将常用功能放在前面）
  const sortOrder = [
    '/dashboard', '/user', '/system/version', '/notification'
  ]
  
  uniqueMenuItems.sort((a, b) => {
    const aIndex = sortOrder.indexOf(a.path)
    const bIndex = sortOrder.indexOf(b.path)
    
    if (aIndex !== -1 && bIndex !== -1) {
      return aIndex - bIndex
    } else if (aIndex !== -1) {
      return -1
    } else if (bIndex !== -1) {
      return 1
    } else {
      return a.title.localeCompare(b.title)
    }
  })
  
  return uniqueMenuItems
}

// 获取动态菜单项
const menuItems = computed(() => generateMenuItems())

// 切换侧边栏
const toggleSidebar = () => {
  collapsed.value = !collapsed.value
}

// 过滤后的菜单项
const filteredMenuItems = computed(() => {
  if (!searchKeyword.value.trim()) {
    return []
  }
  
  const query = searchKeyword.value.toLowerCase().trim()
  return menuItems.value.filter(item => 
    item.title.toLowerCase().includes(query) ||
    item.breadcrumb.toLowerCase().includes(query)
  ).slice(0, 10) // 限制显示10个结果
})

// 处理菜单选择
const handleMenuSelect = (item: CnSidebarSearchResult) => {
  router.push(item.path)
  searchKeyword.value = ''
}

// 处理搜索输入
const handleSearchInput = () => {
  // 输入时的实时处理，现在主要依靠computed自动更新
}

// 处理搜索框回车
const handleSearchEnter = () => {
  if (filteredMenuItems.value.length > 0) {
    handleMenuSelect(filteredMenuItems.value[0])
  }
}

// 展开侧边栏并聚焦搜索
const expandAndFocusSearch = () => {
  if (collapsed.value) {
    collapsed.value = false
    // 等待DOM更新后聚焦搜索框
    setTimeout(() => {
      sidebarRef.value?.focusSearch()
    }, 300)
  }
}

// 键盘快捷键处理
const handleKeyDown = (event) => {
  // Ctrl+K 或 Cmd+K 唤起搜索
  if ((event.ctrlKey || event.metaKey) && event.key === 'k') {
    event.preventDefault()
    
    // 如果侧边栏折叠，先展开
    if (collapsed.value) {
      expandAndFocusSearch()
    } else {
      sidebarRef.value?.focusSearch()
    }
  }
  // Escape 清空搜索
  if (event.key === 'Escape' && searchKeyword.value) {
    searchKeyword.value = ''
  }
}

// 生命周期
onMounted(() => {
  document.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyDown)
})

// 处理用户下拉菜单命令
const handleUserCommand = async (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        })
        
        await userStore.logout()
        router.push('/login')
      } catch {
        // 用户取消退出时保持当前页面。
      }
      break
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  background: transparent;
  --admin-shell-glass: color-mix(in srgb, var(--cn-color-bg-surface) 90%, transparent);
  --admin-shell-strong: color-mix(in srgb, var(--cn-color-bg-surface) 96%, transparent);
  --admin-brand-border-soft: color-mix(in srgb, var(--cn-color-brand-primary) 18%, var(--cn-color-border-subtle));
  --admin-shadow-header: 0 10px 26px color-mix(in srgb, var(--cn-color-text-primary) 8%, transparent);
}

.sidebar-shell {
  overflow: hidden;
  background: var(--cn-color-bg-surface);
  transition: width var(--cn-motion-base) var(--cn-ease-out);
}

.header {
  background-color: var(--admin-shell-glass);
  backdrop-filter: blur(6px);
  border-bottom: 1px solid var(--cn-border-soft);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 18px 0 14px;
  transition:
    background-color var(--cn-motion-base) var(--cn-ease-out),
    border-color var(--cn-motion-base) var(--cn-ease-out),
    box-shadow var(--cn-motion-base) var(--cn-ease-out);
}

.header.header-elevated {
  background-color: var(--admin-shell-strong);
  border-bottom-color: var(--admin-brand-border-soft);
  box-shadow: var(--admin-shadow-header);
}

.header-left {
  display: flex;
  align-items: center;
}

.header-toggle-btn {
  margin-right: 16px;
  width: 36px;
  height: 36px;
  border-radius: var(--cn-radius-sm);
  color: var(--cn-text-secondary);
}

.header-toggle-btn:hover {
  color: var(--cn-color-brand-primary);
  background: var(--cn-color-brand-soft);
}

.header-title {
  font-size: 17px;
  color: var(--cn-text-primary);
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-theme-switch {
  flex-shrink: 0;
}

.user-info {
  display: flex;
  align-items: center;
}

.header-avatar {
  margin-right: 8px;
  border: 2px solid var(--admin-brand-border-soft);
  color: var(--cn-color-brand-primary);
  background-color: var(--cn-color-brand-soft);
}

.username {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 7px 12px;
  border-radius: var(--cn-radius-sm);
  color: var(--cn-text-secondary);
  font-weight: 500;
  transition: var(--cn-transition);
}

.username:hover {
  color: var(--cn-color-brand-primary);
  background: var(--cn-color-brand-soft);
}

.main-content {
  background: transparent;
  padding: 16px;
}

.page-container {
  height: 100%;
  overflow: auto;
  background: var(--admin-shell-glass);
  border: 1px solid var(--cn-border-soft);
  border-radius: var(--cn-radius-md);
  padding: 24px;
  box-shadow: var(--cn-shadow-xs);
}

.admin-page-fade-enter-active,
.admin-page-fade-leave-active {
  transition:
    opacity var(--cn-motion-base) var(--cn-ease-in-out),
    transform var(--cn-motion-base) var(--cn-ease-out);
}

.admin-page-fade-enter-from,
.admin-page-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@media (max-width: 768px) {
  .main-content {
    padding: 10px;
  }

  .page-container {
    padding: 14px;
  }
}
</style> 
