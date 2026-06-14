<template>
  <article class="team-card" @click="goToDetail">
    <div class="team-avatar">
      <img v-if="team.teamAvatar" :src="team.teamAvatar" :alt="team.teamName || '小组头像'" />
      <span v-else class="avatar-text">{{ team.teamName?.charAt(0) || '组' }}</span>
    </div>

    <div class="team-info">
      <div class="team-header">
        <div class="team-title">
          <h3 class="team-name">{{ team.teamName || '未命名小组' }}</h3>
          <CnStatusTag :type="getTypeTone(team.teamType)" size="sm" subtle>
            {{ getTypeText(team.teamType) }}
          </CnStatusTag>
          <CnStatusTag v-if="Number(team.status) === 2" type="danger" size="sm">已满员</CnStatusTag>
        </div>
      </div>

      <p class="team-desc">{{ team.teamDesc || '暂无简介' }}</p>

      <div v-if="parsedTags.length" class="team-tags">
        <CnStatusTag v-for="tag in parsedTags" :key="tag" type="neutral" size="sm" :dot="false" subtle>
          {{ tag }}
        </CnStatusTag>
      </div>

      <div class="team-stats">
        <span class="stat-item">
          <el-icon><User /></el-icon>
          {{ team.currentMembers || 0 }}/{{ team.maxMembers || 0 }}
        </span>
        <span class="stat-item">
          <el-icon><Calendar /></el-icon>
          {{ formatDate(team.createTime) }}
        </span>
        <span v-if="team.totalCheckins" class="stat-item">
          <el-icon><Check /></el-icon>
          {{ team.totalCheckins }} 次打卡
        </span>
      </div>
    </div>

    <div class="team-action" @click.stop>
      <template v-if="!team.joined">
        <el-button
          v-if="Number(team.joinType) === 1 && Number(team.status) !== 2"
          type="primary"
          size="small"
          @click="handleJoin"
          :loading="joining"
        >
          加入
        </el-button>
        <el-button
          v-else-if="Number(team.joinType) === 2 && Number(team.status) !== 2"
          type="primary"
          size="small"
          plain
          @click="handleApply"
          :loading="joining"
        >
          申请
        </el-button>
        <el-button v-else-if="Number(team.joinType) === 3" type="info" size="small" plain disabled>
          仅邀请
        </el-button>
        <el-button v-else-if="Number(team.status) === 2" type="info" size="small" plain disabled>
          已满员
        </el-button>
      </template>
      <CnStatusTag v-else type="success" size="sm">
        <el-icon><SuccessFilled /></el-icon>
        已加入
      </CnStatusTag>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar, Check, SuccessFilled, User } from '@element-plus/icons-vue'
import { CnStatusTag, type CnTone } from '@/design-system'
import teamApi from '@/api/team'

interface TeamRecord {
  id: number | string
  teamName?: string
  teamDesc?: string
  teamAvatar?: string
  teamType?: number | string
  status?: number | string
  tags?: string
  currentMembers?: number
  maxMembers?: number
  createTime?: string
  totalCheckins?: number
  joined?: boolean
  joinType?: number | string
}

const props = defineProps<{
  team: TeamRecord
}>()

const emit = defineEmits<{
  refresh: []
  apply: [team: TeamRecord]
}>()

const router = useRouter()
const joining = ref(false)

const parsedTags = computed(() => parseTags(props.team.tags))

const goToDetail = () => {
  router.push(`/team/${props.team.id}`)
}

const handleJoin = async () => {
  joining.value = true
  try {
    await teamApi.applyJoin(props.team.id)
    ElMessage.success('加入成功')
    emit('refresh')
  } catch (error) {
    console.error('加入失败:', error)
    ElMessage.error(error instanceof Error ? error.message : '加入失败')
  } finally {
    joining.value = false
  }
}

const handleApply = () => {
  emit('apply', props.team)
}

const parseTags = (tags?: string) => {
  if (!tags) return []
  return tags
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean)
    .slice(0, 3)
}

const getTypeText = (type: unknown) => {
  const typeMap: Record<string, string> = {
    1: '目标型',
    2: '学习型',
    3: '打卡型'
  }
  return typeMap[String(type)] || '学习型'
}

const getTypeTone = (type: unknown): CnTone => {
  const toneMap: Record<string, CnTone> = {
    1: 'brand',
    2: 'success',
    3: 'warning'
  }
  return toneMap[String(type)] || 'success'
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}
</script>

<style scoped>
.team-card {
  display: flex;
  align-items: flex-start;
  gap: var(--cn-space-4);
  min-width: 0;
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-card-radius);
  background: var(--cn-card-bg);
  box-shadow: var(--cn-card-shadow);
  cursor: pointer;
  transition:
    transform var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-base) var(--cn-ease-out),
    box-shadow var(--cn-motion-base) var(--cn-ease-out);
}

.team-card:hover {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 28%, var(--cn-color-border));
  box-shadow: var(--cn-shadow-sm);
}

.team-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  overflow: hidden;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
  flex-shrink: 0;
}

.team-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-text {
  font-size: 24px;
  font-weight: 800;
}

.team-info {
  flex: 1;
  min-width: 0;
}

.team-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  min-width: 0;
}

.team-name {
  min-width: 0;
  margin: 0;
  overflow-wrap: anywhere;
  color: var(--cn-color-text-primary);
  font-size: 16px;
  font-weight: 700;
  line-height: 1.35;
}

.team-desc {
  display: -webkit-box;
  margin: var(--cn-space-2) 0 0;
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.team-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-3);
}

.team-stats {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-4);
  margin-top: var(--cn-space-3);
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-weight: 650;
}

.team-action {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.team-action :deep(.cn-status-tag__label) {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-1);
}

@media (max-width: 640px) {
  .team-card {
    display: grid;
  }

  .team-avatar {
    width: 52px;
    height: 52px;
  }

  .avatar-text {
    font-size: 19px;
  }

  .team-action :deep(.el-button) {
    width: 100%;
  }
}
</style>
