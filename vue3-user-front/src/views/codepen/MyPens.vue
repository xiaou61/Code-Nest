<template>
  <CnPage class="my-pens-page" max-width="1320px" full-height>
    <CnPageHeader
      title="我的作品"
      description="管理已发布作品、草稿和付费 Fork 收益，快速回到编辑器继续打磨。"
      eyebrow="MY CODEPEN"
      :breadcrumbs="[{ label: '首页', to: '/' }, { label: '代码广场', to: '/codepen' }, { label: '我的作品' }]"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">{{ allPens.length }} 个作品</CnStatusTag>
        <CnStatusTag type="warning" size="sm" subtle>{{ draftList.length }} 个草稿</CnStatusTag>
      </template>

      <template #actions>
        <el-button icon="TrendCharts" @click="showIncomeStats">收益统计</el-button>
        <el-button type="primary" icon="Plus" @click="createNewPen">创建作品</el-button>
      </template>
    </CnPageHeader>

    <section class="my-pens-stats" aria-label="我的作品概览">
      <CnStatCard
        title="已发布"
        :value="allPens.length"
        unit="个"
        description="当前已发布作品"
        tone="brand"
        trend="flat"
        trend-text="作品"
      />
      <CnStatCard
        title="草稿箱"
        :value="draftList.length"
        unit="个"
        description="待继续编辑"
        tone="warning"
        trend="flat"
        trend-text="草稿"
      />
      <CnStatCard
        title="付费作品"
        :value="paidPens.length"
        unit="个"
        description="可产生 Fork 收益"
        tone="success"
        trend="flat"
        trend-text="收益"
      />
      <CnStatCard
        title="当前视图"
        :value="activeTabLabel"
        description="Tabs 切换后的列表"
        tone="neutral"
        trend="flat"
        trend-text="视图"
      />
    </section>

    <CnSection title="作品列表" description="按发布状态查看作品，点击卡片进入详情或编辑。" divided>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="全部作品" name="all">
        <div class="pen-list" v-loading="loading">
          <div class="pen-grid">
            <PenCard
              v-for="pen in penList"
              :key="pen.id"
              :pen="pen"
              @click="viewPen(pen.id)"
            />
          </div>

          <CnEmptyState
            v-if="!loading && penList.length === 0"
            title="还没有作品"
            description="创建一个作品后，它会出现在这里。"
            icon="CP"
            size="sm"
            surface="plain"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="草稿箱" name="draft">
        <div class="pen-list" v-loading="loading">
          <div class="pen-grid">
            <PenCard
              v-for="pen in draftList"
              :key="pen.id"
              :pen="pen"
              @click="editPen(pen.id)"
            />
          </div>

          <CnEmptyState
            v-if="!loading && draftList.length === 0"
            title="暂无草稿"
            description="保存为草稿的作品会出现在这里。"
            icon="DR"
            size="sm"
            surface="plain"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="付费作品" name="paid">
        <div class="pen-list" v-loading="loading">
          <div class="pen-grid">
            <div
              v-for="pen in paidPens"
              :key="pen.id"
              class="paid-pen-card"
              @click="viewPen(pen.id)"
            >
              <PenCard :pen="pen" />
              <div class="income-badge">
                收益: {{ pen.totalIncome || 0 }} 积分
              </div>
            </div>
          </div>

          <CnEmptyState
            v-if="!loading && paidPens.length === 0"
            title="暂无付费作品"
            description="设置 Fork 价格后，付费作品会在这里汇总。"
            icon="IN"
            size="sm"
            surface="plain"
          />
        </div>
      </el-tab-pane>
      </el-tabs>
    </CnSection>

    <el-dialog
      v-model="showIncomeDialog"
      title="收益统计"
      width="600px"
    >
      <div class="income-stats" v-loading="loadingStats">
        <div class="income-stat-grid">
          <CnStatCard
            title="总收益"
            :value="incomeStats.totalIncome || 0"
            unit="积分"
            description="累计 Fork 收益"
            tone="success"
          />
          <CnStatCard
            title="付费作品数"
            :value="incomeStats.paidPensCount || 0"
            description="已设置价格的作品"
            tone="brand"
          />
          <CnStatCard
            title="总 Fork 次数"
            :value="incomeStats.totalForkCount || 0"
            description="全部付费 Fork 统计"
            tone="info"
          />
        </div>

        <div class="income-details" v-if="incomeStats.details && incomeStats.details.length > 0">
          <h3>作品收益详情</h3>
          <el-table :data="incomeStats.details" class="income-table">
            <el-table-column prop="title" label="作品标题" />
            <el-table-column prop="forkPrice" label="Fork价格" width="100">
              <template #default="{ row }">
                {{ row.forkPrice }} 积分
              </template>
            </el-table-column>
            <el-table-column prop="forkCount" label="Fork次数" width="100" />
            <el-table-column prop="totalIncome" label="收益" width="100">
              <template #default="{ row }">
                {{ row.totalIncome }} 积分
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { codepenApi } from '@/api/codepen'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import PenCard from './components/PenCard.vue'

