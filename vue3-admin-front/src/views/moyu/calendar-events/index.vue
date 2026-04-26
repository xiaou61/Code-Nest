<template>
  <div class="calendar-events-management">
    <!-- 页面头部 -->
    <el-card class="header-card" shadow="never">
      <div class="header-content">
        <div class="title-section">
          <h2>日历事件管理</h2>
          <p>管理程序员日历中的节日事件，包括程序员节日、技术纪念日、开源节日等</p>
        </div>
        <div class="action-section">
          <el-button 
            type="danger" 
            :disabled="selectedEvents.length === 0"
            @click="handleBatchDelete"
          >
            <el-icon><Delete /></el-icon>
            批量删除 ({{ selectedEvents.length }})
          </el-button>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增事件
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 统计信息 -->
    <el-card class="stats-card" shadow="never" v-if="statistics">
      <el-row :gutter="16">
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-number primary">{{ statistics.totalEvents || 0 }}</div>
            <div class="stat-label">总事件数</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-number success">{{ statistics.majorEvents || 0 }}</div>
            <div class="stat-label">重要事件</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="stat-item">
            <div class="stat-number warning">{{ statistics.programmerHolidays || 0 }}</div>
            <div class="stat-label">程序员节日</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="stat-item">
            <div class="stat-number info">{{ statistics.techMemorials || 0 }}</div>
            <div class="stat-label">技术纪念日</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="stat-item">
            <div class="stat-number danger">{{ statistics.openSourceHolidays || 0 }}</div>
            <div class="stat-label">开源节日</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 搜索和操作区域 -->
    <el-card class="search-card" shadow="never">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-input 
            v-model="searchForm.eventName" 
            placeholder="请输入事件名称" 
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
          <el-select v-model="searchForm.eventType" placeholder="事件类型" clearable @change="handleSearch">
            <el-option label="程序员节日" :value="1" />
            <el-option label="技术纪念日" :value="2" />
            <el-option label="开源节日" :value="3" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="searchForm.isMajor" placeholder="重要程度" clearable @change="handleSearch">
            <el-option label="重要事件" :value="1" />
            <el-option label="普通事件" :value="0" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="searchForm.status" placeholder="状态" clearable @change="handleSearch">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 事件表格 -->
    <el-card class="table-card" shadow="never">
      <el-table 
        :data="eventList" 
        v-loading="loading"
        @selection-change="handleSelectionChange"
        stripe
        border
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="eventName" label="事件名称" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="event-name">
              <el-icon :style="{ color: row.color }">
                <component :is="getIconComponent(row.icon)" />
              </el-icon>
              <span style="margin-left: 8px;">{{ row.eventName }}</span>
              <el-tag v-if="row.isMajor" type="danger" size="small" style="margin-left: 8px;">重要</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="eventDate" label="日期" width="100" align="center" />
        <el-table-column prop="eventType" label="事件类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getEventTypeTag(row.eventType)">
              {{ getEventTypeName(row.eventType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="blessingText" label="祝福语" width="150" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="800px"
      :before-close="handleCloseDialog"
      destroy-on-close
    >
      <el-form
        ref="eventFormRef"
        :model="eventForm"
        :rules="eventRules"
        label-width="120px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="事件名称" prop="eventName">
              <el-input v-model="eventForm.eventName" placeholder="请输入事件名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="事件日期" prop="eventDate">
              <el-input v-model="eventForm.eventDate" placeholder="MM-dd格式，如：10-24" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="事件类型" prop="eventType">
              <el-select v-model="eventForm.eventType" placeholder="请选择事件类型">
                <el-option label="程序员节日" :value="1" />
                <el-option label="技术纪念日" :value="2" />
                <el-option label="开源节日" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="重要程度">
              <el-switch
                v-model="eventForm.isMajor"
                :active-value="1"
                :inactive-value="0"
                active-text="重要事件"
                inactive-text="普通事件"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="图标" prop="icon">
              <el-input v-model="eventForm.icon" placeholder="请输入图标名称，如：code" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="颜色" prop="color">
              <el-color-picker v-model="eventForm.color" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="事件描述" prop="description">
          <el-input
            v-model="eventForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入事件描述"
          />
        </el-form-item>

        <el-form-item label="祝福语">
          <el-input
            v-model="eventForm.blessingText"
            type="textarea"
            :rows="2"
            placeholder="请输入节日祝福语（可选）"
          />
        </el-form-item>

        <el-form-item label="相关链接">
          <el-input
            v-model="eventForm.relatedLinksText"
            type="textarea"
            :rows="2"
            placeholder="每行一个链接（可选）"
          />
        </el-form-item>

        <el-form-item label="排序值">
          <el-input-number v-model="eventForm.sortOrder" :min="0" :max="999" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          {{ isEdit ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Edit, Search, Refresh } from '@element-plus/icons-vue'
import { developerCalendarApi } from '@/api/moyu'

// 响应式数据
const loading = ref(false)
const eventList = ref([])
const selectedEvents = ref([])
const statistics = ref(null)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const eventFormRef = ref()

// 搜索表单
const searchForm = reactive({
  eventName: '',
  eventType: null,
  isMajor: null,
  status: null
})

// 分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

// 事件表单
const eventForm = reactive({
  id: null,
  eventName: '',
  eventDate: '',
  eventType: null,
  description: '',
  icon: '',
  color: '#1890ff',
  isMajor: 0,
  blessingText: '',
  relatedLinksText: '',
  sortOrder: 0,
  status: 1
})

// 表单验证规则
const eventRules = {
  eventName: [
    { required: true, message: '请输入事件名称', trigger: 'blur' }
  ],
  eventDate: [
    { required: true, message: '请输入事件日期', trigger: 'blur' },
    { pattern: /^\d{2}-\d{2}$/, message: '日期格式应为MM-dd', trigger: 'blur' }
  ],
  eventType: [
    { required: true, message: '请选择事件类型', trigger: 'change' }
  ],
  description: [
    { required: true, message: '请输入事件描述', trigger: 'blur' }
  ]
}

// 计算属性
const dialogTitle = computed(() => isEdit.value ? '编辑事件' : '新增事件')

// 获取事件类型名称
const getEventTypeName = (type) => {
  const typeMap = {
    1: '程序员节日',
    2: '技术纪念日',
    3: '开源节日'
  }
  return typeMap[type] || '未知'
}

// 获取事件类型标签
const getEventTypeTag = (type) => {
  const tagMap = {
    1: 'warning',
    2: 'info', 
    3: 'danger'
  }
  return tagMap[type] || 'info'
}

// 获取图标组件
const getIconComponent = (iconName) => {
  const iconMap = {
    'code': 'EditPen',
    'calendar': 'Calendar',
    'coffee': 'Mug',
    'github': 'Platform'
  }
  return iconMap[iconName] || 'Calendar'
}

// 加载事件列表
const loadEventList = async () => {
  try {
    loading.value = true
    const data = await developerCalendarApi.getEventList(searchForm)
    eventList.value = data || []
    pagination.total = eventList.value.length
  } catch (error) {
    console.error('加载事件列表失败:', error)
    ElMessage.error('加载事件列表失败')
  } finally {
    loading.value = false
  }
}

// 加载统计信息
const loadStatistics = async () => {
  try {
    const data = await developerCalendarApi.getEventStatistics()
    statistics.value = data
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  pagination.currentPage = 1
  loadEventList()
}

// 重置搜索
const handleReset = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = key === 'status' ? null : ''
  })
  handleSearch()
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedEvents.value = selection
}

// 状态变更
const handleStatusChange = async (row) => {
  try {
    await developerCalendarApi.updateEventStatus(row.id, row.status)
    ElMessage.success('状态更新成功')
    loadStatistics()
  } catch (error) {
    console.error('状态更新失败:', error)
    ElMessage.error('状态更新失败')
    // 恢复原状态
    row.status = row.status === 1 ? 0 : 1
  }
}

// 新增事件
const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 编辑事件
const handleEdit = async (row) => {
  try {
    isEdit.value = true
    const eventData = await developerCalendarApi.getEventById(row.id)
    
    Object.keys(eventForm).forEach(key => {
      if (key === 'relatedLinksText') {
        eventForm[key] = eventData.relatedLinks ? eventData.relatedLinks.join('\n') : ''
      } else {
        eventForm[key] = eventData[key] || (key === 'sortOrder' ? 0 : '')
      }
    })
    
    dialogVisible.value = true
  } catch (error) {
    console.error('加载事件详情失败:', error)
    ElMessage.error('加载事件详情失败')
  }
}

// 删除事件
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除事件 "${row.eventName}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    await developerCalendarApi.deleteEvent(row.id)
    ElMessage.success('删除成功')
    loadEventList()
    loadStatistics()
  } catch (error) {
    if (error === 'cancel') return
    console.error('删除失败:', error)
    ElMessage.error('删除失败')
  }
}

