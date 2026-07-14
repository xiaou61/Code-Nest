<template>
  <el-dialog
    v-model="dialogVisible"
    class="cn-command-palette"
    :width="width"
    align-center
    :show-close="false"
    destroy-on-close
    @opened="focusSearch"
  >
    <template #header>
      <div class="cn-command-palette__header">
        <div class="cn-command-palette__title">
          <strong>{{ title }}</strong>
          <span>{{ description }}</span>
        </div>
        <button class="cn-command-palette__close" type="button" aria-label="关闭命令面板" @click="close">
          Esc
        </button>
      </div>
    </template>

    <div class="cn-command-palette__body">
      <el-input
        ref="searchInputRef"
        :model-value="keyword"
        clearable
        size="large"
        :placeholder="placeholder"
        @update:model-value="emit('update:keyword', String($event))"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <div class="cn-command-palette__scroll">
        <section v-if="recentItems.length" class="cn-command-palette__section">
          <div class="cn-command-palette__section-title">最近访问</div>
          <button
            v-for="item in recentItems"
            :key="`recent-${item.path}`"
            type="button"
            class="cn-command-palette__item"
            @click="selectItem(item)"
          >
            <span class="cn-command-palette__item-icon is-recent">
              <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
            </span>
            <span class="cn-command-palette__item-copy">
              <strong>{{ item.label }}</strong>
              <small>{{ item.desc }}</small>
            </span>
            <span class="cn-command-palette__item-path">{{ item.path }}</span>
          </button>
        </section>

        <section
          v-for="section in filteredSections"
          :key="section.key"
          class="cn-command-palette__section"
        >
          <div class="cn-command-palette__section-title">{{ section.title }}</div>
          <button
            v-for="item in section.items"
            :key="item.path"
            type="button"
            class="cn-command-palette__item"
            :class="{ 'is-active': isActive(item) }"
            @click="selectItem(item)"
          >
            <span class="cn-command-palette__item-icon">
              <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
            </span>
            <span class="cn-command-palette__item-copy">
              <strong>{{ item.label }}</strong>
              <small>{{ item.desc }}</small>
            </span>
            <span class="cn-command-palette__item-path">
              <el-icon><Right /></el-icon>
            </span>
          </button>
        </section>

        <CnEmptyState
          v-if="!filteredSections.length && !recentItems.length"
          :title="emptyTitle"
          :description="emptyDescription"
          size="sm"
          surface="plain"
        />
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { Right, Search } from '@element-plus/icons-vue'
import CnEmptyState from '../data/CnEmptyState.vue'
import type { CnCommandItem, CnCommandPaletteProps } from '../../types/components'

const props = withDefaults(defineProps<CnCommandPaletteProps>(), {
  keyword: '',
  recentItems: () => [],
  activePath: '',
  title: '全站命令面板',
  description: '更快地打开模块、工具和工作台',
  placeholder: '搜索页面、功能或场景',
  emptyTitle: '没有找到匹配功能',
  emptyDescription: '试试搜索模块名称、功能路径或使用场景',
  width: '680px'
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'update:keyword': [value: string]
  close: []
  select: [item: CnCommandItem]
}>()

const searchInputRef = ref()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => {
    emit('update:modelValue', value)
    if (!value) {
      emit('close')
    }
  }
})

const filteredSections = computed(() => {
  const normalizedKeyword = props.keyword.trim().toLowerCase()
  if (!normalizedKeyword) {
    return props.sections
  }

  return props.sections
    .map((section) => ({
      ...section,
      items: section.items.filter((item) =>
        [item.label, item.desc, item.path].some((field) =>
          field?.toLowerCase().includes(normalizedKeyword)
        )
      )
    }))
    .filter((section) => section.items.length > 0)
})

function isActive(item: CnCommandItem) {
  const prefixes = item.matchPrefixes?.length ? item.matchPrefixes : [item.path]
  return prefixes.some((prefix) => {
    if (prefix === '/') {
      return props.activePath === '/'
    }

    return props.activePath === prefix || props.activePath.startsWith(`${prefix}/`)
  })
}

function close() {
  dialogVisible.value = false
}

