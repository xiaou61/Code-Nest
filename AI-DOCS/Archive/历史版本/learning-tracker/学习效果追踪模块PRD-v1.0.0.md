# 学习效果追踪模块 PRD v1.0.0

## 版本信息
- **版本号**: v1.0.0
- **发布日期**: 2025年12月
- **依赖模块**: xiaou-interview
- **主要功能**: 遗忘曲线复习、掌握度标记、学习热力图

## 1. 产品概述

### 1.1 背景与痛点
当前面试题库模块已具备基础的学习功能，但存在以下问题：
- **学了就忘**：用户刷完题目后没有科学的复习提醒，容易遗忘
- **不知道学到哪**：缺乏对学习进度和掌握程度的量化反馈
- **学习无规划**：用户不知道哪些题目需要重点复习
- **缺乏成就感**：看不到自己的学习轨迹和进步

### 1.2 核心价值
- **科学复习**：基于艾宾浩斯遗忘曲线，在最佳时机提醒用户复习
- **精准定位**：通过掌握度标记，帮助用户识别薄弱环节
- **可视化激励**：学习热力图让用户直观感受学习成果

### 1.3 功能定位
本功能**仅在做题模式下启用**，原因：
- 做题模式强调主动回忆，更符合科学学习方法
- 背题模式用于快速浏览复习，不需要评估掌握度
- 区分学习场景，避免功能冗余

## 2. 功能详细设计

### 2.1 掌握度标记

#### 2.1.1 功能说明
在做题模式下，用户查看答案后，需要对自己的掌握程度进行自评标记。

#### 2.1.2 掌握度等级

| 等级 | 名称 | 图标 | 颜色 | 说明 | 复习间隔基数 |
|------|------|------|------|------|-------------|
| 1 | 不会 | ❌ | #F56C6C (红色) | 完全不懂，需要重点学习 | 1天 |
| 2 | 模糊 | 🤔 | #E6A23C (橙色) | 有印象但记不清细节 | 2天 |
| 3 | 熟悉 | 😊 | #409EFF (蓝色) | 基本掌握，偶尔会忘 | 4天 |
| 4 | 已掌握 | ✅ | #67C23A (绿色) | 完全掌握，非常自信 | 7天 |

#### 2.1.3 交互流程
```
用户在做题模式下查看题目
    ↓
点击"查看答案"按钮
    ↓
显示参考答案
    ↓
底部浮现掌握度评估卡片
    ↓
用户点击选择掌握度等级
    ↓
系统记录并计算下次复习时间
    ↓
显示标记成功提示（含下次复习时间）
    ↓
可选择继续下一题或查看复习计划
```

#### 2.1.4 UI设计（做题模式答案区域下方）
```
┌─────────────────────────────────────────────────────────┐
│  📊 这道题你掌握得如何？                                  │
├─────────────────────────────────────────────────────────┤
│                                                         │
│   ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐              │
│   │  ❌  │  │  🤔  │  │  😊  │  │  ✅  │              │
│   │ 不会 │  │ 模糊 │  │ 熟悉 │  │已掌握│              │
│   └──────┘  └──────┘  └──────┘  └──────┘              │
│                                                         │
│   上次标记：熟悉 (2天前)    已复习：3次                    │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

#### 2.1.5 标记规则
- 每道题每个用户只保留最新的掌握度标记
- 标记时自动记录学习时间，用于热力图统计
- 首次标记与重复标记采用不同的复习算法
- 支持在题目列表页直接显示掌握度状态

### 2.2 遗忘曲线复习

#### 2.2.1 艾宾浩斯遗忘曲线原理
根据艾宾浩斯遗忘曲线，人的记忆遗忘规律如下：
- 20分钟后：遗忘约42%
- 1小时后：遗忘约56%
- 1天后：遗忘约66%
- 6天后：遗忘约75%

最佳复习时间点为：**1天、2天、4天、7天、15天、30天**

#### 2.2.2 复习时间计算算法
```java
/**
 * 计算下次复习时间
 * @param masteryLevel 掌握度等级 (1-4)
 * @param reviewCount 已复习次数
 * @return 下次复习的天数间隔
 */
