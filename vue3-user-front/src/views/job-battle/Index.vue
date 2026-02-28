<template>
  <div class="job-battle-page cn-learn-shell">
    <div class="cn-learn-shell__inner">
      <div class="page-header cn-learn-hero cn-wave-reveal">
        <div class="header-content">
          <span class="cn-learn-hero__eyebrow">Job Battle</span>
          <h1 class="page-title cn-learn-hero__title">
            <el-icon><Trophy /></el-icon>
            求职作战台（增强版）
          </h1>
          <p class="page-subtitle cn-learn-hero__desc">
            从 JD 解析到复盘闭环，一站式提升拿 Offer 概率
          </p>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="battle-tabs cn-learn-panel">
        <el-tab-pane label="1. JD解析" name="jd">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card class="form-card" shadow="never">
                <template #header>输入岗位JD</template>
                <el-form label-position="top">
                  <el-form-item label="JD内容">
                    <el-input
                      v-model="jdForm.jdText"
                      type="textarea"
                      :rows="12"
                      placeholder="粘贴岗位JD全文"
                    />
                  </el-form-item>
                  <el-row :gutter="12">
                    <el-col :span="8">
                      <el-form-item label="目标岗位">
                        <el-input v-model="jdForm.targetRole" placeholder="如 Java后端" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="目标级别">
                        <el-input v-model="jdForm.targetLevel" placeholder="如 中级" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="城市">
                        <el-input v-model="jdForm.city" placeholder="如 深圳" />
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-button type="primary" :loading="loading.jd" @click="handleParseJd">
                    解析JD
                  </el-button>
                </el-form>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card class="result-card" shadow="never">
                <template #header>
                  结构化结果
                  <el-tag v-if="jdResult" size="small" :type="jdResult.fallback ? 'warning' : 'success'" class="ml-8">
                    {{ jdResult.fallback ? '降级结果' : 'AI结果' }}
                  </el-tag>
                </template>
                <div v-if="jdResult" class="result-block">
                  <div class="result-item"><b>岗位：</b>{{ jdResult.jobTitle || '-' }}</div>
                  <div class="result-item"><b>级别：</b>{{ jdResult.level || '-' }}</div>
                  <div class="result-item"><b>年限：</b>{{ jdResult.seniorityYears || '-' }}</div>
                  <div class="result-item"><b>必备技能：</b>{{ joinText(jdResult.mustSkills) }}</div>
                  <div class="result-item"><b>加分技能：</b>{{ joinText(jdResult.niceSkills) }}</div>
                  <div class="result-item"><b>关键词：</b>{{ joinText(jdResult.keywords) }}</div>
                  <div class="result-item"><b>风险点：</b>{{ joinText(jdResult.riskPoints) }}</div>
                  <div class="result-item"><b>摘要：</b>{{ jdResult.summary || '-' }}</div>
                  <div class="result-actions">
                    <el-button size="small" type="primary" plain @click="fillParsedJdToMatch">
                      用该结果进入简历匹配
                    </el-button>
                  </div>
                </div>
                <el-empty v-else description="暂无解析结果" />
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="2. 简历匹配" name="match">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card class="form-card" shadow="never">
                <template #header>输入简历信息</template>
                <el-form label-position="top">
                  <el-form-item label="结构化JD JSON">
                    <el-input
                      v-model="matchForm.parsedJdJson"
                      type="textarea"
                      :rows="7"
                      placeholder="可直接使用上一步结果"
                    />
                  </el-form-item>
                  <el-form-item label="简历内容">
                    <el-input
                      v-model="matchForm.resumeText"
                      type="textarea"
                      :rows="9"
                      placeholder="粘贴简历全文"
                    />
                  </el-form-item>
                  <el-row :gutter="12">
                    <el-col :span="12">
                      <el-form-item label="项目亮点（可选）">
                        <el-input v-model="matchForm.projectHighlights" placeholder="可补充重点项目" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="目标公司类型（可选）">
                        <el-input v-model="matchForm.targetCompanyType" placeholder="如 大厂" />
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-button type="primary" :loading="loading.match" @click="handleMatchResume">
                    评估匹配度
                  </el-button>
                </el-form>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card class="result-card" shadow="never">
                <template #header>
                  匹配评估结果
                  <el-tag v-if="matchResult" size="small" :type="matchResult.fallback ? 'warning' : 'success'" class="ml-8">
                    {{ matchResult.fallback ? '降级结果' : 'AI结果' }}
                  </el-tag>
                </template>
                <div v-if="matchResult" class="result-block">
                  <div class="result-item"><b>总体匹配分：</b>{{ matchResult.overallScore ?? '-' }}</div>
                  <div class="result-item"><b>预计通过率：</b>{{ matchResult.estimatedPassRate ?? '-' }}%</div>
                  <div class="result-item">
                    <b>维度评分：</b>
                    <span class="mono">{{ formatJson(matchResult.dimensionScores) }}</span>
                  </div>
                  <div class="result-item"><b>优势：</b>{{ joinText(matchResult.strengths) }}</div>
                  <div class="result-item"><b>缺失关键词：</b>{{ joinText(matchResult.missingKeywords) }}</div>
                  <div class="result-item">
                    <b>差距项：</b>
                    <pre class="json-view">{{ formatJson(matchResult.gaps) }}</pre>
                  </div>
                  <div class="result-item">
                    <b>改写建议：</b>
                    <pre class="json-view">{{ formatJson(matchResult.resumeRewriteSuggestions) }}</pre>
                  </div>
                  <div class="result-actions">
                    <el-button size="small" type="primary" plain @click="fillGapsToPlan">
                      用差距项生成计划
                    </el-button>
                  </div>
                </div>
                <el-empty v-else description="暂无评估结果" />
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="3. 30天计划" name="plan">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card class="form-card" shadow="never">
                <template #header>生成补短板计划</template>
                <el-form label-position="top">
                  <el-form-item label="差距项JSON">
                    <el-input
                      v-model="planForm.gapsJson"
                      type="textarea"
                      :rows="9"
                      placeholder="可直接使用上一步差距项"
                    />
                  </el-form-item>
                  <el-row :gutter="12">
                    <el-col :span="8">
                      <el-form-item label="计划天数">
                        <el-input-number v-model="planForm.targetDays" :min="7" :max="90" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="每周可投入小时">
                        <el-input-number v-model="planForm.weeklyHours" :min="1" :max="60" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="学习偏好">
                        <el-select v-model="planForm.preferredLearningMode" placeholder="可选">
                          <el-option label="混合" value="混合" />
                          <el-option label="实战" value="实战" />
                          <el-option label="文档" value="文档" />
                          <el-option label="视频" value="视频" />
                        </el-select>
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-form-item label="下一场面试日期（可选）">
                    <el-date-picker
                      v-model="planForm.nextInterviewDate"
                      type="date"
                      value-format="YYYY-MM-DD"
                      placeholder="选择日期"
                    />
                  </el-form-item>
                  <el-button type="primary" :loading="loading.plan" @click="handleGeneratePlan">
                    生成30天计划
                  </el-button>
                </el-form>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card class="result-card" shadow="never">
                <template #header>
                  行动计划结果
                  <el-tag v-if="planResult" size="small" :type="planResult.fallback ? 'warning' : 'success'" class="ml-8">
                    {{ planResult.fallback ? '降级结果' : 'AI结果' }}
                  </el-tag>
                </template>
                <div v-if="planResult" class="result-block">
                  <div class="result-item"><b>计划名：</b>{{ planResult.planName || '-' }}</div>
                  <div class="result-item"><b>总天数：</b>{{ planResult.totalDays || '-' }}</div>
                  <div class="result-item"><b>周目标：</b>{{ joinText(planResult.weeklyGoals, ' | ') }}</div>
                  <div class="result-item">
                    <b>里程碑：</b>
                    <pre class="json-view">{{ formatJson(planResult.milestones) }}</pre>
                  </div>
                  <div class="result-item">
                    <b>每日任务（预览）：</b>
                    <pre class="json-view">{{ formatJson(planResult.dailyTasks) }}</pre>
                  </div>
                  <div class="result-item"><b>风险与兜底：</b>{{ joinText(planResult.riskAndFallback) }}</div>
                </div>
                <el-empty v-else description="暂无计划结果" />
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="4. 面试复盘" name="review">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card class="form-card" shadow="never">
                <template #header>输入复盘信息</template>
                <el-form label-position="top">
                  <el-form-item label="面试记录">
                    <el-input
                      v-model="reviewForm.interviewNotes"
                      type="textarea"
                      :rows="8"
                      placeholder="记录本次面试问题、回答情况、反馈"
                    />
                  </el-form-item>
                  <el-form-item label="问答转写（可选）">
                    <el-input
                      v-model="reviewForm.qaTranscriptJson"
                      type="textarea"
                      :rows="6"
                      placeholder="可粘贴问答JSON或文本"
                    />
                  </el-form-item>
                  <el-row :gutter="12">
                    <el-col :span="8">
                      <el-form-item label="面试结果">
                        <el-select v-model="reviewForm.interviewResult">
                          <el-option label="通过" value="pass" />
                          <el-option label="未通过" value="reject" />
                          <el-option label="待定" value="pending" />
                        </el-select>
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="目标岗位">
                        <el-input v-model="reviewForm.targetRole" placeholder="如 Java后端" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="下一场面试日期">
                        <el-date-picker
                          v-model="reviewForm.nextInterviewDate"
                          type="date"
                          value-format="YYYY-MM-DD"
                          placeholder="可选"
                        />
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-button type="primary" :loading="loading.review" @click="handleReviewInterview">
                    生成复盘建议
                  </el-button>
                </el-form>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card class="result-card" shadow="never">
                <template #header>
                  复盘输出
                  <el-tag v-if="reviewResult" size="small" :type="reviewResult.fallback ? 'warning' : 'success'" class="ml-8">
                    {{ reviewResult.fallback ? '降级结果' : 'AI结果' }}
                  </el-tag>
                </template>
                <div v-if="reviewResult" class="result-block">
                  <div class="result-item"><b>结论：</b>{{ reviewResult.overallConclusion || '-' }}</div>
                  <div class="result-item"><b>根因：</b>{{ joinText(reviewResult.rootCauses) }}</div>
                  <div class="result-item"><b>信心分：</b>{{ reviewResult.confidenceScore ?? '-' }}</div>
                  <div class="result-item">
                    <b>高影响修复动作：</b>
                    <pre class="json-view">{{ formatJson(reviewResult.highImpactFixes) }}</pre>
                  </div>
                  <div class="result-item">
                    <b>题型短板：</b>
                    <pre class="json-view">{{ formatJson(reviewResult.questionTypeWeakness) }}</pre>
                  </div>
                  <div class="result-item"><b>7天计划：</b>{{ joinText(reviewResult.next7DayPlan, ' | ') }}</div>
                </div>
                <el-empty v-else description="暂无复盘结果" />
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Trophy } from '@element-plus/icons-vue'
import { jobBattleApi } from '@/api/jobBattle'

