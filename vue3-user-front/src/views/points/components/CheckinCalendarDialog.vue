<template>
  <el-dialog
    class="checkin-calendar-dialog"
    :model-value="modelValue"
    title="打卡日历"
    width="520px"
    destroy-on-close
    @update:model-value="handleVisibleChange"
  >
    <div class="calendar-dialog">
      <div class="month-selector">
        <el-button circle @click="changeMonth(-1)">‹</el-button>
        <strong class="current-month">{{ currentYearMonth }}</strong>
        <el-button circle @click="changeMonth(1)">›</el-button>
      </div>

      <div v-loading="loading" class="calendar-panel">
        <template v-if="!loading">
          <div class="calendar-legend">
            <CnStatusTag type="success" size="sm">已打卡</CnStatusTag>
            <CnStatusTag type="brand" size="sm" subtle>今天</CnStatusTag>
          </div>

          <div class="weekdays" aria-hidden="true">
            <div v-for="day in weekdays" :key="day" class="weekday">{{ day }}</div>
          </div>

          <div class="calendar-grid">
            <div
              v-for="day in prevMonthDays"
              :key="`prev-${day}`"
              class="calendar-day calendar-day--muted"
            >
              {{ day }}
            </div>

            <div
              v-for="day in currentMonthDays"
              :key="`current-${day}`"
              class="calendar-day"
              :class="{
                'is-checked': isCheckedDay(day),
                'is-today': isToday(day),
                'is-future': isFutureDay(day)
              }"
            >
              <span class="day-number">{{ day }}</span>
              <span v-if="isCheckedDay(day)" class="check-mark">✓</span>
            </div>

            <div
              v-for="day in nextMonthDays"
              :key="`next-${day}`"
              class="calendar-day calendar-day--muted"
            >
              {{ day }}
            </div>
          </div>

          <div class="calendar-stats">
            <div class="stat-pill">
              <span>本月打卡</span>
              <strong>{{ calendarData?.currentMonthCheckinDays || 0 }} 天</strong>
            </div>
            <div class="stat-pill">
              <span>连续打卡</span>
              <strong>{{ calendarData?.continuousDays || 0 }} 天</strong>
            </div>
          </div>
        </template>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { CnStatusTag } from '@/design-system'
import pointsApi from '@/api/points'

interface CalendarData {
  checkinDays?: number[]
  currentMonthCheckinDays?: number
  continuousDays?: number
}

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const calendarData = ref<CalendarData | null>(null)
const loading = ref(false)
const selectedYearMonth = ref('')

const weekdays = ['日', '一', '二', '三', '四', '五', '六']

const currentYearMonth = computed(() => {
  if (!selectedYearMonth.value) return ''
  const [year, month] = selectedYearMonth.value.split('-')
  return `${year}年${Number.parseInt(month, 10)}月`
})

const prevMonthDays = computed(() => {
  if (!selectedYearMonth.value) return []

  const [year, month] = selectedYearMonth.value.split('-').map(Number)
  const firstDay = new Date(year, month - 1, 1)
  const dayOfWeek = firstDay.getDay()

  if (dayOfWeek === 0) return []

  const prevMonth = month === 1 ? 12 : month - 1
  const prevYear = month === 1 ? year - 1 : year
  const daysInPrevMonth = new Date(prevYear, prevMonth, 0).getDate()

  const days: number[] = []
  for (let i = dayOfWeek - 1; i >= 0; i -= 1) {
    days.push(daysInPrevMonth - i)
  }
  return days
})

const currentMonthDays = computed(() => {
  if (!selectedYearMonth.value) return []

  const [year, month] = selectedYearMonth.value.split('-').map(Number)
  const daysInMonth = new Date(year, month, 0).getDate()

  return Array.from({ length: daysInMonth }, (_, i) => i + 1)
})

const nextMonthDays = computed(() => {
  if (!selectedYearMonth.value) return []

  const totalCells = 42
  const usedCells = prevMonthDays.value.length + currentMonthDays.value.length
  const remainingCells = totalCells - usedCells

  return Array.from({ length: remainingCells }, (_, i) => i + 1)
})

