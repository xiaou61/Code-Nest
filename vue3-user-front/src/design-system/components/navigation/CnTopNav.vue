<template>
  <header class="cn-top-nav" :class="{ 'is-scrolled': scrolled, 'is-fixed': fixed }">
    <div class="cn-top-nav__content">
      <div class="cn-top-nav__left">
        <button class="cn-top-nav__brand" type="button" @click="emit('brand-click')">
          <strong>{{ brand }}</strong>
          <span>{{ subtitle }}</span>
        </button>

        <div v-if="showWorkspace" class="cn-top-nav__workspace">
          <span>{{ workspaceLabel }}</span>
          <strong>{{ currentRouteLabel }}</strong>
        </div>
      </div>

      <nav class="cn-top-nav__main is-desktop" :aria-label="ariaLabel">
        <router-link
          v-for="item in primaryItems"
          :key="item.path"
          :to="item.path"
          class="cn-top-nav__item"
          :class="{ 'is-active': isRouteActive(item) }"
        >
          <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>

        <el-dropdown
          v-for="dropdown in dropdowns"
          :key="dropdown.key"
          trigger="hover"
          :popper-class="getDropdownPopperClass(dropdown)"
          @command="handleNavCommand"
        >
          <button
            class="cn-top-nav__item cn-top-nav__dropdown"
            type="button"
            :class="{ 'is-active': isDropdownActive(dropdown) }"
          >
            <el-icon v-if="dropdown.icon"><component :is="dropdown.icon" /></el-icon>
            <span>{{ dropdown.label }}</span>
            <el-icon class="cn-top-nav__dropdown-arrow">
              <component :is="dropdown.arrowIcon || ArrowDown" />
            </el-icon>
          </button>

          <template #dropdown>
            <el-dropdown-menu v-if="dropdown.groups">
              <template v-for="group in dropdown.groups" :key="group.title">
                <el-dropdown-item disabled class="cn-top-nav-menu__group-label">
                  {{ group.title }}
                </el-dropdown-item>
                <el-dropdown-item
                  v-for="item in group.items"
                  :key="item.path"
                  :command="item.path"
                  :class="{ 'is-route-active': isRouteActive(item) }"
                >
                  <span class="cn-top-nav-menu__item">
                    <span class="cn-top-nav-menu__icon">
                      <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
                    </span>
                    <span class="cn-top-nav-menu__copy">
                      <strong>{{ item.label }}</strong>
                      <small>{{ item.desc }}</small>
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
                :class="{ 'is-route-active': isRouteActive(item) }"
              >
                <span class="cn-top-nav-menu__item">
                  <span class="cn-top-nav-menu__icon">
                    <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
                  </span>
                  <span class="cn-top-nav-menu__copy">
                    <strong>{{ item.label }}</strong>
                    <small>{{ item.desc }}</small>
                  </span>
                </span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </nav>

      <div class="cn-top-nav__actions">
        <CnThemeSwitch v-if="showThemeSwitch" class="cn-top-nav__theme is-desktop" />

        <button
          class="cn-top-nav__search is-desktop"
          type="button"
          :aria-label="`${searchLabel}（${searchShortcut}）`"
          :title="`${searchLabel} · ${searchShortcut}`"
          @click="emit('search')"
        >
          <el-icon><Search /></el-icon>
        </button>

        <button class="cn-top-nav__icon-button is-mobile" type="button" :aria-label="mobileSearchLabel" @click="openMobileSearch">
          <el-icon><Search /></el-icon>
        </button>

        <button class="cn-top-nav__icon-button is-mobile" type="button" :aria-label="mobileMenuLabel" @click="setMobileOpen(true)">
          <el-icon><Operation /></el-icon>
        </button>

        <button class="cn-top-nav__icon-button" type="button" :aria-label="notificationLabel" @click="emit('notification')">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0">
            <el-icon><Bell /></el-icon>
          </el-badge>
        </button>

        <el-dropdown
          placement="bottom-end"
          popper-class="cn-top-nav-popper"
          @command="handleUserCommand"
        >
          <button class="cn-top-nav__user" type="button">
            <el-avatar :size="32" :src="user?.avatar" :icon="UserFilled" />
            <span v-if="user?.username" class="cn-top-nav__user-copy">
              <strong>{{ user.username }}</strong>
              <small>{{ userCaption }}</small>
            </span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-for="action in userActions"
                :key="action.command"
                :command="action.command"
                :divided="action.divided"
              >
                <el-icon v-if="action.icon"><component :is="action.icon" /></el-icon>
                {{ action.label }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <el-drawer
      :model-value="mobileOpen"
      class="cn-top-nav-mobile"
      direction="rtl"
      :size="mobileDrawerSize"
      :with-header="false"
      @update:model-value="setMobileOpen"
    >
      <div class="cn-top-nav-mobile__shell">
        <div class="cn-top-nav-mobile__top">
          <div>
            <strong>{{ brand }}</strong>
            <p>{{ mobileDescription }}</p>
          </div>
          <button class="cn-top-nav-mobile__close" type="button" @click="setMobileOpen(false)">
            {{ closeLabel }}
          </button>
        </div>

        <button class="cn-top-nav-mobile__search" type="button" @click="openMobileSearch">
          <el-icon><Search /></el-icon>
          <span>{{ mobileSearchTriggerLabel }}</span>
        </button>

        <CnThemeSwitch v-if="showThemeSwitch" class="cn-top-nav-mobile__theme" show-label />

        <section class="cn-top-nav-mobile__section">
          <div class="cn-top-nav-mobile__title">{{ primaryMobileTitle }}</div>
          <button
            v-for="item in primaryItems"
            :key="item.path"
            type="button"
            class="cn-top-nav-mobile__item"
            :class="{ 'is-active': isRouteActive(item) }"
            @click="selectMobileItem(item)"
          >
            <span class="cn-top-nav-mobile__icon">
              <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
            </span>
            <span>{{ item.label }}</span>
          </button>
        </section>

        <section
          v-for="section in mobileSections"
          :key="section.key"
          class="cn-top-nav-mobile__section"
        >
          <div class="cn-top-nav-mobile__title">{{ section.title }}</div>
          <button
            v-for="item in section.items"
            :key="item.path"
            type="button"
            class="cn-top-nav-mobile__item"
            :class="{ 'is-active': isRouteActive(item) }"
            @click="selectMobileItem(item)"
          >
            <span class="cn-top-nav-mobile__icon">
              <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
            </span>
            <span class="cn-top-nav-mobile__copy">
              <strong>{{ item.label }}</strong>
              <small>{{ item.desc }}</small>
            </span>
          </button>
        </section>
      </div>
    </el-drawer>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowDown, Bell, Operation, Search, UserFilled } from '@element-plus/icons-vue'
