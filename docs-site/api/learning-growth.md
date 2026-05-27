# 学习成长 API

本文档详细说明学习成长模块的 API 接口，包括面试题库、模拟面试、学习资产、闪卡、知识图谱、计划打卡、学习小组等。

## 路由约定

| 前缀 | 使用场景 | 登录域 |
| --- | --- | --- |
| `/interview/**` | 面试题库公开接口 | 按接口判断 |
| `/user/mock-interview` | 用户端模拟面试 | 用户登录态 |
| `/user/job-battle` | 用户端求职作战台 | 用户登录态 |
| `/user/plan` | 用户端计划打卡 | 用户登录态 |
| `/user/team` | 用户端学习小组 | 用户登录态 |
| `/flashcard/**` | 闪卡接口 | 用户登录态 |
| `/pub/**` | 公开查询接口 | 无需登录 |
| `/admin/**` | 管理端接口 | 管理员登录态 |

---

## 一、面试题库（InterviewQuestionSetPublicController）

**模块**：`xiaou-interview`
**路由前缀**：`/interview/question-sets`
**权限要求**：部分接口需用户登录

### 1.1 获取题单列表

```
GET /interview/question-sets
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| categoryId | Long | 否 | 分类 ID |
| keyword | String | 否 | 关键词搜索 |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 50,
    "totalPages": 5,
    "records": [
      {
        "id": 1,
        "title": "Java 基础题单",
        "description": "涵盖 Java 核心知识点",
        "categoryId": 1,
        "categoryName": "Java",
        "questionCount": 20,
        "studyCount": 100,
        "creatorName": "系统",
        "createTime": "2024-01-01 12:00:00"
      }
    ],
    "hasNext": true,
    "hasPrevious": false
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/interview/question-sets?pageNum=1&pageSize=10&categoryId=1"
```

### 1.2 获取题单详情

```
GET /interview/question-sets/{id}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 题单 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "title": "Java 基础题单",
    "description": "涵盖 Java 核心知识点",
    "categoryId": 1,
    "categoryName": "Java",
    "questionCount": 20,
    "studyCount": 100,
    "creatorName": "系统",
    "createTime": "2024-01-01 12:00:00",
    "questions": [
      {
        "id": 1,
        "title": "什么是 Java 的多态？",
        "difficulty": 2,
        "category": "Java"
      }
    ]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/interview/question-sets/1"
```

### 1.3 获取随机题目

```
GET /interview/question-sets/{id}/random
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 题单 ID |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| count | Integer | 否 | 题目数量（默认 1，最大 10） |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "title": "什么是 Java 的多态？",
      "content": "请详细解释 Java 的多态特性...",
      "answer": "多态是指...",
      "difficulty": 2,
      "category": "Java"
    }
  ],
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/interview/question-sets/1/random?count=5"
```

---

## 二、面试掌握度（InterviewMasteryController）

**模块**：`xiaou-interview`
**路由前缀**：`/interview/mastery`
**权限要求**：需用户登录

### 2.1 标记掌握度

```
POST /interview/mastery
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| questionId | Long | 是 | 题目 ID |
| questionSetId | Long | 否 | 题单 ID |
| masteryLevel | Integer | 是 | 掌握度：1-不会 2-模糊 3-熟悉 4-已掌握 |

**请求示例**：

```json
{
  "questionId": 1,
  "questionSetId": 1,
  "masteryLevel": 3
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "questionId": 1,
    "masteryLevel": 3,
    "masteryLevelText": "熟悉",
    "reviewCount": 1,
    "lastReviewTime": "2024-01-01 12:00:00",
    "nextReviewTime": "2024-01-03 12:00:00",
    "nextReviewDays": 2,
    "isOverdue": false
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/interview/mastery \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"questionId":1,"questionSetId":1,"masteryLevel":3}'
```

### 2.2 获取掌握度信息

```
GET /interview/mastery/{questionId}
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| questionId | Long | 是 | 题目 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "questionId": 1,
    "masteryLevel": 3,
    "masteryLevelText": "熟悉",
    "reviewCount": 1,
    "lastReviewTime": "2024-01-01 12:00:00",
    "nextReviewTime": "2024-01-03 12:00:00",
    "nextReviewDays": 2,
    "isOverdue": false
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/interview/mastery/1" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 2.3 获取复习统计