watch(
  () => props.modelValue,
  (newValue) => {
    if (newValue) {
      const now = new Date()
      selectedYearMonth.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
      loadCalendarData()
    }
  }
)

const loadCalendarData = async () => {
  loading.value = true
  calendarData.value = null

  try {
    const response = (await pointsApi.getCheckinCalendar(selectedYearMonth.value)) as CalendarData
    calendarData.value = response
  } catch (error) {
    console.error('加载打卡日历失败:', error)
    calendarData.value = null
  } finally {
    loading.value = false
  }
}

const changeMonth = (direction: number) => {
  const [year, month] = selectedYearMonth.value.split('-').map(Number)
  let newYear = year
  let newMonth = month + direction

  if (newMonth > 12) {
    newYear += 1
    newMonth = 1
  } else if (newMonth < 1) {
    newYear -= 1
    newMonth = 12
  }

  selectedYearMonth.value = `${newYear}-${String(newMonth).padStart(2, '0')}`
  loadCalendarData()
}

const isCheckedDay = (day: number) => {
  if (!calendarData.value?.checkinDays || !selectedYearMonth.value) return false
  if (!day || day < 1 || day > 31) return false

  const [year, month] = selectedYearMonth.value.split('-').map(Number)
  const daysInMonth = new Date(year, month, 0).getDate()
  if (day > daysInMonth) return false

  return calendarData.value.checkinDays.includes(day)
}

const isToday = (day: number) => {
  if (!selectedYearMonth.value) return false
  const now = new Date()
  const [year, month] = selectedYearMonth.value.split('-').map(Number)
  return now.getFullYear() === year && now.getMonth() + 1 === month && now.getDate() === day
}

const isFutureDay = (day: number) => {
  if (!selectedYearMonth.value) return false
  const now = new Date()
  const [year, month] = selectedYearMonth.value.split('-').map(Number)
  const targetDate = new Date(year, month - 1, day)
  return targetDate > now
}

const handleVisibleChange = (visible: boolean) => {
  emit('update:modelValue', visible)
}
</script>

<style lang="scss" scoped>
.calendar-dialog {
  display: grid;
  gap: var(--cn-space-4);
}

.month-selector {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
}

.current-month {
  color: var(--cn-color-text-primary);
  font-size: 17px;
}

.calendar-panel {
  min-height: 420px;
}

.calendar-legend {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-bottom: var(--cn-space-3);
}

.weekdays,
.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
}

.weekdays {
  gap: 1px;
  margin-bottom: var(--cn-space-2);
}

.weekday {
  padding: var(--cn-space-2) var(--cn-space-1);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  font-weight: 700;
  text-align: center;
}

.calendar-grid {
  gap: 1px;
  overflow: hidden;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-panel);
  background: var(--cn-color-border-subtle);
}

.calendar-day {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  aspect-ratio: 1;
  min-width: 0;
  background: var(--cn-color-bg-surface);
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 600;
}

.calendar-day--muted {
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-tertiary);
  font-weight: 500;
}

.calendar-day.is-checked {
  background: var(--cn-color-success-soft);
  color: var(--cn-color-success);
}

.calendar-day.is-today {
  box-shadow: inset 0 0 0 2px var(--cn-color-brand-primary);
}

.calendar-day.is-future {
  color: var(--cn-color-text-tertiary);
}

.check-mark {
  position: absolute;
  right: 4px;
  bottom: 3px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-success);
  color: var(--cn-color-bg-surface);
  font-size: 11px;
  font-weight: 800;
}

.calendar-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-3);
  margin-top: var(--cn-space-4);
}

.stat-pill {
  display: grid;
  gap: var(--cn-space-1);
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  text-align: center;

  span {
    color: var(--cn-color-text-secondary);
    font-size: 12px;
  }

  strong {
    color: var(--cn-color-text-primary);
    font-size: 18px;
  }
}

:deep(.el-dialog) {
  max-width: calc(100vw - 32px);
}
</style>