public int calculateNextReviewDays(int masteryLevel, int reviewCount) {
    // 基础间隔：不会=1天, 模糊=2天, 熟悉=4天, 已掌握=7天
    int[] baseIntervals = {1, 2, 4, 7};
    int baseInterval = baseIntervals[masteryLevel - 1];
    
    // 复习次数越多，间隔越长（艾宾浩斯曲线）
    double multiplier = Math.pow(2, Math.min(reviewCount, 5));
    
    // 最长间隔不超过60天
    return Math.min((int)(baseInterval * multiplier), 60);
}
```

**示例计算**:
| 掌握度 | 第1次复习 | 第2次复习 | 第3次复习 | 第4次复习 | 第5次复习 |
|--------|-----------|-----------|-----------|-----------|-----------|
| 不会   | 1天后     | 2天后     | 4天后     | 8天后     | 16天后    |
| 模糊   | 2天后     | 4天后     | 8天后     | 16天后    | 32天后    |
| 熟悉   | 4天后     | 8天后     | 16天后    | 32天后    | 60天后    |
| 已掌握 | 7天后     | 14天后    | 28天后    | 56天后    | 60天后    |

#### 2.2.3 复习提醒入口

**方式一：题库首页顶部提醒卡片**
```
┌─────────────────────────────────────────────────────────┐
│  📚 今日待复习                                          │
├─────────────────────────────────────────────────────────┤
│                                                         │
│   🔴 紧急复习（已逾期）: 5 题                            │
│   🟡 今日待复习: 12 题                                  │
│   🟢 本周待复习: 28 题                                  │
│                                                         │
│   [ 开始复习 ]    [ 查看详情 ]                          │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**方式二：专属复习页面**
路由：`/interview/review`

功能：
- 按紧急程度排序显示待复习题目
- 支持按分类/题单筛选
- 复习完成后自动更新复习计划
- 显示复习进度统计

#### 2.2.4 复习页面UI
```
┌─────────────────────────────────────────────────────────┐
│  📖 智能复习                           进度: 5/17        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │ Q5. HashMap的底层实现原理是什么？                │   │
│  │                                                 │   │
│  │ 所属题单：Java集合框架                           │   │
│  │ 上次标记：模糊 | 距今：3天 | 已复习：2次         │   │
│  │                                                 │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│                  [ 查看答案 ]                           │
│                                                         │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 29%                   │
│                                                         │
│  [ 上一题 ]                               [ 跳过 ]     │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 2.3 学习热力图

#### 2.3.1 功能说明
类似GitHub贡献图，用日历热力图的形式展示用户每天的学习情况，提供直观的学习成就感和连续性激励。

#### 2.3.2 统计维度
- **学习题目数**：当天标记掌握度的题目数量
- **复习题目数**：当天复习的题目数量
- **学习时长**：当天在题目详情页的停留时长（可选）

#### 2.3.3 热度等级
| 等级 | 颜色深度 | 学习题数 |
|------|----------|----------|
| 0 | 灰色 #ebedf0 | 0 题 |
| 1 | 浅绿 #9be9a8 | 1-5 题 |
| 2 | 中绿 #40c463 | 6-15 题 |
| 3 | 深绿 #30a14e | 16-30 题 |
| 4 | 最深绿 #216e39 | 30+ 题 |

#### 2.3.4 热力图UI设计
```
┌─────────────────────────────────────────────────────────┐
│  📈 我的学习轨迹                     2024年12月          │
├─────────────────────────────────────────────────────────┤
│                                                         │
│     1月  2月  3月  4月  5月  6月  7月  8月  9月 10月 11月12月│
│  一  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│  二  ░░░░░░░░░░░░░░░░░░░░░░░░░░▓▓░░░░░░░░░░░░░░░░░░░░░  │
│  三  ░░░░░░░░░░░░░░░░░░░░░░░░░░▓▓▓░░░░░░░░░░░░░░░░░░░░  │
│  四  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░▓▓░░░░░░░░░░░░░░▓▓▓▓▓  │
│  五  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░▓▓▓  │
│  六  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░▓▓  │
│  日  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│                                                         │
│  📊 统计数据                                            │
│  ┌──────────┬──────────┬──────────┬──────────┐         │
│  │ 总学习天数 │ 连续学习  │ 最长连续  │ 本月学习  │         │
│  │    86天   │   7天    │   21天   │   15天   │         │
│  └──────────┴──────────┴──────────┴──────────┘         │
│                                                         │
│  Less ░░▓▓▓▓ More                                      │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

#### 2.3.5 入口位置
- **面试题库首页**：在顶部待复习卡片下方显示迷你版热力图（近30天）
- **个人中心**：完整年度热力图
- **专属页面**：`/interview/my-progress` 学习进度详情页

