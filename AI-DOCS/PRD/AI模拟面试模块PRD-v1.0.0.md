# AI 模拟面试模块 PRD v1.0.0

> 历史说明：本文档中的 `CozeApiClient` 示例与早期 AI 接入描述仅用于回溯方案演进。当前模拟面试链路已经迁移至统一 AI Runtime，并采用 Prompt 模板化与结构化输出约束。

## 📋 项目概述

### 🎯 项目背景
开发者在求职面试前普遍存在以下痛点：
- **缺乏实战经验**：刷了大量面试题，但缺少真实面试场景的模拟练习
- **不知道表达方式**：知道答案却不知道如何有条理地表达
- **无法获得反馈**：自学过程中缺少专业面试官的即时点评和建议
- **面试焦虑**：首次面试紧张，容易因心态问题发挥失常

AI 模拟面试系统旨在通过 AI 技术模拟真实面试场景，帮助用户在实战中提升面试能力，获得即时反馈和专业建议。

### 💡 核心价值
- **真实场景模拟**：AI 面试官根据用户回答动态追问，还原真实面试氛围
- **即时专业反馈**：每道题目即时点评，指出优点和改进方向
- **智能追问机制**：根据回答质量智能决定是否深入追问
- **数据驱动改进**：详细的面试报告帮助用户定位薄弱环节
- **题库深度整合**：与现有面试题库无缝对接，形成"学习-练习-模拟"闭环

### 🏠 模块定位
创建独立的 `xiaou-mock-interview` 模块，与以下模块存在关联：
- **xiaou-interview**：复用面试题库，作为题目来源
- **xiaou-points**：完成模拟面试可获得积分奖励
- **xiaou-notification**：面试报告生成通知
- **xiaou-user**：用户信息和面试历史记录

## 🚀 功能需求

### 1. 面试大厅

#### 1.1 面试方向选择
- **技术栈分类**：Java、前端、Python、Go、全栈等
- **难度级别**：初级（1-3年）、中级（3-5年）、高级（5年+）
- **面试类型**：
  - 技术面试：纯技术问题
  - 综合面试：技术 + 项目经验 + 软技能
  - 专项突破：针对特定知识点（如 Redis、MySQL、Spring）
- **时长设置**：15分钟快速 / 30分钟标准 / 45分钟深度

#### 1.2 面试配置
- **题目数量**：5题 / 8题 / 10题（根据时长自动推荐）
- **追问深度**：浅层（适合新手）/ 标准 / 深入（适合高手）
- **AI 面试官风格**：
  - 温和型：鼓励为主，适合建立信心
  - 标准型：客观公正，模拟常规面试
  - 压力型：高标准严要求，模拟大厂面试

#### 1.3 历史记录
- 查看历史面试列表
- 筛选：按方向、时间、得分筛选
- 重新查看面试报告
- 重做某次面试

### 2. 面试流程

#### 2.1 面试准备页
```
┌─────────────────────────────────────────────────┐
│            🎯 Java 后端技术面试                  │
├─────────────────────────────────────────────────┤
│                                                 │
│  📋 面试信息                                     │
│  ├─ 方向：Java 后端                              │
│  ├─ 难度：中级（3-5年经验）                       │
│  ├─ 题数：8 道                                   │
│  ├─ 时长：约 30 分钟                             │
│  └─ 风格：标准型面试官                            │
│                                                 │
│  💡 面试须知                                     │
│  1. 请在安静环境下进行                           │
│  2. 建议开启麦克风进行语音回答（可选）             │
│  3. 每题回答后会有即时反馈                        │
│  4. 面试结束后生成详细报告                        │
│                                                 │
│              [ 开始面试 ]                        │
│                                                 │
└─────────────────────────────────────────────────┘
```

