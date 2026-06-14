<template>
  <CnPage class="salary-calculator-page" max-width="1280px" full-height>
    <CnPageHeader
      title="打工人时薪计算器"
      description="配置薪资和工作节奏后，实时记录今日收入、工作时长和周期统计。"
      eyebrow="MOYU TOOL"
      :breadcrumbs="[{ label: '摸鱼工具箱', to: '/moyu-tools' }, { label: '时薪计算器' }]"
    >
      <template #meta>
        <CnStatusTag :type="hasConfig ? getWorkStatusTone(calculatorData.workStatus) : 'info'" size="sm">
          {{ hasConfig ? getWorkStatusText() : '未配置' }}
        </CnStatusTag>
        <CnStatusTag v-if="hasConfig" type="success" size="sm" subtle>
          时薪 ¥{{ formatNumber(calculatorData.hourlyRate) }}
        </CnStatusTag>
        <CnStatusTag v-if="calculatorData.todayStartTime" type="neutral" size="sm" subtle>
          {{ formatDateTime(calculatorData.todayStartTime) }} 开始
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="ArrowLeft" @click="goBack">返回工具箱</el-button>
        <el-button v-if="hasConfig" type="primary" :icon="Edit" @click="openConfigDialog">
          编辑薪资
        </el-button>
      </template>
    </CnPageHeader>

    <CnSection v-if="loading" title="正在加载" description="正在读取薪资配置和今日工作记录。" divided>
      <el-skeleton :rows="8" animated />
    </CnSection>

    <CnSection v-else-if="!hasConfig" class="config-section" surface="panel">
      <CnEmptyState
        title="首次使用需要配置薪资信息"
        description="请先配置月薪、每月工作天数和每日工作小时，以便准确计算实时收入。"
        icon="PAY"
        size="lg"
        surface="transparent"
      >
        <template #actions>
          <el-button type="primary" size="large" :icon="Plus" @click="openConfigDialog">
            立即配置
          </el-button>
        </template>
      </CnEmptyState>
    </CnSection>

    <template v-else>
      <section class="stats-grid" aria-label="薪资概览">
        <CnStatCard
          title="月薪"
          :value="`¥${formatNumber(calculatorData.monthlySalary)}`"
          description="当前薪资配置"
          tone="brand"
          trend="flat"
          trend-text="配置"
        />
        <CnStatCard
          title="时薪"
          :value="`¥${formatNumber(calculatorData.hourlyRate)}`"
          description="按工作天数和小时折算"
          tone="success"
          trend="flat"
          trend-text="实时"
        />
        <CnStatCard
          title="今日收入"
          :value="`¥${formatNumber(displayEarnings)}`"
          :description="`已工作 ${formatNumber(displayWorkHours)} 小时`"
          :tone="getWorkStatusTone(calculatorData.workStatus)"
          trend="flat"
          :trend-text="getWorkStatusText()"
        />
        <CnStatCard
          title="今日进度"
          :value="`${formatNumber(workProgress)}%`"
          :description="`目标 ${formatNumber(calculatorData.workHoursPerDay)} 小时`"
          tone="info"
          trend="flat"
          trend-text="工时"
        />
      </section>

      <CnSection title="今日工作控制" description="开始、暂停、恢复或结束今日工作，收入会按时薪实时累计。" divided>
        <div class="work-console">
          <div class="work-live">
            <div class="work-live__header">
              <div class="work-live__title">
                <el-icon><Timer /></el-icon>
                <span>实时收入</span>
              </div>
              <CnStatusTag :type="getWorkStatusTone(calculatorData.workStatus)" size="sm">
                {{ getWorkStatusText() }}
              </CnStatusTag>
            </div>

            <div class="live-earning" :class="{ 'is-running': calculatorData.workStatus === 1 }">
              ¥{{ formatNumber(displayEarnings) }}
            </div>
            <div class="live-subtitle">
              已工作 {{ formatNumber(displayWorkHours) }} 小时
              <span v-if="calculatorData.todayStartTime"> · 开始于 {{ formatDateTime(calculatorData.todayStartTime) }}</span>
            </div>

            <div class="progress-track" aria-hidden="true">
              <span class="progress-fill" :style="{ '--progress-size': `${workProgress}%` }" />
            </div>

            <div v-if="calculatorData.workStatus === 1" class="work-tip is-running">
              <el-icon class="spinning"><Timer /></el-icon>
              实时计算中...
            </div>
            <div v-else-if="calculatorData.workStatus === 2" class="work-tip is-paused">
              <el-icon><VideoPause /></el-icon>
              工作暂停中
            </div>
          </div>

          <div class="work-actions">
            <div class="work-actions__title">工作操作</div>

            <el-button
              v-if="calculatorData.workStatus === 0"
              type="success"
              size="large"
              :icon="VideoPlay"
              :loading="actionLoading"
              @click="startWork"
            >
              开始工作
            </el-button>

            <div v-else-if="calculatorData.workStatus === 1" class="button-stack">
              <el-button
                type="warning"
                size="large"
                :icon="VideoPause"
                :loading="actionLoading"
                @click="pauseWork"
              >
                暂停工作
              </el-button>
              <el-button
                type="danger"
                size="large"
                :icon="SwitchButton"
                :loading="actionLoading"
                @click="endWork"
              >
                结束工作
              </el-button>
            </div>

            <div v-else-if="calculatorData.workStatus === 2" class="button-stack">
              <el-button
                type="primary"
                size="large"
                :icon="VideoPlay"
                :loading="actionLoading"
                @click="resumeWork"
              >
                恢复工作
              </el-button>
              <el-button
                type="danger"
                size="large"
                :icon="SwitchButton"
                :loading="actionLoading"
                @click="endWork"
              >
                结束工作
              </el-button>
            </div>

            <el-button v-else type="info" size="large" :icon="Select" disabled>
              今日完成
            </el-button>

            <div class="work-actions__meta">
              <span>工作日：{{ calculatorData.workDaysPerMonth }} 天/月</span>
              <span>标准工时：{{ formatNumber(calculatorData.workHoursPerDay) }} 小时/天</span>
            </div>
          </div>
        </div>
      </CnSection>

      <CnSection title="周期统计" description="汇总本周和本月已记录的工作时长与收入。" divided>
        <div class="period-grid">
          <article class="period-metric">
            <div class="period-metric__header">
              <el-icon><Calendar /></el-icon>
              <h3>本周统计</h3>
            </div>
            <dl class="metric-list">
              <div>
                <dt>工作时长</dt>
                <dd>{{ formatNumber(calculatorData.weekWorkHours) }} 小时</dd>
              </div>
              <div>
                <dt>收入金额</dt>
                <dd class="is-earning">¥{{ formatNumber(calculatorData.weekEarnings) }}</dd>
              </div>
            </dl>
          </article>

          <article class="period-metric">
            <div class="period-metric__header">
              <el-icon><CreditCard /></el-icon>
              <h3>本月统计</h3>
            </div>
            <dl class="metric-list">
              <div>
                <dt>工作时长</dt>
                <dd>{{ formatNumber(calculatorData.monthWorkHours) }} 小时</dd>
              </div>
              <div>
                <dt>收入金额</dt>
                <dd class="is-earning">¥{{ formatNumber(calculatorData.monthEarnings) }}</dd>
              </div>
            </dl>
          </article>
        </div>
      </CnSection>
    </template>

    <el-dialog
      v-model="showConfigDialog"
      title="薪资配置"
      width="min(520px, 92vw)"
      :close-on-click-modal="false"
    >
      <el-form ref="configFormRef" :model="configForm" :rules="configRules" label-width="120px">
        <el-form-item label="月薪" prop="monthlySalary">
          <el-input-number
            v-model="configForm.monthlySalary"
            class="number-input"
            :min="1000"
            :max="999999.99"
            :precision="2"
            :step="100"
            placeholder="请输入月薪"
          />
          <div class="form-tip">单位：人民币元</div>
        </el-form-item>

        <el-form-item label="每月工作天数" prop="workDaysPerMonth">
          <el-input-number
            v-model="configForm.workDaysPerMonth"
            class="number-input"
            :min="1"
            :max="31"
            :step="1"
            placeholder="请输入每月工作天数"
          />
          <div class="form-tip">一般为22天</div>
        </el-form-item>

        <el-form-item label="每日工作小时" prop="workHoursPerDay">
          <el-input-number
            v-model="configForm.workHoursPerDay"
            class="number-input"
            :min="0.5"
            :max="24"
            :precision="2"
            :step="0.5"
            placeholder="请输入每日工作小时数"
          />
          <div class="form-tip">一般为8小时</div>
        </el-form-item>

        <el-form-item v-if="configForm.monthlySalary && configForm.workDaysPerMonth && configForm.workHoursPerDay">
          <div class="preview-box">
            <div class="preview-title">预计时薪</div>
            <div class="preview-value">¥{{ calculateHourlyRate() }}</div>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showConfigDialog = false">取消</el-button>
          <el-button type="primary" :loading="saveLoading" @click="saveConfig">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  ArrowLeft,
  Calendar,
  CreditCard,
  Edit,
  Plus,
  Select,
  SwitchButton,
  Timer,
  VideoPause,
  VideoPlay
} from '@element-plus/icons-vue'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import type { CnTone } from '@/design-system'
import {
  getSalaryCalculatorData,
  getSalaryConfig,
  handleWorkTime,
  saveOrUpdateSalaryConfig
} from '@/api/moyu'

