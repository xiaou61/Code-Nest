<template>
  <CnPage class="developer-calendar-page" max-width="1280px" full-height>
    <CnPageHeader
      title="程序员日历"
      description="查看程序员节日、技术纪念日和开源相关特殊日期。"
      eyebrow="MOYU TOOL"
      :breadcrumbs="[{ label: '摸鱼工具箱', to: '/moyu-tools' }, { label: '程序员日历' }]"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">{{ currentYear }}年{{ currentMonth }}月</CnStatusTag>
        <CnStatusTag type="info" size="sm" subtle>{{ selectedTypeLabel }}</CnStatusTag>
        <CnStatusTag v-if="majorEventCount" type="warning" size="sm">{{ majorEventCount }} 个重要事件</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="ArrowLeft" @click="goBack">返回工具箱</el-button>
        <el-button type="primary" :icon="Refresh" :loading="loading" @click="loadAll">
          刷新日历
        </el-button>
      </template>
    </CnPageHeader>

    <section class="calendar-stats" aria-label="日历概览">
      <CnStatCard
        title="当前月份"
        :value="`${currentMonth}月`"
        :description="`${currentYear} 年日历视图`"
        tone="brand"
        trend="flat"
        trend-text="月份"
      />
      <CnStatCard
        title="事件数量"
        :value="currentMonthEventCount"
        unit="个"
        :description="selectedTypeLabel"
        tone="info"
        trend="flat"
        trend-text="事件"
      />
      <CnStatCard
        title="今日事件"
        :value="filteredTodayEvents.length"
        unit="个"
        :description="todayRecommend?.hasMajorEvents ? '包含重要事件' : '今日推荐'"
        :tone="todayRecommend?.hasMajorEvents ? 'warning' : 'success'"
        trend="flat"
        trend-text="今日"
      />
    </section>

    <CnSection title="今日推荐" :description="todayDescription" divided>
      <div v-if="todayRecommend" class="today-panel">
        <div class="date-tile">
          <span class="date-tile__month">{{ todayMonth }}月</span>
          <span class="date-tile__day">{{ todayDay }}</span>
        </div>

        <div class="today-copy">
          <div class="today-copy__header">
            <h2>{{ todayRecommend.hasMajorEvents ? '今天有重要事件' : '今日推荐' }}</h2>
            <CnStatusTag :type="todayRecommend.hasMajorEvents ? 'warning' : 'success'" size="sm">
              {{ todayRecommend.hasMajorEvents ? '重要' : '推荐' }}
            </CnStatusTag>
          </div>
          <p>{{ todayRecommend.specialGreeting || defaultGreeting }}</p>

          <div v-if="filteredTodayEvents.length" class="today-events">
            <button
              v-for="event in filteredTodayEvents"
              :key="event.id"
              class="event-row"
              :class="{ 'is-major': event.isMajor }"
              type="button"
              @click="showEventDetail(event)"
            >
              <span class="event-row__icon">
                <el-icon><component :is="getEventIcon(event.eventType)" /></el-icon>
              </span>
              <span class="event-row__content">
                <span class="event-row__title">{{ event.eventName }}</span>
                <span class="event-row__desc">{{ event.description }}</span>
              </span>
              <CnStatusTag v-if="event.isMajor" type="warning" size="sm">重要</CnStatusTag>
            </button>
          </div>

          <CnEmptyState
            v-else
            title="今日暂无匹配事件"
            description="可以切换事件类型，或查看完整月历。"
            icon="CAL"
            size="sm"
            surface="transparent"
          />
        </div>
      </div>

      <CnEmptyState
        v-else
        title="暂无今日推荐"
        description="可以刷新日历后再试。"
        icon="CAL"
        surface="transparent"
      >
        <template #actions>
          <el-button type="primary" :loading="loading" @click="loadAll">重新加载</el-button>
        </template>
      </CnEmptyState>
    </CnSection>

    <CnSection title="月历视图" :description="calendarDescription" divided>
      <template #actions>
        <div class="month-selector">
          <el-button :icon="ArrowLeft" circle @click="previousMonth" />
          <span class="current-month">{{ currentYear }}年{{ currentMonth }}月</span>
          <el-button :icon="ArrowRight" circle @click="nextMonth" />
        </div>
      </template>

      <div class="calendar-toolbar">
        <el-button-group class="event-filter-group">
          <el-button :type="selectedEventType === null ? 'primary' : 'default'" @click="filterByType(null)">
            全部
          </el-button>
          <el-button :type="selectedEventType === 1 ? 'primary' : 'default'" @click="filterByType(1)">
            程序员节日
          </el-button>
          <el-button :type="selectedEventType === 2 ? 'primary' : 'default'" @click="filterByType(2)">
            技术纪念日
          </el-button>
          <el-button :type="selectedEventType === 3 ? 'primary' : 'default'" @click="filterByType(3)">
            开源节日
          </el-button>
        </el-button-group>
      </div>

      <div v-loading="loading" class="calendar-grid">
        <div class="weekdays">
          <div v-for="day in weekdays" :key="day" class="weekday">{{ day }}</div>
        </div>

        <div class="days-grid">
          <button
            v-for="day in calendarDays"
            :key="`${day.year}-${day.month}-${day.day}`"
            class="day-cell"
            :class="{
              'is-other-month': !day.isCurrentMonth,
              'is-today': day.isToday,
              'has-events': day.events.length > 0,
              'has-major': day.hasMajorEvents
            }"
            type="button"
            @click="selectDate(day)"
          >
            <span class="day-number">{{ day.day }}</span>

            <span v-if="day.events.length" class="day-events">
              <span
                v-for="event in day.events.slice(0, 3)"
                :key="event.id"
                class="event-dot"
                :class="[`event-type-${event.eventType}`, { 'is-major': event.isMajor }]"
                :title="event.eventName"
              />
              <span v-if="day.events.length > 3" class="more-events">+{{ day.events.length - 3 }}</span>
            </span>
          </button>
        </div>
      </div>
    </CnSection>

    <el-dialog
      v-model="eventDialogVisible"
      :title="selectedEvent?.eventName"
      width="min(640px, 92vw)"
      destroy-on-close
    >
      <div v-if="selectedEvent" class="event-detail">
        <div class="event-detail__header">
          <div class="event-detail__badges">
            <CnStatusTag :type="getEventTone(selectedEvent.eventType)" size="sm">
              {{ getEventTypeName(selectedEvent.eventType) }}
            </CnStatusTag>
            <CnStatusTag v-if="selectedEvent.isMajor" type="warning" size="sm">重要事件</CnStatusTag>
          </div>
          <span class="event-date">{{ formatEventDate(selectedEvent.eventDate) }}</span>
        </div>

        <p class="event-description">{{ selectedEvent.description }}</p>

        <div v-if="selectedEvent.blessingText" class="blessing-card">
          <el-icon><Star /></el-icon>
          <p>{{ selectedEvent.blessingText }}</p>
        </div>

        <div v-if="selectedEvent.relatedLinks?.length" class="event-links">
          <h4>相关链接</h4>
          <div class="links-list">
            <el-link
              v-for="(link, index) in selectedEvent.relatedLinks"
              :key="`${link}-${index}`"
              :href="link"
              target="_blank"
              type="primary"
            >
              <el-icon><Link /></el-icon>
              {{ link }}
            </el-link>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="dateEventsDialogVisible" :title="`${selectedDate} 的事件`" width="min(560px, 92vw)">
      <div class="date-events-list">
        <button
          v-for="event in selectedDateEvents"
          :key="event.id"
          class="date-event-item"
          type="button"
          @click="showEventDetail(event)"
        >
          <span class="event-row__icon">
            <el-icon><component :is="getEventIcon(event.eventType)" /></el-icon>
          </span>
          <span class="event-row__content">
            <span class="event-row__title">{{ event.eventName }}</span>
            <span class="event-row__desc">{{ event.description }}</span>
            <span class="event-badges">
              <CnStatusTag :type="getEventTone(event.eventType)" size="sm" subtle>
                {{ getEventTypeName(event.eventType) }}
              </CnStatusTag>
              <CnStatusTag v-if="event.isMajor" type="warning" size="sm">重要</CnStatusTag>
            </span>
          </span>
        </button>
      </div>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { Component } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, Box, Calendar, EditPen, Link, Refresh, Star, Trophy } from '@element-plus/icons-vue'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import type { CnTone } from '@/design-system'
