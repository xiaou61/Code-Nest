<template>
  <aside class="cn-sidebar" :class="{ 'is-collapsed': collapsed }" :aria-label="ariaLabel">
    <div class="cn-sidebar__brand">
      <span>{{ collapsed ? collapsedBrand : brand }}</span>
    </div>

    <div v-if="searchable && !collapsed" class="cn-sidebar__search">
      <el-input
        ref="searchInputRef"
        :model-value="searchKeyword"
        :placeholder="searchPlaceholder"
        size="small"
        class="cn-sidebar__search-input"
        clearable
        @update:model-value="emit('update:searchKeyword', String($event))"
        @input="emit('search-input')"
        @keyup.enter="emit('search-enter')"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <div v-else-if="searchable" class="cn-sidebar__collapsed-search">
      <el-tooltip :content="searchTooltip" placement="right">
        <button
          class="cn-sidebar__search-button"
          type="button"
          :aria-label="searchTooltip"
          @click="emit('search-focus-request')"
        >
          <el-icon size="18"><Search /></el-icon>
        </button>
      </el-tooltip>
    </div>

    <div v-if="searchable && !collapsed && searchKeyword" class="cn-sidebar__results">
      <div v-if="searchResults.length" class="cn-sidebar__result-panel">
        <div class="cn-sidebar__result-header">找到 {{ searchResults.length }} 个功能</div>
        <button
          v-for="item in searchResults"
          :key="item.path"
          class="cn-sidebar__result-item"
          type="button"
          @click="emit('result-select', item)"
        >
          <el-icon v-if="item.icon" class="cn-sidebar__result-icon">
            <component :is="item.icon" />
          </el-icon>
          <span class="cn-sidebar__result-content">
            <strong>{{ item.title }}</strong>
            <small>{{ item.breadcrumb }}</small>
          </span>
        </button>
      </div>

      <div v-else class="cn-sidebar__empty">
        <span class="cn-sidebar__empty-icon">SR</span>
        <span>{{ emptyText }}</span>
      </div>
    </div>

    <el-menu
      v-else
      :default-active="activePath"
      :collapse="collapsed"
      router
      class="cn-sidebar__menu"
    >
      <CnSidebarItem
        v-for="item in items"
        :key="item.index || item.path || item.label"
        :item="item"
      />
    </el-menu>
  </aside>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import CnSidebarItem from './CnSidebarItem.vue'
import type { CnSidebarProps, CnSidebarSearchResult } from '../../types/components'

withDefaults(defineProps<CnSidebarProps>(), {
  activePath: '',
  collapsed: false,
  brand: 'Code-Nest',
  collapsedBrand: 'CN',
  searchable: true,
  searchKeyword: '',
  searchResults: () => [],
  searchPlaceholder: '搜索功能... (Ctrl+K)',
  searchTooltip: '搜索功能 (Ctrl+K)',
  ariaLabel: '主导航',
  emptyText: '未找到匹配的功能'
})

const emit = defineEmits<{
  'update:searchKeyword': [value: string]
  'search-input': []
  'search-enter': []
  'search-focus-request': []
  'result-select': [item: CnSidebarSearchResult]
}>()

const searchInputRef = ref()

function focusSearch() {
  searchInputRef.value?.focus?.()
}

defineExpose({
  focusSearch
})
</script>

<style scoped>
.cn-sidebar {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--cn-color-bg-surface);
  border-right: 1px solid var(--cn-border-soft);
  transition: width var(--cn-motion-base) var(--cn-ease-out);
}

.cn-sidebar__brand {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60px;
  flex-shrink: 0;
  border-bottom: 1px solid var(--cn-border-soft);
  background: color-mix(in srgb, var(--cn-color-brand-primary) 4%, var(--cn-color-bg-surface));
  color: var(--cn-color-brand-primary);
  font-family: var(--cn-font-heading);
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 0;
}

.cn-sidebar__search {
  flex-shrink: 0;
  padding: 12px 14px;
  border-bottom: 1px solid var(--cn-border-soft);
  background: color-mix(in srgb, var(--cn-color-bg-surface) 90%, transparent);
}

.cn-sidebar__search-input {
  width: 100%;
}

.cn-sidebar__search-input :deep(.el-input__wrapper) {
  border: 1px solid color-mix(in srgb, var(--cn-color-brand-primary) 18%, var(--cn-color-border-subtle));
  border-radius: 10px;
  background: color-mix(in srgb, var(--cn-color-bg-surface-muted) 92%, transparent);
  box-shadow: none;
  transition: var(--cn-transition);
}

.cn-sidebar__search-input :deep(.el-input__wrapper:hover) {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 34%, var(--cn-color-border-subtle));
  background: var(--cn-color-bg-surface);
}

.cn-sidebar__search-input :deep(.el-input__wrapper.is-focus) {
  border-color: var(--cn-color-brand-primary);
  background: var(--cn-color-bg-surface);
  box-shadow: 0 0 0 2px var(--cn-color-focus-ring);
}

