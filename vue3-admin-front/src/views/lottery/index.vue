<template>
  <CnPage class="lottery-management-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="抽奖系统管理"
      description="统一维护奖品配置、实时监控、抽奖记录、数据统计、调整历史和风险用户。"
      eyebrow="Lottery Operations"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">配置</CnStatusTag>
        <CnStatusTag type="success">监控</CnStatusTag>
        <CnStatusTag type="warning">风控</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="primary" :icon="Refresh" :loading="cacheLoading" @click="refreshCache">刷新缓存</el-button>
        <el-dropdown @command="handleEmergencyAction">
          <el-button type="danger">
            应急操作<el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="circuit-break">
                <el-icon><Warning /></el-icon>
                手动熔断
              </el-dropdown-item>
              <el-dropdown-item command="resume">
                <el-icon><Check /></el-icon>
                恢复服务
              </el-dropdown-item>
              <el-dropdown-item command="enable-degradation">
                <el-icon><Bottom /></el-icon>
                启用降级
              </el-dropdown-item>
              <el-dropdown-item command="disable-degradation">
                <el-icon><Top /></el-icon>
                禁用降级
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
    </CnPageHeader>

    <CnSection title="运营控制台" description="切换不同管理视图，完成抽奖系统的配置、监控和风控操作。" divided>
      <el-tabs v-model="activeTab" class="main-tabs">
        <el-tab-pane label="奖品配置" name="config">
          <PrizeConfig ref="prizeConfigRef" />
        </el-tab-pane>

        <el-tab-pane label="实时监控" name="monitor">
          <RealtimeMonitor ref="realtimeMonitorRef" />
        </el-tab-pane>

        <el-tab-pane label="抽奖记录" name="records">
          <DrawRecords ref="drawRecordsRef" />
        </el-tab-pane>

        <el-tab-pane label="数据统计" name="statistics">
          <DataStatistics ref="dataStatisticsRef" />
        </el-tab-pane>

        <el-tab-pane label="调整历史" name="history">
          <AdjustHistory ref="adjustHistoryRef" />
        </el-tab-pane>

        <el-tab-pane label="风险用户" name="risk">
          <RiskUsers ref="riskUsersRef" />
        </el-tab-pane>
      </el-tabs>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Bottom, Check, Refresh, Top, Warning } from '@element-plus/icons-vue'
import { lotteryAdminApi } from '@/api/lotteryAdmin'
import { CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem } from '@/design-system'

import AdjustHistory from './components/AdjustHistory.vue'
import DataStatistics from './components/DataStatistics.vue'
import DrawRecords from './components/DrawRecords.vue'
import PrizeConfig from './components/PrizeConfig.vue'
import RealtimeMonitor from './components/RealtimeMonitor.vue'
import RiskUsers from './components/RiskUsers.vue'

type EmergencyCommand = 'circuit-break' | 'resume' | 'enable-degradation' | 'disable-degradation'

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '抽奖系统' }, { label: '运营控制台' }]

const activeTab = ref('config')
const cacheLoading = ref(false)

const refreshCache = async () => {
  cacheLoading.value = true
  try {
    await lotteryAdminApi.refreshCache()
    ElMessage.success('缓存刷新成功')
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '刷新缓存失败')
  } finally {
    cacheLoading.value = false
  }
}

const handleEmergencyAction = async (command: EmergencyCommand) => {
  try {
    if (command === 'circuit-break') {
      const { value } = await ElMessageBox.prompt('请输入熔断原因', '手动熔断', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        inputPattern: /.+/,
        inputErrorMessage: '请输入熔断原因'
      })
      await lotteryAdminApi.manualCircuitBreak(value)
      ElMessage.success('熔断成功')
    } else if (command === 'resume') {
      await ElMessageBox.confirm('确认要恢复服务吗？', '恢复服务', {
        type: 'warning'
      })
      await lotteryAdminApi.resumeService()
      ElMessage.success('服务已恢复')
    } else if (command === 'enable-degradation') {
      await ElMessageBox.confirm('确认要启用降级模式吗？', '启用降级', {
        type: 'warning'
      })
      await lotteryAdminApi.enableDegradation()
      ElMessage.success('降级模式已启用')
    } else if (command === 'disable-degradation') {
      await lotteryAdminApi.disableDegradation()
      ElMessage.success('降级模式已禁用')
    }
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}
</script>

<style scoped>
.lottery-management-page {
  min-height: 100%;
}

.main-tabs :deep(.el-tabs__header) {
  margin-bottom: var(--cn-space-4);
}

.main-tabs :deep(.el-tabs__content) {
  overflow: visible;
}
</style>