const router = useRouter()

// 标签页
const activeTab = ref('all')
const loading = ref(false)

// 作品列表
const allPens = ref([])
const draftList = ref([])

// 计算付费作品
const paidPens = computed(() => {
  return allPens.value.filter(pen => !pen.isFree)
})

// 当前显示的列表
const penList = computed(() => {
  if (activeTab.value === 'draft') {
    return draftList.value
  } else if (activeTab.value === 'paid') {
    return paidPens.value
  }
  return allPens.value
})

const activeTabLabel = computed(() => {
  const map = {
    all: '全部作品',
    draft: '草稿箱',
    paid: '付费作品'
  }
  return map[activeTab.value] || '全部作品'
})

// 收益统计
const showIncomeDialog = ref(false)
const loadingStats = ref(false)
const incomeStats = ref({})

// 创建新作品
const createNewPen = () => {
  router.push('/codepen/editor')
}

// 查看作品
const viewPen = (id) => {
  router.push(`/codepen/${id}`)
}

// 编辑作品
const editPen = (id) => {
  router.push(`/codepen/editor/${id}`)
}

// 标签页切换
const handleTabChange = (tab) => {
  if (tab === 'draft' && draftList.value.length === 0) {
    loadDrafts()
  }
}

// 加载全部作品
const loadAllPens = async () => {
  try {
    loading.value = true
    allPens.value = await codepenApi.getMyPens({ status: 1 })
  } catch (error) {
    console.error('加载作品失败:', error)
    ElMessage.error('加载作品失败')
  } finally {
    loading.value = false
  }
}

// 加载草稿
const loadDrafts = async () => {
  try {
    loading.value = true
    draftList.value = await codepenApi.getMyDrafts()
  } catch (error) {
    console.error('加载草稿失败:', error)
  } finally {
    loading.value = false
  }
}

// 显示收益统计
const showIncomeStats = async () => {
  showIncomeDialog.value = true
  try {
    loadingStats.value = true
    incomeStats.value = await codepenApi.getIncomeStats()
  } catch (error) {
    console.error('加载收益统计失败:', error)
  } finally {
    loadingStats.value = false
  }
}

// 初始化
onMounted(() => {
  loadAllPens()
})
</script>

<style scoped lang="scss">
.my-pens-page {
  min-height: calc(100vh - 68px);
}

.my-pens-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
  margin-bottom: var(--cn-space-4);
}

.pen-list {
  min-height: 340px;
}

.pen-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--cn-space-4);
  margin-bottom: var(--cn-space-5);
}

.paid-pen-card {
  position: relative;
  cursor: pointer;
}

.income-badge {
  position: absolute;
  top: var(--cn-space-3);
  right: var(--cn-space-3);
  z-index: 10;
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 var(--cn-space-3);
  border: 1px solid color-mix(in srgb, var(--cn-color-warning) 26%, var(--cn-color-border-subtle));
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-warning-soft);
  color: var(--cn-color-warning);
  font-size: 12px;
  font-weight: 700;
}

.income-stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-3);
}

.income-details {
  margin-top: var(--cn-space-5);
}

.income-details h3 {
  margin: 0 0 var(--cn-space-3);
  color: var(--cn-color-text-primary);
  font-size: 16px;
  font-weight: 650;
}

.income-table {
  width: 100%;
}

@media (max-width: 992px) {
  .my-pens-stats,
  .income-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .my-pens-stats,
  .income-stat-grid,
  .pen-grid {
    grid-template-columns: 1fr;
  }
}
</style>

