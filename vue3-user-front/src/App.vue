<template>
  <div id="app">
    <!-- 全局导航栏 -->
    <div class="global-header" v-if="!isAuthPage">
      <div class="header-content">
        <!-- Logo区域 -->
        <div class="logo" @click="goHome">
          <h2>Code Nest</h2>
        </div>
        
        <!-- 主导航区域 -->
        <div class="main-nav">
          <router-link to="/" class="nav-item" active-class="active">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </router-link>
          <router-link to="/community" class="nav-item" active-class="active">
            <el-icon><ChatDotRound /></el-icon>
            <span>技术社区</span>
          </router-link>
          <router-link to="/moments" class="nav-item" active-class="active">
            <el-icon><Picture /></el-icon>
            <span>朋友圈</span>
          </router-link>
          <router-link to="/chat" class="nav-item" active-class="active">
            <el-icon><Message /></el-icon>
            <span>聊天室</span>
          </router-link>
          
          <!-- 学习工具下拉菜单 -->
          <el-dropdown trigger="hover" @command="handleNavCommand">
            <div class="nav-item nav-dropdown">
              <el-icon><Document /></el-icon>
              <span>学习</span>
              <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="/interview">
                  <el-icon><Document /></el-icon>
                  面试题库
                </el-dropdown-item>
                <el-dropdown-item command="/mock-interview">
                  <el-icon><Mic /></el-icon>
                  AI模拟面试
                </el-dropdown-item>
                <el-dropdown-item command="/knowledge">
                  <el-icon><DataAnalysis /></el-icon>
                  知识图谱
                </el-dropdown-item>
                <el-dropdown-item command="/plan">
                  <el-icon><Calendar /></el-icon>
                  计划打卡
                </el-dropdown-item>
                <el-dropdown-item command="/team">
                  <el-icon><UserFilled /></el-icon>
                  学习小组
                </el-dropdown-item>
                <el-dropdown-item command="/flashcard">
                  <el-icon><Postcard /></el-icon>
                  闪卡记忆
                </el-dropdown-item>
                <el-dropdown-item divided command="/oj">
                  <el-icon><Monitor /></el-icon>
                  在线判题
                </el-dropdown-item>
                <el-dropdown-item command="/oj/playground">
                  <el-icon><Cpu /></el-icon>
                  练习场
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          
          <!-- 创作工具下拉菜单 -->
          <el-dropdown trigger="hover" @command="handleNavCommand">
            <div class="nav-item nav-dropdown">
              <el-icon><Tools /></el-icon>
              <span>创作</span>
              <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="/codepen">
                  <el-icon><Promotion /></el-icon>
                  代码共享器
                </el-dropdown-item>
                <el-dropdown-item command="/blog">
                  <el-icon><Reading /></el-icon>
                  我的博客
                </el-dropdown-item>
                <el-dropdown-item command="/resume">
                  <el-icon><EditPen /></el-icon>
                  简历工坊
                </el-dropdown-item>
                <el-dropdown-item command="/dev-tools">
                  <el-icon><Tools /></el-icon>
                  程序员工具
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          
          <!-- 娱乐工具下拉菜单 -->
          <el-dropdown trigger="hover" @command="handleNavCommand">
            <div class="nav-item nav-dropdown">
              <el-icon><Coffee /></el-icon>
              <span>娱乐</span>
              <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="/moyu-tools">
                  <el-icon><Coffee /></el-icon>
                  摸鱼工具
                </el-dropdown-item>
                <el-dropdown-item command="/lottery">
                  <el-icon><Trophy /></el-icon>
                  幸运抽奖
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        
        <!-- 用户操作区域 -->
        <div class="user-actions">
          <div class="action-item" @click="goToNotification">
            <el-badge :value="unreadCount" :hidden="unreadCount === 0">
              <el-icon><Bell /></el-icon>
            </el-badge>
          </div>
          
          <el-dropdown @command="handleUserAction" placement="bottom-end">
            <div class="user-avatar">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar" :icon="UserFilled" />
              <span v-if="userStore.userInfo" class="username">{{ userStore.userInfo.username }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item command="mypens">
                  <el-icon><Promotion /></el-icon>
                  我的作品
                </el-dropdown-item>
                <el-dropdown-item command="points">
                  <el-icon><Trophy /></el-icon>
                  积分中心
                </el-dropdown-item>
                <el-dropdown-item command="version">
                  <el-icon><Calendar /></el-icon>
                  版本历史
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>
    
    <!-- 主要内容区域 -->
    <div class="app-main" :class="{ 'with-header': !isAuthPage }">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  HomeFilled, Document, DataAnalysis, ChatDotRound, Picture, Bell, 
  User, UserFilled, SwitchButton, Calendar, Tools, Coffee, Message, Trophy, Reading, Promotion, ArrowDown, EditPen, Mic, Postcard,
  Monitor, Cpu
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 未读消息数量
const unreadCount = ref(0)

// 判断是否是认证页面（登录/注册）
const isAuthPage = computed(() => {
  return route.path === '/login' || route.path === '/register'
})

// 回到首页
const goHome = () => {
  router.push('/')
}

// 跳转到通知中心
const goToNotification = () => {
  router.push('/notification')
}

// 处理导航下拉菜单命令
const handleNavCommand = (command) => {
  router.push(command)
}

// 处理用户操作
const handleUserAction = async (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'mypens':
      router.push('/codepen/my')
      break
    case 'points':
      router.push('/points')
      break
    case 'version':
      router.push('/version-history')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        userStore.logout()
        ElMessage.success('退出登录成功')
        router.push('/login')
      } catch (error) {
        // 用户取消
      }
      break
  }
}
</script>

