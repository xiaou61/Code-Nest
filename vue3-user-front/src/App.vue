<template>
  <div id="app">
    <a href="#app-main-content" class="skip-link">跳到主要内容</a>

    <div class="global-header" v-if="!isAuthPage" :class="{ 'header-scrolled': isHeaderScrolled }">
      <div class="header-content">
        <div class="header-left">
          <div class="logo" @click="goHome">
            <h2>Code Nest</h2>
            <span class="logo-subtitle">Developer Growth OS</span>
          </div>

          <div class="workspace-pill">
            <span class="workspace-label">当前</span>
            <span class="workspace-title">{{ currentRouteLabel }}</span>
          </div>
        </div>

        <div class="main-nav desktop-only">
          <router-link
            v-for="item in primaryNavItems"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ active: isMenuRouteActive(item) }"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </router-link>

          <el-dropdown
            v-for="dropdown in desktopDropdowns"
            :key="dropdown.key"
            trigger="hover"
            @command="handleNavCommand"
            :popper-class="dropdown.key === 'learning' ? 'global-nav-popper global-nav-learning-popper' : 'global-nav-popper global-nav-rich-popper'"
          >
            <div class="nav-item nav-dropdown" :class="{ active: isDropdownActive(dropdown) }">
              <el-icon><component :is="dropdown.icon" /></el-icon>
              <span>{{ dropdown.label }}</span>
              <el-icon class="dropdown-arrow"><component :is="dropdown.arrowIcon" /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu v-if="dropdown.groups">
                <template v-for="group in dropdown.groups" :key="group.title">
                  <el-dropdown-item disabled class="learn-menu-group-label">
                    {{ group.title }}
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-for="item in group.items"
                    :key="item.path"
                    :command="item.path"
                    :class="{ 'is-route-active': isMenuRouteActive(item) }"
                  >
                    <span class="learn-menu-item">
                      <span class="learn-menu-icon">
                        <el-icon><component :is="item.icon" /></el-icon>
                      </span>
                      <span class="learn-menu-text">
                        <span class="learn-menu-title">{{ item.label }}</span>
                        <span class="learn-menu-desc">{{ item.desc }}</span>
                      </span>
                    </span>
                  </el-dropdown-item>
                </template>
              </el-dropdown-menu>

              <el-dropdown-menu v-else>
                <el-dropdown-item
                  v-for="item in dropdown.items"
                  :key="item.path"
                  :command="item.path"
                  :class="{ 'is-route-active': isMenuRouteActive(item) }"
                >
                  <span class="rich-menu-item">
                    <span class="rich-menu-icon">
                      <el-icon><component :is="item.icon" /></el-icon>
                    </span>
                    <span class="rich-menu-text">
                      <span class="rich-menu-title">{{ item.label }}</span>
                      <span class="rich-menu-desc">{{ item.desc }}</span>
                    </span>
                  </span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <div class="user-actions">
          <button class="search-trigger desktop-only" type="button" @click="openCommandPalette">
            <el-icon><Search /></el-icon>
            <span>全站搜索</span>
            <kbd>Ctrl K</kbd>
          </button>

          <button class="icon-ghost mobile-only" type="button" @click="openCommandPalette" aria-label="打开全站搜索">
            <el-icon><Search /></el-icon>
          </button>

          <button class="icon-ghost mobile-only" type="button" @click="mobileMenuVisible = true" aria-label="打开导航菜单">
            <el-icon><Operation /></el-icon>
          </button>

          <div class="header-action-item" @click="goToNotification">
            <el-badge :value="unreadCount" :hidden="unreadCount === 0">
              <el-icon><Bell /></el-icon>
            </el-badge>
          </div>

          <el-dropdown @command="handleUserAction" placement="bottom-end" popper-class="global-nav-popper">
            <div class="user-avatar">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar" :icon="UserFilled" />
              <div v-if="userStore.userInfo" class="user-copy">
                <span class="username">{{ userStore.userInfo.username }}</span>
                <span class="user-caption">个人工作台</span>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item command="learningAssets">
                  <el-icon><Postcard /></el-icon>
                  我的学习资产
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

    <div id="app-main-content" tabindex="-1" class="app-main" :class="{ 'with-header': !isAuthPage }">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>

    <el-dialog
      v-model="commandVisible"
      class="command-dialog"
      width="680px"
      align-center
      :show-close="false"
      destroy-on-close
    >
      <template #header>
        <div class="command-header">
          <div class="command-title">
            <strong>全站命令面板</strong>
            <span>更快地打开模块、工具和工作台</span>
          </div>
          <button class="command-close" type="button" @click="commandVisible = false" aria-label="关闭">
            Esc
          </button>
        </div>
      </template>

      <div class="command-body">
        <el-input
          ref="commandInputRef"
          v-model="commandKeyword"
          clearable
          size="large"
          placeholder="搜索页面、功能或场景"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <div class="command-scroll">
          <section v-if="recentCommandItems.length" class="command-section">
            <div class="command-section-title">最近访问</div>
            <button
              v-for="item in recentCommandItems"
              :key="`recent-${item.path}`"
              type="button"
              class="command-item"
              @click="navigateFromPalette(item)"
            >
              <span class="command-item-icon recent">
                <el-icon><component :is="item.icon" /></el-icon>
              </span>
              <span class="command-item-copy">
                <strong>{{ item.label }}</strong>
                <small>{{ item.desc }}</small>
              </span>
              <span class="command-item-path">{{ item.path }}</span>
            </button>
          </section>

          <section v-for="section in filteredCommandSections" :key="section.key" class="command-section">
            <div class="command-section-title">{{ section.title }}</div>
            <button
              v-for="item in section.items"
              :key="item.path"
              type="button"
              class="command-item"
              :class="{ active: isMenuRouteActive(item) }"
              @click="navigateFromPalette(item)"
            >
              <span class="command-item-icon">
                <el-icon><component :is="item.icon" /></el-icon>
              </span>
              <span class="command-item-copy">
                <strong>{{ item.label }}</strong>
                <small>{{ item.desc }}</small>
              </span>
              <span class="command-item-path">
                <el-icon><Right /></el-icon>
              </span>
            </button>
          </section>

          <el-empty v-if="!filteredCommandSections.length && !recentCommandItems.length" description="没有找到匹配功能" />
        </div>
      </div>
    </el-dialog>

    <el-drawer
      v-model="mobileMenuVisible"
      class="mobile-nav-drawer"
      direction="rtl"
      size="88%"
      :with-header="false"
    >
      <div class="mobile-nav-shell">
        <div class="mobile-nav-top">
          <div>
            <strong>Code Nest</strong>
            <p>把常用模块收进一个清晰的移动工作台。</p>
          </div>
          <button class="command-close" type="button" @click="mobileMenuVisible = false">关闭</button>
        </div>

        <button class="mobile-search-trigger" type="button" @click="openCommandPalette">
          <el-icon><Search /></el-icon>
          <span>打开全站命令面板</span>
        </button>

        <section class="mobile-nav-section">
          <div class="mobile-nav-title">主入口</div>
          <button
            v-for="item in primaryNavItems"
            :key="item.path"
            type="button"
            class="mobile-nav-item"
            :class="{ active: isMenuRouteActive(item) }"
            @click="navigateInMobile(item.path)"
          >
            <span class="mobile-nav-icon">
              <el-icon><component :is="item.icon" /></el-icon>
            </span>
            <span>{{ item.label }}</span>
          </button>
        </section>

        <section
          v-for="section in mobileSections"
          :key="section.key"
          class="mobile-nav-section"
        >
          <div class="mobile-nav-title">{{ section.title }}</div>
          <button
            v-for="item in section.items"
            :key="item.path"
            type="button"
            class="mobile-nav-item"
            :class="{ active: isMenuRouteActive(item) }"
            @click="navigateInMobile(item.path)"
          >
            <span class="mobile-nav-icon">
              <el-icon><component :is="item.icon" /></el-icon>
            </span>
            <span class="mobile-nav-copy">
              <strong>{{ item.label }}</strong>
              <small>{{ item.desc }}</small>
            </span>
          </button>
        </section>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell,
  Calendar,
  Operation,
  Postcard,
  Promotion,
  Right,
  Search,
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
const commandInputRef = ref()
const mobileMenuVisible = ref(false)

