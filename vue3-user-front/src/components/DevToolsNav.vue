<template>
  <div class="dev-tools-nav">
    <div class="nav-container">
      <div class="nav-brand">
        <el-icon class="brand-icon"><Tools /></el-icon>
        <span class="brand-text">程序员工具</span>
      </div>
      
      <div class="nav-items">
        <router-link
          v-for="tool in tools"
          :key="tool.path"
          :to="tool.path"
          class="nav-item"
          :class="{ active: $route.path === tool.path }"
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

<script setup>
import { useRouter } from 'vue-router'
import { 
  Tools, Document, Operation, ChatLineSquare, ArrowLeft 
} from '@element-plus/icons-vue'

const router = useRouter()

// 工具列表
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

// 返回工具箱首页
const goBack = () => {
  router.push('/dev-tools')
}
</script>

<style scoped>
.dev-tools-nav {
  background: white;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.nav-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #374151;
}

.brand-icon {
  font-size: 1.5rem;
  color: #3b82f6;
}

.brand-text {
  font-size: 1.1rem;
}

.nav-items {
  display: flex;
  gap: 8px;
  flex: 1;
  justify-content: center;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  text-decoration: none;
  color: #6b7280;
  transition: all 0.3s;
  font-size: 0.9rem;
}

.nav-item:hover {
  color: #3b82f6;
  background: #f3f4f6;
}

.nav-item.active {
  color: #3b82f6;
  background: #dbeafe;
  font-weight: 500;
}

.nav-icon {
  font-size: 1.1rem;
}

.nav-actions {
  display: flex;
  gap: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .nav-container {
    padding: 0 15px;
    flex-wrap: wrap;
    height: auto;
    min-height: 60px;
    padding-top: 10px;
    padding-bottom: 10px;
  }
  
  .nav-items {
    order: 3;
    width: 100%;
    justify-content: space-around;
    margin-top: 10px;
  }
  
  .nav-item {
    flex-direction: column;
    gap: 4px;
    padding: 8px 12px;
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
    padding: 8px;
  }
}
</style>
