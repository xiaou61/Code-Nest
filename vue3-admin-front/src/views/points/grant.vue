<template>
  <CnPage class="points-grant-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="积分发放管理"
      description="为用户发放积分奖励，支持单个发放和批量发放。"
      eyebrow="Points Grant"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">后台发放</CnStatusTag>
        <CnStatusTag type="success">最近记录 {{ historyData.length }} 条</CnStatusTag>
      </template>
      <template #actions>
        <el-button :icon="Refresh" :loading="historyLoading" @click="loadHistory">刷新记录</el-button>
        <el-button type="primary" @click="router.push('/points/details')">查看明细</el-button>
      </template>
    </CnPageHeader>

    <div class="grant-grid">
      <CnSection title="单个发放积分" description="查找指定用户并发放积分奖励。" divided>
        <el-form ref="singleGrantFormRef" :model="singleGrantForm" :rules="singleGrantRules" label-width="100px">
          <el-form-item label="用户ID" prop="userId">
            <el-input v-model="singleGrantForm.userId" placeholder="请输入用户ID" type="number">
              <template #append>
                <el-button :icon="Search" :loading="searchingUser" @click="handleSearchUser">查找</el-button>
              </template>
            </el-input>
          </el-form-item>

          <div v-if="selectedUser" class="user-info-display">
            <div class="user-card">
              <el-avatar :size="50">
                <el-icon><User /></el-icon>
              </el-avatar>
              <div class="user-details">
                <div class="user-name">{{ selectedUser.userName || `用户${selectedUser.userId}` }}</div>
                <div class="user-stats">
                  <CnStatusTag type="brand" size="sm">当前积分 {{ formatNumber(selectedUser.totalPoints) }}</CnStatusTag>
                  <CnStatusTag type="success" size="sm">连续 {{ selectedUser.continuousDays || 0 }} 天</CnStatusTag>
                </div>
              </div>
            </div>
          </div>

          <el-form-item label="积分数量" prop="points">
            <el-input-number v-model="singleGrantForm.points" :min="1" :max="10000" controls-position="right" class="full-width" />
          </el-form-item>

          <el-form-item label="发放原因" prop="reason">
            <el-input
              v-model="singleGrantForm.reason"
              type="textarea"
              :rows="4"
              placeholder="请输入发放原因（例如：活动奖励、bug反馈奖励等）"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :icon="Check" :loading="singleGrantLoading" class="full-width" size="large" @click="handleSingleGrant">
              确认发放
            </el-button>
          </el-form-item>
        </el-form>
      </CnSection>

      <CnSection title="批量发放积分" description="输入多个用户 ID，为一组用户批量发放积分。" divided>
        <el-form ref="batchGrantFormRef" :model="batchGrantForm" :rules="batchGrantRules" label-width="100px">
          <el-form-item label="用户ID列表" prop="userIdsText">
            <el-input
              v-model="batchGrantForm.userIdsText"
              type="textarea"
              :rows="6"
              placeholder="请输入用户ID，多个用户用英文逗号分隔&#10;例如：123,456,789,101,202"
            />
            <div class="form-tip">
              <el-icon><InfoFilled /></el-icon>
              支持批量输入多个用户ID，用英文逗号分隔
            </div>
          </el-form-item>

          <el-form-item label="积分数量" prop="points">
            <el-input-number v-model="batchGrantForm.points" :min="1" :max="10000" controls-position="right" class="full-width" />
          </el-form-item>

          <el-form-item label="发放原因" prop="reason">
            <el-input
              v-model="batchGrantForm.reason"
              type="textarea"
              :rows="4"
              placeholder="请输入发放原因（例如：春节活动奖励、社区贡献奖励等）"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>

          <el-form-item>
            <el-button type="warning" :icon="Finished" :loading="batchGrantLoading" class="full-width" size="large" @click="handleBatchGrant">
              批量发放
            </el-button>
          </el-form-item>
        </el-form>
      </CnSection>
    </div>

    <CnSection title="最近发放记录" description="最近 10 条后台发放积分记录。" divided>
      <template #actions>
        <el-button type="primary" link @click="router.push('/points/details')">查看全部</el-button>
      </template>

      <CnDataTable
        :columns="historyColumns"
        :data="historyData"
        :loading="historyLoading"
        :pagination="null"
        row-key="id"
        empty-title="暂无发放记录"
        empty-description="还没有后台发放积分记录。"
        empty-icon="PG"
      >
        <template #user="{ row }">
          <div class="user-cell">
            <el-avatar :size="32">
              <el-icon><User /></el-icon>
            </el-avatar>
            <div class="user-info">
              <div class="user-name">{{ row.userName || `用户${row.userId}` }}</div>
              <div class="user-id">ID: {{ row.userId }}</div>
            </div>
          </div>
        </template>

        <template #pointsChange="{ row }">
          <div class="points-change points-positive">+{{ formatNumber(row.pointsChange) }}</div>
        </template>

        <template #pointsType="{ row }">
          <CnStatusTag :type="row.pointsType === 1 ? 'warning' : 'info'" size="sm">{{ row.pointsTypeDesc }}</CnStatusTag>
        </template>

        <template #admin="{ row }">
          <div v-if="row.adminId" class="admin-cell">
            <div class="admin-name">{{ row.adminName || `管理员${row.adminId}` }}</div>
          </div>
          <CnStatusTag v-else type="neutral" size="sm" subtle>系统</CnStatusTag>
        </template>

        <template #createTime="{ row }">
          <div class="time-cell">
            <div class="time-date">{{ formatDate(row.createTime) }}</div>
            <div class="time-time">{{ formatTime(row.createTime) }}</div>
          </div>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="showBatchResultDialog" title="批量发放结果" width="800px">
      <div v-if="batchResult" class="batch-result">
        <div class="result-summary">
          <div class="summary-item success">
            <div class="summary-number">{{ batchResult.successCount }}</div>
            <div class="summary-label">成功</div>
          </div>
          <div class="summary-item error">
            <div class="summary-number">{{ batchResult.failCount }}</div>
            <div class="summary-label">失败</div>
          </div>
          <div class="summary-item info">
            <div class="summary-number">{{ formatNumber(batchResult.totalPointsGranted) }}</div>
            <div class="summary-label">总积分</div>
          </div>
        </div>

        <CnSection title="详细结果" surface="plain" divided>
          <CnDataTable
            :columns="batchResultColumns"
            :data="batchResult.grantResults || []"
            :pagination="null"
            row-key="userId"
            empty-title="暂无结果明细"
            empty-description="批量发放接口未返回明细。"
            empty-icon="BR"
          >
            <template #success="{ row }">
              <CnStatusTag :type="row.success ? 'success' : 'danger'" size="sm">
                {{ row.success ? '成功' : '失败' }}
              </CnStatusTag>
            </template>
            <template #currentBalance="{ row }">
              <span v-if="row.success">{{ formatNumber(row.currentBalance) }}</span>
              <span v-else>-</span>
            </template>
          </CnDataTable>
        </CnSection>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showBatchResultDialog = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Finished, InfoFilled, Refresh, Search, User } from '@element-plus/icons-vue'
