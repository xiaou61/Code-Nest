<template>
  <div class="cn-theme-switch" role="group" aria-label="主题切换">
    <button
      v-for="option in visibleOptions"
      :key="option.value"
      class="cn-theme-switch__button"
      type="button"
      :class="{ 'is-active': themeMode === option.value }"
      :aria-pressed="themeMode === option.value"
      :aria-label="`切换到${option.label}主题`"
      :title="option.description || option.label"
      @click="setTheme(option.value)"
    >
      <span class="cn-theme-switch__icon" aria-hidden="true">{{ option.icon }}</span>
      <span v-if="showLabel" class="cn-theme-switch__label">{{ option.label }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useTheme } from '../../composables/useTheme'
import type { CodeNestThemeMode } from '../../types/theme'

interface ThemeSwitchOption {
  value: CodeNestThemeMode
  label: string
  description?: string
  icon: string
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

const iconMap: Record<CodeNestThemeMode, string> = {
  system: 'SYS',
  light: 'L',
  dark: 'D',
  'professional-blue': 'PB',
  growth: 'G',
  'high-contrast': 'HC'
}

const visibleOptions = computed<ThemeSwitchOption[]>(() =>
  options
    .filter((option) => props.modes.includes(option.value))
    .map((option) => ({
      ...option,
      icon: iconMap[option.value]
    }))
)
</script>

<style scoped>
.cn-theme-switch {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-bg-surface);
  box-shadow: var(--cn-shadow-xs);
}

.cn-theme-switch__button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 34px;
  height: 30px;
  padding: 0 9px;
  border: 0;
  border-radius: var(--cn-radius-pill);
  color: var(--cn-color-text-secondary);
  background: transparent;
  font: inherit;
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
  transition:
    color var(--cn-motion-fast) var(--cn-ease-out),
    background-color var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out);
}

.cn-theme-switch__button:hover {
  color: var(--cn-color-brand-primary);
  background: var(--cn-color-brand-soft);
}

.cn-theme-switch__button:focus-visible {
  outline: 2px solid var(--cn-color-brand-primary);
  outline-offset: 2px;
}

.cn-theme-switch__button.is-active {
  color: var(--cn-button-primary-color);
  background: var(--cn-color-brand-primary);
  box-shadow: 0 6px 14px color-mix(in srgb, var(--cn-color-brand-primary) 24%, transparent);
}

.cn-theme-switch__icon {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0;
}

.cn-theme-switch__label {
  white-space: nowrap;
  font-size: 12px;
  font-weight: 600;
}
</style>
