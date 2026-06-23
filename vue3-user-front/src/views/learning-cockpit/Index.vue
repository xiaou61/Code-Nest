<template>
  <CnPage class="learning-cockpit-page" surface="transparent" max-width="1440px" full-height>
      <CnPageHeader
        class="cockpit-page-header cn-wave-reveal"
        title="AI 学习成长驾驶舱 2.1"
        description="把 OJ、题库、闪卡、计划打卡、积分统一到一张看板：成长分、能力雷达、短板诊断、今日任务和推荐下一步。"
        eyebrow="Learning Cockpit 2.1"
        :breadcrumbs="breadcrumbs"
      >
        <template #meta>
          <CnStatusTag type="brand" size="sm">
            <el-icon><Calendar /></el-icon>
            {{ overview.weekRange || '--' }}
          </CnStatusTag>
          <CnStatusTag type="success" size="sm">
            <el-icon><Compass /></el-icon>
            {{ targetProfile.targetRole || '通用' }} · {{ targetProfile.weeklyHours || 0 }}h/周
          </CnStatusTag>
          <CnStatusTag type="info" size="sm">
            <el-icon><Clock /></el-icon>
            更新于 {{ overview.generatedAt || '--' }}
          </CnStatusTag>
        </template>

        <template #actions>
          <el-button plain @click="settingVisible = true">
            <el-icon><Setting /></el-icon>
            目标设置
          </el-button>
          <el-button plain :loading="loading" @click="loadOverview">
            <el-icon><Refresh /></el-icon>
            刷新看板
          </el-button>
        </template>
      </CnPageHeader>

      <CnSection class="cockpit-tabs" surface="panel">
        <el-tabs v-model="activeTab" class="cockpit-tabs-inner" @tab-change="handleTabChange">
          <el-tab-pane label="驾驶舱总览" name="overview">
            <section class="intelligence-grid">
              <article class="growth-score-card cn-learn-panel cn-learn-float">
                <div class="score-ring" :style="{ '--score': growthScore.score || 0 }">
                  <div class="score-ring-inner">
                    <strong>{{ growthScore.score || 0 }}</strong>
                    <span>{{ growthScore.level || '-' }}</span>
                  </div>
                </div>
                <div class="score-content">
                  <div class="summary-label">本周成长分</div>
                  <h3>{{ growthScore.levelText || '等待数据' }}</h3>
                  <p>{{ growthScore.trendText || '暂无成长趋势数据' }}</p>
                  <CnStatusTag :type="growthScore.qualified ? 'success' : 'warning'" size="sm">
                    {{ growthScore.qualified ? '节奏健康' : '需要校准' }}
                  </CnStatusTag>
                </div>
              </article>

              <article class="ai-review-card cn-learn-panel cn-learn-float">
                <header class="mini-panel-head">
                  <span>AI 学习复盘</span>
                  <CnStatusTag size="sm" type="brand" subtle>Growth Intelligence</CnStatusTag>
                </header>
                <h3>{{ aiReview.headline || summary.headline || '暂无复盘' }}</h3>
                <div class="review-lines">
                  <p><strong>优势</strong>{{ aiReview.strength || '等待更多学习数据生成优势判断。' }}</p>
                  <p><strong>风险</strong>{{ aiReview.risk || '暂无明显风险。' }}</p>
                  <p><strong>建议</strong>{{ aiReview.suggestion || growthScore.advice || '先完成一个关键动作。' }}</p>
                </div>
              </article>

              <article class="today-task-card cn-learn-panel cn-learn-float">
                <header class="mini-panel-head">
                  <span>今日任务闭环</span>
                  <small>{{ todayTasks.length }} 项</small>
                </header>
                <div v-if="todayTasks.length" class="today-task-list">
                  <button
                    v-for="task in todayTasks"
                    :key="`${task.priority}-${task.moduleKey}`"
                    class="today-task-item"
                    type="button"
                    @click="goRoute(task.routePath)"
                  >
                    <span class="task-priority">{{ task.priority }}</span>
                    <span class="task-main">
                      <strong>{{ task.title }}</strong>
                      <em>{{ task.estimatedMinutes || 10 }} 分钟 · {{ task.description }}</em>
                    </span>
                    <CnStatusTag size="sm" :type="task.done ? 'success' : 'info'">
                      {{ task.done ? '已达标' : '待执行' }}
                    </CnStatusTag>
                  </button>
                </div>
                <CnEmptyState v-else title="暂无今日任务" description="当前没有待执行学习动作。" icon="TODO" size="sm" surface="transparent" />
              </article>
            </section>

            <section class="diagnosis-grid">
              <article class="cn-learn-panel panel-block">
                <header class="panel-head">
                  <h3>能力雷达</h3>
                  <span class="panel-subtitle">五模块完成率结构</span>
                </header>
                <div v-if="abilityRadar.length" class="radar-list">
                  <div
                    v-for="item in abilityRadar"
                    :key="item.key"
                    class="radar-item"
                    :style="radarStyle(item)"
                  >
                    <div class="radar-meta">
                      <strong>{{ item.label }}</strong>
                      <span>{{ item.actual }} / {{ item.target }}</span>
                    </div>
                    <div class="radar-track">
                      <span></span>
                    </div>
                    <small>{{ item.description }}</small>
                  </div>
                </div>
                <CnEmptyState v-else title="暂无能力雷达数据" description="完成更多学习动作后会生成模块能力结构。" icon="RAD" size="sm" surface="transparent" />
              </article>

              <article class="cn-learn-panel panel-block">
                <header class="panel-head">
                  <h3>短板诊断</h3>
                  <span class="panel-subtitle">按影响程度排序</span>
                </header>
                <div v-if="weaknesses.length" class="weakness-list">
                  <div v-for="item in weaknesses" :key="`${item.moduleKey}-${item.title}`" class="weakness-item">
                    <div class="weakness-top">
                      <CnStatusTag size="sm" :type="severityTagType(item.severity)">
                        {{ severityText(item.severity) }}
                      </CnStatusTag>
                      <strong>{{ item.title }}</strong>
                      <span>{{ item.impactScore || 0 }}</span>
                    </div>
                    <p>{{ item.description }}</p>
                    <el-button link type="primary" @click="goRoute(item.routePath)">{{ item.actionText }}</el-button>
                  </div>
                </div>
                <CnEmptyState v-else title="暂无短板诊断" description="当前没有明显风险，继续保持稳定执行。" icon="OK" size="sm" surface="transparent" />
              </article>
            </section>

            <section class="summary-grid">
              <CnStatCard
                class="summary-card"
                title="本周总完成率"
                :value="summary.completionRate || 0"
                unit="%"
                description="跨模块周目标完成情况"
                tone="brand"
                :trend="summary.completionRate >= 80 ? 'up' : summary.completionRate >= 50 ? 'flat' : 'down'"
                :trend-text="summary.completionRate >= 80 ? '健康' : summary.completionRate >= 50 ? '推进中' : '需加速'"
              >
              </CnStatCard>
              <CnStatCard
                class="summary-card"
                title="周目标进度"
                :value="`${summary.totalCompleted || 0} / ${summary.totalTarget || 0}`"
                :description="`有效活跃天数 ${summary.activeDays || 0} 天`"
                tone="success"
                trend="flat"
                trend-text="执行"
              />
              <CnStatCard
                class="summary-card"
                title="OJ 周榜排名"
                :value="rankingDisplay.weeklyRankText"
                :description="rankingDisplay.rankDeltaText ? `${rankingDisplay.rankTrendText} · ${rankingDisplay.rankDeltaText}` : rankingDisplay.rankTrendText"
                tone="warning"
                trend="flat"
                trend-text="排名"
              />
              <article class="summary-card cn-learn-panel cn-learn-float">
                <div class="summary-label">推荐下一步</div>
                <div class="summary-action">
                  <h4>{{ topAction.title || '今日先完成1个关键动作' }}</h4>
                  <p>{{ topAction.description || summary.headline }}</p>
                  <el-button
                    v-if="topAction.routePath"
                    type="primary"
                    size="small"
                    @click="goRoute(topAction.routePath)"
                  >
                    立即执行
                  </el-button>
                </div>
              </article>
            </section>

            <section class="main-grid">
              <div class="main-left">
                <article class="cn-learn-panel panel-block">
                  <header class="panel-head">
                    <h3>周目标完成表</h3>
                    <span class="panel-subtitle">卡片 + 时间线 + 表格（信息最全）</span>
                  </header>
                  <el-table :data="moduleGoals" stripe border class="goal-table" v-loading="loading">
                    <el-table-column prop="moduleName" label="模块" min-width="120" />
                    <el-table-column label="目标" width="110" align="center">
                      <template #default="{ row }">
                        {{ row.target }} {{ row.unit }}
                      </template>
                    </el-table-column>
                    <el-table-column label="实际" width="110" align="center">
                      <template #default="{ row }">
                        {{ row.actual }} {{ row.unit }}
                      </template>
                    </el-table-column>
                    <el-table-column label="完成率" min-width="180">
                      <template #default="{ row }">
                        <div class="rate-cell">
                          <el-progress :show-text="false" :percentage="row.completionRate" :stroke-width="8" />
                          <span>{{ row.completionRate }}%</span>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column label="状态" width="100" align="center">
                      <template #default="{ row }">
                        <CnStatusTag size="sm" :type="statusTagType(row.status)">
                          {{ statusText(row.status) }}
                        </CnStatusTag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="hint" label="洞察" min-width="240" show-overflow-tooltip />
                    <el-table-column label="操作" width="120" align="center">
                      <template #default="{ row }">
                        <el-button link type="primary" @click="goRoute(row.routePath)">去完成</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </article>

                <article class="cn-learn-panel panel-block">
                  <header class="panel-head">
                    <h3>本周节奏时间线</h3>
                    <span class="panel-subtitle">按天看学习强度与打卡节奏</span>
                  </header>
                  <div class="timeline-wrap" v-if="trend.length">
                    <div class="timeline-track"></div>
                    <div v-for="item in trend" :key="item.date" class="timeline-item">
                      <div class="timeline-date">{{ formatDate(item.date) }}</div>
                      <div class="timeline-node" :style="{ '--score': item.score }">
                        <span>{{ item.score }}</span>
                      </div>
                      <div class="timeline-meta">
                        <p>题库 {{ item.interviewCount }}</p>
                        <p>闪卡 {{ item.flashcardCount }}</p>
                        <p>积分 {{ item.pointsCheckin ? '✓' : '-' }}</p>
                      </div>
                    </div>
                  </div>
                  <CnEmptyState v-else title="暂无趋势数据" description="本周学习轨迹将在数据同步后展示。" icon="TL" size="sm" surface="transparent" />
                </article>
              </div>

              <div class="main-right">
                <article class="cn-learn-panel panel-block">
                  <header class="panel-head">
                    <h3>排名变化</h3>
                    <span class="panel-subtitle">周榜 vs 总榜</span>
                  </header>
                  <div class="rank-grid">
                    <div class="rank-card">
                      <label>OJ 周榜</label>
                      <strong>{{ rankingDisplay.weeklyRankText }}</strong>
                      <small>参与人数 {{ ranking.weeklyPopulation || 0 }}</small>
                    </div>
                    <div class="rank-card">
                      <label>OJ 总榜</label>
                      <strong>{{ rankingDisplay.allRankText }}</strong>
                      <small>参与人数 {{ ranking.allPopulation || 0 }}</small>
                    </div>
                  </div>
                  <p class="rank-trend">{{ rankingDisplay.rankTrendText }}</p>
                  <div class="rank-history" v-if="rankingTrend.length">
                    <div v-for="item in rankingTrend" :key="item.weekStart" class="rank-history-item">
                      <span>{{ item.weekStart }}</span>
                      <strong>{{ item.weeklyRank ? `#${item.weeklyRank}` : '未上榜' }}</strong>
                    </div>
                  </div>
                  <p class="rank-comment">{{ ranking.comment || '保持稳定执行，排名会持续改善。' }}</p>
                </article>

                <article class="cn-learn-panel panel-block">
                  <header class="panel-head">
                    <h3>推荐下一步</h3>
                    <span class="panel-subtitle">按优先级执行</span>
                  </header>
                  <div v-if="nextActions.length" class="action-list">
                    <div v-for="action in nextActions" :key="`${action.moduleKey}-${action.priority}`" class="action-item">
                      <div class="action-priority">P{{ action.priority }}</div>
                      <div class="action-content">
                        <h4>{{ action.title }}</h4>
                        <p>{{ action.description }}</p>
                        <p v-if="action.reason" class="action-reason">策略说明：{{ action.reason }}</p>
                        <p v-if="action.expectedGain" class="action-gain">预期收益：{{ action.expectedGain }}</p>
                        <el-button link type="primary" @click="goRoute(action.routePath)">去执行</el-button>
                      </div>
                    </div>
                  </div>
                  <CnEmptyState v-else title="暂无建议" description="当前没有新的行动建议。" icon="ACT" size="sm" surface="transparent" />
                </article>

                <article class="cn-learn-panel panel-block headline-block">
                  <h4>本周提示</h4>
                  <p>{{ summary.headline || '先完成本周最低完成率模块，再冲刺排名。' }}</p>
                </article>
              </div>
            </section>
          </el-tab-pane>

          <el-tab-pane label="自动驾驶计划" name="autopilot" lazy>
            <div class="autopilot-tab-wrap">
              <GrowthAutopilotPanel />
            </div>
          </el-tab-pane>
        </el-tabs>
      </CnSection>

      <el-dialog v-model="settingVisible" title="周目标设置" width="520px">
        <el-form label-width="92px" class="target-form">
          <el-form-item label="目标模式">
            <el-radio-group v-model="targetSettings.manual">
              <el-radio :label="false">自动（推荐）</el-radio>
              <el-radio :label="true">手动覆盖</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="目标岗位" v-if="targetSettings.manual">
            <el-select
              v-model="targetSettings.targetRole"
              filterable
              allow-create
              default-first-option
              placeholder="选择或输入岗位"
              class="role-select"
            >
              <el-option v-for="item in roleOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>

          <el-form-item label="周投入时长">
            <div class="hours-wrap">
              <el-slider v-model="targetSettings.weeklyHours" :min="3" :max="40" :step="1" />
              <span class="hours-text">{{ targetSettings.weeklyHours }} h/周</span>
            </div>
          </el-form-item>

          <el-alert
            :title="targetProfile.note || '系统将根据岗位与周投入时长自动换算五模块周目标。'"
            type="info"
            :closable="false"
            show-icon
          />
        </el-form>

        <template #footer>
          <el-button @click="settingVisible = false">取消</el-button>
          <el-button @click="resetAutoSettings">恢复自动</el-button>
          <el-button type="primary" :loading="savingProfile" @click="applyTargetSettings">保存并刷新</el-button>
        </template>
      </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar, Clock, Compass, Refresh, Setting } from '@element-plus/icons-vue'