<style>
#app {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  height: 100vh;
  overflow-x: hidden;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* 全局导航栏样式 */
.global-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 64px;
  max-width: 1200px;
  margin: 0 auto;
}

/* Logo样式 */
.logo {
  cursor: pointer;
  transition: all 0.3s ease;
}

.logo h2 {
  margin: 0;
  color: #1890ff;
  font-size: 24px;
  font-weight: bold;
}

.logo:hover h2 {
  color: #40a9ff;
}

/* 主导航样式 */
.main-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  color: #666;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.nav-item:hover {
  color: #1890ff;
  background-color: rgba(24, 144, 255, 0.06);
}

.nav-item.active {
  color: #1890ff;
  background-color: rgba(24, 144, 255, 0.1);
  font-weight: 600;
}

.nav-item .el-icon {
  font-size: 16px;
}

/* 导航下拉菜单样式 */
.nav-dropdown {
  cursor: pointer;
  position: relative;
}

.dropdown-arrow {
  font-size: 12px;
  margin-left: 2px;
  transition: transform 0.3s ease;
}

.nav-dropdown:hover .dropdown-arrow {
  transform: rotate(180deg);
}

/* 下拉菜单项样式 */
:deep(.el-dropdown-menu__item) {
  padding: 8px 16px;
}

:deep(.el-dropdown-menu__item .el-icon) {
  margin-right: 8px;
  font-size: 14px;
}

/* 用户操作区域样式 */
.user-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.action-item {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.action-item:hover {
  background-color: rgba(24, 144, 255, 0.06);
}

.action-item .el-icon {
  font-size: 18px;
  color: #666;
}

.action-item:hover .el-icon {
  color: #1890ff;
}

.user-avatar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.user-avatar:hover {
  background-color: rgba(24, 144, 255, 0.06);
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 主要内容区域样式 */
.app-main {
  min-height: 100vh;
}

.app-main.with-header {
  padding-top: 64px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    padding: 0 16px;
  }
  
  .main-nav {
    gap: 4px;
  }
  
  .nav-item {
    padding: 6px 12px;
    font-size: 13px;
  }
  
  .nav-item span {
    display: none;
  }
  
  .username {
    display: none;
  }
  
  .user-actions {
    gap: 12px;
  }
}

@media (max-width: 480px) {
  .logo h2 {
    font-size: 20px;
  }
  
  .nav-item {
    padding: 6px 8px;
  }
  
  .main-nav {
    gap: 2px;
  }
}

/* 全局过渡动画 */
.v-enter-active,
.v-leave-active {
  transition: all 0.3s ease;
}

.v-enter-from,
.v-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style> 
