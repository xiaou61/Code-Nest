<template>
  <div class="dashboard">
    <h1 class="page-title">仪表板</h1>
    
    <!-- 欢迎信息 -->
    <el-row :gutter="20">
      <el-col :xs="24" :lg="16">
        <el-card>
          <template #header>
            <span>欢迎回来</span>
          </template>
          
          <div class="welcome-content">
            <h2 class="welcome-title">欢迎回来，{{ userStore.realName || userStore.username }}！</h2>
            <p class="welcome-subtitle">
              今天是 {{ currentDate }}，祝您工作愉快！
            </p>
            
            <div class="role-line" v-if="userStore.roles.length > 0">
              <el-tag type="success" class="role-tag">
                当前角色：{{ currentRoles }}
              </el-tag>
              <el-tag type="info" v-if="userStore.userInfo?.lastLoginTime">
                上次登录：{{ userStore.userInfo.lastLoginTime }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :lg="8">
        <el-card>
          <template #header>
            <span>快捷操作</span>
          </template>
          
          <div class="quick-actions">
            <el-button
              type="primary"
              @click="$router.push('/login-logs')"
              class="quick-btn"
            >
              <el-icon><Document /></el-icon>
              登录日志
            </el-button>
            
            <el-button
              type="success"
              @click="$router.push('/profile')"
              class="quick-btn"
            >
              <el-icon><User /></el-icon>
              个人中心
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 当前日期
const currentDate = computed(() => {
  const now = new Date()
  return now.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
})

// 当前角色
const currentRoles = computed(() => {
  if (userStore.roles.length === 0) return '暂无角色'
  return userStore.roles.map(role => role.roleName).join('、')
})
</script>

<style scoped>
.dashboard {
  display: grid;
  gap: 4px;
}

.page-title {
  margin: 0 0 24px;
  font-size: 28px;
  font-weight: 600;
  color: var(--cn-text-primary);
}

.welcome-content {
  padding: 18px 0 6px;
}

.welcome-title {
  margin: 0;
  font-size: 24px;
  color: var(--cn-text-primary);
}

.welcome-subtitle {
  margin: 10px 0 0;
  color: var(--cn-text-secondary);
}

.role-line {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.role-tag {
  margin-right: 0;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quick-btn {
  width: 100%;
  justify-content: flex-start;
  height: 40px;
}

@media (max-width: 768px) {
  .page-title {
    margin-bottom: 16px;
    font-size: 24px;
  }

  .welcome-title {
    font-size: 21px;
  }
}
</style> 
