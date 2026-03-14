<template>
  <div class="career-loop-page cn-learn-shell">
    <div class="cn-learn-shell__inner">
      <div class="page-header cn-learn-hero cn-wave-reveal">
        <div class="header-content">
          <span class="cn-learn-hero__eyebrow">Career Loop</span>
          <h1 class="page-title cn-learn-hero__title">
            <el-icon><Connection /></el-icon>
            求职闭环中台
          </h1>
          <p class="page-subtitle cn-learn-hero__desc">
            把 JD 解析、计划执行、模拟面试和复盘统一到一条可追踪主线
          </p>
        </div>
        <div class="header-actions">
          <el-button plain :loading="loading.sync" @click="handleSync">手动同步</el-button>
          <el-button type="primary" :loading="loading.start" @click="handleStart">重启闭环会话</el-button>
        </div>
      </div>

      <el-row :gutter="12" class="summary-row">
        <el-col :xs="12" :sm="12" :md="6">
          <el-card shadow="never" class="summary-card">
            <div class="summary-label">当前阶段</div>
            <div class="summary-value">{{ current.session.currentStageLabel }}</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6">
          <el-card shadow="never" class="summary-card">
            <div class="summary-label">阶段进度</div>
            <div class="summary-value">{{ current.session.stagePercent }}%</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6">
          <el-card shadow="never" class="summary-card">
            <div class="summary-label">健康分</div>
            <div class="summary-value">{{ current.session.healthScore }}</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6">
          <el-card shadow="never" class="summary-card">
            <div class="summary-label">计划进度</div>
            <div class="summary-value">{{ current.snapshot.planProgress }}%</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="12" class="viz-row">
        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-title">阶段能力雷达</div>
            </template>
            <div class="radar-wrap">
              <svg
                class="radar-svg"
                viewBox="0 0 320 320"
                role="img"
                aria-label="求职闭环阶段能力雷达图"
              >
                <polygon
                  v-for="(polygon, index) in radarGridPolygons"
                  :key="`grid-${index}`"
                  :points="pointsToString(polygon)"
                  class="radar-grid"
                />
                <line
                  v-for="(axis, index) in radarAxes"
                  :key="`axis-${index}`"
                  :x1="radarCenter.x"
                  :y1="radarCenter.y"
                  :x2="axis.point.x"
                  :y2="axis.point.y"
                  class="radar-axis"
                />
                <polygon :points="pointsToString(radarValuePolygon)" class="radar-value" />
                <circle
                  v-for="(point, index) in radarValuePolygon"
                  :key="`point-${index}`"
                  :cx="point.x"
                  :cy="point.y"
                  r="3.5"
                  class="radar-point"
                />
                <text
                  v-for="(axis, index) in radarAxes"
                  :key="`label-${index}`"
                  :x="axis.labelPoint.x"
                  :y="axis.labelPoint.y"
                  class="radar-label"
                >
                  {{ axis.label }}
                </text>
              </svg>
              <div class="radar-legend">
                <div
                  v-for="item in radarIndicators"
                  :key="item.key"
                  class="radar-legend-item"
                >
                  <span>{{ item.label }}</span>
                  <b>{{ item.value }}</b>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-title">近4周进度热力图</div>
            </template>
            <div class="heatmap-head">
              <span v-for="day in heatmapDayLabels" :key="day">{{ day }}</span>
            </div>
            <div class="heatmap-grid">
              <div
                v-for="cell in heatmapCells"
                :key="cell.dateKey"
                class="heatmap-cell"
                :class="[`level-${cell.level}`, { today: cell.isToday }]"
                :title="`${cell.dateLabel} 活跃度 ${cell.level}/4`"
              />
            </div>
            <div class="heatmap-legend">
              <span>低</span>
              <span class="legend-dot level-1" />
              <span class="legend-dot level-2" />
              <span class="legend-dot level-3" />
              <span class="legend-dot level-4" />
              <span>高</span>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="12" class="content-row">
        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-title">阶段时间线</div>
            </template>
            <el-timeline v-if="timeline.length">
              <el-timeline-item
                v-for="item in timeline"
                :key="item.id"
                :timestamp="item.createTime || ''"
                type="primary"
              >
                <div class="timeline-title">{{ mapStageLabel(item.fromStage) }} -> {{ mapStageLabel(item.toStage) }}</div>
                <div class="timeline-meta">来源：{{ item.triggerSource || '-' }}{{ item.triggerRefId ? ` / ${item.triggerRefId}` : '' }}</div>
                <div class="timeline-note">{{ item.note || '无备注' }}</div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无阶段推进记录" :image-size="80" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="panel-card">
            <template #header>
              <div class="panel-title">风险与建议</div>
            </template>
            <div class="section-block">
              <div class="section-subtitle">风险项</div>
              <div v-if="current.riskFlags.length" class="tag-list">
                <el-tag
                  v-for="(risk, index) in current.riskFlags"
                  :key="`risk-${index}`"
                  type="danger"
                  effect="plain"
                >
                  {{ risk }}
                </el-tag>
              </div>
              <el-empty v-else description="暂无风险项" :image-size="60" />
            </div>

            <div class="section-block">
              <div class="section-subtitle">下一步建议</div>
              <div v-if="current.nextSuggestions.length" class="suggestion-list">
                <div v-for="(item, index) in current.nextSuggestions" :key="`suggestion-${index}`" class="suggestion-item">
                  {{ index + 1 }}. {{ item }}
                </div>
              </div>
              <el-empty v-else description="暂无建议" :image-size="60" />
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="panel-card coach-card">
        <template #header>
          <div class="panel-title">AI教练摘要</div>
        </template>
        <CoachWorkspace compact scene="career" />
      </el-card>

      <el-card shadow="never" class="panel-card">
        <template #header>
          <div class="panel-title">动作清单</div>
        </template>
        <el-table :data="actions" stripe border v-loading="loading.main">
          <el-table-column prop="title" label="动作" min-width="220" show-overflow-tooltip />
          <el-table-column prop="description" label="说明" min-width="260" show-overflow-tooltip />
          <el-table-column prop="stage" label="阶段" width="180">
            <template #default="{ row }">
              {{ mapStageLabel(row.stage) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="mapActionStatusTag(row.status)">{{ mapActionStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="goByActionType(row.actionType)">去执行</el-button>
              <el-button
                link
                type="success"
                :disabled="row.status === 'done'"
                @click="handleDoneAction(row)"
              >
                标记完成
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Connection } from '@element-plus/icons-vue'
import { careerLoopApi } from '@/api/careerLoop'
import CoachWorkspace from '@/components/ai-growth-coach/CoachWorkspace.vue'
import {
  adaptCareerLoopCurrent,
  mapActionStatusLabel,
  mapActionStatusTag,
  mapStageLabel
} from '@/utils/career-loop-adapter'

const router = useRouter()

const loading = reactive({
  main: false,
  start: false,
  sync: false
})

const current = ref(adaptCareerLoopCurrent())
const timeline = ref([])
const actions = ref([])
const heatmapDayLabels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const radarCenter = { x: 160, y: 160 }
const radarRadius = 104

const fetchAll = async () => {
  loading.main = true
  try {
    const [currentData, timelineData, actionData] = await Promise.all([
      careerLoopApi.getCurrent(),
      careerLoopApi.getTimeline(),
      careerLoopApi.getActions()
    ])
    current.value = adaptCareerLoopCurrent(currentData || {})
    timeline.value = Array.isArray(timelineData) ? timelineData : []
    actions.value = Array.isArray(actionData) ? actionData : []
  } catch (e) {
    console.error('加载求职闭环数据失败', e)
    ElMessage.error('加载求职闭环数据失败')
  } finally {
    loading.main = false
  }
}

const handleStart = async () => {
  loading.start = true
  try {
    await careerLoopApi.start({
      targetRole: current.value.session.targetRole || '',
      targetCompanyType: current.value.session.targetCompanyType || ''
    })
    ElMessage.success('闭环会话已重启')
    await fetchAll()
  } catch (e) {
    console.error('重启闭环会话失败', e)
    ElMessage.error('重启闭环会话失败')
  } finally {
    loading.start = false
  }
}

const handleSync = async () => {
  loading.sync = true
  try {
    await careerLoopApi.sync({})
    ElMessage.success('闭环状态已同步')
    await fetchAll()
  } catch (e) {
    console.error('同步闭环状态失败', e)
    ElMessage.error('同步闭环状态失败')
  } finally {
    loading.sync = false
  }
}

const handleDoneAction = async (row) => {
  try {
    await careerLoopApi.completeAction(row.id)
    ElMessage.success('动作已标记完成')
    await fetchAll()
  } catch (e) {
    console.error('标记动作完成失败', e)
    ElMessage.error('标记动作完成失败')
  }
}

const goByActionType = (actionType) => {
  const routeMap = {
    setup: { path: '/job-battle', query: { step: '0', from: 'career-loop', action: 'setup' } },
    resume: { path: '/job-battle', query: { step: '1', from: 'career-loop', action: 'resume' } },
    plan: { path: '/job-battle', query: { step: '2', from: 'career-loop', action: 'plan' } },
    study: { path: '/plan' },
    mock: { path: '/mock-interview/config' },
    review: { path: '/job-battle', query: { step: '3', from: 'career-loop', action: 'review' } }
  }
  router.push(routeMap[actionType] || { path: '/job-battle' })
}

const toPercent = (value, fallback = 0) => {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) {
    return fallback
  }
  return Math.max(0, Math.min(100, Math.round(numberValue)))
}

const clamp = (value, min, max) => Math.max(min, Math.min(max, value))

const radarIndicators = computed(() => {
  const doneCount = actions.value.filter(item => item.status === 'done').length
  const totalCount = actions.value.length || 1
  const doneRate = Math.round((doneCount / totalCount) * 100)
  const mockCount = Number(current.value.snapshot.mockCount || 0)
  const reviewCount = Number(current.value.snapshot.reviewCount || 0)
  const latestMockScore = Number(current.value.snapshot.latestMockScore || 0)
  const riskCount = current.value.riskFlags.length

  return [
    { key: 'match', label: '岗位匹配', value: toPercent(current.value.session.stagePercent) },
    { key: 'execute', label: '执行节奏', value: toPercent(Math.max(doneRate, current.value.snapshot.planProgress || 0)) },
    { key: 'mock', label: '面试实战', value: toPercent(mockCount * 18 + latestMockScore * 0.45) },
    { key: 'review', label: '复盘沉淀', value: toPercent(reviewCount * 34) },
    { key: 'health', label: '健康稳定', value: toPercent(current.value.session.healthScore, 60) },
    { key: 'risk', label: '风险控制', value: toPercent(100 - riskCount * 18, 100) }
  ]
})

const radarAxes = computed(() => {
  const count = radarIndicators.value.length
  return radarIndicators.value.map((item, index) => {
    const angle = -Math.PI / 2 + (Math.PI * 2 * index) / count
    const point = {
      x: radarCenter.x + Math.cos(angle) * radarRadius,
      y: radarCenter.y + Math.sin(angle) * radarRadius
    }
    const labelPoint = {
      x: radarCenter.x + Math.cos(angle) * (radarRadius + 24),
      y: radarCenter.y + Math.sin(angle) * (radarRadius + 24)
    }
    return { ...item, point, labelPoint, angle }
  })
})

const radarGridPolygons = computed(() => {
  const levels = [0.25, 0.5, 0.75, 1]
  return levels.map(level => radarAxes.value.map(axis => ({
    x: radarCenter.x + Math.cos(axis.angle) * radarRadius * level,
    y: radarCenter.y + Math.sin(axis.angle) * radarRadius * level
  })))
})

const radarValuePolygon = computed(() => {
  return radarAxes.value.map(axis => {
    const ratio = clamp(axis.value / 100, 0, 1)
    return {
      x: radarCenter.x + Math.cos(axis.angle) * radarRadius * ratio,
      y: radarCenter.y + Math.sin(axis.angle) * radarRadius * ratio
    }
  })
})

const pointsToString = (points) => points.map(point => `${point.x},${point.y}`).join(' ')

const toDateKey = (dateValue) => {
  const parsedValue = typeof dateValue === 'string' ? dateValue.replace(' ', 'T') : dateValue
  const date = new Date(parsedValue)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const addHeat = (map, key, delta) => {
  if (!key) {
    return
  }
  const currentValue = map.get(key) || 0
  map.set(key, currentValue + delta)
}

const heatmapCells = computed(() => {
  const today = new Date()
  const dates = []
  for (let offset = 27; offset >= 0; offset -= 1) {
    const date = new Date(today)
    date.setDate(today.getDate() - offset)
    dates.push(date)
  }

  const intensityMap = new Map()
  timeline.value.forEach(item => {
    addHeat(intensityMap, toDateKey(item.createTime), 2)
  })
  actions.value.forEach(item => {
    if (item.status === 'done') {
      addHeat(intensityMap, toDateKey(item.updateTime), 1)
    }
  })

  const planProgress = toPercent(current.value.snapshot.planProgress || 0)
  const mockCount = Number(current.value.snapshot.mockCount || 0)
  const reviewCount = Number(current.value.snapshot.reviewCount || 0)
  const recentActiveDays = Math.max(0, Math.round((planProgress / 100) * 28))
  for (let i = 0; i < recentActiveDays; i += 1) {
    const key = toDateKey(dates[dates.length - 1 - i])
    addHeat(intensityMap, key, i < 7 ? 2 : 1)
  }
  for (let i = 0; i < mockCount; i += 1) {
    const key = toDateKey(dates[Math.max(0, dates.length - 1 - i * 3)])
    addHeat(intensityMap, key, 2)
  }
  for (let i = 0; i < reviewCount; i += 1) {
    const key = toDateKey(dates[Math.max(0, dates.length - 1 - i * 5)])
    addHeat(intensityMap, key, 1)
  }

  return dates.map(date => {
    const dateKey = toDateKey(date)
    const score = intensityMap.get(dateKey) || 0
    const level = clamp(score, 0, 4)
    return {
      dateKey,
      dateLabel: `${date.getMonth() + 1}/${date.getDate()}`,
      level,
      isToday: dateKey === toDateKey(today)
    }
  })
})

onMounted(async () => {
  await fetchAll()
})
</script>

<style scoped>
.career-loop-page {
  min-height: calc(100vh - 68px);
}

.page-header {
  margin-bottom: 14px;
  border-radius: 18px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-title {
  margin: 0 0 10px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-subtitle {
  margin: 0;
  opacity: 0.88;
}

.summary-row,
.viz-row,
.content-row {
  margin-bottom: 12px;
}

.summary-card,
.panel-card {
  border-radius: 12px;
}

.coach-card {
  margin-bottom: 12px;
}

.summary-label {
  color: #7a8aa6;
  font-size: 12px;
  margin-bottom: 6px;
}

.summary-value {
  color: #1f2d3d;
  font-size: 18px;
  font-weight: 600;
}

.panel-title {
  color: #243b53;
  font-weight: 600;
}

.radar-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.radar-svg {
  width: 320px;
  max-width: 100%;
  height: auto;
  flex-shrink: 0;
}

.radar-grid {
  fill: none;
  stroke: #dbe8fb;
  stroke-width: 1;
}

.radar-axis {
  stroke: #d3e2f8;
  stroke-width: 1;
}

.radar-value {
  fill: rgba(49, 130, 206, 0.2);
  stroke: #2b6cb0;
  stroke-width: 2;
}

.radar-point {
  fill: #2b6cb0;
}

.radar-label {
  fill: #486581;
  font-size: 11px;
  text-anchor: middle;
  dominant-baseline: middle;
}

.radar-legend {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.radar-legend-item {
  border: 1px solid #dce8fb;
  border-radius: 8px;
  padding: 8px 10px;
  background: #f8fbff;
  color: #334e68;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}

.heatmap-head {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
  margin-bottom: 8px;
  color: #7a8aa6;
  font-size: 12px;
  text-align: center;
}

.heatmap-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 6px;
}

.heatmap-cell {
  height: 22px;
  border-radius: 6px;
  border: 1px solid #dfe8f7;
  background: #f3f6fb;
}

.heatmap-cell.level-1 {
  background: #d9f1df;
  border-color: #bee4ca;
}

.heatmap-cell.level-2 {
  background: #a9e4bb;
  border-color: #8dd7a5;
}

.heatmap-cell.level-3 {
  background: #69c98c;
  border-color: #54b979;
}

.heatmap-cell.level-4 {
  background: #2f9e66;
  border-color: #258a58;
}

.heatmap-cell.today {
  box-shadow: inset 0 0 0 2px #1f6feb;
}

.heatmap-legend {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6b7f99;
  font-size: 12px;
}

.legend-dot {
  width: 16px;
  height: 10px;
  border-radius: 5px;
  display: inline-block;
}

.legend-dot.level-1 {
  background: #d9f1df;
}

.legend-dot.level-2 {
  background: #a9e4bb;
}

.legend-dot.level-3 {
  background: #69c98c;
}

.legend-dot.level-4 {
  background: #2f9e66;
}

.timeline-title {
  color: #1f2d3d;
  font-weight: 600;
  margin-bottom: 3px;
}

.timeline-meta {
  color: #7a8aa6;
  font-size: 12px;
  margin-bottom: 3px;
}

.timeline-note {
  color: #425673;
  font-size: 13px;
}

.section-block + .section-block {
  margin-top: 14px;
}

.section-subtitle {
  margin-bottom: 8px;
  color: #425673;
  font-weight: 600;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.suggestion-item {
  border: 1px solid #dce8fb;
  background: #f7fbff;
  border-radius: 8px;
  padding: 8px 10px;
  color: #425673;
  line-height: 1.6;
}

@media (max-width: 992px) {
  .page-header {
    flex-direction: column;
  }

  .radar-wrap {
    flex-direction: column;
    align-items: stretch;
  }

  .radar-svg {
    width: 100%;
  }

  .radar-legend {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
