<template>
  <div id="app">
    <a href="#app-main-content" class="skip-link">跳到主要内容</a>

    <!-- 全局导航栏 -->
    <div class="global-header" v-if="!isAuthPage" :class="{ 'header-scrolled': isHeaderScrolled }">
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
          <el-dropdown trigger="hover" @command="handleNavCommand" popper-class="global-nav-popper">
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
          <el-dropdown trigger="hover" @command="handleNavCommand" popper-class="global-nav-popper">
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
          <el-dropdown trigger="hover" @command="handleNavCommand" popper-class="global-nav-popper">
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
          
          <el-dropdown @command="handleUserAction" placement="bottom-end" popper-class="global-nav-popper">
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
    <div id="app-main-content" tabindex="-1" class="app-main" :class="{ 'with-header': !isAuthPage }">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
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
const isHeaderScrolled = ref(false)

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

const handleWindowScroll = () => {
  if (isAuthPage.value) {
    isHeaderScrolled.value = false
    return
  }
  isHeaderScrolled.value = window.scrollY > 8
}

onMounted(() => {
  handleWindowScroll()
  window.addEventListener('scroll', handleWindowScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleWindowScroll)
})
</script>

<style>
#app {
  min-height: 100vh;
  overflow-x: hidden;
  font-family: var(--cn-font-sans);
  color: var(--cn-text-primary);
}

.skip-link {
  position: fixed;
  left: 14px;
  top: -44px;
  z-index: 1200;
  padding: 8px 12px;
  border-radius: 8px;
  background: #1f6feb;
  color: #fff;
  text-decoration: none;
  font-size: 13px;
  transition: top var(--cn-motion-fast) var(--cn-ease-out);
}

.skip-link:focus {
  top: 12px;
}

.global-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--cn-border-soft);
  box-shadow: 0 10px 30px rgba(18, 38, 63, 0.06);
  transition:
    background-color var(--cn-motion-base) var(--cn-ease-out),
    box-shadow var(--cn-motion-base) var(--cn-ease-out),
    border-color var(--cn-motion-base) var(--cn-ease-out);
}

.global-header.header-scrolled {
  background: rgba(255, 255, 255, 0.94);
  border-bottom-color: #d8e4f6;
  box-shadow: 0 14px 34px rgba(18, 38, 63, 0.1);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 22px;
  height: 68px;
  max-width: 1360px;
  margin: 0 auto;
  transition: height var(--cn-motion-base) var(--cn-ease-out);
}

.global-header.header-scrolled .header-content {
  height: 64px;
}

.logo {
  cursor: pointer;
  transition: var(--cn-transition);
}

.logo h2 {
  margin: 0;
  font-family: var(--cn-font-heading);
  font-size: 24px;
  font-weight: 600;
  letter-spacing: 0.03em;
  color: var(--cn-primary);
}

.logo:hover {
  transform: translateY(-1px);
  opacity: 0.92;
}

.main-nav {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--cn-border-soft);
  border-radius: 14px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.nav-item {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 10px;
  color: var(--cn-text-secondary);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: var(--cn-transition);
  white-space: nowrap;
}

.nav-item::after {
  content: '';
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: 4px;
  height: 2px;
  border-radius: 999px;
  background: linear-gradient(90deg, #1f6feb 0%, #4d93ff 100%);
  transform: scaleX(0);
  transform-origin: center;
  transition: transform var(--cn-motion-base) var(--cn-ease-out);
}

.nav-item:hover {
  color: var(--cn-primary);
  background: #edf3ff;
}

.nav-item:hover::after,
.nav-item.active::after {
  transform: scaleX(1);
}

.nav-item.active {
  color: var(--cn-primary);
  font-weight: 600;
  background: linear-gradient(180deg, #f3f8ff 0%, #eaf2ff 100%);
  box-shadow: inset 0 0 0 1px #d7e5ff;
}

.nav-item .el-icon {
  font-size: 16px;
}

.nav-dropdown {
  cursor: pointer;
  position: relative;
}

.dropdown-arrow {
  margin-left: 2px;
  font-size: 12px;
  transition: transform 0.25s ease;
}

.nav-dropdown:hover .dropdown-arrow {
  transform: rotate(180deg);
}

.global-nav-popper .el-dropdown-menu {
  border: 1px solid var(--cn-border-soft);
  border-radius: 10px;
  box-shadow: 0 12px 30px rgba(18, 38, 63, 0.1);
}

.global-nav-popper .el-dropdown-menu__item {
  padding: 8px 16px;
  color: var(--cn-text-secondary);
}

.global-nav-popper .el-dropdown-menu__item .el-icon {
  margin-right: 8px;
  font-size: 14px;
  color: #6c84b5;
}

.global-nav-popper .el-dropdown-menu__item:not(.is-disabled):hover {
  background: #eef4ff;
  color: var(--cn-primary);
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-item {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: var(--cn-transition);
}

.action-item:hover {
  border-color: #d6e3f8;
  background: #eef4ff;
}

.action-item .el-icon {
  font-size: 18px;
  color: var(--cn-text-secondary);
}

.action-item:hover .el-icon {
  color: var(--cn-primary);
}

.user-avatar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 10px 5px 6px;
  border: 1px solid transparent;
  border-radius: 10px;
  cursor: pointer;
  transition: var(--cn-transition);
}

.user-avatar:hover {
  border-color: #d6e3f8;
  background: #eef4ff;
}

.username {
  max-width: 104px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 500;
  color: var(--cn-text-secondary);
}

.app-main {
  position: relative;
  min-height: 100vh;
  background:
    radial-gradient(circle at 2% -18%, rgba(31, 111, 235, 0.2) 0%, rgba(31, 111, 235, 0) 32%),
    radial-gradient(circle at 86% -20%, rgba(107, 165, 255, 0.22) 0%, rgba(107, 165, 255, 0) 38%),
    linear-gradient(170deg, #f7faff 0%, #f1f5fc 65%, #edf2fa 100%);
}

.app-main.with-header {
  padding-top: 68px;
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition:
    opacity var(--cn-motion-base) var(--cn-ease-in-out),
    transform var(--cn-motion-base) var(--cn-ease-out);
}

.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@media (max-width: 1024px) {
  .header-content {
    padding: 0 14px;
  }

  .main-nav {
    gap: 2px;
    padding: 4px;
  }

  .nav-item {
    padding: 7px 10px;
  }
}

@media (max-width: 768px) {
  .header-content {
    height: 62px;
    gap: 8px;
  }

  .global-header.header-scrolled .header-content {
    height: 58px;
  }

  .app-main.with-header {
    padding-top: 62px;
  }

  .main-nav {
    flex: 1;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .main-nav::-webkit-scrollbar {
    display: none;
  }

  .nav-item {
    padding: 7px 10px;
    font-size: 13px;
  }

  .nav-item span {
    display: none;
  }

  .username {
    display: none;
  }
}

@media (max-width: 480px) {
  .logo h2 {
    font-size: 20px;
  }

  .action-item {
    width: 34px;
    height: 34px;
  }
}
</style> 
