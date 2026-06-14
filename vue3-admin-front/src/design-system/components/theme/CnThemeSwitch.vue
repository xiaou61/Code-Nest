<template>
  <el-dropdown
    trigger="click"
    placement="bottom-end"
    popper-class="cn-theme-switch-popper"
    @command="handleThemeCommand"
  >
    <button
      class="cn-theme-switch"
      type="button"
      :aria-label="`当前主题：${currentOption?.label || '跟随系统'}`"
      :title="currentOption?.description || currentOption?.label || '主题设置'"
    >
      <span class="cn-theme-switch__icon" aria-hidden="true">
        <el-icon><component :is="currentOption?.icon || Monitor" /></el-icon>
      </span>
      <span v-if="showLabel" class="cn-theme-switch__label">
        {{ currentOption?.label || '主题' }}
      </span>
      <el-icon v-if="showLabel" class="cn-theme-switch__arrow" aria-hidden="true">
        <ArrowDown />
      </el-icon>
    </button>

    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="option in visibleOptions"
          :key="option.value"
          :command="option.value"
          :class="{ 'is-active': themeMode === option.value }"
        >
          <span class="cn-theme-switch-menu__item">
            <span class="cn-theme-switch-menu__icon">
              <el-icon><component :is="option.icon" /></el-icon>
            </span>
            <span class="cn-theme-switch-menu__copy">
              <strong>{{ option.label }}</strong>
              <small>{{ option.description }}</small>
            </span>
          </span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowDown, Brush, MagicStick, Monitor, Moon, SemiSelect, Sunny } from '@element-plus/icons-vue'
import { useTheme } from '../../composables/useTheme'
import type { CodeNestThemeMode } from '../../types/theme'
import type { Component } from 'vue'

interface ThemeSwitchOption {
  value: CodeNestThemeMode
  label: string
  description?: string
  icon: Component
}

interface CnThemeSwitchProps {
  showLabel?: boolean
  modes?: CodeNestThemeMode[]
}

const props = withDefaults(defineProps<CnThemeSwitchProps>(), {
  showLabel: false,
  modes: () => ['system', 'light', 'dark', 'professional-blue']
})

const { themeMode, options, setTheme } = useTheme()

const iconMap: Record<CodeNestThemeMode, Component> = {
  system: Monitor,
  light: Sunny,
  dark: Moon,
  'professional-blue': Brush,
  growth: MagicStick,
  'high-contrast': SemiSelect
}

const visibleOptions = computed<ThemeSwitchOption[]>(() =>
  options
    .filter((option) => props.modes.includes(option.value))
    .map((option) => ({
      ...option,
      icon: iconMap[option.value]
    }))
)

const currentOption = computed(() =>
  visibleOptions.value.find((option) => option.value === themeMode.value) || visibleOptions.value[0]
)

function handleThemeCommand(command: string | number | object) {
  setTheme(String(command) as CodeNestThemeMode)
}
</script>

<style scoped>
.cn-theme-switch {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-width: 38px;
  height: 38px;
  padding: 0 10px;
  border: 1px solid color-mix(in srgb, var(--cn-color-brand-primary) 18%, var(--cn-color-border-subtle));
  border-radius: 12px;
  background: color-mix(in srgb, var(--cn-color-bg-surface-muted) 92%, transparent);
  color: var(--cn-color-text-secondary);
  font: inherit;
  line-height: 1;
  cursor: pointer;
  transition:
    color var(--cn-motion-fast) var(--cn-ease-out),
    background-color var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out);
}

.cn-theme-switch:hover {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 34%, var(--cn-color-border-subtle));
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
}

.cn-theme-switch:focus-visible {
  outline: 2px solid var(--cn-color-brand-primary);
  outline-offset: 2px;
}

.cn-theme-switch__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.cn-theme-switch__label {
  white-space: nowrap;
  font-size: 13px;
  font-weight: 600;
}

.cn-theme-switch__arrow {
  font-size: 12px;
  transition: transform var(--cn-motion-fast) var(--cn-ease-out);
}

.cn-theme-switch[aria-expanded='true'] .cn-theme-switch__arrow {
  transform: rotate(180deg);
}

:global(.cn-theme-switch-popper .el-dropdown-menu) {
  min-width: 220px;
  padding: 8px;
  border: 1px solid color-mix(in srgb, var(--cn-color-brand-primary) 18%, var(--cn-color-border-subtle));
  border-radius: 14px;
  background: color-mix(in srgb, var(--cn-color-bg-surface) 96%, transparent);
  box-shadow: 0 16px 34px color-mix(in srgb, var(--cn-color-brand-primary) 14%, transparent);
  backdrop-filter: blur(10px);
}

:global(.cn-theme-switch-popper .el-dropdown-menu__item) {
  min-height: auto;
  margin: 2px 0;
  padding: 0;
  border-radius: 12px;
  line-height: 1;
}

:global(.cn-theme-switch-popper .el-dropdown-menu__item:not(.is-disabled):hover),
:global(.cn-theme-switch-popper .el-dropdown-menu__item.is-active) {
  background: color-mix(in srgb, var(--cn-color-brand-soft) 78%, var(--cn-color-bg-surface));
  color: var(--cn-color-brand-primary);
}

:global(.cn-theme-switch-menu__item) {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 10px;
}

:global(.cn-theme-switch-menu__icon) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  border-radius: 10px;
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
}

:global(.cn-theme-switch-menu__copy) {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

:global(.cn-theme-switch-menu__copy strong) {
  color: var(--cn-color-text-primary);
  font-size: 13px;
  font-weight: 600;
}

:global(.cn-theme-switch-menu__copy small) {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  line-height: 1.35;
}
</style>