import { getMonthCalendar, getTodayRecommend } from '@/api/moyu'

type EventType = 1 | 2 | 3

interface CalendarEvent {
  id: number | string
  eventName: string
  description?: string
  eventDate: string
  eventType: EventType
  isMajor?: boolean
  blessingText?: string
  relatedLinks?: string[]
}

interface TodayRecommend {
  hasMajorEvents?: boolean
  specialGreeting?: string
  todayEvents?: CalendarEvent[]
}

interface MonthCalendar {
  eventsByDate?: Record<string, CalendarEvent[]>
}

interface CalendarDay {
  day: number
  month: number
  year: number
  isCurrentMonth: boolean
  isToday: boolean
  events: CalendarEvent[]
  hasMajorEvents: boolean
}

const router = useRouter()

const loading = ref(false)
const todayRecommend = ref<TodayRecommend | null>(null)
const monthCalendar = ref<MonthCalendar | null>(null)
const selectedEventType = ref<EventType | null>(null)
const eventDialogVisible = ref(false)
const dateEventsDialogVisible = ref(false)
const selectedEvent = ref<CalendarEvent | null>(null)
const selectedDateEvents = ref<CalendarEvent[]>([])
const selectedDate = ref('')

const now = new Date()
const todayMonth = now.getMonth() + 1
const todayDay = now.getDate()

