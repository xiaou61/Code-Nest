<template>
  <section :class="['coach-workspace', { compact }]">
    <div class="coach-header">
      <div>
        <p class="coach-eyebrow">AI Growth Coach</p>
        <h3>{{ compact ? 'AI教练摘要' : 'AI成长教练' }}</h3>
        <p class="coach-subtitle">{{ detail.headline || '正在为你整理最新学习与求职诊断…' }}</p>
      </div>
      <div class="coach-actions">
        <el-tag size="small" effect="plain">{{ sceneLabel }}</el-tag>
        <el-button plain size="small" :loading="refreshing" @click="refreshCoach">刷新诊断</el-button>
        <el-button plain size="small" :disabled="!detail.snapshotId" @click="openReplanDialog">时间压缩重排</el-button>
        <el-button type="primary" size="small" @click="openChatPanel">继续追问</el-button>
      </div>
    </div>

    <el-skeleton v-if="loading" :rows="4" animated />

    <template v-else>
      <div class="score-grid">
        <article class="score-card">
          <span>综合准备度</span>
          <strong>{{ detail.overallScore || 0 }}</strong>
        </article>
        <article class="score-card">
          <span>学习评分</span>
          <strong>{{ detail.learningScore || 0 }}</strong>
        </article>
        <article class="score-card">
          <span>求职评分</span>
          <strong>{{ detail.careerScore || 0 }}</strong>
        </article>
        <article class="score-card">
          <span>执行评分</span>
          <strong>{{ detail.executionScore || 0 }}</strong>
        </article>
      </div>

      <div class="coach-summary">
        <div class="summary-block">
          <label>本次诊断</label>
          <p>{{ detail.summary || '暂无诊断摘要' }}</p>
        </div>
        <div class="summary-block">
          <label>风险等级</label>
          <el-tag :type="riskTagType(detail.riskLevel)" effect="light">{{ detail.riskLevel || 'MEDIUM' }}</el-tag>
          <span v-if="detail.fallbackOnly" class="fallback-note">当前为规则兜底结果</span>
        </div>
      </div>

      <div v-if="detail.focusAreas?.length" class="tag-panel">
        <label>优先关注</label>
        <div class="tag-list">
          <el-tag v-for="item in detail.focusAreas" :key="item" type="primary" effect="plain">{{ item }}</el-tag>
        </div>
      </div>

      <div v-if="detail.riskFlags?.length" class="tag-panel">
        <label>风险提醒</label>
        <div class="tag-list">
          <el-tag v-for="item in detail.riskFlags" :key="item" type="danger" effect="plain">{{ item }}</el-tag>
        </div>
      </div>

      <div class="action-panel">
        <div class="panel-head">
          <h4>行动清单</h4>
          <span>{{ compact ? '展示前2条高优先建议' : '按优先级执行，完成后可回流诊断' }}</span>
        </div>
        <div v-if="visibleActions.length" class="action-list">
          <article v-for="action in visibleActions" :key="action.actionId" class="action-item">
            <div class="action-top">
              <div>
                <strong>{{ action.title }}</strong>
                <p>{{ action.description }}</p>
              </div>
              <el-tag size="small" :type="priorityTagType(action.priority)" effect="light">{{ action.priority }}</el-tag>
            </div>
            <p class="action-reason">{{ action.reason }}</p>
            <div class="action-meta">
              <span>预期收益：{{ action.expectedGain || '保持稳定推进' }}</span>
              <span>预计耗时：{{ action.estimatedMinutes || 0 }} 分钟</span>
            </div>
            <div class="action-buttons">
              <el-button link type="primary" @click="goRoute(action.targetRoute)">去执行</el-button>
              <el-button
                link
                type="success"
                :disabled="action.status === 'DONE'"
                @click="completeAction(action)"
              >
                {{ action.status === 'DONE' ? '已完成' : '标记完成' }}
              </el-button>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无建议动作" :image-size="64" />
      </div>

      <div class="resource-panel">
        <div class="panel-head">
          <h4>推荐资源</h4>
          <span>{{ compact ? '优先补充前2条高匹配资源' : '结合当前诊断优先查看这些内容' }}</span>
        </div>
        <div v-if="visibleResources.length" class="resource-list">
          <article
            v-for="resource in visibleResources"
            :key="`${resource.resourceType || 'resource'}-${resource.title || resource.route}`"
            class="resource-item"
          >
            <div class="resource-top">
              <div class="resource-main">
                <el-tag size="small" effect="light">{{ resource.sourceLabel || resource.resourceType || '资源' }}</el-tag>
                <strong>{{ resource.title || '未命名资源' }}</strong>
              </div>
              <el-button link type="primary" :disabled="!resource.route" @click="goRoute(resource.route)">
                去查看
              </el-button>
            </div>
            <p v-if="resource.summary" class="resource-summary">{{ resource.summary }}</p>
            <p v-if="resource.reason" class="resource-reason">推荐理由：{{ resource.reason }}</p>
          </article>
        </div>
        <el-empty v-else description="暂无推荐资源，刷新诊断后再试" :image-size="60" />
      </div>

      <div v-if="!compact" class="chat-panel">
        <div class="panel-head">
          <h4>追问式对话</h4>
          <span>围绕当前诊断继续追问，回答会尽量引用快照依据</span>
        </div>
        <div class="suggested-list" v-if="detail.suggestedQuestions?.length">
          <el-button
            v-for="item in detail.suggestedQuestions"
            :key="item"
            plain
            size="small"
            @click="quickAsk(item)"
          >
            {{ item }}
          </el-button>
        </div>
        <div class="chat-history">
          <div v-if="chatMessages.length" class="chat-message-list">
            <div
              v-for="item in chatMessages"
              :key="item.messageId || `${item.role}-${item.content}`"
              :class="['chat-message', item.role]"
            >
              <span>{{ item.role === 'assistant' ? '教练' : '我' }}</span>
              <p>{{ item.content }}</p>
            </div>
          </div>
          <el-empty v-else description="点击推荐问题开始追问" :image-size="56" />
        </div>
        <div class="chat-compose">
          <el-input
            v-model="chatInput"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
            placeholder="追问例如：如果我这周只有5小时，该怎么压缩安排？"
          />
          <el-button type="primary" :loading="chatting" @click="sendChat">发送追问</el-button>
        </div>
      </div>
    </template>

    <el-drawer v-model="chatDrawerVisible" title="AI成长教练对话" size="420px" :with-header="true">
      <div class="drawer-chat">
        <div class="suggested-list" v-if="detail.suggestedQuestions?.length">
          <el-button
            v-for="item in detail.suggestedQuestions"
            :key="`drawer-${item}`"
            plain
            size="small"
            @click="quickAsk(item)"
          >
            {{ item }}
          </el-button>
        </div>
        <div class="chat-history compact-history">
          <div v-if="chatMessages.length" class="chat-message-list">
            <div
              v-for="item in chatMessages"
              :key="`drawer-${item.messageId || item.content}`"
              :class="['chat-message', item.role]"
            >
              <span>{{ item.role === 'assistant' ? '教练' : '我' }}</span>
              <p>{{ item.content }}</p>
            </div>
          </div>
          <el-empty v-else description="先从推荐问题开始" :image-size="56" />
        </div>
        <div class="chat-compose">
          <el-input v-model="chatInput" type="textarea" :rows="3" placeholder="继续追问…" />
          <el-button type="primary" :loading="chatting" @click="sendChat">发送追问</el-button>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="replanDialogVisible" title="时间压缩重排" width="620px">
      <div class="replan-form">
        <p class="replan-hint">输入你这周还可以投入的分钟数，教练会临时压缩一版高收益行动清单。</p>
        <div class="replan-input-row">
          <el-input-number v-model="replanMinutes" :min="15" :max="600" :step="15" controls-position="right" />
          <span class="replan-unit">分钟</span>
          <el-button type="primary" :loading="replanning" @click="submitReplan">生成压缩建议</el-button>
        </div>
      </div>

      <div v-if="replanResult" class="replan-result">
        <div class="summary-block replan-summary">
          <label>压缩摘要</label>
          <p>{{ replanResult.summary || '暂无压缩建议' }}</p>
          <div class="replan-meta">
            <span>原始总时长：{{ replanResult.originalTotalMinutes || 0 }} 分钟</span>
            <span>当前预算：{{ replanResult.availableMinutes || replanMinutes }} 分钟</span>
          </div>
          <span v-if="replanResult.fallbackOnly" class="fallback-note">当前为规则兜底结果</span>
        </div>

        <div class="replan-section">
          <div class="panel-head">
            <h4>保留动作</h4>
            <span>建议优先投入这些高收益动作</span>
          </div>
          <div v-if="replanResult.actions?.length" class="action-list">
            <article v-for="action in replanResult.actions" :key="`replan-${action.actionId}`" class="action-item">
              <div class="action-top">
                <div>
                  <strong>{{ action.title }}</strong>
                  <p>{{ action.description }}</p>
                </div>
                <el-tag size="small" :type="priorityTagType(action.priority)" effect="light">{{ action.priority }}</el-tag>
              </div>
              <p class="action-reason">{{ action.reason }}</p>
              <div class="action-meta">
                <span>预期收益：{{ action.expectedGain || '保持稳定推进' }}</span>
                <span>预计耗时：{{ action.estimatedMinutes || 0 }} 分钟</span>
              </div>
              <div class="action-buttons">
                <el-button link type="primary" @click="goRoute(action.targetRoute)">去执行</el-button>
                <el-button link type="success" :disabled="action.status === 'DONE'" @click="completeAction(action)">
                  {{ action.status === 'DONE' ? '已完成' : '标记完成' }}
                </el-button>
              </div>
            </article>
          </div>
          <el-empty v-else description="当前预算下没有可保留动作" :image-size="56" />
        </div>

        <div v-if="replanResult.deferredActions?.length" class="replan-section">
          <div class="panel-head">
            <h4>建议延后</h4>
            <span>这部分动作可放到下一轮刷新或下一天处理</span>
          </div>
          <div class="tag-list">
            <el-tag
              v-for="action in replanResult.deferredActions"
              :key="`deferred-${action.actionId}`"
              type="info"
              effect="plain"
            >
              {{ action.title }} · {{ action.estimatedMinutes || 0 }} 分钟
            </el-tag>
          </div>
        </div>

        <div v-if="replanResult.suggestedQuestions?.length" class="replan-section">
          <div class="panel-head">
            <h4>继续追问</h4>
            <span>可以继续围绕压缩结果追问教练</span>
          </div>
          <div class="suggested-list">
            <el-button
              v-for="item in replanResult.suggestedQuestions"
              :key="`replan-question-${item}`"
              plain
              size="small"
              @click="askFromReplan(item)"
            >
              {{ item }}
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { aiGrowthCoachApi } from '@/api/aiGrowthCoach'

