<template>
  <CnPage class="sensitive-strategy-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="策略配置"
      description="按业务模块和风险等级配置敏感词命中后的处理动作、通知、用户限制和启用状态。"
      eyebrow="Sensitive Strategy"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">内容安全</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ tableData.length }} 条</CnStatusTag>
        <CnStatusTag type="danger">限制用户 {{ limitedCount }} 条</CnStatusTag>
        <CnStatusTag type="success">启用 {{ enabledCount }} 条</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="loadStrategies">刷新</el-button>
        <el-button type="warning" :icon="Refresh" @click="handleRefreshCache">刷新缓存</el-button>
      </template>
    </CnPageHeader>

    <div class="sensitive-stat-grid">
      <CnStatCard title="策略总量" :value="tableData.length" description="当前已加载的策略配置数量" tone="brand" />
      <CnStatCard title="启用策略" :value="enabledCount" description="状态为启用的策略配置" tone="success" />
      <CnStatCard title="通知管理员" :value="notifyCount" description="命中后会通知管理员的策略" tone="warning" />
      <CnStatCard title="限制用户" :value="limitedCount" description="命中后会限制用户行为的策略" tone="danger" />
    </div>

    <CnSection
      v-for="moduleItem in modules"
      :key="moduleItem.value"
      :title="moduleItem.label"
      :description="`${getModuleStrategies(moduleItem.value).length} 条策略配置`"
      divided
    >
      <CnDataTable
        :columns="tableColumns"
        :data="getModuleStrategies(moduleItem.value)"
        :loading="loading"
        :pagination="null"
        row-key="id"
      >
        <template #toolbar>
          <CnToolbar :title="moduleItem.title" :description="moduleItem.description" align="center">
            <template #meta>
              <CnStatusTag type="neutral" size="sm">{{ moduleItem.value }}</CnStatusTag>
              <CnStatusTag type="brand" size="sm">
                {{ getModuleStrategies(moduleItem.value).length }} 条
              </CnStatusTag>
            </template>
          </CnToolbar>
        </template>

        <template #level="{ row }">
          <CnStatusTag :type="getLevelTone(row.level)" size="sm">
            {{ getLevelText(row.level) }}
          </CnStatusTag>
        </template>

        <template #action="{ row }">
          <CnStatusTag :type="getActionTone(row.action)" size="sm">
            {{ getActionText(row.action) }}
          </CnStatusTag>
        </template>

        <template #notifyAdmin="{ row }">
          <CnStatusTag :type="normalizeFlag(row.notifyAdmin) ? 'success' : 'neutral'" size="sm">
            {{ normalizeFlag(row.notifyAdmin) ? '是' : '否' }}
          </CnStatusTag>
        </template>

        <template #limitUser="{ row }">
          <CnStatusTag :type="normalizeFlag(row.limitUser) ? 'danger' : 'neutral'" size="sm">
            {{ normalizeFlag(row.limitUser) ? '是' : '否' }}
          </CnStatusTag>
        </template>

        <template #limitDuration="{ row }">
          <span class="muted-text">{{ row.limitDuration ? `${row.limitDuration} 分钟` : '-' }}</span>
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="row.status === 1 ? 'success' : 'danger'" size="sm">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" link size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link size="small" :icon="Refresh" @click="handleReset(row)">重置</el-button>
          </div>
        </template>

        <template #empty>
          <CnEmptyState
            title="暂无策略"
            :description="`${moduleItem.label} 当前没有策略配置。`"
            icon="ST"
            surface="transparent"
          />
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog
      v-model="dialogVisible"
      title="编辑策略"
      width="600px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px">
        <el-form-item label="策略名称">
          <el-input v-model="form.strategyName" disabled />
        </el-form-item>
        <el-form-item label="模块">
          <el-input v-model="form.module" disabled />
        </el-form-item>
        <el-form-item label="风险等级">
          <CnStatusTag :type="getLevelTone(form.level)" size="sm">
            {{ getLevelText(form.level) }}
          </CnStatusTag>
        </el-form-item>
        <el-form-item label="处理动作" prop="action">
          <el-select v-model="form.action" placeholder="请选择处理动作" class="full-width-control">
            <el-option label="替换" value="replace" />
            <el-option label="拒绝" value="reject" />
            <el-option label="警告" value="warn" />
          </el-select>
        </el-form-item>
        <el-form-item label="通知管理员" prop="notifyAdmin">
          <el-radio-group v-model="form.notifyAdmin">
            <el-radio :value="1">是</el-radio>
            <el-radio :value="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="限制用户" prop="limitUser">
          <el-radio-group v-model="form.limitUser">
            <el-radio :value="1">是</el-radio>
            <el-radio :value="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.limitUser === 1" label="限制时长(分钟)" prop="limitDuration">
          <el-input-number v-model="form.limitDuration" :min="1" :max="43200" class="full-width-control" />
        </el-form-item>
        <el-form-item label="策略描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Refresh } from '@element-plus/icons-vue'