#### 2.2 面试进行中
```
┌─────────────────────────────────────────────────┐
│  Q3/8        Java 后端面试        ⏱️ 15:23      │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ 👨‍💼 面试官                                │   │
│  │                                         │   │
│  │ 请介绍一下 HashMap 的底层实现原理，      │   │
│  │ 以及 JDK 1.8 做了哪些优化？              │   │
│  │                                         │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ 💬 我的回答                              │   │
│  │                                         │   │
│  │ HashMap 底层是数组+链表的结构...        │   │
│  │                                         │   │
│  │                                         │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  [🎤 语音输入]        [ 提交回答 ]              │
│                                                 │
│  进度: ███████░░░░░░░░░░░░░ 3/8                │
└─────────────────────────────────────────────────┘
```

#### 2.3 AI 反馈与追问
```
┌─────────────────────────────────────────────────┐
│  Q3/8 - 追问        Java 后端面试    ⏱️ 18:45   │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ 📝 回答评价                    得分: 7/10 │   │
│  │                                         │   │
│  │ ✅ 优点：                               │   │
│  │ • 正确描述了数组+链表的基本结构          │   │
│  │ • 提到了红黑树的优化                     │   │
│  │                                         │   │
│  │ ⚠️ 可改进：                             │   │
│  │ • 链表转红黑树的阈值条件可以更准确       │   │
│  │ • 可以补充扩容机制的说明                 │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ 👨‍💼 追问                                 │   │
│  │                                         │   │
│  │ 你提到了红黑树，那请问链表转换成红黑树   │   │
│  │ 的具体条件是什么？为什么选择 8 作为阈值？│   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ 💬 我的回答                              │   │
│  │                                         │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  [ 跳过追问 ]        [ 提交回答 ]              │
└─────────────────────────────────────────────────┘
```

#### 2.4 面试状态机
```
开始面试 → 抽取题目 → 展示问题 → 等待回答 → AI评价
                          ↑              │
                          └─── 追问 ←────┘
                                         │
                          下一题 ←───────┘
                             │
                          结束 → 生成报告
```

### 3. 面试报告

#### 3.1 总体评价
```
┌─────────────────────────────────────────────────┐
│            📊 面试报告                          │
│            Java 后端技术面试                     │
│            2025-12-11 14:30                     │
├─────────────────────────────────────────────────┤
│                                                 │
│  🎯 综合评分                                    │
│                                                 │
│       ████████████████████░░░░ 78/100          │
│                                                 │
│  📈 各维度得分                                  │
│  ┌────────────────────────────────────────┐    │
│  │ 基础知识    ████████████████░░░░ 80%   │    │
│  │ 深度理解    ██████████████░░░░░░ 70%   │    │
│  │ 表达能力    ████████████████████ 85%   │    │
│  │ 应变能力    ██████████████░░░░░░ 72%   │    │
│  └────────────────────────────────────────┘    │
│                                                 │
│  💡 AI 总评                                     │
│  候选人对 Java 基础知识掌握扎实，能够清晰       │
│  表达核心概念。建议加强对底层原理的深入理解，   │
│  特别是 JVM 和并发编程相关内容。                │
│                                                 │
└─────────────────────────────────────────────────┘
```

#### 3.2 题目详情
- 每道题目的问题、回答、评分、反馈
- 追问记录和评价
- 参考答案对比（可选查看）
- 相关知识点链接

#### 3.3 改进建议
- 薄弱知识点列表
- 推荐学习资源（关联知识图谱）
- 推荐练习题目（关联题库）
- 下次面试建议配置

#### 3.4 报告操作
- 下载 PDF 报告
- 分享到社区动态
- 添加到简历面试经历

### 4. AI 面试官

