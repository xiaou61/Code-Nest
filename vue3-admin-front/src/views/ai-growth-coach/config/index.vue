<template>
  <div class="ai-growth-coach-config-page">
    <el-card shadow="never" class="hero-card">
      <div class="hero-header">
        <div>
          <h2>AI成长教练策略配置</h2>
          <p>调整快照过期时间、推荐问题模板以及场景开关。</p>
        </div>
        <div class="hero-actions">
          <el-button :loading="loading" @click="loadConfigs">刷新</el-button>
          <el-button type="primary" :loading="saving" @click="saveConfigs">保存配置</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table :data="configs" v-loading="loading" border>
        <el-table-column prop="configKey" label="配置键" min-width="220" />
        <el-table-column label="配置值" min-width="280">
          <template #default="{ row }">
            <el-input v-model="row.configValue" type="textarea" :rows="2" />
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="220">
          <template #default="{ row }">
            <el-input v-model="row.remark" />
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="120">
          <template #default="{ row }">
            <el-select v-model="row.status" style="width: 100%">
              <el-option label="启用" value="ENABLED" />
              <el-option label="停用" value="DISABLED" />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import aiGrowthCoachAdminApi from '@/api/aiGrowthCoach'

const loading = ref(false)
const saving = ref(false)
const configs = ref([])

const loadConfigs = async () => {
  loading.value = true
  try {
    const res = await aiGrowthCoachAdminApi.getConfigs()
    configs.value = Array.isArray(res) ? res : []
  } catch (error) {
    console.error('加载AI成长教练配置失败', error)
    ElMessage.error('加载AI成长教练配置失败')
  } finally {
    loading.value = false
  }
}

const saveConfigs = async () => {
  saving.value = true
  try {
    await aiGrowthCoachAdminApi.updateConfigs(configs.value.map(item => ({
      configKey: item.configKey,
      configValue: item.configValue,
      remark: item.remark,
      status: item.status
    })))
    ElMessage.success('配置已保存')
    await loadConfigs()
  } catch (error) {
    console.error('保存AI成长教练配置失败', error)
    ElMessage.error('保存AI成长教练配置失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadConfigs()
})
</script>

<style scoped>
.ai-growth-coach-config-page {
  padding: 20px;
}

.hero-card {
  margin-bottom: 16px;
}

.hero-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.hero-header h2 {
  margin: 0 0 8px;
}

.hero-header p {
  margin: 0;
  color: #909399;
}

.hero-actions {
  display: flex;
  gap: 8px;
}
</style>