import CnThemeSwitch from '../theme/CnThemeSwitch.vue'
import type {
  CnTopNavDropdown,
  CnTopNavItem,
  CnTopNavProps
} from '../../types/components'

const props = withDefaults(defineProps<CnTopNavProps>(), {
  brand: 'Code Nest',
  subtitle: 'Developer Growth OS',
  workspaceLabel: '当前',
  showWorkspace: false,
  fallbackLabel: '工作台',
  activePath: '',
  activeFullPath: '',
  dropdowns: () => [],
  mobileSections: () => [],
  user: null,
  userCaption: '个人工作台',
  userActions: () => [],
  unreadCount: 0,
  searchLabel: '全站搜索',
  searchShortcut: 'Ctrl K',
  mobileSearchLabel: '打开全站搜索',
  mobileMenuLabel: '打开导航菜单',
  notificationLabel: '打开通知中心',
  mobileSearchTriggerLabel: '打开全站命令面板',
  mobileDescription: '把常用模块收进一个清晰的移动工作台。',
  primaryMobileTitle: '主入口',
  closeLabel: '关闭',
  mobileDrawerSize: '88%',
  ariaLabel: '主导航',
  showThemeSwitch: true,
  mobileOpen: false,
  fixed: true,
  scrolled: false
})

const emit = defineEmits<{
  'update:mobileOpen': [value: boolean]
  'brand-click': []
  search: []
  notification: []
  navigate: [path: string]
  'user-action': [command: string]
}>()