#### 4.1 Prompt 设计
```
# 系统角色设定
你是一位资深的技术面试官，拥有10年大厂面试经验。你的任务是对候选人进行技术面试评估。

# 面试参数
- 方向：{direction}（如：Java后端）
- 难度：{level}（如：中级3-5年）
- 风格：{style}（如：标准型）

# 面试规则
1. 根据候选人回答质量决定是否追问（最多追问2次）
2. 回答质量评分标准：
   - 9-10分：回答完整准确，有深度见解
   - 7-8分：回答基本正确，但有细节遗漏
   - 5-6分：回答部分正确，存在明显错误
   - 0-4分：回答错误或无法回答
3. 追问触发条件：
   - 得分 7-8：追问细节或延伸
   - 得分 5-6：给予提示后追问
   - 得分 9-10 或 0-4：直接下一题

# 当前问题
{question}

# 候选人回答
{answer}

# 请输出（JSON格式）
{
  "score": 0-10,
  "feedback": {
    "strengths": ["优点1", "优点2"],
    "improvements": ["改进点1", "改进点2"]
  },
  "nextAction": "followUp|nextQuestion",
  "followUpQuestion": "追问内容（如果需要追问）",
  "referencePoints": ["考察点1", "考察点2"]
}
```

#### 4.2 面试官风格差异

| 风格 | 评分标准 | 追问频率 | 反馈风格 |
|------|----------|----------|----------|
| 温和型 | 宽松+1分 | 低 | 鼓励为主，少批评 |
| 标准型 | 正常 | 中等 | 客观公正 |
| 压力型 | 严格-1分 | 高 | 直接指出问题 |

#### 4.3 题目抽取策略
1. 根据方向从题库筛选候选题目
2. 根据难度设置题目分布：
   - 初级：简单60% + 中等30% + 困难10%
   - 中级：简单30% + 中等50% + 困难20%
   - 高级：简单10% + 中等40% + 困难50%
3. 保证知识点覆盖多样性
4. 避免重复（参考用户历史面试记录）

### 5. 语音功能（v1.1.0）

#### 5.1 语音输入
- 使用 Web Speech API 进行语音识别
- 支持实时转文字显示
- 支持普通话识别
- 回退方案：纯文字输入

#### 5.2 语音播报
- AI 面试官问题语音播报
- 使用浏览器 SpeechSynthesis API
- 可选开关

### 6. 积分联动

#### 6.1 积分奖励规则
| 行为 | 积分 | 说明 |
|------|------|------|
| 完成一次面试 | 20 | 基础奖励 |
| 面试得分 80+ | 额外 30 | 高分奖励 |
| 面试得分 90+ | 额外 50 | 优秀奖励 |
| 首次面试 | 额外 50 | 新手引导 |
| 连续7天面试 | 100 | 坚持奖励 |

## 🔧 技术实现方案

### 1. 数据库设计

#### 1.1 面试会话表（mock_interview_session）
```sql
CREATE TABLE mock_interview_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    direction VARCHAR(50) NOT NULL COMMENT '面试方向（java/frontend/python等）',
    level TINYINT NOT NULL COMMENT '难度级别：1-初级 2-中级 3-高级',
    interview_type TINYINT DEFAULT 1 COMMENT '面试类型：1-技术 2-综合 3-专项',
    style TINYINT DEFAULT 2 COMMENT 'AI风格：1-温和 2-标准 3-压力',
    question_count INT NOT NULL COMMENT '题目数量',
    duration_minutes INT COMMENT '预计时长（分钟）',
    status TINYINT DEFAULT 0 COMMENT '状态：0-进行中 1-已完成 2-已中断',
    total_score INT COMMENT '总分（满分100）',
    knowledge_score INT COMMENT '知识得分',
    depth_score INT COMMENT '深度得分',
    expression_score INT COMMENT '表达得分',
    adaptability_score INT COMMENT '应变得分',
    ai_summary TEXT COMMENT 'AI总体评价',
    ai_suggestion TEXT COMMENT 'AI改进建议',
    start_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_user_status (user_id, status),
    INDEX idx_direction (direction),
    INDEX idx_create_time (create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模拟面试会话表';
```

