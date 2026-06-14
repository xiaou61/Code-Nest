# AI模拟面试 Coze工作流配置指南

本文档介绍AI模拟面试模块所需的Coze工作流配置。

## 概述

AI模拟面试模块需要配置3个Coze工作流：

| 工作流 | 枚举名 | 用途 |
|--------|--------|------|
| 答案评价 | `MOCK_INTERVIEW_EVALUATE` | AI评价候选人答案并给出反馈 |
| 面试总结 | `MOCK_INTERVIEW_SUMMARY` | AI生成面试总结报告和建议 |
| AI出题 | `MOCK_INTERVIEW_GENERATE_QUESTIONS` | AI根据方向和难度生成面试题目 |

## 配置位置

工作流ID配置在 `xiaou-common` 模块的枚举类中：

```
xiaou-common/src/main/java/com/xiaou/common/enums/CozeWorkflowEnum.java
```

配置好Coze工作流后，将真实的工作流ID替换到对应的枚举值中。

---

## 工作流1：答案评价 (MOCK_INTERVIEW_EVALUATE)

### 功能说明
评价候选人的回答，给出评分、反馈、以及是否需要追问。

### 输入参数

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| direction | String | 是 | 面试方向 | "java" |
| level | String | 是 | 难度级别 | "初级" / "中级" / "高级" |
| style | String | 是 | 面试风格 | "友好型" / "标准型" / "严格型" |
| question | String | 是 | 面试问题 | "请介绍HashMap的底层实现原理" |
| answer | String | 是 | 候选人回答 | "HashMap基于数组+链表..." |
| followUpCount | Integer | 是 | 当前追问次数 | 0 |

### Coze工作流提示词模板

```
# 角色设定
你是一位资深{{direction}}技术面试官，拥有10年大厂面试经验。

# 面试配置
- 难度级别：{{level}}
- 面试风格：{{style}}

# 评价规则
1. 评分标准（0-10分）：
   - 9-10：回答完整准确，有深度见解
   - 7-8：基本正确，有细节遗漏
   - 5-6：部分正确，存在明显错误
   - 0-4：错误或无法回答
2. 追问规则（当前追问次数：{{followUpCount}}，最多2次）：
   - 得分7-8且追问次数<2：可以追问深入
   - 其他情况：进入下一题

# 当前问题
{{question}}

# 候选人回答
{{answer}}

# 输出要求
请严格按以下JSON格式输出，不要输出其他内容：
{
  "score": 评分数字(0-10),
  "feedback": {
    "strengths": ["优点1", "优点2"],
    "improvements": ["改进点1", "改进点2"]
  },
  "nextAction": "followUp或nextQuestion",
  "followUpQuestion": "追问内容（仅当nextAction为followUp时填写）",
  "referencePoints": ["考察知识点1", "考察知识点2"]
}
```

### 输出格式

Coze工作流返回格式为 `{"output": "实际JSON字符串"}`，output字段内容如下：

```json
{
  "score": 8,
  "feedback": {
    "strengths": ["对HashMap基本原理理解正确", "提到了红黑树优化"],
    "improvements": ["可以补充扩容机制细节", "建议说明线程安全问题"]
  },
  "nextAction": "followUp",
  "followUpQuestion": "HashMap在多线程环境下有什么问题？如何解决？",
  "referencePoints": ["HashMap", "红黑树", "哈希冲突"]
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| score | Integer | 评分（0-10） |
| feedback.strengths | Array | 回答的优点列表 |
| feedback.improvements | Array | 需要改进的地方列表 |
| nextAction | String | 下一步动作：`followUp`(追问) 或 `nextQuestion`(下一题) |
| followUpQuestion | String | 追问内容（仅当nextAction为followUp时有效） |
| referencePoints | Array | 本题考察的知识点 |

---

## 工作流2：面试总结 (MOCK_INTERVIEW_SUMMARY)

### 功能说明
面试结束后生成总结报告和改进建议。

### 输入参数

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| direction | String | 是 | 面试方向 | "java" |
| level | String | 是 | 难度级别 | "中级" |
| questionCount | Integer | 是 | 题目总数 | 5 |
| answeredCount | Integer | 是 | 已回答数 | 4 |
| skippedCount | Integer | 是 | 跳过数 | 1 |
| totalScore | Integer | 是 | 总得分（百分制） | 72 |
| qaList | String | 是 | 问答记录JSON数组 | 见下方示例 |

**qaList 格式示例：**
```json
[
  {
    "question": "请介绍HashMap的底层实现原理",
    "answer": "HashMap基于数组+链表实现...",
    "score": 8,
    "status": "已答"
  },
  {
    "question": "什么是JVM垃圾回收？",
    "answer": "",
    "score": 0,
    "status": "跳过"
  }
]
```

### Coze工作流提示词模板

```
# 角色设定
你是一位资深技术面试官，需要为本次模拟面试生成总结报告。

# 面试信息
- 面试方向：{{direction}}
- 难度级别：{{level}}
- 题目总数：{{questionCount}}
- 已回答：{{answeredCount}}题
- 跳过：{{skippedCount}}题
- 总得分：{{totalScore}}分

# 问答记录
{{qaList}}

# 输出要求
请根据以上信息，生成一份简洁的面试总结（200字左右），包含：
1. 整体表现评价
2. 知识掌握情况
3. 主要优势
4. 需要改进的方向
5. 学习建议

