<template>
  <div class="task-list">
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="3" animated />
    </div>

    <CnEmptyState
      v-else-if="tasks.length === 0"
      title="暂无任务"
      description="创建任务后，小组成员可以围绕任务进行打卡。"
      icon="TK"
      size="sm"
      surface="transparent"
    />

    <div v-else class="tasks">
      <article v-for="task in tasks" :key="task.id" class="task-item" :class="{ completed: task.todayChecked }">
        <div class="task-main">
          <button class="task-check" type="button" :aria-label="task.todayChecked ? '已打卡' : '打卡'" @click="handleCheckin(task)">
            <el-icon v-if="task.todayChecked" class="checked"><CircleCheckFilled /></el-icon>
            <el-icon v-else class="unchecked"><CircleCheck /></el-icon>
          </button>

          <div class="task-content">
            <div class="task-title">{{ task.taskName || '未命名任务' }}</div>
            <div v-if="task.taskDesc" class="task-desc">{{ task.taskDesc }}</div>
            <div class="task-meta">
              <CnStatusTag :type="getRepeatTone(task.repeatType)" size="sm" :dot="false" subtle>
                {{ getRepeatText(task.repeatType) }}
              </CnStatusTag>
              <CnStatusTag type="neutral" size="sm" :dot="false" subtle>
                {{ task.taskTypeName || '自定义任务' }}
              </CnStatusTag>
              <span class="checkin-count">
                <el-icon><Check /></el-icon>
                {{ task.checkinCount || 0 }} 人已打卡
              </span>
            </div>
          </div>
        </div>

        <div v-if="isAdmin" class="task-actions">
          <el-dropdown trigger="click">
            <el-button text size="small" aria-label="任务操作">
              <el-icon><MoreFilled /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleEdit(task)">
                  <el-icon><Edit /></el-icon>
                  编辑
                </el-dropdown-item>
                <el-dropdown-item @click="handleToggleStatus(task)" :disabled="Number(task.status) === 3">
                  <el-icon><VideoPause /></el-icon>
                  {{ Number(task.status) === 1 ? '暂停' : '启用' }}
                </el-dropdown-item>
                <el-dropdown-item @click="handleDelete(task)" divided>
                  <el-icon><Delete /></el-icon>
                  删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </article>
    </div>

    <div v-if="showAll && hasMore" class="load-more">
      <el-button text @click="loadMore" :loading="loadingMore">加载更多</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, CircleCheck, CircleCheckFilled, Delete, Edit, MoreFilled, VideoPause } from '@element-plus/icons-vue'
import { CnEmptyState, CnStatusTag, type CnTone } from '@/design-system'
import teamApi from '@/api/team'

interface TeamTask {
  id: number | string
  taskName?: string
  taskDesc?: string
  repeatType?: number | string
  taskTypeName?: string
  checkinCount?: number
  todayChecked?: boolean | number
  status?: number | string
}

const props = withDefaults(
  defineProps<{
    teamId: string | number
    isAdmin?: boolean
    showAll?: boolean
  }>(),
  {
    isAdmin: false,
    showAll: false
  }
)

const emit = defineEmits<{
  checkin: [task: TeamTask]
  edit: [task: TeamTask]
}>()

const tasks = ref<TeamTask[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(false)
const pageNum = ref(1)

onMounted(() => {
  loadTasks()
})

watch(
  () => props.teamId,
  () => {
    pageNum.value = 1
    loadTasks()
  }
)

const loadTasks = async () => {
  loading.value = true
  try {
    let response: TeamTask[]
    if (props.showAll) {
      response = (await teamApi.getTaskList(props.teamId, { pageNum: pageNum.value, pageSize: 10 })) as TeamTask[]
      tasks.value = response || []
      hasMore.value = false
    } else {
      response = (await teamApi.getTodayTasks(props.teamId)) as TeamTask[]
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
    const response = (await teamApi.getTaskList(props.teamId, { pageNum: pageNum.value, pageSize: 10 })) as TeamTask[]
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

const handleCheckin = (task: TeamTask) => {
  if (!task.todayChecked) {
    emit('checkin', task)
  }
}

const handleEdit = (task: TeamTask) => {
  emit('edit', task)
}

const handleToggleStatus = async (task: TeamTask) => {
  const newStatus = Number(task.status) === 1 ? 2 : 1
  try {
    await teamApi.setTaskStatus(task.id, newStatus)
    ElMessage.success(newStatus === 1 ? '任务已启用' : '任务已暂停')
    loadTasks()
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const handleDelete = async (task: TeamTask) => {
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

const getRepeatText = (type: unknown) => {
  const map: Record<string, string> = {
    1: '每日',
    2: '工作日'
  }
  return map[String(type)] || '单次'
}

const getRepeatTone = (type: unknown): CnTone => {
  const map: Record<string, CnTone> = {
    1: 'brand',
    2: 'success'
  }
  return map[String(type)] || 'warning'
}

defineExpose({ loadTasks })
</script>

<style scoped>
.task-list {
  min-width: 0;
}

.loading-state {
  padding: var(--cn-space-5) 0;
}

.tasks {
  display: grid;
}

.task-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-3);
  padding: var(--cn-space-3);
  border-radius: var(--cn-radius-card);
  transition: background-color var(--cn-motion-base) var(--cn-ease-out);
}

.task-item:hover {
  background: var(--cn-color-bg-surface-muted);
}

.task-item + .task-item {
  border-top: 1px solid var(--cn-color-border-subtle);
}

.task-item.completed {
  opacity: 0.74;
}

.task-item.completed .task-title {
  color: var(--cn-color-text-tertiary);
  text-decoration: line-through;
}

.task-main {
  display: flex;
  gap: var(--cn-space-3);
  flex: 1;
  min-width: 0;
}

.task-check {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
  flex-shrink: 0;
}

.task-check .el-icon {
  font-size: 21px;
}

.task-check .checked {
  color: var(--cn-color-success);
}

.task-check .unchecked {
  color: var(--cn-color-text-disabled);
}

.task-check:hover .unchecked {
  color: var(--cn-color-brand-primary);
}

.task-content {
  flex: 1;
  min-width: 0;
}

.task-title {
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.task-desc {
  margin-top: var(--cn-space-1);
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-2);
}

.checkin-count {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-weight: 650;
}

.task-actions {
  flex-shrink: 0;
}

.load-more {
  display: flex;
  justify-content: center;
  padding: var(--cn-space-3) 0;
}
</style>