const props = defineProps({
  scene: {
    type: String,
    default: 'hybrid'
  },
  compact: {
    type: Boolean,
    default: false
  }
})

const router = useRouter()
const loading = ref(false)
const refreshing = ref(false)
const chatting = ref(false)
const replanning = ref(false)
const normalizeDetail = (payload = {}) => ({
  actions: [],
  riskFlags: [],
  focusAreas: [],
  suggestedQuestions: [],
  resources: [],
  ...payload
})
const normalizeReplan = (payload = {}) => ({
  actions: [],
  deferredActions: [],
  suggestedQuestions: [],
  ...payload
})
const detail = ref(normalizeDetail())
const chatMessages = ref([])
const chatSessionId = ref(null)
const chatInput = ref('')
const chatDrawerVisible = ref(false)
const replanDialogVisible = ref(false)
const replanMinutes = ref(90)
const replanResult = ref(null)

const sceneLabel = computed(() => {
  const map = {
    learning: '学习视角',
    career: '求职视角',
    hybrid: '学习 + 求职'
  }
  return map[props.scene] || '综合视角'
})

const visibleActions = computed(() => {
  const list = detail.value?.actions || []
  return props.compact ? list.slice(0, 2) : list
})

const visibleResources = computed(() => {
  const list = detail.value?.resources || []
  return props.compact ? list.slice(0, 2) : list
})

