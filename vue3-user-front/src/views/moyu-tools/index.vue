<template>
  <div class="moyu-tools">
    <!-- 头部标题区域 -->
    <div class="tools-header">
      <div class="header-content">
        <div class="title-section">
          <h1 class="main-title">
            <el-icon class="title-icon"><Coffee /></el-icon>
            摸鱼工具箱
          </h1>
          <p class="subtitle">工作间隙的轻松时光，了解世界热点动态</p>
        </div>
      </div>
    </div>

    <!-- 工具卡片区域 -->
    <div class="tools-grid">
      <div class="tool-card hot-topics-tool" @click="navigateToTool('hot-topics')">
        <div class="card-header">
          <div class="tool-icon">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="tool-badge">热门</div>
        </div>
        <div class="card-content">
          <h3 class="tool-title">今日热榜</h3>
          <p class="tool-description">
            汇聚各大平台热门话题，一站式浏览微博、知乎、抖音等27个平台的实时热榜
          </p>
          <div class="tool-features">
            <span class="feature-tag">实时更新</span>
            <span class="feature-tag">多平台</span>
            <span class="feature-tag">分类浏览</span>
            <span class="feature-tag">一键跳转</span>
          </div>
        </div>
        <div class="card-footer">
          <el-button type="primary" size="large" class="tool-button">
            <el-icon><Right /></el-icon>
            立即使用
          </el-button>
        </div>
      </div>

      <!-- 时薪计算器工具卡片 -->
      <div class="tool-card salary-calculator-tool" @click="navigateToTool('salary-calculator')">
        <div class="card-header">
          <div class="tool-icon">
            <el-icon><Money /></el-icon>
          </div>
          <div class="tool-badge">激励</div>
        </div>
        <div class="card-content">
          <h3 class="tool-title">时薪计算器</h3>
          <p class="tool-description">
            实时计算工作收入，追踪每日工作时长，让每分钟的努力都有意义
          </p>
          <div class="tool-features">
            <span class="feature-tag">实时计算</span>
            <span class="feature-tag">收入统计</span>
            <span class="feature-tag">工作跟踪</span>
            <span class="feature-tag">激励展示</span>
          </div>
        </div>
        <div class="card-footer">
          <el-button type="success" size="large" class="tool-button">
            <el-icon><Right /></el-icon>
            开始计算
          </el-button>
        </div>
      </div>

      <!-- 程序员日历工具卡片 -->
      <div class="tool-card calendar-tool" @click="navigateToTool('calendar')">
        <div class="card-header">
          <div class="tool-icon">
            <el-icon><Calendar /></el-icon>
          </div>
          <div class="tool-badge">精选</div>
        </div>
        <div class="card-content">
          <h3 class="tool-title">程序员日历</h3>
          <p class="tool-description">
            发现属于程序员的特殊时刻，包含技术纪念日、开源节日、程序员节日等
          </p>
          <div class="tool-features">
            <span class="feature-tag">月历视图</span>
            <span class="feature-tag">今日推荐</span>
            <span class="feature-tag">事件详情</span>
            <span class="feature-tag">类型筛选</span>
          </div>
        </div>
        <div class="card-footer">
          <el-button type="info" size="large" class="tool-button">
            <el-icon><Right /></el-icon>
            查看日历
          </el-button>
        </div>
      </div>

      <!-- 每日内容工具卡片 -->
      <div class="tool-card content-tool" @click="navigateToTool('daily-content')">
        <div class="card-header">
          <div class="tool-icon">
            <el-icon><Reading /></el-icon>
          </div>
          <div class="tool-badge">学习</div>
        </div>
        <div class="card-content">
          <h3 class="tool-title">每日内容</h3>
          <p class="tool-description">
            每天一点小知识，包含编程格言、技术小贴士、代码片段、历史回顾等
          </p>
          <div class="tool-features">
            <span class="feature-tag">今日推荐</span>
            <span class="feature-tag">分类浏览</span>
            <span class="feature-tag">点赞收藏</span>
            <span class="feature-tag">知识积累</span>
          </div>
        </div>
        <div class="card-footer">
          <el-button type="warning" size="large" class="tool-button">
            <el-icon><Right /></el-icon>
            开始学习
          </el-button>
        </div>
      </div>

      <!-- Bug商店工具卡片 -->
      <div class="tool-card bug-store-tool" @click="navigateToTool('bug-store')">
        <div class="card-header">
          <div class="tool-icon">
            <el-icon><Notification /></el-icon>
          </div>
          <div class="tool-badge">发现</div>
        </div>
        <div class="card-content">
          <h3 class="tool-title">Bug 商店</h3>
          <p class="tool-description">
            随机发现经典Bug，学习分析思路和解决方案，提升调试技能和经验
          </p>
          <div class="tool-features">
            <span class="feature-tag">随机推荐</span>
            <span class="feature-tag">经典Bug</span>
            <span class="feature-tag">解决方案</span>
            <span class="feature-tag">技能提升</span>
          </div>
        </div>
        <div class="card-footer">
          <el-button type="danger" size="large" class="tool-button">
            <el-icon><Right /></el-icon>
            来一发Bug
          </el-button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { 
  Coffee, TrendCharts, Right, Money, Calendar, Reading, Notification
} from '@element-plus/icons-vue'

