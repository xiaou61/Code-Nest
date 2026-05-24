<template>
  <div class="task-list">
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="3" animated />
    </div>
    
    <div v-else-if="tasks.length === 0" class="empty-state">
      <el-empty description="暂无任务" :image-size="80" />
    </div>
    
    <div v-else class="tasks">
      <div 
        v-for="task in tasks" 
        :key="task.id" 
        class="task-item"
        :class="{ 'completed': task.todayChecked }"
      >
        <div class="task-main">
          <div class="task-check" @click="handleCheckin(task)">
            <el-icon v-if="task.todayChecked" class="checked"><CircleCheckFilled /></el-icon>
            <el-icon v-else class="unchecked"><CircleCheck /></el-icon>
          </div>
          
          <div class="task-content">
            <div class="task-title">{{ task.taskName }}</div>
            <div class="task-desc" v-if="task.taskDesc">{{ task.taskDesc }}</div>
            <div class="task-meta">
              <span v-if="task.repeatType === 1" class="meta-tag daily">每日</span>
              <span v-else-if="task.repeatType === 2" class="meta-tag weekly">工作日</span>
              <span v-else class="meta-tag once">单次</span>
              <span class="task-type">{{ task.taskTypeName || '自定义任务' }}</span>
              <span class="checkin-count">
                <el-icon><Check /></el-icon>
                {{ task.checkinCount || 0 }}人已打卡
              </span>
            </div>
          </div>
        </div>
        
        <div class="task-actions" v-if="isAdmin">
          <el-dropdown trigger="click">
            <el-button text size="small">
              <el-icon><MoreFilled /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleEdit(task)">
                  <el-icon><Edit /></el-icon>编辑
                </el-dropdown-item>
                <el-dropdown-item 
                  @click="handleToggleStatus(task)"
                  :disabled="task.status === 3"
                >
                  <el-icon><VideoPause /></el-icon>
                  {{ task.status === 1 ? '暂停' : '启用' }}
                </el-dropdown-item>
                <el-dropdown-item @click="handleDelete(task)" divided>
                  <el-icon><Delete /></el-icon>删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>
    
    <!-- 加载更多 -->
    <div v-if="showAll && hasMore" class="load-more">
      <el-button text @click="loadMore" :loading="loadingMore">
        加载更多
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, CircleCheckFilled, Check, MoreFilled, Edit, Delete, VideoPause } from '@element-plus/icons-vue'
import teamApi from '@/api/team'

const props = defineProps({
  teamId: { type: [String, Number], required: true },
  isAdmin: { type: Boolean, default: false },
  showAll: { type: Boolean, default: false }
})

const emit = defineEmits(['checkin', 'edit'])

const tasks = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(false)
const pageNum = ref(1)

onMounted(() => {
  loadTasks()
})

watch(() => props.teamId, () => {
  pageNum.value = 1
  loadTasks()
})

const loadTasks = async () => {
  loading.value = true
  try {
    let response
    if (props.showAll) {
      response = await teamApi.getTaskList(props.teamId, { pageNum: pageNum.value, pageSize: 10 })
      tasks.value = response || []
      hasMore.value = false
    } else {
      response = await teamApi.getTodayTasks(props.teamId)
      tasks.value = response || []
    }
  } catch (error) {
    console.error('加载任务失败:', error)
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  loadingMore.value = true
  pageNum.value++
  try {
    const response = await teamApi.getTaskList(props.teamId, { pageNum: pageNum.value, pageSize: 10 })
    const newTasks = response || []
    tasks.value = [...tasks.value, ...newTasks]
    hasMore.value = false
  } catch (error) {
    console.error('加载更多失败:', error)
    pageNum.value--
  } finally {
    loadingMore.value = false
  }
}

const handleCheckin = (task) => {
  if (!task.todayChecked) {
    emit('checkin', task)
  }
}

const handleEdit = (task) => {
  emit('edit', task)
}

const handleToggleStatus = async (task) => {
  const newStatus = task.status === 1 ? 2 : 1
  try {
    await teamApi.setTaskStatus(task.id, newStatus)
    ElMessage.success(newStatus === 1 ? '任务已启用' : '任务已暂停')
    loadTasks()
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const handleDelete = async (task) => {
  try {
    await ElMessageBox.confirm('确定要删除这个任务吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await teamApi.deleteTask(task.id)
    ElMessage.success('删除成功')
    loadTasks()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

defineExpose({ loadTasks })
</script>

<style lang="scss" scoped>
.task-list {
  .loading-state, .empty-state {
    padding: 20px 0;
  }
}

.task-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 12px;
  border-radius: 8px;
  transition: background 0.2s;
  
  &:hover {
    background: #f8f9fc;
  }
  
  &.completed {
    opacity: 0.7;
    
    .task-title {
      text-decoration: line-through;
      color: #999;
    }
  }
  
  & + .task-item {
    border-top: 1px solid #f0f0f0;
  }
}

.task-main {
  display: flex;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.task-check {
  cursor: pointer;
  padding-top: 2px;
  
  .checked {
    font-size: 20px;
    color: #67c23a;
  }
  
  .unchecked {
    font-size: 20px;
    color: #dcdfe6;
    
    &:hover {
      color: #409eff;
    }
  }
}

.task-content {
  flex: 1;
  min-width: 0;
  
  .task-title {
    font-size: 14px;
    font-weight: 500;
    color: #333;
    margin-bottom: 4px;
  }
  
  .task-desc {
    font-size: 13px;
    color: #999;
    margin-bottom: 6px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  
  .task-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .meta-tag {
      padding: 1px 6px;
      font-size: 11px;
      border-radius: 4px;
      
      &.daily { background: #e8f4fd; color: #409eff; }
      &.weekly { background: #f0f9eb; color: #67c23a; }
      &.once { background: #fdf2e9; color: #e6a23c; }
    }
    
    .checkin-count {
      display: flex;
      align-items: center;
      gap: 3px;
      font-size: 12px;
      color: #999;
    }

    .task-type {
      font-size: 12px;
      color: #666;
    }
  }
}

.task-actions {
  flex-shrink: 0;
}

.load-more {
  text-align: center;
  padding: 12px 0;
}
</style>
