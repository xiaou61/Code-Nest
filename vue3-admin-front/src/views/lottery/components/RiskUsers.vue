<template>
  <div class="risk-users">
    <CnSection title="筛选条件" description="按风险等级和黑名单状态筛选抽奖用户。" surface="plain" divided>
      <div class="filter-grid">
        <el-select v-model="queryForm.riskLevel" placeholder="风险等级" clearable>
          <el-option label="正常" :value="0" />
          <el-option label="低风险" :value="1" />
          <el-option label="中风险" :value="2" />
          <el-option label="高风险" :value="3" />
        </el-select>
        <el-select v-model="queryForm.isBlacklist" placeholder="黑名单" clearable>
          <el-option label="是" :value="1" />
          <el-option label="否" :value="0" />
        </el-select>
        <div class="filter-actions">
          <el-button type="primary" @click="loadRiskUsers">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>
    </CnSection>

    <CnSection title="风险用户" :description="`共 ${total} 个用户`" surface="plain" divided>
      <CnDataTable
        :columns="columns"
        :data="userList"
        :loading="loading"
        :pagination="tablePagination"
        row-key="userId"
        border
        empty-title="暂无风险用户"
        empty-description="当前筛选条件下没有风险用户记录。"
        empty-icon="RU"
        @page-change="handlePageChange"
        @page-size-change="handleSizeChange"
      >
        <template #riskLevel="{ row }">
          <CnStatusTag :type="getRiskLevelTone(row.riskLevel)" size="sm">
            {{ getRiskLevelName(row.riskLevel) }}
          </CnStatusTag>
        </template>

        <template #continuousNoWin="{ row }">
          <span :class="{ 'high-continuous': Number(row.continuousNoWin) >= 20 }">
            {{ row.continuousNoWin }}
          </span>
        </template>

        <template #isBlacklist="{ row }">
          <CnStatusTag :type="row.isBlacklist === 1 ? 'danger' : 'success'" size="sm">
            {{ row.isBlacklist === 1 ? '是' : '否' }}
          </CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button link type="primary" size="small" @click="handleEvaluateRisk(row)">评估风险</el-button>
            <el-button link type="warning" size="small" @click="handleDetectAbnormal(row)">检测异常</el-button>
            <el-button link type="danger" size="small" @click="handleResetLimit(row)">重置限制</el-button>
            <el-button link :type="row.isBlacklist === 1 ? 'success' : 'danger'" size="small" @click="handleToggleBlacklist(row)">
              {{ row.isBlacklist === 1 ? '移除黑名单' : '加入黑名单' }}
            </el-button>
          </div>
        </template>
      </CnDataTable>
    </CnSection>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { lotteryAdminApi } from '@/api/lotteryAdmin'
import { CnDataTable, CnSection, CnStatusTag } from '@/design-system'
import type { CnPagination, CnTableColumn, CnTone } from '@/design-system'

interface RiskUser extends Record<string, unknown> {
  userId: number
  riskLevel?: number
  todayDrawCount?: number
  totalDrawCount?: number
  todayWinCount?: number
  totalWinCount?: number
  continuousNoWin?: number
  isBlacklist?: number
}

const loading = ref(false)
const userList = ref<RiskUser[]>([])
const total = ref(0)

const queryForm = reactive({
  riskLevel: null as number | null,
  isBlacklist: null as number | null,
  minContinuousNoWin: null as number | null,
  page: 1,
  size: 20
})

const columns: CnTableColumn<RiskUser>[] = [
  { prop: 'userId', label: '用户ID', width: 100 },
  { prop: 'riskLevel', label: '风险等级', width: 110, slot: 'riskLevel' },
  { prop: 'todayDrawCount', label: '今日抽奖', width: 100, align: 'center' },
  { prop: 'totalDrawCount', label: '累计抽奖', width: 100, align: 'center' },
  { prop: 'todayWinCount', label: '今日中奖', width: 100, align: 'center' },
  { prop: 'totalWinCount', label: '累计中奖', width: 100, align: 'center' },
  { prop: 'continuousNoWin', label: '连续未中奖', width: 120, align: 'center', slot: 'continuousNoWin' },
  { prop: 'isBlacklist', label: '黑名单', width: 100, align: 'center', slot: 'isBlacklist' },
  { label: '操作', width: 300, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: queryForm.page,
  pageSize: queryForm.size,
  total: total.value,
  pageSizes: [10, 20, 50, 100]
}))

const loadRiskUsers = async () => {
  loading.value = true
  try {
    const res = await lotteryAdminApi.getRiskUserList(queryForm)
    userList.value = res.records || []
    total.value = res.total || 0
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  Object.assign(queryForm, {
    riskLevel: null,
    isBlacklist: null,
    minContinuousNoWin: null,
    page: 1,
    size: 20
  })
  loadRiskUsers()
}

const handleEvaluateRisk = async (row: RiskUser) => {
  try {
    const riskLevel = await lotteryAdminApi.evaluateRiskLevel(row.userId)
    ElMessage.success(`评估完成，风险等级：${getRiskLevelName(riskLevel)}`)
    loadRiskUsers()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '评估失败')
  }
}

const handleDetectAbnormal = async (row: RiskUser) => {
  try {
    const hasAbnormal = await lotteryAdminApi.detectAbnormalBehavior(row.userId)
    if (hasAbnormal) {
      ElMessage.warning('检测到异常行为！')
    } else {
      ElMessage.success('行为正常')
    }
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '检测失败')
  }
}

const handleResetLimit = async (row: RiskUser) => {
  try {
    await ElMessageBox.confirm('确认要重置该用户的抽奖限制吗？', '提示', {
      type: 'warning'
    })
    await lotteryAdminApi.resetUserLimit(row.userId)
    ElMessage.success('重置成功')
    loadRiskUsers()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

const handleToggleBlacklist = async (row: RiskUser) => {
  try {
    const isBlacklist = row.isBlacklist === 1 ? false : true
    await ElMessageBox.confirm(`确认要${isBlacklist ? '加入' : '移除'}黑名单吗？`, '提示', { type: 'warning' })
    await lotteryAdminApi.setUserBlacklist(row.userId, isBlacklist)
    ElMessage.success(isBlacklist ? '已加入黑名单' : '已移除黑名单')
    loadRiskUsers()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

const handlePageChange = (page: number) => {
  queryForm.page = page
  loadRiskUsers()
}

const handleSizeChange = (size: number) => {
  queryForm.size = size
  queryForm.page = 1
  loadRiskUsers()
}

const getRiskLevelName = (level?: number) => {
  const names = ['正常', '低风险', '中风险', '高风险']
  return typeof level === 'number' ? names[level] || '未知' : '未知'
}

const getRiskLevelTone = (level?: number): CnTone => {
  if (level === 0) return 'success'
  if (level === 1) return 'info'
  if (level === 2) return 'warning'
  return 'danger'
}

onMounted(() => {
  loadRiskUsers()
})
</script>

<style scoped>
.risk-users {
  display: grid;
  gap: var(--cn-space-4);
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(160px, 1fr)) auto;
  gap: var(--cn-space-3);
  align-items: center;
}

.filter-actions,
.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.high-continuous {
  color: var(--cn-color-danger);
  font-weight: 700;
}

@media (max-width: 820px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>