import { learningCockpitApi } from '@/api/learningCockpit'
import GrowthAutopilotPanel from '@/views/growth-autopilot/GrowthAutopilotPanel.vue'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const savingProfile = ref(false)
const settingVisible = ref(false)
const breadcrumbs = [{ label: '学习成长' }, { label: '驾驶舱' }]
const allowedTabs = ['overview', 'autopilot']
const normalizeTab = (tabName) => (allowedTabs.includes(tabName) ? tabName : 'overview')
const activeTab = ref(normalizeTab(route.query.tab))
const settingsStorageKey = 'cn_learning_cockpit_target_settings_v2'
const roleOptions = ['后端开发', '前端开发', '全栈开发', '算法工程师', '测试开发', '产品经理', '运维开发']
const targetSettings = reactive({
  manual: false,
  targetRole: '',
  weeklyHours: 8
})

const overview = ref({
  weekRange: '',
  generatedAt: '',
  targetProfile: {},
  summary: {},
  moduleGoals: [],
  ranking: {},
  trend: [],
  nextActions: [],
  growthScore: {},
  abilityRadar: [],
  weaknesses: [],
  todayTasks: [],
  aiReview: {}
})

const summary = computed(() => overview.value?.summary || {})
const targetProfile = computed(() => overview.value?.targetProfile || {})
const moduleGoals = computed(() => overview.value?.moduleGoals || [])
const ranking = computed(() => overview.value?.ranking || {})
const rankingTrend = computed(() => ranking.value?.trend || [])
const trend = computed(() => overview.value?.trend || [])
const nextActions = computed(() => overview.value?.nextActions || [])
const topAction = computed(() => nextActions.value[0] || {})
const growthScore = computed(() => overview.value?.growthScore || {})
const abilityRadar = computed(() => overview.value?.abilityRadar || [])
const weaknesses = computed(() => overview.value?.weaknesses || [])
const todayTasks = computed(() => overview.value?.todayTasks || [])
const aiReview = computed(() => overview.value?.aiReview || {})