const activeTab = ref('jd')
const loading = reactive({
  jd: false,
  match: false,
  plan: false,
  review: false
})

const jdForm = reactive({
  jdText: '',
  targetRole: '',
  targetLevel: '',
  city: ''
})

const matchForm = reactive({
  parsedJdJson: '',
  resumeText: '',
  projectHighlights: '',
  targetCompanyType: ''
})

const planForm = reactive({
  gapsJson: '',
  targetDays: 30,
  weeklyHours: 6,
  preferredLearningMode: '',
  nextInterviewDate: ''
})

const reviewForm = reactive({
  interviewNotes: '',
  qaTranscriptJson: '',
  interviewResult: 'pending',
  targetRole: '',
  nextInterviewDate: ''
})

const jdResult = ref(null)
const matchResult = ref(null)
const planResult = ref(null)
const reviewResult = ref(null)

const handleParseJd = async () => {
  if (!jdForm.jdText.trim()) {
    ElMessage.warning('请先输入JD内容')
    return
  }
  loading.jd = true
  try {
    const data = await jobBattleApi.parseJd(jdForm)
    jdResult.value = data
    ElMessage.success('JD解析完成')
  } catch (e) {
    console.error('JD解析失败', e)
  } finally {
    loading.jd = false
  }
}

const handleMatchResume = async () => {
  if (!matchForm.parsedJdJson.trim()) {
    ElMessage.warning('请先填写结构化JD JSON')
    return
  }
  if (!matchForm.resumeText.trim()) {
    ElMessage.warning('请先填写简历内容')
    return
  }
  loading.match = true
  try {
    const data = await jobBattleApi.matchResume(matchForm)
    matchResult.value = data
    ElMessage.success('简历匹配完成')
  } catch (e) {
    console.error('简历匹配失败', e)
  } finally {
    loading.match = false
  }
}

