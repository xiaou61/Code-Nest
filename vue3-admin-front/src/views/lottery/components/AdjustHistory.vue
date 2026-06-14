<template>
  <div class="adjust-history">
    <CnSection title="查询条件" description="按奖品 ID 查询概率调整历史。" surface="plain" divided>
      <div class="filter-grid">
        <el-input v-model="queryForm.prizeId" placeholder="请输入奖品ID" clearable />
        <div class="filter-actions">
          <el-button type="primary" @click="loadHistory">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>
    </CnSection>

    <CnSection title="调整历史" :description="`共 ${total} 条调整记录`" surface="plain" divided>
      <CnDataTable
        :columns="columns"
        :data="historyList"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        border
        empty-title="暂无调整历史"
        empty-description="当前查询条件下没有概率调整记录。"
        empty-icon="AH"
        @page-change="handlePageChange"
        @page-size-change="handleSizeChange"
      >
        <template #oldProbability="{ row }">
          {{ formatProbability(row.oldProbability) }}
        </template>

        <template #newProbability="{ row }">
          {{ formatProbability(row.newProbability) }}
        </template>

        <template #change="{ row }">
          <CnStatusTag :type="getChangeTone(row.oldProbability, row.newProbability)" size="sm">
            {{ getChangeText(row.oldProbability, row.newProbability) }}
          </CnStatusTag>
        </template>

        <template #operator="{ row }">
          {{ row.operatorId === 0 ? '系统自动' : `管理员${row.operatorId}` }}
        </template>
      </CnDataTable>
    </CnSection>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { lotteryAdminApi } from '@/api/lotteryAdmin'
import { CnDataTable, CnSection, CnStatusTag } from '@/design-system'
import type { CnPagination, CnTableColumn, CnTone } from '@/design-system'

interface AdjustRecord extends Record<string, unknown> {
  id: number
  prizeId?: number
  prizeName?: string
  oldProbability?: number
  newProbability?: number
  adjustReason?: string
  operatorId?: number
  createTime?: string
}

const loading = ref(false)
const historyList = ref<AdjustRecord[]>([])
const total = ref(0)

const queryForm = reactive({
  prizeId: null as number | null,
  page: 1,
  size: 20
})

const columns: CnTableColumn<AdjustRecord>[] = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'prizeId', label: '奖品ID', width: 100 },
  { prop: 'prizeName', label: '奖品名称', minWidth: 140, showOverflowTooltip: true },
  { prop: 'oldProbability', label: '旧概率', width: 120, slot: 'oldProbability' },
  { prop: 'newProbability', label: '新概率', width: 120, slot: 'newProbability' },
  { label: '变化', width: 110, slot: 'change' },
  { prop: 'adjustReason', label: '调整原因', minWidth: 220, showOverflowTooltip: true },
  { label: '操作人', width: 110, slot: 'operator' },
  { prop: 'createTime', label: '调整时间', width: 180 }
]

const tablePagination = computed<CnPagination>(() => ({
  page: queryForm.page,
  pageSize: queryForm.size,
  total: total.value,
  pageSizes: [10, 20, 50, 100]
}))

const loadHistory = async () => {
  loading.value = true
  try {
    const res = await lotteryAdminApi.getAdjustHistory(queryForm.prizeId, queryForm.page, queryForm.size)
    historyList.value = res.records || []
    total.value = res.total || 0
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  queryForm.prizeId = null
  queryForm.page = 1
  loadHistory()
}

const handlePageChange = (page: number) => {
  queryForm.page = page
  loadHistory()
}

const handleSizeChange = (size: number) => {
  queryForm.size = size
  queryForm.page = 1
  loadHistory()
}

const formatProbability = (value?: number) => `${((value || 0) * 100).toFixed(4)}%`

const getChangeText = (oldProb?: number, newProb?: number) => {
  const oldValue = oldProb || 0
  const newValue = newProb || 0
  if (!oldValue) {
    return newValue > 0 ? '+100.00%' : '0.00%'
  }
  const change = (((newValue - oldValue) / oldValue) * 100).toFixed(2)
  return Number(change) > 0 ? `+${change}%` : `${change}%`
}

const getChangeTone = (oldProb?: number, newProb?: number): CnTone => {
  return (newProb || 0) > (oldProb || 0) ? 'success' : 'danger'
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
.adjust-history {
  display: grid;
  gap: var(--cn-space-4);
}

.filter-grid {
  display: grid;
  grid-template-columns: minmax(180px, 280px) auto;
  gap: var(--cn-space-3);
  align-items: center;
}

.filter-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

@media (max-width: 680px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>
