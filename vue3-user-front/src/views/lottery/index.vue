<template>
  <div class="lottery-page">
    <!-- 标题栏 -->
    <div class="page-header">
      <h1 class="page-title">🎉 幸运大抽奖</h1>
      <p class="page-subtitle">消耗 100 积分，赢取丰厚奖励！</p>
    </div>

    <!-- 统计信息卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
              <el-icon><Coin /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">我的积分</div>
              <div class="stat-value">{{ userPoints }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
              <el-icon><Calendar /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">今日剩余次数</div>
              <div class="stat-value">{{ remainingCount }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
              <el-icon><Trophy /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">累计抽奖</div>
              <div class="stat-value">{{ statistics.totalDrawCount || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%)">
              <el-icon><Star /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">累计中奖</div>
              <div class="stat-value">{{ statistics.totalWinCount || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 九宫格抽奖 -->
    <el-card shadow="hover" class="lottery-card">
      <div class="lottery-grid-wrapper">
        <div class="lottery-grid">
          <div 
            v-for="(item, index) in gridItems" 
            :key="index"
            class="grid-item"
            :class="{ 
              'active': currentIndex === index,
              'center': index === 4,
              'winner': winnerIndex === index && showWinner
            }">
            <!-- 中心抽奖按钮 -->
            <div v-if="index === 4" class="draw-button-wrapper" @click="testClick">
              <el-button 
                type="danger" 
                :loading="drawing"
                :disabled="!canDraw"
                size="large"
                class="draw-btn"
                @click.stop="handleDraw"
                :title="!canDraw ? getDrawButtonTip : ''">
                <span v-if="!drawing">{{ canDraw ? '立即抽奖' : getDrawButtonText }}</span>
                <span v-else>抽奖中...</span>
              </el-button>
              <div class="cost-tips">消耗 100 积分</div>
            </div>
            <!-- 奖品格子 -->
            <div v-else class="prize-box">
              <div class="prize-icon">
                {{ getPrizeIcon(item.prizeLevel) }}
              </div>
              <div class="prize-name">{{ item.prizeName }}</div>
              <div class="prize-points">{{ item.prizePoints }} 积分</div>
              <el-tag 
                :type="getPrizeLevelType(item.prizeLevel)" 
                size="small"
                class="prize-tag">
                {{ (item.probability * 100).toFixed(1) }}%
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 抽奖规则 -->
    <el-card shadow="hover" class="rules-card">
      <template #header>
        <h3>📋 抽奖规则</h3>
      </template>
      <div class="rules-content" v-html="sanitizedRules"></div>
    </el-card>

    <!-- 我的记录 -->
    <el-card shadow="hover" class="records-card">
      <template #header>
        <div class="records-header">
          <h3>🎯 我的抽奖记录</h3>
          <el-button type="primary" size="small" @click="loadRecords">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>
      
      <el-table :data="recordList" stripe>
        <el-table-column prop="prizeName" label="奖品" width="150" />
        <el-table-column label="奖品等级" width="120">
          <template #default="{ row }">
            <el-tag :type="getPrizeLevelType(row.prizeLevel)" size="small">
              {{ getPrizeLevelName(row.prizeLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="prizePoints" label="获得积分" width="120" align="center">
          <template #default="{ row }">
            <span style="color: #67c23a; font-weight: bold">+{{ row.prizePoints }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="strategyType" label="抽奖策略" width="150" />
        <el-table-column prop="createTime" label="抽奖时间" width="180" />
      </el-table>
      
      <el-pagination
        v-model:current-page="recordQuery.page"
        v-model:page-size="recordQuery.size"
        :total="recordTotal"
        :page-sizes="[5, 10, 20]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadRecords"
        @current-change="loadRecords"
        style="margin-top: 20px; justify-content: flex-end" />
    </el-card>

    <!-- 中奖弹窗 -->
    <el-dialog 
      v-model="resultDialog" 
      :title="drawResult?.prizeLevel === 8 ? '很遗憾' : '恭喜中奖！'"
      width="500px"
      :show-close="false"
      center>
      <div class="result-content">
        <div v-if="drawResult?.prizeLevel === 8" class="result-fail">
          <div class="result-icon">😔</div>
          <div class="result-text">{{ drawResult.prizeName }}</div>
          <div class="result-tips">再接再厉，下次一定中！</div>
        </div>
        <div v-else class="result-success">
          <div class="result-icon">🎉</div>
          <div class="result-prize-name">{{ drawResult.prizeName }}</div>
          <div class="result-prize-points">
            <el-icon><Coin /></el-icon>
            +{{ drawResult.prizePoints }} 积分
          </div>
          <div class="result-tips">奖励已发放到您的账户</div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="handleContinue" size="large">
          {{ canDraw ? '再抽一次' : '确定' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Coin, 
  Calendar, 
  Trophy, 
  Star, 
  Refresh 
} from '@element-plus/icons-vue'
import { lotteryApi } from '@/api/lottery'
import pointsApi from '@/api/points'
import { sanitizeHtml } from '@/utils/markdown'

const drawing = ref(false)
const resultDialog = ref(false)
const drawResult = ref(null)

const prizeList = ref([])
const gridItems = ref([])
const remainingCount = ref(0)
const statistics = ref({})
const rules = ref('')
const recordList = ref([])
const recordTotal = ref(0)
const pointsBalance = ref(null)
const sanitizedRules = computed(() => sanitizeHtml(rules.value || ''))

// 九宫格动画相关
const currentIndex = ref(-1)
const winnerIndex = ref(-1)
const showWinner = ref(false)

const recordQuery = reactive({
  page: 1,
  size: 10
})

// 用户积分
const userPoints = computed(() => pointsBalance.value?.totalPoints || 0)

// 是否可以抽奖
const canDraw = computed(() => {
  return remainingCount.value > 0 && userPoints.value >= 100 && !drawing.value
})

// 抽奖按钮文本
const getDrawButtonText = computed(() => {
  if (remainingCount.value <= 0) return '次数不足'
  if (userPoints.value < 100) return '积分不足'
  return '立即抽奖'
})

// 抽奖按钮提示
const getDrawButtonTip = computed(() => {
  if (remainingCount.value <= 0) return '今日抽奖次数已用完'
  if (userPoints.value < 100) return `当前积分：${userPoints.value}，需要 100 积分`
  return ''
})

// 加载初始数据
const loadInitialData = async () => {
  try {
    await Promise.all([
      loadPrizeList(),
      loadRemainingCount(),
      loadStatistics(),
      loadRules(),
      loadRecords(),
      loadPointsBalance()
    ])
  } catch (error) {
    console.error('加载初始数据失败', error)
  }
}

// 加载积分余额
const loadPointsBalance = async () => {
  try {
    console.log('开始加载积分余额...')
    pointsBalance.value = await pointsApi.getPointsBalance()
    console.log('积分余额加载成功:', pointsBalance.value)
  } catch (error) {
    console.error('加载积分余额失败', error)
  }
}

// 加载奖品列表
const loadPrizeList = async () => {
  try {
    const prizes = await lotteryApi.getPrizeList()
    prizeList.value = prizes
    
    // 构建九宫格数据（取前8个奖品，中间是抽奖按钮）
    if (prizes.length > 0) {
      const items = []
      // 取前8个奖品填充宫格（0-3, 5-8位置，4是中心按钮）
      for (let i = 0; i < Math.min(8, prizes.length); i++) {
        items.push(prizes[i])
      }
      // 如果奖品不足8个，重复填充
      while (items.length < 8) {
        items.push(...prizes.slice(0, Math.min(8 - items.length, prizes.length)))
      }
      // 插入中心位置
      items.splice(4, 0, null) // 第5个位置是抽奖按钮
      gridItems.value = items
    }
  } catch (error) {
    ElMessage.error(error.message || '加载奖品列表失败')
  }
}

// 加载剩余次数
const loadRemainingCount = async () => {
  try {
    console.log('开始加载剩余次数...')
    remainingCount.value = await lotteryApi.getRemainingCount()
    console.log('剩余次数加载成功:', remainingCount.value)
  } catch (error) {
    console.error('加载剩余次数失败', error)
  }
}

// 加载统计信息
const loadStatistics = async () => {
  try {
    statistics.value = await lotteryApi.getStatistics()
  } catch (error) {
    console.error('加载统计信息失败', error)
  }
}

// 加载规则
const loadRules = async () => {
  try {
    rules.value = await lotteryApi.getRules()
  } catch (error) {
    console.error('加载规则失败', error)
    rules.value = `
      <ul>
        <li>每次抽奖消耗 100 积分</li>
        <li>每日最多抽奖 10 次</li>
        <li>中奖后积分立即到账</li>
        <li>连续 20 次未中奖，必中三等奖及以上</li>
        <li>奖品库存有限，先到先得</li>
      </ul>
    `
  }
}

// 加载抽奖记录
const loadRecords = async () => {
  try {
    const res = await lotteryApi.getDrawRecords(recordQuery)
    recordList.value = res.records
    recordTotal.value = res.total
  } catch (error) {
    ElMessage.error(error.message || '加载记录失败')
  }
}

// 执行抽奖
const handleDraw = async () => {
  console.log('抽奖按钮被点击')
  console.log('canDraw:', canDraw.value)
  console.log('userPoints:', userPoints.value)
  console.log('remainingCount:', remainingCount.value)
  console.log('drawing:', drawing.value)
  
  if (!canDraw.value) {
    console.warn('抽奖条件不满足')
    ElMessage.warning('今日抽奖次数已用完或积分不足')
    return
  }
  
  drawing.value = true
  currentIndex.value = -1
  showWinner.value = false
  
  try {
    console.log('开始调用抽奖 API')
    // 调用抽奖接口
    drawResult.value = await lotteryApi.draw({ strategyType: 'ALIAS_METHOD' })
    console.log('抽奖结果:', drawResult.value)
    
    // 找到中奖奖品在九宫格中的位置（排除中心按钮）
    const targetPrizeId = drawResult.value.prizeId
    let targetIndex = gridItems.value.findIndex((item, index) => 
      index !== 4 && item && item.prizeId === targetPrizeId
    )
    
    // 如果没找到，随机一个位置
    if (targetIndex === -1) {
      const validIndexes = [0, 1, 2, 3, 5, 6, 7, 8]
      targetIndex = validIndexes[Math.floor(Math.random() * validIndexes.length)]
    }
    
    // 开始跑灯动画
    await runLotteryAnimation(targetIndex)
    
    // 显示中奖效果
    winnerIndex.value = targetIndex
    showWinner.value = true
    
    // 延迟显示弹窗
    setTimeout(() => {
      drawing.value = false
      resultDialog.value = true
      currentIndex.value = -1
      
      // 刷新数据
      loadRemainingCount()
      loadStatistics()
      loadRecords()
      loadPointsBalance()
    }, 1000)
    
  } catch (error) {
    console.error('抽奖失败:', error)
    drawing.value = false
    currentIndex.value = -1
    ElMessage.error(error.message || '抽奖失败')
  }
}

// 用于调试 - 打印按钮状态
const debugButtonState = () => {
  console.log('=== 按钮状态调试 ===')
  console.log('userPoints:', userPoints.value)
  console.log('remainingCount:', remainingCount.value)
  console.log('drawing:', drawing.value)
  console.log('canDraw:', canDraw.value)
  console.log('pointsBalance:', pointsBalance.value)
}
window.debugLottery = debugButtonState

// 测试点击事件
const testClick = () => {
  console.log('wrapper div 被点击了')
}

// 九宫格跑灯动画
const runLotteryAnimation = (targetIndex) => {
  return new Promise((resolve) => {
    const path = [0, 1, 2, 5, 8, 7, 6, 3] // 顺时针路径
    let currentStep = 0
    const totalSteps = 3 * 8 + path.indexOf(targetIndex) + 1 // 转3圈后停在目标位置
    const initialSpeed = 100
    const finalSpeed = 300
    
    const animate = () => {
      if (currentStep >= totalSteps) {
        resolve()
        return
      }
      
      currentIndex.value = path[currentStep % 8]
      currentStep++
      
      // 速度逐渐减慢
      const progress = currentStep / totalSteps
      const speed = initialSpeed + (finalSpeed - initialSpeed) * Math.pow(progress, 2)
      
      setTimeout(animate, speed)
    }
    
    animate()
  })
}

// 继续抽奖
const handleContinue = () => {
  resultDialog.value = false
  showWinner.value = false
  winnerIndex.value = -1
  if (canDraw.value) {
    // 可以继续抽奖
  }
}

// 获取奖品图标
const getPrizeIcon = (level) => {
  const icons = {
    1: '🏆', // 特等奖
    2: '🥇', // 一等奖
    3: '🥈', // 二等奖
    4: '🥉', // 三等奖
    5: '🎁', // 四等奖
    6: '🎀', // 五等奖
    7: '🎈', // 六等奖
    8: '😢'  // 未中奖
  }
  return icons[level] || '🎁'
}

// 获取奖品等级名称
const getPrizeLevelName = (level) => {
  const names = ['', '特等奖', '一等奖', '二等奖', '三等奖', '四等奖', '五等奖', '六等奖', '未中奖']
  return names[level] || '未知'
}

// 获取奖品等级类型
const getPrizeLevelType = (level) => {
  if (level === 1) return 'danger'
  if (level <= 3) return 'warning'
  if (level <= 7) return 'success'
  return 'info'
}

onMounted(() => {
  console.log('=== 抽奖页面已挂载 ===')
  loadInitialData()
  
  // 3秒后自动打印状态
  setTimeout(() => {
    console.log('=== 3秒后自动状态检查 ===')
    console.log('userPoints:', userPoints.value)
    console.log('remainingCount:', remainingCount.value)
    console.log('canDraw:', canDraw.value)
    console.log('drawing:', drawing.value)
    console.log('pointsBalance:', pointsBalance.value)
    console.log('gridItems length:', gridItems.value.length)
  }, 3000)
})
</script>

<style scoped lang="scss">
.lottery-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  
  .page-header {
    text-align: center;
    margin-bottom: 30px;
    
    .page-title {
      font-size: 36px;
      font-weight: bold;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      margin-bottom: 10px;
    }
    
    .page-subtitle {
      font-size: 18px;
      color: #909399;
    }
  }
  
  .stat-cards {
    margin-bottom: 30px;
    
    .stat-card {
      .stat-content {
        display: flex;
        align-items: center;
        gap: 15px;
        
        .stat-icon {
          width: 60px;
          height: 60px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: white;
          font-size: 28px;
        }
        
        .stat-info {
          flex: 1;
          
          .stat-label {
            font-size: 14px;
            color: #909399;
            margin-bottom: 5px;
          }
          
          .stat-value {
            font-size: 28px;
            font-weight: bold;
            color: #303133;
          }
        }
      }
    }
  }
  
  .lottery-card {
    margin-bottom: 30px;
    
    .lottery-grid-wrapper {
      display: flex;
      justify-content: center;
      padding: 40px 20px;
      
      .lottery-grid {
        display: grid;
        grid-template-columns: repeat(3, 140px);
        grid-template-rows: repeat(3, 140px);
        gap: 10px;
        
        .grid-item {
          background: linear-gradient(145deg, #ffffff, #f5f5f5);
          border: 3px solid #e0e0e0;
          border-radius: 16px;
          display: flex;
          align-items: center;
          justify-content: center;
          cursor: pointer;
          transition: all 0.3s ease;
          position: relative;
          overflow: hidden;
          
          &::before {
            content: '';
            position: absolute;
            inset: 0;
            background: linear-gradient(145deg, transparent, rgba(255, 255, 255, 0.1));
            opacity: 0;
            transition: opacity 0.3s ease;
            pointer-events: none; /* 不阻挡点击事件 */
          }
          
          &:hover:not(.center) {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
            
            &::before {
              opacity: 1;
            }
          }
          
          &.active {
            border-color: #409eff;
            background: linear-gradient(145deg, #e3f2ff, #b3d9ff);
            box-shadow: 0 0 30px rgba(64, 158, 255, 0.6);
            transform: scale(1.05);
          }
          
          &.winner {
            border-color: #f56c6c;
            background: linear-gradient(145deg, #ffe5e5, #ffcccc);
            box-shadow: 0 0 40px rgba(245, 108, 108, 0.8);
            animation: winner-pulse 0.6s ease-in-out infinite;
          }
          
          &.center {
            cursor: default;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            border-color: #667eea;
            
            &:hover {
              transform: none;
              box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
            }
          }
          
          .draw-button-wrapper {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 8px;
            position: relative;
            z-index: 10; /* 确保按钮在最上层 */
            width: 100%;
            height: 100%;
            justify-content: center;
            
            .draw-btn {
              font-size: 16px;
              font-weight: bold;
              padding: 12px 24px;
              height: auto;
              z-index: 11;
              position: relative;
            }
            
            .cost-tips {
              font-size: 12px;
              color: rgba(255, 255, 255, 0.9);
            }
          }
          
          .prize-box {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 10px;
            text-align: center;
            width: 100%;
            
            .prize-icon {
              font-size: 40px;
              margin-bottom: 8px;
            }
            
            .prize-name {
              font-size: 13px;
              font-weight: bold;
              color: #303133;
              margin-bottom: 4px;
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
              max-width: 100%;
            }
            
            .prize-points {
              font-size: 14px;
              font-weight: bold;
              color: #67c23a;
              margin-bottom: 4px;
            }
            
            .prize-tag {
              font-size: 11px;
            }
          }
        }
      }
    }
  }
  
  .rules-card {
    margin-bottom: 30px;
    
    h3 {
      margin: 0;
      font-size: 18px;
    }
    
    .rules-content {
      line-height: 1.8;
      color: #606266;
      
      :deep(ul) {
        padding-left: 20px;
        
        li {
          margin-bottom: 10px;
        }
      }
    }
  }
  
  @media (max-width: 768px) {
    padding: 15px;
    
    .page-header {
      .page-title {
        font-size: 28px;
      }
      
      .page-subtitle {
        font-size: 14px;
      }
    }
    
    .stat-cards {
      margin-bottom: 20px;
      
      .stat-card .stat-content {
        .stat-icon {
          width: 50px;
          height: 50px;
          font-size: 24px;
        }
        
        .stat-info {
          .stat-label {
            font-size: 12px;
          }
          
          .stat-value {
            font-size: 22px;
          }
        }
      }
    }
    
    .lottery-grid-wrapper {
      padding: 20px 10px !important;
      
      .lottery-grid {
        grid-template-columns: repeat(3, 100px) !important;
        grid-template-rows: repeat(3, 100px) !important;
        gap: 8px !important;
        
        .grid-item {
          border-radius: 12px;
          
          .prize-box {
            padding: 8px;
            
            .prize-icon {
              font-size: 30px;
              margin-bottom: 4px;
            }
            
            .prize-name {
              font-size: 11px;
            }
            
            .prize-points {
              font-size: 12px;
            }
          }
          
          .draw-button-wrapper {
            .draw-btn {
              font-size: 14px;
              padding: 10px 20px;
            }
            
            .cost-tips {
              font-size: 10px;
            }
          }
        }
      }
    }
  }
  
  .records-card {
    .records-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      h3 {
        margin: 0;
        font-size: 18px;
      }
    }
    
    :deep(.el-pagination) {
      display: flex;
    }
  }
}

.result-content {
  padding: 20px;
  text-align: center;
  
  .result-icon {
    font-size: 80px;
    margin-bottom: 20px;
  }
  
  .result-fail {
    .result-text {
      font-size: 24px;
      font-weight: bold;
      color: #909399;
      margin-bottom: 10px;
    }
    
    .result-tips {
      font-size: 14px;
      color: #c0c4cc;
    }
  }
  
  .result-success {
    .result-prize-name {
      font-size: 28px;
      font-weight: bold;
      color: #303133;
      margin-bottom: 15px;
    }
    
    .result-prize-points {
      font-size: 36px;
      font-weight: bold;
      color: #67c23a;
      margin-bottom: 15px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
    }
    
    .result-tips {
      font-size: 14px;
      color: #909399;
    }
  }
}

@keyframes winner-pulse {
  0%, 100% {
    transform: scale(1.05);
    box-shadow: 0 0 40px rgba(245, 108, 108, 0.8);
  }
  50% {
    transform: scale(1.1);
    box-shadow: 0 0 50px rgba(245, 108, 108, 1);
  }
}
</style>