const allCommandItems = flattenCommandItems(commandSections)

const isAuthPage = computed(() => route.path === '/login' || route.path === '/register')

const recentCommandPaths = ref(readCommandHistory())

const filteredCommandSections = computed(() => {
  const keyword = commandKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return commandSections
  }

  return commandSections
    .map((section) => ({
      ...section,
      items: section.items.filter((item) =>
        [item.label, item.desc, item.path].some((field) => field?.toLowerCase().includes(keyword))
      )
    }))
    .filter((section) => section.items.length > 0)
})

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

const currentRouteLabel = computed(() => {
  const current = allCommandItems.find((item) => isMenuRouteActive(item)) ||
    primaryNavItems.find((item) => isMenuRouteActive(item))
  return current?.label || route.meta?.title || '工作台'
})

function normalizeMatchPrefixes(item) {
  if (Array.isArray(item?.matchPrefixes) && item.matchPrefixes.length > 0) {
    return item.matchPrefixes
  }
  return item?.path ? [item.path] : []
}

function isMenuRouteActive(item) {
  return normalizeMatchPrefixes(item).some((prefix) => {
    if (prefix === '/') {
      return route.path === '/'
    }
    return route.path === prefix || route.path.startsWith(`${prefix}/`) || route.fullPath.startsWith(prefix)
  })
}

