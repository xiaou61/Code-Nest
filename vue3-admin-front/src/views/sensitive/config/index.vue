<template>
  <CnPage class="sensitive-config-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="配置管理"
      description="维护敏感词检测中的同音字和形似字映射，刷新缓存后会影响实时内容检测结果。"
      eyebrow="Sensitive Config"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">内容安全</CnStatusTag>
        <CnStatusTag type="neutral">同音字 {{ homophoneTotal }} 条</CnStatusTag>
        <CnStatusTag type="info">形似字 {{ similarCharTotal }} 条</CnStatusTag>
        <CnStatusTag type="success">启用 {{ enabledMappingCount }} 条</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="activeLoading" @click="handleRefreshActiveCache">刷新当前缓存</el-button>
        <el-button type="primary" :icon="Plus" @click="handleAddActiveMapping">新增映射</el-button>
      </template>
    </CnPageHeader>

    <div class="sensitive-stat-grid">
      <CnStatCard title="同音字映射" :value="homophoneTotal" description="可用于识别同音替换表达的映射数量" tone="brand" />
      <CnStatCard title="形似字映射" :value="similarCharTotal" description="可用于识别字形混淆表达的映射数量" tone="info" />
      <CnStatCard title="启用映射" :value="enabledMappingCount" description="当前页处于启用状态的映射数量" tone="success" />
      <CnStatCard title="当前配置" :value="activeTabLabel" description="正在查看和维护的检测配置类型" tone="warning" />
    </div>

    <CnSection title="字符映射配置" description="同音字和形似字配置会在检测前参与文本归一化处理。" divided>
      <el-tabs v-model="activeTab" class="config-tabs">
        <el-tab-pane label="同音字管理" name="homophone">
          <CnDataTable
            :columns="homophoneColumns"
            :data="homophoneData"
            :loading="homophoneLoading"
            :pagination="homophonePagination"
            row-key="id"
            @page-change="handleHomophonePageChange"
            @page-size-change="handleHomophonePageSizeChange"
          >
            <template #toolbar>
              <CnToolbar title="同音字数据" description="维护原始字符与同音替换字符的映射关系。" align="center">
                <template #meta>
                  <CnStatusTag type="neutral" size="sm">每页 {{ homophoneQuery.pageSize }} 条</CnStatusTag>
                  <CnStatusTag type="success" size="sm">启用 {{ enabledHomophoneCountInPage }} 条</CnStatusTag>
                </template>

                <el-button type="primary" :icon="Plus" @click="handleAddHomophone">新增同音字</el-button>
                <el-button type="warning" :icon="Refresh" @click="handleRefreshHomophoneCache">刷新缓存</el-button>
              </CnToolbar>
            </template>

            <template #homophoneOriginal="{ row }">
              <div class="mapping-cell">
                <strong>{{ row.originalChar || '-' }}</strong>
                <span>ID {{ row.id }}</span>
              </div>
            </template>

            <template #homophoneChars="{ row }">
              <span class="mapping-list">{{ row.homophoneChars || '-' }}</span>
            </template>

            <template #homophoneStatus="{ row }">
              <CnStatusTag :type="row.status === 1 ? 'success' : 'danger'" size="sm">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </CnStatusTag>
            </template>

            <template #homophoneActions="{ row }">
              <div class="table-actions">
                <el-button type="primary" link size="small" :icon="Edit" @click="handleEditHomophone(row)">编辑</el-button>
                <el-button type="danger" link size="small" :icon="Delete" @click="handleDeleteHomophone(row)">删除</el-button>
              </div>
            </template>

            <template #empty>
              <CnEmptyState
                title="暂无同音字映射"
                description="当前没有可展示的同音字配置，可以新增映射后刷新缓存。"
                icon="HY"
                surface="transparent"
              >
                <template #actions>
                  <el-button type="primary" @click="handleAddHomophone">新增同音字</el-button>
                </template>
              </CnEmptyState>
            </template>
          </CnDataTable>
        </el-tab-pane>

        <el-tab-pane label="形似字管理" name="similarChar">
          <CnDataTable
            :columns="similarCharColumns"
            :data="similarCharData"
            :loading="similarCharLoading"
            :pagination="similarCharPagination"
            row-key="id"
            @page-change="handleSimilarCharPageChange"
            @page-size-change="handleSimilarCharPageSizeChange"
          >
            <template #toolbar>
              <CnToolbar title="形似字数据" description="维护原始字符与字形相近字符的映射关系。" align="center">
                <template #meta>
                  <CnStatusTag type="neutral" size="sm">每页 {{ similarCharQuery.pageSize }} 条</CnStatusTag>
                  <CnStatusTag type="success" size="sm">启用 {{ enabledSimilarCharCountInPage }} 条</CnStatusTag>
                </template>

                <el-button type="primary" :icon="Plus" @click="handleAddSimilarChar">新增形似字</el-button>
                <el-button type="warning" :icon="Refresh" @click="handleRefreshSimilarCharCache">刷新缓存</el-button>
              </CnToolbar>
            </template>

            <template #similarCharOriginal="{ row }">
              <div class="mapping-cell">
                <strong>{{ row.originalChar || '-' }}</strong>
                <span>ID {{ row.id }}</span>
              </div>
            </template>

            <template #similarChars="{ row }">
              <span class="mapping-list">{{ row.similarChars || '-' }}</span>
            </template>

            <template #similarCharStatus="{ row }">
              <CnStatusTag :type="row.status === 1 ? 'success' : 'danger'" size="sm">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </CnStatusTag>
            </template>

            <template #similarCharActions="{ row }">
              <div class="table-actions">
                <el-button type="primary" link size="small" :icon="Edit" @click="handleEditSimilarChar(row)">编辑</el-button>
                <el-button type="danger" link size="small" :icon="Delete" @click="handleDeleteSimilarChar(row)">删除</el-button>
              </div>
            </template>

            <template #empty>
              <CnEmptyState
                title="暂无形似字映射"
                description="当前没有可展示的形似字配置，可以新增映射后刷新缓存。"
                icon="SC"
                surface="transparent"
              >
                <template #actions>
                  <el-button type="primary" @click="handleAddSimilarChar">新增形似字</el-button>
                </template>
              </CnEmptyState>
            </template>
          </CnDataTable>
        </el-tab-pane>
      </el-tabs>
    </CnSection>

    <el-dialog
      v-model="homophoneDialogVisible"
      :title="homophoneDialogTitle"
      width="600px"
      @close="resetHomophoneForm"
    >
      <el-form ref="homophoneFormRef" :model="homophoneForm" :rules="homophoneFormRules" label-width="100px">
        <el-form-item label="原始字符" prop="originalChar">
          <el-input v-model="homophoneForm.originalChar" placeholder="请输入原始字符" maxlength="10" />
        </el-form-item>
        <el-form-item label="同音字" prop="homophoneChars">
          <el-input
            v-model="homophoneForm.homophoneChars"
            type="textarea"
            :rows="3"
            placeholder="请输入同音字，用逗号分隔，如：沙,煞,啥"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="homophoneForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="homophoneDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmitHomophone">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="similarCharDialogVisible"
      :title="similarCharDialogTitle"
      width="600px"
      @close="resetSimilarCharForm"
    >
      <el-form ref="similarCharFormRef" :model="similarCharForm" :rules="similarCharFormRules" label-width="100px">
        <el-form-item label="原始字符" prop="originalChar">
          <el-input v-model="similarCharForm.originalChar" placeholder="请输入原始字符" maxlength="10" />
        </el-form-item>
        <el-form-item label="形似字" prop="similarChars">
          <el-input
            v-model="similarCharForm.similarChars"
            type="textarea"
            :rows="3"
            placeholder="请输入形似字，用逗号分隔，如：艹,屮"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="similarCharForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="similarCharDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmitSimilarChar">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue'
