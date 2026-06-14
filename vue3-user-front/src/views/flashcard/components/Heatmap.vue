<template>
  <div class="heatmap-container">
    <div class="heatmap-header">
      <div class="heatmap-title">
        <el-icon><Calendar /></el-icon>
        <span>学习热力图</span>
      </div>
      <div class="heatmap-summary" v-if="summary">
        <span>过去一年学习了 <strong>{{ summary.totalDays }}</strong> 天，共 <strong>{{ summary.totalCards }}</strong> 张卡片</span>
      </div>
    </div>
    
    <div class="heatmap-content" v-if="data && data.length > 0">
      <div class="heatmap-months">
        <span v-for="month in months" :key="month" class="month-label">{{ month }}</span>
      </div>
      <div class="heatmap-grid">
        <div class="heatmap-weekdays">
          <span>一</span>
          <span>三</span>
          <span>五</span>
        </div>
        <div class="heatmap-cells">
          <div 
            v-for="(week, weekIndex) in weeks" 
            :key="weekIndex" 
            class="heatmap-week"
          >
            <el-tooltip
              v-for="(day, dayIndex) in week"
              :key="dayIndex"
              :content="day ? `${day.date}: 学习 ${day.count} 张卡片` : ''"
              placement="top"
              :disabled="!day"
            >
              <div 
                class="heatmap-day"
                :class="[`level-${day?.level || 0}`, { empty: !day }]"
                @click="day && $emit('dayClick', day)"
              />
            </el-tooltip>
          </div>
        </div>
      </div>
      <div class="heatmap-legend">
        <span class="legend-label">少</span>
        <div class="legend-cell level-0"></div>
        <div class="legend-cell level-1"></div>
        <div class="legend-cell level-2"></div>
        <div class="legend-cell level-3"></div>
        <div class="legend-cell level-4"></div>
        <span class="legend-label">多</span>
      </div>
    </div>
    
    <CnEmptyState v-else title="暂无学习记录" icon="HEAT" size="sm" surface="transparent" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Calendar } from '@element-plus/icons-vue'
import { CnEmptyState } from '@/design-system'

interface HeatmapDay {
  date: string
  count: number
  level?: number
}

const props = withDefaults(defineProps<{
  data?: HeatmapDay[]
}>(), {
  data: () => []
})

defineEmits<{
  (e: 'dayClick', day: HeatmapDay): void
}>()

// 计算总结数据
const summary = computed(() => {
  if (!props.data || props.data.length === 0) return null
  const totalDays = props.data.filter(d => d.count > 0).length
  const totalCards = props.data.reduce((sum, d) => sum + (d.count || 0), 0)
  return { totalDays, totalCards }
})

// 生成月份标签
const months = computed(() => {
  const monthNames = ['一月', '二月', '三月', '四月', '五月', '六月', 
                     '七月', '八月', '九月', '十月', '十一月', '十二月']
  const result: string[] = []
  const today = new Date()
  for (let i = 11; i >= 0; i--) {
    const d = new Date(today.getFullYear(), today.getMonth() - i, 1)
    result.push(monthNames[d.getMonth()])
  }
  return result
})

// 生成周数据（按列排列）
const weeks = computed(() => {
  if (!props.data || props.data.length === 0) return []
  
  // 创建日期到数据的映射
  const dataMap = new Map<string, HeatmapDay>()
  props.data.forEach(item => {
    dataMap.set(item.date, item)
  })
  
  const result: Array<Array<HeatmapDay | null>> = []
  const today = new Date()
  const startDate = new Date(today)
  startDate.setDate(startDate.getDate() - 364) // 过去一年
  
  // 调整到周一
  const dayOfWeek = startDate.getDay()
  const diff = dayOfWeek === 0 ? -6 : 1 - dayOfWeek
  startDate.setDate(startDate.getDate() + diff)
  
  let currentDate = new Date(startDate)
  let currentWeek: Array<HeatmapDay | null> = []
  
  while (currentDate <= today) {
    const dateStr = currentDate.toISOString().split('T')[0]
    const dayData = dataMap.get(dateStr)
    
    currentWeek.push(dayData ? { ...dayData } : null)
    
    if (currentWeek.length === 7) {
      result.push(currentWeek)
      currentWeek = []
    }
    
    currentDate.setDate(currentDate.getDate() + 1)
  }
  
  if (currentWeek.length > 0) {
    while (currentWeek.length < 7) {
      currentWeek.push(null)
    }
    result.push(currentWeek)
  }
  
  return result
})
</script>

<style lang="scss" scoped>
.heatmap-container {
  background: var(--cn-color-bg-surface);
  border-radius: var(--cn-radius-panel);
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-color-border-subtle);
  box-shadow: var(--cn-shadow-card);
}

.heatmap-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--cn-space-4);
  
  .heatmap-title {
    display: flex;
    align-items: center;
    gap: var(--cn-space-2);
    font-size: 16px;
    font-weight: 600;
    color: var(--cn-color-text-primary);
  }
  
  .heatmap-summary {
    font-size: 13px;
    color: var(--cn-color-text-secondary);
    
    strong {
      color: var(--cn-color-brand-primary);
    }
  }
}

.heatmap-content {
  overflow-x: auto;
}

.heatmap-months {
  display: flex;
  padding-left: 30px;
  margin-bottom: 4px;
  
  .month-label {
    flex: 1;
    font-size: 11px;
    color: var(--cn-color-text-secondary);
    text-align: center;
  }
}

.heatmap-grid {
  display: flex;
  gap: 4px;
}

.heatmap-weekdays {
  display: flex;
  flex-direction: column;
  justify-content: space-around;
  padding-right: 4px;
  
  span {
    font-size: 10px;
    color: var(--cn-color-text-secondary);
    height: 12px;
    line-height: 12px;
  }
}

.heatmap-cells {
  display: flex;
  gap: 3px;
  flex: 1;
}

.heatmap-week {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.heatmap-day {
  width: 12px;
  height: 12px;
  border-radius: 2px;
  cursor: pointer;
  transition: transform var(--cn-motion-fast) var(--cn-ease-out);
  
  &:hover:not(.empty) {
    transform: scale(1.2);
  }
  
  &.empty {
    background: transparent;
    cursor: default;
  }
}

.heatmap-day.level-0,
.legend-cell.level-0 {
  background: var(--cn-color-bg-surface-muted);
}

.heatmap-day.level-1,
.legend-cell.level-1 {
  background: color-mix(in srgb, var(--cn-color-success) 24%, var(--cn-color-bg-surface));
}

.heatmap-day.level-2,
.legend-cell.level-2 {
  background: color-mix(in srgb, var(--cn-color-success) 48%, var(--cn-color-bg-surface));
}

.heatmap-day.level-3,
.legend-cell.level-3 {
  background: color-mix(in srgb, var(--cn-color-success) 74%, var(--cn-color-bg-surface));
}

.heatmap-day.level-4,
.legend-cell.level-4 {
  background: var(--cn-color-success);
}

.heatmap-legend {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  margin-top: 12px;
  
  .legend-label {
    font-size: 11px;
    color: var(--cn-color-text-secondary);
  }
  
  .legend-cell {
    width: 12px;
    height: 12px;
    border-radius: 2px;
  }
}
</style>