const rankingDisplay = computed(() => {
  const weeklyRank = ranking.value.weeklyRank
  const allRank = ranking.value.allRank
  const delta = ranking.value.weeklyVsAllDelta
  const weekDelta = ranking.value.weeklyVsLastWeekDelta
  const trendText = ranking.value.trendText
  return {
    weeklyRankText: weeklyRank ? `#${weeklyRank}` : '未上榜',
    allRankText: allRank ? `#${allRank}` : '未上榜',
    rankDeltaText: Number.isFinite(delta)
      ? (delta > 0 ? `较总榜领先 ${delta} 位` : delta < 0 ? `较总榜落后 ${Math.abs(delta)} 位` : '与总榜持平')
      : '',
    rankTrendText: trendText || (Number.isFinite(weekDelta)
      ? (weekDelta > 0 ? `较上周上升 ${weekDelta} 位` : weekDelta < 0 ? `较上周下降 ${Math.abs(weekDelta)} 位` : '较上周持平')
      : '暂无上周基线')
  }
})

const statusText = (status) => {
  const map = {
    done: '已达标',
    on_track: '正常',
    warning: '预警',
    behind: '落后'
  }
  return map[status] || '未知'
}

const statusTagType = (status) => {
  const map = {
    done: 'success',
    on_track: 'brand',
    warning: 'warning',
    behind: 'danger'
  }
  return map[status] || 'info'
}