import {
  addHomophone,
  addSimilarChar,
  deleteHomophone,
  deleteSimilarChar,
  listHomophones,
  listSimilarChars,
  refreshHomophoneCache,
  refreshSimilarCharCache,
  updateHomophone,
  updateSimilarChar
} from '@/api/sensitive'
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
import type { CnBreadcrumbItem, CnPagination, CnTableColumn } from '@/design-system'

type ConfigTab = 'homophone' | 'similarChar'

interface HomophoneRecord {
  id: number
  originalChar: string
  homophoneChars: string
  status: number
  createTime?: string
  [key: string]: unknown
}

interface SimilarCharRecord {
  id: number
  originalChar: string
  similarChars: string
  status: number
  createTime?: string
  [key: string]: unknown
}

interface MappingQuery {
  pageNum: number
  pageSize: number
  originalChar: string
  status: number | null
}

interface HomophoneForm {
  id: number | null
  originalChar: string
  homophoneChars: string
  status: number
}

interface SimilarCharForm {
  id: number | null
  originalChar: string
  similarChars: string
  status: number
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '敏感词管理' }, { label: '配置管理' }]

const activeTab = ref<ConfigTab>('homophone')

const homophoneLoading = ref(false)
const homophoneDialogVisible = ref(false)
const homophoneDialogTitle = ref('')
const homophoneData = ref<HomophoneRecord[]>([])
const homophoneTotal = ref(0)
const homophoneFormRef = ref<FormInstance>()