import { pointsApi } from '@/api/points'
import { CnDataTable, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem, CnTableColumn } from '@/design-system'

interface UserPointsInfo {
  userId: number
  userName?: string
  totalPoints?: number
  continuousDays?: number
  avatar?: string
  realName?: string
  nickname?: string
  monthCheckinDays?: number
  hasCheckedToday?: boolean
  lastCheckinDate?: string
}

interface PointsDetail extends Record<string, unknown> {
  id?: number
  userId: number
  userName?: string
  pointsChange?: number
  pointsType?: number
  pointsTypeDesc?: string
  description?: string
  adminId?: number
  adminName?: string
  createTime?: string
}

interface BatchGrantResult {
  successCount?: number
  failCount?: number
  totalPointsGranted?: number
  grantResults?: BatchGrantRow[]
}

interface BatchGrantRow extends Record<string, unknown> {
  userId: number
  userName?: string
  success?: boolean
  message?: string
  currentBalance?: number
}

const router = useRouter()
const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '积分管理' }, { label: '积分发放' }]

const selectedUser = ref<UserPointsInfo | null>(null)
const searchingUser = ref(false)
const singleGrantLoading = ref(false)
const batchGrantLoading = ref(false)
const historyLoading = ref(false)
const historyData = ref<PointsDetail[]>([])
const showBatchResultDialog = ref(false)
const batchResult = ref<BatchGrantResult | null>(null)
const singleGrantFormRef = ref()
const batchGrantFormRef = ref()

