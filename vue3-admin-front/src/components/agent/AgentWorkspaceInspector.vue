<template>
  <aside class="agent-inspector" aria-label="执行详情">
    <header class="agent-inspector__header">
      <div>
        <span>当前响应</span>
        <strong>{{ message?.toolName || '未命中工具' }}</strong>
      </div>
      <el-tag v-if="message?.status" size="small" :type="statusTagType(message.status)">
        {{ statusText(message.status) }}
      </el-tag>
    </header>

    <el-tabs v-model="activeTab" class="agent-inspector__tabs" stretch>
      <el-tab-pane name="execution" label="执行">
        <div v-if="message" class="agent-inspector__pane">
          <section v-if="message.plan?.length" class="agent-inspector__section">
            <h3>执行计划</h3>
            <ol class="agent-inspector__timeline">
              <li v-for="(step, index) in message.plan" :key="`${message.id}-plan-${index}`">
                <span class="agent-inspector__marker" :data-status="step.status" />
                <div>
                  <strong>{{ step.title || `步骤 ${index + 1}` }}</strong>
                  <p>{{ step.detail || step.status || '-' }}</p>
                </div>
              </li>
            </ol>
          </section>

          <section v-if="message.diff?.length" class="agent-inspector__section">
            <h3>变更差异</h3>
            <div class="agent-inspector__diffs">
              <article v-for="(item, index) in message.diff" :key="`${message.id}-diff-${index}`">
                <strong>{{ item.field || '变更项' }}</strong>
                <p v-if="item.description">{{ item.description }}</p>
                <dl v-else>
                  <div><dt>原值</dt><dd>{{ formatValue(item.beforeValue) }}</dd></div>
                  <div><dt>新值</dt><dd>{{ formatValue(item.afterValue) }}</dd></div>
                </dl>
              </article>
            </div>
          </section>

          <section v-if="message.nextActions?.length" class="agent-inspector__section">
            <h3>后续建议</h3>
            <ul class="agent-inspector__actions">
              <li v-for="action in message.nextActions" :key="action">{{ action }}</li>
            </ul>
          </section>

          <section class="agent-inspector__section agent-inspector__metadata">
            <h3>运行元数据</h3>
            <dl>
              <div><dt>Trace</dt><dd>{{ message.traceId || '-' }}</dd></div>
              <div><dt>Audit</dt><dd>{{ message.auditId || message.confirmation?.auditId || '-' }}</dd></div>
              <div><dt>风险</dt><dd>{{ riskText(message) }}</dd></div>
              <div v-if="message.errorCode"><dt>错误码</dt><dd>{{ message.errorCode }}</dd></div>
            </dl>
          </section>
        </div>
        <el-empty v-else :image-size="64" description="选择一条智能体响应" />
      </el-tab-pane>

      <el-tab-pane name="artifacts" label="结果">
        <div v-if="message?.artifacts?.length" class="agent-inspector__pane">
          <AgentArtifactViewer
            v-for="(artifact, index) in message.artifacts"
            :key="`${message.id}-artifact-${index}`"
            :artifact="artifact"
          />
        </div>
        <el-empty v-else :image-size="64" description="暂无结构化结果" />
      </el-tab-pane>

      <el-tab-pane name="trace" label="追踪">
        <ol v-if="message?.trace?.length" class="agent-inspector__trace">
          <li v-for="(step, index) in message.trace" :key="`${message.id}-trace-${index}`">
            <span class="agent-inspector__marker" :data-status="step.status" />
            <div>
              <header>
                <strong>{{ stageText(step.stage) }}</strong>
                <time v-if="step.elapsedMs !== null && step.elapsedMs !== undefined">
                  {{ step.elapsedMs }} ms
                </time>
              </header>
              <p>{{ step.detail || step.status || '-' }}</p>
            </div>
          </li>
        </ol>
        <el-empty v-else :image-size="64" description="暂无运行追踪" />
      </el-tab-pane>
    </el-tabs>
  </aside>
</template>

<script setup>
import { ref, watch } from 'vue'
import AgentArtifactViewer from './AgentArtifactViewer.vue'

const props = defineProps({
  message: {
    type: Object,
    default: null,
  },
})

const activeTab = ref('execution')

watch(() => props.message?.id, () => {
  if (props.message?.artifacts?.length && !props.message?.plan?.length && !props.message?.diff?.length) {
    activeTab.value = 'artifacts'
  } else {
    activeTab.value = 'execution'
  }
})

function statusText(status) {
  const textMap = {
    answered: '已回答',
    confirm_required: '待确认',
    executed: '已执行',
    cancelled: '已取消',
    rejected: '已拒绝',
    error: '异常',
  }
  return textMap[status] || status
}

function statusTagType(status) {
  if (status === 'executed' || status === 'answered') return 'success'
  if (status === 'confirm_required') return 'warning'
  if (status === 'error' || status === 'rejected') return 'danger'
  return 'info'
}

function stageText(stage) {
  const stageMap = {
    'request.received': '收到请求',
    'session.loaded': '载入会话',
    'plan.resolved': '解析计划',
    'plan.clarification': '补充信息',
    'policy.checked': '策略校验',
    'preview.generated': '生成预览',
    'confirmation.required': '等待确认',
    'confirmation.verified': '确认通过',
    'tool.executed': '执行工具',
    'audit.recorded': '记录审计',
  }
  return stageMap[stage] || stage || '未知阶段'
}