const severityText = (severity) => {
  const map = {
    HIGH: '高风险',
    MEDIUM: '中风险',
    LOW: '低风险'
  }
  return map[severity] || '待观察'
}

const severityTagType = (severity) => {
  const map = {
    HIGH: 'danger',
    MEDIUM: 'warning',
    LOW: 'success'
  }
  return map[severity] || 'info'
}

const radarStyle = (item = {}) => ({
  '--radar-score': `${Math.max(0, Math.min(100, Number(item.score) || 0))}%`,
  '--radar-color': item.color || 'var(--cn-color-brand-primary)'
})

const formatDate = (text) => {
  if (!text || text.length < 10) return text || '--'
  const [, month, day] = text.split('-')
  return `${month}/${day}`
}

const handleTabChange = (tabName) => {
  const nextTab = normalizeTab(tabName)
  if (nextTab !== activeTab.value) {
    activeTab.value = nextTab
  }
  const nextQuery = { ...route.query }
  if (nextTab === 'overview') {
    delete nextQuery.tab
  } else {
    nextQuery.tab = nextTab
  }
  const currentTab = route.query.tab
  const queryMatched = nextTab === 'overview' ? !currentTab : currentTab === nextTab
  if (queryMatched) {
    return
  }
  router.replace({ path: route.path, query: nextQuery })
}