#### 1.2 面试问答记录表（mock_interview_qa）
```sql
CREATE TABLE mock_interview_qa (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    question_id BIGINT COMMENT '关联的题库题目ID（可为空）',
    question_order INT NOT NULL COMMENT '题目序号',
    question_content TEXT NOT NULL COMMENT '问题内容',
    question_type TINYINT DEFAULT 1 COMMENT '问题类型：1-主问题 2-追问',
    parent_qa_id BIGINT COMMENT '父问答ID（追问时关联主问题）',
    user_answer TEXT COMMENT '用户回答',
    answer_duration_seconds INT COMMENT '回答用时（秒）',
    score INT COMMENT '得分（0-10）',
    ai_feedback TEXT COMMENT 'AI反馈（JSON格式）',
    reference_answer TEXT COMMENT '参考答案',
    knowledge_points VARCHAR(500) COMMENT '考察知识点（逗号分隔）',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待回答 1-已回答 2-已跳过',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_question_id (question_id),
    INDEX idx_parent_qa_id (parent_qa_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模拟面试问答记录表';
```

#### 1.3 面试方向配置表（mock_interview_direction）
```sql
CREATE TABLE mock_interview_direction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    direction_code VARCHAR(50) NOT NULL COMMENT '方向代码（java/frontend等）',
    direction_name VARCHAR(100) NOT NULL COMMENT '方向名称',
    icon VARCHAR(50) COMMENT '图标',
    description VARCHAR(500) COMMENT '描述',
    category_ids VARCHAR(200) COMMENT '关联的题库分类ID（逗号分隔）',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY uk_direction_code (direction_code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模拟面试方向配置表';
```

#### 1.4 用户面试统计表（mock_interview_user_stats）
```sql
CREATE TABLE mock_interview_user_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '统计ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    total_interviews INT DEFAULT 0 COMMENT '总面试次数',
    completed_interviews INT DEFAULT 0 COMMENT '完成面试次数',
    avg_score DECIMAL(5,2) DEFAULT 0 COMMENT '平均分',
    highest_score INT DEFAULT 0 COMMENT '最高分',
    total_questions INT DEFAULT 0 COMMENT '总回答题数',
    correct_questions INT DEFAULT 0 COMMENT '高分题数（>=7分）',
    interview_streak INT DEFAULT 0 COMMENT '连续面试天数',
    max_streak INT DEFAULT 0 COMMENT '最长连续天数',
    last_interview_date DATE COMMENT '最后面试日期',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户面试统计表';
```

### 2. 后端接口设计

#### 2.1 面试大厅接口（/user/mock-interview）
```
GET  /user/mock-interview/directions       # 获取面试方向列表
GET  /user/mock-interview/config           # 获取面试配置选项
POST /user/mock-interview/create           # 创建面试会话
GET  /user/mock-interview/history          # 获取面试历史记录
GET  /user/mock-interview/{id}/report      # 获取面试报告
DELETE /user/mock-interview/{id}           # 删除面试记录
```

#### 2.2 面试进行接口（/user/mock-interview/session）
```
POST /user/mock-interview/session/start    # 开始面试（获取第一题）
POST /user/mock-interview/session/answer   # 提交回答
POST /user/mock-interview/session/skip     # 跳过当前题目
GET  /user/mock-interview/session/next     # 获取下一题
POST /user/mock-interview/session/end      # 结束面试
GET  /user/mock-interview/session/{id}/status # 获取会话状态
```

#### 2.3 统计接口（/user/mock-interview/stats）
```
GET  /user/mock-interview/stats/overview   # 获取统计总览
GET  /user/mock-interview/stats/trend      # 获取趋势数据
GET  /user/mock-interview/stats/weakness   # 获取薄弱点分析
```

#### 2.4 管理员接口（/admin/mock-interview）
```
GET  /admin/mock-interview/directions      # 获取方向配置列表
POST /admin/mock-interview/directions      # 新增方向配置
PUT  /admin/mock-interview/directions/{id} # 更新方向配置
DELETE /admin/mock-interview/directions/{id} # 删除方向配置
GET  /admin/mock-interview/stats           # 获取整体统计数据
GET  /admin/mock-interview/sessions        # 获取面试记录列表
```

### 3. 核心服务实现