interface SalaryCalculatorData {
  monthlySalary: number
  workDaysPerMonth: number
  workHoursPerDay: number
  hourlyRate: number
  todayWorkHours: number
  todayEarnings: number
  todayStartTime: string | Date | null
  totalPauseMinutes?: number
  workStatus: number
  weekWorkHours: number
  weekEarnings: number
  monthWorkHours: number
  monthEarnings: number
}

interface SalaryConfigForm {
  monthlySalary: number | null
  workDaysPerMonth: number
  workHoursPerDay: number
}

type WorkAction = 'START' | 'PAUSE' | 'RESUME' | 'END'

const router = useRouter()

const loading = ref(true)
const actionLoading = ref(false)
const saveLoading = ref(false)
const showConfigDialog = ref(false)
const hasConfig = ref(false)

const realtimeTimer = ref<ReturnType<typeof setInterval> | null>(null)
const realtimeWorkHours = ref(0)
const realtimeEarnings = ref(0)

const calculatorData = reactive<SalaryCalculatorData>({
  monthlySalary: 0,
  workDaysPerMonth: 22,
  workHoursPerDay: 8,
  hourlyRate: 0,
  todayWorkHours: 0,
  todayEarnings: 0,
  todayStartTime: null,
  workStatus: 0,
  weekWorkHours: 0,
  weekEarnings: 0,
  monthWorkHours: 0,
  monthEarnings: 0
})