const currentTotalMinutes = computed(() => (detail.value?.actions || []).reduce((total, item) => total + (item?.estimatedMinutes || 0), 0))

const riskTagType = (level) => {
  const map = {
    LOW: 'success',
    MEDIUM: 'warning',
    HIGH: 'danger'
  }
  return map[level] || 'info'
}

const priorityTagType = (priority) => {
  const map = {
    P0: 'danger',
    P1: 'warning',
    P2: 'info'
  }
  return map[priority] || 'info'
}

const loadSummary = async () => {
  loading.value = true
  try {
    const data = await aiGrowthCoachApi.getSummary(props.scene)
    detail.value = normalizeDetail(data)
    chatMessages.value = []
    chatSessionId.value = null
    replanResult.value = null
    replanDialogVisible.value = false
  } catch (error) {
    console.error('加载AI成长教练失败', error)
    ElMessage.error('加载AI成长教练失败')
  } finally {
    loading.value = false
  }
}

const refreshCoach = async () => {
  refreshing.value = true
  try {
    detail.value = normalizeDetail(await aiGrowthCoachApi.refresh({
      scene: props.scene,
      force: true
    }))
    chatMessages.value = []
    chatSessionId.value = null
    replanResult.value = null
    replanDialogVisible.value = false
    ElMessage.success('教练诊断已刷新')
  } catch (error) {
    console.error('刷新AI成长教练失败', error)
    ElMessage.error('刷新AI成长教练失败')
  } finally {
    refreshing.value = false
  }
}