const handleGeneratePlan = async () => {
  if (!planForm.gapsJson.trim()) {
    ElMessage.warning('请先填写差距项JSON')
    return
  }
  loading.plan = true
  try {
    const data = await jobBattleApi.generatePlan(planForm)
    planResult.value = data
    ElMessage.success('计划生成完成')
  } catch (e) {
    console.error('计划生成失败', e)
  } finally {
    loading.plan = false
  }
}

const handleReviewInterview = async () => {
  if (!reviewForm.interviewNotes.trim()) {
    ElMessage.warning('请先填写面试记录')
    return
  }
  if (!reviewForm.targetRole.trim()) {
    ElMessage.warning('请先填写目标岗位')
    return
  }
  loading.review = true
  try {
    const data = await jobBattleApi.reviewInterview(reviewForm)
    reviewResult.value = data
    ElMessage.success('复盘生成完成')
  } catch (e) {
    console.error('复盘生成失败', e)
  } finally {
    loading.review = false
  }
}

const fillParsedJdToMatch = () => {
  if (!jdResult.value) {
    return
  }
  matchForm.parsedJdJson = JSON.stringify(jdResult.value, null, 2)
  if (!matchForm.targetCompanyType) {
    matchForm.targetCompanyType = '大厂'
  }
  activeTab.value = 'match'
}

