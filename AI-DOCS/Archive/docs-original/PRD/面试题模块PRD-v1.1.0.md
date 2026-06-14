# 面试题模块PRD-v1.1.0

> 历史说明：本文档中的 Coze 工作流设计属于项目早期 AI 解答方案。当前主干 AI 能力已统一迁移到 `LangChain4j + LangGraph4j + LlamaIndex` Runtime，本文保留作需求演进记录。

## 版本信息
- **版本号**: v1.1.0
- **发布日期**: 2025年9月
- **基础版本**: v1.0.0
- **主要更新**: AI解答功能

## 1. 版本概述

### 1.1 更新背景
基于用户对智能化学习体验的需求，在面试题模块现有功能基础上，集成AI解答功能，为用户提供个性化的题目解析和学习辅助。

### 1.2 核心更新
- **AI智能解答**：集成Coze工作流，为每个题目提供AI生成的解答内容
- **多维度解答**：提供一句话总结、详细答案、形象举例三种解答形式
- **智能缓存**：Redis缓存机制，提升用户体验和系统性能
- **刷新控制**：智能刷新策略，平衡用户需求和系统资源

### 1.3 技术集成
- **Coze工作流**: 工作流ID `7555326749979721743`
- **缓存策略**: Redis 7天缓存机制
- **数据持久化**: 数据库存储AI解答内容

## 2. 新增功能详述

### 2.1 AI解答核心功能

#### 2.1.1 功能入口
**位置**: 题目学习页面 (`/interview/questions/:setId/:questionId`)

**展示形式**:
- 在题目内容下方增加AI解答区域
- 初始状态显示"获得AI解答"按钮
- 获取后显示解答内容和操作按钮

#### 2.1.2 AI解答内容结构

**Coze工作流返回数据**:
```json
{
  "output": "AI一句话总结版本",
  "output1": "AI详细答案",
  "output2": "形象举例的版本"
}
```

**前端展示结构**:
```
┌─────────────────────────────────────┐
│ 🤖 AI智能解答                        │
├─────────────────────────────────────┤
│ 📝 一句话总结                        │
│ [output内容]                        │
├─────────────────────────────────────┤
│ 💡 详细解答                          │
│ [output1内容]                       │
├─────────────────────────────────────┤
│ 🌰 形象举例                          │
│ [output2内容]                       │
├─────────────────────────────────────┤
│ [获取] [强制刷新]                     │
└─────────────────────────────────────┘
```

#### 2.1.3 交互流程

**初次获取流程**:
1. 用户点击"获得AI解答"按钮
2. 系统检查Redis缓存和数据库
3. 如无缓存，调用Coze工作流API
4. 存储结果到数据库和Redis（7天TTL）
5. 展示AI解答内容
6. 按钮变更为"获取"和"强制刷新"

**再次访问流程**:
1. 优先从Redis缓存读取
2. Redis过期则从数据库读取
3. 数据库无数据则显示"获得AI解答"按钮

**操作按钮逻辑**:

| 按钮 | 触发条件 | 执行逻辑 |
|------|----------|----------|
| 获得AI解答 | 无任何数据 | 调用API→入库→缓存→展示 |
| 获取 | 有历史数据 | 缓存读取→数据库读取→展示 |
| 强制刷新 | Redis无数据且DB有数据 | 调用API→更新DB→缓存→展示 |

### 2.2 数据模型设计

#### 2.2.1 AI解答表结构
```sql
CREATE TABLE interview_ai_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    summary TEXT COMMENT 'AI一句话总结（output）',
    detailed_answer LONGTEXT COMMENT 'AI详细答案（output1）',
    example TEXT COMMENT '形象举例（output2）',
    coze_workflow_id VARCHAR(50) DEFAULT '7555326749979721743' COMMENT 'Coze工作流ID',
    generate_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'AI生成时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_question_id (question_id),
    UNIQUE KEY uk_question_id (question_id)
) COMMENT = '面试题AI解答表';
```

#### 2.2.2 Redis缓存设计
**Key命名规则**: `interview:ai:answer:{questionId}`

**数据结构**:
```json
{
  "questionId": 12345,
  "summary": "AI一句话总结",
  "detailedAnswer": "AI详细答案",
  "example": "形象举例",
  "generateTime": "2025-09-29 12:00:00",
  "cacheTime": "2025-09-29 12:00:00"
}
```

**TTL设置**: 7天 (604800秒)

### 2.3 API接口设计

#### 2.3.1 获得AI解答
```
POST /interview/questions/{questionId}/ai-answer
```

**请求参数**:
```json
{
  "questionId": 12345,
  "forceRefresh": false
}
```

**响应数据**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "questionId": 12345,
    "summary": "AI一句话总结",
    "detailedAnswer": "AI详细答案", 
    "example": "形象举例",
    "generateTime": "2025-09-29 12:00:00",
    "fromCache": true
  }
}
```

#### 2.3.2 检查AI解答状态
```
GET /interview/questions/{questionId}/ai-answer/status
```

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "hasData": true,
    "inCache": true,
    "inDatabase": true,
    "canRefresh": true,
    "generateTime": "2025-09-29 12:00:00"
  }
}
```

### 2.4 Coze工作流集成

#### 2.4.1 API调用规范
**工作流ID**: `7555326749979721743`

**请求参数**:
```json
{
  "question": "题目标题/题目内容",
  "answer": "题目的参考答案内容"
}
```

**响应处理**:
```json
{
  "output": "提取到summary字段",
  "output1": "提取到detailedAnswer字段", 
  "output2": "提取到example字段"
}
```

#### 2.4.2 异常处理
- **API调用失败**: 显示错误提示，不影响原有功能
- **响应格式异常**: 记录日志，显示"AI解答暂时不可用"
- **超时处理**: 设置30秒超时，超时后取消请求