.cn-sidebar__search-input :deep(.el-input__inner) {
  color: var(--cn-text-primary);
  font-size: 13px;
}

.cn-sidebar__search-input :deep(.el-input__inner::placeholder) {
  color: var(--cn-text-tertiary);
}

.cn-sidebar__search-input :deep(.el-input__prefix) {
  color: color-mix(in srgb, var(--cn-color-brand-primary) 72%, var(--cn-color-text-secondary));
}

.cn-sidebar__collapsed-search {
  display: flex;
  justify-content: center;
  flex-shrink: 0;
  padding: 8px;
  border-bottom: 1px solid var(--cn-border-soft);
}

.cn-sidebar__search-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--cn-text-secondary);
  cursor: pointer;
  transition: var(--cn-transition);
}

.cn-sidebar__search-button:hover {
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
}

.cn-sidebar__search-button:focus-visible,
.cn-sidebar__result-item:focus-visible {
  outline: 2px solid var(--cn-color-brand-primary);
  outline-offset: 2px;
}

.cn-sidebar__results {
  flex: 1;
  overflow-y: auto;
  padding: 10px 14px 16px;
}

.cn-sidebar__result-panel {
  overflow: hidden;
  border: 1px solid var(--cn-border-soft);
  border-radius: var(--cn-radius-md);
  background: var(--cn-color-bg-surface);
  box-shadow: var(--cn-shadow-xs);
}

.cn-sidebar__result-header {
  padding: 10px 12px;
  border-bottom: 1px solid var(--cn-border-soft);
  background: color-mix(in srgb, var(--cn-color-bg-surface-muted) 92%, transparent);
  color: var(--cn-text-secondary);
  font-size: 12px;
}

.cn-sidebar__result-item {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 50px;
  padding: 10px 12px;
  border: 0;
  border-bottom: 1px solid var(--cn-color-border-subtle);
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: var(--cn-transition);
}

.cn-sidebar__result-item:hover {
  background: var(--cn-color-brand-soft);
}

.cn-sidebar__result-item:last-child {
  border-bottom: none;
}

.cn-sidebar__result-icon {
  flex-shrink: 0;
  margin-right: 10px;
  color: color-mix(in srgb, var(--cn-color-brand-primary) 72%, var(--cn-color-text-secondary));
  font-size: 16px;
}

.cn-sidebar__result-content {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.cn-sidebar__result-content strong,
.cn-sidebar__result-content small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cn-sidebar__result-content strong {
  color: var(--cn-text-primary);
  font-size: 13px;
  font-weight: 600;
}

.cn-sidebar__result-content small {
  color: var(--cn-text-tertiary);
  font-size: 11px;
}

.cn-sidebar__empty {
  display: grid;
  justify-items: center;
  gap: var(--cn-space-2);
  padding: 40px 20px;
  border: 1px dashed var(--cn-border);
  border-radius: var(--cn-radius-md);
  background: color-mix(in srgb, var(--cn-color-bg-surface) 90%, transparent);
  color: var(--cn-text-tertiary);
  text-align: center;
  font-size: 12px;
}

.cn-sidebar__empty-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-brand-primary);
  font-size: 11px;
  font-weight: 800;
}

.cn-sidebar__menu {
  flex: 1;
  overflow-y: auto;
  padding: 8px 10px 14px;
  border: none;
  background: transparent;
}

.cn-sidebar__menu :deep(.el-menu-item),
.cn-sidebar__menu :deep(.el-sub-menu__title) {
  height: 42px;
  margin: 4px 0;
  border-radius: 10px;
  color: var(--cn-text-secondary);
  font-weight: 500;
  line-height: 42px;
}

.cn-sidebar__menu :deep(.el-menu-item .el-icon),
.cn-sidebar__menu :deep(.el-sub-menu__title .el-icon) {
  color: color-mix(in srgb, var(--cn-color-brand-primary) 72%, var(--cn-color-text-secondary));
}

.cn-sidebar__menu :deep(.el-menu-item:hover),
.cn-sidebar__menu :deep(.el-sub-menu__title:hover) {
  background: var(--cn-color-brand-soft) !important;
  color: var(--cn-color-brand-primary) !important;
}

.cn-sidebar__menu :deep(.el-menu-item:hover .el-icon),
.cn-sidebar__menu :deep(.el-sub-menu__title:hover .el-icon) {
  color: var(--cn-color-brand-primary);
}

.cn-sidebar__menu :deep(.el-menu-item.is-active) {
  background: color-mix(in srgb, var(--cn-color-brand-soft) 78%, var(--cn-color-bg-surface)) !important;
  color: var(--cn-color-brand-primary) !important;
  box-shadow: inset 3px 0 0 var(--cn-color-brand-primary);
}

.cn-sidebar__menu :deep(.el-menu-item.is-active .el-icon) {
  color: var(--cn-color-brand-primary);
}

.cn-sidebar__menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: var(--cn-color-brand-primary) !important;
}

.cn-sidebar__menu :deep(.el-menu--inline .el-menu-item) {
  padding-left: 46px !important;
}
</style>