// 批量删除
const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedEvents.value.length} 个事件吗？`,
      '确认批量删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    const ids = selectedEvents.value.map(item => item.id)
    await developerCalendarApi.batchDeleteEvents(ids)
    ElMessage.success('批量删除成功')
    selectedEvents.value = []
    loadEventList()
    loadStatistics()
  } catch (error) {
    if (error === 'cancel') return
    console.error('批量删除失败:', error)
    ElMessage.error('批量删除失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!eventFormRef.value) return
  
  try {
    await eventFormRef.value.validate()
    submitLoading.value = true
    
    const formData = { ...eventForm }
    // 处理相关链接
    if (formData.relatedLinksText) {
      formData.relatedLinks = formData.relatedLinksText.split('\n').filter(link => link.trim())
    }
    delete formData.relatedLinksText
    
    if (isEdit.value) {
      await developerCalendarApi.updateEvent(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await developerCalendarApi.createEvent(formData)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    loadEventList()
    loadStatistics()
  } catch (error) {
    if (error.name !== 'ValidationError') {
      console.error('提交失败:', error)
      ElMessage.error('提交失败')
    }
  } finally {
    submitLoading.value = false
  }
}

// 重置表单
const resetForm = () => {
  Object.keys(eventForm).forEach(key => {
    if (key === 'color') {
      eventForm[key] = '#1890ff'
    } else if (key === 'isMajor') {
      eventForm[key] = 0
    } else if (key === 'status') {
      eventForm[key] = 1
    } else if (key === 'sortOrder') {
      eventForm[key] = 0
    } else {
      eventForm[key] = null
    }
  })
}

// 关闭对话框
const handleCloseDialog = (done) => {
  resetForm()
  done()
}

// 分页处理
const handleSizeChange = (size) => {
  pagination.pageSize = size
  loadEventList()
}

const handleCurrentChange = (page) => {
  pagination.currentPage = page
  loadEventList()
}

// 初始化
onMounted(() => {
  loadEventList()
  loadStatistics()
})
</script>

<style scoped>
.calendar-events-management {
  padding: 0;
}

.header-card {
  margin-bottom: 16px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.title-section h2 {
  margin: 0 0 8px 0;
  color: #303133;
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
}

.stat-item {
  text-align: center;
  padding: 20px;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 8px;
}

.stat-number.primary { color: #409eff; }
.stat-number.success { color: #67c23a; }
.stat-number.warning { color: #e6a23c; }
.stat-number.info { color: #909399; }
.stat-number.danger { color: #f56c6c; }

.stat-label {
  color: #909399;
  font-size: 14px;
}

.search-card {
  margin-bottom: 16px;
}

.table-card {
  margin-bottom: 16px;
}

.event-name {
  display: flex;
  align-items: center;
}

.pagination-container {
  display: flex;
  justify-content: right;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    gap: 16px;
  }
  
  .action-section {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