const configForm = reactive<SalaryConfigForm>({
  monthlySalary: null,
  workDaysPerMonth: 22,
  workHoursPerDay: 8
})

const configFormRef = ref<FormInstance>()

const displayWorkHours = computed(() => {
  if (calculatorData.workStatus === 1) {
    return realtimeWorkHours.value
  }
  return calculatorData.todayWorkHours || 0
})

const displayEarnings = computed(() => {
  if (calculatorData.workStatus === 1) {
    return realtimeEarnings.value
  }
  return calculatorData.todayEarnings || 0
})

const workProgress = computed(() => {
  const targetHours = Number(calculatorData.workHoursPerDay) || 0
  if (!targetHours) return 0
  return Math.min(100, Number(((displayWorkHours.value / targetHours) * 100).toFixed(2)))
})

const configRules: FormRules = {
  monthlySalary: [
    { required: true, message: '请输入月薪', trigger: 'blur' },
    { type: 'number', min: 1000, max: 999999.99, message: '月薪应在1000-999999.99之间', trigger: 'blur' }
  ],
  workDaysPerMonth: [
    { required: true, message: '请输入每月工作天数', trigger: 'blur' },
    { type: 'number', min: 1, max: 31, message: '工作天数应在1-31之间', trigger: 'blur' }
  ],
  workHoursPerDay: [
    { required: true, message: '请输入每日工作小时数', trigger: 'blur' },
    { type: 'number', min: 0.5, max: 24, message: '工作小时数应在0.5-24之间', trigger: 'blur' }
  ]
}

const goBack = () => {
  router.push('/moyu-tools')
}

const openConfigDialog = () => {
  hydrateConfigFormFromCalculator()
  showConfigDialog.value = true
  loadConfigData()
}

const hydrateConfigFormFromCalculator = () => {
  configForm.monthlySalary = calculatorData.monthlySalary || null
  configForm.workDaysPerMonth = calculatorData.workDaysPerMonth || 22
  configForm.workHoursPerDay = calculatorData.workHoursPerDay || 8
}

const calculateHourlyRate = () => {
  if (!configForm.monthlySalary || !configForm.workDaysPerMonth || !configForm.workHoursPerDay) {
    return '0.00'
  }
  const rate = configForm.monthlySalary / (configForm.workDaysPerMonth * configForm.workHoursPerDay)
  return rate.toFixed(2)
}

const getWorkStatusText = () => {
  switch (calculatorData.workStatus) {
    case 0:
      return '待开始'
    case 1:
      return '工作中'
    case 2:
      return '暂停中'
    case 3:
      return '已完成'
    default:
      return '待开始'
  }
}

const getWorkStatusTone = (status?: number): CnTone => {
  switch (status) {
    case 1:
      return 'success'
    case 2:
      return 'warning'
    case 3:
      return 'info'
    case 0:
      return 'neutral'
    default:
      return 'neutral'
  }
}