function isDropdownActive(dropdown) {
  const items = dropdown.groups ? dropdown.groups.flatMap((group) => group.items) : dropdown.items
  return items.some((item) => isMenuRouteActive(item))
}

function goHome() {
  router.push('/')
}

function goToNotification() {
  router.push('/notification')
}

function handleNavCommand(command) {
  navigate(command)
}

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

function navigateInMobile(path) {
  mobileMenuVisible.value = false
  navigate(path)
}

function openCommandPalette() {
  mobileMenuVisible.value = false
  commandVisible.value = true
  nextTick(() => {
    commandInputRef.value?.focus?.()
  })
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
  inset: 0 0 auto;
  z-index: 1000;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(14px);
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
  height: 74px;
  max-width: 1440px;
  margin: 0 auto;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.logo {
  display: flex;
  flex-direction: column;
  gap: 2px;
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

.logo-subtitle {
  font-size: 11px;
  color: #7b8aa5;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.logo:hover {
  transform: translateY(-1px);
  opacity: 0.92;
}

.workspace-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(237, 244, 255, 0.9);
  border: 1px solid #d9e7fd;
}

.workspace-label {
  font-size: 12px;
  color: #6b7f9f;
}

.workspace-title {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 600;
  color: #1d4f9e;
}

.main-nav {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  padding: 5px;
  background: rgba(255, 255, 255, 0.76);
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
}

.dropdown-arrow {
  margin-left: 2px;
  font-size: 12px;
  transition: transform 0.25s ease;
}

.nav-dropdown:hover .dropdown-arrow {
  transform: rotate(180deg);
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-trigger,
.icon-ghost,
.command-close,
.mobile-search-trigger,
.mobile-nav-item,
.command-item {
  appearance: none;
  border: none;
  background: none;
  font: inherit;
}

.search-trigger {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  padding: 0 12px;
  border-radius: 12px;
  border: 1px solid #d9e5f7;
  background: rgba(248, 251, 255, 0.92);
  color: #617797;
  cursor: pointer;
  transition: var(--cn-transition);
}

.search-trigger:hover {
  color: var(--cn-primary);
  border-color: #bfd4ff;
  background: #f2f7ff;
}

.search-trigger kbd {
  padding: 2px 6px;
  border-radius: 6px;
  border: 1px solid #dbe5f4;
  background: #fff;
  font-size: 11px;
  color: #8a99b2;
}

.icon-ghost,
.header-action-item {
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

.icon-ghost:hover,
.header-action-item:hover {
  border-color: #d6e3f8;
  background: #eef4ff;
}

.icon-ghost .el-icon,
.header-action-item .el-icon {
  font-size: 18px;
  color: var(--cn-text-secondary);
}

.icon-ghost:hover .el-icon,
.header-action-item:hover .el-icon {
  color: var(--cn-primary);
}

.user-avatar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 10px 5px 6px;
  border: 1px solid transparent;
  border-radius: 12px;
  cursor: pointer;
  transition: var(--cn-transition);
}

.user-avatar:hover {
  border-color: #d6e3f8;
  background: #eef4ff;
}

.user-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.username {
  max-width: 104px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 600;
  color: var(--cn-text-secondary);
}

.user-caption {
  font-size: 11px;
  color: #8b9ab2;
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
  padding-top: 74px;
}

.global-nav-popper .el-dropdown-menu {
  border: 1px solid var(--cn-border-soft);
  border-radius: 12px;
  box-shadow: 0 16px 34px rgba(16, 47, 89, 0.14);
}

.global-nav-popper .el-dropdown-menu__item {
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

.global-nav-learning-popper .el-dropdown-menu,
.global-nav-rich-popper .el-dropdown-menu {
  min-width: 308px;
  padding: 8px;
  border-radius: 14px;
  border-color: #dbe9fb;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
}

.learn-menu-group-label {
  pointer-events: none;
  min-height: auto;
  padding: 6px 10px 4px;
  margin-top: 4px;
  color: #8b9ab6;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  background: transparent !important;
}

.global-nav-learning-popper .el-dropdown-menu__item,
.global-nav-rich-popper .el-dropdown-menu__item {
  min-height: auto;
  line-height: 1;
  margin: 2px 0;
  padding: 0;
  border-radius: 12px;
  transition:
    background-color var(--cn-motion-fast) var(--cn-ease-out),
    transform var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out);
}

.learn-menu-item,
.rich-menu-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
}

.learn-menu-icon,
.rich-menu-icon,
.command-item-icon,
.mobile-nav-icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #1f6feb;
  background: linear-gradient(135deg, #e9f2ff 0%, #dcecff 100%);
  box-shadow: inset 0 0 0 1px rgba(97, 144, 223, 0.2);
  flex-shrink: 0;
}

.learn-menu-icon .el-icon,
.rich-menu-icon .el-icon,
.command-item-icon .el-icon,
.mobile-nav-icon .el-icon {
  margin-right: 0;
  font-size: 15px;
  color: inherit;
}

.learn-menu-text,
.rich-menu-text,
.command-item-copy,
.mobile-nav-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.learn-menu-title,
.rich-menu-title,
.command-item-copy strong,
.mobile-nav-copy strong {
  font-size: 13px;
  font-weight: 600;
  color: var(--cn-text-primary);
}

.learn-menu-desc,
.rich-menu-desc,
.command-item-copy small,
.mobile-nav-copy small {
  font-size: 12px;
  color: #7f8ba3;
  line-height: 1.4;
}

.global-nav-learning-popper .el-dropdown-menu__item:not(.is-disabled):hover,
.global-nav-rich-popper .el-dropdown-menu__item:not(.is-disabled):hover,
.command-item:hover,
.mobile-nav-item:hover {
  transform: translateY(-1px);
  background: linear-gradient(180deg, #f5f9ff 0%, #e9f2ff 100%);
  box-shadow: inset 0 0 0 1px #d6e7ff;
}

.global-nav-learning-popper .el-dropdown-menu__item.is-route-active,
.global-nav-rich-popper .el-dropdown-menu__item.is-route-active,
.command-item.active,
.mobile-nav-item.active {
  background: linear-gradient(180deg, #f3f8ff 0%, #e4efff 100%);
  box-shadow:
    inset 0 0 0 1px #cde1ff,
    0 6px 14px rgba(28, 101, 209, 0.14);
}

.global-nav-learning-popper .el-dropdown-menu__item.is-route-active .learn-menu-icon,
.global-nav-rich-popper .el-dropdown-menu__item.is-route-active .rich-menu-icon,
.command-item.active .command-item-icon,
.mobile-nav-item.active .mobile-nav-icon {
  color: #fff;
  background: linear-gradient(135deg, #2c7af2 0%, #1793ed 100%);
  box-shadow: none;
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

.command-dialog .el-dialog {
  padding: 0;
}

.command-dialog .el-dialog__header {
  padding: 18px 20px 12px;
}

.command-dialog .el-dialog__body {
  padding: 0 20px 20px;
}

.command-header,
.mobile-nav-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.command-title,
.mobile-nav-top > div {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.command-title span,
.mobile-nav-top p {
  margin: 0;
  font-size: 13px;
  color: #7f8ba3;
}

.command-close {
  padding: 6px 10px;
  border-radius: 10px;
  border: 1px solid #d9e5f7;
  background: #f8fbff;
  color: #6c84b5;
  cursor: pointer;
  transition: var(--cn-transition);
}

.command-close:hover {
  color: var(--cn-primary);
  border-color: #bfd4ff;
  background: #eef4ff;
}

.command-body {
  display: grid;
  gap: 14px;
}

.command-scroll {
  max-height: 62vh;
  overflow-y: auto;
  padding-right: 4px;
}

.command-section {
  display: grid;
  gap: 8px;
}

.command-section + .command-section {
  margin-top: 16px;
}

.command-section-title,
.mobile-nav-title {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: #7f8ba3;
  text-transform: uppercase;
}

.command-item,
.mobile-nav-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 14px;
  cursor: pointer;
  text-align: left;
  transition: var(--cn-transition);
}

.command-item-path {
  margin-left: auto;
  color: #8da0bd;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.command-item-icon.recent {
  color: #0f8d72;
  background: linear-gradient(135deg, #e4fbf5 0%, #d5f5ec 100%);
  box-shadow: inset 0 0 0 1px rgba(33, 170, 108, 0.18);
}

.mobile-nav-drawer .el-drawer__body {
  padding: 0;
}

.mobile-nav-shell {
  height: 100%;
  padding: 22px 18px 24px;
  display: grid;
  align-content: start;
  gap: 16px;
  background: linear-gradient(180deg, #fbfdff 0%, #f2f7ff 100%);
}

.mobile-search-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 13px 14px;
  border-radius: 14px;
  border: 1px solid #d8e6fb;
  background: #fff;
  color: var(--cn-primary);
  cursor: pointer;
  transition: var(--cn-transition);
}

.mobile-search-trigger:hover {
  border-color: #bfd4ff;
  background: #f3f8ff;
}

.mobile-nav-section {
  display: grid;
  gap: 8px;
}

.mobile-nav-item span:not(.mobile-nav-icon) {
  min-width: 0;
}

.desktop-only {
  display: inline-flex;
}

.mobile-only {
  display: none;
}

@media (max-width: 1180px) {
  .workspace-pill {
    display: none;
  }
}

@media (max-width: 992px) {
  .header-content {
    height: 66px;
    padding: 0 14px;
  }

  .app-main.with-header {
    padding-top: 66px;
  }

  .desktop-only {
    display: none;
  }

  .mobile-only {
    display: inline-flex;
  }

  .user-caption {
    display: none;
  }
}

@media (max-width: 768px) {
  .logo h2 {
    font-size: 20px;
  }

  .logo-subtitle,
  .username {
    display: none;
  }
}

@media (max-width: 480px) {
  .user-actions {
    gap: 8px;
  }

  .icon-ghost,
  .header-action-item {
    width: 34px;
    height: 34px;
  }
}
</style>