const goRoute = (path) => {
  if (!path) return
  if (typeof path === 'string' && path.startsWith('/growth-autopilot')) {
    router.push({ path: '/learning-cockpit', query: { tab: 'autopilot' } })
    return
  }
  router.push(path)
}

const clampWeeklyHours = (value) => Math.max(3, Math.min(40, Number(value) || 8))

const applyProfileToSettings = (profile = {}) => {
  targetSettings.targetRole = profile?.targetRole || ''
  targetSettings.weeklyHours = clampWeeklyHours(profile?.weeklyHours)
}

const loadLocalSettings = () => {
  try {
    const raw = localStorage.getItem(settingsStorageKey)
    if (!raw) return
    const parsed = JSON.parse(raw)
    targetSettings.manual = Boolean(parsed?.manual)
  } catch (error) {
    targetSettings.manual = false
  }
}

const persistLocalSettings = () => {
  localStorage.setItem(settingsStorageKey, JSON.stringify({
    manual: targetSettings.manual
  }))
}

const resetAutoSettings = () => {
  targetSettings.manual = false
  persistLocalSettings()
  applyProfileToSettings(overview.value?.targetProfile || {})
  settingVisible.value = false
}

const applyTargetSettings = async () => {
  if (targetSettings.manual && !targetSettings.targetRole) {
    ElMessage.warning('手动模式下请先选择或输入目标岗位')
    return
  }
  const payload = {
    weeklyHours: clampWeeklyHours(targetSettings.weeklyHours)
  }
  if (targetSettings.manual) {
    payload.targetRole = targetSettings.targetRole
  }

  savingProfile.value = true
  try {
    await learningCockpitApi.saveTargetProfile(payload)
    persistLocalSettings()
    settingVisible.value = false
    ElMessage.success('目标设置已保存到后端会话')
    await loadOverview()
  } catch (error) {
    console.error('保存目标设置失败', error)
    ElMessage.error('保存目标设置失败，请稍后重试')
  } finally {
    savingProfile.value = false
  }
}

