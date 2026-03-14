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

          <el-sub-menu index="/mock-interview">
            <template #title>
              <el-icon><Mic /></el-icon>
              <span>模拟面试运营</span>
            </template>
            <el-menu-item index="/mock-interview/sessions">
              <el-icon><DataAnalysis /></el-icon>
              <span>面试会话</span>
            </el-menu-item>
            <el-menu-item index="/mock-interview/directions">
              <el-icon><SetUp /></el-icon>
              <span>方向配置</span>
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
            <el-menu-item index="/oj/contests">
              <el-icon><Trophy /></el-icon>
              <span>赛事管理</span>
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

          <el-sub-menu index="/learning-assets">
            <template #title>
              <el-icon><Collection /></el-icon>
              <span>学习资产管理</span>
            </template>
            <el-menu-item index="/learning-assets/review">
              <el-icon><Edit /></el-icon>
              <span>审核台</span>
            </el-menu-item>
            <el-menu-item index="/learning-assets/statistics">
              <el-icon><DataAnalysis /></el-icon>
              <span>统计分析</span>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="/ai-growth-coach">
            <template #title>
              <el-icon><DataAnalysis /></el-icon>
              <span>AI成长教练</span>
            </template>
            <el-menu-item index="/ai-growth-coach/statistics">
              <el-icon><DataAnalysis /></el-icon>
              <span>教练统计</span>
            </el-menu-item>
            <el-menu-item index="/ai-growth-coach/failures">
              <el-icon><Warning /></el-icon>
              <span>失败案例</span>
            </el-menu-item>
            <el-menu-item index="/ai-growth-coach/config">
              <el-icon><SetUp /></el-icon>
              <span>策略配置</span>
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
  Check,
  Mic
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
const isMainScrolled = ref(false)

const handleMainScroll = (event) => {
  isMainScrolled.value = event.target.scrollTop > 8
}

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
    'mock-interview': 'Mic',
    'oj': 'Monitor',
    'contests': 'Trophy',
    'categories': 'FolderOpened',
    'tags': 'PriceTag',
    'question-sets': 'Collection',
    'questions': 'Edit',
    'knowledge': 'DataAnalysis',
    'maps': 'Share',
    'ai-growth-coach': 'DataAnalysis',
    'failures': 'Warning',
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
    '模拟面试': 'Mic',
    '赛事': 'Trophy',
    '题单': 'Collection',
    '知识': 'DataAnalysis',
    '图谱': 'Share',
    '教练': 'DataAnalysis',
    '失败': 'Warning',
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
    'mock-interview': '模拟面试运营',
    'knowledge': '知识图谱管理', 
    'learning-assets': '学习资产管理',
    'ai-growth-coach': 'AI成长教练',
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
  background: transparent;
}

.sidebar {
  background: linear-gradient(180deg, #fdfefe 0%, #f7fafe 100%);
  border-right: 1px solid var(--cn-border-soft);
  transition: width 0.24s ease;
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--cn-primary);
  font-family: var(--cn-font-heading);
  font-size: 19px;
  font-weight: 600;
  letter-spacing: 0.02em;
  border-bottom: 1px solid var(--cn-border-soft);
  background: rgba(31, 111, 235, 0.04);
}

.header {
  background-color: rgba(255, 255, 255, 0.9);
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
  background-color: rgba(255, 255, 255, 0.96);
  border-bottom-color: #d8e4f6;
  box-shadow: 0 10px 26px rgba(18, 38, 63, 0.08);
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
  color: var(--cn-primary);
  background: var(--cn-primary-soft);
}

.header-title {
  font-size: 17px;
  color: var(--cn-text-primary);
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
}

.header-avatar {
  margin-right: 8px;
  border: 2px solid #dce6f5;
  color: var(--cn-primary);
  background-color: #f1f6ff;
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
  color: var(--cn-primary);
  background: var(--cn-primary-soft);
}

.sidebar-menu {
  border: none;
  background: transparent;
  flex: 1;
  overflow-y: auto;
  padding: 8px 10px 14px;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 42px;
  line-height: 42px;
  margin: 4px 0;
  border-radius: 10px;
  color: var(--cn-text-secondary);
  font-weight: 500;
}

