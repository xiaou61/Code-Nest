<template>
  <div class="prize-config">
    <CnSection title="奖品操作" description="维护奖品配置、启停状态、暂停恢复和概率归一化。" surface="plain" divided>
      <div class="action-bar">
        <div class="action-left">
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增奖品</el-button>
          <el-button :icon="Refresh" @click="loadPrizeList">刷新</el-button>
          <el-button :icon="Tools" @click="normalizeDialog = true">概率归一化</el-button>
        </div>
        <div class="action-right">
          <el-button type="success" :disabled="selectedIds.length === 0" @click="handleBatchEnable">批量启用</el-button>
          <el-button type="warning" :disabled="selectedIds.length === 0" @click="handleBatchDisable">批量禁用</el-button>
        </div>
      </div>
    </CnSection>

    <CnSection title="奖品列表" :description="`当前共 ${prizeList.length} 个奖品配置`" surface="plain" divided>
      <CnDataTable
        :columns="columns"
        :data="prizeList"
        :loading="loading"
        :pagination="null"
        row-key="id"
        border
        empty-title="暂无奖品配置"
        empty-description="还没有配置奖品，可以先新增一个奖品。"
        empty-icon="PC"
        @selection-change="handleSelectionChange"
      >
        <template #prizeLevel="{ row }">
          <CnStatusTag :type="getLevelTone(row.prizeLevel)" size="sm">
            {{ getLevelName(row.prizeLevel) }}
          </CnStatusTag>
        </template>

        <template #baseProbability="{ row }">
          {{ formatProbability(row.baseProbability) }}
        </template>

        <template #currentProbability="{ row }">
          <span :class="{ 'probability-changed': row.currentProbability !== row.baseProbability }">
            {{ formatProbability(row.currentProbability) }}
          </span>
        </template>

        <template #stock="{ row }">
          <span v-if="row.totalStock && row.totalStock > 0">{{ row.currentStock }} / {{ row.totalStock }}</span>
          <CnStatusTag v-else type="info" size="sm" subtle>无限制</CnStatusTag>
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="getPrizeStatusTone(row)" size="sm">
            {{ getPrizeStatusText(row) }}
          </CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link :type="row.isActive === 1 ? 'warning' : 'success'" size="small" @click="handleToggleStatus(row)">
              {{ row.isActive === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button v-if="row.isSuspended === 1" link type="success" size="small" @click="handleResume(row)">恢复</el-button>
            <el-button v-else link type="warning" size="small" @click="handleSuspend(row)">暂停</el-button>
            <el-button link type="primary" size="small" @click="handleAdjustProbability(row)">调整概率</el-button>
          </div>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-form-item label="奖品名称" prop="prizeName">
          <el-input v-model="formData.prizeName" placeholder="请输入奖品名称" />
        </el-form-item>
        <el-form-item label="奖品等级" prop="prizeLevel">
          <el-select v-model="formData.prizeLevel" placeholder="请选择奖品等级">
            <el-option label="特等奖" :value="1" />
            <el-option label="一等奖" :value="2" />
            <el-option label="二等奖" :value="3" />
            <el-option label="三等奖" :value="4" />
            <el-option label="四等奖" :value="5" />
            <el-option label="五等奖" :value="6" />
            <el-option label="六等奖" :value="7" />
            <el-option label="未中奖" :value="8" />
          </el-select>
        </el-form-item>
        <el-form-item label="奖励积分" prop="prizePoints">
          <el-input-number v-model="formData.prizePoints" :min="0" />
        </el-form-item>
        <el-form-item label="基础概率" prop="baseProbability">
          <el-input-number v-model="formData.baseProbability" :min="0" :max="1" :step="0.0001" :precision="4" />
          <span class="inline-tip">{{ formatProbability(formData.baseProbability) }}</span>
        </el-form-item>
        <el-form-item label="目标回报率" prop="targetReturnRate">
          <el-input-number v-model="formData.targetReturnRate" :min="0" :max="1" :step="0.0001" :precision="4" />
          <span class="inline-tip">{{ formatProbability(formData.targetReturnRate) }}</span>
        </el-form-item>
        <el-form-item label="最大回报率" prop="maxReturnRate">
          <el-input-number v-model="formData.maxReturnRate" :min="0" :max="1" :step="0.0001" :precision="4" />
          <span class="inline-tip">{{ formatProbability(formData.maxReturnRate) }}</span>
        </el-form-item>
        <el-form-item label="最小回报率" prop="minReturnRate">
          <el-input-number v-model="formData.minReturnRate" :min="0" :max="1" :step="0.0001" :precision="4" />
          <span class="inline-tip">{{ formatProbability(formData.minReturnRate) }}</span>
        </el-form-item>
        <el-form-item label="总库存">
          <el-input-number v-model="formData.totalStock" :min="-1" />
          <span class="inline-tip">-1表示无限制</span>
        </el-form-item>
        <el-form-item label="调整策略" prop="adjustStrategy">
          <el-select v-model="formData.adjustStrategy">
            <el-option label="自动调整" value="AUTO" />
            <el-option label="手动调整" value="MANUAL" />
            <el-option label="固定概率" value="FIXED" />
          </el-select>
        </el-form-item>
        <el-form-item label="奖品描述">
          <el-input v-model="formData.prizeDesc" type="textarea" :rows="3" placeholder="请输入奖品描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="suspendDialog" title="暂停奖品" width="400px">
      <el-form label-width="100px">
        <el-form-item label="暂停时长">
          <el-input-number v-model="suspendMinutes" :min="0" />
          <span class="inline-tip">分钟（0表示手动恢复）</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="suspendDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmSuspend">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="adjustDialog" title="调整概率" width="400px">
      <el-form label-width="100px">
        <el-form-item label="当前概率">
          <span>{{ formatProbability(currentPrize?.currentProbability) }}</span>
        </el-form-item>
        <el-form-item label="新概率">
          <el-input-number v-model="newProbability" :min="0" :max="1" :step="0.0001" :precision="4" />
          <span class="inline-tip">{{ formatProbability(newProbability) }}</span>
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input v-model="adjustReason" type="textarea" :rows="3" placeholder="请输入调整原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmAdjust">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="normalizeDialog" title="概率归一化" width="500px">
      <el-alert
        title="归一化说明"
        type="info"
        description="将所有奖品的当前概率按比例调整，确保概率总和为1.0"
        :closable="false"
        class="normalize-alert"
      />
      <el-button type="primary" class="normalize-action" @click="validateProbability">验证当前概率</el-button>
      <div v-if="probabilitySum !== null" class="normalize-result">
        <p>当前概率总和：<strong>{{ probabilitySum }}</strong></p>
        <CnStatusTag v-if="needsNormalization" type="warning">需要归一化</CnStatusTag>
        <CnStatusTag v-else type="success">概率正常</CnStatusTag>
      </div>
      <template #footer>
        <el-button @click="normalizeDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!needsNormalization" @click="confirmNormalize">执行归一化</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Refresh, Tools } from '@element-plus/icons-vue'
import { lotteryAdminApi } from '@/api/lotteryAdmin'
import { CnDataTable, CnSection, CnStatusTag } from '@/design-system'
import type { CnTableColumn, CnTone } from '@/design-system'

interface PrizeConfig extends Record<string, unknown> {
  id: number | null
  prizeName: string
  prizeLevel: number
  prizePoints?: number
  baseProbability?: number
  currentProbability?: number
  targetReturnRate?: number
  maxReturnRate?: number
  minReturnRate?: number
  totalStock?: number
  currentStock?: number
  adjustStrategy?: string
  prizeDesc?: string
  isActive?: number
  isSuspended?: number
}

interface PrizeForm {
  id: number | null
  prizeName: string
  prizeLevel: number
  prizePoints: number
  baseProbability: number
  targetReturnRate: number
  maxReturnRate: number
  minReturnRate: number
  totalStock: number
  adjustStrategy: string
  prizeDesc: string
}

const loading = ref(false)
const saving = ref(false)
const prizeList = ref<PrizeConfig[]>([])
const selectedIds = ref<number[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增奖品')
const formRef = ref<FormInstance>()
const suspendDialog = ref(false)
const suspendPrizeId = ref<number | null>(null)
const suspendMinutes = ref(60)
const adjustDialog = ref(false)
const currentPrize = ref<PrizeConfig | null>(null)
const newProbability = ref(0)
const adjustReason = ref('')
const normalizeDialog = ref(false)
const probabilitySum = ref<number | null>(null)
const needsNormalization = ref(false)

const formData = reactive<PrizeForm>({
  id: null,
  prizeName: '',
  prizeLevel: 1,
  prizePoints: 0,
  baseProbability: 0.1,
  targetReturnRate: 0.01,
  maxReturnRate: 0.015,
  minReturnRate: 0.005,
  totalStock: -1,
  adjustStrategy: 'AUTO',
  prizeDesc: ''
})

const formRules: FormRules<PrizeForm> = {
  prizeName: [{ required: true, message: '请输入奖品名称', trigger: 'blur' }],
  prizeLevel: [{ required: true, message: '请选择奖品等级', trigger: 'change' }],
  baseProbability: [{ required: true, message: '请输入基础概率', trigger: 'blur' }]
}

const columns: CnTableColumn<PrizeConfig>[] = [
  { type: 'selection', width: 55 },
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'prizeName', label: '奖品名称', minWidth: 140, showOverflowTooltip: true },
  { prop: 'prizeLevel', label: '奖品等级', width: 110, slot: 'prizeLevel' },
  { prop: 'prizePoints', label: '奖励积分', width: 100 },
  { prop: 'baseProbability', label: '基础概率', width: 120, slot: 'baseProbability' },
  { prop: 'currentProbability', label: '当前概率', width: 120, slot: 'currentProbability' },
  { label: '库存', width: 120, slot: 'stock' },
  { label: '状态', width: 110, slot: 'status' },
  { label: '操作', width: 330, fixed: 'right', slot: 'actions' }
]

const loadPrizeList = async () => {
  loading.value = true
  try {
    prizeList.value = await lotteryAdminApi.getPrizeConfigList()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  Object.assign(formData, {
    id: null,
    prizeName: '',
    prizeLevel: 1,
    prizePoints: 0,
    baseProbability: 0.1,
    targetReturnRate: 0.01,
    maxReturnRate: 0.015,
    minReturnRate: 0.005,
    totalStock: -1,
    adjustStrategy: 'AUTO',
    prizeDesc: ''
  })
}

const handleAdd = () => {
  dialogTitle.value = '新增奖品'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: PrizeConfig) => {
  dialogTitle.value = '编辑奖品'
  Object.assign(formData, { ...row })
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    await lotteryAdminApi.savePrizeConfig(formData)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadPrizeList()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

const handleToggleStatus = async (row: PrizeConfig) => {
  try {
    const isActive = row.isActive !== 1
    await lotteryAdminApi.togglePrizeStatus(row.id, isActive)
    ElMessage.success(isActive ? '启用成功' : '禁用成功')
    loadPrizeList()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  }
}

const handleSuspend = (row: PrizeConfig) => {
  suspendPrizeId.value = row.id
  suspendMinutes.value = 60
  suspendDialog.value = true
}

const confirmSuspend = async () => {
  try {
    await lotteryAdminApi.suspendPrize(suspendPrizeId.value, suspendMinutes.value)
    ElMessage.success('暂停成功')
    suspendDialog.value = false
    loadPrizeList()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  }
}

const handleResume = async (row: PrizeConfig) => {
  try {
    await ElMessageBox.confirm('确认要恢复该奖品吗？', '提示', {
      type: 'warning'
    })
    await lotteryAdminApi.suspendPrize(row.id, 0)
    ElMessage.success('恢复成功')
    loadPrizeList()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

const handleAdjustProbability = (row: PrizeConfig) => {
  currentPrize.value = row
  newProbability.value = Number(row.currentProbability || 0)
  adjustReason.value = ''
  adjustDialog.value = true
}

const confirmAdjust = async () => {
  if (!adjustReason.value) {
    ElMessage.warning('请输入调整原因')
    return
  }
  try {
    await lotteryAdminApi.adjustProbability({
      prizeId: currentPrize.value?.id,
      newProbability: newProbability.value,
      reason: adjustReason.value
    })
    ElMessage.success('调整成功')
    adjustDialog.value = false
    loadPrizeList()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '调整失败')
  }
}

const validateProbability = async () => {
  try {
    const sum = await lotteryAdminApi.validateProbabilitySum()
    probabilitySum.value = sum
    needsNormalization.value = Math.abs(sum - 1.0) > 0.0001
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '验证失败')
  }
}

const confirmNormalize = async () => {
  try {
    await lotteryAdminApi.normalizeAllProbabilities()
    ElMessage.success('归一化成功')
    normalizeDialog.value = false
    loadPrizeList()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '归一化失败')
  }
}

const handleSelectionChange = (selection: unknown[]) => {
  selectedIds.value = (selection as PrizeConfig[]).map((item) => Number(item.id))
}

const handleBatchEnable = async () => {
  try {
    await lotteryAdminApi.batchToggleStatus({
      prizeIds: selectedIds.value,
      isActive: true
    })
    ElMessage.success('批量启用成功')
    loadPrizeList()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  }
}

const handleBatchDisable = async () => {
  try {
    await lotteryAdminApi.batchToggleStatus({
      prizeIds: selectedIds.value,
      isActive: false
    })
    ElMessage.success('批量禁用成功')
    loadPrizeList()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  }
}

const getLevelName = (level?: number) => {
  const names = ['', '特等奖', '一等奖', '二等奖', '三等奖', '四等奖', '五等奖', '六等奖', '未中奖']
  return level ? names[level] || '未知' : '未知'
}

const getLevelTone = (level?: number): CnTone => {
  if (level === 1) return 'danger'
  if (level && level <= 3) return 'warning'
  if (level && level <= 7) return 'success'
  return 'info'
}

const getPrizeStatusText = (row: PrizeConfig) => {
  if (row.isSuspended === 1) return '已暂停'
  if (row.isActive === 1) return '启用中'
  return '已禁用'
}

const getPrizeStatusTone = (row: PrizeConfig): CnTone => {
  if (row.isSuspended === 1) return 'danger'
  if (row.isActive === 1) return 'success'
  return 'info'
}

const formatProbability = (value?: number) => `${(Number(value || 0) * 100).toFixed(4)}%`

onMounted(() => {
  loadPrizeList()
})
</script>

<style scoped>
.prize-config {
  display: grid;
  gap: var(--cn-space-4);
}

.action-bar,
.action-left,
.action-right,
.table-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
}

.action-bar {
  justify-content: space-between;
}

.probability-changed {
  color: var(--cn-color-warning);
  font-weight: 700;
}

.inline-tip {
  margin-left: var(--cn-space-2);
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.normalize-alert,
.normalize-action {
  margin-bottom: var(--cn-space-4);
}

.normalize-result {
  display: grid;
  gap: var(--cn-space-2);
  margin-bottom: var(--cn-space-4);
}

.normalize-result p {
  margin: 0;
  color: var(--cn-color-text-secondary);
}
</style>