const loadOverview = async () => {
  loading.value = true
  try {
    const data = await learningCockpitApi.getOverview()
    overview.value = data || {
      targetProfile: {},
      summary: {},
      moduleGoals: [],
      ranking: {},
      trend: [],
      nextActions: [],
      growthScore: {},
      abilityRadar: [],
      weaknesses: [],
      todayTasks: [],
      aiReview: {}
    }
    applyProfileToSettings(overview.value?.targetProfile || {})
  } catch (error) {
    console.error('加载学习成长驾驶舱失败', error)
    ElMessage.error('加载学习成长驾驶舱失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadLocalSettings()
  loadOverview()
})

watch(
  () => route.query.tab,
  (tabName) => {
    const nextTab = normalizeTab(tabName)
    if (activeTab.value !== nextTab) {
      activeTab.value = nextTab
    }
  }
)
</script>

<style scoped>
.learning-cockpit-page {
  min-height: calc(100vh - 68px);
  overflow-x: clip;
  --cockpit-panel-border: var(--cn-color-border-subtle);
  --cockpit-panel-bg: var(--cn-color-bg-surface-muted);
  --cockpit-panel-bg-strong: color-mix(in srgb, var(--cn-color-brand-soft) 26%, var(--cn-color-bg-surface));
  --cockpit-text-strong: var(--cn-color-text-primary);
  --cockpit-text-body: var(--cn-color-text-secondary);
  --cockpit-text-muted: var(--cn-color-text-tertiary);
  --cockpit-accent: var(--cn-color-brand-primary);
  --cockpit-accent-soft: var(--cn-color-brand-soft);
  --cockpit-on-accent: white;
}

.cockpit-tabs {
  padding: 10px 14px 14px;
  min-width: 0;
}

:deep(.cockpit-tabs-inner),
:deep(.cockpit-tabs-inner > .el-tabs__content),
:deep(.cockpit-tabs-inner > .el-tabs__content > .el-tab-pane) {
  min-width: 0;
}

:deep(.cockpit-tabs-inner > .el-tabs__header) {
  margin-bottom: 14px;
}

:deep(.cockpit-tabs-inner > .el-tabs__header .el-tabs__nav-wrap::after) {
  background-color: var(--cockpit-panel-border);
}

:deep(.cockpit-tabs-inner .el-tabs__item) {
  height: 40px;
  font-size: 14px;
  font-weight: 600;
  color: var(--cockpit-text-body);
}

:deep(.cockpit-tabs-inner .el-tabs__item.is-active) {
  color: var(--cockpit-accent);
}

.autopilot-tab-wrap {
  padding-top: 4px;
}

.intelligence-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.15fr) minmax(0, 1.25fr);
  gap: 12px;
  margin-bottom: 12px;
}

.intelligence-grid,
.diagnosis-grid,
.summary-grid,
.main-grid,
.main-left,
.main-right,
.panel-block,
.summary-card,
.cn-learn-panel {
  min-width: 0;
}

