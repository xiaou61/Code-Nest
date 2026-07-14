<template>
  <el-drawer
    v-model="drawerVisible"
    class="cn-theme-drawer"
    :size="size"
    :show-close="false"
    append-to-body
  >
    <div class="cn-theme-drawer__shell">
      <header class="cn-theme-drawer__header">
        <div class="cn-theme-drawer__title-group">
          <p class="cn-theme-drawer__eyebrow">Code Nest Theme</p>
          <h2 class="cn-theme-drawer__title">{{ title }}</h2>
          <p v-if="description" class="cn-theme-drawer__description">{{ description }}</p>
        </div>

        <button
          class="cn-theme-drawer__close"
          type="button"
          aria-label="关闭主题设置"
          @click="closeDrawer"
        >
          <el-icon aria-hidden="true"><Close /></el-icon>
        </button>
      </header>

      <main class="cn-theme-drawer__body">
        <section class="cn-theme-drawer__section">
          <div class="cn-theme-drawer__section-head">
            <div>
              <h3 class="cn-theme-drawer__section-title">主题模式</h3>
              <p class="cn-theme-drawer__section-desc">选择会立即保存到本机偏好。</p>
            </div>
            <CnStatusTag type="brand" size="sm" :dot="false">{{ resolvedThemeLabel }}</CnStatusTag>
          </div>

          <div class="cn-theme-drawer__option-grid">
            <button
              v-for="option in visibleOptions"
              :key="option.value"
              class="cn-theme-drawer__option"
              :class="[
                `cn-theme-drawer__option--${option.value}`,
                { 'is-active': themeMode === option.value }
              ]"
              type="button"
              :aria-pressed="themeMode === option.value"
              @click="selectTheme(option.value)"
            >
              <span class="cn-theme-drawer__swatch" aria-hidden="true">
                <span class="cn-theme-drawer__swatch-page" />
                <span class="cn-theme-drawer__swatch-surface" />
                <span class="cn-theme-drawer__swatch-brand" />
              </span>

              <span class="cn-theme-drawer__option-copy">
                <strong>{{ option.label }}</strong>
                <span>{{ option.description }}</span>
              </span>

              <el-icon v-if="themeMode === option.value" class="cn-theme-drawer__option-check" aria-hidden="true">
                <Check />
              </el-icon>
            </button>
          </div>
        </section>

        <section class="cn-theme-drawer__section">
          <div class="cn-theme-drawer__section-head">
            <div>
              <h3 class="cn-theme-drawer__section-title">当前状态</h3>
              <p class="cn-theme-drawer__section-desc">主题状态由 design-system 统一写入文档根节点。</p>
            </div>
          </div>

          <dl class="cn-theme-drawer__meta-list">
            <div class="cn-theme-drawer__meta-item">
              <dt>偏好模式</dt>
              <dd>{{ activeModeLabel }}</dd>
            </div>
            <div class="cn-theme-drawer__meta-item">
              <dt>解析主题</dt>
              <dd>{{ resolvedThemeLabel }}</dd>
            </div>
            <div class="cn-theme-drawer__meta-item">
              <dt>色彩方案</dt>
              <dd>{{ isDark ? '暗色' : '亮色' }}</dd>
            </div>
          </dl>
        </section>
      </main>

      <footer class="cn-theme-drawer__footer">
        <el-button @click="selectTheme('system')">跟随系统</el-button>
        <el-button type="primary" @click="closeDrawer">完成</el-button>
      </footer>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Check, Close } from '@element-plus/icons-vue'
import CnStatusTag from '../data/CnStatusTag.vue'
import { useTheme } from '../../composables/useTheme'
import type { CnThemeDrawerProps } from '../../types/components'
import type { CodeNestThemeMode, CodeNestThemeName } from '../../types/theme'

const defaultDrawerModes: CodeNestThemeMode[] = ['system', 'light', 'dark', 'professional-blue']
const advancedDrawerModes: CodeNestThemeMode[] = [...defaultDrawerModes, 'growth', 'high-contrast']