.sidebar-menu :deep(.el-menu-item .el-icon),
.sidebar-menu :deep(.el-sub-menu__title .el-icon) {
  color: #6281be;
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background: #eef4ff !important;
  color: var(--cn-primary) !important;
}

.sidebar-menu :deep(.el-menu-item:hover .el-icon),
.sidebar-menu :deep(.el-sub-menu__title:hover .el-icon) {
  color: var(--cn-primary);
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  color: var(--cn-primary) !important;
  background: linear-gradient(90deg, #e5efff 0%, #f3f7ff 100%) !important;
  box-shadow: inset 3px 0 0 var(--cn-primary);
}

.sidebar-menu :deep(.el-menu-item.is-active .el-icon) {
  color: var(--cn-primary);
}

.sidebar-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: var(--cn-primary) !important;
}

.sidebar-menu :deep(.el-menu--inline .el-menu-item) {
  padding-left: 46px !important;
}

.main-content {
  background: transparent;
  padding: 16px;
}

.page-container {
  height: 100%;
  overflow: auto;
  background: rgba(255, 255, 255, 0.78);
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

.sidebar-search-input-wrapper {
  padding: 12px 14px;
  border-bottom: 1px solid var(--cn-border-soft);
  background: rgba(255, 255, 255, 0.72);
  flex-shrink: 0;
}

.sidebar-search-input {
  width: 100%;
}

.sidebar-search-input :deep(.el-input__wrapper) {
  background: #f4f8ff;
  border: 1px solid #d6e3f5;
  border-radius: 10px;
  box-shadow: none;
  transition: all 0.3s ease;
}

.sidebar-search-input :deep(.el-input__wrapper:hover) {
  border-color: #bdd2f2;
  background: #f8fbff;
}

.sidebar-search-input :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  border-color: var(--cn-primary);
  box-shadow: 0 0 0 2px rgba(31, 111, 235, 0.12);
}

.sidebar-search-input :deep(.el-input__inner) {
  color: var(--cn-text-primary);
  font-size: 13px;
}

.sidebar-search-input :deep(.el-input__inner::placeholder) {
  color: var(--cn-text-tertiary);
}

.sidebar-search-input :deep(.el-input__prefix) {
  color: #7690bf;
}

.search-results-container {
  flex: 1;
  overflow-y: auto;
  padding: 10px 14px 16px;
}

.search-results {
  border-radius: var(--cn-radius-md);
  background: var(--cn-surface-1);
  border: 1px solid var(--cn-border-soft);
  box-shadow: var(--cn-shadow-xs);
}

.search-results-header {
  padding: 10px 12px;
  font-size: 12px;
  color: var(--cn-text-secondary);
  border-bottom: 1px solid var(--cn-border-soft);
  background: #f6f9ff;
}

.search-result-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  cursor: pointer;
  transition: var(--cn-transition);
  border-bottom: 1px solid #edf2fa;
}

.search-result-item:hover {
  background: #f1f6ff;
}

.search-result-item:last-child {
  border-bottom: none;
}

.search-result-icon {
  margin-right: 10px;
  color: #5f7fbd;
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
  color: var(--cn-text-primary);
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.search-result-path {
  font-size: 11px;
  color: var(--cn-text-tertiary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.search-no-results {
  text-align: center;
  padding: 40px 20px;
  color: var(--cn-text-tertiary);
  border-radius: var(--cn-radius-md);
  background: rgba(255, 255, 255, 0.66);
  border: 1px dashed var(--cn-border);
}

.no-results-icon {
  font-size: 22px;
  margin-bottom: 8px;
  opacity: 0.75;
}

.no-results-text {
  font-size: 12px;
}

.sidebar-search-collapsed {
  display: flex;
  justify-content: center;
  padding: 8px;
  border-bottom: 1px solid var(--cn-border-soft);
}

.search-toggle-btn {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex !important;
  align-items: center;
  justify-content: center;
  color: var(--cn-text-secondary) !important;
  transition: var(--cn-transition);
}

.search-toggle-btn:hover {
  background: var(--cn-primary-soft) !important;
  color: var(--cn-primary) !important;
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