.growth-score-card {
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.score-ring {
  --score: 0;
  width: 118px;
  height: 118px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: conic-gradient(var(--cockpit-accent) calc(var(--score) * 1%), var(--cockpit-accent-soft) 0);
  box-shadow:
    inset 0 0 0 1px var(--cockpit-panel-border),
    0 14px 28px color-mix(in srgb, var(--cockpit-accent) 16%, transparent);
}

.score-ring-inner {
  width: 86px;
  height: 86px;
  border-radius: 50%;
  background: var(--cn-color-bg-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  color: var(--cockpit-accent);
}

.score-ring-inner strong {
  font-size: 30px;
  line-height: 1;
}

.score-ring-inner span {
  margin-top: 4px;
  font-size: 13px;
  font-weight: 700;
}

.score-content h3,
.ai-review-card h3 {
  margin: 0 0 8px;
  color: var(--cockpit-text-strong);
  font-size: 18px;
}

.score-content p {
  margin: 0 0 10px;
  color: var(--cockpit-text-body);
  line-height: 1.7;
  font-size: 13px;
}

.ai-review-card,
.today-task-card {
  padding: 16px;
}

.mini-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
  color: var(--cockpit-text-muted);
  font-size: 13px;
  font-weight: 700;
}

.mini-panel-head small {
  color: var(--cockpit-text-muted);
  font-size: 12px;
}

.review-lines {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.review-lines p {
  margin: 0;
  color: var(--cockpit-text-body);
  font-size: 13px;
  line-height: 1.65;
}

.review-lines strong {
  display: inline-flex;
  min-width: 42px;
  margin-right: 8px;
  color: var(--cockpit-accent);
}

.today-task-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.today-task-item {
  width: 100%;
  border: 1px solid var(--cockpit-panel-border);
  border-radius: 8px;
  background: var(--cockpit-panel-bg);
  padding: 9px;
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr) auto;
  align-items: center;
  gap: 9px;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.today-task-item:hover {
  border-color: var(--cockpit-accent);
  box-shadow: 0 10px 24px color-mix(in srgb, var(--cockpit-accent) 12%, transparent);
  transform: translateY(-1px);
}

.task-priority {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--cockpit-on-accent);
  font-weight: 700;
  background: var(--cockpit-accent);
}

.task-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.task-main strong {
  color: var(--cockpit-text-strong);
  font-size: 13px;
}

.task-main em {
  color: var(--cockpit-text-body);
  font-size: 12px;
  font-style: normal;
  line-height: 1.45;
}

.diagnosis-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
  gap: 12px;
  margin-bottom: 12px;
}

.radar-list {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.radar-item {
  border: 1px solid var(--cockpit-panel-border);
  border-radius: 10px;
  background: var(--cockpit-panel-bg);
  padding: 10px;
  min-width: 0;
}

.radar-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.radar-meta strong {
  color: var(--cockpit-text-strong);
  font-size: 13px;
}

.radar-meta span {
  color: var(--cockpit-text-muted);
  font-size: 12px;
}

.radar-track {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--cockpit-accent-soft);
}

.radar-track span {
  display: block;
  width: var(--radar-score);
  height: 100%;
  border-radius: inherit;
  background: var(--radar-color);
}

.radar-item small {
  display: block;
  margin-top: 8px;
  color: var(--cockpit-text-body);
  font-size: 12px;
  line-height: 1.55;
}

.weakness-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.weakness-item {
  border: 1px solid var(--cockpit-panel-border);
  border-radius: 10px;
  background: var(--cockpit-panel-bg);
  padding: 10px;
}

.weakness-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.weakness-top strong {
  min-width: 0;
  flex: 1;
  color: var(--cockpit-text-strong);
  font-size: 13px;
}

.weakness-top span {
  color: var(--cockpit-accent);
  font-weight: 700;
  font-size: 13px;
}

.weakness-item p {
  margin: 8px 0 4px;
  color: var(--cockpit-text-body);
  font-size: 13px;
  line-height: 1.65;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.summary-card {
  padding: 16px;
}

.summary-label {
  font-size: 12px;
  color: var(--cockpit-text-muted);
  margin-bottom: 8px;
}

.summary-action h4 {
  margin: 0 0 6px;
  font-size: 15px;
  color: var(--cockpit-text-strong);
}

.summary-action p {
  margin: 0 0 8px;
  color: var(--cockpit-text-body);
  font-size: 13px;
  line-height: 1.6;
}

.main-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(280px, 1fr);
  gap: 12px;
}

.main-left,
.main-right {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel-block {
  padding: 14px;
  overflow: hidden;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.panel-head h3 {
  margin: 0;
  color: var(--cockpit-text-strong);
  font-size: 17px;
}

.panel-subtitle {
  color: var(--cockpit-text-muted);
  font-size: 12px;
}

.rate-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rate-cell .el-progress {
  flex: 1;
}

.rate-cell span {
  width: 48px;
  text-align: right;
  font-size: 12px;
  color: var(--cockpit-text-body);
}

.goal-table {
  width: 100%;
}

:deep(.goal-table .el-table__inner-wrapper),
:deep(.goal-table .el-scrollbar__wrap) {
  min-width: 0;
}

:deep(.goal-table .el-scrollbar__view) {
  min-width: 900px;
}

.timeline-wrap {
  position: relative;
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
}

.timeline-track {
  position: absolute;
  left: 6%;
  right: 6%;
  top: 42px;
  height: 2px;
  background: var(--cockpit-accent);
}

.timeline-item {
  position: relative;
  text-align: center;
  z-index: 1;
}

.timeline-date {
  font-size: 12px;
  color: var(--cockpit-text-muted);
  margin-bottom: 10px;
}

.timeline-node {
  --node-color: color-mix(in srgb, var(--cn-color-success) calc(var(--score) * 1%), var(--cn-color-warning));
  margin: 0 auto 10px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--node-color);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--cockpit-on-accent);
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 10px 22px color-mix(in srgb, var(--cockpit-accent) 24%, transparent);
}