const allNavigationItems = computed<CnTopNavItem[]>(() => [
  ...props.primaryItems,
  ...props.dropdowns.flatMap((dropdown) =>
    dropdown.groups ? dropdown.groups.flatMap((group) => group.items) : dropdown.items || []
  ),
  ...props.mobileSections.flatMap((section) => section.items)
])

const currentRouteLabel = computed(() => {
  return allNavigationItems.value.find((item) => isRouteActive(item))?.label || props.fallbackLabel
})

function normalizeMatchPrefixes(item: CnTopNavItem) {
  if (Array.isArray(item.matchPrefixes) && item.matchPrefixes.length > 0) {
    return item.matchPrefixes
  }
  return item.path ? [item.path] : []
}

function isRouteActive(item: CnTopNavItem) {
  return normalizeMatchPrefixes(item).some((prefix) => {
    if (prefix === '/') {
      return props.activePath === '/'
    }

    return (
      props.activePath === prefix ||
      props.activePath.startsWith(`${prefix}/`) ||
      props.activeFullPath.startsWith(prefix)
    )
  })
}

function isDropdownActive(dropdown: CnTopNavDropdown) {
  const items = dropdown.groups ? dropdown.groups.flatMap((group) => group.items) : dropdown.items || []
  return items.some((item) => isRouteActive(item))
}

function getDropdownPopperClass(dropdown: CnTopNavDropdown) {
  return dropdown.groups
    ? 'cn-top-nav-popper cn-top-nav-learning-popper'
    : 'cn-top-nav-popper cn-top-nav-rich-popper'
}

function setMobileOpen(value: boolean) {
  emit('update:mobileOpen', value)
}

function handleNavCommand(command: string | number | object) {
  emit('navigate', String(command))
}

function handleUserCommand(command: string | number | object) {
  emit('user-action', String(command))
}

function openMobileSearch() {
  setMobileOpen(false)
  emit('search')
}

function selectMobileItem(item: CnTopNavItem) {
  setMobileOpen(false)
  emit('navigate', item.path)
}
</script>

<style>
.cn-top-nav {
  z-index: 1000;
  background: color-mix(in srgb, var(--cn-color-bg-surface) 82%, transparent);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--cn-border-soft);
  box-shadow: 0 10px 30px color-mix(in srgb, var(--cn-color-text-primary) 6%, transparent);
  transition:
    background-color var(--cn-motion-base) var(--cn-ease-out),
    box-shadow var(--cn-motion-base) var(--cn-ease-out),
    border-color var(--cn-motion-base) var(--cn-ease-out);
}

.cn-top-nav.is-fixed {
  position: fixed;
  inset: 0 0 auto;
}

.cn-top-nav.is-scrolled {
  background: color-mix(in srgb, var(--cn-color-bg-surface) 94%, transparent);
  border-bottom-color: color-mix(in srgb, var(--cn-color-brand-primary) 18%, var(--cn-color-border-subtle));
  box-shadow: 0 14px 34px color-mix(in srgb, var(--cn-color-text-primary) 10%, transparent);
}

.cn-top-nav__content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 14px;
  height: 74px;
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 22px;
}

.cn-top-nav__left {
  display: flex;
  align-items: center;
  justify-self: start;
  gap: 16px;
  min-width: 0;
  max-width: 100%;
}

.cn-top-nav__brand,
.cn-top-nav__item,
.cn-top-nav__icon-button,
.cn-top-nav__search,
.cn-top-nav__user,
.cn-top-nav-mobile__close,
.cn-top-nav-mobile__search,
.cn-top-nav-mobile__item {
  appearance: none;
  border: none;
  background: none;
  font: inherit;
}

.cn-top-nav__brand {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  cursor: pointer;
  text-align: left;
  transition: var(--cn-transition);
}

.cn-top-nav__brand strong {
  color: var(--cn-color-brand-primary);
  font-family: var(--cn-font-heading);
  font-size: 24px;
  font-weight: 600;
  letter-spacing: 0.03em;
}

