<template>
  <div class="developer-calendar">
    <!-- 头部区域 -->
    <div class="calendar-header">
      <div class="header-content">
        <div class="title-section">
          <h1 class="page-title">
            <el-icon class="title-icon"><Calendar /></el-icon>
            程序员日历
          </h1>
          <p class="page-subtitle">发现属于程序员的每一个特殊时刻</p>
        </div>

        <!-- 今日推荐卡片 -->
        <div class="today-recommend" v-if="todayRecommend">
          <div class="recommend-header">
            <div class="recommend-date">
              <span class="month">{{ currentMonth }}月</span>
              <span class="day">{{ currentDay }}</span>
            </div>
            <div class="recommend-info">
              <h3>{{ todayRecommend.hasMajorEvents ? '今天有重要事件！' : '今日推荐' }}</h3>
              <p v-if="todayRecommend.specialGreeting">{{ todayRecommend.specialGreeting }}</p>
              <p v-else>{{ getRandomGreeting() }}</p>
            </div>
          </div>

          <div class="today-events" v-if="todayRecommend.todayEvents?.length">
            <div class="event-item"
                 v-for="event in todayRecommend.todayEvents"
                 :key="event.id"
                 :class="{ 'major-event': event.isMajor }"
                 @click="showEventDetail(event)">
              <div class="event-icon">
                <el-icon><component :is="getEventIcon(event.eventType)" /></el-icon>
              </div>
              <div class="event-content">
                <h4>{{ event.eventName }}</h4>
                <p>{{ event.description }}</p>
              </div>
              <div class="event-badge" v-if="event.isMajor">
                <el-tag type="danger" size="small">重要</el-tag>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 日历控制区域 -->
    <div class="calendar-controls">
      <div class="control-section">
        <!-- 月份切换 -->
        <div class="month-selector">
          <el-button @click="previousMonth" :icon="ArrowLeft" circle />
          <span class="current-month">{{ currentYear }}年{{ currentMonth }}月</span>
          <el-button @click="nextMonth" :icon="ArrowRight" circle />
        </div>

        <!-- 事件类型筛选 -->
        <div class="event-filters">
          <el-button-group>
            <el-button
              :type="selectedEventType === null ? 'primary' : ''"
              @click="filterByType(null)">
              全部
            </el-button>
            <el-button
              :type="selectedEventType === 1 ? 'primary' : ''"
              @click="filterByType(1)">
              程序员节日
            </el-button>
            <el-button
              :type="selectedEventType === 2 ? 'primary' : ''"
              @click="filterByType(2)">
              技术纪念日
            </el-button>
            <el-button
              :type="selectedEventType === 3 ? 'primary' : ''"
              @click="filterByType(3)">
              开源节日
            </el-button>
          </el-button-group>
        </div>
      </div>
    </div>

    <!-- 日历主体 -->
    <div class="calendar-main" v-loading="loading">
      <div class="calendar-grid">
        <!-- 星期头部 -->
        <div class="weekdays">
          <div class="weekday" v-for="day in weekdays" :key="day">{{ day }}</div>
        </div>

        <!-- 日期网格 -->
        <div class="days-grid">
          <div
            class="day-cell"
            v-for="day in calendarDays"
            :key="`${day.year}-${day.month}-${day.day}`"
            :class="{
              'other-month': !day.isCurrentMonth,
              'today': day.isToday,
              'has-events': day.events?.length > 0,
              'has-major': day.hasMajorEvents
            }"
            @click="selectDate(day)">

            <div class="day-number">{{ day.day }}</div>

            <div class="day-events" v-if="day.events?.length">
              <div
                class="event-dot"
                v-for="event in day.events.slice(0, 3)"
                :key="event.id"
                :class="[`event-type-${event.eventType}`, { 'major': event.isMajor }]"
                :title="event.eventName">
              </div>
              <div class="more-events" v-if="day.events.length > 3">+{{ day.events.length - 3 }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 事件详情对话框 -->
    <el-dialog
      v-model="eventDialogVisible"
      :title="selectedEvent?.eventName"
      width="600px"
      destroy-on-close>

      <div class="event-detail" v-if="selectedEvent">
        <div class="event-header">
          <div class="event-type-badge">
            <el-tag :type="getEventTypeTag(selectedEvent.eventType)">
              {{ getEventTypeName(selectedEvent.eventType) }}
            </el-tag>
            <el-tag v-if="selectedEvent.isMajor" type="danger" size="small">重要事件</el-tag>
          </div>
          <div class="event-date">{{ formatEventDate(selectedEvent.eventDate) }}</div>
        </div>

        <div class="event-description">
          {{ selectedEvent.description }}
        </div>

        <div class="event-blessing" v-if="selectedEvent.blessingText">
          <el-card class="blessing-card">
            <div class="blessing-content">
              <el-icon class="blessing-icon"><Star /></el-icon>
              <p>{{ selectedEvent.blessingText }}</p>
            </div>
          </el-card>
        </div>

        <div class="event-links" v-if="selectedEvent.relatedLinks?.length">
          <h4>相关链接</h4>
          <div class="links-list">
            <el-link
              v-for="(link, index) in selectedEvent.relatedLinks"
              :key="index"
              :href="link"
              target="_blank"
              type="primary">
              <el-icon><Link /></el-icon>
              {{ link }}
            </el-link>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 日期事件列表对话框 -->
    <el-dialog
      v-model="dateEventsDialogVisible"
      :title="`${selectedDate} 的事件`"
      width="500px">

      <div class="date-events-list">
        <div class="event-list-item"
             v-for="event in selectedDateEvents"
             :key="event.id"
             @click="showEventDetail(event)">
          <div class="event-icon">
            <el-icon><component :is="getEventIcon(event.eventType)" /></el-icon>
          </div>
          <div class="event-info">
            <h4>{{ event.eventName }}</h4>
            <p>{{ event.description }}</p>
            <div class="event-badges">
              <el-tag :type="getEventTypeTag(event.eventType)" size="small">
                {{ getEventTypeName(event.eventType) }}
              </el-tag>
              <el-tag v-if="event.isMajor" type="danger" size="small">重要</el-tag>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Calendar, ArrowLeft, ArrowRight, Star, Link,
  EditPen, Trophy, Box
} from '@element-plus/icons-vue'
import {
  getTodayRecommend,
  getMonthCalendar
} from '@/api/moyu'

