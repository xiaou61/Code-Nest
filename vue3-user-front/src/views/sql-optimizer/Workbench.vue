<template>
  <div class="sql-workbench-page">
    <div class="page-header">
      <div>
        <h2>SQL 优化工作台 2.0</h2>
        <p>输入 SQL、EXPLAIN 与表结构，快速拿到可执行优化建议（M1 基础版）。</p>
      </div>
      <div class="header-actions">
        <el-button @click="fillDemo">填充示例</el-button>
        <el-button :disabled="!form.sql.trim()" @click="formatSqlInput">格式化SQL</el-button>
        <el-button :disabled="!canAnalyze" @click="runRewrite" :loading="rewriteLoading">生成重写建议</el-button>
        <el-button @click="openBatchDialog">批量分析</el-button>
        <el-button type="primary" :loading="analyzeLoading" :disabled="!canAnalyze" @click="runAnalyze">
          开始分析
        </el-button>
      </div>
    </div>

    <el-row :gutter="18">
      <el-col :xs="24" :lg="16">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="panel-title">分析输入</div>
          </template>

          <el-form label-position="top">
            <el-form-item label="SQL">
              <el-input
                v-model="form.sql"
                type="textarea"
                :rows="5"
                placeholder="请输入待优化 SQL，例如 SELECT * FROM users WHERE name = 'test'"
              />
            </el-form-item>

            <el-form-item label="EXPLAIN 结果">
              <el-input
                v-model="form.explainResult"
                type="textarea"
                :rows="6"
                placeholder="支持 TABLE 文本或 EXPLAIN FORMAT=JSON 结果"
              />
            </el-form-item>

            <el-row :gutter="12">
              <el-col :xs="24" :md="12">
                <el-form-item label="EXPLAIN 格式">
                  <el-radio-group v-model="form.explainFormat">
                    <el-radio-button label="TABLE">TABLE</el-radio-button>
                    <el-radio-button label="JSON">JSON</el-radio-button>
                  </el-radio-group>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="MySQL 版本">
                  <el-select v-model="form.mysqlVersion" style="width: 100%">
                    <el-option label="MySQL 8.0" value="8.0" />
                    <el-option label="MySQL 5.7" value="5.7" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="表结构 DDL">
              <div class="ddl-list">
                <div v-for="(item, index) in form.tableStructures" :key="`ddl-${index}`" class="ddl-item">
                  <el-input v-model="item.tableName" placeholder="表名（可选）" class="ddl-table-name" />
                  <el-input v-model="item.ddl" type="textarea" :rows="4" placeholder="CREATE TABLE ..." />
                  <div class="ddl-actions">
                    <el-button link type="danger" :disabled="form.tableStructures.length <= 1" @click="removeDDL(index)">
                      删除
                    </el-button>
                  </div>
                </div>
                <el-button text type="primary" @click="addDDL">+ 新增一张表结构</el-button>
              </div>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card class="panel-card summary-card" shadow="never">
          <template #header>
            <div class="panel-title">本次结果</div>
          </template>
          <div v-if="analysisResult" class="summary-content">
            <el-progress type="dashboard" :percentage="scorePercent" :color="scoreColor" />
            <div class="summary-meta">
              <el-tag :type="scoreTagType">{{ scoreLabel }}</el-tag>
              <el-tag v-if="analysisResult.fallback" type="warning">降级结果</el-tag>
            </div>
            <p class="summary-desc">
              记录ID：{{ currentRecordId || '-' }}
            </p>
            <div class="summary-actions">
              <el-button size="small" :disabled="!currentRecordId" @click="exportCurrentCase('json')">导出JSON</el-button>
              <el-button size="small" :disabled="!currentRecordId" @click="exportCurrentCase('markdown')">导出Markdown</el-button>
            </div>
          </div>
          <el-empty v-else description="尚未分析" :image-size="90" />
        </el-card>

        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="panel-title">
              工作台案例
              <div class="history-header-actions">
                <el-button link :loading="historyLoading" @click="loadHistory">刷新</el-button>
                <el-button link @click="exportFilteredCases('json')" :loading="historyExportLoading">导出筛选JSON</el-button>
                <el-button link @click="exportFilteredCases('markdown')" :loading="historyExportLoading">导出筛选Markdown</el-button>
              </div>
            </div>
          </template>
          <div v-loading="historyLoading">
            <div class="history-toolbar">
              <el-select
                v-model="historyFilters.favorite"
                clearable
                placeholder="收藏状态"
                class="history-filter"
                @change="onHistoryFilterChange"
              >
                <el-option :value="true" label="仅收藏" />
                <el-option :value="false" label="未收藏" />
              </el-select>
              <el-select
                v-model="historyFilters.hasRewrite"
                clearable
                placeholder="重写结果"
                class="history-filter"
                @change="onHistoryFilterChange"
              >
                <el-option :value="true" label="含重写" />
                <el-option :value="false" label="不含重写" />
              </el-select>
              <el-select
                v-model="historyFilters.hasCompare"
                clearable
                placeholder="对比结果"
                class="history-filter"
                @change="onHistoryFilterChange"
              >
                <el-option :value="true" label="含对比" />
                <el-option :value="false" label="不含对比" />
              </el-select>
              <el-select
                v-model="historyFilters.highestSeverity"
                clearable
                placeholder="最高严重级别"
                class="history-filter"
                @change="onHistoryFilterChange"
              >
                <el-option value="HIGH" label="HIGH" />
                <el-option value="MEDIUM" label="MEDIUM" />
                <el-option value="LOW" label="LOW" />
                <el-option value="NONE" label="NONE" />
              </el-select>
              <el-select
                v-model="historySort.sortBy"
                placeholder="排序字段"
                class="history-filter"
                @change="onHistoryFilterChange"
              >
                <el-option value="createTime" label="按时间" />
                <el-option value="score" label="按评分" />
                <el-option value="severity" label="按严重级别" />
              </el-select>
              <el-select
                v-model="historySort.sortOrder"
                placeholder="排序方向"
                class="history-filter"
                @change="onHistoryFilterChange"
              >
                <el-option value="desc" label="降序" />
                <el-option value="asc" label="升序" />
              </el-select>
            </div>
            <el-empty v-if="historyList.length === 0" description="暂无记录" :image-size="84" />
            <div v-else class="history-list">
              <div v-for="item in historyList" :key="item.id" class="history-item">
                <button
                  class="history-main"
                  type="button"
                  @click="loadHistoryDetail(item)"
                >
                  <div class="history-title">#{{ item.id }} · 评分 {{ item.score ?? '-' }}</div>
                  <div class="history-preview">{{ item.originalSqlPreview || '无SQL预览' }}</div>
                  <div class="history-tags">
                    <el-tag v-if="item.highestSeverity && item.highestSeverity !== 'NONE'" :type="severityTagType(item.highestSeverity)" size="small">
                      {{ item.highestSeverity }}
                    </el-tag>
                    <el-tag v-if="item.hasRewrite" size="small" type="success">含重写</el-tag>
                    <el-tag v-if="item.hasCompare" size="small" type="info">含对比</el-tag>
                    <el-tag v-if="item.fallback" size="small" type="warning">降级</el-tag>
                  </div>
                  <div class="history-time">{{ item.createTime }}</div>
                </button>
                <div class="history-actions">
                  <el-button
                    link
                    :type="item.favorite ? 'warning' : 'info'"
                    @click.stop="toggleCaseFavorite(item)"
                  >
                    {{ item.favorite ? '取消收藏' : '收藏' }}
                  </el-button>
                </div>
              </div>
            </div>
            <el-pagination
              class="history-pagination"
              layout="total, sizes, prev, pager, next"
              :page-sizes="[6, 10, 20]"
              :page-size="historyPagination.pageSize"
              :current-page="historyPagination.pageNum"
              :total="historyTotal"
              @current-change="onHistoryPageChange"
              @size-change="onHistorySizeChange"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="panel-card result-card" shadow="never">
      <template #header>
        <div class="panel-title">分析结果</div>
      </template>

      <el-empty v-if="!analysisResult" description="请先执行分析" />

      <div v-else>
        <el-alert
          v-if="analysisResult.fallback"
          type="warning"
          show-icon
          :closable="false"
          title="当前为降级结果（统一 AI 运行时不可用或返回异常），后续完成模型配置后可获得完整结果。"
        />

        <h4 class="section-title">问题清单</h4>
        <el-table :data="analysisResult.problems || []" border>
          <el-table-column prop="type" label="类型" min-width="150" />
          <el-table-column prop="severity" label="严重级别" width="120" />
          <el-table-column prop="description" label="说明" min-width="240" />
          <el-table-column prop="location" label="位置" min-width="140" />
        </el-table>

        <h4 class="section-title">优化建议</h4>
        <el-table :data="analysisResult.suggestions || []" border>
          <el-table-column prop="type" label="建议类型" width="150" />
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column prop="reason" label="原因" min-width="220" />
          <el-table-column prop="expectedImprovement" label="预期收益" min-width="180" />
        </el-table>

        <h4 class="section-title">EXPLAIN 解读</h4>
        <el-table :data="analysisResult.explainAnalysis || []" border>
          <el-table-column prop="table" label="表" width="120" />
          <el-table-column prop="type" label="访问类型" width="120" />
          <el-table-column prop="rows" label="扫描行数" width="120" />
          <el-table-column prop="extra" label="Extra" min-width="180" />
          <el-table-column prop="issue" label="问题说明" min-width="220" />
        </el-table>

        <h4 class="section-title">优化 SQL</h4>
        <el-input
          :model-value="analysisResult.optimizedSql || '暂无优化 SQL（可能是降级结果）'"
          type="textarea"
          :rows="4"
          readonly
        />
        <div class="section-actions">
          <el-button
            size="small"
            :disabled="!analysisResult.optimizedSql"
            @click="copyText(analysisResult.optimizedSql, '优化SQL')"
          >
            复制优化SQL
          </el-button>
        </div>

        <template v-if="rewriteResult">
          <h4 class="section-title">重写说明</h4>
          <el-alert
            :type="rewriteResult.fallback ? 'warning' : 'success'"
            show-icon
            :closable="false"
            :title="rewriteResult.rewriteReason || (rewriteResult.fallback ? '当前为降级重写结果' : '已生成重写建议')"
          />

          <div class="rewrite-grid">
            <el-card shadow="never" class="rewrite-card">
              <template #header>
                <div class="rewrite-card-header">
                  <span>索引 DDL 建议</span>
                  <el-button
                    link
                    size="small"
                    :disabled="!(rewriteResult.indexDdls || []).length"
                    @click="copyText((rewriteResult.indexDdls || []).join('\n'), '索引DDL')"
                  >
                    复制
                  </el-button>
                </div>
              </template>
              <el-empty v-if="!(rewriteResult.indexDdls || []).length" description="暂无索引DDL建议" :image-size="80" />
              <el-input
                v-else
                :model-value="(rewriteResult.indexDdls || []).join('\n')"
                type="textarea"
                :rows="Math.min(Math.max((rewriteResult.indexDdls || []).length + 1, 4), 10)"
                readonly
              />
            </el-card>
            <el-card shadow="never" class="rewrite-card">
              <template #header>风险提示</template>
              <el-empty v-if="!(rewriteResult.riskWarnings || []).length" description="暂无风险提示" :image-size="80" />
              <ul v-else class="risk-list">
                <li v-for="(risk, idx) in rewriteResult.riskWarnings" :key="`risk-${idx}`">{{ risk }}</li>
              </ul>
              <p class="expected-improve">
                预期收益：{{ rewriteResult.expectedImprovement || '待评估' }}
              </p>
            </el-card>
          </div>
        </template>
      </div>
    </el-card>

    <el-card class="panel-card" shadow="never">
      <template #header>
        <div class="panel-title">优化前后收益对比</div>
      </template>
      <el-form label-position="top">
        <el-row :gutter="12">
          <el-col :xs="24" :md="12">
            <el-form-item label="优化前 SQL">
              <el-input v-model="compareForm.beforeSql" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item label="优化前 EXPLAIN">
              <el-input v-model="compareForm.beforeExplain" type="textarea" :rows="4" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="优化后 SQL">
              <el-input v-model="compareForm.afterSql" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item label="优化后 EXPLAIN">
              <el-input v-model="compareForm.afterExplain" type="textarea" :rows="4" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="EXPLAIN 格式">
          <el-radio-group v-model="compareForm.explainFormat">
            <el-radio-button label="TABLE">TABLE</el-radio-button>
            <el-radio-button label="JSON">JSON</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <div class="compare-actions">
          <el-button type="primary" :loading="compareLoading" @click="runCompare">执行收益对比</el-button>
        </div>
      </el-form>

      <div v-if="compareResult" class="compare-result">
        <el-alert
          :type="compareResult.fallback ? 'warning' : 'success'"
          :closable="false"
          show-icon
          :title="compareResult.summary || '收益评估已生成'"
        />
        <div class="compare-metrics">
          <div class="metric-item">
            <label>收益评分</label>
            <strong>{{ compareResult.improvementScore ?? 0 }}</strong>
          </div>
          <div class="metric-item">
            <label>扫描行变化</label>
            <strong>{{ compareResult.deltaRows || '-' }}</strong>
          </div>
          <div class="metric-item">
            <label>访问类型变化</label>
            <strong>{{ compareResult.deltaType || '-' }}</strong>
          </div>
          <div class="metric-item">
            <label>Extra变化</label>
            <strong>{{ compareResult.deltaExtra || '-' }}</strong>
          </div>
        </div>
        <ul v-if="(compareResult.attention || []).length" class="risk-list">
          <li v-for="(tip, idx) in compareResult.attention" :key="`tip-${idx}`">{{ tip }}</li>
        </ul>
        <el-table :data="compareTableRows" border size="small" class="compare-table">
          <el-table-column prop="metric" label="指标" width="160" />
          <el-table-column prop="value" label="变化说明" min-width="220" />
        </el-table>
      </div>
    </el-card>

    <el-dialog v-model="batchDialogVisible" title="批量分析（JSON数组）" width="860px" destroy-on-close>
      <el-alert
        type="info"
        show-icon
        :closable="false"
        title="输入格式：数组，每项结构与单条分析一致。支持最多20条。"
      />
      <el-input
        v-model="batchInput"
        type="textarea"
        :rows="14"
        class="batch-textarea"
        placeholder="[{...},{...}]"
      />
      <div class="batch-summary" v-if="batchResult">
        <el-tag type="info">总数 {{ batchResult.total || 0 }}</el-tag>
        <el-tag type="success">成功 {{ batchResult.successCount || 0 }}</el-tag>
        <el-tag type="warning">降级 {{ batchResult.fallbackCount || 0 }}</el-tag>
        <el-tag type="danger">失败 {{ batchResult.failedCount || 0 }}</el-tag>
      </div>
      <div v-if="batchResult?.items?.length" class="batch-tools">
        <el-select v-model="batchSortMode" class="batch-sort" placeholder="选择排序方式">
          <el-option label="按提交顺序" value="index" />
          <el-option label="按评分降序" value="scoreDesc" />
          <el-option label="按严重级别降序" value="severityDesc" />
        </el-select>
        <el-button
          type="warning"
          plain
          :disabled="failedBatchCount === 0"
          :loading="batchRetryLoading"
          @click="retryFailedBatchItems"
        >
          重试失败项（{{ failedBatchCount }}）
        </el-button>
      </div>
      <el-table v-if="batchResult?.items?.length" :data="displayedBatchItems" border max-height="260">
        <el-table-column prop="index" label="#" width="70" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'">{{ row.success ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="100">
          <template #default="{ row }">
            {{ row?.result?.analysis?.score ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="降级" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.success" :type="row?.result?.fallback ? 'warning' : 'success'" size="small">
              {{ row?.result?.fallback ? '是' : '否' }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="严重级别" width="120">
          <template #default="{ row }">
            <el-tag v-if="getBatchHighestSeverity(row) !== 'NONE'" :type="severityTagType(getBatchHighestSeverity(row))">
              {{ getBatchHighestSeverity(row) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="信息" min-width="260" />
      </el-table>
      <template #footer>
        <el-button @click="batchDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="batchLoading" @click="runBatchAnalyze">执行批量分析</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { sqlOptimizerApi } from '@/api/sqlOptimizer'

const analyzeLoading = ref(false)
const rewriteLoading = ref(false)
const historyLoading = ref(false)
const historyExportLoading = ref(false)
const batchLoading = ref(false)
const batchRetryLoading = ref(false)
const compareLoading = ref(false)
const analysisResult = ref(null)
const rewriteResult = ref(null)
const compareResult = ref(null)
const historyList = ref([])
const historyTotal = ref(0)
const currentRecordId = ref(null)
const batchDialogVisible = ref(false)
const batchInput = ref('')
const batchResult = ref(null)
const batchSortMode = ref('index')
const lastBatchPayload = ref([])

const historyPagination = reactive({
  pageNum: 1,
  pageSize: 6
})

const historyFilters = reactive({
  favorite: undefined,
  hasRewrite: undefined,
  hasCompare: undefined,
  highestSeverity: ''
})

const historySort = reactive({
  sortBy: 'createTime',
  sortOrder: 'desc'
})

const SEVERITY_WEIGHT = {
  HIGH: 3,
  MEDIUM: 2,
  LOW: 1,
  NONE: 0
}

const SQL_BREAK_KEYWORDS = [
  'SELECT',
  'FROM',
  'WHERE',
  'GROUP BY',
  'ORDER BY',
  'HAVING',
  'LIMIT',
  'LEFT JOIN',
  'RIGHT JOIN',
  'INNER JOIN',
  'OUTER JOIN',
  'JOIN',
  'ON',
  'UNION ALL',
  'UNION'
]

const compareForm = reactive({
  beforeSql: '',
  afterSql: '',
  beforeExplain: '',
  afterExplain: '',
  explainFormat: 'TABLE'
})

const form = reactive({
  sql: '',
  explainResult: '',
  explainFormat: 'TABLE',
  mysqlVersion: '8.0',
  tableStructures: [
    {
      tableName: '',
      ddl: ''
    }
  ]
})

const canAnalyze = computed(() => {
  if (!form.sql.trim() || !form.explainResult.trim()) {
    return false
  }
  return form.tableStructures.some((item) => item.ddl && item.ddl.trim())
})

const displayedBatchItems = computed(() => {
  const items = batchResult.value?.items || []
  return sortBatchItems(items, batchSortMode.value)
})

const failedBatchCount = computed(() => {
  const items = batchResult.value?.items || []
  return items.filter((item) => !item.success).length
})

const compareTableRows = computed(() => {
  if (!compareResult.value) {
    return []
  }

  return [
    { metric: '收益评分', value: String(compareResult.value.improvementScore ?? '-') },
    { metric: '扫描行变化', value: compareResult.value.deltaRows || '-' },
    { metric: '访问类型变化', value: compareResult.value.deltaType || '-' },
    { metric: 'Extra变化', value: compareResult.value.deltaExtra || '-' },
    { metric: '总结', value: compareResult.value.summary || '-' }
  ]
})

const scorePercent = computed(() => Number(analysisResult.value?.score || 0))

const scoreColor = computed(() => {
  const score = scorePercent.value
  if (score >= 80) {
    return '#17b26a'
  }
  if (score >= 60) {
    return '#f79009'
  }
  return '#f04438'
})

const scoreTagType = computed(() => {
  const score = scorePercent.value
  if (score >= 80) {
    return 'success'
  }
  if (score >= 60) {
    return 'warning'
  }
  return 'danger'
})

const scoreLabel = computed(() => `综合评分 ${scorePercent.value}`)

const normalizeSeverity = (severity) => {
  const value = String(severity || '').trim().toUpperCase()
  return value in SEVERITY_WEIGHT ? value : 'NONE'
}

const severityTagType = (severity) => {
  const normalized = normalizeSeverity(severity)
  if (normalized === 'HIGH') {
    return 'danger'
  }
  if (normalized === 'MEDIUM') {
    return 'warning'
  }
  if (normalized === 'LOW') {
    return 'success'
  }
  return 'info'
}

const getHighestSeverityFromProblems = (problems) => {
  if (!Array.isArray(problems) || problems.length === 0) {
    return 'NONE'
  }

  let highest = 'NONE'
  for (const problem of problems) {
    const current = normalizeSeverity(problem?.severity)
    if (SEVERITY_WEIGHT[current] > SEVERITY_WEIGHT[highest]) {
      highest = current
    }
  }
  return highest
}

const getBatchHighestSeverity = (batchItem) => {
  return getHighestSeverityFromProblems(batchItem?.result?.analysis?.problems || [])
}

const sortBatchItems = (items, sortMode) => {
  const cloned = [...items]

  if (sortMode === 'scoreDesc') {
    return cloned.sort((a, b) => (b?.result?.analysis?.score || -1) - (a?.result?.analysis?.score || -1))
  }

  if (sortMode === 'severityDesc') {
    return cloned.sort((a, b) => {
      const aSeverity = SEVERITY_WEIGHT[getBatchHighestSeverity(a)] || 0
      const bSeverity = SEVERITY_WEIGHT[getBatchHighestSeverity(b)] || 0
      if (bSeverity !== aSeverity) {
        return bSeverity - aSeverity
      }
      return (b?.result?.analysis?.score || -1) - (a?.result?.analysis?.score || -1)
    })
  }

  return cloned.sort((a, b) => (a?.index || 0) - (b?.index || 0))
}

const formatSqlText = (sql) => {
  let formatted = sql.replace(/\s+/g, ' ').trim()

  for (const keyword of SQL_BREAK_KEYWORDS) {
    const pattern = new RegExp(`\\s+${keyword.replace(/\s+/g, '\\s+')}\\b`, 'ig')
    formatted = formatted.replace(pattern, `\n${keyword}`)
  }

  formatted = formatted
    .replace(/\s*,\s*/g, ', ')
    .replace(/\(\s+/g, '(')
    .replace(/\s+\)/g, ')')

  return formatted
}

const addDDL = () => {
  form.tableStructures.push({
    tableName: '',
    ddl: ''
  })
}

const removeDDL = (index) => {
  form.tableStructures.splice(index, 1)
}

const fillDemo = () => {
  form.sql = "SELECT * FROM users WHERE name = 'test'"
  form.explainResult = [
    '| id | select_type | table | type | possible_keys | key | rows | Extra |',
    '| 1 | SIMPLE | users | ALL | NULL | NULL | 120000 | Using where |'
  ].join('\n')
  form.explainFormat = 'TABLE'
  form.mysqlVersion = '8.0'
  form.tableStructures = [
    {
      tableName: 'users',
      ddl: 'CREATE TABLE users (id BIGINT PRIMARY KEY, name VARCHAR(100), email VARCHAR(200), created_at DATETIME) ENGINE=InnoDB;'
    }
  ]
  compareForm.beforeSql = form.sql
  compareForm.afterSql = "SELECT id, name, email FROM users WHERE name = 'test'"
  compareForm.beforeExplain = form.explainResult
  compareForm.explainFormat = 'TABLE'
  compareForm.afterExplain = [
    '| id | select_type | table | type | possible_keys | key | rows | Extra |',
    '| 1 | SIMPLE | users | ref | idx_users_name | idx_users_name | 1 | Using where |'
  ].join('\n')
}

const formatSqlInput = () => {
  if (!form.sql.trim()) {
    ElMessage.warning('请输入SQL后再格式化')
    return
  }
  form.sql = formatSqlText(form.sql)
  ElMessage.success('SQL格式化完成')
}

const copyText = async (text, label) => {
  if (!text || !String(text).trim()) {
    ElMessage.warning(`暂无可复制的${label}`)
    return
  }

  try {
    await navigator.clipboard.writeText(String(text))
    ElMessage.success(`${label}已复制`)
  } catch (error) {
    const textarea = document.createElement('textarea')
    textarea.value = String(text)
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success(`${label}已复制`)
  }
}

const downloadTextFile = (content, fileName, mimeType) => {
  const blob = new Blob([content], { type: mimeType })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

const renderMarkdownList = (items) => {
  if (!Array.isArray(items) || items.length === 0) {
    return '- 无'
  }
  return items.map((item) => `- ${item}`).join('\n')
}

const buildCaseMarkdown = (detail) => {
  const problemLines = (detail?.analysis?.problems || []).map((problem, index) =>
    `${index + 1}. [${problem?.severity || 'NONE'}] ${problem?.type || '-'} - ${problem?.description || '-'}（${problem?.location || '-'}）`
  )
  const suggestionLines = (detail?.analysis?.suggestions || []).map((suggestion, index) =>
    `${index + 1}. ${suggestion?.title || '-'}（${suggestion?.type || '-'}）：${suggestion?.reason || '-'}`
  )

  const lines = [
    `# SQL优化工作台案例 #${detail?.id || '-'}`,
    '',
    `- 工作流版本：${detail?.workflowVersion || '-'}`,
    `- 综合评分：${detail?.score ?? '-'}`,
    `- 是否降级：${detail?.fallback ? '是' : '否'}`,
    `- 创建时间：${detail?.createTime || '-'}`,
    '',
    '## 原始SQL',
    '```sql',
    detail?.originalSql || '',
    '```',
    '',
    '## EXPLAIN',
    '```text',
    detail?.explainResult || '',
    '```',
    '',
    '## 问题清单',
    ...(problemLines.length ? problemLines : ['- 无']),
    '',
    '## 优化建议',
    ...(suggestionLines.length ? suggestionLines : ['- 无']),
    '',
    '## 重写结果',
    '```sql',
    detail?.rewrite?.optimizedSql || detail?.analysis?.optimizedSql || '',
    '```',
    '',
    '### 索引DDL建议',
    renderMarkdownList(detail?.rewrite?.indexDdls || []),
    '',
    '### 风险提示',
    renderMarkdownList(detail?.rewrite?.riskWarnings || []),
    '',
    '## 收益对比',
    `- 收益评分：${detail?.compare?.improvementScore ?? '-'}`,
    `- 扫描行变化：${detail?.compare?.deltaRows || '-'}`,
    `- 访问类型变化：${detail?.compare?.deltaType || '-'}`,
    `- Extra变化：${detail?.compare?.deltaExtra || '-'}`,
    `- 总结：${detail?.compare?.summary || '-'}`,
    '### 注意事项',
    renderMarkdownList(detail?.compare?.attention || [])
  ]

  return lines.join('\n')
}

const exportCurrentCase = async (format) => {
  if (!currentRecordId.value) {
    ElMessage.warning('请先分析或加载案例后再导出')
    return
  }

  try {
    const detail = await sqlOptimizerApi.getWorkbenchCaseDetail(currentRecordId.value)
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-')
    if (format === 'json') {
      const content = JSON.stringify(detail, null, 2)
      downloadTextFile(content, `sql-workbench-case-${currentRecordId.value}-${timestamp}.json`, 'application/json;charset=utf-8')
      ElMessage.success('案例JSON已导出')
      return
    }

    if (format === 'markdown') {
      const content = buildCaseMarkdown(detail)
      downloadTextFile(content, `sql-workbench-case-${currentRecordId.value}-${timestamp}.md`, 'text/markdown;charset=utf-8')
      ElMessage.success('案例Markdown已导出')
      return
    }

    ElMessage.warning('不支持的导出格式')
  } catch (error) {
    console.error('导出案例失败:', error)
    ElMessage.error('导出失败，请稍后重试')
  }
}

const fetchAllFilteredCaseSummaries = async () => {
  const pageSize = 50
  let pageNum = 1
  const allRecords = []
  let total = 0

  do {
    const pageData = await sqlOptimizerApi.getWorkbenchCases({
      pageNum,
      pageSize,
      favorite: historyFilters.favorite,
      hasRewrite: historyFilters.hasRewrite,
      hasCompare: historyFilters.hasCompare,
      highestSeverity: historyFilters.highestSeverity || undefined,
      sortBy: historySort.sortBy,
      sortOrder: historySort.sortOrder
    })

    const records = pageData?.records || []
    total = pageData?.total || 0
    allRecords.push(...records)
    pageNum += 1
  } while (allRecords.length < total)

  return allRecords
}

const buildFilteredCasesMarkdown = (records) => {
  const lines = [
    '# SQL优化工作台筛选案例导出',
    '',
    `- 导出时间：${new Date().toLocaleString()}`,
    `- 总记录数：${records.length}`,
    `- 收藏筛选：${historyFilters.favorite === undefined ? '全部' : (historyFilters.favorite ? '仅收藏' : '未收藏')}`,
    `- 重写筛选：${historyFilters.hasRewrite === undefined ? '全部' : (historyFilters.hasRewrite ? '含重写' : '不含重写')}`,
    `- 对比筛选：${historyFilters.hasCompare === undefined ? '全部' : (historyFilters.hasCompare ? '含对比' : '不含对比')}`,
    `- 严重级别筛选：${historyFilters.highestSeverity || '全部'}`,
    `- 排序：${historySort.sortBy} ${historySort.sortOrder}`,
    '',
    '## 案例列表'
  ]

  if (!records.length) {
    lines.push('- 无匹配案例')
    return lines.join('\n')
  }

  records.forEach((item, index) => {
    lines.push(`${index + 1}. #${item.id} | 评分 ${item.score ?? '-'} | 严重级别 ${item.highestSeverity || 'NONE'} | 创建时间 ${item.createTime || '-'}`)
    lines.push(`   - SQL预览：${item.originalSqlPreview || '无'}`)
    lines.push(`   - 标签：${item.favorite ? '收藏' : '未收藏'} / ${item.hasRewrite ? '含重写' : '无重写'} / ${item.hasCompare ? '含对比' : '无对比'} / ${item.fallback ? '降级' : '正常'}`)
  })

  return lines.join('\n')
}

const exportFilteredCases = async (format) => {
  historyExportLoading.value = true
  try {
    const records = await fetchAllFilteredCaseSummaries()
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-')

    if (format === 'json') {
      downloadTextFile(
        JSON.stringify({
          exportTime: new Date().toISOString(),
          filters: { ...historyFilters },
          sort: { ...historySort },
          total: records.length,
          records
        }, null, 2),
        `sql-workbench-cases-filtered-${timestamp}.json`,
        'application/json;charset=utf-8'
      )
      ElMessage.success(`已导出${records.length}条筛选案例（JSON）`)
      return
    }

    if (format === 'markdown') {
      downloadTextFile(
        buildFilteredCasesMarkdown(records),
        `sql-workbench-cases-filtered-${timestamp}.md`,
        'text/markdown;charset=utf-8'
      )
      ElMessage.success(`已导出${records.length}条筛选案例（Markdown）`)
      return
    }

    ElMessage.warning('不支持的导出格式')
  } catch (error) {
    console.error('导出筛选案例失败:', error)
    ElMessage.error('导出筛选案例失败')
  } finally {
    historyExportLoading.value = false
  }
}

const buildPayload = () => ({
  sql: form.sql.trim(),
  explainResult: form.explainResult.trim(),
  explainFormat: form.explainFormat,
  mysqlVersion: form.mysqlVersion,
  tableStructures: form.tableStructures
    .filter((item) => item.ddl && item.ddl.trim())
    .map((item) => ({
      tableName: (item.tableName || '').trim(),
      ddl: item.ddl.trim()
    }))
})

const syncCompareWithCurrentInput = () => {
  compareForm.beforeSql = form.sql
  compareForm.beforeExplain = form.explainResult
  compareForm.explainFormat = form.explainFormat
  if (rewriteResult.value?.optimizedSql) {
    compareForm.afterSql = rewriteResult.value.optimizedSql
  }
}

const runAnalyze = async () => {
  if (!canAnalyze.value) {
    ElMessage.warning('请先补全 SQL、EXPLAIN 和至少一份表结构 DDL')
    return
  }

  analyzeLoading.value = true
  try {
    const data = await sqlOptimizerApi.analyzeWorkbench(buildPayload())
    analysisResult.value = data?.analysis || null
    rewriteResult.value = data?.rewrite || null
    currentRecordId.value = data?.recordId || null
    syncCompareWithCurrentInput()

    if (analysisResult.value?.fallback) {
      ElMessage.warning('分析已完成：当前为降级结果，待统一 AI 运行时配置完成后会输出完整结果')
    } else {
      ElMessage.success('分析完成')
    }

    await loadHistory()
  } catch (error) {
    console.error('工作台分析失败:', error)
  } finally {
    analyzeLoading.value = false
  }
}

const runRewrite = async () => {
  if (!canAnalyze.value) {
    ElMessage.warning('请先补全 SQL、EXPLAIN 和至少一份表结构 DDL')
    return
  }

  rewriteLoading.value = true
  try {
    const data = await sqlOptimizerApi.rewriteWorkbench(buildPayload())
    analysisResult.value = data?.analysis || analysisResult.value
    rewriteResult.value = data?.rewrite || null
    currentRecordId.value = data?.recordId || currentRecordId.value
    syncCompareWithCurrentInput()

    if (data?.fallback) {
      ElMessage.warning('重写建议已生成：当前包含降级结果，可在统一 AI 运行时配置完成后重试')
    } else {
      ElMessage.success('重写建议生成完成')
    }

    await loadHistory()
  } catch (error) {
    console.error('生成重写建议失败:', error)
  } finally {
    rewriteLoading.value = false
  }
}

const loadHistory = async () => {
  historyLoading.value = true
  try {
    const data = await sqlOptimizerApi.getWorkbenchCases({
      pageNum: historyPagination.pageNum,
      pageSize: historyPagination.pageSize,
      favorite: historyFilters.favorite,
      hasRewrite: historyFilters.hasRewrite,
      hasCompare: historyFilters.hasCompare,
      highestSeverity: historyFilters.highestSeverity || undefined,
      sortBy: historySort.sortBy,
      sortOrder: historySort.sortOrder
    })
    historyList.value = data?.records || []
    historyTotal.value = data?.total || 0
  } catch (error) {
    console.error('加载历史失败:', error)
  } finally {
    historyLoading.value = false
  }
}

const onHistoryFilterChange = () => {
  historyPagination.pageNum = 1
  loadHistory()
}

const onHistoryPageChange = (page) => {
  historyPagination.pageNum = page
  loadHistory()
}

const onHistorySizeChange = (size) => {
  historyPagination.pageSize = size
  historyPagination.pageNum = 1
  loadHistory()
}

const loadHistoryDetail = async (item) => {
  if (!item?.id) {
    return
  }
  try {
    const detail = await sqlOptimizerApi.getWorkbenchCaseDetail(item.id)
    analysisResult.value = detail?.analysis || null
    rewriteResult.value = detail?.rewrite || null
    compareResult.value = detail?.compare || null
    currentRecordId.value = item.id

    form.sql = detail?.originalSql || ''
    form.explainResult = detail?.explainResult || ''
    form.explainFormat = detail?.explainFormat || 'TABLE'
    form.mysqlVersion = detail?.mysqlVersion || '8.0'
    if (Array.isArray(detail?.tableStructures) && detail.tableStructures.length) {
      form.tableStructures = detail.tableStructures.map((table) => ({
        tableName: table?.tableName || '',
        ddl: table?.ddl || ''
      }))
    } else {
      form.tableStructures = [{ tableName: '', ddl: '' }]
    }

    compareForm.beforeSql = detail?.originalSql || ''
    compareForm.beforeExplain = detail?.explainResult || ''
    compareForm.explainFormat = detail?.explainFormat || 'TABLE'
    if (detail?.rewrite?.optimizedSql) {
      compareForm.afterSql = detail.rewrite.optimizedSql
    }

    ElMessage.success(`已加载记录 #${item.id}`)
  } catch (error) {
    console.error('加载记录详情失败:', error)
    ElMessage.error('加载记录详情失败')
  }
}

const toggleCaseFavorite = async (item) => {
  if (!item?.id) {
    return
  }
  try {
    const favorite = await sqlOptimizerApi.toggleFavorite(item.id)
    item.favorite = !!favorite
    ElMessage.success(favorite ? '已收藏案例' : '已取消收藏')
    await loadHistory()
  } catch (error) {
    console.error('切换收藏状态失败:', error)
    ElMessage.error('收藏状态更新失败')
  }
}

const openBatchDialog = () => {
  if (!batchInput.value.trim()) {
    batchInput.value = JSON.stringify([buildPayload()], null, 2)
  }
  batchResult.value = null
  batchSortMode.value = 'index'
  lastBatchPayload.value = []
  batchDialogVisible.value = true
}

const parseBatchInput = () => {
  try {
    return JSON.parse(batchInput.value)
  } catch (error) {
    ElMessage.error('JSON 格式不正确，请检查后重试')
    return null
  }
}

const executeBatchAnalyze = async (items, isRetry = false) => {
  if (isRetry) {
    batchRetryLoading.value = true
  } else {
    batchLoading.value = true
  }

  try {
    const data = await sqlOptimizerApi.batchAnalyzeWorkbench({ items })
    batchResult.value = data
    lastBatchPayload.value = items
    ElMessage.success(isRetry ? '失败项重试完成' : '批量分析完成')
    await loadHistory()
  } catch (error) {
    console.error('批量分析失败:', error)
    ElMessage.error(isRetry ? '失败项重试失败' : '批量分析失败')
  } finally {
    if (isRetry) {
      batchRetryLoading.value = false
    } else {
      batchLoading.value = false
    }
  }
}

const runBatchAnalyze = async () => {
  const parsed = parseBatchInput()
  if (!parsed) {
    return
  }

  if (!Array.isArray(parsed) || parsed.length === 0) {
    ElMessage.warning('请提供非空数组')
    return
  }

  if (parsed.length > 20) {
    ElMessage.warning('单次最多支持20条')
    return
  }

  await executeBatchAnalyze(parsed, false)
}

const retryFailedBatchItems = async () => {
  if (!batchResult.value?.items?.length) {
    ElMessage.warning('暂无可重试项')
    return
  }

  const retryItems = batchResult.value.items
    .filter((item) => !item.success)
    .map((item) => lastBatchPayload.value[item.index])
    .filter(Boolean)

  if (!retryItems.length) {
    ElMessage.info('没有失败项需要重试')
    return
  }

  batchInput.value = JSON.stringify(retryItems, null, 2)
  await executeBatchAnalyze(retryItems, true)
}

const runCompare = async () => {
  if (!compareForm.beforeSql.trim() || !compareForm.afterSql.trim() || !compareForm.beforeExplain.trim() || !compareForm.afterExplain.trim()) {
    ElMessage.warning('请先补全对比输入')
    return
  }

  compareLoading.value = true
  try {
    const data = await sqlOptimizerApi.compareWorkbench({
      recordId: currentRecordId.value || undefined,
      beforeSql: compareForm.beforeSql,
      afterSql: compareForm.afterSql,
      beforeExplain: compareForm.beforeExplain,
      afterExplain: compareForm.afterExplain,
      explainFormat: compareForm.explainFormat
    })
    compareResult.value = data?.compare || null
    if (data?.fallback) {
      ElMessage.warning('收益对比已完成：当前为降级结果，可在统一 AI 运行时配置完成后重试')
    } else {
      ElMessage.success('收益对比完成')
    }
    await loadHistory()
  } catch (error) {
    console.error('收益对比失败:', error)
  } finally {
    compareLoading.value = false
  }
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
.sql-workbench-page {
  min-height: calc(100vh - 120px);
  padding: 24px;
  background: linear-gradient(180deg, #f3f8ff 0%, #eef3fb 100%);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.page-header h2 {
  margin: 0;
  font-size: 28px;
  color: #13315c;
}

.page-header p {
  margin: 8px 0 0;
  color: #5f6c88;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.panel-card {
  margin-bottom: 18px;
  border-radius: 16px;
  border: 1px solid #dbe6f6;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 700;
  color: #1b3a66;
}

.history-header-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.ddl-list {
  width: 100%;
}

.ddl-item {
  padding: 12px;
  margin-bottom: 12px;
  border-radius: 12px;
  border: 1px solid #dce8f8;
  background: #f8fbff;
}

.ddl-table-name {
  margin-bottom: 10px;
}

.ddl-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 6px;
}

.summary-card {
  text-align: center;
}

.summary-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.summary-meta {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.summary-desc {
  margin-top: 10px;
  color: #51607f;
  font-size: 13px;
}

.summary-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}

.history-toolbar {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 10px;
}

.history-filter {
  width: 100%;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.history-item {
  display: flex;
  align-items: stretch;
  border: 1px solid #dbe5f6;
  border-radius: 10px;
  background: #f7faff;
}

.history-main {
  flex: 1;
  width: 100%;
  padding: 10px 12px;
  text-align: left;
  cursor: pointer;
  border: none;
  background: transparent;
}

.history-item:hover {
  border-color: #3a7afe;
}

.history-title {
  font-size: 13px;
  font-weight: 600;
  color: #1b3a66;
}

.history-preview {
  margin-top: 6px;
  font-size: 12px;
  color: #51607f;
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.history-time {
  margin-top: 4px;
  font-size: 12px;
  color: #667892;
}

.history-actions {
  display: flex;
  align-items: center;
  padding-right: 10px;
}

.history-pagination {
  margin-top: 12px;
  justify-content: flex-end;
}

.result-card {
  margin-top: 2px;
}

.section-title {
  margin: 18px 0 10px;
  color: #1b3a66;
}

.section-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.rewrite-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.rewrite-card {
  border: 1px solid #dbe6f6;
  border-radius: 12px;
}

.rewrite-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.risk-list {
  padding-left: 18px;
  margin: 0;
}

.risk-list li {
  margin-bottom: 6px;
  color: #42526e;
}

.expected-improve {
  margin-top: 10px;
  margin-bottom: 0;
  color: #1f6feb;
  font-size: 13px;
}

.batch-textarea {
  margin-top: 12px;
}

.batch-summary {
  display: flex;
  gap: 8px;
  margin: 12px 0;
}

.batch-tools {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.batch-sort {
  width: 220px;
}

.compare-actions {
  display: flex;
  justify-content: flex-end;
}

.compare-result {
  margin-top: 12px;
}

.compare-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 10px;
}

.metric-item {
  padding: 10px;
  border-radius: 10px;
  border: 1px solid #d8e3f6;
  background: #f8fbff;
}

.metric-item label {
  display: block;
  font-size: 12px;
  color: #6c7b95;
}

.metric-item strong {
  display: block;
  margin-top: 4px;
  color: #1b3a66;
}

.compare-table {
  margin-top: 10px;
}

@media (max-width: 992px) {
  .sql-workbench-page {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .rewrite-grid {
    grid-template-columns: 1fr;
  }

  .compare-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .batch-tools {
    flex-direction: column;
    align-items: stretch;
  }

  .batch-sort {
    width: 100%;
  }

  .history-toolbar {
    grid-template-columns: 1fr;
  }

  .summary-actions {
    flex-direction: column;
    width: 100%;
  }

  .history-header-actions {
    flex-wrap: wrap;
    justify-content: flex-end;
  }
}
</style>
