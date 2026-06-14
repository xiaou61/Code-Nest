<template>
  <div id="app">
    <a href="#app-main-content" class="skip-link">跳到主要内容</a>

    <CnTopNav
      v-if="!isAuthPage"
      v-model:mobile-open="mobileMenuVisible"
      :primary-items="primaryNavItems"
      :dropdowns="desktopDropdowns"
      :mobile-sections="mobileSections"
      :active-path="route.path"
      :active-full-path="route.fullPath"
      :fallback-label="route.meta?.title || '工作台'"
      :user="userStore.userInfo"
      :user-actions="userActions"
      :unread-count="unreadCount"
      :scrolled="isHeaderScrolled"
      @brand-click="navigate('/')"
      @search="openCommandPalette"
      @notification="navigate('/notification')"
      @navigate="navigate"
      @user-action="handleUserAction"
    />

    <div id="app-main-content" tabindex="-1" class="app-main" :class="{ 'with-header': !isAuthPage }">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>

    <CnCommandPalette
      v-model="commandVisible"
      v-model:keyword="commandKeyword"
      :sections="commandSections"
      :recent-items="recentCommandItems"
      :active-path="route.path"
      @select="navigateFromPalette"
      @close="commandKeyword = ''"
    />

  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { CnCommandPalette, CnTopNav } from '@/design-system'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell,
  Calendar,
  Postcard,
  Promotion,
  SwitchButton,
  Trophy,
  User,
  UserFilled
} from '@element-plus/icons-vue'
import {
  commandSections,
  creationMenuItems,
  desktopDropdowns,
  flattenCommandItems,
  learningMenuGroups,
  leisureMenuItems,
  primaryNavItems
} from '@/config/navigation'
import { readCommandHistory, writeCommandHistory } from '@/utils/command-history'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const unreadCount = ref(0)
const isHeaderScrolled = ref(false)
const commandVisible = ref(false)
const commandKeyword = ref('')
const mobileMenuVisible = ref(false)

const allCommandItems = flattenCommandItems(commandSections)

const isAuthPage = computed(() => route.path === '/login' || route.path === '/register')

const recentCommandPaths = ref(readCommandHistory())

const recentCommandItems = computed(() =>
  recentCommandPaths.value
    .map((path) => allCommandItems.find((item) => item.path === path))
    .filter(Boolean)
)

const mobileSections = computed(() => [
  {
    key: 'learning',
    title: '学习',
    items: learningMenuGroups.flatMap((group) => group.items)
  },
  {
    key: 'creation',
    title: '创作',
    items: creationMenuItems
  },
  {
    key: 'leisure',
    title: '娱乐与辅助',
    items: leisureMenuItems.concat([
      { path: '/points', label: '积分中心', desc: '签到、积分和排行榜', icon: Trophy },
      { path: '/notification', label: '通知中心', desc: '查看系统消息和提醒', icon: Bell },
      { path: '/profile', label: '个人中心', desc: '账号设置与信息维护', icon: UserFilled }
    ])
  }
])

const userActions = [
  { command: 'profile', label: '个人中心', icon: User },
  { command: 'learningAssets', label: '我的学习资产', icon: Postcard },
  { command: 'mypens', label: '我的作品', icon: Promotion },
  { command: 'points', label: '积分中心', icon: Trophy },
  { command: 'version', label: '版本历史', icon: Calendar },
  { command: 'logout', label: '退出登录', icon: SwitchButton, divided: true }
]

function rememberCommand(path) {
  recentCommandPaths.value = writeCommandHistory(path)
}

function navigate(path) {
  if (!path) return
  rememberCommand(path)
  router.push(path)
}

function navigateFromPalette(item) {
  commandVisible.value = false
  commandKeyword.value = ''
  navigate(item.path)
}

function openCommandPalette() {
  mobileMenuVisible.value = false
  commandVisible.value = true
}

async function handleUserAction(command) {
  switch (command) {
    case 'profile':
      navigate('/profile')
      break
    case 'learningAssets':
      navigate('/learning-assets')
      break
    case 'mypens':
      navigate('/codepen/my')
      break
    case 'points':
      navigate('/points')
      break
    case 'version':
      navigate('/version-history')
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

function handleWindowScroll() {
  if (isAuthPage.value) {
    isHeaderScrolled.value = false
    return
  }
  isHeaderScrolled.value = window.scrollY > 8
}

function handleGlobalKeydown(event) {
  const isShortcut = (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k'
  if (isShortcut) {
    event.preventDefault()
    openCommandPalette()
    return
  }

  if (event.key === 'Escape') {
    commandVisible.value = false
    mobileMenuVisible.value = false
  }
}

watch(
  () => route.fullPath,
  () => {
    commandVisible.value = false
    mobileMenuVisible.value = false
    commandKeyword.value = ''
  }
)

onMounted(() => {
  handleWindowScroll()
  window.addEventListener('scroll', handleWindowScroll, { passive: true })
  window.addEventListener('keydown', handleGlobalKeydown)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleWindowScroll)
  window.removeEventListener('keydown', handleGlobalKeydown)
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
  background: var(--cn-color-brand-primary);
  color: var(--cn-button-primary-color);
  text-decoration: none;
  font-size: 13px;
  transition: top var(--cn-motion-fast) var(--cn-ease-out);
}

.skip-link:focus {
  top: 12px;
}

.app-main {
  position: relative;
  min-height: 100vh;
  background: var(--cn-color-bg-page);
}

.app-main.with-header {
  padding-top: 74px;
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

@media (max-width: 992px) {
  .app-main.with-header {
    padding-top: 66px;
  }
}
</style>