const fillGapsToPlan = () => {
  const gaps = matchResult.value?.gaps || []
  planForm.gapsJson = JSON.stringify(gaps, null, 2)
  activeTab.value = 'plan'
}

const joinText = (arr, sep = '、') => {
  if (!Array.isArray(arr) || arr.length === 0) {
    return '-'
  }
  return arr.join(sep)
}

const formatJson = (value) => {
  if (value == null) {
    return '-'
  }
  try {
    return JSON.stringify(value, null, 2)
  } catch (e) {
    return String(value)
  }
}
</script>

<style scoped>
.job-battle-page {
  min-height: calc(100vh - 68px);
}

.page-header {
  margin-bottom: 20px;
  border-radius: 20px;
}

.page-title {
  margin: 0 0 10px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-subtitle {
  margin: 0;
  opacity: 0.88;
}

.battle-tabs {
  border-radius: 16px;
  padding: 12px 16px 16px;
}

.form-card,
.result-card {
  min-height: 680px;
  border-radius: 14px;
}

.result-block {
  font-size: 13px;
  line-height: 1.7;
}

.result-item {
  margin-bottom: 10px;
  color: #46556e;
}

.result-actions {
  margin-top: 14px;
}

.json-view {
  margin: 8px 0 0;
  padding: 10px;
  border-radius: 8px;
  max-height: 190px;
  overflow: auto;
  border: 1px solid #dce8fb;
  background: #f8fbff;
  color: #33475f;
  white-space: pre-wrap;
  word-break: break-word;
}

.mono {
  font-family: Consolas, 'Courier New', monospace;
}

.ml-8 {
  margin-left: 8px;
}

@media (max-width: 992px) {
  :deep(.el-col) {
    width: 100%;
    max-width: 100%;
    flex: 0 0 100%;
    margin-bottom: 14px;
  }

  .form-card,
  .result-card {
    min-height: auto;
  }
}
</style>