请严格按以下JSON格式输出：
{
  "summary": "总结内容（200字左右）",
  "overallLevel": "优秀/良好/一般/需加强",
  "suggestions": ["建议1", "建议2", "建议3"]
}
```

### 输出格式

```json
{
  "summary": "候选人在Java基础方向表现良好，对集合框架和JVM有一定理解，但在并发编程方面存在知识盲区。建议系统学习Java并发包，多进行实战练习。",
  "overallLevel": "良好",
  "suggestions": [
    "深入学习Java并发编程，掌握锁机制和线程池",
    "多进行源码阅读，理解框架设计思想",
    "加强算法练习，提升编码能力"
  ]
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| summary | String | 面试总结文本（200字左右） |
| overallLevel | String | 整体水平：优秀/良好/一般/需加强 |
| suggestions | Array | 改进建议列表（3-5条） |

---

## 工作流3：AI出题 (MOCK_INTERVIEW_GENERATE_QUESTIONS)

### 功能说明
根据面试方向和难度级别，AI生成面试题目。

### 输入参数

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| direction | String | 是 | 面试方向 | "java" |
| level | String | 是 | 难度级别 | "初级" / "中级" / "高级" |
| count | Integer | 是 | 需要生成的题目数量 | 5 |

### Coze工作流提示词模板

```
# 角色设定
你是一位资深{{direction}}技术面试官，需要为模拟面试生成题目。

# 出题要求
- 面试方向：{{direction}}
- 难度级别：{{level}}
- 题目数量：{{count}}道

# 出题规则
1. 题目应覆盖该方向的核心知识点
2. 难度要符合{{level}}级别要求：
   - 初级：基础概念、常见API使用
   - 中级：原理分析、常见问题解决
   - 高级：源码分析、架构设计、性能优化
3. 每道题需要提供参考答案和考察知识点
4. 题目应具有区分度，能考察候选人真实水平
5. 避免过于简单或过于偏门的问题

# 输出要求
请严格按以下JSON数组格式输出，不要输出其他内容：
[
  {
    "question": "面试问题",
    "answer": "参考答案（100-200字，简明扼要）",
    "knowledgePoints": "知识点1,知识点2,知识点3"
  }
]
```

### 输出格式

```json
[
  {
    "question": "请介绍HashMap的底层实现原理，以及JDK1.8做了哪些优化？",
    "answer": "HashMap基于数组+链表实现，通过hash函数计算key的存储位置。JDK1.8的优化：1.当链表长度超过8且数组长度>=64时转为红黑树，查询复杂度从O(n)降为O(logn)；2.扩容时采用高低位链表避免rehash；3.hash函数简化，使用扰动函数减少碰撞。",
    "knowledgePoints": "HashMap,红黑树,哈希冲突,扩容机制"
  },
  {
    "question": "synchronized和ReentrantLock有什么区别？各自适用什么场景？",
    "answer": "主要区别：1.synchronized是JVM层面的锁，ReentrantLock是API层面；2.ReentrantLock支持公平锁、可中断、超时获取等特性；3.synchronized自动释放锁，ReentrantLock需手动释放。场景：简单同步用synchronized，需要高级特性用ReentrantLock。",
    "knowledgePoints": "synchronized,ReentrantLock,并发,锁机制"
  }
]
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| question | String | 面试问题 |
| answer | String | 参考答案（100-200字） |
| knowledgePoints | String | 考察的知识点，逗号分隔 |

---

## 面试方向参考

系统支持的面试方向（direction参数值）：

| 方向代码 | 方向名称 | 说明 |
|----------|----------|------|
| java | Java后端 | Java核心、JVM、Spring、MySQL、Redis等 |
| frontend | 前端开发 | JavaScript、Vue、React、CSS、浏览器等 |
| python | Python开发 | Python基础、Django/Flask、数据处理等 |
| go | Go开发 | Go语言特性、并发、微服务等 |
| fullstack | 全栈开发 | 前后端综合、系统设计等 |
| database | 数据库 | MySQL、Redis、MongoDB、SQL优化等 |
| devops | DevOps | Linux、Docker、K8s、CI/CD等 |
| algorithm | 算法 | 数据结构、算法设计、复杂度分析等 |

---

## 配置步骤

1. **创建Coze工作流**
   - 登录 [Coze平台](https://www.coze.cn)
   - 分别创建上述3个工作流
   - 配置输入参数和提示词

2. **获取工作流ID**
   - 在工作流详情页获取工作流ID

3. **更新代码配置**
   - 修改 `CozeWorkflowEnum.java` 中对应的工作流ID：
   ```java
   MOCK_INTERVIEW_EVALUATE("你的工作流ID", "模拟面试答案评价", "..."),
   MOCK_INTERVIEW_SUMMARY("你的工作流ID", "模拟面试总结", "..."),
   MOCK_INTERVIEW_GENERATE_QUESTIONS("你的工作流ID", "模拟面试AI出题", "...")
   ```

4. **测试验证**
   - 启动应用，测试模拟面试功能
   - 检查AI评价、总结、出题是否正常工作

---

## 降级机制

当Coze服务不可用时，系统会自动启用本地降级方案：

- **答案评价**：基于回答长度进行简单评分
- **面试总结**：根据总分生成固定模板总结
- **AI出题**：使用预置的本地题库（Java/前端/Python）

这确保了即使AI服务异常，用户仍可正常使用模拟面试功能。

---

## 注意事项

1. **输出格式**：Coze工作流的输出节点需要配置为JSON格式
2. **output包装**：系统会自动处理Coze返回的 `{"output": "..."}` 格式
3. **错误处理**：工作流应避免输出非JSON内容，否则解析会失败
4. **性能考虑**：建议在Coze工作流中启用缓存，减少重复调用