const sendChat = async () => {
  if (!chatInput.value.trim() || !detail.value?.snapshotId) {
    return
  }
  chatting.value = true
  try {
    const res = await aiGrowthCoachApi.chat({
      sessionId: chatSessionId.value,
      snapshotId: detail.value.snapshotId,
      scene: props.scene,
      message: chatInput.value.trim()
    })
    chatSessionId.value = res.sessionId
    chatMessages.value = res.messages || []
    detail.value.suggestedQuestions = res.suggestedQuestions || detail.value.suggestedQuestions || []
    chatInput.value = ''
  } catch (error) {
    console.error('发送AI成长教练对话失败', error)
    ElMessage.error('发送追问失败')
  } finally {
    chatting.value = false
  }
}

const quickAsk = (question) => {
  chatInput.value = question
  if (!props.compact) {
    sendChat()
    return
  }
  chatDrawerVisible.value = true
}

const completeAction = async (action) => {
  if (!action?.actionId) return
  try {
    await aiGrowthCoachApi.updateActionStatus(action.actionId, 'DONE')
    syncActionStatus(action.actionId, 'DONE')
    ElMessage.success('动作已标记完成')
  } catch (error) {
    console.error('更新动作状态失败', error)
    ElMessage.error('更新动作状态失败')
  }
}

const syncActionStatus = (actionId, status) => {
  ;[detail.value?.actions, replanResult.value?.actions, replanResult.value?.deferredActions].forEach((list) => {
    if (!Array.isArray(list)) return
    const target = list.find(item => item?.actionId === actionId)
    if (target) {
      target.status = status
    }
  })
}

const goRoute = (path) => {
  if (!path) return
  router.push(path)
}

const openChatPanel = () => {
  if (props.compact) {
    chatDrawerVisible.value = true
    return
  }
  if (detail.value?.suggestedQuestions?.length) {
    chatInput.value = detail.value.suggestedQuestions[0]
    sendChat()
  }
}

const suggestReplanMinutes = () => {
  const total = currentTotalMinutes.value || 90
  return Math.max(30, Math.min(240, Math.ceil((total * 0.5) / 15) * 15))
}

const openReplanDialog = () => {
  if (!detail.value?.snapshotId) {
    ElMessage.warning('先生成教练快照后再试')
    return
  }
  replanMinutes.value = suggestReplanMinutes()
  replanResult.value = null
  replanDialogVisible.value = true
}

const submitReplan = async () => {
  if (!detail.value?.snapshotId) {
    ElMessage.warning('当前没有可重排的教练快照')
    return
  }
  replanning.value = true
  try {
    replanResult.value = normalizeReplan(await aiGrowthCoachApi.replan({
      snapshotId: detail.value.snapshotId,
      scene: props.scene,
      availableMinutes: replanMinutes.value
    }))
    ElMessage.success('已生成压缩建议')
  } catch (error) {
    console.error('AI成长教练重排失败', error)
    ElMessage.error('重排失败，请稍后重试')
  } finally {
    replanning.value = false
  }
}

const askFromReplan = (question) => {
  replanDialogVisible.value = false
  quickAsk(question)
}

onMounted(() => {
  loadSummary()
})

watch(
  () => props.scene,
  () => {
    loadSummary()
  }
)
</script>