#### 3.1 模块结构
```
xiaou-mock-interview/
├── controller/
│   ├── MockInterviewController.java       # 用户端面试控制器
│   ├── MockInterviewSessionController.java # 面试会话控制器
│   ├── MockInterviewStatsController.java  # 统计控制器
│   └── admin/
│       └── AdminMockInterviewController.java # 管理端控制器
├── domain/
│   ├── MockInterviewSession.java          # 面试会话实体
│   ├── MockInterviewQA.java               # 问答记录实体
│   ├── MockInterviewDirection.java        # 方向配置实体
│   └── MockInterviewUserStats.java        # 用户统计实体
├── dto/
│   ├── request/
│   │   ├── CreateInterviewRequest.java    # 创建面试请求
│   │   ├── SubmitAnswerRequest.java       # 提交回答请求
│   │   └── InterviewHistoryRequest.java   # 历史查询请求
│   └── response/
│       ├── InterviewQuestionResponse.java # 面试问题响应
│       ├── AnswerFeedbackResponse.java    # 回答反馈响应
│       ├── InterviewReportResponse.java   # 面试报告响应
│       └── InterviewStatsResponse.java    # 统计响应
├── service/
│   ├── MockInterviewService.java          # 面试核心服务
│   ├── AIInterviewerService.java          # AI面试官服务
│   ├── QuestionSelectorService.java       # 题目选择服务
│   ├── InterviewReportService.java        # 报告生成服务
│   └── impl/
│       └── ...
├── mapper/
│   ├── MockInterviewSessionMapper.java
│   ├── MockInterviewQAMapper.java
│   ├── MockInterviewDirectionMapper.java
│   └── MockInterviewUserStatsMapper.java
└── config/
    └── AIConfig.java                      # AI服务配置
```

#### 3.2 AI 面试官服务
```java
@Service
public class AIInterviewerService {
    
    @Autowired
    private CozeApiClient cozeApiClient; // 或 OpenAI Client
    
    /**
     * 评价用户回答并决定下一步动作
     */
    public AnswerEvaluation evaluateAnswer(EvaluateRequest request) {
        String prompt = buildPrompt(request);
        
        // 调用 AI 服务
        String response = cozeApiClient.chat(prompt);
        
        // 解析响应
        return parseEvaluation(response);
    }
    
    /**
     * 构建评价 Prompt
     */
    private String buildPrompt(EvaluateRequest request) {
        return String.format("""
            # 角色设定
            你是一位资深%s面试官，拥有10年大厂面试经验。
            
            # 面试配置
            - 难度级别：%s
            - 面试风格：%s
            
            # 评价规则
            1. 评分标准（0-10分）：
               - 9-10：回答完整准确，有深度见解
               - 7-8：基本正确，有细节遗漏
               - 5-6：部分正确，存在明显错误
               - 0-4：错误或无法回答
            2. 追问规则：
               - 得分7-8且追问次数<2：建议追问
               - 其他情况：下一题
            
            # 当前问题
            %s
            
            # 候选人回答
            %s
            
            # 输出要求（JSON格式）
            {
              "score": 评分数字,
              "feedback": {
                "strengths": ["优点1", "优点2"],
                "improvements": ["改进点1", "改进点2"]
              },
              "nextAction": "followUp或nextQuestion",
              "followUpQuestion": "追问内容（如需追问）",
              "referencePoints": ["考察点1", "考察点2"]
            }
            """,
            request.getDirection(),
            request.getLevel(),
            request.getStyle(),
            request.getQuestion(),
            request.getAnswer()
        );
    }
}
```