function selectItem(item: CnCommandItem) {
  emit('select', item)
}

function focusSearch() {
  nextTick(() => {
    searchInputRef.value?.focus?.()
  })
}

defineExpose({
  focusSearch
})
</script>

<style scoped>
.cn-command-palette :deep(.el-dialog) {
  padding: 0;
}

.cn-command-palette :deep(.el-dialog__header) {
  padding: var(--cn-space-5) var(--cn-space-5) var(--cn-space-3);
}

.cn-command-palette :deep(.el-dialog__body) {
  padding: 0 var(--cn-space-5) var(--cn-space-5);
}

.cn-command-palette__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-3);
}

.cn-command-palette__title {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-2);
}

.cn-command-palette__title strong {
  color: var(--cn-color-text-primary);
  font-size: 16px;
}

.cn-command-palette__title span {
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.cn-command-palette__close,
.cn-command-palette__item {
  appearance: none;
  border: none;
  background: none;
  font: inherit;
}

.cn-command-palette__close {
  padding: 6px 10px;
  border: 1px solid color-mix(in srgb, var(--cn-color-brand-primary) 18%, var(--cn-color-border-subtle));
  border-radius: 10px;
  background: color-mix(in srgb, var(--cn-color-bg-surface-muted) 92%, transparent);
  color: var(--cn-color-text-secondary);
  cursor: pointer;
  transition: var(--cn-transition);
}

.cn-command-palette__close:hover {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 34%, var(--cn-color-border-subtle));
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
}

.cn-command-palette__close:focus-visible,
.cn-command-palette__item:focus-visible {
  outline: 2px solid var(--cn-color-brand-primary);
  outline-offset: 2px;
}

.cn-command-palette__body {
  display: grid;
  gap: var(--cn-space-4);
}

.cn-command-palette__scroll {
  max-height: 62vh;
  overflow-y: auto;
  padding-right: var(--cn-space-1);
}

.cn-command-palette__section {
  display: grid;
  gap: var(--cn-space-2);
}

.cn-command-palette__section + .cn-command-palette__section {
  margin-top: var(--cn-space-4);
}

.cn-command-palette__section-title {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

.cn-command-palette__item {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  width: 100%;
  padding: var(--cn-space-3);
  border-radius: var(--cn-radius-card);
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: var(--cn-transition);
}

.cn-command-palette__item:hover {
  transform: translateY(-1px);
  background: color-mix(in srgb, var(--cn-color-brand-soft) 70%, var(--cn-color-bg-surface));
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--cn-color-brand-primary) 18%, var(--cn-color-border-subtle));
}

.cn-command-palette__item.is-active {
  background: color-mix(in srgb, var(--cn-color-brand-soft) 82%, var(--cn-color-bg-surface));
  box-shadow:
    inset 0 0 0 1px color-mix(in srgb, var(--cn-color-brand-primary) 34%, var(--cn-color-border-subtle)),
    0 6px 14px color-mix(in srgb, var(--cn-color-brand-primary) 14%, transparent);
}

.cn-command-palette__item-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--cn-color-brand-primary) 18%, var(--cn-color-border-subtle));
}

.cn-command-palette__item-icon.is-recent {
  background: var(--cn-color-success-soft);
  color: var(--cn-color-success);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--cn-color-success) 18%, var(--cn-color-border-subtle));
}

.cn-command-palette__item-icon .el-icon {
  margin-right: 0;
  color: inherit;
  font-size: 15px;
}

.cn-command-palette__item.is-active .cn-command-palette__item-icon {
  background: var(--cn-color-brand-primary);
  color: var(--cn-button-primary-color);
  box-shadow: none;
}

.cn-command-palette__item-copy {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-1);
  min-width: 0;
}

.cn-command-palette__item-copy strong,
.cn-command-palette__item-copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cn-command-palette__item-copy strong {
  color: var(--cn-color-text-primary);
  font-size: 13px;
  font-weight: 650;
}

.cn-command-palette__item-copy small {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  line-height: 1.4;
}

.cn-command-palette__item-path {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-left: auto;
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

@media (max-width: 768px) {
  .cn-command-palette__item-path {
    display: none;
  }
}
</style>