.cn-top-nav__brand span {
  color: var(--cn-color-text-tertiary);
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.cn-top-nav__brand:hover {
  opacity: 0.92;
  transform: translateY(-1px);
}

.cn-top-nav__brand:focus-visible,
.cn-top-nav__item:focus-visible,
.cn-top-nav__search:focus-visible,
.cn-top-nav__icon-button:focus-visible,
.cn-top-nav__user:focus-visible,
.cn-top-nav-mobile__close:focus-visible,
.cn-top-nav-mobile__search:focus-visible,
.cn-top-nav-mobile__item:focus-visible {
  outline: 2px solid var(--cn-color-brand-primary);
  outline-offset: 2px;
}

.cn-top-nav__workspace {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 8px 12px;
  border: 1px solid color-mix(in srgb, var(--cn-color-brand-primary) 18%, var(--cn-color-border-subtle));
  border-radius: 999px;
  background: var(--cn-color-brand-soft);
}

.cn-top-nav__workspace span {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.cn-top-nav__workspace strong {
  max-width: 180px;
  overflow: hidden;
  color: var(--cn-color-brand-primary);
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cn-top-nav__main {
  position: relative;
  display: flex;
  align-items: center;
  justify-self: center;
  gap: 6px;
  min-width: 0;
  padding: 5px;
  border: 1px solid var(--cn-border-soft);
  border-radius: 14px;
  background: color-mix(in srgb, var(--cn-color-bg-surface) 82%, transparent);
  box-shadow: inset 0 1px 0 color-mix(in srgb, var(--cn-color-bg-surface) 86%, transparent);
}

.cn-top-nav__main :deep(.el-tooltip__trigger) {
  display: inline-flex;
  align-items: center;
}

.cn-top-nav__item {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  overflow: hidden;
  min-height: 40px;
  padding: 0 14px;
  border-radius: 10px;
  color: var(--cn-text-secondary);
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  line-height: 1;
  text-decoration: none;
  transition: var(--cn-transition);
  white-space: nowrap;
}

.cn-top-nav__item::after {
  position: absolute;
  right: 10px;
  bottom: 6px;
  left: 10px;
  height: 2px;
  border-radius: 999px;
  background: var(--cn-color-brand-primary);
  content: '';
  transform: scaleX(0);
  transform-origin: center;
  transition: transform var(--cn-motion-base) var(--cn-ease-out);
}

.cn-top-nav__item:hover {
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
}

.cn-top-nav__item:hover::after,
.cn-top-nav__item.is-active::after {
  transform: scaleX(1);
}

.cn-top-nav__item.is-active {
  background: color-mix(in srgb, var(--cn-color-brand-soft) 76%, var(--cn-color-bg-surface));
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--cn-color-brand-primary) 18%, var(--cn-color-border-subtle));
  color: var(--cn-color-brand-primary);
  font-weight: 600;
}

.cn-top-nav__item .el-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  line-height: 1;
}

.cn-top-nav__dropdown-arrow {
  margin-left: 2px;
  font-size: 12px;
  transition: transform var(--cn-motion-fast) var(--cn-ease-out);
}

.cn-top-nav__dropdown:hover .cn-top-nav__dropdown-arrow {
  transform: rotate(180deg);
}

.cn-top-nav__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  justify-self: end;
  gap: 12px;
  min-width: 0;
}

.cn-top-nav__theme {
  flex-shrink: 0;
}

.cn-top-nav__search {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 40px;
  height: 40px;
  padding: 0;
  border: 1px solid color-mix(in srgb, var(--cn-color-brand-primary) 18%, var(--cn-color-border-subtle));
  border-radius: 12px;
  background: color-mix(in srgb, var(--cn-color-bg-surface-muted) 92%, transparent);
  color: var(--cn-color-text-secondary);
  cursor: pointer;
  line-height: 1;
  transition: var(--cn-transition);
}

.cn-top-nav__search:hover {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 34%, var(--cn-color-border-subtle));
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
}

.cn-top-nav__search .el-icon {
  font-size: 18px;
}

.cn-top-nav__icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: 1px solid transparent;
  border-radius: 10px;
  cursor: pointer;
  transition: var(--cn-transition);
}

.cn-top-nav__icon-button:hover {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 18%, var(--cn-color-border-subtle));
  background: var(--cn-color-brand-soft);
}