// 响应式数据
const loading = ref(false)
const todayRecommend = ref(null)
const monthCalendar = ref(null)
const selectedEventType = ref(null)
const eventDialogVisible = ref(false)
const dateEventsDialogVisible = ref(false)
const selectedEvent = ref(null)
const selectedDateEvents = ref([])
const selectedDate = ref('')

// 日历数据
const currentDate = reactive({
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1
})

const weekdays = ['日', '一', '二', '三', '四', '五', '六']

const greetings = [
  '今天又是充满代码的一天 ✨',
  '愿你的代码没有bug 🐛',
  '记得多喝水，保护好眼睛 👀',
  '今天也要开心写代码哦 😊',
  '每一行代码都是创造 🚀'
]

// 计算属性
const currentYear = computed(() => currentDate.year)
const currentMonth = computed(() => currentDate.month)
const currentDay = computed(() => new Date().getDate())

const calendarDays = computed(() => {
  if (!monthCalendar.value) return []

  const year = currentDate.year
  const month = currentDate.month
  const firstDay = new Date(year, month - 1, 1)
  const lastDay = new Date(year, month, 0)
  const daysInMonth = lastDay.getDate()
  const startWeekday = firstDay.getDay()

  const days = []
  const today = new Date()
  const todayStr = today.toISOString().split('T')[0]

  // 添加上个月的日期
  const prevMonth = month === 1 ? 12 : month - 1
  const prevYear = month === 1 ? year - 1 : year
  const prevLastDay = new Date(prevYear, prevMonth, 0).getDate()

  for (let i = startWeekday - 1; i >= 0; i--) {
    const day = prevLastDay - i
    days.push({
      day,
      month: prevMonth,
      year: prevYear,
      isCurrentMonth: false,
      isToday: false,
      events: [],
      hasMajorEvents: false
    })
  }

  // 添加当月日期
  for (let day = 1; day <= daysInMonth; day++) {
    const dateStr = `${String(day).padStart(2, '0')}`
    const fullDateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    const dayEvents = monthCalendar.value.eventsByDate?.[dateStr] || []
    const hasMajorEvents = dayEvents.some(event => event.isMajor)

    days.push({
      day,
      month,
      year,
      isCurrentMonth: true,
      isToday: fullDateStr === todayStr,
      events: dayEvents,
      hasMajorEvents
    })
  }

  // 填充下个月的日期，使总数为42 (6周 × 7天)
  const remainingDays = 42 - days.length
  const nextMonth = month === 12 ? 1 : month + 1
  const nextYear = month === 12 ? year + 1 : year

  for (let day = 1; day <= remainingDays; day++) {
    days.push({
      day,
      month: nextMonth,
      year: nextYear,
      isCurrentMonth: false,
      isToday: false,
      events: [],
      hasMajorEvents: false
    })
  }

  return days
})