const singleGrantForm = reactive({
  userId: '',
  points: null as number | null,
  reason: ''
})

const batchGrantForm = reactive({
  userIdsText: '',
  points: null as number | null,
  reason: ''
})

const singleGrantRules = {
  userId: [{ required: true, message: '请输入用户ID', trigger: 'blur' }],
  points: [
    { required: true, message: '请输入积分数量', trigger: 'blur' },
    { type: 'number', min: 1, max: 10000, message: '积分数量必须在1-10000之间', trigger: 'blur' }
  ],
  reason: [
    { required: true, message: '请输入发放原因', trigger: 'blur' },
    { max: 200, message: '发放原因不能超过200个字符', trigger: 'blur' }
  ]
}

const batchGrantRules = {
  userIdsText: [{ required: true, message: '请输入用户ID', trigger: 'blur' }],
  points: [
    { required: true, message: '请输入积分数量', trigger: 'blur' },
    { type: 'number', min: 1, max: 10000, message: '积分数量必须在1-10000之间', trigger: 'blur' }
  ],
  reason: [
    { required: true, message: '请输入发放原因', trigger: 'blur' },
    { max: 200, message: '发放原因不能超过200个字符', trigger: 'blur' }
  ]
}

const historyColumns: CnTableColumn<PointsDetail>[] = [
  { label: '用户', width: 160, slot: 'user' },
  { prop: 'pointsChange', label: '积分变动', width: 110, align: 'center', slot: 'pointsChange' },
  { prop: 'pointsType', label: '类型', width: 110, align: 'center', slot: 'pointsType' },
  { prop: 'description', label: '发放原因', minWidth: 220, showOverflowTooltip: true },
  { label: '操作管理员', width: 130, align: 'center', slot: 'admin' },
  { prop: 'createTime', label: '发放时间', width: 170, align: 'center', slot: 'createTime' }
]

const batchResultColumns: CnTableColumn<BatchGrantRow>[] = [
  { prop: 'userId', label: '用户ID', width: 100, align: 'center' },
  { prop: 'userName', label: '用户名', width: 150, showOverflowTooltip: true },
  { prop: 'success', label: '状态', width: 100, align: 'center', slot: 'success' },
  { prop: 'message', label: '结果信息', minWidth: 160, showOverflowTooltip: true },
  { prop: 'currentBalance', label: '当前余额', width: 120, align: 'right', slot: 'currentBalance' }
]

const formatNumber = (num?: number | string | null) => {
  if (!num) return '0'
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

const formatDate = (dateTime?: string) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleDateString('zh-CN')
}

const formatTime = (dateTime?: string) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleTimeString('zh-CN')
}

const handleSearchUser = async () => {
  if (!singleGrantForm.userId) {
    ElMessage.warning('请输入用户ID')
    return
  }

  searchingUser.value = true
  try {
    const userPointsInfo = await pointsApi.getUserPointsInfo(singleGrantForm.userId)
    selectedUser.value = {
      userId: userPointsInfo.userId,
      userName: userPointsInfo.userName,
      totalPoints: userPointsInfo.totalPoints,
      continuousDays: userPointsInfo.continuousDays,
      avatar: userPointsInfo.avatar,
      realName: userPointsInfo.realName,
      nickname: userPointsInfo.nickName,
      monthCheckinDays: userPointsInfo.monthCheckinDays,
      hasCheckedToday: userPointsInfo.hasCheckedToday,
      lastCheckinDate: userPointsInfo.lastCheckinDate
    }
    ElMessage.success('用户信息加载成功')
  } catch (error) {
    console.error('查找用户失败:', error)
    ElMessage.error(error.message || '用户不存在或查找失败')
    selectedUser.value = null
  } finally {
    searchingUser.value = false
  }
}