const currentDate = reactive({
  year: now.getFullYear(),
  month: now.getMonth() + 1
})

const weekdays = ['日', '一', '二', '三', '四', '五', '六']

const greetings = [
  '今天又是充满代码的一天 ✨',
  '愿你的代码没有bug 🐛',
  '记得多喝水，保护好眼睛 👀',
  '今天也要开心写代码哦 😊',
  '每一行代码都是创造 🚀'
]

const defaultGreeting = greetings[Math.floor(Math.random() * greetings.length)]

const currentYear = computed(() => currentDate.year)
const currentMonth = computed(() => currentDate.month)

const selectedTypeLabel = computed(() => {
  if (!selectedEventType.value) return '全部事件'
  return getEventTypeName(selectedEventType.value)
})

const filteredTodayEvents = computed(() => {
  const events = todayRecommend.value?.todayEvents || []
  if (!selectedEventType.value) return events
  return events.filter((event) => event.eventType === selectedEventType.value)
})

const calendarDays = computed<CalendarDay[]>(() => {
  if (!monthCalendar.value) return []

  const year = currentDate.year
  const month = currentDate.month
  const firstDay = new Date(year, month - 1, 1)
  const lastDay = new Date(year, month, 0)
  const daysInMonth = lastDay.getDate()
  const startWeekday = firstDay.getDay()

  const days: CalendarDay[] = []
  const today = new Date()
  const todayStr = today.toISOString().split('T')[0]

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

  for (let day = 1; day <= daysInMonth; day++) {
    const dateStr = `${String(day).padStart(2, '0')}`
    const fullDateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    const rawEvents = monthCalendar.value.eventsByDate?.[dateStr] || []
    const events = selectedEventType.value
      ? rawEvents.filter((event) => event.eventType === selectedEventType.value)
      : rawEvents

    days.push({
      day,
      month,
      year,
      isCurrentMonth: true,
      isToday: fullDateStr === todayStr,
      events,
      hasMajorEvents: events.some((event) => event.isMajor)
    })
  }

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

const currentMonthEvents = computed(() => {
  return calendarDays.value
    .filter((day) => day.isCurrentMonth)
    .flatMap((day) => day.events)
})

const currentMonthEventCount = computed(() => currentMonthEvents.value.length)
const majorEventCount = computed(() => currentMonthEvents.value.filter((event) => event.isMajor).length)

const todayDescription = computed(() => {
  if (!todayRecommend.value) return '今日推荐暂未加载。'
  return `今日展示 ${filteredTodayEvents.value.length} 个匹配事件。`
})

const calendarDescription = computed(() => {
  return `当前视图包含 ${currentMonthEventCount.value} 个${selectedTypeLabel.value}。`
})

const goBack = () => {
  router.push('/moyu-tools')
}

const getEventTypeName = (type?: number) => {
  const typeMap: Record<number, string> = {
    1: '程序员节日',
    2: '技术纪念日',
    3: '开源节日'
  }
  return type ? typeMap[type] || '未知' : '全部事件'
}

const getEventTone = (type?: number): CnTone => {
  const toneMap: Record<number, CnTone> = {
    1: 'warning',
    2: 'info',
    3: 'success'
  }
  return type ? toneMap[type] || 'neutral' : 'neutral'
}

const getEventIcon = (type?: number): Component => {
  const iconMap: Record<number, Component> = {
    1: EditPen,
    2: Trophy,
    3: Box
  }
  return type ? iconMap[type] || Calendar : Calendar
}

const formatEventDate = (eventDate?: string) => {
  if (!eventDate) return ''
  const [month, day] = eventDate.split('-')
  return `${parseInt(month, 10)}月${parseInt(day, 10)}日`
}

const loadTodayRecommend = async () => {
  try {
    const data = (await getTodayRecommend()) as TodayRecommend | null
    todayRecommend.value = data
  } catch (error) {
    console.error('加载今日推荐失败:', error)
  }
}

const loadMonthCalendar = async () => {
  try {
    loading.value = true
    const data = (await getMonthCalendar(currentDate.year, currentDate.month)) as MonthCalendar | null
    monthCalendar.value = data
  } catch (error) {
    console.error('加载月历数据失败:', error)
    ElMessage.error('加载日历数据失败')
  } finally {
    loading.value = false
  }
}

const loadAll = async () => {
  await Promise.all([loadTodayRecommend(), loadMonthCalendar()])
}

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

const filterByType = (eventType: EventType | null) => {
  selectedEventType.value = eventType
  loadMonthCalendar()
}

const selectDate = (day: CalendarDay) => {
  if (day.events.length > 0) {
    selectedDateEvents.value = day.events
    selectedDate.value = `${day.year}-${String(day.month).padStart(2, '0')}-${String(day.day).padStart(2, '0')}`
    dateEventsDialogVisible.value = true
  }
}

const showEventDetail = (event: CalendarEvent) => {
  selectedEvent.value = event
  eventDialogVisible.value = true
  dateEventsDialogVisible.value = false
}

onMounted(() => {
  loadAll()
})
</script>

<style scoped>
.developer-calendar-page {
  min-width: 0;
}

.calendar-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.today-panel {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: var(--cn-space-5);
  align-items: start;
  min-width: 0;
}

.date-tile {
  display: grid;
  justify-items: center;
  gap: var(--cn-space-1);
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-panel);
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
}

.date-tile__month {
  font-size: 13px;
  font-weight: 700;
}

.date-tile__day {
  font-family: var(--cn-font-heading);
  font-size: 42px;
  font-weight: 800;
  line-height: 1;
}

.today-copy {
  display: grid;
  gap: var(--cn-space-4);
  min-width: 0;
}

.today-copy__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
}

