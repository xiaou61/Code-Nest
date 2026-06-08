<template>
  <CnPage class="points-management-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="积分管理"
      description="管理系统积分发放和用户积分数据。"
      eyebrow="Points Management"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">总发放 {{ formatNumber(statistics?.totalPointsIssued) }}</CnStatusTag>
        <CnStatusTag type="success">活跃用户 {{ statistics?.activeUserCount || 0 }}</CnStatusTag>
        <CnStatusTag type="warning">后台发放 {{ formatNumber(statistics?.adminGrantPointsSum) }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" @click="loadStatistics">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="goGrant">发放积分</el-button>
      </template>
    </CnPageHeader>

    <div v-if="statistics" class="stats-grid">
      <CnStatCard title="总发放积分" :value="formatNumber(statistics.totalPointsIssued)" description="平台累计发放积分总量" tone="brand" />
      <CnStatCard title="打卡积分" :value="formatNumber(statistics.checkinPointsSum)" description="用户打卡获得的积分" tone="success" />
      <CnStatCard title="后台发放" :value="formatNumber(statistics.adminGrantPointsSum)" description="管理员主动发放积分" tone="warning" />
      <CnStatCard title="活跃用户" :value="statistics.activeUserCount || 0" description="近期有积分行为的用户" tone="info" />
    </div>

    <div class="action-grid">
      <button type="button" class="action-card" @click="goGrant">
        <el-icon class="action-icon"><Plus /></el-icon>
        <span class="action-title">发放积分</span>
        <span class="action-desc">为指定用户发放积分奖励</span>
      </button>
      <button type="button" class="action-card" @click="router.push('/points/users')">
        <el-icon class="action-icon"><Trophy /></el-icon>
        <span class="action-title">积分排行</span>
        <span class="action-desc">查看用户积分排行榜</span>
      </button>
      <button type="button" class="action-card" @click="router.push('/points/details')">
        <el-icon class="action-icon"><Document /></el-icon>
        <span class="action-title">积分明细</span>
        <span class="action-desc">查看所有积分变动记录</span>
      </button>
      <button type="button" class="action-card" @click="showBatchGrantDialog = true">
        <el-icon class="action-icon"><UserFilled /></el-icon>
        <span class="action-title">批量发放</span>
        <span class="action-desc">批量为多个用户发放积分</span>
      </button>
    </div>

    <CnSection v-if="statistics?.userRankings" title="积分排行榜 TOP 10" description="当前积分排名靠前的用户。" divided>
      <template #actions>
        <el-button type="primary" link @click="router.push('/points/users')">查看全部</el-button>
      </template>

      <div class="ranking-list">
        <div
          v-for="(user, index) in statistics.userRankings.slice(0, 10)"
          :key="user.userId"
          class="ranking-item"
          :class="{ 'top-three': index < 3 }"
        >
          <div class="rank-number" :class="`rank-${index + 1}`">{{ index + 1 }}</div>
          <div class="user-info">
            <div class="user-name">{{ user.userName }}</div>
            <div class="user-days">连续{{ user.continuousDays }}天</div>
          </div>
          <div class="points-info">
            <div class="points">{{ formatNumber(user.totalPoints) }}积分</div>
            <div class="yuan">≈{{ user.balanceYuan }}元</div>
          </div>
        </div>
      </div>
    </CnSection>

    <el-dialog v-model="showBatchGrantDialog" title="批量发放积分" width="500px">
      <el-form ref="batchGrantFormRef" :model="batchGrantForm" :rules="batchGrantRules" label-width="100px">
        <el-form-item label="用户ID" prop="userIdsText">
          <el-input
            v-model="batchGrantForm.userIdsText"
            type="textarea"
            :rows="3"
            placeholder="请输入用户ID，多个用户用英文逗号分隔，例如：123,456,789"
          />
          <div class="form-tip">请输入要发放积分的用户ID，用英文逗号分隔</div>
        </el-form-item>
        <el-form-item label="积分数量" prop="points">
          <el-input-number v-model="batchGrantForm.points" :min="1" :max="10000" controls-position="right" placeholder="请输入积分数量" />
        </el-form-item>
        <el-form-item label="发放原因" prop="reason">
          <el-input v-model="batchGrantForm.reason" placeholder="请输入发放原因" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showBatchGrantDialog = false">取消</el-button>
          <el-button type="primary" :loading="batchGrantLoading" @click="handleBatchGrant">确认发放</el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Plus, Refresh, Trophy, UserFilled } from '@element-plus/icons-vue'