// 工具函数
const getRandomGreeting = () => {
  return greetings[Math.floor(Math.random() * greetings.length)]
}

const getEventTypeName = (type) => {
  const typeMap = {
    1: '程序员节日',
    2: '技术纪念日',
    3: '开源节日'
  }
  return typeMap[type] || '未知'
}

const getEventTypeTag = (type) => {
  const tagMap = {
    1: 'warning',
    2: 'info',
    3: 'success'
  }
  return tagMap[type] || 'info'
}

const getEventIcon = (type) => {
  const iconMap = {
    1: EditPen,
    2: Trophy,
    3: Box
  }
  return iconMap[type] || Calendar
}

const formatEventDate = (eventDate) => {
  const [month, day] = eventDate.split('-')
  return `${parseInt(month)}月${parseInt(day)}日`
}

// 数据加载
const loadTodayRecommend = async () => {
  try {
    const data = await getTodayRecommend()
    todayRecommend.value = data
  } catch (error) {
    console.error('加载今日推荐失败:', error)
  }
}

const loadMonthCalendar = async () => {
  try {
    loading.value = true
    const data = await getMonthCalendar(currentDate.year, currentDate.month)
    monthCalendar.value = data
  } catch (error) {
    console.error('加载月历数据失败:', error)
    ElMessage.error('加载日历数据失败')
  } finally {
    loading.value = false
  }
}

// 事件处理
const previousMonth = () => {
  if (currentDate.month === 1) {
    currentDate.month = 12
    currentDate.year--
  } else {
    currentDate.month--
  }
  loadMonthCalendar()
}

const nextMonth = () => {
  if (currentDate.month === 12) {
    currentDate.month = 1
    currentDate.year++
  } else {
    currentDate.month++
  }
  loadMonthCalendar()
}

const filterByType = (eventType) => {
  selectedEventType.value = eventType
  loadMonthCalendar()
}

const selectDate = (day) => {
  if (day.events?.length > 0) {
    selectedDateEvents.value = day.events
    selectedDate.value = `${day.year}-${String(day.month).padStart(2, '0')}-${String(day.day).padStart(2, '0')}`
    dateEventsDialogVisible.value = true
  }
}

const showEventDetail = (event) => {
  selectedEvent.value = event
  eventDialogVisible.value = true
  dateEventsDialogVisible.value = false
}

// 初始化
onMounted(() => {
  loadTodayRecommend()
  loadMonthCalendar()
})
</script>

