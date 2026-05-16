<template>
  <div class="bug-store-management">
    <!-- 页面头部 -->
    <el-card class="header-card" shadow="never">
      <div class="header-content">
        <div class="title-section">
          <h2>Bug商店管理</h2>
          <p>管理Bug商店中的经典Bug案例，包括添加、编辑、删除Bug条目等操作</p>
        </div>
        <div class="action-section">
          <el-button 
            type="danger" 
            :disabled="selectedBugs.length === 0"
            @click="handleBatchDelete"
          >
            <el-icon><Delete /></el-icon>
            批量删除 ({{ selectedBugs.length }})
          </el-button>
          <el-button type="success" @click="handleBatchImport">
            <el-icon><Upload /></el-icon>
            批量导入
          </el-button>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增Bug
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 统计信息 -->
    <el-card class="stats-card" shadow="never" v-if="statistics">
      <el-row :gutter="16">
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-number primary">{{ statistics.totalBugs || 0 }}</div>
            <div class="stat-label">总Bug数</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-number success">{{ statistics.easyBugs || 0 }}</div>
            <div class="stat-label">初级Bug</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-number warning">{{ statistics.mediumBugs || 0 }}</div>
            <div class="stat-label">中级Bug</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-number danger">{{ statistics.hardBugs || 0 }}</div>
            <div class="stat-label">高级Bug</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 搜索和操作区域 -->
    <el-card class="search-card" shadow="never">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-input 
            v-model="searchForm.title" 
            placeholder="请输入Bug标题" 
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="4">
          <el-select v-model="searchForm.difficultyLevel" placeholder="难度等级" clearable @change="handleSearch">
            <el-option label="初级" :value="1" />
            <el-option label="中级" :value="2" />
            <el-option label="高级" :value="3" />
            <el-option label="专家级" :value="4" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="searchForm.status" placeholder="状态" clearable @change="handleSearch">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-input 
            v-model="searchForm.techTag" 
            placeholder="技术标签" 
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- Bug列表 -->
    <el-card class="table-card" shadow="never">
      <el-table 
        :data="bugList" 
        v-loading="tableLoading"
        @selection-change="handleSelectionChange"
        row-key="id"
        stripe
        border
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="Bug标题" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="bug-title-cell">
              <span class="title-text">{{ row.title }}</span>
              <el-tag 
                :type="getDifficultyTagType(row.difficultyLevel)" 
                size="small"
                class="difficulty-tag"
              >
                {{ getDifficultyName(row.difficultyLevel) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phenomenon" label="现象描述" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="phenomenon-text">{{ truncateText(row.phenomenon, 60) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="techTags" label="技术标签" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="tech-tags-cell">
              <el-tag 
                v-for="tag in getTechTagsArray(row.techTags)" 
                :key="tag"
                size="small"
                type="info"
                class="tech-tag"
              >
                {{ tag }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch 
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleView(row)">
              查看
            </el-button>
            <el-button type="warning" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- Bug详情对话框 -->
    <el-dialog 
      v-model="viewDialogVisible" 
      title="Bug详情" 
      width="800px"
      :close-on-click-modal="false"
    >
      <div class="bug-detail" v-if="currentBug">
        <!-- Bug基本信息 -->
        <div class="detail-section">
          <h3>基本信息</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="Bug标题">
              {{ currentBug.title }}
            </el-descriptions-item>
            <el-descriptions-item label="难度等级">
              <el-tag :type="getDifficultyTagType(currentBug.difficultyLevel)">
                {{ getDifficultyName(currentBug.difficultyLevel) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="技术标签">
              <div class="tech-tags">
                <el-tag 
                  v-for="tag in getTechTagsArray(currentBug.techTags)" 
                  :key="tag"
                  size="small"
                  type="info"
                >
                  {{ tag }}
                </el-tag>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="currentBug.status === 1 ? 'success' : 'danger'">
                {{ currentBug.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              {{ formatDateTime(currentBug.createTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="更新时间">
              {{ formatDateTime(currentBug.updateTime) }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- Bug详细内容 -->
        <div class="detail-section">
          <h3>现象描述</h3>
          <div class="content-box phenomenon">
            {{ currentBug.phenomenon }}
          </div>
        </div>

        <div class="detail-section">
          <h3>原因分析</h3>
          <div class="content-box analysis">
            <pre>{{ currentBug.causeAnalysis }}</pre>
          </div>
        </div>

        <div class="detail-section">
          <h3>解决方案</h3>
          <div class="content-box solution">
            <pre>{{ currentBug.solution }}</pre>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 新增/编辑Bug对话框 -->
    <el-dialog 
      v-model="formDialogVisible" 
      :title="isEditing ? '编辑Bug' : '新增Bug'"
      width="900px"
      :close-on-click-modal="false"
    >
      <el-form :model="bugForm" :rules="bugRules" ref="bugFormRef" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="Bug标题" prop="title">
              <el-input 
                v-model="bugForm.title" 
                placeholder="请输入Bug标题"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="难度等级" prop="difficultyLevel">
              <el-select v-model="bugForm.difficultyLevel" placeholder="选择难度等级">
                <el-option label="初级" :value="1" />
                <el-option label="中级" :value="2" />
                <el-option label="高级" :value="3" />
                <el-option label="专家级" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="bugForm.status">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="技术标签" prop="techTags">
              <el-input 
                v-model="bugForm.techTags" 
                placeholder="请输入技术标签，多个标签用逗号分隔"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="现象描述" prop="phenomenon">
              <el-input 
                v-model="bugForm.phenomenon" 
                type="textarea"
                :rows="4"
                placeholder="请描述Bug的现象表现"
                maxlength="1000"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="原因分析" prop="causeAnalysis">
              <el-input 
                v-model="bugForm.causeAnalysis" 
                type="textarea"
                :rows="5"
                placeholder="请分析Bug产生的原因"
                maxlength="2000"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="解决方案" prop="solution">
              <el-input 
                v-model="bugForm.solution" 
                type="textarea"
                :rows="6"
                placeholder="请提供Bug的解决方案"
                maxlength="2000"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序值" prop="sortOrder">
              <el-input-number 
                v-model="bugForm.sortOrder" 
                :min="0" 
                :max="9999"
                placeholder="数值越大越靠前"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="formDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
            {{ isEditing ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 批量导入对话框 -->
    <el-dialog v-model="importDialogVisible" title="批量导入Bug" width="600px">
      <div class="import-content">
        <el-alert
          title="导入说明"
          type="info"
          show-icon
          :closable="false"
        >
          <p>1. 请使用标准的Excel格式文件</p>
          <p>2. 表格列顺序：Bug标题、现象描述、原因分析、解决方案、技术标签、难度等级</p>
          <p>3. 难度等级：1-初级，2-中级，3-高级，4-专家级</p>
        </el-alert>
        
        <div class="upload-section">
          <el-upload
            ref="uploadRef"
            class="upload-demo"
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls"
            :on-change="handleFileChange"
          >
            <el-button type="primary">
              <el-icon><Upload /></el-icon>
              选择文件
            </el-button>
            <template #tip>
              <div class="el-upload__tip">
                只能上传 xlsx/xls 文件
              </div>
            </template>
          </el-upload>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="importDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleImportSubmit" :loading="importLoading">
            导入
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Search, 
  Plus, 
  Delete, 
  Upload,
} from '@element-plus/icons-vue'
import { moyuApi } from '@/api/moyu'

// 响应式数据
const tableLoading = ref(false)
const submitLoading = ref(false)
const importLoading = ref(false)
const bugList = ref([])
const selectedBugs = ref([])
const statistics = ref({})

// 分页信息
const pagination = reactive({
  current: 1,
  size: 20,
  total: 0
})

// 搜索表单
const searchForm = reactive({
  title: '',
  difficultyLevel: null,
  status: null,
  techTag: ''
})

// 对话框控制
const viewDialogVisible = ref(false)
const formDialogVisible = ref(false)
const importDialogVisible = ref(false)
const isEditing = ref(false)
const currentBug = ref(null)

// Bug表单
const bugForm = reactive({
  id: null,
  title: '',
  phenomenon: '',
  causeAnalysis: '',
  solution: '',
  techTags: '',
  difficultyLevel: 1,
  status: 1,
  sortOrder: 0
})

// 表单验证规则
const bugRules = {
  title: [
    { required: true, message: '请输入Bug标题', trigger: 'blur' }
  ],
  phenomenon: [
    { required: true, message: '请输入现象描述', trigger: 'blur' }
  ],
  causeAnalysis: [
    { required: true, message: '请输入原因分析', trigger: 'blur' }
  ],
  solution: [
    { required: true, message: '请输入解决方案', trigger: 'blur' }
  ],
  difficultyLevel: [
    { required: true, message: '请选择难度等级', trigger: 'change' }
  ]
}

const bugFormRef = ref(null)
const uploadRef = ref(null)

// 初始化
onMounted(() => {
  loadBugList()
  loadStatistics()
})

// 加载Bug列表
const loadBugList = async () => {
  try {
    tableLoading.value = true
    const params = {
      current: pagination.current,
      size: pagination.size,
      ...searchForm
    }
    
    const response = await moyuApi.bugStore.getBugList(params)
    
    if (response && response.records) {
      bugList.value = response.records
      pagination.total = response.total
    }
  } catch (error) {
    console.error('获取Bug列表失败:', error)
    ElMessage.error('获取Bug列表失败')
  } finally {
    tableLoading.value = false
  }
}

// 加载统计信息
const loadStatistics = async () => {
  try {
    // 这里可以根据需要调用统计接口
    statistics.value = {
      totalBugs: 0,
      easyBugs: 0,
      mediumBugs: 0,
      hardBugs: 0
    }
  } catch (error) {
    console.error('获取统计信息失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  loadBugList()
}

// 重置搜索
const handleReset = () => {
  Object.assign(searchForm, {
    title: '',
    difficultyLevel: null,
    status: null,
    techTag: ''
  })
  handleSearch()
}

// 分页处理
const handleSizeChange = (val) => {
  pagination.size = val
  loadBugList()
}

const handleCurrentChange = (val) => {
  pagination.current = val
  loadBugList()
}

// 选择处理
const handleSelectionChange = (val) => {
  selectedBugs.value = val
}

// 新增Bug
const handleAdd = () => {
  resetBugForm()
  isEditing.value = false
  formDialogVisible.value = true
}

// 编辑Bug
const handleEdit = (row) => {
  Object.assign(bugForm, { ...row })
  isEditing.value = true
  formDialogVisible.value = true
}

// 查看Bug详情
const handleView = (row) => {
  currentBug.value = row
  viewDialogVisible.value = true
}

// 删除Bug
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除Bug"${row.title}"吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await moyuApi.bugStore.deleteBug(row.id)
    ElMessage.success('删除成功')
    loadBugList()
  } catch (error) {
    if (error === 'cancel') return
    console.error('删除Bug失败:', error)
    ElMessage.error('删除Bug失败')
  }
}

// 批量删除
const handleBatchDelete = async () => {
  if (selectedBugs.value.length === 0) {
    ElMessage.warning('请选择要删除的Bug')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的${selectedBugs.value.length}个Bug吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 这里需要添加批量删除的API
    for (const bug of selectedBugs.value) {
      await moyuApi.bugStore.deleteBug(bug.id)
    }
    
    ElMessage.success('批量删除成功')
    loadBugList()
  } catch (error) {
    if (error === 'cancel') return
    console.error('批量删除失败:', error)
    ElMessage.error('批量删除失败')
  }
}

// 状态切换
const handleStatusChange = async (row) => {
  try {
    await moyuApi.bugStore.updateBug(row.id, { 
      ...row,
      status: row.status 
    })
    ElMessage.success('状态更新成功')
  } catch (error) {
    console.error('更新状态失败:', error)
    ElMessage.error('更新状态失败')
    // 恢复原状态
    row.status = row.status === 1 ? 0 : 1
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!bugFormRef.value) return
  
  try {
    await bugFormRef.value.validate()
    submitLoading.value = true
    
    if (isEditing.value) {
      await moyuApi.bugStore.updateBug(bugForm.id, bugForm)
      ElMessage.success('更新成功')
    } else {
      await moyuApi.bugStore.addBug(bugForm)
      ElMessage.success('创建成功')
    }
    
    formDialogVisible.value = false
    loadBugList()
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error(isEditing.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

// 批量导入
const handleBatchImport = () => {
  importDialogVisible.value = true
}

// 文件选择处理
const handleFileChange = (file) => {
  console.log('Selected file:', file)
}

// 导入提交
const handleImportSubmit = async () => {
  try {
    importLoading.value = true
    // 这里处理文件上传和导入逻辑
    ElMessage.success('导入成功')
    importDialogVisible.value = false
    loadBugList()
  } catch (error) {
    console.error('导入失败:', error)
    ElMessage.error('导入失败')
  } finally {
    importLoading.value = false
  }
}

// 重置Bug表单
const resetBugForm = () => {
  Object.assign(bugForm, {
    id: null,
    title: '',
    phenomenon: '',
    causeAnalysis: '',
    solution: '',
    techTags: '',
    difficultyLevel: 1,
    status: 1,
    sortOrder: 0
  })
}

// 工具函数
const getDifficultyName = (level) => {
  const names = { 1: '初级', 2: '中级', 3: '高级', 4: '专家级' }
  return names[level] || '未知'
}

const getDifficultyTagType = (level) => {
  const types = { 1: 'success', 2: 'warning', 3: 'danger', 4: '' }
  return types[level] || ''
}

const getTechTagsArray = (techTags) => {
  if (!techTags) return []
  return techTags.split(',').map(tag => tag.trim()).filter(tag => tag)
}

const truncateText = (text, maxLength) => {
  if (!text) return ''
  return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  return new Date(dateTime).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}
</script>

<style scoped>
.bug-store-management {
  padding: 20px;
}

.header-card {
  margin-bottom: 16px;
  border: none;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-section h2 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 20px;
}

.title-section p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.action-section {
  display: flex;
  gap: 12px;
}

.stats-card {
  margin-bottom: 16px;
  border: none;
}

.stat-item {
  text-align: center;
  padding: 16px;
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 8px;
}

.stat-number.primary { color: #409eff; }
.stat-number.success { color: #67c23a; }
.stat-number.warning { color: #e6a23c; }
.stat-number.info { color: #909399; }
.stat-number.danger { color: #f56c6c; }
.stat-number.popular { color: #f39c12; }

.stat-label {
  color: #909399;
  font-size: 14px;
}

.search-card, .table-card {
  border: none;
}

.search-card {
  margin-bottom: 16px;
}

.bug-title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.difficulty-tag {
  flex-shrink: 0;
}

.tech-tags-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.tech-tag {
  margin: 0;
}

.phenomenon-text {
  color: #606266;
  font-size: 13px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.bug-detail {
  max-height: 600px;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section h3 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 16px;
  border-left: 4px solid #409eff;
  padding-left: 12px;
}

.content-box {
  padding: 16px;
  border-radius: 8px;
  line-height: 1.6;
  word-wrap: break-word;
}

.content-box.phenomenon {
  background: #fef0f0;
  border: 1px solid #fde2e2;
  color: #f56c6c;
}

.content-box.analysis {
  background: #f0f9ff;
  border: 1px solid #e1f5fe;
  color: #409eff;
}

.content-box.solution {
  background: #f0f9f0;
  border: 1px solid #e2f7e2;
  color: #67c23a;
}

.content-box pre {
  margin: 0;
  font-family: inherit;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.tech-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.dialog-footer {
  text-align: right;
}

.import-content {
  margin-bottom: 20px;
}

.upload-section {
  margin-top: 20px;
}

.upload-demo {
  width: 100%;
}

.el-upload__tip {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