#### 3.3 题目选择服务
```java
@Service
public class QuestionSelectorService {
    
    @Autowired
    private InterviewQuestionMapper questionMapper;
    
    @Autowired
    private MockInterviewQAMapper qaMapper;
    
    /**
     * 根据配置选择题目
     */
    public List<Long> selectQuestions(SelectRequest request) {
        // 1. 获取方向关联的分类
        List<Long> categoryIds = getCategoryIds(request.getDirection());
        
        // 2. 获取用户历史已答题目（避免重复）
        Set<Long> answeredIds = getAnsweredQuestionIds(request.getUserId());
        
        // 3. 根据难度设置分布比例
        Map<String, Double> distribution = getDifficultyDistribution(request.getLevel());
        
        // 4. 按比例抽取题目
        List<Long> selectedIds = new ArrayList<>();
        int count = request.getQuestionCount();
        
        for (Map.Entry<String, Double> entry : distribution.entrySet()) {
            int num = (int) Math.ceil(count * entry.getValue());
            List<Long> candidates = questionMapper.selectByDifficulty(
                categoryIds, entry.getKey(), answeredIds, num * 2
            );
            Collections.shuffle(candidates);
            selectedIds.addAll(candidates.subList(0, Math.min(num, candidates.size())));
        }
        
        // 5. 随机打乱顺序
        Collections.shuffle(selectedIds);
        return selectedIds.subList(0, Math.min(count, selectedIds.size()));
    }
    
    private Map<String, Double> getDifficultyDistribution(int level) {
        return switch (level) {
            case 1 -> Map.of("easy", 0.6, "medium", 0.3, "hard", 0.1);
            case 2 -> Map.of("easy", 0.3, "medium", 0.5, "hard", 0.2);
            case 3 -> Map.of("easy", 0.1, "medium", 0.4, "hard", 0.5);
            default -> Map.of("easy", 0.33, "medium", 0.34, "hard", 0.33);
        };
    }
}
```

### 4. 前端实现

#### 4.1 目录结构
```
vue3-user-front/src/views/mock-interview/
├── index.vue                    # 面试大厅页面
├── config.vue                   # 面试配置页面
├── session.vue                  # 面试进行页面
├── report.vue                   # 面试报告页面
├── history.vue                  # 历史记录页面
├── components/
│   ├── DirectionCard.vue        # 方向选择卡片
│   ├── InterviewChat.vue        # 面试对话组件
│   ├── QuestionCard.vue         # 问题卡片组件
│   ├── FeedbackPanel.vue        # 反馈面板组件
│   ├── ScoreChart.vue           # 得分图表组件
│   ├── VoiceInput.vue           # 语音输入组件
│   └── ReportSummary.vue        # 报告摘要组件
└── api/
    └── mockInterview.js         # API接口封装
```

#### 4.2 核心状态管理
```javascript
// stores/mockInterview.js
import { defineStore } from 'pinia'

export const useMockInterviewStore = defineStore('mockInterview', {
  state: () => ({
    currentSession: null,          // 当前会话
    currentQuestion: null,         // 当前问题
    questionIndex: 0,              // 题目序号
    totalQuestions: 0,             // 总题数
    answers: [],                   // 回答记录
    isSubmitting: false,           // 提交中
    interviewStatus: 'idle',       // idle/ongoing/finished
    timer: 0,                      // 计时器
  }),
  
  actions: {
    // 开始面试
    async startInterview(config) {
      const res = await api.createInterview(config)
      this.currentSession = res.data
      this.totalQuestions = config.questionCount
      this.interviewStatus = 'ongoing'
      this.startTimer()
      return this.fetchNextQuestion()
    },
    
    // 提交回答
    async submitAnswer(answer) {
      this.isSubmitting = true
      try {
        const res = await api.submitAnswer({
          sessionId: this.currentSession.id,
          qaId: this.currentQuestion.id,
          answer: answer
        })
        this.answers.push(res.data)
        return res.data
      } finally {
        this.isSubmitting = false
      }
    },
    
    // 获取下一题
    async fetchNextQuestion() {
      const res = await api.getNextQuestion(this.currentSession.id)
      if (res.data.finished) {
        this.interviewStatus = 'finished'
        return null
      }
      this.currentQuestion = res.data
      this.questionIndex++
      return res.data
    },
    
    // 结束面试
    async endInterview() {
      await api.endInterview(this.currentSession.id)
      this.interviewStatus = 'finished'
      this.stopTimer()
    }
  }
})
```

