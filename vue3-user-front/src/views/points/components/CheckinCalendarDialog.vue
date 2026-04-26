<template>
  <div v-if="modelValue" class="dialog-overlay" @click="closeDialog">
    <div class="dialog-content" @click.stop>
      <div class="dialog-header">
        <h3>打卡日历</h3>
        <button class="close-btn" @click="closeDialog">✕</button>
      </div>
      
      <div class="dialog-body">
        <!-- 月份选择 -->
        <div class="month-selector">
          <button class="month-btn" @click="changeMonth(-1)">‹</button>
          <span class="current-month">{{ currentYearMonth }}</span>
          <button class="month-btn" @click="changeMonth(1)">›</button>
        </div>
        
        <div v-if="loading" class="loading-state">
          <div class="loading-spinner">⏳</div>
          <p>加载中...</p>
        </div>
        
        <div v-else class="calendar-container">
          <!-- 星期标题 -->
          <div class="weekdays">
            <div class="weekday" v-for="day in weekdays" :key="day">
              {{ day }}
            </div>
          </div>
          
          <!-- 日期网格 -->
          <div class="calendar-grid">
            <!-- 上个月的日期（灰色显示，不显示打卡状态） -->
            <div 
              v-for="day in prevMonthDays" 
              :key="`prev-${day}`"
              class="calendar-day prev-month"
            >
              {{ day }}
            </div>
            
            <!-- 当前月的日期 -->
            <div 
              v-for="day in currentMonthDays" 
              :key="`current-${day}`"
              class="calendar-day current-month"
              :class="{
                'checked': isCheckedDay(day),
                'today': isToday(day),
                'future': isFutureDay(day)
              }"
            >
              <span class="day-number">{{ day }}</span>
              <div v-if="isCheckedDay(day)" class="check-mark">✓</div>
            </div>
            
            <!-- 下个月的日期（灰色显示） -->
            <div 
              v-for="day in nextMonthDays" 
              :key="`next-${day}`"
              class="calendar-day next-month"
            >
              {{ day }}
            </div>
          </div>
          
          <!-- 统计信息 -->
          <div class="calendar-stats">
            <div class="stat-item">
              <span class="stat-label">本月打卡</span>
              <span class="stat-value">{{ calendarData?.currentMonthCheckinDays || 0 }} 天</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">连续打卡</span>
              <span class="stat-value">{{ calendarData?.continuousDays || 0 }} 天</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import pointsApi from '@/api/points'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

// 响应式数据
const calendarData = ref(null)
const loading = ref(false)
const selectedYearMonth = ref('')

// 星期标题
const weekdays = ['日', '一', '二', '三', '四', '五', '六']

// 计算属性
const currentYearMonth = computed(() => {
  if (!selectedYearMonth.value) return ''
  const [year, month] = selectedYearMonth.value.split('-')
  return `${year}年${parseInt(month)}月`
})

// 获取上个月需要显示的日期
const prevMonthDays = computed(() => {
  if (!selectedYearMonth.value) return []
  
  const [year, month] = selectedYearMonth.value.split('-').map(Number)
  const firstDay = new Date(year, month - 1, 1)
  const dayOfWeek = firstDay.getDay()
  
  if (dayOfWeek === 0) return []
  
  const prevMonth = month === 1 ? 12 : month - 1
  const prevYear = month === 1 ? year - 1 : year
  const daysInPrevMonth = new Date(prevYear, prevMonth, 0).getDate()
  
  const days = []
  for (let i = dayOfWeek - 1; i >= 0; i--) {
    days.push(daysInPrevMonth - i)
  }
  return days
})

// 获取当前月的日期
const currentMonthDays = computed(() => {
  if (!selectedYearMonth.value) return []
  
  const [year, month] = selectedYearMonth.value.split('-').map(Number)
  const daysInMonth = new Date(year, month, 0).getDate()
  
  return Array.from({ length: daysInMonth }, (_, i) => i + 1)
})

// 获取下个月需要显示的日期
const nextMonthDays = computed(() => {
  if (!selectedYearMonth.value) return []
  
  const totalCells = 42 // 6行 x 7列
  const usedCells = prevMonthDays.value.length + currentMonthDays.value.length
  const remainingCells = totalCells - usedCells
  
  return Array.from({ length: remainingCells }, (_, i) => i + 1)
})

// 监听弹窗打开
watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    // 默认显示当前月份
    const now = new Date()
    selectedYearMonth.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
    loadCalendarData()
  }
})

// 加载打卡日历数据
const loadCalendarData = async () => {
  loading.value = true
  
  // 清除旧数据，防止显示缓存
  calendarData.value = null
  
  try {
    const response = await pointsApi.getCheckinCalendar(selectedYearMonth.value)
    calendarData.value = response
    
    // 添加调试信息
    console.log('加载日历数据:', {
      yearMonth: selectedYearMonth.value,
      checkinDays: response.checkinDays,
      totalCheckinDays: response.totalCheckinDays
    })
  } catch (error) {
    console.error('加载打卡日历失败:', error)
    calendarData.value = null
  } finally {
    loading.value = false
  }
}