<style scoped>
.coach-workspace {
  padding: 16px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(243, 249, 255, 0.98));
  border: 1px solid #d8e7fb;
}

.coach-workspace.compact {
  padding: 14px;
}

.coach-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.coach-eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.12em;
  color: #6a82a9;
  text-transform: uppercase;
}

.coach-header h3 {
  margin: 0 0 6px;
  color: #1e3f68;
}

.coach-subtitle {
  margin: 0;
  color: #5d769b;
  line-height: 1.7;
}

.coach-actions {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  flex-wrap: wrap;
}

.score-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.score-card {
  padding: 14px;
  border-radius: 16px;
  border: 1px solid #dce7fb;
  background: #f8fbff;
}

.score-card span {
  display: block;
  font-size: 12px;
  color: #7a90b1;
  margin-bottom: 6px;
}

.score-card strong {
  font-size: 30px;
  color: #1f63c5;
}

.coach-summary {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 12px;
  margin-bottom: 16px;
}

.summary-block {
  border: 1px solid #dce7fb;
  border-radius: 16px;
  padding: 14px;
  background: #fff;
}

.summary-block label,
.tag-panel label {
  display: block;
  color: #7d91b1;
  font-size: 12px;
  margin-bottom: 8px;
}

.summary-block p {
  margin: 0;
  color: #425f84;
  line-height: 1.8;
}

.fallback-note {
  display: inline-block;
  margin-left: 8px;
  color: #d9822b;
  font-size: 12px;
}

.tag-panel {
  margin-bottom: 14px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.action-panel,
.resource-panel,
.chat-panel {
  margin-top: 16px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 10px;
}

.panel-head h4 {
  margin: 0;
  color: #214069;
}

.panel-head span {
  color: #7c90ad;
  font-size: 12px;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.action-item {
  border: 1px solid #dce7fb;
  border-radius: 16px;
  padding: 14px;
  background: #fff;
}

.action-top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.action-top strong {
  color: #1f395f;
}

.action-top p {
  margin: 6px 0 0;
  color: #617ca0;
  line-height: 1.7;
}

.action-reason {
  margin: 8px 0 0;
  color: #436ca2;
  font-size: 13px;
}

.action-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 8px;
  color: #7286a6;
  font-size: 12px;
}

.action-buttons {
  margin-top: 8px;
  display: flex;
  gap: 10px;
}

.resource-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}

.resource-item {
  border: 1px solid #dce7fb;
  border-radius: 16px;
  padding: 14px;
  background: linear-gradient(180deg, #ffffff, #f8fbff);
}

.resource-top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
}

.resource-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.resource-main strong {
  color: #1f395f;
  line-height: 1.7;
}

.resource-summary {
  margin: 10px 0 0;
  color: #5f789b;
  line-height: 1.7;
}

.resource-reason {
  margin: 8px 0 0;
  color: #3568a9;
  font-size: 13px;
  line-height: 1.6;
}

.suggested-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.chat-history {
  min-height: 160px;
  border: 1px solid #dce7fb;
  border-radius: 16px;
  background: #fbfdff;
  padding: 12px;
}

.compact-history {
  min-height: 220px;
}

.chat-message-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chat-message {
  max-width: 86%;
  padding: 10px 12px;
  border-radius: 14px;
  background: #eef5ff;
}

.chat-message.user {
  margin-left: auto;
  background: #dff1ff;
}

.chat-message span {
  display: block;
  margin-bottom: 4px;
  color: #6d84a8;
  font-size: 12px;
}

.chat-message p {
  margin: 0;
  color: #2b466d;
  line-height: 1.7;
}

.chat-compose {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.replan-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.replan-hint {
  margin: 0;
  color: #5f789b;
  line-height: 1.7;
}

.replan-input-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.replan-unit {
  color: #6d84a8;
  font-size: 13px;
}

.replan-result {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.replan-summary {
  background: #f9fbff;
}

.replan-meta {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: #7286a6;
  font-size: 12px;
}

.replan-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

@media (max-width: 992px) {
  .score-grid,
  .coach-summary {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 768px) {
  .score-grid,
  .coach-summary {
    grid-template-columns: 1fr;
  }

  .replan-input-row {
    align-items: stretch;
  }
}
</style>