const router = useRouter()

// 导航到具体工具
const navigateToTool = (toolName) => {
  router.push(`/moyu-tools/${toolName}`)
}
</script>

<style scoped>
.moyu-tools {
  min-height: 100vh;
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 50%, #fecfef 100%);
  position: relative;
  overflow-x: hidden;
}

.moyu-tools::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 20% 30%, rgba(255, 255, 255, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 70%, rgba(255, 255, 255, 0.1) 0%, transparent 50%);
  pointer-events: none;
}

.moyu-tools > * {
  position: relative;
  z-index: 1;
}

/* 头部区域 */
.tools-header {
  padding: 80px 20px 60px;
  text-align: center;
  color: white;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
}

.main-title {
  font-size: 3.5rem;
  font-weight: 700;
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: linear-gradient(45deg, #fff, #ffe0e6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.title-icon {
  font-size: 3rem;
  color: #ff6b6b;
}

.subtitle {
  font-size: 1.25rem;
  opacity: 0.9;
  margin: 0;
  font-weight: 300;
}

/* 工具卡片区域 */
.tools-grid {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px 80px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 30px;
}

.tool-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 30px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  position: relative;
  overflow: hidden;
}

.tool-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #ff9a9e, #fecfef);
  transform: scaleX(0);
  transition: transform 0.3s ease;
}

.tool-card:hover::before {
  transform: scaleX(1);
}

.tool-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 30px 60px rgba(0, 0, 0, 0.15);
}

.hot-topics-tool:hover {
  background: linear-gradient(135deg, rgba(255, 107, 107, 0.05), rgba(255, 154, 158, 0.05));
}

.salary-calculator-tool:hover {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.05), rgba(16, 185, 129, 0.05));
}

.calendar-tool:hover {
  background: linear-gradient(135deg, rgba(116, 185, 255, 0.05), rgba(108, 92, 231, 0.05));
}

.content-tool:hover {
  background: linear-gradient(135deg, rgba(253, 203, 110, 0.05), rgba(255, 193, 7, 0.05));
}

.bug-store-tool:hover {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.05), rgba(220, 38, 38, 0.05));
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.tool-icon {
  width: 60px;
  height: 60px;
  border-radius: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  color: white;
}

.hot-topics-tool .tool-icon {
  background: linear-gradient(135deg, #ff6b6b, #ff8e8e);
}

.salary-calculator-tool .tool-icon {
  background: linear-gradient(135deg, #22c55e, #10b981);
}

.calendar-tool .tool-icon {
  background: linear-gradient(135deg, #74b9ff, #6c5ce7);
}

.content-tool .tool-icon {
  background: linear-gradient(135deg, #fdcb6e, #ffc107);
}

.bug-store-tool .tool-icon {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}

.coming-soon .tool-icon {
  background: linear-gradient(135deg, #94a3b8, #cbd5e1);
}

.tool-badge {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
  color: white;
}

.hot-topics-tool .tool-badge {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}

.salary-calculator-tool .tool-badge {
  background: linear-gradient(135deg, #059669, #047857);
}

.calendar-tool .tool-badge {
  background: linear-gradient(135deg, #4a90e2, #357abd);
}

.content-tool .tool-badge {
  background: linear-gradient(135deg, #f39c12, #e67e22);
}

.bug-store-tool .tool-badge {
  background: linear-gradient(135deg, #dc2626, #b91c1c);
}

.coming-soon .tool-badge {
  background: linear-gradient(135deg, #64748b, #475569);
}

.card-content {
  margin-bottom: 30px;
}

.tool-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 12px 0;
}

.tool-description {
  color: #6b7280;
  line-height: 1.6;
  margin: 0 0 20px 0;
  font-size: 0.95rem;
}

.tool-features {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.feature-tag {
  padding: 4px 12px;
  background: rgba(107, 114, 128, 0.1);
  border-radius: 12px;
  font-size: 0.8rem;
  color: #374151;
  font-weight: 500;
}

.card-footer {
  text-align: center;
}

.tool-button {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-weight: 600;
  font-size: 1rem;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .main-title {
    font-size: 2.5rem;
    flex-direction: column;
    gap: 8px;
  }

  .title-icon {
    font-size: 2rem;
  }

  .tools-grid {
    grid-template-columns: 1fr;
    padding: 0 15px 60px;
  }

  .tool-card {
    padding: 24px;
  }
}

@media (max-width: 480px) {
  .tools-header {
    padding: 60px 15px 40px;
  }

  .main-title {
    font-size: 2rem;
  }

  .tool-card {
    padding: 20px;
  }
}
</style>