#### 4.3 语音输入组件
```vue
<template>
  <div class="voice-input">
    <el-button 
      :type="isRecording ? 'danger' : 'primary'" 
      circle
      @click="toggleRecording"
    >
      <el-icon><Microphone /></el-icon>
    </el-button>
    <span v-if="isRecording" class="recording-tip">
      正在录音... {{ transcript }}
    </span>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const emit = defineEmits(['transcriptUpdate', 'transcriptEnd'])

const isRecording = ref(false)
const transcript = ref('')
let recognition = null

onMounted(() => {
  if ('webkitSpeechRecognition' in window) {
    recognition = new webkitSpeechRecognition()
    recognition.continuous = true
    recognition.interimResults = true
    recognition.lang = 'zh-CN'
    
    recognition.onresult = (event) => {
      let final = ''
      let interim = ''
      
      for (let i = event.resultIndex; i < event.results.length; i++) {
        if (event.results[i].isFinal) {
          final += event.results[i][0].transcript
        } else {
          interim += event.results[i][0].transcript
        }
      }
      
      transcript.value = final + interim
      emit('transcriptUpdate', transcript.value)
    }
    
    recognition.onend = () => {
      isRecording.value = false
      emit('transcriptEnd', transcript.value)
    }
  }
})

const toggleRecording = () => {
  if (isRecording.value) {
    recognition?.stop()
  } else {
    transcript.value = ''
    recognition?.start()
    isRecording.value = true
  }
}

onUnmounted(() => {
  recognition?.stop()
})
</script>
```

## 📐 接口请求/响应示例

### 创建面试会话
```json
// POST /user/mock-interview/create
// 请求
{
  "direction": "java",
  "level": 2,
  "interviewType": 1,
  "style": 2,
  "questionCount": 8
}

// 响应
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "sessionId": 10001,
    "direction": "java",
    "directionName": "Java 后端",
    "level": 2,
    "levelName": "中级（3-5年）",
    "questionCount": 8,
    "estimatedMinutes": 30,
    "createTime": "2025-12-11 14:30:00"
  }
}
```

### 开始面试/获取题目
```json
// POST /user/mock-interview/session/start
// 请求
{
  "sessionId": 10001
}

// 响应
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "qaId": 20001,
    "questionOrder": 1,
    "totalQuestions": 8,
    "questionContent": "请介绍一下 HashMap 的底层实现原理，以及 JDK 1.8 做了哪些优化？",
    "questionType": 1,
    "knowledgePoints": ["HashMap", "数据结构", "JDK1.8"],
    "estimatedTime": 180
  }
}
```

### 提交回答
```json
// POST /user/mock-interview/session/answer
// 请求
{
  "sessionId": 10001,
  "qaId": 20001,
  "answer": "HashMap底层是数组+链表的结构。在JDK1.8中，当链表长度超过8时会转换为红黑树...",
  "durationSeconds": 120
}

// 响应
{
  "code": 200,
  "message": "评价完成",
  "data": {
    "qaId": 20001,
    "score": 7,
    "feedback": {
      "strengths": [
        "正确描述了数组+链表的基本结构",
        "提到了红黑树的优化"
      ],
      "improvements": [
        "链表转红黑树的阈值条件可以更准确",
        "可以补充扩容机制的说明"
      ]
    },
    "nextAction": "followUp",
    "followUpQuestion": {
      "qaId": 20002,
      "questionContent": "你提到了红黑树，那请问链表转换成红黑树的具体条件是什么？为什么选择 8 作为阈值？",
      "questionType": 2
    },
    "hasNext": true
  }
}
```