const props = withDefaults(defineProps<CnThemeDrawerProps>(), {
  title: '主题设置',
  description: '切换 Code Nest 的亮色、暗色与品牌主题。',
  size: '380px',
  showAdvancedThemes: false
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'theme-change': [mode: CodeNestThemeMode]
}>()

const { themeMode, resolvedTheme, isDark, options, setTheme } = useTheme()

const themeNameLabels: Record<CodeNestThemeName, string> = {
  light: '亮色',
  dark: '暗色',
  'professional-blue': '专业蓝',
  growth: '成长绿',
  'high-contrast': '高对比'
}

const drawerVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const visibleOptions = computed(() => {
  const visibleModes = props.showAdvancedThemes ? advancedDrawerModes : defaultDrawerModes
  return options.filter((option) => visibleModes.includes(option.value))
})

const activeModeLabel = computed(() => {
  return options.find((option) => option.value === themeMode.value)?.label || themeMode.value
})

const resolvedThemeLabel = computed(() => themeNameLabels[resolvedTheme.value])

function closeDrawer() {
  drawerVisible.value = false
}

function selectTheme(mode: CodeNestThemeMode) {
  setTheme(mode)
  emit('theme-change', mode)
}
</script>

<style scoped>
.cn-theme-drawer :deep(.el-drawer__body) {
  padding: 0;
  background: var(--cn-color-bg-page);
}

.cn-theme-drawer__shell {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  background: var(--cn-color-bg-page);
  color: var(--cn-color-text-primary);
}

.cn-theme-drawer__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-4);
  padding: var(--cn-space-6);
  border-bottom: 1px solid var(--cn-color-border-subtle);
  background: var(--cn-color-bg-surface);
}

.cn-theme-drawer__title-group {
  display: grid;
  gap: var(--cn-space-2);
  min-width: 0;
}

.cn-theme-drawer__eyebrow {
  margin: 0;
  color: var(--cn-color-brand-primary);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

.cn-theme-drawer__title {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 22px;
  font-weight: 750;
  line-height: 1.25;
}

.cn-theme-drawer__description {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.cn-theme-drawer__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  cursor: pointer;
  transition:
    color var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    background-color var(--cn-motion-fast) var(--cn-ease-out);
}

.cn-theme-drawer__close:hover {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 30%, var(--cn-color-border-subtle));
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
}

.cn-theme-drawer__close:focus-visible,
.cn-theme-drawer__option:focus-visible {
  outline: 2px solid var(--cn-color-brand-primary);
  outline-offset: 2px;
}

.cn-theme-drawer__body {
  display: grid;
  gap: var(--cn-space-4);
  flex: 1;
  padding: var(--cn-space-5);
}

.cn-theme-drawer__section {
  display: grid;
  gap: var(--cn-space-4);
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-panel);
  background: var(--cn-color-bg-surface);
  box-shadow: var(--cn-shadow-card);
}

.cn-theme-drawer__section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-3);
}

.cn-theme-drawer__section-title {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.4;
}

.cn-theme-drawer__section-desc {
  margin: var(--cn-space-1) 0 0;
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.cn-theme-drawer__option-grid {
  display: grid;
  gap: var(--cn-space-3);
}

.cn-theme-drawer__option {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--cn-space-3);
  width: 100%;
  min-height: 76px;
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    background-color var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out);
}

.cn-theme-drawer__option:hover {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 30%, var(--cn-color-border-subtle));
}

.cn-theme-drawer__option.is-active {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 52%, var(--cn-color-border-subtle));
  background: color-mix(in srgb, var(--cn-color-brand-soft) 78%, var(--cn-color-bg-surface));
  box-shadow: 0 10px 24px color-mix(in srgb, var(--cn-color-brand-primary) 12%, transparent);
}