.today-copy h2 {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 20px;
  font-weight: 700;
  line-height: 1.35;
}

.today-copy p {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.today-events,
.date-events-list {
  display: grid;
  gap: var(--cn-space-3);
}

.event-row,
.date-event-item {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  width: 100%;
  min-width: 0;
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface);
  color: inherit;
  cursor: pointer;
  text-align: left;
  transition:
    background-color var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    transform var(--cn-motion-fast) var(--cn-ease-out);
}

.event-row:hover,
.date-event-item:hover {
  transform: translateX(2px);
  border-color: var(--cn-color-brand-primary);
  background: var(--cn-color-bg-surface-muted);
}

.event-row.is-major {
  border-color: color-mix(in srgb, var(--cn-color-warning) 38%, var(--cn-color-border-subtle));
  background: var(--cn-color-warning-soft);
}

.event-row__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
  flex-shrink: 0;
  font-size: 18px;
}

.event-row__content {
  display: grid;
  flex: 1;
  gap: var(--cn-space-1);
  min-width: 0;
}

.event-row__title {
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
}

.event-row__desc {
  display: -webkit-box;
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.calendar-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: var(--cn-space-4);
}

.month-selector {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-3);
}

.current-month {
  min-width: 112px;
  color: var(--cn-color-text-primary);
  font-size: 15px;
  font-weight: 700;
  text-align: center;
}

