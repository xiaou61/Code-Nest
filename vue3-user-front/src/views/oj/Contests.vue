<template>
  <div class="oj-contests cn-learn-shell">
    <div class="cn-learn-shell__inner">
      <section class="cn-learn-hero cn-wave-reveal">
        <div class="cn-learn-hero__content">
          <span class="cn-learn-hero__eyebrow">Contest Hub</span>
          <h1 class="cn-learn-hero__title">赛事中心</h1>
          <p class="cn-learn-hero__desc">集中查看周赛与挑战赛，报名后即可进入比赛题单并参与实时排名。</p>
        </div>
        <div class="cn-learn-hero__meta">
          <span class="cn-learn-chip">总赛事 {{ total }}</span>
          <span class="cn-learn-chip">进行中 {{ runningCount }}</span>
          <span class="cn-learn-chip">当前页 {{ queryForm.pageNum }}</span>
        </div>
      </section>

      <div class="cn-learn-layout contests-layout">
        <aside class="cn-learn-sidebar contests-sidebar">
          <div class="cn-learn-panel cn-learn-float cn-learn-reveal filter-panel">
            <div class="panel-title">
              <el-icon><Search /></el-icon>
              <span>筛选条件</span>
            </div>
            <el-input
              v-model="keywordInput"
              placeholder="搜索赛事标题"
              clearable
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />
            <el-select v-model="queryForm.contestType" placeholder="赛事类型" clearable @change="handleSearch">
              <el-option label="周赛" value="weekly" />
              <el-option label="挑战赛" value="challenge" />
            </el-select>
            <el-select v-model="queryForm.status" placeholder="赛事状态" clearable @change="handleSearch">
              <el-option label="即将开始" :value="1" />
              <el-option label="进行中" :value="2" />
              <el-option label="已结束" :value="3" />
            </el-select>
            <div class="filter-actions">
              <el-button type="primary" @click="handleSearch">搜索</el-button>
              <el-button @click="handleReset">
                <el-icon><Refresh /></el-icon>
                重置
              </el-button>
            </div>
          </div>
        </aside>

        <main class="cn-learn-main">
          <div class="cn-learn-panel cn-learn-reveal list-panel" v-loading="loading">
            <div class="list-grid" v-if="contestList.length > 0">
              <article
                v-for="contest in contestList"
                :key="contest.id"
                class="contest-card cn-learn-float cn-learn-shine"
              >
                <div class="contest-card__header">
                  <el-tag size="small" effect="dark" :type="contest.contestType === 'weekly' ? 'success' : 'warning'">
                    {{ getContestTypeLabel(contest.contestType) }}
                  </el-tag>
                  <el-tag size="small" :type="getContestStatusTag(contest.status)">
                    {{ getContestStatusLabel(contest.status) }}
                  </el-tag>
                </div>
                <h3 class="contest-card__title">{{ contest.title }}</h3>
                <p class="contest-card__desc">{{ contest.description || '暂无赛事描述' }}</p>
                <div class="contest-card__meta">
                  <span class="meta-item">
                    <el-icon><Calendar /></el-icon>
                    {{ contest.startTime || '--' }} ~ {{ contest.endTime || '--' }}
                  </span>
                  <span class="meta-item">
                    <el-icon><List /></el-icon>
                    {{ contest.problemCount }} 题
                  </span>
                  <span class="meta-item">
                    <el-icon><UserFilled /></el-icon>
                    {{ contest.participantCount }} 人
                  </span>
                </div>
                <div class="contest-card__actions">
                  <el-button type="primary" plain @click="openContestDetail(contest)">
                    <el-icon><ArrowRight /></el-icon>
                    查看详情
                  </el-button>
                  <el-button
                    type="success"
                    :loading="joiningId === contest.id"
                    :disabled="!canJoin(contest)"
                    @click="handleJoin(contest)"
                  >
                    {{ canJoin(contest) ? '立即报名' : '不可报名' }}
                  </el-button>
                </div>
              </article>
            </div>

            <el-empty v-else-if="!loading" description="暂无赛事数据" :image-size="110" />
          </div>

          <div class="pagination-wrapper cn-learn-reveal" v-if="total > 0">
            <el-pagination
              v-model:current-page="queryForm.pageNum"
              v-model:page-size="queryForm.pageSize"
              :page-sizes="[6, 12, 18]"
              :total="total"
              layout="total, sizes, prev, pager, next"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Calendar, List, Refresh, Search, UserFilled } from '@element-plus/icons-vue'