```
GET /interview/mastery/review-stats
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "overdueCount": 5,
    "todayCount": 10,
    "weekCount": 30,
    "totalLearned": 100,
    "level1Count": 20,
    "level2Count": 30,
    "level3Count": 35,
    "level4Count": 15
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/interview/mastery/review-stats" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 2.4 获取复习列表

```
GET /interview/mastery/review-list
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | String | 否 | 类型：overdue-逾期 today-今日 week-本周 all-全部（默认 all） |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "questionId": 1,
      "questionTitle": "什么是 Java 的多态？",
      "masteryLevel": 2,
      "masteryLevelText": "模糊",
      "reviewCount": 1,
      "lastReviewTime": "2024-01-01 12:00:00",
      "nextReviewTime": "2024-01-02 12:00:00",
      "isOverdue": true
    }
  ],
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/interview/mastery/review-list?type=overdue" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 2.5 获取学习热力图

```
GET /interview/mastery/heatmap
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| year | Integer | 否 | 年份（默认当前年） |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "year": 2024,
    "totalDays": 100,
    "currentStreak": 5,
    "longestStreak": 30,
    "monthStats": {
      "2024-01": 20,
      "2024-02": 15,
      "2024-03": 25
    },
    "dailyData": [
      {
        "date": "2024-01-01",
        "count": 5,
        "level": 2,
        "learnCount": 3,
        "reviewCount": 2
      }
    ]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/interview/mastery/heatmap?year=2024" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 三、模拟面试（MockInterviewController）

**模块**：`xiaou-mock-interview`
**路由前缀**：`/user/mock-interview`
**权限要求**：需用户登录

### 3.1 获取面试配置

```
GET /user/mock-interview/config
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "levels": [
      {
        "code": 1,
        "name": "初级",
        "description": "适合 1-3 年经验"
      },
      {
        "code": 2,
        "name": "中级",
        "description": "适合 3-5 年经验"
      }
    ],
    "types": [
      {
        "code": 1,
        "name": "技术面试",
        "description": "考察技术能力"
      }
    ],
    "styles": [
      {
        "code": 1,
        "name": "温和",
        "description": "友好引导型"
      }
    ],
    "questionCounts": [5, 8, 10],
    "questionModes": [
      {
        "code": 1,
        "name": "本地题库",
        "description": "从题库中抽取"
      },
      {
        "code": 2,
        "name": "AI 出题",
        "description": "AI 实时生成"
      }
    ]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/user/mock-interview/config" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 3.2 创建面试会话

```
POST /user/mock-interview/create
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| direction | String | 是 | 面试方向（如 Java、前端） |
| level | Integer | 是 | 难度级别：1-初级 2-中级 3-高级 |
| interviewType | Integer | 否 | 面试类型（默认 1） |
| style | Integer | 否 | AI 风格（默认 2） |
| questionCount | Integer | 否 | 题目数量（默认 5） |
| questionMode | Integer | 否 | 出题模式：1-本地题库 2-AI出题（默认 2） |
| questionSetIds | List<Long> | 否 | 题单 ID 列表（本地题库模式） |

**请求示例**：

```json
{
  "direction": "Java",
  "level": 2,
  "interviewType": 1,
  "style": 2,
  "questionCount": 5,
  "questionMode": 2
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "sessionId": 1,
    "direction": "Java",
    "level": 2,
    "levelText": "中级",
    "interviewType": 1,
    "style": 2,
    "questionCount": 5,
    "questionMode": 2,
    "status": 1,
    "statusText": "进行中",
    "createTime": "2024-01-01 12:00:00"
  },
  "timestamp": 1710000000000
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 600 | 您有正在进行的面试，请先完成或结束当前面试 |
| 600 | 没有找到符合条件的题目 |

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/user/mock-interview/create \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"direction":"Java","level":2,"questionCount":5,"questionMode":2}'
```

### 3.3 获取面试会话详情

```
GET /user/mock-interview/{sessionId}
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | Long | 是 | 会话 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "sessionId": 1,
    "direction": "Java",
    "level": 2,
    "levelText": "中级",
    "interviewType": 1,
    "style": 2,
    "questionCount": 5,
    "questionMode": 2,
    "status": 1,
    "statusText": "进行中",
    "currentQuestionOrder": 1,
    "durationMinutes": 30,
    "createTime": "2024-01-01 12:00:00",
    "qaList": [
      {
        "id": 1,
        "questionOrder": 1,
        "questionContent": "请解释 Java 中的多态性",
        "questionType": 1,
        "status": 1,
        "statusText": "待回答"
      }
    ]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/user/mock-interview/1" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 3.4 提交答案