.calendar-grid {
  overflow: hidden;
  min-width: 0;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-panel);
  background: var(--cn-color-bg-surface);
}

.weekdays,
.days-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
}

.weekdays {
  background: var(--cn-color-bg-surface-muted);
}

.weekday {
  padding: var(--cn-space-3);
  border-right: 1px solid var(--cn-color-border-subtle);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  font-weight: 700;
  text-align: center;
}

.weekday:last-child {
  border-right: 0;
}

.day-cell {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-2);
  min-width: 0;
  min-height: 116px;
  padding: var(--cn-space-3);
  border: 0;
  border-right: 1px solid var(--cn-color-border-subtle);
  border-bottom: 1px solid var(--cn-color-border-subtle);
  background: transparent;
  color: var(--cn-color-text-primary);
  cursor: pointer;
  text-align: left;
  transition:
    background-color var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out);
}

.day-cell:nth-child(7n) {
  border-right: 0;
}

.day-cell:hover {
  background: var(--cn-color-bg-surface-muted);
}

.day-cell.is-other-month {
  color: var(--cn-color-text-tertiary);
  opacity: 0.52;
}

.day-cell.is-today {
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
}

.day-cell.has-major {
  box-shadow: inset 0 0 0 2px var(--cn-color-warning);
}

.day-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: var(--cn-radius-pill);
  font-size: 13px;
  font-weight: 800;
}

.day-cell.is-today .day-number {
  background: var(--cn-color-brand-primary);
  color: var(--cn-color-text-inverse);
}

.day-events {
  display: flex;
  align-content: flex-start;
  flex-wrap: wrap;
  gap: 5px;
  min-height: 18px;
}

.event-dot {
  width: 8px;
  height: 8px;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-text-tertiary);
}

.event-dot.event-type-1 {
  background: var(--cn-color-warning);
}

.event-dot.event-type-2 {
  background: var(--cn-color-info);
}

.event-dot.event-type-3 {
  background: var(--cn-color-success);
}

.event-dot.is-major {
  width: 12px;
  height: 12px;
  box-shadow: 0 0 0 2px var(--cn-color-bg-surface), 0 0 0 4px var(--cn-color-warning);
}

.more-events {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.event-detail {
  display: grid;
  gap: var(--cn-space-4);
  min-width: 0;
}

.event-detail__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
}

.event-detail__badges,
.event-badges {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.event-date {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.event-description {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 15px;
  line-height: 1.8;
}

.blessing-card {
  display: flex;
  align-items: flex-start;
  gap: var(--cn-space-3);
  padding: var(--cn-space-4);
  border-left: 3px solid var(--cn-color-warning);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-warning-soft);
  color: var(--cn-color-text-primary);
}

.blessing-card .el-icon {
  margin-top: 2px;
  color: var(--cn-color-warning);
  font-size: 18px;
}

.blessing-card p {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.event-links {
  display: grid;
  gap: var(--cn-space-3);
}

.event-links h4 {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 15px;
  font-weight: 700;
}

.links-list {
  display: grid;
  gap: var(--cn-space-2);
}

@media (max-width: 980px) {
  .calendar-stats {
    grid-template-columns: minmax(0, 1fr);
  }

  .today-panel {
    grid-template-columns: minmax(0, 1fr);
  }

  .date-tile {
    justify-self: start;
    width: 120px;
  }
}

@media (max-width: 768px) {
  .calendar-toolbar {
    justify-content: flex-start;
  }

  .event-filter-group {
    display: flex;
    flex-wrap: wrap;
  }

  .day-cell {
    min-height: 86px;
    padding: var(--cn-space-2);
  }

  .event-detail__header {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 560px) {
  .weekdays,
  .days-grid {
    min-width: 560px;
  }

  .calendar-grid {
    overflow-x: auto;
  }

  .today-copy__header,
  .event-row,
  .date-event-item {
    align-items: flex-start;
  }
}
</style>