#### 2.3.6 数据悬浮提示
鼠标悬停在某一天时显示：
```
┌────────────────────┐
│ 2024年12月15日      │
│ 学习了 12 道题      │
│ 复习了 5 道题       │
└────────────────────┘
```

## 3. 数据模型设计

### 3.1 学习掌握度记录表
```sql
CREATE TABLE interview_mastery_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    question_set_id BIGINT NOT NULL COMMENT '题单ID（冗余，方便查询）',
    mastery_level TINYINT NOT NULL DEFAULT 1 COMMENT '掌握度等级 1-不会 2-模糊 3-熟悉 4-已掌握',
    review_count INT NOT NULL DEFAULT 0 COMMENT '复习次数',
    last_review_time DATETIME COMMENT '上次复习时间',
    next_review_time DATETIME COMMENT '下次应复习时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '首次学习时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_question (user_id, question_id),
    INDEX idx_user_next_review (user_id, next_review_time),
    INDEX idx_user_mastery (user_id, mastery_level)
) COMMENT = '面试题掌握度记录表';
```

### 3.2 每日学习统计表
```sql
CREATE TABLE interview_daily_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    stat_date DATE NOT NULL COMMENT '统计日期',
    learn_count INT NOT NULL DEFAULT 0 COMMENT '新学习题目数',
    review_count INT NOT NULL DEFAULT 0 COMMENT '复习题目数',
    total_count INT NOT NULL DEFAULT 0 COMMENT '总学习题目数（新学习+复习）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_date (user_id, stat_date),
    INDEX idx_user_date_range (user_id, stat_date)
) COMMENT = '面试题每日学习统计表';
```

### 3.3 学习掌握度历史记录表（可选，用于分析学习轨迹）
```sql
CREATE TABLE interview_mastery_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    mastery_level TINYINT NOT NULL COMMENT '掌握度等级',
    is_review TINYINT NOT NULL DEFAULT 0 COMMENT '是否复习 0-首次学习 1-复习',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    INDEX idx_user_time (user_id, create_time),
    INDEX idx_question_time (question_id, create_time)
) COMMENT = '面试题掌握度变更历史表';
```

## 4. API接口设计

### 4.1 掌握度相关接口

#### 4.1.1 标记题目掌握度
```
POST /interview/mastery/mark
```

**请求参数**:
```json
{
  "questionId": 12345,
  "questionSetId": 100,
  "masteryLevel": 3,
  "isReview": false
}
```

**响应数据**:
```json
{
  "code": 200,
  "message": "标记成功",
  "data": {
    "questionId": 12345,
    "masteryLevel": 3,
    "masteryLevelText": "熟悉",
    "reviewCount": 1,
    "nextReviewTime": "2025-12-28 00:00:00",
    "nextReviewDays": 4
  }
}
```

#### 4.1.2 获取题目掌握度
```
GET /interview/mastery/{questionId}
```

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "questionId": 12345,
    "masteryLevel": 3,
    "masteryLevelText": "熟悉",
    "reviewCount": 2,
    "lastReviewTime": "2025-12-20 10:30:00",
    "nextReviewTime": "2025-12-28 00:00:00",
    "isOverdue": false
  }
}
```

#### 4.1.3 批量获取掌握度（题单题目列表用）
```
POST /interview/mastery/batch
```

**请求参数**:
```json
{
  "questionIds": [12345, 12346, 12347]
}
```

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "12345": {"masteryLevel": 3, "nextReviewTime": "2025-12-28"},
    "12346": {"masteryLevel": 1, "nextReviewTime": "2025-12-25"},
    "12347": null
  }
}
```

### 4.2 复习相关接口

#### 4.2.1 获取待复习统计
```
GET /interview/review/stats
```

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "overdueCount": 5,
    "todayCount": 12,
    "weekCount": 28,
    "totalLearned": 156
  }
}
```

#### 4.2.2 获取待复习题目列表
```
GET /interview/review/list?type=today&page=1&size=20
```

**参数说明**:
- type: overdue(逾期) / today(今日) / week(本周) / all(全部)

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "total": 12,
    "list": [
      {
        "questionId": 12345,
        "questionSetId": 100,
        "questionTitle": "HashMap的底层实现原理",
        "questionSetTitle": "Java集合框架",
        "masteryLevel": 2,
        "lastReviewTime": "2025-12-21 10:30:00",
        "nextReviewTime": "2025-12-24 00:00:00",
        "reviewCount": 2,
        "overdueDays": 0
      }
    ]
  }
}
```