import { ojApi } from '@/api/oj'
import {
  adaptContestList,
  getContestStatusLabel,
  getContestStatusTag,
  getContestTypeLabel
} from '@/utils/oj-contest-adapter'
import { useRevealMotion } from '@/utils/reveal-motion'

const router = useRouter()
useRevealMotion('.oj-contests .cn-learn-reveal')

const loading = ref(false)
const total = ref(0)
const contestList = ref([])
const joiningId = ref(null)
const keywordInput = ref('')

const queryForm = reactive({
  pageNum: 1,
  pageSize: 12,
  keyword: '',
  contestType: '',
  status: null
})

const runningCount = computed(() => contestList.value.filter((item) => item.status === 2).length)

const loadContests = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: queryForm.pageNum,
      pageSize: queryForm.pageSize
    }
    if (queryForm.keyword) params.keyword = queryForm.keyword
    if (queryForm.contestType) params.contestType = queryForm.contestType
    if (queryForm.status != null) params.status = queryForm.status

    const data = await ojApi.getContestList(params)
    const adapted = adaptContestList(data)
    contestList.value = adapted.records
    total.value = adapted.total
  } catch {
    contestList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryForm.keyword = keywordInput.value.trim()
  queryForm.pageNum = 1
  loadContests()
}

const handleReset = () => {
  keywordInput.value = ''
  queryForm.keyword = ''
  queryForm.contestType = ''
  queryForm.status = null
  queryForm.pageNum = 1
  loadContests()
}

const handleSizeChange = () => {
  queryForm.pageNum = 1
  loadContests()
}

const handleCurrentChange = () => {
  loadContests()
}

const openContestDetail = (contest) => {
  router.push(`/oj/contests/${contest.id}`)
}

const canJoin = (contest) => {
  return contest.status === 1 || contest.status === 2
}

const handleJoin = async (contest) => {
  if (!canJoin(contest)) {
    ElMessage.warning('当前赛事不可报名')
    return
  }
  joiningId.value = contest.id
  try {
    await ojApi.joinContest(contest.id)
    ElMessage.success('报名成功')
    loadContests()
  } catch (error) {
    const message = String(error?.message || '')
    if (/duplicate|已报名/i.test(message)) {
      ElMessage.info('你已报名该赛事')
      return
    }
  } finally {
    joiningId.value = null
  }
}

onMounted(() => {
  loadContests()
})
</script>

<style scoped>
.oj-contests {
  min-height: calc(100vh - 68px);
}

.contests-layout {
  align-items: flex-start;
}

.contests-sidebar {
  width: 296px;
}

.filter-panel {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--cn-text-primary);
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.list-panel {
  padding: 18px;
  min-height: 360px;
}

.list-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.contest-card {
  border-radius: 16px;
  border: 1px solid rgba(132, 171, 236, 0.26);
  background: #fff;
  box-shadow: 0 12px 28px rgba(26, 78, 148, 0.1);
  padding: 16px;
}

.contest-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.contest-card__title {
  margin: 0 0 8px;
  color: var(--cn-text-primary);
  font-size: 17px;
  line-height: 1.4;
}

.contest-card__desc {
  margin: 0 0 12px;
  min-height: 42px;
  color: var(--cn-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.contest-card__meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
  color: #6b7d9f;
  font-size: 12px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.contest-card__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 14px;
  padding: 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(115, 156, 225, 0.24);
  box-shadow: 0 18px 42px rgba(22, 63, 119, 0.12);
}

@media (max-width: 1200px) {
  .list-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .contests-layout {
    flex-direction: column;
  }

  .contests-sidebar {
    width: 100%;
  }

  .contest-card__actions {
    justify-content: flex-start;
  }
}
</style>