import { pointsApi } from '@/api/points'
import { CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem } from '@/design-system'

interface UserRanking {
  userId: number
  userName?: string
  continuousDays?: number
  totalPoints?: number
  balanceYuan?: number | string
}

interface PointsStatistics {
  totalPointsIssued?: number
  checkinPointsSum?: number
  adminGrantPointsSum?: number
  activeUserCount?: number
  userRankings?: UserRanking[]
}

const router = useRouter()
const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '积分管理' }, { label: '积分概览' }]

const statistics = ref<PointsStatistics | null>(null)
const showBatchGrantDialog = ref(false)
const batchGrantLoading = ref(false)
const batchGrantFormRef = ref()

const batchGrantForm = reactive({
  userIdsText: '',
  points: null as number | null,
  reason: ''
})

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

const formatNumber = (num?: number | string | null) => {
  if (!num) return '0'
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

const loadStatistics = async () => {
  try {
    const data = await pointsApi.getPointsStatistics()
    statistics.value = data
  } catch (error) {
    console.error('加载积分统计数据失败:', error)
    ElMessage.error('加载数据失败')
  }
}

const goGrant = () => {
  router.push('/points/grant')
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

    await ElMessageBox.confirm(`确认为${userIds.length}个用户发放${batchGrantForm.points}积分吗？`, '确认发放', {
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

    ElMessage.success(`批量发放完成！成功：${result.successCount}人，失败：${result.failCount}人`)
    batchGrantFormRef.value.resetFields()
    Object.assign(batchGrantForm, {
      userIdsText: '',
      points: null,
      reason: ''
    })
    showBatchGrantDialog.value = false
    await loadStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量发放积分失败:', error)
      ElMessage.error(error.message || '批量发放积分失败')
    }
  } finally {
    batchGrantLoading.value = false
  }
}

onMounted(() => {
  loadStatistics()
})
</script>

<style scoped>
.points-management-page {
  min-height: 100%;
}

.stats-grid,
.action-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.action-card {
  display: grid;
  justify-items: center;
  gap: var(--cn-space-2);
  min-width: 0;
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-card-radius);
  background: var(--cn-card-bg);
  color: inherit;
  cursor: pointer;
  text-align: center;
  box-shadow: var(--cn-card-shadow);
  transition:
    transform var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out);
}

.action-card:hover {
  transform: translateY(-1px);
  box-shadow: var(--cn-shadow-sm);
}

.action-icon {
  color: var(--cn-color-brand-primary);
  font-size: 30px;
}

.action-title {
  color: var(--cn-color-text-primary);
  font-size: 16px;
  font-weight: 650;
}

.action-desc {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.ranking-list {
  display: grid;
  max-height: 430px;
  overflow-y: auto;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  padding: var(--cn-space-3) 0;
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.ranking-item:last-child {
  border-bottom: none;
}

.rank-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  font-weight: 700;
}

.rank-number.rank-1,
.rank-number.rank-2,
.rank-number.rank-3 {
  background: var(--cn-color-warning-soft);
  color: var(--cn-color-warning);
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name,
.points {
  color: var(--cn-color-text-primary);
  font-weight: 650;
}

.user-days,
.yuan,
.form-tip {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.points-info {
  text-align: right;
}

.dialog-footer {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--cn-space-2);
}

@media (max-width: 1180px) {
  .stats-grid,
  .action-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .stats-grid,
  .action-grid {
    grid-template-columns: 1fr;
  }

  .dialog-footer {
    justify-content: flex-start;
  }
}
</style>