### 获取面试报告
```json
// GET /user/mock-interview/{id}/report
// 响应
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "sessionId": 10001,
    "direction": "java",
    "directionName": "Java 后端",
    "level": 2,
    "totalScore": 78,
    "dimensions": {
      "knowledge": 80,
      "depth": 70,
      "expression": 85,
      "adaptability": 72
    },
    "aiSummary": "候选人对 Java 基础知识掌握扎实，能够清晰表达核心概念。建议加强对底层原理的深入理解，特别是 JVM 和并发编程相关内容。",
    "aiSuggestion": [
      "建议深入学习 JVM 内存模型和垃圾回收机制",
      "并发编程相关知识点需要加强",
      "可以多进行项目实战，积累经验"
    ],
    "weakPoints": ["JVM", "并发编程", "MySQL优化"],
    "qaList": [
      {
        "qaId": 20001,
        "questionOrder": 1,
        "questionContent": "请介绍一下 HashMap 的底层实现原理...",
        "userAnswer": "HashMap底层是数组+链表的结构...",
        "score": 7,
        "feedback": {...},
        "followUps": [
          {
            "qaId": 20002,
            "questionContent": "链表转换成红黑树的具体条件...",
            "userAnswer": "...",
            "score": 6
          }
        ]
      }
    ],
    "duration": 1845,
    "startTime": "2025-12-11 14:30:00",
    "endTime": "2025-12-11 15:00:45",
    "pointsEarned": 20
  }
}
```

## ✅ 实现范围

### 第一期功能（v1.0.0）
- ✅ **面试大厅**：方向选择、难度配置、历史记录
- ✅ **核心流程**：创建面试、答题、AI评价、追问机制
- ✅ **面试报告**：总分、分维度评分、AI总评、题目详情
- ✅ **题库对接**：从 xiaou-interview 抽取题目
- ✅ **积分联动**：完成面试获得积分奖励
- ✅ **基础统计**：面试次数、平均分、完成率

### 第二期功能（v1.1.0）
- 🔲 **语音输入**：支持语音回答（Web Speech API）
- 🔲 **语音播报**：AI面试官语音提问
- 🔲 **报告导出**：导出 PDF 格式报告
- 🔲 **分享功能**：分享报告到社区动态
- 🔲 **趋势图表**：面试成绩趋势分析

### 第三期功能（v1.2.0）
- 🔲 **视频面试**：开启摄像头模拟真实面试
- 🔲 **表情分析**：AI分析面试者表情和状态
- 🔲 **多轮追问**：更智能的追问策略
- 🔲 **自定义题目**：支持用户上传题目进行面试
- 🔲 **排行榜**：面试成绩排行榜

## 📝 附录

### A. 面试方向枚举
```java
public enum InterviewDirection {
    JAVA("java", "Java 后端", "☕"),
    FRONTEND("frontend", "前端开发", "🌐"),
    PYTHON("python", "Python 开发", "🐍"),
    GO("go", "Go 开发", "🔷"),
    FULLSTACK("fullstack", "全栈开发", "🔄"),
    DATABASE("database", "数据库", "🗄️"),
    DEVOPS("devops", "DevOps", "🔧"),
    ALGORITHM("algorithm", "算法", "🧮");
}
```

### B. 难度级别枚举
```java
public enum InterviewLevel {
    JUNIOR(1, "初级", "1-3年经验"),
    MIDDLE(2, "中级", "3-5年经验"),
    SENIOR(3, "高级", "5年以上经验");
}
```

### C. AI风格枚举
```java
public enum InterviewStyle {
    GENTLE(1, "温和型", "鼓励为主，适合建立信心"),
    STANDARD(2, "标准型", "客观公正，模拟常规面试"),
    PRESSURE(3, "压力型", "高标准严要求，模拟大厂面试");
}
```

### D. 积分规则配置
```java
public enum MockInterviewPointsRule {
    COMPLETE_INTERVIEW(20, "完成面试"),
    SCORE_80_PLUS(30, "得分80+"),
    SCORE_90_PLUS(50, "得分90+"),
    FIRST_INTERVIEW(50, "首次面试"),
    STREAK_7_DAYS(100, "连续7天面试");
}
```

---

*文档版本: v1.0.0*  
*创建时间: 2025-12-11*  
*最后更新: 2025-12-11*
