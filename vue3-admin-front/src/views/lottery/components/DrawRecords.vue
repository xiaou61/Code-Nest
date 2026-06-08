<template>
  <div class="draw-records">
    <CnSection title="查询条件" description="按用户、奖品和抽奖日期查询记录。" surface="plain" divided>
      <div class="filter-grid">
        <el-input v-model="queryForm.userId" placeholder="请输入用户ID" clearable />
        <el-input v-model="queryForm.prizeId" placeholder="请输入奖品ID" clearable />
        <el-date-picker
          v-model="queryForm.startDate"
          type="date"
          placeholder="开始日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
        />
        <el-date-picker
          v-model="queryForm.endDate"
          type="date"
          placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
        />
        <div class="filter-actions">
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>
    </CnSection>

    <CnSection title="抽奖记录" :description="`共 ${total} 条抽奖记录`" surface="plain" divided>
      <CnDataTable
        :columns="columns"
        :data="recordList"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        border
        empty-title="暂无抽奖记录"
        empty-description="当前查询条件下没有抽奖记录。"
        empty-icon="LR"
        @page-change="handlePageChange"
        @page-size-change="handleSizeChange"
      >
        <template #prizeLevel="{ row }">
          <CnStatusTag :type="getLevelTone(row.prizeLevel)" size="sm">
            {{ getLevelName(row.prizeLevel) }}
          </CnStatusTag>
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

interface DrawRecord extends Record<string, unknown> {
  id: number
  userId?: number
  prizeName?: string
  prizePoints?: number
  prizeLevel?: number
  strategyType?: string
  ip?: string
  device?: string
  createTime?: string
}

const loading = ref(false)
const recordList = ref<DrawRecord[]>([])
const total = ref(0)

const queryForm = reactive({
  userId: null as number | null,
  prizeId: null as number | null,
  startDate: '',
  endDate: '',
  page: 1,
  size: 20
})

const columns: CnTableColumn<DrawRecord>[] = [
  { prop: 'id', label: '记录ID', width: 90 },
  { prop: 'userId', label: '用户ID', width: 100 },
  { prop: 'prizeName', label: '奖品名称', minWidth: 140, showOverflowTooltip: true },
  { prop: 'prizePoints', label: '获得积分', width: 100, align: 'center' },
  { prop: 'prizeLevel', label: '奖品等级', width: 110, align: 'center', slot: 'prizeLevel' },
  { prop: 'strategyType', label: '抽奖策略', width: 120 },
  { prop: 'ip', label: 'IP地址', width: 140 },
  { prop: 'device', label: '设备信息', minWidth: 160, showOverflowTooltip: true },
  { prop: 'createTime', label: '抽奖时间', width: 180 }
]

const tablePagination = computed<CnPagination>(() => ({
  page: queryForm.page,
  pageSize: queryForm.size,
  total: total.value,
  pageSizes: [10, 20, 50, 100]
}))

const handleQuery = async () => {
  loading.value = true
  try {
    const res = await lotteryAdminApi.getAllDrawRecords(queryForm)
    recordList.value = res.records || []
    total.value = res.total || 0
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '查询失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  Object.assign(queryForm, {
    userId: null,
    prizeId: null,
    startDate: '',
    endDate: '',
    page: 1,
    size: 20
  })
  handleQuery()
}

const handlePageChange = (page: number) => {
  queryForm.page = page
  handleQuery()
}

const handleSizeChange = (size: number) => {
  queryForm.size = size
  queryForm.page = 1
  handleQuery()
}

const getLevelName = (level?: number) => {
  const names = ['', '特等奖', '一等奖', '二等奖', '三等奖', '四等奖', '五等奖', '六等奖', '未中奖']
  return level ? names[level] || '未知' : '未知'
}

const getLevelTone = (level?: number): CnTone => {
  if (level === 1) return 'danger'
  if (level && level <= 3) return 'warning'
  if (level && level <= 7) return 'success'
  return 'info'
}

onMounted(() => {
  handleQuery()
})
</script>

<style scoped>
.draw-records {
  display: grid;
  gap: var(--cn-space-4);
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(150px, 1fr)) auto;
  gap: var(--cn-space-3);
  align-items: center;
}

.filter-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

@media (max-width: 1080px) {
  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>
