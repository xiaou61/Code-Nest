<template>
  <div class="pen-card" @click="handleClick">
    <!-- 预览图 -->
    <div class="pen-preview">
      <div class="preview-placeholder">
        <el-icon><Document /></el-icon>
        <span>{{ pen.title }}</span>
      </div>
      
      <!-- 付费标识 -->
      <div class="price-badge" v-if="!pen.isFree">
        <el-icon><Lock /></el-icon>
        {{ pen.forkPrice }} 积分
      </div>
    </div>

    <!-- 作品信息 -->
    <div class="pen-info">
      <h3 class="pen-title">{{ pen.title }}</h3>
      <p class="pen-description" v-if="pen.description">
        {{ pen.description }}
      </p>

      <!-- 标签 -->
      <div class="pen-tags" v-if="pen.tags && pen.tags.length > 0">
        <CnStatusTag
          v-for="(tag, index) in pen.tags.slice(0, 3)"
          :key="index"
          size="sm"
          type="info"
          subtle
        >
          {{ tag }}
        </CnStatusTag>
      </div>

      <!-- 作者信息 -->
      <div class="pen-author">
        <el-avatar :size="24" :src="pen.userAvatar">
          {{ pen.userNickname?.charAt(0) }}
        </el-avatar>
        <span class="author-name">{{ pen.userNickname }}</span>
      </div>

      <!-- 统计信息 -->
      <div class="pen-stats">
        <span class="stat-item">
          <el-icon><View /></el-icon>
          {{ formatNumber(pen.viewCount) }}
        </span>
        <span class="stat-item">
          <el-icon><Star /></el-icon>
          {{ formatNumber(pen.likeCount) }}
        </span>
        <span class="stat-item">
          <el-icon><Collection /></el-icon>
          {{ formatNumber(pen.collectCount) }}
        </span>
        <span class="stat-item" v-if="pen.forkCount > 0">
          <el-icon><CopyDocument /></el-icon>
          {{ formatNumber(pen.forkCount) }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Document, Lock, View, Star, Collection, CopyDocument } from '@element-plus/icons-vue'
import { CnStatusTag } from '@/design-system'

const props = defineProps({
  pen: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['click'])

const handleClick = () => {
  emit('click', props.pen)
}

// 格式化数字
const formatNumber = (num) => {
  if (!num) return 0
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num
}
</script>

<style scoped lang="scss">
.pen-card {
  min-width: 0;
  height: 100%;
  background: var(--cn-card-bg);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-radius-card);
  box-shadow: var(--cn-card-shadow);
  overflow: hidden;
  cursor: pointer;
  transition:
    transform var(--cn-motion-base) var(--cn-ease-out),
    border-color var(--cn-motion-base) var(--cn-ease-out),
    box-shadow var(--cn-motion-base) var(--cn-ease-out);

  &:hover {
    transform: translateY(-2px);
    border-color: color-mix(in srgb, var(--cn-color-brand-primary) 28%, var(--cn-card-border));
    box-shadow: var(--cn-shadow-sm);
  }

  .pen-preview {
    position: relative;
    height: 200px;
    background: color-mix(in srgb, var(--cn-color-bg-surface-muted) 76%, var(--cn-color-brand-soft));
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--cn-color-brand-primary);
    overflow: hidden;

    .preview-placeholder {
      text-align: center;
      padding: var(--cn-space-5);
      min-width: 0;

      .el-icon {
        font-size: 48px;
        margin-bottom: var(--cn-space-2);
      }

      span {
        display: block;
        max-width: 220px;
        font-size: 16px;
        font-weight: 500;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .price-badge {
      position: absolute;
      top: var(--cn-space-3);
      right: var(--cn-space-3);
      background: var(--cn-color-warning-soft);
      color: var(--cn-color-warning);
      padding: 5px var(--cn-space-3);
      border: 1px solid color-mix(in srgb, var(--cn-color-warning) 26%, var(--cn-color-border-subtle));
      border-radius: var(--cn-radius-pill);
      font-size: 12px;
      font-weight: 600;
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }

  .pen-info {
    padding: var(--cn-space-4);

    .pen-title {
      font-size: 16px;
      font-weight: 600;
      color: var(--cn-color-text-primary);
      margin: 0 0 var(--cn-space-2);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .pen-description {
      font-size: 14px;
      color: var(--cn-color-text-secondary);
      margin: 0 0 var(--cn-space-3);
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      line-height: 1.5;
      max-height: 3em;
    }

    .pen-tags {
      display: flex;
      flex-wrap: wrap;
      gap: var(--cn-space-2);
      margin-bottom: var(--cn-space-3);
    }

    .pen-author {
      display: flex;
      align-items: center;
      gap: var(--cn-space-2);
      margin-bottom: var(--cn-space-3);
      padding-bottom: var(--cn-space-3);
      border-bottom: 1px solid var(--cn-color-border-subtle);

      .author-name {
        font-size: 14px;
        color: var(--cn-color-text-secondary);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .pen-stats {
      display: flex;
      flex-wrap: wrap;
      gap: var(--cn-space-3);

      .stat-item {
        display: flex;
        align-items: center;
        gap: var(--cn-space-1);
        font-size: 13px;
        color: var(--cn-color-text-tertiary);

        .el-icon {
          font-size: 16px;
        }
      }
    }
  }
}
</style>