function riskText(message) {
  const values = [message?.riskLevel, message?.riskCategory].filter(Boolean)
  return values.length ? values.join(' / ') : '-'
}

function formatValue(value) {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}
</script>

<style scoped>
.agent-inspector {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-width: 0;
  min-height: 0;
  border-left: 1px solid var(--cn-color-border-subtle);
  background: var(--cn-color-bg-surface);
}

.agent-inspector__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  min-height: 68px;
  padding: var(--cn-space-3) var(--cn-space-4);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.agent-inspector__header > div {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.agent-inspector__header span {
  color: var(--cn-color-text-tertiary);
  font-size: 11px;
}

.agent-inspector__header strong {
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-inspector__tabs {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 0;
}

.agent-inspector__tabs :deep(.el-tabs__header) {
  margin: 0;
}

.agent-inspector__tabs :deep(.el-tabs__content),
.agent-inspector__tabs :deep(.el-tab-pane) {
  min-height: 0;
  height: 100%;
}

.agent-inspector__tabs :deep(.el-tabs__content) {
  overflow-y: auto;
  padding: 0 var(--cn-space-4) var(--cn-space-4);
}

.agent-inspector__pane {
  display: grid;
  align-content: start;
}

.agent-inspector__section {
  display: grid;
  gap: var(--cn-space-3);
  padding: var(--cn-space-4) 0;
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.agent-inspector__section:last-child {
  border-bottom: 0;
}

.agent-inspector__section h3 {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 12px;
  text-transform: uppercase;
}

.agent-inspector__timeline,
.agent-inspector__trace {
  display: grid;
  gap: 0;
  margin: 0;
  padding: 0;
  list-style: none;
}

.agent-inspector__timeline li,
.agent-inspector__trace li {
  position: relative;
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr);
  gap: var(--cn-space-3);
  padding-bottom: var(--cn-space-4);
}

.agent-inspector__timeline li:not(:last-child)::before,
.agent-inspector__trace li:not(:last-child)::before {
  position: absolute;
  top: 12px;
  bottom: 0;
  left: 5px;
  width: 1px;
  background: var(--cn-color-border);
  content: '';
}

.agent-inspector__marker {
  position: relative;
  z-index: 1;
  width: 11px;
  height: 11px;
  margin-top: 3px;
  border: 2px solid var(--cn-color-bg-surface);
  border-radius: 50%;
  background: var(--cn-color-brand-primary);
  box-shadow: 0 0 0 1px var(--cn-color-brand-primary);
}

.agent-inspector__marker[data-status='blocked'],
.agent-inspector__marker[data-status='error'],
.agent-inspector__marker[data-status='rejected'] {
  background: var(--cn-color-danger);
  box-shadow: 0 0 0 1px var(--cn-color-danger);
}

.agent-inspector__timeline strong,
.agent-inspector__trace strong {
  color: var(--cn-color-text-primary);
  font-size: 12px;
}

.agent-inspector__timeline p,
.agent-inspector__trace p {
  margin: 3px 0 0;
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  line-height: 1.55;
}

.agent-inspector__trace {
  padding-top: var(--cn-space-4);
}

.agent-inspector__trace header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-2);
}

.agent-inspector__trace time {
  color: var(--cn-color-text-tertiary);
  font-size: 11px;
}

.agent-inspector__diffs {
  display: grid;
  gap: var(--cn-space-3);
}

.agent-inspector__diffs article {
  display: grid;
  gap: var(--cn-space-2);
  padding-left: var(--cn-space-3);
  border-left: 3px solid var(--cn-color-warning);
}

.agent-inspector__diffs strong,
.agent-inspector__diffs p,
.agent-inspector__diffs dl,
.agent-inspector__diffs dd,
.agent-inspector__diffs dt {
  margin: 0;
}

.agent-inspector__diffs strong,
.agent-inspector__diffs p,
.agent-inspector__diffs dl {
  font-size: 12px;
}

.agent-inspector__diffs p,
.agent-inspector__diffs dd {
  color: var(--cn-color-text-secondary);
}

.agent-inspector__diffs dl {
  display: grid;
  gap: 4px;
}

.agent-inspector__diffs dl div,
.agent-inspector__metadata dl div {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr);
  gap: var(--cn-space-2);
}

.agent-inspector__diffs dt,
.agent-inspector__metadata dt {
  color: var(--cn-color-text-tertiary);
}

.agent-inspector__actions {
  display: grid;
  gap: var(--cn-space-2);
  margin: 0;
  padding-left: 18px;
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  line-height: 1.55;
}

.agent-inspector__metadata dl {
  display: grid;
  gap: var(--cn-space-2);
  margin: 0;
}

.agent-inspector__metadata dt,
.agent-inspector__metadata dd {
  min-width: 0;
  margin: 0;
  font-size: 11px;
  overflow-wrap: anywhere;
}

.agent-inspector__metadata dd {
  color: var(--cn-color-text-secondary);
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
}
</style>