const handleSingleGrant = async () => {
  if (!singleGrantFormRef.value) return

  try {
    await singleGrantFormRef.value.validate()
    await ElMessageBox.confirm(`确认为用户 ${singleGrantForm.userId} 发放 ${singleGrantForm.points} 积分吗？`, '确认发放', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })

    singleGrantLoading.value = true
    await pointsApi.grantPoints({
      userId: parseInt(singleGrantForm.userId),
      points: singleGrantForm.points,
      reason: singleGrantForm.reason
    })

    ElMessage.success('积分发放成功')
    singleGrantFormRef.value.resetFields()
    Object.assign(singleGrantForm, {
      userId: '',
      points: null,
      reason: ''
    })
    selectedUser.value = null
    await loadHistory()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('发放积分失败:', error)
      ElMessage.error(error.message || '发放积分失败')
    }
  } finally {
    singleGrantLoading.value = false
  }
}

const handleBatchGrant = async () => {
  if (!batchGrantFormRef.value) return

  try {
    await batchGrantFormRef.value.validate()

    const userIds = batchGrantForm.userIdsText
      .split(',')
      .map((id) => parseInt(id.trim()))
      .filter((id) => !Number.isNaN(id))

    if (userIds.length === 0) {
      ElMessage.warning('请输入有效的用户ID')
      return
    }

    await ElMessageBox.confirm(`确认为${userIds.length}个用户发放${batchGrantForm.points}积分吗？`, '确认批量发放', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })

    batchGrantLoading.value = true
    const result = await pointsApi.batchGrantPoints({
      userIds,
      points: batchGrantForm.points,
      reason: batchGrantForm.reason
    })

    batchResult.value = result
    showBatchResultDialog.value = true
    ElMessage.success(`批量发放完成！成功：${result.successCount}人，失败：${result.failCount}人`)
    batchGrantFormRef.value.resetFields()
    Object.assign(batchGrantForm, {
      userIdsText: '',
      points: null,
      reason: ''
    })
    await loadHistory()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量发放积分失败:', error)
      ElMessage.error(error.message || '批量发放积分失败')
    }
  } finally {
    batchGrantLoading.value = false
  }
}

const loadHistory = async () => {
  historyLoading.value = true
  try {
    const result = await pointsApi.getAllPointsDetailList({
      pageNum: 1,
      pageSize: 10,
      pointsType: 1
    })
    historyData.value = Array.isArray(result?.records) ? result.records : []
  } catch (error) {
    console.error('加载历史记录失败:', error)
  } finally {
    historyLoading.value = false
  }
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
.points-grant-page {
  min-height: 100%;
}

.grant-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.user-info-display {
  margin: var(--cn-space-4) 0;
}

.user-card,
.grant-user-info {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.user-details,
.user-info {
  min-width: 0;
}

.user-name,
.admin-name,
.time-date {
  color: var(--cn-color-text-primary);
  font-weight: 650;
}

.user-stats,
.user-cell,
.dialog-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
}

.user-id,
.time-time,
.form-tip {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.points-change.points-positive {
  color: var(--cn-color-success);
  font-weight: 650;
}

.time-cell,
.admin-cell {
  text-align: center;
}

.form-tip {
  margin-top: var(--cn-space-1);
}

.full-width {
  width: 100%;
}

.batch-result {
  display: grid;
  gap: var(--cn-space-4);
}

.result-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.summary-item {
  min-width: 0;
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  text-align: center;
}

.summary-item.success {
  border-color: color-mix(in srgb, var(--cn-color-success) 36%, var(--cn-color-border-subtle));
}

.summary-item.error {
  border-color: color-mix(in srgb, var(--cn-color-danger) 36%, var(--cn-color-border-subtle));
}

.summary-item.info {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 36%, var(--cn-color-border-subtle));
}

.summary-number {
  margin-bottom: var(--cn-space-1);
  color: var(--cn-color-text-primary);
  font-size: 24px;
  font-weight: 700;
}

.summary-label {
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.dialog-footer {
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .grant-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .result-summary {
    grid-template-columns: 1fr;
  }

  .dialog-footer {
    justify-content: flex-start;
  }
}
</style>
