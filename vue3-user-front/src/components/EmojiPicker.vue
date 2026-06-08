<template>
  <el-popover
    v-model:visible="visible"
    placement="bottom"
    :width="320"
    trigger="manual"
  >
    <template #reference>
      <el-button text @click="togglePicker">
        <span class="emoji-icon">😊</span>
        表情
      </el-button>
    </template>

    <div class="emoji-picker">
      <CnStatusTag type="brand" size="sm" subtle>常用表情</CnStatusTag>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="表情" name="smileys">
          <div class="emoji-grid">
            <button
              v-for="emoji in smileys"
              :key="emoji"
              type="button"
              class="emoji-item"
              @click.stop="selectEmoji(emoji)"
            >
              {{ emoji }}
            </button>
          </div>
        </el-tab-pane>
        <el-tab-pane label="手势" name="gestures">
          <div class="emoji-grid">
            <button
              v-for="emoji in gestures"
              :key="emoji"
              type="button"
              class="emoji-item"
              @click.stop="selectEmoji(emoji)"
            >
              {{ emoji }}
            </button>
          </div>
        </el-tab-pane>
        <el-tab-pane label="其他" name="others">
          <div class="emoji-grid">
            <button
              v-for="emoji in others"
              :key="emoji"
              type="button"
              class="emoji-item"
              @click.stop="selectEmoji(emoji)"
            >
              {{ emoji }}
            </button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { CnStatusTag } from '@/design-system'

const emit = defineEmits<{
  select: [emoji: string]
}>()

const visible = ref(false)
const activeTab = ref('smileys')

const smileys = ['😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂', '🙂', '🙃', '😉', '😊', '😇', '🥰', '😍', '🤩', '😘', '😗', '😚', '😙', '😋', '😛', '😜', '🤪', '😝', '🤑', '🤗', '🤭', '🤫', '🤔', '🤐', '🤨', '😐', '😑', '😶', '😏', '😒', '🙄', '😬', '🤥', '😌', '😔', '😪', '🤤', '😴', '😷', '🤒', '🤕', '🤢', '🤮', '🤧', '🥵', '🥶', '😵', '🤯', '🤠', '🥳', '😎', '🤓', '🧐']

const gestures = ['👍', '👎', '👊', '✊', '🤛', '🤜', '🤞', '✌️', '🤟', '🤘', '👌', '🤏', '👈', '👉', '👆', '👇', '☝️', '👋', '🤚', '🖐', '✋', '🖖', '👏', '🙌', '👐', '🤲', '🤝', '🙏']

const others = ['❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔', '❣️', '💕', '💞', '💓', '💗', '💖', '💘', '💝', '💟', '☮️', '✝️', '☪️', '🕉', '☸️', '✡️', '🔯', '🕎', '☯️', '☦️', '🛐', '⭐', '🌟', '✨', '⚡', '💥', '🔥', '☀️', '🌈', '☁️', '⛅', '⛈', '🌤', '🌥', '🌦', '🌧', '🌨', '🌩', '🌪', '❄️', '☃️', '⛄']

const togglePicker = () => {
  visible.value = !visible.value
}

const selectEmoji = (emoji) => {
  emit('select', emoji)
}
</script>

<style scoped>
.emoji-picker {
  max-height: 300px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-2);
  padding: var(--cn-space-1);
  background: var(--cn-color-bg-elevated);
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: var(--cn-space-2);
  padding: var(--cn-space-2) 0;
}

.emoji-item {
  appearance: none;
  border: 1px solid transparent;
  background: transparent;
  font-size: 24px;
  cursor: pointer;
  text-align: center;
  padding: var(--cn-space-1);
  border-radius: var(--cn-radius-control);
  transition: var(--cn-transition);
  line-height: 1.25;
}

.emoji-item:hover {
  background-color: var(--cn-color-bg-surface-muted);
  border-color: var(--cn-color-border-subtle);
  transform: scale(1.2);
}

.emoji-item:focus-visible {
  outline: 2px solid var(--cn-color-focus-ring);
  outline-offset: 2px;
}

.emoji-icon {
  font-size: 16px;
  margin-right: var(--cn-space-1);
}
</style>