.cn-theme-drawer__swatch {
  display: grid;
  grid-template-columns: repeat(3, 14px);
  align-items: end;
  gap: 4px;
  width: 58px;
  height: 42px;
  padding: 6px;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface);
}

.cn-theme-drawer__swatch-page,
.cn-theme-drawer__swatch-surface,
.cn-theme-drawer__swatch-brand {
  display: block;
  border-radius: 5px;
}

.cn-theme-drawer__swatch-page {
  height: 28px;
}

.cn-theme-drawer__swatch-surface {
  height: 22px;
}

.cn-theme-drawer__swatch-brand {
  height: 34px;
}

.cn-theme-drawer__option--system .cn-theme-drawer__swatch-page,
.cn-theme-drawer__option--light .cn-theme-drawer__swatch-page {
  background: var(--cn-slate-100);
}

.cn-theme-drawer__option--system .cn-theme-drawer__swatch-surface,
.cn-theme-drawer__option--light .cn-theme-drawer__swatch-surface {
  background: white;
}

.cn-theme-drawer__option--system .cn-theme-drawer__swatch-brand,
.cn-theme-drawer__option--light .cn-theme-drawer__swatch-brand {
  background: var(--cn-blue-600);
}

.cn-theme-drawer__option--dark .cn-theme-drawer__swatch-page {
  background: var(--cn-slate-900);
}

.cn-theme-drawer__option--dark .cn-theme-drawer__swatch-surface {
  background: var(--cn-slate-800);
}

.cn-theme-drawer__option--dark .cn-theme-drawer__swatch-brand {
  background: var(--cn-blue-400);
}

.cn-theme-drawer__option--professional-blue .cn-theme-drawer__swatch-page {
  background: var(--cn-blue-100);
}

.cn-theme-drawer__option--professional-blue .cn-theme-drawer__swatch-surface {
  background: white;
}

.cn-theme-drawer__option--professional-blue .cn-theme-drawer__swatch-brand {
  background: var(--cn-blue-700);
}

.cn-theme-drawer__option--growth .cn-theme-drawer__swatch-page {
  background: color-mix(in srgb, var(--cn-green-500) 16%, white);
}

.cn-theme-drawer__option--growth .cn-theme-drawer__swatch-surface {
  background: white;
}

.cn-theme-drawer__option--growth .cn-theme-drawer__swatch-brand {
  background: var(--cn-green-600);
}

.cn-theme-drawer__option--high-contrast .cn-theme-drawer__swatch-page {
  background: white;
}

.cn-theme-drawer__option--high-contrast .cn-theme-drawer__swatch-surface {
  background: var(--cn-slate-100);
}

.cn-theme-drawer__option--high-contrast .cn-theme-drawer__swatch-brand {
  background: black;
}

.cn-theme-drawer__option-copy {
  display: grid;
  gap: var(--cn-space-1);
  min-width: 0;
}

.cn-theme-drawer__option-copy strong {
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.35;
}

.cn-theme-drawer__option-copy span {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.cn-theme-drawer__option-check {
  color: var(--cn-color-brand-primary);
  font-size: 18px;
}

.cn-theme-drawer__meta-list {
  display: grid;
  gap: var(--cn-space-2);
  margin: 0;
}

.cn-theme-drawer__meta-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  min-height: 38px;
  padding: 0 var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.cn-theme-drawer__meta-item dt {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.cn-theme-drawer__meta-item dd {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 13px;
  font-weight: 700;
}

.cn-theme-drawer__footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--cn-space-2);
  padding: var(--cn-space-4) var(--cn-space-5);
  border-top: 1px solid var(--cn-color-border-subtle);
  background: var(--cn-color-bg-surface);
}

@media (max-width: 768px) {
  .cn-theme-drawer__header,
  .cn-theme-drawer__body,
  .cn-theme-drawer__footer {
    padding-right: var(--cn-space-4);
    padding-left: var(--cn-space-4);
  }

  .cn-theme-drawer__option {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .cn-theme-drawer__option-check {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