```
POST /user/mock-interview/submit
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | Long | 是 | 会话 ID |
| qaId | Long | 是 | 问题 ID |
| answer | String | 是 | 回答内容 |

**请求示例**：

```json
{
  "sessionId": 1,
  "qaId": 1,
  "answer": "多态是指同一个接口可以有多种不同的实现方式..."
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "qaId": 1,
    "score": 85,
    "aiFeedback": "回答很好地解释了多态的概念...",
    "nextQuestion": {
      "id": 2,
      "questionOrder": 2,
      "questionContent": "请解释 Java 中的抽象类和接口的区别",
      "questionType": 1,
      "status": 1,
      "statusText": "待回答"
    }
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/user/mock-interview/submit \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"sessionId":1,"qaId":1,"answer":"多态是指同一个接口可以有多种不同的实现方式..."}'
```

### 3.5 结束面试

```
POST /user/mock-interview/{sessionId}/end
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | Long | 是 | 会话 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "sessionId": 1,
    "totalScore": 85,
    "totalQuestions": 5,
    "answeredQuestions": 4,
    "averageScore": 82,
    "aiSummary": "本次面试表现良好...",
    "aiSuggestion": "建议加强...",
    "createTime": "2024-01-01 12:00:00",
    "endTime": "2024-01-01 12:30:00"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/user/mock-interview/1/end \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 3.6 获取面试历史

```
GET /user/mock-interview/history
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| direction | String | 否 | 面试方向筛选 |
| status | Integer | 否 | 状态筛选 |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 20,
    "totalPages": 2,
    "records": [
      {
        "sessionId": 1,
        "direction": "Java",
        "level": 2,
        "levelText": "中级",
        "status": 2,
        "statusText": "已完成",
        "totalScore": 85,
        "questionCount": 5,
        "answeredQuestions": 5,
        "createTime": "2024-01-01 12:00:00",
        "endTime": "2024-01-01 12:30:00"
      }
    ],
    "hasNext": true,
    "hasPrevious": false
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/user/mock-interview/history?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 四、计划打卡（UserPlanController）

**模块**：`xiaou-plan`
**路由前缀**：`/user/plan`
**权限要求**：需用户登录

### 4.1 创建计划

```
POST /user/plan
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| planName | String | 是 | 计划名称 |
| planDesc | String | 否 | 计划描述 |
| planType | Integer | 是 | 计划类型：1-学习 2-运动 3-阅读 4-其他 |
| targetType | Integer | 否 | 目标类型（默认 1） |
| targetValue | Integer | 否 | 目标值（默认 1） |
| targetUnit | String | 否 | 目标单位（默认"次"） |
| startDate | String | 是 | 开始日期（yyyy-MM-dd） |
| endDate | String | 否 | 结束日期（yyyy-MM-dd） |
| dailyStartTime | String | 否 | 每日开始时间（HH:mm） |
| dailyEndTime | String | 否 | 每日结束时间（HH:mm） |
| repeatType | Integer | 否 | 重复类型：1-每天 2-工作日 3-周末 4-自定义 |
| repeatDays | String | 否 | 重复日期（逗号分隔，如"1,3,5"） |
| remindBefore | Integer | 否 | 提前提醒分钟数（默认 30） |
| remindDeadline | Integer | 否 | 截止提醒分钟数（默认 10） |
| remindEnabled | Integer | 否 | 是否启用提醒（默认 1） |

**请求示例**：