const formatNumber = (value?: number | string | null) => {
  if (!value && value !== 0) return '0.00'
  return Number(value).toFixed(2)
}

const formatDateTime = (dateTime?: string | Date | null) => {
  if (!dateTime) return ''
  return new Date(dateTime).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const syncRealtimeSnapshot = () => {
  realtimeWorkHours.value = calculatorData.todayWorkHours || 0
  realtimeEarnings.value = calculatorData.todayEarnings || 0
}

const loadCalculatorData = async () => {
  try {
    loading.value = true
    const data = (await getSalaryCalculatorData()) as Partial<SalaryCalculatorData> | null

    if (data && data.monthlySalary) {
      Object.assign(calculatorData, data)
      hasConfig.value = true
      syncRealtimeSnapshot()
    } else {
      hasConfig.value = false
      syncRealtimeSnapshot()
    }
  } catch (error: any) {
    console.error('加载计算器数据失败:', error)
    if (error?.response?.status === 401) {
      ElMessage.error('请先登录')
    } else {
      ElMessage.error('加载数据失败')
    }
  } finally {
    loading.value = false
  }
}

const loadConfigData = async () => {
  try {
    const data = (await getSalaryConfig()) as Partial<SalaryConfigForm> | null
    if (data) {
      Object.assign(configForm, data)
    }
  } catch (error) {
    console.error('加载配置数据失败:', error)
  }
}

const saveConfig = async () => {
  if (!configFormRef.value) return

  try {
    await configFormRef.value.validate()
    saveLoading.value = true

    await saveOrUpdateSalaryConfig(configForm)
    ElMessage.success('配置保存成功')
    showConfigDialog.value = false

    await loadCalculatorData()
    hydrateConfigFormFromCalculator()
  } catch (error) {
    if (error !== false) {
      console.error('保存配置失败:', error)
      ElMessage.error('保存配置失败')
    }
  } finally {
    saveLoading.value = false
  }
}

const handleWorkAction = async (action: WorkAction, remark: string) => {
  actionLoading.value = true
  const data = (await handleWorkTime({ action, remark })) as Partial<SalaryCalculatorData>
  Object.assign(calculatorData, data)
  syncRealtimeSnapshot()
}

const startWork = async () => {
  try {
    await handleWorkAction('START', '开始今日工作')
    ElMessage.success('开始工作记录')
    startRealTimeTimer()
  } catch (error) {
    console.error('开始工作失败:', error)
    ElMessage.error('开始工作失败')
  } finally {
    actionLoading.value = false
  }
}

const pauseWork = async () => {
  try {
    await handleWorkAction('PAUSE', '暂停工作')
    ElMessage.success('工作已暂停')
    stopRealTimeTimer()
  } catch (error) {
    console.error('暂停工作失败:', error)
    ElMessage.error('暂停工作失败')
  } finally {
    actionLoading.value = false
  }
}

const resumeWork = async () => {
  try {
    await handleWorkAction('RESUME', '恢复工作')
    ElMessage.success('工作已恢复')
    startRealTimeTimer()
  } catch (error) {
    console.error('恢复工作失败:', error)
    ElMessage.error('恢复工作失败')
  } finally {
    actionLoading.value = false
  }
}

const endWork = async () => {
  try {
    await ElMessageBox.confirm('确定结束今日工作吗？', '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await handleWorkAction('END', '结束今日工作')
    ElMessage.success('工作记录已完成')
    stopRealTimeTimer()
  } catch (error) {
    if (error === 'cancel') return
    console.error('结束工作失败:', error)
    ElMessage.error('结束工作失败')
  } finally {
    actionLoading.value = false
  }
}

const startRealTimeTimer = () => {
  if (realtimeTimer.value) return

  realtimeTimer.value = setInterval(() => {
    if (!calculatorData.todayStartTime || !calculatorData.hourlyRate) return

    const now = new Date()
    const startTime = new Date(calculatorData.todayStartTime)

    let totalMinutes = Math.floor((now.getTime() - startTime.getTime()) / (1000 * 60))
    const pauseMinutes = calculatorData.totalPauseMinutes || 0
    totalMinutes = Math.max(0, totalMinutes - pauseMinutes)

    const workHours = totalMinutes / 60
    realtimeWorkHours.value = workHours
    realtimeEarnings.value = workHours * calculatorData.hourlyRate
  }, 1000)
}

const stopRealTimeTimer = () => {
  if (realtimeTimer.value) {
    clearInterval(realtimeTimer.value)
    realtimeTimer.value = null
  }
}

onMounted(async () => {
  await loadCalculatorData()
  if (!hasConfig.value) {
    await loadConfigData()
  } else {
    hydrateConfigFormFromCalculator()
  }

  if (calculatorData.workStatus === 1) {
    startRealTimeTimer()
  }
})

onUnmounted(() => {
  stopRealTimeTimer()
})
</script>

<style scoped>
.salary-calculator-page {
  min-width: 0;
}

.config-section :deep(.cn-section__body) {
  padding: var(--cn-space-6);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.work-console {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.65fr);
  gap: var(--cn-space-5);
  min-width: 0;
}

.work-live {
  display: grid;
  gap: var(--cn-space-4);
  min-width: 0;
}

.work-live__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
}

.work-live__title {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-2);
  color: var(--cn-color-text-primary);
  font-size: 15px;
  font-weight: 700;
}