.cn-top-nav__icon-button .el-icon {
  color: var(--cn-text-secondary);
  font-size: 18px;
}

.cn-top-nav__icon-button:hover .el-icon {
  color: var(--cn-color-brand-primary);
}

.cn-top-nav__user {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 40px;
  padding: 5px 10px 5px 6px;
  border: 1px solid transparent;
  border-radius: 12px;
  cursor: pointer;
  transition: var(--cn-transition);
}

.cn-top-nav__user:hover {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 18%, var(--cn-color-border-subtle));
  background: var(--cn-color-brand-soft);
}

.cn-top-nav__user-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
  text-align: left;
}

.cn-top-nav__user-copy strong {
  max-width: 104px;
  overflow: hidden;
  color: var(--cn-text-secondary);
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cn-top-nav__user-copy small {
  color: var(--cn-color-text-tertiary);
  font-size: 11px;
}

.cn-top-nav-popper .el-dropdown-menu {
  border: 1px solid var(--cn-border-soft);
  border-radius: 12px;
  box-shadow: 0 16px 34px color-mix(in srgb, var(--cn-color-brand-primary) 14%, transparent);
}

.cn-top-nav-popper .el-dropdown-menu__item {
  color: var(--cn-text-secondary);
}

.cn-top-nav-popper .el-dropdown-menu__item > .el-icon {
  margin-right: 8px;
  color: var(--cn-color-text-tertiary);
  font-size: 14px;
}

.cn-top-nav-popper .el-dropdown-menu__item:not(.is-disabled):hover {
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
}

.cn-top-nav-learning-popper .el-dropdown-menu,
.cn-top-nav-rich-popper .el-dropdown-menu {
  min-width: 320px;
  padding: 8px;
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 18%, var(--cn-color-border-subtle));
  border-radius: 14px;
  background: color-mix(in srgb, var(--cn-color-bg-surface) 94%, transparent);
  backdrop-filter: blur(10px);
}

.cn-top-nav-menu__group-label {
  min-height: auto;
  margin-top: 4px;
  padding: 6px 10px 4px;
  background: transparent !important;
  color: var(--cn-color-text-tertiary);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
  pointer-events: none;
  text-transform: uppercase;
}

.cn-top-nav-learning-popper .el-dropdown-menu__item,
.cn-top-nav-rich-popper .el-dropdown-menu__item {
  display: flex;
  align-items: center;
  min-height: auto;
  margin: 2px 0;
  padding: 4px;
  border-radius: 12px;
  line-height: 1;
  transition:
    background-color var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out),
    transform var(--cn-motion-fast) var(--cn-ease-out);
}

.cn-top-nav-menu__item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  box-sizing: border-box;
  padding: 10px 14px;
}

.cn-top-nav-menu__icon,
.cn-top-nav-mobile__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: var(--cn-color-brand-soft);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--cn-color-brand-primary) 18%, var(--cn-color-border-subtle));
  color: var(--cn-color-brand-primary);
}

.cn-top-nav-menu__icon .el-icon,
.cn-top-nav-mobile__icon .el-icon {
  width: 1em;
  height: 1em;
  margin-right: 0;
  color: inherit;
  font-size: 15px;
}

.cn-top-nav-popper .cn-top-nav-menu__icon .el-icon {
  margin-right: 0;
}

.cn-top-nav-menu__copy,
.cn-top-nav-mobile__copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  justify-content: center;
  min-width: 0;
}

.cn-top-nav-menu__copy strong,
.cn-top-nav-mobile__copy strong {
  color: var(--cn-text-primary);
  font-size: 13px;
  font-weight: 600;
}

.cn-top-nav-menu__copy small,
.cn-top-nav-mobile__copy small {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  line-height: 1.4;
}

.cn-top-nav-learning-popper .el-dropdown-menu__item:not(.is-disabled):hover,
.cn-top-nav-rich-popper .el-dropdown-menu__item:not(.is-disabled):hover,
.cn-top-nav-mobile__item:hover {
  background: color-mix(in srgb, var(--cn-color-brand-soft) 70%, var(--cn-color-bg-surface));
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--cn-color-brand-primary) 18%, var(--cn-color-border-subtle));
  transform: translateY(-1px);
}