### 4.3 热力图相关接口

#### 4.3.1 获取学习热力图数据
```
GET /interview/heatmap?year=2025
```

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "year": 2025,
    "totalDays": 86,
    "currentStreak": 7,
    "longestStreak": 21,
    "monthStats": {
      "2025-12": 15
    },
    "dailyData": [
      {"date": "2025-12-01", "count": 5, "level": 1},
      {"date": "2025-12-02", "count": 12, "level": 2},
      {"date": "2025-12-03", "count": 0, "level": 0}
    ]
  }
}
```

#### 4.3.2 获取某日学习详情
```
GET /interview/heatmap/detail?date=2025-12-15
```

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "date": "2025-12-15",
    "learnCount": 8,
    "reviewCount": 4,
    "totalCount": 12,
    "questions": [
      {
        "questionId": 12345,
        "title": "HashMap的底层实现原理",
        "masteryLevel": 3,
        "isReview": true
      }
    ]
  }
}
```

## 5. 前端实现要点

### 5.1 组件结构
```
src/views/interview/
├── components/
│   ├── MasterySelector.vue      # 掌握度选择器组件
│   ├── ReviewReminderCard.vue   # 待复习提醒卡片
│   ├── LearningHeatmap.vue      # 学习热力图组件
│   └── MasteryBadge.vue         # 掌握度徽章（用于列表展示）
├── QuestionDetail.vue           # 题目详情页（修改）
├── Review.vue                   # 复习页面（新增）
└── MyProgress.vue               # 学习进度页面（新增）
```

### 5.2 做题模式集成改造点

在 `QuestionDetail.vue` 中：
1. 查看答案后显示掌握度选择器
2. 显示当前题目的学习历史（上次标记、复习次数）
3. 标记后显示下次复习时间

### 5.3 热力图组件推荐
- 使用 `vue-calendar-heatmap` 或 `echarts` 实现
- 支持年视图切换
- 支持点击日期查看详情

### 5.4 状态管理（Pinia）
```javascript
// stores/learningTracker.js
export const useLearningTrackerStore = defineStore('learningTracker', {
  state: () => ({
    reviewStats: null,         // 待复习统计
    masteryCache: new Map(),   // 掌握度缓存
    heatmapData: null,         // 热力图数据
  }),
  actions: {
    async markMastery(questionId, level) { /* ... */ },
    async fetchReviewStats() { /* ... */ },
    async fetchHeatmap(year) { /* ... */ },
  }
})
```

## 6. 页面路由规划

| 路由 | 页面 | 说明 |
|------|------|------|
| /interview/review | 智能复习页 | 待复习题目列表和复习模式 |
| /interview/my-progress | 学习进度页 | 热力图和统计数据 |

## 7. 实现计划

### 7.1 第一阶段：掌握度标记（核心）
- 数据库表创建
- 后端接口开发（标记、查询）
- 前端掌握度选择器组件
- 做题模式页面集成

### 7.2 第二阶段：遗忘曲线复习
- 复习算法实现
- 待复习统计接口
- 复习提醒卡片组件
- 智能复习页面开发

### 7.3 第三阶段：学习热力图
- 每日统计数据记录
- 热力图数据接口
- 热力图组件开发
- 学习进度页面

### 7.4 预估工期
| 阶段 | 内容 | 后端 | 前端 |
|------|------|------|------|
| 第一阶段 | 掌握度标记 | 1天 | 1天 |
| 第二阶段 | 遗忘曲线复习 | 1天 | 1.5天 |
| 第三阶段 | 学习热力图 | 0.5天 | 1.5天 |
| 联调测试 | 整体测试 | - | 1天 |
| **总计** | | 2.5天 | 5天 |

## 8. 注意事项

### 8.1 性能考虑
- 掌握度查询使用缓存，减少数据库压力
- 热力图数据按年缓存，过期时间24小时
- 批量查询接口减少请求次数

### 8.2 数据一致性
- 标记掌握度时同步更新每日统计
- 使用事务保证数据完整性

### 8.3 用户体验
- 掌握度选择器使用动画过渡，增强反馈感
- 标记成功后显示鼓励性提示语
- 连续学习达成时显示成就提示

### 8.4 隐私保护
- 热力图数据仅用户本人可见
- 学习记录不公开展示

---

**文档版本**: v1.0.0  
**更新日期**: 2025年12月24日  
**维护团队**: Code-Nest开发团队