.work-live__title .el-icon {
  color: var(--cn-color-success);
}

.live-earning {
  min-width: 0;
  color: var(--cn-color-success);
  font-family: var(--cn-font-heading);
  font-size: 42px;
  font-weight: 800;
  line-height: 1.1;
  overflow-wrap: anywhere;
  transition: color var(--cn-motion-base) var(--cn-ease-out);
}

.live-earning.is-running {
  animation: earning-pulse 2s var(--cn-ease-in-out) infinite alternate;
}

.live-subtitle {
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.progress-track {
  overflow: hidden;
  height: 10px;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-bg-surface-muted);
}

.progress-fill {
  display: block;
  width: var(--progress-size);
  height: 100%;
  border-radius: inherit;
  background: color-mix(in srgb, var(--cn-color-success) 62%, var(--cn-color-brand-primary));
  transition: width var(--cn-motion-base) var(--cn-ease-out);
}

.work-tip {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-2);
  width: fit-content;
  min-height: 30px;
  padding: 0 var(--cn-space-3);
  border-radius: var(--cn-radius-pill);
  font-size: 13px;
  font-weight: 700;
}

.work-tip.is-running {
  background: var(--cn-color-success-soft);
  color: var(--cn-color-success);
}

.work-tip.is-paused {
  background: var(--cn-color-warning-soft);
  color: var(--cn-color-warning);
}

.spinning {
  animation: spin 2s linear infinite;
}

.work-actions {
  display: grid;
  align-content: start;
  gap: var(--cn-space-4);
  min-width: 0;
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.work-actions__title {
  color: var(--cn-color-text-primary);
  font-size: 15px;
  font-weight: 700;
}

.button-stack {
  display: grid;
  gap: var(--cn-space-3);
}

.button-stack .el-button,
.work-actions > .el-button {
  width: 100%;
  margin-left: 0;
}

.work-actions__meta {
  display: grid;
  gap: var(--cn-space-2);
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
  line-height: 1.6;
}

.period-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.period-metric {
  display: grid;
  gap: var(--cn-space-4);
  min-width: 0;
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-card-radius);
  background: var(--cn-card-bg);
  box-shadow: var(--cn-card-shadow);
}

.period-metric__header {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  color: var(--cn-color-text-primary);
}

.period-metric__header .el-icon {
  color: var(--cn-color-brand-primary);
  font-size: 18px;
}

.period-metric__header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.metric-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-4);
  margin: 0;
}

.metric-list div {
  min-width: 0;
}

.metric-list dt {
  margin: 0 0 var(--cn-space-2);
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.metric-list dd {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 22px;
  font-weight: 750;
  line-height: 1.2;
  overflow-wrap: anywhere;
}

.metric-list dd.is-earning {
  color: var(--cn-color-success);
}

.number-input {
  width: 100%;
}

.form-tip {
  margin-top: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  line-height: 1.5;
}

.preview-box {
  width: 100%;
  padding: var(--cn-space-4);
  border: 1px solid color-mix(in srgb, var(--cn-color-success) 28%, var(--cn-color-border-subtle));
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-success-soft);
  text-align: center;
}

.preview-title {
  color: var(--cn-color-success);
  font-size: 13px;
  font-weight: 700;
}

.preview-value {
  margin-top: var(--cn-space-2);
  color: var(--cn-color-success);
  font-family: var(--cn-font-heading);
  font-size: 24px;
  font-weight: 800;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--cn-space-2);
}

@keyframes earning-pulse {
  from {
    color: var(--cn-color-success);
  }

  to {
    color: var(--cn-color-brand-primary);
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1100px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .work-console {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-grid,
  .period-grid,
  .metric-list {
    grid-template-columns: minmax(0, 1fr);
  }

  .work-live__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .live-earning {
    font-size: 34px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .live-earning.is-running,
  .spinning {
    animation: none;
  }
}
</style>
