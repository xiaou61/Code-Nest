<template>
  <div class="layout-container">
    <el-container>
      <!-- 侧边栏 -->
      <el-aside :width="sidebarWidth" class="sidebar">
        <!-- Logo -->
        <div class="logo">
          <span v-if="!collapsed">Code-Nest</span>
          <span v-else>CN</span>
        </div>
        
        <!-- 菜单搜索框 -->
        <div class="sidebar-search-input-wrapper" v-if="!collapsed">
          <el-input
            v-model="searchKeyword"
            placeholder="🔍 搜索功能... (Ctrl+K)"
            size="small"
            @keyup.enter="handleSearchEnter"
            @input="handleSearchInput"
            ref="searchInput"
            class="sidebar-search-input"
            clearable
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        
        <!-- 折叠状态下的搜索按钮 -->
        <div class="sidebar-search-collapsed" v-else>
          <el-tooltip content="搜索功能 (Ctrl+K)" placement="right">
            <el-button 
              text 
              @click="expandAndFocusSearch"
              class="search-toggle-btn"
            >
              <el-icon size="18"><Search /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
        
        <!-- 搜索结果区域 -->
        <div v-if="!collapsed && searchKeyword" class="search-results-container">
          <!-- 有搜索结果 -->
          <div v-if="filteredMenuItems.length > 0" class="search-results">
            <div class="search-results-header">
              找到 {{ filteredMenuItems.length }} 个功能
            </div>
            <div 
              v-for="item in filteredMenuItems" 
              :key="item.path"
              class="search-result-item"
              @click="handleMenuSelect(item)"
            >
              <el-icon class="search-result-icon">
                <component :is="item.icon" />
              </el-icon>
              <div class="search-result-content">
                <div class="search-result-title">{{ item.title }}</div>
                <div class="search-result-path">{{ item.breadcrumb }}</div>
              </div>
            </div>
          </div>
          
          <!-- 无搜索结果 -->
          <div v-else class="search-no-results">
            <div class="no-results-icon">🔍</div>
            <div class="no-results-text">未找到匹配的功能</div>
          </div>
        </div>

        <!-- 导航菜单 -->
        <el-menu
          :default-active="currentRoute"
          :collapse="collapsed"
          router
          class="sidebar-menu"
          v-if="!searchKeyword"
        >
          <el-menu-item index="/dashboard">
            <el-icon><Odometer /></el-icon>
            <span>仪表板</span>
          </el-menu-item>
          
          <el-menu-item index="/user">
            <el-icon><Avatar /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          
          <el-sub-menu index="/interview">
            <template #title>
              <el-icon><Document /></el-icon>
              <span>面试题目管理</span>
            </template>
            <el-menu-item index="/interview/categories">
              <el-icon><FolderOpened /></el-icon>
              <span>分类管理</span>
            </el-menu-item>
            <el-menu-item index="/interview/question-sets">
              <el-icon><Collection /></el-icon>
              <span>题单管理</span>
            </el-menu-item>
            <el-menu-item index="/interview/questions">
              <el-icon><Edit /></el-icon>
              <span>题目管理</span>
            </el-menu-item>
          </el-sub-menu>
          
          <el-sub-menu index="/oj">
            <template #title>
              <el-icon><Monitor /></el-icon>
              <span>OJ 判题管理</span>
            </template>
            <el-menu-item index="/oj/problems">
              <el-icon><Edit /></el-icon>
              <span>题目管理</span>
            </el-menu-item>
            <el-menu-item index="/oj/tags">
              <el-icon><PriceTag /></el-icon>
              <span>标签管理</span>
            </el-menu-item>
          </el-sub-menu>
          
          <el-sub-menu index="/resume">
            <template #title>
              <el-icon><EditPen /></el-icon>
              <span>简历中心</span>
            </template>
            <el-menu-item index="/resume/templates">
              <el-icon><Document /></el-icon>
              <span>模板管理</span>
            </el-menu-item>
            <el-menu-item index="/resume/analytics">
              <el-icon><DataAnalysis /></el-icon>
              <span>数据总览</span>
            </el-menu-item>
            <el-menu-item index="/resume/reports">
              <el-icon><Warning /></el-icon>
              <span>健康巡检</span>
            </el-menu-item>
          </el-sub-menu>
          
          <el-sub-menu index="/knowledge">
            <template #title>
              <el-icon><DataAnalysis /></el-icon>
              <span>知识图谱管理</span>
            </template>
            <el-menu-item index="/knowledge/maps">
              <el-icon><Share /></el-icon>
              <span>图谱管理</span>
            </el-menu-item>
          </el-sub-menu>
          
          <el-sub-menu index="/community">
            <template #title>
              <el-icon><ChatDotRound /></el-icon>
              <span>社区管理</span>
            </template>
            <el-menu-item index="/community/categories">
              <el-icon><Files /></el-icon>
              <span>分类管理</span>
            </el-menu-item>
            <el-menu-item index="/community/tags">
              <el-icon><PriceTag /></el-icon>
              <span>标签管理</span>
            </el-menu-item>
            <el-menu-item index="/community/posts">
              <el-icon><Document /></el-icon>
              <span>帖子管理</span>
            </el-menu-item>
            <el-menu-item index="/community/comments">
              <el-icon><ChatLineRound /></el-icon>
              <span>评论管理</span>
            </el-menu-item>
            <el-menu-item index="/community/users">
              <el-icon><User /></el-icon>
              <span>用户管理</span>
            </el-menu-item>
          </el-sub-menu>
          
          <el-sub-menu index="/moments">
            <template #title>
              <el-icon><Picture /></el-icon>
              <span>朋友圈管理</span>
            </template>
            <el-menu-item index="/moments/list">
              <el-icon><Document /></el-icon>
              <span>动态管理</span>
            </el-menu-item>
            <el-menu-item index="/moments/comments">
              <el-icon><ChatLineRound /></el-icon>
              <span>评论管理</span>
            </el-menu-item>
            <el-menu-item index="/moments/statistics">
              <el-icon><DataAnalysis /></el-icon>
              <span>数据统计</span>
            </el-menu-item>
          </el-sub-menu>
          
          <el-sub-menu index="/chat">
            <template #title>
              <el-icon><Message /></el-icon>
              <span>聊天室管理</span>
            </template>
            <el-menu-item index="/chat/messages">
              <el-icon><ChatDotRound /></el-icon>
              <span>消息管理</span>
            </el-menu-item>
            <el-menu-item index="/chat/users">
              <el-icon><User /></el-icon>
              <span>在线用户</span>
            </el-menu-item>
          </el-sub-menu>
          
          <el-sub-menu index="/logs">
            <template #title>
              <el-icon><Document /></el-icon>
              <span>日志管理</span>
            </template>
            <el-menu-item index="/logs/login">
              <el-icon><UserFilled /></el-icon>
              <span>登录日志</span>
            </el-menu-item>
            <el-menu-item index="/logs/operation">
              <el-icon><Operation /></el-icon>
              <span>操作日志</span>
            </el-menu-item>
          </el-sub-menu>
          
          <el-menu-item index="/notification">
            <el-icon><Bell /></el-icon>
            <span>通知管理</span>
          </el-menu-item>
          
          <el-sub-menu index="/sensitive">
            <template #title>
              <el-icon><Warning /></el-icon>
              <span>敏感词管理</span>
            </template>
            <el-menu-item index="/sensitive/words">
              <el-icon><EditPen /></el-icon>
              <span>词库管理</span>
            </el-menu-item>
            <el-menu-item index="/sensitive/whitelist">
              <el-icon><Check /></el-icon>
              <span>白名单管理</span>
            </el-menu-item>
            <el-menu-item index="/sensitive/strategy">
              <el-icon><SetUp /></el-icon>
              <span>策略配置</span>
            </el-menu-item>
            <el-menu-item index="/sensitive/statistics">
              <el-icon><DataAnalysis /></el-icon>
              <span>统计分析</span>
            </el-menu-item>
            <el-menu-item index="/sensitive/source">
              <el-icon><FolderOpened /></el-icon>
              <span>词库来源</span>
            </el-menu-item>
            <el-menu-item index="/sensitive/version">
              <el-icon><Document /></el-icon>
              <span>版本历史</span>
            </el-menu-item>
            <el-menu-item index="/sensitive/config">
              <el-icon><Tools /></el-icon>
              <span>配置管理</span>
            </el-menu-item>
          </el-sub-menu>
          
          <el-sub-menu index="/filestorage">
            <template #title>
              <el-icon><FolderOpened /></el-icon>
              <span>文件存储管理</span>
            </template>
            <el-menu-item index="/filestorage/storage-config">
              <el-icon><SetUp /></el-icon>
              <span>存储配置</span>
            </el-menu-item>
            <el-menu-item index="/filestorage/file-management">
              <el-icon><Document /></el-icon>
              <span>文件管理</span>
            </el-menu-item>
            <el-menu-item index="/filestorage/migration">
              <el-icon><Sort /></el-icon>
              <span>文件迁移</span>
            </el-menu-item>
            <el-menu-item index="/filestorage/system-settings">
              <el-icon><Tools /></el-icon>
              <span>系统设置</span>
            </el-menu-item>
          </el-sub-menu>
          
          <el-sub-menu index="/codepen">
            <template #title>
              <el-icon><EditPen /></el-icon>
              <span>代码共享器管理</span>
            </template>
            <el-menu-item index="/codepen/pens">
              <el-icon><Document /></el-icon>
              <span>作品管理</span>
            </el-menu-item>
            <el-menu-item index="/codepen/templates">
              <el-icon><Files /></el-icon>
              <span>模板管理</span>
            </el-menu-item>
            <el-menu-item index="/codepen/tags">
              <el-icon><CollectionTag /></el-icon>
              <span>标签管理</span>
            </el-menu-item>
            <el-menu-item index="/codepen/statistics">
              <el-icon><DataAnalysis /></el-icon>
              <span>数据统计</span>
            </el-menu-item>
          </el-sub-menu>
          
          <el-sub-menu index="/moyu">
            <template #title>
              <el-icon><Coffee /></el-icon>
              <span>摸鱼工具管理</span>
            </template>
            <el-menu-item index="/moyu/calendar-events">
              <el-icon><Calendar /></el-icon>
              <span>日历事件管理</span>
            </el-menu-item>
            <el-menu-item index="/moyu/daily-content">
              <el-icon><Document /></el-icon>
              <span>每日内容管理</span>
            </el-menu-item>
            <el-menu-item index="/moyu/statistics">
              <el-icon><DataAnalysis /></el-icon>
              <span>统计分析</span>
            </el-menu-item>
            <el-menu-item index="/moyu/bug-store">
              <el-icon><Warning /></el-icon>
              <span>Bug商店管理</span>
            </el-menu-item>
          </el-sub-menu>
          
          <el-sub-menu index="/points">
            <template #title>
              <el-icon><Coin /></el-icon>
              <span>积分管理</span>
            </template>
            <el-menu-item index="/points/index">
              <el-icon><DataAnalysis /></el-icon>
              <span>积分概览</span>
            </el-menu-item>
            <el-menu-item index="/points/users">
              <el-icon><Trophy /></el-icon>
              <span>积分排行</span>
            </el-menu-item>
            <el-menu-item index="/points/details">
              <el-icon><Document /></el-icon>
              <span>积分明细</span>
            </el-menu-item>
            <el-menu-item index="/points/grant">
              <el-icon><Plus /></el-icon>
              <span>积分发放</span>
            </el-menu-item>
          </el-sub-menu>
          
          <el-menu-item index="/lottery">
            <el-icon><Trophy /></el-icon>
            <span>抽奖管理</span>
          </el-menu-item>
          
          <el-sub-menu index="/blog">
            <template #title>
              <el-icon><Reading /></el-icon>
              <span>博客管理</span>
            </template>
            <el-menu-item index="/blog/articles">
              <el-icon><Document /></el-icon>
              <span>文章管理</span>
            </el-menu-item>
            <el-menu-item index="/blog/categories">
              <el-icon><FolderOpened /></el-icon>
              <span>分类管理</span>
            </el-menu-item>
            <el-menu-item index="/blog/tags">
              <el-icon><PriceTag /></el-icon>
              <span>标签管理</span>
            </el-menu-item>
          </el-sub-menu>
          
          <el-sub-menu index="/system">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/system/version">
              <el-icon><Document /></el-icon>
              <span>版本管理</span>
            </el-menu-item>
            <el-sub-menu index="/system/monitor">
              <template #title>
                <el-icon><Monitor /></el-icon>
                <span>系统监控</span>
              </template>
              <el-menu-item index="/system/monitor/sql">
                <el-icon><DataAnalysis /></el-icon>
                <span>SQL监控</span>
              </el-menu-item>
            </el-sub-menu>
          </el-sub-menu>
        </el-menu>
      </el-aside>
      
      <el-container>
        <!-- 头部 -->
        <el-header height="60px" class="header">
          <div class="header-left">
            <!-- 折叠按钮 -->
            <el-button
              type="text"
              @click="toggleSidebar"
              style="font-size: 18px; margin-right: 20px;"
            >
              <el-icon>
                <Expand v-if="collapsed" />
                <Fold v-else />
              </el-icon>
            </el-button>
            
            <!-- 面包屑 -->
            <span style="font-size: 16px; color: #333;">{{ currentTitle }}</span>
          </div>
          
          <div class="header-right">
            <!-- 用户信息 -->
            <div class="user-info">
              <!-- 用户头像 -->
              <el-avatar
                :size="32"
                :src="userStore.avatar"
                style="margin-right: 8px;"
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
        <el-main class="main-content">
          <div class="page-container">
            <router-view />
          </div>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
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
  Notification,
  Message,
  Picture,
  Warning,
  EditPen,
  Share,
  Search,
  Coin,
  Trophy,
  Plus,
  Coffee,
  Calendar,
  Reading,
  Check
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

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
const searchInput = ref()

