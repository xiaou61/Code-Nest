<template>
  <div class="adjust-history">
    <el-card shadow="hover">
      <el-form :model="queryForm" inline>
        <el-form-item label="奖品ID">
          <el-input v-model="queryForm.prizeId" placeholder="请输入奖品ID" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadHistory">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="historyList" stripe border v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="prizeId" label="奖品ID" width="100" />
        <el-table-column prop="prizeName" label="奖品名称" width="120" />
        <el-table-column label="旧概率" width="120">
          <template #default="{ row }">
            {{ (row.oldProbability * 100).toFixed(4) }}%
          </template>
        </el-table-column>
        <el-table-column label="新概率" width="120">
          <template #default="{ row }">
            {{ (row.newProbability * 100).toFixed(4) }}%
          </template>
        </el-table-column>
        <el-table-column label="变化" width="100">
          <template #default="{ row }">
            <span :class="getChangeClass(row.oldProbability, row.newProbability)">
              {{ getChangeText(row.oldProbability, row.newProbability) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="adjustReason" label="调整原因" show-overflow-tooltip />
        <el-table-column label="操作人" width="100">
          <template #default="{ row }">
            {{ row.operatorId === 0 ? '系统自动' : `管理员${row.operatorId}` }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="调整时间" width="180" />
      </el-table>

      <el-pagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadHistory"
        @current-change="loadHistory"
        style="margin-top: 20px; justify-content: flex-end" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { lotteryAdminApi } from '@/api/lotteryAdmin'

const loading = ref(false)
const historyList = ref([])
const total = ref(0)

const queryForm = reactive({
  prizeId: null,
  page: 1,
  size: 20
})

const loadHistory = async () => {
  loading.value = true
  try {
    const res = await lotteryAdminApi.getAdjustHistory(
      queryForm.prizeId,
      queryForm.page,
      queryForm.size
    )
    historyList.value = res.records
    total.value = res.total
  } catch (error) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  queryForm.prizeId = null
  queryForm.page = 1
  loadHistory()
}

const getChangeText = (oldProb, newProb) => {
  if (!oldProb) {
    return newProb > 0 ? '+100.00%' : '0.00%'
  }
  const change = ((newProb - oldProb) / oldProb * 100).toFixed(2)
  return change > 0 ? `+${change}%` : `${change}%`
}

const getChangeClass = (oldProb, newProb) => {
  return newProb > oldProb ? 'change-increase' : 'change-decrease'
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped lang="scss">
.adjust-history {
  .change-increase {
    color: #67c23a;
    font-weight: bold;
  }
  
  .change-decrease {
    color: #f56c6c;
    font-weight: bold;
  }
  
  :deep(.el-pagination) {
    display: flex;
  }
}
</style>

