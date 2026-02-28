<template>
  <div class="oj-ranking">
    <div class="page-header">
      <el-button text @click="$router.push('/oj')">
        <el-icon><ArrowLeft /></el-icon>
        返回题目列表
      </el-button>
      <h2>排行榜</h2>
    </div>

    <el-card shadow="never" class="ranking-card">
      <template #header>
        <div class="card-header">
          <el-radio-group v-model="rankType" size="small" @change="loadRanking">
            <el-radio-button value="all">总榜</el-radio-button>
            <el-radio-button value="weekly">周榜</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="rankingList"
        style="width: 100%"
        :row-class-name="rowClassName"
      >
        <el-table-column label="排名" width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.rank === 1" class="medal gold">🥇</span>
            <span v-else-if="row.rank === 2" class="medal silver">🥈</span>
            <span v-else-if="row.rank === 3" class="medal bronze">🥉</span>
            <span v-else class="rank-num">{{ row.rank }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="row.avatar" class="user-avatar">
                {{ (row.nickname || '')[0] || '?' }}
              </el-avatar>
              <span class="user-name">{{ row.nickname }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="通过题数" width="120" align="center">
          <template #default="{ row }">
            <span class="ac-count">{{ row.acceptedCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="提交次数" width="120" align="center">
          <template #default="{ row }">
            <span class="text-muted">{{ row.submissionCount }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && rankingList.length === 0" description="暂无排名数据" :image-size="100" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ojApi } from '@/api/oj'

const rankType = ref('all')
const rankingList = ref([])
const loading = ref(false)
const currentUserId = ref(null)

// 尝试获取当前用户ID
try {
  const userStr = localStorage.getItem('user') || sessionStorage.getItem('user')
  if (userStr) {
    const user = JSON.parse(userStr)
    currentUserId.value = user.id || user.userId
  }
} catch {}

const loadRanking = async () => {
  loading.value = true
  try {
    rankingList.value = await ojApi.getRanking(rankType.value) || []
  } catch {
    rankingList.value = []
  } finally {
    loading.value = false
  }
}

const rowClassName = ({ row }) => {
  if (currentUserId.value && row.userId === currentUserId.value) {
    return 'current-user-row'
  }
  return ''
}

onMounted(() => loadRanking())
</script>

<style scoped>
.oj-ranking {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
}

.ranking-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: center;
}

.medal {
  font-size: 22px;
}

.rank-num {
  font-size: 15px;
  font-weight: 600;
  color: #6b7280;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 14px;
}

.user-name {
  font-weight: 500;
  color: #1f2937;
}

.ac-count {
  font-weight: 700;
  color: #10b981;
  font-size: 16px;
}

.text-muted {
  color: #9ca3af;
  font-size: 13px;
}

:deep(.current-user-row) {
  background: #eff6ff !important;
}

:deep(.current-user-row) .user-name {
  color: #2563eb;
  font-weight: 600;
}
</style>
