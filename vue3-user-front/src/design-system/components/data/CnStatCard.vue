<template>
  <article class="cn-stat-card" :class="[`cn-stat-card--${tone}`, { 'is-loading': loading }]">
    <div class="cn-stat-card__topline">
      <span class="cn-stat-card__tone" aria-hidden="true" />
      <p class="cn-stat-card__title">{{ title }}</p>
    </div>

    <div v-if="loading" class="cn-stat-card__skeleton" aria-label="加载中">
      <span class="cn-stat-card__skeleton-line is-wide" />
      <span class="cn-stat-card__skeleton-line" />
    </div>

    <template v-else>
      <div class="cn-stat-card__value-row">
        <span class="cn-stat-card__value">{{ value }}</span>
        <span v-if="unit" class="cn-stat-card__unit">{{ unit }}</span>
      </div>

      <div v-if="description || trendText" class="cn-stat-card__footer">
        <p v-if="description" class="cn-stat-card__description">{{ description }}</p>
        <span
          v-if="trendText"
          class="cn-stat-card__trend"
          :class="trend ? `cn-stat-card__trend--${trend}` : undefined"
        >
          {{ trendText }}
        </span>
      </div>
    </template>
  </article>
</template>

<script setup lang="ts">
import type { CnStatCardProps } from '../../types/components'

withDefaults(defineProps<CnStatCardProps>(), {
  unit: '',
  description: '',
  trend: 'flat',
  trendText: '',
  tone: 'brand',
  loading: false
})
</script>

<style scoped>
.cn-stat-card {
  position: relative;
  overflow: hidden;
  min-width: 0;
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-card-radius);
  background: var(--cn-card-bg);
  box-shadow: var(--cn-card-shadow);
  transition:
    transform var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-base) var(--cn-ease-out),
    box-shadow var(--cn-motion-base) var(--cn-ease-out);
}

.cn-stat-card:hover {
  transform: translateY(-1px);
  box-shadow: var(--cn-shadow-sm);
}

.cn-stat-card__topline {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  min-width: 0;
}

.cn-stat-card__tone {
  width: 8px;
  height: 24px;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-stat-tone-color);
  flex-shrink: 0;
}

.cn-stat-card__title {
  margin: 0;
  overflow-wrap: anywhere;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
}

.cn-stat-card__value-row {
  display: flex;
  align-items: baseline;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-4);
  min-width: 0;
}

.cn-stat-card__value {
  min-width: 0;
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: 32px;
  font-weight: 700;
  line-height: 1.1;
  overflow-wrap: anywhere;
}

.cn-stat-card__unit {
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
  font-weight: 600;
}

.cn-stat-card__footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-4);
}

.cn-stat-card__description {
  margin: 0;
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
  line-height: 1.55;
}

.cn-stat-card__trend {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 var(--cn-space-2);
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.cn-stat-card__trend--up {
  color: var(--cn-color-success);
  background: var(--cn-color-success-soft);
}

.cn-stat-card__trend--down {
  color: var(--cn-color-danger);
  background: var(--cn-color-danger-soft);
}

.cn-stat-card__trend--flat {
  color: var(--cn-color-text-secondary);
}

.cn-stat-card__skeleton {
  display: grid;
  gap: var(--cn-space-3);
  margin-top: var(--cn-space-4);
}

.cn-stat-card__skeleton-line {
  display: block;
  width: 46%;
  height: 14px;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-bg-surface-muted);
  animation: cn-stat-card-loading 1.2s var(--cn-ease-in-out) infinite alternate;
}

.cn-stat-card__skeleton-line.is-wide {
  width: 72%;
  height: 30px;
}

.cn-stat-card--brand {
  --cn-stat-tone-color: var(--cn-color-brand-primary);
}

.cn-stat-card--success {
  --cn-stat-tone-color: var(--cn-color-success);
}

.cn-stat-card--warning {
  --cn-stat-tone-color: var(--cn-color-warning);
}

.cn-stat-card--danger {
  --cn-stat-tone-color: var(--cn-color-danger);
}

.cn-stat-card--info {
  --cn-stat-tone-color: var(--cn-color-info);
}

.cn-stat-card--neutral {
  --cn-stat-tone-color: var(--cn-color-text-tertiary);
}

@keyframes cn-stat-card-loading {
  from {
    opacity: 0.58;
  }

  to {
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .cn-stat-card,
  .cn-stat-card__skeleton-line {
    animation: none;
    transition: none;
  }
}
</style>