### 2.5 缓存策略

#### 2.5.1 缓存逻辑
```
请求AI解答
    ↓
检查Redis缓存
    ├── 有缓存 → 直接返回
    └── 无缓存
        ↓
    检查数据库
        ├── 有数据 → 回填缓存 → 返回数据
        └── 无数据 → 调用Coze → 入库 → 缓存 → 返回
```

#### 2.5.2 刷新策略
**强制刷新条件判断**:
```java
public boolean canForceRefresh(Long questionId) {
    boolean hasCache = redisTemplate.hasKey("interview:ai:answer:" + questionId);
    boolean hasDatabase = aiAnswerMapper.existsByQuestionId(questionId);
    return !hasCache && hasDatabase;
}
```

## 3. 用户体验优化

### 3.1 页面交互优化
- **Loading状态**: 调用AI时显示加载动画和进度提示
- **错误提示**: 友好的错误信息展示
- **响应式设计**: 适配移动端显示
- **内容展示**: Markdown格式支持，代码高亮

### 3.2 性能优化
- **防重复点击**: 按钮防抖处理，避免重复请求
- **分页加载**: AI解答内容过长时支持分页展示
- **缓存预热**: 热门题目的AI解答预生成
- **异步处理**: AI调用采用异步方式，提升响应速度

### 3.3 统计分析
- **使用统计**: 记录AI解答功能使用频率
- **效果分析**: 跟踪用户学习效果提升
- **成本控制**: 监控Coze API调用量和成本

## 4. 系统架构变更

### 4.1 后端模块调整

#### 4.1.1 新增组件
```
xiaou-interview/
├── controller/
│   └── InterviewAiAnswerController.java
├── service/
│   ├── InterviewAiAnswerService.java
│   └── impl/InterviewAiAnswerServiceImpl.java
├── mapper/
│   └── InterviewAiAnswerMapper.java
├── entity/
│   └── InterviewAiAnswer.java
└── integration/
    └── CozeWorkflowClient.java
```

#### 4.1.2 配置文件调整
```yaml
# application.yml
coze:
  workflow:
    baseUrl: https://api.coze.com
    workflowId: 7555326749979721743
    timeout: 30000
    
redis:
  interview:
    ai-answer:
      ttl: 604800 # 7天
      key-prefix: interview:ai:answer
```

### 4.2 前端组件调整

#### 4.2.1 新增组件
```
src/views/interview/
├── components/
│   ├── AiAnswerPanel.vue      # AI解答面板
│   ├── AiAnswerButton.vue     # AI解答按钮
│   └── LoadingSpinner.vue     # 加载动画
└── questions/
    └── QuestionDetail.vue     # 题目详情页（更新）
```

#### 4.2.2 API接口
```javascript
// src/api/interview.js
export const getAiAnswer = (questionId, forceRefresh = false) => {
  return request({
    url: `/interview/questions/${questionId}/ai-answer`,
    method: 'post',
    data: { questionId, forceRefresh }
  })
}

export const getAiAnswerStatus = (questionId) => {
  return request({
    url: `/interview/questions/${questionId}/ai-answer/status`,
    method: 'get'
  })
}
```

## 5. 上线计划

### 5.1 开发阶段
| 阶段 | 内容 | 预计时间 |
|------|------|----------|
| 数据库设计 | 建表、索引创建 | 1天 |
| 后端开发 | API接口、业务逻辑 | 3天 |
| 前端开发 | 组件、页面调整 | 2天 |
| 联调测试 | 功能测试、性能测试 | 1天 |
| 上线部署 | 生产环境部署 | 0.5天 |

### 5.2 测试要点
- **功能测试**: AI解答获取、缓存机制、刷新逻辑
- **性能测试**: 并发请求、缓存命中率、响应时间
- **兼容性测试**: 不同浏览器、移动端适配
- **异常测试**: 网络异常、API异常、数据异常处理

### 5.3 监控指标
- **功能指标**: AI解答成功率、缓存命中率
- **性能指标**: API响应时间、页面加载时间
- **业务指标**: 功能使用率、用户满意度
- **成本指标**: Coze API调用次数、费用控制

## 6. 风险评估与应对

### 6.1 技术风险
| 风险点 | 影响程度 | 应对策略 |
|--------|----------|----------|
| Coze API不稳定 | 中 | 熔断机制、降级处理 |
| Redis缓存失效 | 低 | 数据库兜底、重建缓存 |
| AI内容质量 | 中 | 内容审核、人工校验 |

### 6.2 业务风险
| 风险点 | 影响程度 | 应对策略 |
|--------|----------|----------|
| API成本过高 | 高 | 频率限制、缓存优化 |
| 用户过度依赖 | 中 | 功能引导、学习建议 |
| 内容版权问题 | 低 | 免责声明、内容标注 |

## 7. 后续优化方向

### 7.1 功能增强
- **个性化解答**: 根据用户水平调整解答深度
- **语音解答**: 支持语音播报AI解答内容
- **学习路径**: 基于AI解答推荐相关题目
- **错误反馈**: 用户可反馈AI解答质量

### 7.2 技术优化
- **本地缓存**: 浏览器本地存储常用解答
- **CDN加速**: AI解答内容CDN分发
- **智能预加载**: 预测用户需求，提前生成解答
- **多模型支持**: 支持多种AI模型对比

### 7.3 数据分析
- **学习效果分析**: 跟踪AI解答对学习效果的影响
- **内容质量评估**: 统计用户对AI解答的满意度
- **使用模式分析**: 分析用户使用AI解答的规律
- **成本效益分析**: 评估功能投入产出比

---

**文档版本**: v1.1.0  
**更新日期**: 2025年9月29日  
**维护团队**: Code-Nest开发团队