// 智能图标推断函数
const getIconByPath = (path, title = '') => {
  // 精确路径匹配（优先级最高）
  const exactIconMap = {
    '/dashboard': 'Odometer',
    '/user': 'Avatar',
    '/notification': 'Bell',
    '/system/version': 'Document',
    '/system/monitor/sql': 'DataAnalysis'
  }
  
  if (exactIconMap[path]) {
    return exactIconMap[path]
  }
  
  // 路径关键词匹配
  const pathKeywords = {
    'dashboard': 'Odometer',
    'user': 'Avatar', 
    'interview': 'Document',
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
    'version': 'Document',
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
      return pathKeywords[segment]
    }
  }
  
  // 标题关键词匹配
  const titleKeywords = {
    '仪表板': 'Odometer',
    '用户': 'Avatar',
    '管理': 'Setting',
    '分类': 'FolderOpened',
    '标签': 'PriceTag',
    '题目': 'Edit',
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
      return icon
    }
  }
  
  // 默认图标
  return 'Document'
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
    'knowledge': '知识图谱管理', 
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
const handleMenuSelect = (item) => {
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
      if (searchInput.value) {
        searchInput.value.focus()
      }
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
    } else if (searchInput.value) {
      searchInput.value.focus()
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
      } catch (error) {
        console.log('取消退出:', error)
      }
      break
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background-color: #001529;
  transition: width 0.2s;
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #1e3a8a;
}