```json
{
  "planName": "每天刷 10 道算法题",
  "planDesc": "提升算法能力",
  "planType": 1,
  "targetType": 1,
  "targetValue": 10,
  "targetUnit": "道",
  "startDate": "2024-01-01",
  "endDate": "2024-03-31",
  "repeatType": 1,
  "remindEnabled": 1
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "planName": "每天刷 10 道算法题",
    "planDesc": "提升算法能力",
    "planType": 1,
    "planTypeText": "学习",
    "status": 1,
    "statusText": "进行中",
    "startDate": "2024-01-01",
    "endDate": "2024-03-31",
    "totalCheckinDays": 0,
    "currentStreak": 0,
    "maxStreak": 0,
    "createTime": "2024-01-01 12:00:00"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/user/plan \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"planName":"每天刷10道算法题","planType":1,"startDate":"2024-01-01","repeatType":1}'
```

### 4.2 打卡

```
POST /user/plan/{planId}/checkin
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| planId | Long | 是 | 计划 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "planId": 1,
    "checkinDate": "2024-01-01",
    "continuousDays": 1,
    "totalCheckinDays": 1,
    "pointsEarned": 10,
    "isTodayFirst": true
  },
  "timestamp": 1710000000000
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 600 | 今日已打卡，请勿重复操作 |
| 600 | 计划不存在 |
| 600 | 计划未在进行中 |

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/user/plan/1/checkin \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 4.3 获取计划列表

```
GET /user/plan/list
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| status | Integer | 否 | 状态筛选：1-进行中 2-已暂停 3-已完成 |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 5,
    "totalPages": 1,
    "records": [
      {
        "id": 1,
        "planName": "每天刷 10 道算法题",
        "planDesc": "提升算法能力",
        "planType": 1,
        "planTypeText": "学习",
        "status": 1,
        "statusText": "进行中",
        "startDate": "2024-01-01",
        "endDate": "2024-03-31",
        "totalCheckinDays": 10,
        "currentStreak": 5,
        "maxStreak": 10,
        "todayCheckedIn": true,
        "createTime": "2024-01-01 12:00:00"
      }
    ],
    "hasNext": false,
    "hasPrevious": false
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/user/plan/list?pageNum=1&pageSize=10&status=1" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 4.4 暂停计划

```
PUT /user/plan/{planId}/pause
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| planId | Long | 是 | 计划 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X PUT http://localhost:9999/api/user/plan/1/pause \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 4.5 恢复计划

```
PUT /user/plan/{planId}/resume
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| planId | Long | 是 | 计划 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X PUT http://localhost:9999/api/user/plan/1/resume \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 五、闪卡（FlashcardDeckController）

**模块**：`xiaou-flashcard`
**路由前缀**：`/flashcard/deck`
**权限要求**：需用户登录

### 5.1 创建卡组

```
POST /flashcard/deck
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 卡组名称 |
| description | String | 否 | 卡组描述 |
| coverImage | String | 否 | 封面图片 URL |
| isPublic | Integer | 否 | 是否公开：0-私有 1-公开（默认 0） |
| tags | String | 否 | 标签（逗号分隔） |

**请求示例**：

```json
{
  "name": "Java 基础闪卡",
  "description": "Java 核心知识点",
  "isPublic": 1,
  "tags": "Java,基础"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "name": "Java 基础闪卡",
    "description": "Java 核心知识点",
    "isPublic": 1,
    "tags": "Java,基础",
    "cardCount": 0,
    "studyCount": 0,
    "forkCount": 0,
    "createTime": "2024-01-01 12:00:00"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/flashcard/deck \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"name":"Java 基础闪卡","description":"Java 核心知识点","isPublic":1}'
```

### 5.2 获取卡组列表

```
GET /flashcard/deck/list
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| keyword | String | 否 | 关键词搜索 |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 5,
    "totalPages": 1,
    "records": [
      {
        "id": 1,
        "name": "Java 基础闪卡",
        "description": "Java 核心知识点",
        "isPublic": 1,
        "tags": "Java,基础",
        "cardCount": 20,
        "studyCount": 50,
        "forkCount": 5,
        "createTime": "2024-01-01 12:00:00"
      }
    ],
    "hasNext": false,
    "hasPrevious": false
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/flashcard/deck/list?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 5.3 添加卡片

