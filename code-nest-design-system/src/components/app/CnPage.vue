<template>
  <main
    class="cn-page"
    :class="[
      `cn-page--${surface}`,
      {
        'cn-page--dense': dense,
        'cn-page--full-height': fullHeight
      }
    ]"
    :style="pageStyle"
  >
    <header v-if="hasIntro" class="cn-page__intro">
      <slot name="header">
        <p v-if="title" class="cn-page__title">{{ title }}</p>
        <p v-if="description" class="cn-page__description">{{ description }}</p>
      </slot>
    </header>

    <div class="cn-page__body">
      <slot />
    </div>
  </main>
</template>

<script setup lang="ts">
import { computed, useSlots } from 'vue'
import type { CSSProperties } from 'vue'
import type { CnPageProps } from '../../types/components'

const props = withDefaults(defineProps<CnPageProps>(), {
  dense: false,
  fullHeight: false,
  surface: 'transparent',
  maxWidth: '1280px'
})

const slots = useSlots()

const hasIntro = computed(() => Boolean(slots.header || props.title || props.description))

const pageStyle = computed<CSSProperties>(() => ({
  '--cn-page-max-width': props.maxWidth
}))
</script>

<style scoped>
.cn-page {
  width: min(100%, var(--cn-page-max-width));
  min-width: 0;
  margin: 0 auto;
  padding: var(--cn-space-6);
  color: var(--cn-color-text-primary);
  transition:
    color var(--cn-motion-base) var(--cn-ease-out),
    background-color var(--cn-motion-base) var(--cn-ease-out),
    border-color var(--cn-motion-base) var(--cn-ease-out),
    box-shadow var(--cn-motion-base) var(--cn-ease-out);
}

.cn-page--dense {
  padding: var(--cn-space-4);
}

.cn-page--full-height {
  min-height: 100%;
}

.cn-page--plain {
  border-radius: var(--cn-radius-panel);
  background: var(--cn-color-bg-surface);
}

.cn-page--panel {
  border: 1px solid var(--cn-panel-border);
  border-radius: var(--cn-panel-radius);
  background: var(--cn-panel-bg);
  box-shadow: var(--cn-shadow-card);
}

.cn-page__intro {
  margin-bottom: var(--cn-space-6);
}

.cn-page__title {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: 24px;
  font-weight: 650;
  line-height: 1.25;
}

.cn-page__description {
  max-width: 760px;
  margin: var(--cn-space-2) 0 0;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.cn-page__body {
  display: grid;
  gap: var(--cn-space-5);
  min-width: 0;
}

@media (max-width: 768px) {
  .cn-page,
  .cn-page--dense {
    padding: var(--cn-space-4);
  }

  .cn-page__title {
    font-size: 20px;
  }
}
</style>
