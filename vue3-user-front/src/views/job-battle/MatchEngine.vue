<template>
  <div class="job-match-engine-page cn-learn-shell">
    <el-card class="hero-card" shadow="never">
      <div class="hero-head">
        <div>
          <h2>岗位匹配引擎 2.0</h2>
          <p>一次输入简历，批量评估多个岗位 JD，输出可解释评分、差距优先级与下一步动作。</p>
        </div>
        <div class="hero-actions">
          <el-button @click="fillExample">填充示例</el-button>
          <el-button type="primary" :loading="running" @click="runAnalysis">开始分析</el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" class="section-row">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="form-card">
          <template #header>
            <div class="card-header">
              <span>输入区</span>
            </div>
          </template>

          <el-form label-position="top">
            <el-form-item label="简历文本">
              <el-input
                v-model="form.resumeText"
                type="textarea"
                :rows="8"
                placeholder="粘贴简历正文，建议包含项目经历和量化结果。"
              />
            </el-form-item>

            <el-form-item label="项目亮点（可选）">
              <el-input
                v-model="form.projectHighlights"
                type="textarea"
                :rows="3"
                placeholder="例如：主导百万级并发链路优化，接口 P99 延迟下降 42%。"
              />
            </el-form-item>

            <el-form-item label="目标公司类型（可选）">
              <el-select v-model="form.targetCompanyType" clearable style="width: 260px">
                <el-option label="互联网" value="互联网" />
                <el-option label="外企" value="外企" />
                <el-option label="国企/央企" value="国企/央企" />
                <el-option label="创业公司" value="创业公司" />
              </el-select>
            </el-form-item>
          </el-form>

          <div class="target-toolbar">
            <h3>目标岗位列表</h3>
            <el-button type="primary" plain @click="addTarget">新增岗位</el-button>
          </div>

          <div class="target-list">
            <el-card v-for="(target, idx) in form.targets" :key="idx" shadow="hover" class="target-item">
              <template #header>
                <div class="target-head">
                  <span>岗位 {{ idx + 1 }}</span>
                  <el-button v-if="form.targets.length > 1" type="danger" link @click="removeTarget(idx)">
                    删除
                  </el-button>
                </div>
              </template>

              <el-row :gutter="12">
                <el-col :xs="24" :sm="8">
                  <el-form-item label="岗位名称（可选）">
                    <el-input v-model="target.targetRole" placeholder="如：Java后端开发" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="8">
                  <el-form-item label="级别（可选）">
                    <el-input v-model="target.targetLevel" placeholder="如：P6 / 资深" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="8">
                  <el-form-item label="城市（可选）">
                    <el-input v-model="target.city" placeholder="如：上海" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item label="岗位JD">
                <el-input
                  v-model="target.jdText"
                  type="textarea"
                  :rows="6"
                  placeholder="粘贴岗位JD全文。"
                />
              </el-form-item>
            </el-card>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="tips-card">
          <template #header>
            <div class="card-header">
              <span>使用建议</span>
            </div>
          </template>
          <ol class="tips-list">
            <li>优先放 3-5 个目标岗位，能看到“投递优先级”。</li>
            <li>简历里尽量包含量化结果，评分稳定性更高。</li>
            <li>关注 P0/P1 差距项，先补核心短板再扩展。</li>
            <li>分析完成后可回到作战台继续生成冲刺计划。</li>
          </ol>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="analysisResult" shadow="never" class="result-card">
      <template #header>
        <div class="card-header">
          <span>分析结果</span>
          <el-tag type="success">{{ analysisResult.analysisName }}</el-tag>
        </div>
      </template>

      <el-row :gutter="12" class="summary-grid">
        <el-col :xs="12" :sm="6">
          <div class="summary-item">
            <div class="label">最佳匹配分</div>
            <div class="value">{{ analysisResult.bestScore }}</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="summary-item">
            <div class="label">平均匹配分</div>
            <div class="value">{{ analysisResult.averageScore }}</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="summary-item">
            <div class="label">目标岗位数</div>
            <div class="value">{{ analysisResult.targetCount }}</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="summary-item">
            <div class="label">降级岗位数</div>
            <div class="value">{{ analysisResult.fallbackCount }}</div>
          </div>
        </el-col>
      </el-row>

      <div class="next-actions">
        <h4>推荐下一步</h4>
        <el-tag
          v-for="(item, idx) in analysisResult.nextActions || []"
          :key="idx"
          effect="plain"
          class="action-tag"
        >
          {{ item }}
        </el-tag>
      </div>

      <el-table :data="analysisResult.ranking || []" border style="width: 100%">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-panel">
              <div class="expand-block">
                <h5>关键优势</h5>
                <el-tag v-for="(s, i) in row.strengths || []" :key="`s-${i}`" class="mini-tag">{{ s }}</el-tag>
              </div>
              <div class="expand-block">
                <h5>缺失关键词</h5>
                <el-tag
                  v-for="(k, i) in row.missingKeywords || []"
                  :key="`k-${i}`"
                  type="warning"
                  class="mini-tag"
                >
                  {{ k }}
                </el-tag>
              </div>
              <div class="expand-block">
                <h5>差距项（Top3）</h5>
                <div v-for="(g, i) in row.topGaps || []" :key="`g-${i}`" class="gap-item">
                  <el-tag size="small" :type="g.priority === 'P0' ? 'danger' : (g.priority === 'P1' ? 'warning' : 'info')">
                    {{ g.priority || 'P2' }}
                  </el-tag>
                  <span class="gap-skill">{{ g.skill }}</span>
                  <span class="gap-action">{{ g.suggestedAction }}</span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="rank" label="#" width="54" />
        <el-table-column prop="targetRole" label="岗位" min-width="160" />
        <el-table-column prop="targetLevel" label="级别" min-width="100" />
        <el-table-column prop="city" label="城市" min-width="90" />
        <el-table-column label="引擎分" min-width="140">
          <template #default="{ row }">
            <el-progress :percentage="row.engineScore || 0" :stroke-width="10" :show-text="false" />
            <span class="score-text">{{ row.engineScore }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="overallScore" label="匹配总分" width="98" />
        <el-table-column prop="estimatedPassRate" label="通过率" width="88" />
        <el-table-column label="P0/P1" width="88">
          <template #default="{ row }">{{ row.p0GapCount || 0 }}/{{ row.p1GapCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="降级" width="70">
          <template #default="{ row }">
            <el-tag :type="row.fallback ? 'warning' : 'success'">{{ row.fallback ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="history-card">
      <template #header>
        <div class="card-header">
          <span>历史分析</span>
          <div>
            <el-input
              v-model="historyQuery.keyword"
              placeholder="搜索分析名称/岗位"
              clearable
              style="width: 220px"
              @keyup.enter="loadHistory"
            />
          </div>
        </div>
      </template>

      <el-table v-loading="loadingHistory" :data="historyList" border>
        <el-table-column prop="id" label="ID" width="76" />
        <el-table-column prop="analysisName" label="分析名称" min-width="220" />
        <el-table-column prop="bestTargetRole" label="最佳岗位" min-width="140" />
        <el-table-column prop="bestScore" label="最佳分" width="90" />
        <el-table-column prop="targetCount" label="岗位数" width="80" />
        <el-table-column prop="createTime" label="时间" min-width="170" />
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button type="primary" link @click="applyHistory(row.id)">加载</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="historyQuery.pageNum"
          v-model:page-size="historyQuery.pageSize"
          :total="historyTotal"
          layout="total, prev, pager, next"
          @current-change="loadHistory"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { jobBattleApi } from '@/api/jobBattle'

const running = ref(false)
const loadingHistory = ref(false)
const analysisResult = ref(null)
const historyList = ref([])
const historyTotal = ref(0)

const historyQuery = reactive({
  keyword: '',
  pageNum: 1,
  pageSize: 8
})

const form = reactive({
  resumeText: '',
  projectHighlights: '',
  targetCompanyType: '互联网',
  targets: [createTarget()]
})

function createTarget() {
  return {
    targetRole: '',
    targetLevel: '',
    city: '',
    jdText: ''
  }
}

function addTarget() {
  if (form.targets.length >= 10) {
    ElMessage.warning('单次最多添加10个岗位')
    return
  }
  form.targets.push(createTarget())
}

function removeTarget(index) {
  form.targets.splice(index, 1)
}

function validateForm() {
  if (!form.resumeText || !form.resumeText.trim()) {
    ElMessage.warning('请先输入简历文本')
    return false
  }
  const invalid = form.targets.findIndex(item => !item.jdText || !item.jdText.trim())
  if (invalid !== -1) {
    ElMessage.warning(`请补全岗位${invalid + 1}的JD文本`)
    return false
  }
  return true
}

async function runAnalysis() {
  if (!validateForm()) return
  running.value = true
  try {
    const payload = {
      resumeText: form.resumeText.trim(),
      projectHighlights: form.projectHighlights?.trim() || '',
      targetCompanyType: form.targetCompanyType || '',
      targets: form.targets.map(item => ({
        targetRole: item.targetRole?.trim() || '',
        targetLevel: item.targetLevel?.trim() || '',
        city: item.city?.trim() || '',
        jdText: item.jdText?.trim() || ''
      }))
    }
    const data = await jobBattleApi.runMatchEngine(payload)
    analysisResult.value = data
    ElMessage.success('岗位匹配分析完成')
    loadHistory()
  } catch (error) {
    console.error('岗位匹配分析失败', error)
    ElMessage.error('岗位匹配分析失败')
  } finally {
    running.value = false
  }
}

async function loadHistory() {
  loadingHistory.value = true
  try {
    const data = await jobBattleApi.getMatchEngineHistory({
      keyword: historyQuery.keyword,
      pageNum: historyQuery.pageNum,
      pageSize: historyQuery.pageSize
    })
    historyList.value = data?.records || []
    historyTotal.value = data?.total || 0
  } catch (error) {
    console.error('获取历史记录失败', error)
    ElMessage.error('获取历史记录失败')
  } finally {
    loadingHistory.value = false
  }
}

async function applyHistory(id) {
  try {
    const data = await jobBattleApi.getMatchEngineHistoryDetail(id)
    analysisResult.value = data
    ElMessage.success('已加载历史分析结果')
  } catch (error) {
    console.error('加载历史详情失败', error)
    ElMessage.error('加载历史详情失败')
  }
}

async function loadLatest() {
  try {
    const data = await jobBattleApi.getLatestMatchEngineResult()
    if (data) {
      analysisResult.value = data
    }
  } catch (error) {
    console.warn('加载最近分析结果失败', error)
  }
}

function fillExample() {
  form.resumeText = `5年Java后端开发经验，负责支付与订单核心链路。熟悉 Spring Boot、MySQL、Redis、Kafka，主导过性能优化与容灾演练。最近一年负责网关限流与缓存策略改造，接口P99从380ms降到210ms。`
  form.projectHighlights = '负责高并发交易系统优化，参与服务拆分和链路压测，推动故障演练制度化。'
  form.targetCompanyType = '互联网'
  form.targets = [
    {
      targetRole: 'Java后端开发',
      targetLevel: '资深',
      city: '上海',
      jdText: '负责交易系统后端研发，要求熟悉 Java、Spring Boot、MySQL、Redis、Kafka，具备高并发和分布式架构经验，有性能优化实战经验。'
    },
    {
      targetRole: '后端工程师',
      targetLevel: 'P6',
      city: '杭州',
      jdText: '负责平台中台服务开发，要求 Java 基础扎实，熟悉微服务治理、缓存策略、数据库调优，具备跨团队沟通能力。'
    }
  ]
}

onMounted(async () => {
  await Promise.all([loadLatest(), loadHistory()])
})
</script>

<style scoped>
.job-match-engine-page {
  max-width: 1320px;
  margin: 0 auto;
  padding: 12px;
}

.section-row {
  margin-top: 12px;
}

.hero-card {
  border-radius: 16px;
}

.hero-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.hero-head h2 {
  margin: 0;
  font-size: 26px;
  color: #123a6f;
}

.hero-head p {
  margin: 8px 0 0;
  color: #4a6789;
}

.hero-actions {
  display: flex;
  gap: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.form-card,
.tips-card,
.result-card,
.history-card {
  border-radius: 14px;
}

.target-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.target-toolbar h3 {
  margin: 0;
  font-size: 16px;
  color: #1b3e6e;
}

.target-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.target-item {
  border-radius: 12px;
}

.target-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tips-list {
  margin: 0;
  padding-left: 18px;
  color: #4a6789;
  line-height: 1.8;
}

.result-card,
.history-card {
  margin-top: 14px;
}

.summary-grid {
  margin-bottom: 10px;
}

.summary-item {
  border-radius: 10px;
  padding: 12px;
  background: linear-gradient(135deg, #f5f9ff 0%, #ecf4ff 100%);
}

.summary-item .label {
  color: #6481a4;
  font-size: 13px;
}

.summary-item .value {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 700;
  color: #1c4f90;
}

.next-actions {
  margin: 4px 0 12px;
}

.next-actions h4 {
  margin: 0 0 8px;
  color: #1b3e6e;
}

.action-tag {
  margin-right: 8px;
  margin-bottom: 8px;
}

.score-text {
  margin-left: 8px;
  font-weight: 600;
  color: #1c4f90;
}

.expand-panel {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.expand-block h5 {
  margin: 0 0 8px;
  color: #1b3e6e;
}

.mini-tag {
  margin-right: 6px;
  margin-bottom: 6px;
}

.gap-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 8px;
  border-left: 3px solid #dce8f9;
  padding-left: 8px;
}

.gap-skill {
  font-weight: 600;
  color: #214f86;
}

.gap-action {
  color: #537294;
  font-size: 13px;
}

.pagination-wrap {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>

