<template>
  <section class="cn-page-header" :class="{ 'cn-page-header--compact': compact }">
    <nav v-if="breadcrumbs?.length" class="cn-page-header__breadcrumbs" aria-label="面包屑">
      <template v-for="(item, index) in breadcrumbs" :key="`${item.label}-${index}`">
        <RouterLink
          v-if="item.to && index < breadcrumbs.length - 1"
          class="cn-page-header__breadcrumb-link"
          :to="item.to"
        >
          {{ item.label }}
        </RouterLink>
        <span v-else class="cn-page-header__breadcrumb-current">{{ item.label }}</span>
        <span v-if="index < breadcrumbs.length - 1" class="cn-page-header__breadcrumb-separator">/</span>
      </template>
    </nav>

    <div class="cn-page-header__main">
      <div class="cn-page-header__copy">
        <p v-if="eyebrow" class="cn-page-header__eyebrow">{{ eyebrow }}</p>
        <h1 class="cn-page-header__title">{{ title }}</h1>
        <p v-if="description" class="cn-page-header__description">{{ description }}</p>
        <div v-if="hasMeta" class="cn-page-header__meta">
          <slot name="meta" />
        </div>
      </div>

      <div v-if="hasActions" class="cn-page-header__actions">
        <slot name="actions" />
      </div>
    </div>

    <div v-if="hasDefault" class="cn-page-header__extra">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, useSlots } from 'vue'
import type { CnPageHeaderProps } from '../../types/components'

withDefaults(defineProps<CnPageHeaderProps>(), {
  description: '',
  eyebrow: '',
  breadcrumbs: () => [],
  compact: false
})

const slots = useSlots()

const hasActions = computed(() => Boolean(slots.actions))
const hasMeta = computed(() => Boolean(slots.meta))
const hasDefault = computed(() => Boolean(slots.default))
</script>

<style scoped>
.cn-page-header {
  display: grid;
  gap: var(--cn-space-4);
  min-width: 0;
}

.cn-page-header--compact {
  gap: var(--cn-space-3);
}

.cn-page-header__breadcrumbs {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
  line-height: 1.4;
}

.cn-page-header__breadcrumb-link {
  color: var(--cn-color-text-secondary);
  text-decoration: none;
  transition: color var(--cn-motion-fast) var(--cn-ease-out);
}

.cn-page-header__breadcrumb-link:hover {
  color: var(--cn-color-brand-primary);
}

.cn-page-header__breadcrumb-current {
  color: var(--cn-color-text-tertiary);
}

.cn-page-header__breadcrumb-separator {
  color: var(--cn-color-border);
}

.cn-page-header__main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-5);
  min-width: 0;
}

.cn-page-header__copy {
  min-width: 0;
}

.cn-page-header__eyebrow {
  margin: 0 0 var(--cn-space-2);
  color: var(--cn-color-brand-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
}

.cn-page-header__title {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: 26px;
  font-weight: 650;
  line-height: 1.25;
}

.cn-page-header--compact .cn-page-header__title {
  font-size: 22px;
}

.cn-page-header__description {
  max-width: 780px;
  margin: var(--cn-space-2) 0 0;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.cn-page-header__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-3);
}

.cn-page-header__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--cn-space-2);
  flex-shrink: 0;
}

.cn-page-header__extra {
  min-width: 0;
}

@media (max-width: 768px) {
  .cn-page-header__main {
    display: grid;
  }

  .cn-page-header__actions {
    justify-content: flex-start;
  }

  .cn-page-header__title {
    font-size: 22px;
  }
}
</style>