.header {
  background-color: white;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
}

.username {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.username:hover {
  background-color: #f5f5f5;
}

.sidebar-menu {
  border: none;
  background-color: #001529;
  flex: 1;
  overflow-y: auto;
}

.sidebar-menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.8);
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background-color: #1890ff !important;
  color: white;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background-color: #1890ff !important;
  color: white;
}

.sidebar-menu :deep(.el-sub-menu__title) {
  color: rgba(255, 255, 255, 0.8);
}

.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background-color: #1890ff !important;
  color: white;
}

.main-content {
  background-color: #f0f2f5;
  padding: 0;
}

.page-container {
  height: 100%;
  overflow: auto;
}

/* 侧边栏搜索输入框容器 */
.sidebar-search-input-wrapper {
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  flex-shrink: 0; /* 防止搜索区域被压缩 */
}

.sidebar-search-input {
  width: 100%;
}

.sidebar-search :deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  transition: all 0.3s ease;
}

.sidebar-search :deep(.el-input__wrapper:hover) {
  background-color: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.3);
}

.sidebar-search :deep(.el-input__wrapper.is-focus) {
  background-color: rgba(255, 255, 255, 0.2);
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.sidebar-search :deep(.el-input__inner) {
  color: white;
  font-size: 13px;
}

.sidebar-search :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.6);
}