<style scoped>
.developer-calendar {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

/* 头部区域 */
.calendar-header {
  padding: 60px 20px;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
}

.title-section {
  text-align: center;
  margin-bottom: 40px;
}

.page-title {
  font-size: 3rem;
  font-weight: 700;
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.title-icon {
  font-size: 3rem;
  color: #ffd93d;
}

.page-subtitle {
  font-size: 1.2rem;
  opacity: 0.9;
  margin: 0;
}

/* 今日推荐卡片 */
.today-recommend {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 24px;
  padding: 30px;
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.recommend-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.recommend-date {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  padding: 16px;
  min-width: 80px;
}

.recommend-date .month {
  font-size: 0.9rem;
  opacity: 0.8;
}

.recommend-date .day {
  font-size: 2rem;
  font-weight: bold;
  line-height: 1;
}

.recommend-info h3 {
  margin: 0 0 8px 0;
  font-size: 1.4rem;
  color: #ffd93d;
}

.recommend-info p {
  margin: 0;
  opacity: 0.9;
}

.today-events {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.event-item {
  display: flex;
  align-items: center;
  gap: 16px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.event-item:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateX(4px);
}

.event-item.major-event {
  border: 2px solid #ffd93d;
  background: rgba(255, 217, 61, 0.1);
}

.event-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
}

.event-content h4 {
  margin: 0 0 4px 0;
  font-size: 1.1rem;
}

.event-content p {
  margin: 0;
  opacity: 0.8;
  font-size: 0.9rem;
}

.event-badge {
  margin-left: auto;
}

/* 控制区域 */
.calendar-controls {
  padding: 0 20px 20px;
}

.control-section {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 20px;
  backdrop-filter: blur(10px);
}

.month-selector {
  display: flex;
  align-items: center;
  gap: 16px;
}

.current-month {
  font-size: 1.2rem;
  font-weight: 600;
  min-width: 120px;
  text-align: center;
}

/* 日历主体 */
.calendar-main {
  padding: 0 20px 60px;
}

.calendar-grid {
  max-width: 1200px;
  margin: 0 auto;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  overflow: hidden;
  color: #333;
}

.weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background: #f8f9fa;
}

.weekday {
  padding: 16px;
  text-align: center;
  font-weight: 600;
  color: #666;
  border-right: 1px solid #e9ecef;
}

.weekday:last-child {
  border-right: none;
}

.days-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}

.day-cell {
  min-height: 120px;
  padding: 12px;
  border-right: 1px solid #e9ecef;
  border-bottom: 1px solid #e9ecef;
  cursor: pointer;
  transition: background-color 0.2s ease;
  display: flex;
  flex-direction: column;
}

.day-cell:nth-child(7n) {
  border-right: none;
}

.day-cell:hover {
  background: #f8f9fa;
}

.day-cell.other-month {
  opacity: 0.3;
}

.day-cell.today {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.day-cell.today .day-number {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.day-cell.has-major {
  border: 2px solid #ffd93d;
}

.day-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  margin-bottom: 8px;
}

.day-events {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.event-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ddd;
}

.event-dot.event-type-1 {
  background: #f39c12;
}

.event-dot.event-type-2 {
  background: #3498db;
}

.event-dot.event-type-3 {
  background: #27ae60;
}

.event-dot.major {
  width: 12px;
  height: 12px;
  background: #e74c3c;
  border: 2px solid white;
}

.more-events {
  font-size: 0.7rem;
  color: #666;
  margin-top: 2px;
}

/* 事件详情对话框 */
.event-detail {
  color: #333;
}

.event-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.event-type-badge {
  display: flex;
  gap: 8px;
}

.event-date {
  font-weight: 600;
  color: #666;
}

.event-description {
  line-height: 1.6;
  margin-bottom: 20px;
  font-size: 1.1rem;
}

.blessing-card {
  margin-bottom: 20px;
  border-left: 4px solid #ffd93d;
}

.blessing-content {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.blessing-icon {
  color: #ffd93d;
  font-size: 1.2rem;
  margin-top: 2px;
}

.blessing-content p {
  margin: 0;
  font-style: italic;
  color: #666;
}

.event-links h4 {
  margin: 0 0 12px 0;
  color: #333;
}

.links-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 日期事件列表对话框 */
.date-events-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.event-list-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  background: #f8f9fa;
  cursor: pointer;
  transition: all 0.2s ease;
}

.event-list-item:hover {
  background: #e9ecef;
  transform: translateX(4px);
}

.event-list-item .event-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  font-size: 1.3rem;
}

.event-info h4 {
  margin: 0 0 8px 0;
  color: #333;
}

.event-info p {
  margin: 0 0 8px 0;
  color: #666;
  font-size: 0.9rem;
}

.event-badges {
  display: flex;
  gap: 8px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-title {
    font-size: 2rem;
    flex-direction: column;
    gap: 8px;
  }

  .control-section {
    flex-direction: column;
    gap: 16px;
  }

  .recommend-header {
    flex-direction: column;
    text-align: center;
  }

  .day-cell {
    min-height: 80px;
    padding: 8px;
  }

  .event-filters :deep(.el-button-group) {
    display: flex;
    flex-wrap: wrap;
  }
}
</style>