const homophoneQuery = reactive<MappingQuery>({
  pageNum: 1,
  pageSize: 10,
  originalChar: '',
  status: null
})

const homophoneForm = reactive<HomophoneForm>({
  id: null,
  originalChar: '',
  homophoneChars: '',
  status: 1
})

const homophoneFormRules: FormRules<HomophoneForm> = {
  originalChar: [{ required: true, message: '请输入原始字符', trigger: 'blur' }],
  homophoneChars: [{ required: true, message: '请输入同音字', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const similarCharLoading = ref(false)
const similarCharDialogVisible = ref(false)
const similarCharDialogTitle = ref('')
const similarCharData = ref<SimilarCharRecord[]>([])
const similarCharTotal = ref(0)
const similarCharFormRef = ref<FormInstance>()

const similarCharQuery = reactive<MappingQuery>({
  pageNum: 1,
  pageSize: 10,
  originalChar: '',
  status: null
})

const similarCharForm = reactive<SimilarCharForm>({
  id: null,
  originalChar: '',
  similarChars: '',
  status: 1
})

const similarCharFormRules: FormRules<SimilarCharForm> = {
  originalChar: [{ required: true, message: '请输入原始字符', trigger: 'blur' }],
  similarChars: [{ required: true, message: '请输入形似字', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const homophoneColumns: CnTableColumn<HomophoneRecord>[] = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'originalChar', label: '原始字符', width: 130, slot: 'homophoneOriginal', showOverflowTooltip: true },
  { prop: 'homophoneChars', label: '同音字列表', minWidth: 260, slot: 'homophoneChars', showOverflowTooltip: true },
  { prop: 'status', label: '状态', width: 90, slot: 'homophoneStatus' },
  { prop: 'createTime', label: '创建时间', width: 180, showOverflowTooltip: true },
  { label: '操作', width: 130, fixed: 'right', slot: 'homophoneActions' }
]

const similarCharColumns: CnTableColumn<SimilarCharRecord>[] = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'originalChar', label: '原始字符', width: 130, slot: 'similarCharOriginal', showOverflowTooltip: true },
  { prop: 'similarChars', label: '形似字列表', minWidth: 260, slot: 'similarChars', showOverflowTooltip: true },
  { prop: 'status', label: '状态', width: 90, slot: 'similarCharStatus' },
  { prop: 'createTime', label: '创建时间', width: 180, showOverflowTooltip: true },
  { label: '操作', width: 130, fixed: 'right', slot: 'similarCharActions' }
]

const homophonePagination = computed<CnPagination>(() => ({
  page: homophoneQuery.pageNum,
  pageSize: homophoneQuery.pageSize,
  total: homophoneTotal.value,
  pageSizes: [10, 20, 50, 100]
}))

const similarCharPagination = computed<CnPagination>(() => ({
  page: similarCharQuery.pageNum,
  pageSize: similarCharQuery.pageSize,
  total: similarCharTotal.value,
  pageSizes: [10, 20, 50, 100]
}))

const enabledHomophoneCountInPage = computed(() => homophoneData.value.filter((item) => item.status === 1).length)
const enabledSimilarCharCountInPage = computed(() => similarCharData.value.filter((item) => item.status === 1).length)
const enabledMappingCount = computed(() => enabledHomophoneCountInPage.value + enabledSimilarCharCountInPage.value)
const activeLoading = computed(() => (activeTab.value === 'homophone' ? homophoneLoading.value : similarCharLoading.value))
const activeTabLabel = computed(() => (activeTab.value === 'homophone' ? '同音字' : '形似字'))

onMounted(() => {
  loadHomophones()
  loadSimilarChars()
})

watch(activeTab, (newTab) => {
  if (newTab === 'homophone') {
    loadHomophones()
    return
  }
  loadSimilarChars()
})

const loadHomophones = async () => {
  homophoneLoading.value = true
  try {
    const response = await listHomophones({ ...homophoneQuery })
    homophoneData.value = response?.records || []
    homophoneTotal.value = response?.total || 0
  } catch (error) {
    console.error('查询同音字失败:', error)
    ElMessage.error('查询同音字失败')
  } finally {
    homophoneLoading.value = false
  }
}

const loadSimilarChars = async () => {
  similarCharLoading.value = true
  try {
    const response = await listSimilarChars({ ...similarCharQuery })
    similarCharData.value = response?.records || []
    similarCharTotal.value = response?.total || 0
  } catch (error) {
    console.error('查询形似字失败:', error)
    ElMessage.error('查询形似字失败')
  } finally {
    similarCharLoading.value = false
  }
}

const handleHomophonePageChange = (page: number) => {
  homophoneQuery.pageNum = page
  loadHomophones()
}

const handleHomophonePageSizeChange = (size: number) => {
  homophoneQuery.pageSize = size
  homophoneQuery.pageNum = 1
  loadHomophones()
}

const handleSimilarCharPageChange = (page: number) => {
  similarCharQuery.pageNum = page
  loadSimilarChars()
}

const handleSimilarCharPageSizeChange = (size: number) => {
  similarCharQuery.pageSize = size
  similarCharQuery.pageNum = 1
  loadSimilarChars()
}

const handleAddActiveMapping = () => {
  if (activeTab.value === 'homophone') {
    handleAddHomophone()
    return
  }
  handleAddSimilarChar()
}

const handleRefreshActiveCache = () => {
  if (activeTab.value === 'homophone') {
    handleRefreshHomophoneCache()
    return
  }
  handleRefreshSimilarCharCache()
}

const handleAddHomophone = () => {
  homophoneDialogTitle.value = '新增同音字'
  resetHomophoneForm()
  homophoneDialogVisible.value = true
}

const handleEditHomophone = (row: HomophoneRecord) => {
  homophoneDialogTitle.value = '编辑同音字'
  Object.assign(homophoneForm, {
    id: row.id,
    originalChar: row.originalChar || '',
    homophoneChars: row.homophoneChars || '',
    status: Number(row.status) === 0 ? 0 : 1
  })
  homophoneDialogVisible.value = true
}

const handleSubmitHomophone = async () => {
  if (!homophoneFormRef.value) return

  const valid = await homophoneFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (homophoneForm.id) {
      await updateHomophone({ ...homophoneForm })
      ElMessage.success('更新成功')
    } else {
      await addHomophone({ ...homophoneForm })
      ElMessage.success('新增成功')
    }
    homophoneDialogVisible.value = false
    loadHomophones()
  } catch (error) {
    console.error(homophoneForm.id ? '更新同音字失败:' : '新增同音字失败:', error)
    ElMessage.error(homophoneForm.id ? '更新失败' : '新增失败')
  }
}

const handleDeleteHomophone = async (row: HomophoneRecord) => {
  try {
    await ElMessageBox.confirm(`确定要删除同音字映射 "${row.originalChar}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteHomophone(row.id)
    ElMessage.success('删除成功')
    loadHomophones()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除同音字失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleRefreshHomophoneCache = async () => {
  try {
    await refreshHomophoneCache()
    ElMessage.success('同音字缓存刷新成功')
  } catch (error) {
    console.error('刷新同音字缓存失败:', error)
    ElMessage.error('缓存刷新失败')
  }
}

const resetHomophoneForm = () => {
  Object.assign(homophoneForm, {
    id: null,
    originalChar: '',
    homophoneChars: '',
    status: 1
  })
  homophoneFormRef.value?.clearValidate()
}

const handleAddSimilarChar = () => {
  similarCharDialogTitle.value = '新增形似字'
  resetSimilarCharForm()
  similarCharDialogVisible.value = true
}

const handleEditSimilarChar = (row: SimilarCharRecord) => {
  similarCharDialogTitle.value = '编辑形似字'
  Object.assign(similarCharForm, {
    id: row.id,
    originalChar: row.originalChar || '',
    similarChars: row.similarChars || '',
    status: Number(row.status) === 0 ? 0 : 1
  })
  similarCharDialogVisible.value = true
}

const handleSubmitSimilarChar = async () => {
  if (!similarCharFormRef.value) return

  const valid = await similarCharFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (similarCharForm.id) {
      await updateSimilarChar({ ...similarCharForm })
      ElMessage.success('更新成功')
    } else {
      await addSimilarChar({ ...similarCharForm })
      ElMessage.success('新增成功')
    }
    similarCharDialogVisible.value = false
    loadSimilarChars()
  } catch (error) {
    console.error(similarCharForm.id ? '更新形似字失败:' : '新增形似字失败:', error)
    ElMessage.error(similarCharForm.id ? '更新失败' : '新增失败')
  }
}

const handleDeleteSimilarChar = async (row: SimilarCharRecord) => {
  try {
    await ElMessageBox.confirm(`确定要删除形似字映射 "${row.originalChar}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteSimilarChar(row.id)
    ElMessage.success('删除成功')
    loadSimilarChars()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除形似字失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleRefreshSimilarCharCache = async () => {
  try {
    await refreshSimilarCharCache()
    ElMessage.success('形似字缓存刷新成功')
  } catch (error) {
    console.error('刷新形似字缓存失败:', error)
    ElMessage.error('缓存刷新失败')
  }
}

const resetSimilarCharForm = () => {
  Object.assign(similarCharForm, {
    id: null,
    originalChar: '',
    similarChars: '',
    status: 1
  })
  similarCharFormRef.value?.clearValidate()
}
</script>

<style scoped>
.sensitive-config-page {
  min-height: 100%;
}

.sensitive-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.config-tabs {
  min-width: 0;
}

.config-tabs :deep(.el-tabs__header) {
  margin-bottom: var(--cn-space-4);
}

.config-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: var(--cn-color-border-subtle);
}

.config-tabs :deep(.el-tabs__item) {
  color: var(--cn-color-text-secondary);
  font-weight: 650;
}

.config-tabs :deep(.el-tabs__item.is-active) {
  color: var(--cn-color-brand-primary);
}

.mapping-cell {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.mapping-cell strong {
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mapping-cell span {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.mapping-list {
  display: block;
  overflow: hidden;
  color: var(--cn-color-text-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
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
