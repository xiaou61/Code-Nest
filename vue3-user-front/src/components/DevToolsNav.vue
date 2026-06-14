<template>
  <div class="dev-tools-nav">
    <div class="nav-container">
      <div class="nav-brand">
        <el-icon class="brand-icon"><Tools /></el-icon>
        <span class="brand-text">程序员工具</span>
        <CnStatusTag type="brand" size="sm" subtle>工具集</CnStatusTag>
      </div>

      <div class="nav-items">
        <router-link
          v-for="tool in tools"
          :key="tool.path"
          :to="tool.path"
          class="nav-item"
          :class="{ active: route.path === tool.path }"
        >
          <el-icon class="nav-icon">
            <component :is="tool.icon" />
          </el-icon>
          <span class="nav-text">{{ tool.name }}</span>
        </router-link>
      </div>
      
      <div class="nav-actions">
        <el-button size="small" text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回工具箱
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import {
  Tools,
  Document,
  Operation,
  ChatLineSquare,
  ArrowLeft
} from '@element-plus/icons-vue'
import { CnStatusTag } from '@/design-system'

const router = useRouter()
const route = useRoute()

const tools = [
  {
    name: 'JSON工具',
    path: '/dev-tools/json',
    icon: Document
  },
  {
    name: '文本比对',
    path: '/dev-tools/text-diff',
    icon: Operation
  },
  {
    name: '聚合翻译',
    path: '/dev-tools/translation',
    icon: ChatLineSquare
  }
]

const goBack = () => {
  router.push('/dev-tools')
}
</script>

<style scoped>
.dev-tools-nav {
  background: var(--cn-nav-bg);
  border-bottom: 1px solid var(--cn-color-border-subtle);
  box-shadow: var(--cn-shadow-xs);
  backdrop-filter: blur(14px);
}

.nav-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 var(--cn-space-5);
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-5);
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  font-weight: 600;
  color: var(--cn-color-text-primary);
  min-width: 0;
}

.brand-icon {
  font-size: 1.5rem;
  color: var(--cn-color-brand-primary);
}

.brand-text {
  font-size: 1.1rem;
}

.nav-items {
  display: flex;
  gap: var(--cn-space-2);
  flex: 1;
  justify-content: center;
  min-width: 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  padding: var(--cn-space-2) var(--cn-space-4);
  border-radius: var(--cn-radius-control);
  text-decoration: none;
  color: var(--cn-color-text-secondary);
  transition: var(--cn-transition);
  font-size: 0.9rem;
  min-width: 0;
}

.nav-item:hover {
  color: var(--cn-color-brand-primary);
  background: var(--cn-color-bg-surface-muted);
}

.nav-item.active {
  color: var(--cn-nav-active-color);
  background: var(--cn-nav-active-bg);
  font-weight: 500;
}

.nav-icon {
  font-size: 1.1rem;
}

.nav-actions {
  display: flex;
  gap: var(--cn-space-3);
}

@media (max-width: 768px) {
  .nav-container {
    padding: var(--cn-space-3) var(--cn-space-4);
    flex-wrap: wrap;
    height: auto;
    min-height: 60px;
  }

  .nav-items {
    order: 3;
    width: 100%;
    justify-content: space-around;
    margin-top: var(--cn-space-2);
  }

  .nav-item {
    flex-direction: column;
    gap: var(--cn-space-1);
    padding: var(--cn-space-2) var(--cn-space-3);
  }

  .nav-text {
    font-size: 0.8rem;
  }

  .nav-actions {
    order: 2;
  }
}

@media (max-width: 480px) {
  .brand-text {
    display: none;
  }

  .nav-text {
    display: none;
  }

  .nav-item {
    padding: var(--cn-space-2);
  }
}
</style>
