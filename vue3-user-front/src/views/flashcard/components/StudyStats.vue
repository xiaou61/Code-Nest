<template>
  <div class="study-stats">
    <div class="stats-header">
      <el-icon><DataLine /></el-icon>
      <span>学习统计</span>
    </div>
    
    <div class="stats-grid">
      <CnStatCard title="今日已学" :value="stats?.todayLearnedCount || 0" tone="brand" />
      <CnStatCard title="待复习" :value="stats?.todayDueCount || 0" tone="warning" />
      <CnStatCard title="新卡片" :value="stats?.todayNewCount || 0" tone="success" />
      <CnStatCard title="连续天数" :value="stats?.streakDays || 0" tone="danger" />
    </div>
    
    <div class="mastery-section" v-if="stats">
      <div class="section-title">掌握程度</div>
      <div class="mastery-bar">
        <div 
          class="mastery-segment mastered" 
          :style="{ '--mastery-size': masteredPercent + '%' }"
          v-if="masteredPercent > 0"
        >
          <span v-if="masteredPercent > 15">{{ stats.masteredCount }}</span>
        </div>
        <div 
          class="mastery-segment learning" 
          :style="{ '--mastery-size': learningPercent + '%' }"
          v-if="learningPercent > 0"
        >
          <span v-if="learningPercent > 15">{{ stats.learningCount }}</span>
        </div>
        <div 
          class="mastery-segment new" 
          :style="{ '--mastery-size': newPercent + '%' }"
          v-if="newPercent > 0"
        >
          <span v-if="newPercent > 15">{{ stats.newCount }}</span>
        </div>
      </div>
      <div class="mastery-legend">
        <div class="legend-item">
          <span class="dot mastered"></span>
          <span>已掌握 {{ stats.masteredCount || 0 }}</span>
        </div>
        <div class="legend-item">
          <span class="dot learning"></span>
          <span>学习中 {{ stats.learningCount || 0 }}</span>
        </div>
        <div class="legend-item">
          <span class="dot new"></span>
          <span>新学 {{ stats.newCount || 0 }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { DataLine } from '@element-plus/icons-vue'
import { CnStatCard } from '@/design-system'

interface StudyStatsData {
  todayLearnedCount?: number
  todayDueCount?: number
  todayNewCount?: number
  streakDays?: number
  masteredCount?: number
  learningCount?: number
  newCount?: number
}

const props = defineProps<{
  stats?: StudyStatsData | null
}>()

const total = computed(() => {
  if (!props.stats) return 0
  return (props.stats.masteredCount || 0) + 
         (props.stats.learningCount || 0) + 
         (props.stats.newCount || 0)
})

const masteredPercent = computed(() => {
  if (total.value === 0) return 0
  return Math.round((props.stats?.masteredCount || 0) / total.value * 100)
})

const learningPercent = computed(() => {
  if (total.value === 0) return 0
  return Math.round((props.stats?.learningCount || 0) / total.value * 100)
})

const newPercent = computed(() => {
  if (total.value === 0) return 0
  return 100 - masteredPercent.value - learningPercent.value
})
</script>

<style lang="scss" scoped>
.study-stats {
  background: var(--cn-color-bg-surface);
  border-radius: var(--cn-radius-panel);
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-color-border-subtle);
  box-shadow: var(--cn-shadow-card);
}

.stats-header {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  font-size: 16px;
  font-weight: 600;
  color: var(--cn-color-text-primary);
  margin-bottom: var(--cn-space-4);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--cn-space-3);
  margin-bottom: var(--cn-space-5);
}

.mastery-section {
  .section-title {
    font-size: 14px;
    font-weight: 500;
    color: var(--cn-color-text-primary);
    margin-bottom: var(--cn-space-3);
  }
}

.mastery-bar {
  display: flex;
  height: 24px;
  border-radius: var(--cn-radius-pill);
  overflow: hidden;
  background: var(--cn-color-bg-surface-muted);
  
  .mastery-segment {
    display: flex;
    align-items: center;
    justify-content: center;
    width: var(--mastery-size);
    color: var(--cn-button-primary-color);
    font-size: 12px;
    font-weight: 500;
    transition: width var(--cn-motion-base) var(--cn-ease-out);
    
    &.mastered {
      background: var(--cn-color-success);
    }
    
    &.learning {
      background: var(--cn-color-warning);
    }
    
    &.new {
      background: var(--cn-color-text-tertiary);
    }
  }
}

.mastery-legend {
  display: flex;
  justify-content: center;
  gap: var(--cn-space-5);
  margin-top: var(--cn-space-3);
  
  .legend-item {
    display: flex;
    align-items: center;
    gap: var(--cn-space-2);
    font-size: 12px;
    color: var(--cn-color-text-secondary);
    
    .dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      
      &.mastered {
        background: var(--cn-color-success);
      }
      
      &.learning {
        background: var(--cn-color-warning);
      }
      
      &.new {
        background: var(--cn-color-text-tertiary);
      }
    }
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .mastery-legend {
    flex-wrap: wrap;
    justify-content: flex-start;
    gap: var(--cn-space-3);
  }
}

@media (prefers-reduced-motion: reduce) {
  .mastery-segment {
    transition: none;
  }
}
</style>