import { listStrategy, refreshStrategyCache, resetStrategy, updateStrategy } from '@/api/sensitive'
import {
  CnDataTable,
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag,
  CnToolbar
} from '@/design-system'
import type { CnBreadcrumbItem, CnTableColumn, CnTone } from '@/design-system'

type StrategyAction = 'replace' | 'reject' | 'warn' | string

interface StrategyRecord {
  id: number
  strategyName: string
  module: string
  level: number | null
  action: StrategyAction
  notifyAdmin: number | boolean
  limitUser: number | boolean
  limitDuration: number | null
  description?: string
  status: number
  [key: string]: unknown
}

interface StrategyForm {
  id: number | null
  strategyName: string
  module: string
  level: number | null
  action: StrategyAction
  notifyAdmin: number
  limitUser: number
  limitDuration: number | null
  description: string
  status: number
}

interface StrategyModule {
  label: string
  title: string
  description: string
  value: string
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '敏感词管理' }, { label: '策略配置' }]

const loading = ref(false)
const dialogVisible = ref(false)
const tableData = ref<StrategyRecord[]>([])
const formRef = ref<FormInstance>()

const modules: StrategyModule[] = [
  {
    label: '社区模块 (community)',
    title: '社区模块策略',
    description: '覆盖帖子、评论和社区互动内容的敏感词处理策略。',
    value: 'community'
  },
  {
    label: '面试模块 (interview)',
    title: '面试模块策略',
    description: '覆盖面试题、答案解析和模拟面试文本的检测策略。',
    value: 'interview'
  },
  {
    label: '朋友圈模块 (moment)',
    title: '朋友圈模块策略',
    description: '覆盖动态、评论和互动内容的敏感词处理策略。',
    value: 'moment'
  },
  {
    label: '博客模块 (blog)',
    title: '博客模块策略',
    description: '覆盖博客文章、摘要和评论内容的敏感词处理策略。',
    value: 'blog'
  }
]

const form = reactive<StrategyForm>({
  id: null,
  strategyName: '',
  module: '',
  level: null,
  action: '',
  notifyAdmin: 0,
  limitUser: 0,
  limitDuration: null,
  description: '',
  status: 1
})