.cn-top-nav-learning-popper .el-dropdown-menu__item.is-route-active,
.cn-top-nav-rich-popper .el-dropdown-menu__item.is-route-active,
.cn-top-nav-mobile__item.is-active {
  background: color-mix(in srgb, var(--cn-color-brand-soft) 82%, var(--cn-color-bg-surface));
  box-shadow:
    inset 0 0 0 1px color-mix(in srgb, var(--cn-color-brand-primary) 34%, var(--cn-color-border-subtle)),
    0 6px 14px color-mix(in srgb, var(--cn-color-brand-primary) 14%, transparent);
}

.cn-top-nav-learning-popper .el-dropdown-menu__item.is-route-active .cn-top-nav-menu__icon,
.cn-top-nav-rich-popper .el-dropdown-menu__item.is-route-active .cn-top-nav-menu__icon,
.cn-top-nav-mobile__item.is-active .cn-top-nav-mobile__icon {
  background: var(--cn-color-brand-primary);
  box-shadow: none;
  color: var(--cn-button-primary-color);
}

.cn-top-nav-mobile .el-drawer__body {
  padding: 0;
}

.cn-top-nav-mobile__shell {
  display: grid;
  align-content: start;
  gap: 16px;
  height: 100%;
  padding: 22px 18px 24px;
  background: var(--cn-color-bg-page);
}

.cn-top-nav-mobile__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.cn-top-nav-mobile__top > div {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.cn-top-nav-mobile__top p {
  margin: 0;
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.cn-top-nav-mobile__close {
  padding: 6px 10px;
  border: 1px solid color-mix(in srgb, var(--cn-color-brand-primary) 18%, var(--cn-color-border-subtle));
  border-radius: 10px;
  background: color-mix(in srgb, var(--cn-color-bg-surface-muted) 92%, transparent);
  color: var(--cn-color-text-secondary);
  cursor: pointer;
  transition: var(--cn-transition);
}

.cn-top-nav-mobile__close:hover {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 34%, var(--cn-color-border-subtle));
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
}

.cn-top-nav-mobile__search {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 13px 14px;
  border: 1px solid color-mix(in srgb, var(--cn-color-brand-primary) 18%, var(--cn-color-border-subtle));
  border-radius: 14px;
  background: var(--cn-color-bg-surface);
  color: var(--cn-color-brand-primary);
  cursor: pointer;
  transition: var(--cn-transition);
}

.cn-top-nav-mobile__search:hover {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 34%, var(--cn-color-border-subtle));
  background: var(--cn-color-brand-soft);
}

.cn-top-nav-mobile__theme {
  justify-content: center;
}

.cn-top-nav-mobile__section {
  display: grid;
  gap: 8px;
}

.cn-top-nav-mobile__title {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.cn-top-nav-mobile__item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px;
  border-radius: 14px;
  cursor: pointer;
  text-align: left;
  transition: var(--cn-transition);
}

.cn-top-nav-mobile__item span:not(.cn-top-nav-mobile__icon) {
  min-width: 0;
}

.cn-top-nav .is-desktop {
  display: inline-flex;
}

.cn-top-nav .is-mobile {
  display: none;
}

@media (max-width: 1180px) {
  .cn-top-nav__workspace {
    display: none;
  }
}

@media (max-width: 992px) {
  .cn-top-nav__content {
    height: 66px;
    padding: 0 14px;
  }

  .cn-top-nav .is-desktop {
    display: none;
  }

  .cn-top-nav .is-mobile {
    display: inline-flex;
  }

  .cn-top-nav__user-copy small {
    display: none;
  }
}

@media (max-width: 768px) {
  .cn-top-nav__brand strong {
    font-size: 20px;
  }

  .cn-top-nav__brand span,
  .cn-top-nav__user-copy strong {
    display: none;
  }
}

@media (max-width: 480px) {
  .cn-top-nav__actions {
    gap: 8px;
  }

  .cn-top-nav__icon-button {
    width: 34px;
    height: 34px;
  }
}
</style>