```
POST /flashcard/card
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| deckId | Long | 是 | 卡组 ID |
| frontContent | String | 是 | 正面内容 |
| backContent | String | 是 | 背面内容 |
| contentType | Integer | 否 | 内容类型：1-文本 2-Markdown 3-代码（默认 1） |
| tags | String | 否 | 标签（逗号分隔） |

**请求示例**：

```json
{
  "deckId": 1,
  "frontContent": "什么是 Java 的多态？",
  "backContent": "多态是指同一个接口可以有多种不同的实现方式...",
  "contentType": 1,
  "tags": "Java,多态"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "deckId": 1,
    "frontContent": "什么是 Java 的多态？",
    "backContent": "多态是指同一个接口可以有多种不同的实现方式...",
    "contentType": 1,
    "tags": "Java,多态",
    "createTime": "2024-01-01 12:00:00"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/flashcard/card \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"deckId":1,"frontContent":"什么是 Java 的多态？","backContent":"多态是指..."}'
```

### 5.4 获取今日学习卡片

```
GET /flashcard/study/today
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| limit | Integer | 否 | 数量限制（默认 20） |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "deckId": 1,
      "deckName": "Java 基础闪卡",
      "frontContent": "什么是 Java 的多态？",
      "backContent": "多态是指同一个接口可以有多种不同的实现方式...",
      "contentType": 1,
      "masteryLevel": 0,
      "isReview": false
    }
  ],
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/flashcard/study/today?limit=10" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 5.5 提交学习结果

```
POST /flashcard/study/submit
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| cardId | Long | 是 | 卡片 ID |
| quality | Integer | 是 | 评分：1-完全不记得 2-模糊 3-记得 4-很熟悉 |

**请求示例**：

```json
{
  "cardId": 1,
  "quality": 3
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "cardId": 1,
    "masteryLevel": 2,
    "masteryLevelText": "学习中",
    "reviewCount": 1,
    "nextReviewTime": "2024-01-03 12:00:00",
    "nextReviewDays": 2,
    "isCorrect": true
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/flashcard/study/submit \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"cardId":1,"quality":3}'
```

---

## 六、知识图谱（PubKnowledgeMapController）

**模块**：`xiaou-knowledge`
**路由前缀**：`/pub/knowledge/maps`
**权限要求**：无需登录

### 6.1 获取已发布图谱列表

```
GET /pub/knowledge/maps
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| keyword | String | 否 | 关键词搜索 |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 5,
    "totalPages": 1,
    "records": [
      {
        "id": 1,
        "title": "Java 知识图谱",
        "description": "涵盖 Java 核心知识点",
        "coverImage": "/files/knowledge/1.png",
        "nodeCount": 50,
        "viewCount": 100,
        "createTime": "2024-01-01 12:00:00"
      }
    ],
    "hasNext": false,
    "hasPrevious": false
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/pub/knowledge/maps?pageNum=1&pageSize=10"
```

### 6.2 获取图谱详情

```
GET /pub/knowledge/maps/{id}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 图谱 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "title": "Java 知识图谱",
    "description": "涵盖 Java 核心知识点",
    "coverImage": "/files/knowledge/1.png",
    "nodeCount": 50,
    "viewCount": 101,
    "nodes": [
      {
        "id": 1,
        "title": "Java 基础",
        "parentId": null,
        "level": 0,
        "sortOrder": 1
      },
      {
        "id": 2,
        "title": "面向对象",
        "parentId": 1,
        "level": 1,
        "sortOrder": 1
      }
    ],
    "createTime": "2024-01-01 12:00:00"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/pub/knowledge/maps/1"
```

---

## 相关文档

| 文档 | 说明 |
|------|------ |
| [题库与成长闭环](/modules/interview-and-growth) | 学习成长模块详解 |
| [面试题库](/modules/interview) | 面试题库详解 |
| [模拟面试与求职作战台](/modules/mock-interview-job-battle) | 模拟面试详解 |
| [闪卡](/modules/flashcard) | 闪卡模块详解 |
| [知识图谱](/modules/knowledge) | 知识图谱详解 |
| [计划与学习小组](/modules/plan-team) | 计划打卡详解 |
| [响应体与错误码](/reference/response-errors) | 完整错误码列表 |
| [API 路由索引](/reference/api-routes) | 完整接口清单 |