// 切换月份
const changeMonth = (direction) => {
  const [year, month] = selectedYearMonth.value.split('-').map(Number)
  let newYear = year
  let newMonth = month + direction
  
  if (newMonth > 12) {
    newYear++
    newMonth = 1
  } else if (newMonth < 1) {
    newYear--
    newMonth = 12
  }
  
  selectedYearMonth.value = `${newYear}-${String(newMonth).padStart(2, '0')}`
  loadCalendarData()
}

// 判断是否为已打卡日期
const isCheckedDay = (day) => {
  // 确保有数据且为当前查询的月份
  if (!calendarData.value?.checkinDays || !selectedYearMonth.value) return false
  
  // 确保day是有效的日期
  if (!day || day < 1 || day > 31) return false
  
  // 检查当前月份是否包含这个日期
  const [year, month] = selectedYearMonth.value.split('-').map(Number)
  const daysInMonth = new Date(year, month, 0).getDate()
  if (day > daysInMonth) return false
  
  // 检查是否在打卡日期列表中
  const isChecked = calendarData.value.checkinDays.includes(day)
  
  // 添加调试信息（可以在生产环境中移除）
  if (isChecked) {
    console.log(`✅ 打卡日期: ${selectedYearMonth.value}-${String(day).padStart(2, '0')}`)
  }
  
  return isChecked
}

// 判断是否为今天
const isToday = (day) => {
  const now = new Date()
  const [year, month] = selectedYearMonth.value.split('-').map(Number)
  return now.getFullYear() === year && 
         now.getMonth() + 1 === month && 
         now.getDate() === day
}

// 判断是否为未来日期
const isFutureDay = (day) => {
  const now = new Date()
  const [year, month] = selectedYearMonth.value.split('-').map(Number)
  const targetDate = new Date(year, month - 1, day)
  return targetDate > now
}

// 关闭弹窗
const closeDialog = () => {
  emit('update:modelValue', false)
}
</script>

<style lang="scss" scoped>
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.dialog-content {
  background: white;
  border-radius: 16px;
  width: 100%;
  max-width: 400px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
  
  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #333;
  }
  
  .close-btn {
    background: none;
    border: none;
    font-size: 20px;
    color: #999;
    cursor: pointer;
    padding: 4px;
    border-radius: 4px;
    
    &:hover {
      background: #f5f5f5;
      color: #666;
    }
  }
}

.dialog-body {
  flex: 1;
  padding: 20px;
  overflow: hidden;
}

.month-selector {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  
  .month-btn {
    background: #f5f5f5;
    border: none;
    border-radius: 8px;
    width: 36px;
    height: 36px;
    font-size: 18px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background-color 0.3s ease;
    
    &:hover {
      background: #e0e0e0;
    }
  }
  
  .current-month {
    font-size: 16px;
    font-weight: 600;
    color: #333;
  }
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #999;
  
  .loading-spinner {
    font-size: 32px;
    margin-bottom: 12px;
    animation: spin 1s linear infinite;
  }
  
  p {
    margin: 0;
    font-size: 14px;
  }
}

.calendar-container {
  .weekdays {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 1px;
    margin-bottom: 8px;
    
    .weekday {
      text-align: center;
      padding: 8px 4px;
      font-size: 12px;
      font-weight: 500;
      color: #666;
      background: #f8f9fa;
    }
  }
  
  .calendar-grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 1px;
    background: #e9ecef;
    border-radius: 8px;
    overflow: hidden;
    margin-bottom: 20px;
  }
  
  .calendar-day {
    aspect-ratio: 1;
    background: white;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    position: relative;
    font-size: 14px;
    
    &.prev-month,
    &.next-month {
      color: #ccc;
      background: #f8f9fa;
      
      /* 确保上个月和下个月的日期不会显示任何打卡样式 */
      &.checked {
        background: #f8f9fa !important;
        color: #ccc !important;
        font-weight: normal !important;
        
        .check-mark {
          display: none !important;
        }
      }
    }
    
    &.current-month {
      &.checked {
        background: linear-gradient(135deg, #67c23a, #85ce61);
        color: white;
        font-weight: 600;
        
        .check-mark {
          position: absolute;
          bottom: 2px;
          right: 2px;
          font-size: 10px;
          background: rgba(255, 255, 255, 0.9);
          color: #67c23a;
          border-radius: 50%;
          width: 16px;
          height: 16px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-weight: bold;
        }
      }
      
      &.today {
        border: 2px solid #409eff;
        font-weight: 600;
        color: #409eff;
        
        &.checked {
          border-color: #67c23a;
        }
      }
      
      &.future {
        color: #ddd;
      }
    }
    
    .day-number {
      font-size: 14px;
    }
  }
}

.calendar-stats {
  display: flex;
  justify-content: space-around;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  
  .stat-item {
    text-align: center;
    
    .stat-label {
      display: block;
      font-size: 12px;
      color: #999;
      margin-bottom: 4px;
    }
    
    .stat-value {
      display: block;
      font-size: 16px;
      font-weight: 600;
      color: #333;
    }
  }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