.timeline-meta {
  padding: 8px 6px;
  border-radius: 8px;
  border: 1px solid var(--cockpit-panel-border);
  background: var(--cockpit-panel-bg);
}

.timeline-meta p {
  margin: 0;
  line-height: 1.5;
  font-size: 12px;
  color: var(--cockpit-text-body);
}

.rank-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.rank-card {
  border-radius: 10px;
  border: 1px solid var(--cockpit-panel-border);
  padding: 12px;
  background: var(--cockpit-panel-bg-strong);
}

.rank-card label {
  display: block;
  color: var(--cockpit-text-muted);
  font-size: 12px;
  margin-bottom: 8px;
}

.rank-card strong {
  font-size: 24px;
  color: var(--cockpit-accent);
}

.rank-card small {
  display: block;
  margin-top: 6px;
  color: var(--cockpit-text-muted);
  font-size: 12px;
}

.rank-comment {
  margin: 10px 0 0;
  color: var(--cockpit-text-body);
  line-height: 1.7;
  font-size: 13px;
}

.rank-trend {
  margin: 10px 0 0;
  color: var(--cockpit-accent);
  font-size: 13px;
  font-weight: 600;
}

.rank-history {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.rank-history-item {
  border: 1px solid var(--cockpit-panel-border);
  border-radius: 8px;
  background: var(--cockpit-panel-bg);
  padding: 6px 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.rank-history-item span {
  font-size: 12px;
  color: var(--cockpit-text-muted);
}

.rank-history-item strong {
  font-size: 13px;
  color: var(--cockpit-text-strong);
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.action-item {
  display: flex;
  gap: 10px;
  border-radius: 10px;
  border: 1px solid var(--cockpit-panel-border);
  background: var(--cockpit-panel-bg);
  padding: 10px;
}

.action-priority {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--cockpit-on-accent);
  font-weight: 700;
  background: var(--cockpit-accent);
}

.action-content h4 {
  margin: 0 0 4px;
  font-size: 14px;
  color: var(--cockpit-text-strong);
}

.action-content p {
  margin: 0;
  font-size: 13px;
  color: var(--cockpit-text-body);
  line-height: 1.65;
}

.action-reason {
  margin-top: 6px !important;
  color: var(--cockpit-accent) !important;
  font-size: 12px !important;
}

.action-gain {
  margin-top: 2px !important;
  color: var(--cn-color-success) !important;
  font-size: 12px !important;
  font-weight: 600;
}

.headline-block h4 {
  margin: 0 0 8px;
  color: var(--cockpit-text-strong);
}

.headline-block p {
  margin: 0;
  color: var(--cockpit-text-body);
  line-height: 1.8;
}

.target-form {
  padding: 4px 6px 0;
}

.role-select {
  width: 100%;
}

.hours-wrap {
  width: 100%;
}

.hours-text {
  display: inline-block;
  margin-top: 4px;
  color: var(--cockpit-text-body);
  font-size: 13px;
  font-weight: 600;
}

@media (max-width: 1200px) {
  .intelligence-grid,
  .diagnosis-grid {
    grid-template-columns: 1fr;
  }

  .radar-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .main-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .growth-score-card {
    align-items: flex-start;
    flex-direction: column;
  }

  .today-task-item {
    grid-template-columns: 30px minmax(0, 1fr);
  }

  .today-task-item .cn-status-tag {
    grid-column: 2;
    justify-self: flex-start;
  }

  .radar-list,
  .weakness-list {
    grid-template-columns: 1fr;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .timeline-wrap {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .timeline-track {
    display: none;
  }

  .rank-grid {
    grid-template-columns: 1fr;
  }

  .rank-history {
    grid-template-columns: 1fr;
  }
}
</style>
