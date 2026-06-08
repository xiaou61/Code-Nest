<template>
  <div class="deck-card" @click="$emit('click')">
    <div class="deck-cover">
      <img v-if="deck.coverImage" :src="deck.coverImage" alt="封面" />
      <div v-else class="default-cover">
        <el-icon :size="32"><Collection /></el-icon>
      </div>
      <div class="deck-stats-overlay">
        <span class="stat-item">
          <el-icon><Files /></el-icon>
          {{ deck.cardCount || 0 }}
        </span>
        <span class="stat-item" v-if="deck.forkCount">
          <el-icon><DocumentCopy /></el-icon>
          {{ deck.forkCount }}
        </span>
      </div>
    </div>
    
    <div class="deck-info">
      <h3 class="deck-name">{{ deck.name }}</h3>
      <p class="deck-description">{{ deck.description || '暂无描述' }}</p>
      
      <div class="deck-tags" v-if="deck.tags">
        <CnStatusTag v-for="tag in tagList" :key="tag" size="sm" type="info" subtle>
          {{ tag }}
        </CnStatusTag>
      </div>
      
      <div class="deck-progress" v-if="showProgress && deck.learnedCount !== undefined">
        <el-progress 
          :percentage="progressPercent" 
          :stroke-width="6"
          :show-text="false"
        />
        <span class="progress-text">
          已学 {{ deck.learnedCount || 0 }} / {{ deck.cardCount || 0 }}
        </span>
      </div>
      
      <div class="deck-footer">
        <div class="deck-author">
          <el-avatar :size="20" :src="deck.userAvatar">
            {{ deck.userName?.charAt(0) }}
          </el-avatar>
          <span class="author-name">{{ deck.userName }}</span>
        </div>
        <div class="deck-due" v-if="deck.dueCount > 0">
          <CnStatusTag type="warning" size="sm">
            {{ deck.dueCount }} 待复习
          </CnStatusTag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Collection, Files, DocumentCopy } from '@element-plus/icons-vue'
import { CnStatusTag } from '@/design-system'

interface DeckCardData {
  coverImage?: string
  cardCount?: number
  forkCount?: number
  name?: string
  description?: string
  tags?: string
  learnedCount?: number
  userAvatar?: string
  userName?: string
  dueCount?: number
}

const props = withDefaults(defineProps<{
  deck: DeckCardData
  showProgress?: boolean
}>(), {
  showProgress: false
})

defineEmits(['click'])

const tagList = computed(() => {
  if (!props.deck.tags) return []
  return props.deck.tags.split(',').map((tag) => tag.trim()).filter(Boolean).slice(0, 3)
})

const progressPercent = computed(() => {
  if (!props.deck.cardCount || props.deck.cardCount === 0) return 0
  return Math.round((props.deck.learnedCount || 0) / props.deck.cardCount * 100)
})
</script>

<style lang="scss" scoped>
.deck-card {
  background: var(--cn-color-bg-surface);
  border-radius: var(--cn-radius-panel);
  overflow: hidden;
  cursor: pointer;
  transition:
    transform var(--cn-motion-base) var(--cn-ease-out),
    border-color var(--cn-motion-base) var(--cn-ease-out),
    box-shadow var(--cn-motion-base) var(--cn-ease-out);
  border: 1px solid var(--cn-color-border-subtle);
  box-shadow: var(--cn-shadow-card);
  
  &:hover {
    transform: translateY(-3px);
    box-shadow: var(--cn-shadow-md);
    border-color: color-mix(in srgb, var(--cn-color-brand-primary) 34%, var(--cn-color-border-subtle));
  }
}

.deck-cover {
  position: relative;
  height: 120px;
  background: color-mix(in srgb, var(--cn-color-brand-soft) 72%, var(--cn-color-bg-surface));
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  
  .default-cover {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: var(--cn-color-brand-primary);
    opacity: 0.6;
  }
  
  .deck-stats-overlay {
    position: absolute;
    bottom: 8px;
    right: 8px;
    display: flex;
    gap: 8px;
    
    .stat-item {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 4px 8px;
      background: color-mix(in srgb, var(--cn-color-text-primary) 72%, transparent);
      color: var(--cn-button-primary-color);
      border-radius: var(--cn-radius-control);
      font-size: 12px;
    }
  }
}

.deck-info {
  padding: var(--cn-space-4);
}

.deck-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--cn-color-text-primary);
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.deck-description {
  font-size: 13px;
  color: var(--cn-color-text-secondary);
  margin: 0 0 12px 0;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  min-height: 40px;
}

.deck-tags {
  display: flex;
  gap: var(--cn-space-2);
  flex-wrap: wrap;
  margin-bottom: var(--cn-space-3);
}

.deck-progress {
  margin-bottom: var(--cn-space-3);
  
  .progress-text {
    display: block;
    margin-top: var(--cn-space-1);
    font-size: 12px;
    color: var(--cn-color-text-secondary);
  }
}

.deck-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  
  .deck-author {
    display: flex;
    align-items: center;
    gap: var(--cn-space-2);
    
    .author-name {
      font-size: 12px;
      color: var(--cn-color-text-secondary);
    }
  }
}
</style>
