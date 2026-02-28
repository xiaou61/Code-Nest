<template>
  <div class="sensitive-words-page">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-card shadow="never">
        <el-form :model="queryForm" inline>
          <el-form-item label="敏感词">
            <el-input
              v-model="queryForm.word"
              placeholder="请输入敏感词"
              clearable
              style="width: 200px"
            />
          </el-form-item>
          <el-form-item label="分类">
            <el-select
              v-model="queryForm.categoryId"
              placeholder="请选择分类"
              clearable
              style="width: 150px"
            >
              <el-option
                v-for="category in categories"
                :key="category.id"
                :label="category.name"
                :value="category.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="风险等级">
            <el-select
              v-model="queryForm.level"
              placeholder="请选择风险等级"
              clearable
              style="width: 130px"
            >
              <el-option label="低风险" :value="1" />
              <el-option label="中风险" :value="2" />
              <el-option label="高风险" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select
              v-model="queryForm.status"
              placeholder="请选择状态"
              clearable
              style="width: 120px"
            >
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleQuery">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
            <el-button @click="resetQuery">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- 操作栏 -->
    <div class="toolbar">
      <el-card shadow="never">
        <el-row :gutter="10">
          <el-col :span="1.5">
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              新增
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="danger"
              :disabled="selectedRows.length === 0"
              @click="handleBatchDelete"
            >
              <el-icon><Delete /></el-icon>
              批量删除
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="info" @click="handleImport">
              <el-icon><Upload /></el-icon>
              导入
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="success"
              :loading="exporting"
              @click="handleExport"
            >
              <el-icon><Download /></el-icon>
              导出
            </el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="warning" @click="handleRefresh">
              <el-icon><Refresh /></el-icon>
              刷新词库
            </el-button>
          </el-col>
        </el-row>
      </el-card>
    </div>

    <!-- 数据表格 -->
    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="tableData"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="word" label="敏感词" min-width="120" />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag
              :type="getLevelTagType(row.level)"
              size="small"
            >
              {{ getLevelText(row.level) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理动作" width="100">
          <template #default="{ row }">
            <el-tag
              :type="getActionTagType(row.action)"
              size="small"
            >
              {{ getActionText(row.action) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 1 ? 'success' : 'danger'"
              size="small"
            >
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="creatorName" label="创建人" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="queryForm.pageNum"
          v-model:page-size="queryForm.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleQuery"
          @current-change="handleQuery"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="敏感词" prop="word">
          <el-input
            v-model="form.word"
            placeholder="请输入敏感词"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select
            v-model="form.categoryId"
            placeholder="请选择分类"
            style="width: 100%"
          >
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="风险等级" prop="level">
          <el-select
            v-model="form.level"
            placeholder="请选择风险等级"
            style="width: 100%"
          >
            <el-option label="低风险" :value="1" />
            <el-option label="中风险" :value="2" />
            <el-option label="高风险" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理动作" prop="action">
          <el-select
            v-model="form.action"
            placeholder="请选择处理动作"
            style="width: 100%"
          >
            <el-option label="替换" :value="1" />
            <el-option label="拒绝" :value="2" />
            <el-option label="审核" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 导入对话框 -->
    <el-dialog
      v-model="importDialogVisible"
      title="批量导入敏感词（预览确认）"
      width="620px"
      @close="resetImportState"
    >
      <div class="import-tips">
        <el-alert
          title="导入说明"
          type="info"
          :closable="false"
        >
          <div>1. 支持 TXT 文件格式</div>
          <div>2. 每行一个敏感词，或用逗号分隔</div>
          <div>3. 重复的敏感词将自动跳过</div>
        </el-alert>
      </div>
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :on-change="handleFileChange"
        :file-list="fileList"
        accept=".txt"
        drag
      >
        <el-icon class="el-icon--upload"><Upload /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
      </el-upload>
      <div v-if="previewResult" class="preview-result">
        <el-descriptions :column="3" border size="small" title="导入预览">
          <el-descriptions-item label="总词条">{{ previewResult.total || 0 }}</el-descriptions-item>
          <el-descriptions-item label="可导入">{{ previewResult.importableCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="无效词条">{{ previewResult.invalidCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="文件内重复">{{ previewResult.duplicateInFileCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="库内已存在">{{ previewResult.duplicateInDbCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="有效去重后">{{ previewResult.validCount || 0 }}</el-descriptions-item>
        </el-descriptions>
        <div class="preview-samples" v-if="previewResult.importableSamples?.length">
          <div class="preview-samples-title">可导入示例</div>
          <el-tag
            v-for="item in previewResult.importableSamples"
            :key="'importable-' + item"
            type="success"
            size="small"
            class="sample-tag"
          >
            {{ item }}
          </el-tag>
        </div>
      </div>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button
          type="info"
          :loading="previewingImport"
          @click="handlePreviewImport"
        >
          预览
        </el-button>
        <el-button
          type="primary"
          :loading="importing"
          :disabled="!previewResult || (previewResult.importableCount || 0) <= 0"
          @click="handleImportSubmit"
        >
          确认导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Refresh,
  Plus,
  Delete,
  Upload,
  Download
} from '@element-plus/icons-vue'
import {
  listSensitiveWords,
  addSensitiveWord,
  updateSensitiveWord,
  deleteSensitiveWord,
  deleteSensitiveWords,
  previewSensitiveWordsImport,
  confirmSensitiveWordsImport,
  exportSensitiveWords,
  refreshWordLibrary,
  listSensitiveCategories
} from '@/api/sensitive'

// 响应式数据
const loading = ref(false)
const dialogVisible = ref(false)
const importDialogVisible = ref(false)
const importing = ref(false)
const previewingImport = ref(false)
const exporting = ref(false)
const dialogTitle = ref('')
const tableData = ref([])
const categories = ref([])
const selectedRows = ref([])
const total = ref(0)
const fileList = ref([])
const previewResult = ref(null)
const formRef = ref()
const uploadRef = ref()

// 查询表单
const queryForm = reactive({
  pageNum: 1,
  pageSize: 10,
  word: '',
  categoryId: null,
  level: null,
  status: null
})

// 编辑表单
const form = reactive({
  id: null,
  word: '',
  categoryId: null,
  level: 1,
  action: 1,
  status: 1
})

// 表单验证规则
const formRules = {
  word: [
    { required: true, message: '请输入敏感词', trigger: 'blur' },
    { min: 1, max: 50, message: '长度在 1 到 50 个字符', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  level: [
    { required: true, message: '请选择风险等级', trigger: 'change' }
  ],
  action: [
    { required: true, message: '请选择处理动作', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

// 方法
const handleQuery = async () => {
  loading.value = true
  try {
    const response = await listSensitiveWords(queryForm)
    tableData.value = response.records
    total.value = response.total
  } catch (error) {
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryForm.pageNum = 1
  queryForm.pageSize = 10
  queryForm.word = ''
  queryForm.categoryId = null
  queryForm.level = null
  queryForm.status = null
  handleQuery()
}

const handleAdd = () => {
  dialogTitle.value = '新增敏感词'
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑敏感词'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate()
  if (!valid) return

  try {
    if (form.id) {
      await updateSensitiveWord(form)
      ElMessage.success('更新成功')
    } else {
      await addSensitiveWord(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    handleQuery()
  } catch (error) {
    ElMessage.error(form.id ? '更新失败' : '新增失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这个敏感词吗？', '提示', {
      type: 'warning'
    })
    await deleteSensitiveWord(row.id)
    ElMessage.success('删除成功')
    handleQuery()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm('确定要删除选中的敏感词吗？', '提示', {
      type: 'warning'
    })
    const ids = selectedRows.value.map(row => row.id)
    await deleteSensitiveWords(ids)
    ElMessage.success('删除成功')
    handleQuery()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleImport = () => {
  resetImportState()
  importDialogVisible.value = true
}

const handleFileChange = (file, files) => {
  fileList.value = files
  previewResult.value = null
}

const handlePreviewImport = async () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择要导入的文件')
    return
  }

  previewingImport.value = true
  try {
    const file = fileList.value[0].raw
    const result = await previewSensitiveWordsImport(file)
    previewResult.value = result
    ElMessage.success('预览完成，请确认后导入')
  } catch (error) {
    ElMessage.error('预览失败')
  } finally {
    previewingImport.value = false
  }
}

const handleImportSubmit = async () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择要导入的文件')
    return
  }

  importing.value = true
  try {
    const file = fileList.value[0].raw
    const result = await confirmSensitiveWordsImport(file)
    ElMessage.success(
      `导入完成：成功 ${result.success} 个，重复 ${result.duplicate} 个，失败 ${result.errors.length} 个`
    )
    importDialogVisible.value = false
    resetImportState()
    handleQuery()
  } catch (error) {
    ElMessage.error('导入失败')
  } finally {
    importing.value = false
  }
}

const handleExport = async () => {
  exporting.value = true
  try {
    const result = await exportSensitiveWords({
      word: queryForm.word,
      categoryId: queryForm.categoryId,
      level: queryForm.level,
      status: queryForm.status,
      startTime: queryForm.startTime,
      endTime: queryForm.endTime
    })

    const blob = new Blob([result.content || ''], { type: 'text/csv;charset=utf-8;' })
    const fileName = result.fileName || `sensitive_words_${Date.now()}.csv`

    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success(`导出成功，共 ${result.total || 0} 条`)
  } catch (error) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const handleRefresh = async () => {
  try {
    await refreshWordLibrary()
    ElMessage.success('敏感词库刷新成功')
  } catch (error) {
    ElMessage.error('敏感词库刷新失败')
  }
}

const resetImportState = () => {
  fileList.value = []
  previewResult.value = null
  importing.value = false
  previewingImport.value = false
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}

const resetForm = () => {
  form.id = null
  form.word = ''
  form.categoryId = null
  form.level = 1
  form.action = 1
  form.status = 1
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

const loadCategories = async () => {
  try {
    const result = await listSensitiveCategories()
    categories.value = result
  } catch (error) {
    ElMessage.error('加载分类失败')
  }
}

// 工具方法
const getLevelText = (level) => {
  const levelMap = { 1: '低风险', 2: '中风险', 3: '高风险' }
  return levelMap[level] || '未知'
}

const getLevelTagType = (level) => {
  const typeMap = { 1: 'success', 2: 'warning', 3: 'danger' }
  return typeMap[level] || 'info'
}

const getActionText = (action) => {
  const actionMap = { 1: '替换', 2: '拒绝', 3: '审核' }
  return actionMap[action] || '未知'
}

const getActionTagType = (action) => {
  const typeMap = { 1: 'info', 2: 'danger', 3: 'warning' }
  return typeMap[action] || 'info'
}

// 生命周期
onMounted(() => {
  loadCategories()
  handleQuery()
})
</script>

<style scoped>
.sensitive-words-page {
  padding: 20px;
}

.search-bar {
  margin-bottom: 16px;
}

.toolbar {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 20px;
  text-align: center;
}

.import-tips {
  margin-bottom: 20px;
}

.preview-result {
  margin-top: 16px;
}

.preview-samples {
  margin-top: 12px;
}

.preview-samples-title {
  margin-bottom: 8px;
  font-size: 13px;
  color: #606266;
}

.sample-tag {
  margin-right: 6px;
  margin-bottom: 6px;
}
</style> 