const formRules: FormRules<StrategyForm> = {
  action: [{ required: true, message: '请选择处理动作', trigger: 'change' }],
  notifyAdmin: [{ required: true, message: '请选择是否通知管理员', trigger: 'change' }],
  limitUser: [{ required: true, message: '请选择是否限制用户', trigger: 'change' }],
  limitDuration: [
    {
      validator: (_rule, value, callback) => {
        if (form.limitUser === 1 && !value) {
          callback(new Error('请输入限制时长'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const tableColumns: CnTableColumn<StrategyRecord>[] = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'strategyName', label: '策略名称', minWidth: 150, showOverflowTooltip: true },
  { prop: 'level', label: '风险等级', width: 110, slot: 'level' },
  { prop: 'action', label: '处理动作', width: 110, slot: 'action' },
  { prop: 'notifyAdmin', label: '通知管理员', width: 110, slot: 'notifyAdmin' },
  { prop: 'limitUser', label: '限制用户', width: 110, slot: 'limitUser' },
  { prop: 'limitDuration', label: '限制时长', width: 120, slot: 'limitDuration' },
  { prop: 'description', label: '策略描述', minWidth: 220, showOverflowTooltip: true },
  { prop: 'status', label: '状态', width: 90, slot: 'status' },
  { label: '操作', width: 130, fixed: 'right', slot: 'actions' }
]

const enabledCount = computed(() => tableData.value.filter((item) => item.status === 1).length)
const notifyCount = computed(() => tableData.value.filter((item) => normalizeFlag(item.notifyAdmin)).length)
const limitedCount = computed(() => tableData.value.filter((item) => normalizeFlag(item.limitUser)).length)

onMounted(() => {
  loadStrategies()
})

const loadStrategies = async () => {
  loading.value = true
  try {
    const response = await listStrategy({
      pageNum: 1,
      pageSize: 100
    })
    tableData.value = response?.records || []
  } catch (error) {
    console.error('查询策略配置失败:', error)
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

const getModuleStrategies = (moduleName: string) => {
  return tableData.value.filter((item) => item.module === moduleName)
}

const handleEdit = (row: StrategyRecord) => {
  Object.assign(form, {
    id: row.id,
    strategyName: row.strategyName || '',
    module: row.module || '',
    level: row.level ?? null,
    action: row.action || '',
    notifyAdmin: normalizeFlag(row.notifyAdmin) ? 1 : 0,
    limitUser: normalizeFlag(row.limitUser) ? 1 : 0,
    limitDuration: row.limitDuration ?? null,
    description: row.description || '',
    status: Number(row.status) === 0 ? 0 : 1
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    await updateStrategy({
      ...form,
      limitDuration: form.limitUser === 1 ? form.limitDuration : null
    })
    ElMessage.success('更新成功')
    dialogVisible.value = false
    loadStrategies()
  } catch (error) {
    console.error('更新策略配置失败:', error)
    ElMessage.error('更新失败')
  }
}

const handleReset = async (row: StrategyRecord) => {
  try {
    await ElMessageBox.confirm(`确定要重置策略 "${row.strategyName}" 为默认值吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await resetStrategy(row.id)
    ElMessage.success('重置成功')
    loadStrategies()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重置策略配置失败:', error)
      ElMessage.error('重置失败')
    }
  }
}

const handleRefreshCache = async () => {
  try {
    await refreshStrategyCache()
    ElMessage.success('缓存刷新成功')
  } catch (error) {
    console.error('刷新策略缓存失败:', error)
    ElMessage.error('缓存刷新失败')
  }
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    strategyName: '',
    module: '',
    level: null,
    action: '',
    notifyAdmin: 0,
    limitUser: 0,
    limitDuration: null,
    description: '',
    status: 1
  })
  formRef.value?.resetFields()
}

const normalizeFlag = (value: number | boolean | undefined | null) => value === true || value === 1

const getLevelText = (level: number | null) => {
  const levelMap: Record<number, string> = { 1: '低风险', 2: '中风险', 3: '高风险' }
  return level ? levelMap[level] || '未知' : '未知'
}

const getLevelTone = (level: number | null): CnTone => {
  const toneMap: Record<number, CnTone> = { 1: 'success', 2: 'warning', 3: 'danger' }
  return level ? toneMap[level] || 'info' : 'info'
}

const getActionText = (action: StrategyAction) => {
  const actionMap: Record<string, string> = { replace: '替换', reject: '拒绝', warn: '警告' }
  return actionMap[action] || String(action || '未知')
}

const getActionTone = (action: StrategyAction): CnTone => {
  const toneMap: Record<string, CnTone> = { replace: 'info', reject: 'danger', warn: 'warning' }
  return toneMap[action] || 'info'
}
</script>

<style scoped>
.sensitive-strategy-page {
  min-height: 100%;
}

.sensitive-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.muted-text {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.table-actions,
.dialog-footer {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.table-actions .el-button {
  margin-left: 0;
}

.dialog-footer {
  justify-content: flex-end;
}

@media (max-width: 1180px) {
  .sensitive-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .sensitive-stat-grid {
    grid-template-columns: 1fr;
  }

  .dialog-footer {
    justify-content: flex-start;
  }
}
</style>