.sidebar-search :deep(.el-input__prefix) {
  color: rgba(255, 255, 255, 0.8);
}

/* 搜索结果容器 - 占用菜单区域的空间 */
.search-results-container {
  flex: 1; /* 占用剩余空间，和菜单区域一样 */
  overflow-y: auto;
  padding: 0 16px 16px 16px;
}

/* 搜索结果区域 */
.search-results {
  border-radius: 6px;
  background-color: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

/* 搜索结果滚动条样式 */
.search-results::-webkit-scrollbar {
  width: 4px;
}

.search-results::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
}

.search-results::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
}

.search-results::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}

.search-results-header {
  padding: 8px 12px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background-color: rgba(255, 255, 255, 0.05);
}

.search-result-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.search-result-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.search-result-item:last-child {
  border-bottom: none;
}

.search-result-icon {
  margin-right: 10px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 16px;
  flex-shrink: 0;
}

.search-result-content {
  flex: 1;
  min-width: 0;
}

.search-result-title {
  font-size: 13px;
  font-weight: 500;
  color: white;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.search-result-path {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 无搜索结果 */
.search-no-results {
  text-align: center;
  padding: 40px 20px;
  color: rgba(255, 255, 255, 0.6);
  border-radius: 6px;
  background-color: rgba(255, 255, 255, 0.02);
}

.no-results-icon {
  font-size: 24px;
  margin-bottom: 8px;
  opacity: 0.5;
}

.no-results-text {
  font-size: 12px;
}

/* 折叠状态下的搜索按钮 */
.sidebar-search-collapsed {
  display: flex;
  justify-content: center;
  padding: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.search-toggle-btn {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex !important;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.8) !important;
  transition: all 0.3s ease;
}

.search-toggle-btn:hover {
  background-color: rgba(255, 255, 255, 0.1) !important;
  color: white !important;
}
</style> 
