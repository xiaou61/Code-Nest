# OJ 判题 API

本文档详细说明 OJ 判题模块的 API 接口，包括题目管理、代码提交、判题、赛事等。

## 路由约定

| 前缀 | 使用场景 | 登录域 |
| --- | --- | --- |
| `/oj` | OJ 公开接口 | 按接口判断 |
| `/admin/oj` | 管理端接口 | 管理员登录态 |

---

## 一、题目公开接口（OjProblemPublicController）

**模块**：`xiaou-oj`
**路由前缀**：`/oj`
**权限要求**：部分接口需用户登录

### 1.1 获取题目列表

```
GET /oj/problems/list
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| difficulty | String | 否 | 难度：easy/medium/hard |
| tagId | Long | 否 | 标签 ID |
| keyword | String | 否 | 关键词搜索 |
| status | String | 否 | 状态筛选：unsolved/solved |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 100,
    "totalPages": 10,
    "records": [
      {
        "id": 1,
        "title": "两数之和",
        "difficulty": "easy",
        "difficultyText": "简单",
        "acceptCount": 1000,
        "submitCount": 2000,
        "acceptRate": 50.0,
        "tags": ["数组", "哈希表"],
        "isSolved": false
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
curl "http://localhost:9999/api/oj/problems/list?pageNum=1&pageSize=10&difficulty=easy"
```

### 1.2 获取题目详情

```
GET /oj/problems/{id}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 题目 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "title": "两数之和",
    "description": "给定一个整数数组 nums 和一个目标值 target...",
    "inputFormat": "第一行包含数组长度 n...",
    "outputFormat": "输出两个数的下标...",
    "sampleInput": "[2, 7, 11, 15]\n9",
    "sampleOutput": "[0, 1]",
    "difficulty": "easy",
    "difficultyText": "简单",
    "timeLimit": 1000,
    "memoryLimit": 256,
    "acceptCount": 1000,
    "submitCount": 2000,
    "acceptRate": 50.0,
    "tags": ["数组", "哈希表"],
    "isSolved": false,
    "hints": ["可以使用哈希表存储已遍历的元素"]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/oj/problems/1"
```

### 1.3 获取每日一题

```
GET /oj/problems/daily
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 5,
    "title": "最长回文子串",
    "difficulty": "medium",
    "difficultyText": "中等",
    "tags": ["字符串", "动态规划"],
    "date": "2024-01-15"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/oj/problems/daily"
```

### 1.4 获取标签列表

```
GET /oj/problems/tags
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "name": "数组",
      "problemCount": 50
    },
    {
      "id": 2,
      "name": "链表",
      "problemCount": 30
    }
  ],
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/oj/problems/tags"
```

### 1.5 获取排行榜

```
GET /oj/ranking
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 20） |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "pageNum": 1,
    "pageSize": 20,
    "total": 100,
    "totalPages": 5,
    "records": [
      {
        "rank": 1,
        "userId": 1,
        "nickname": "算法大神",
        "avatar": "/files/avatar/1.png",
        "solvedCount": 200,
        "acceptRate": 85.5,
        "totalScore": 5000
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
curl "http://localhost:9999/api/oj/ranking?pageNum=1&pageSize=20"
```

---

## 二、代码提交（OjSubmissionController）

**模块**：`xiaou-oj`
**路由前缀**：`/oj`
**权限要求**：需用户登录

### 2.1 运行代码（Playground）

```
POST /oj/run
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| language | String | 是 | 语言：java/python/cpp/go/javascript |
| code | String | 是 | 代码内容 |
| input | String | 否 | 输入数据 |

**请求示例**：

```json
{
  "language": "java",
  "code": "import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int a = sc.nextInt();\n        int b = sc.nextInt();\n        System.out.println(a + b);\n    }\n}",
  "input": "1 2"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "status": "accepted",
    "statusText": "运行成功",
    "stdout": "3\n",
    "stderr": "",
    "timeUsed": 100,
    "memoryUsed": 10240
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/oj/run \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"language":"java","code":"import java.util.Scanner;...","input":"1 2"}'
```

### 2.2 提交代码

```
POST /oj/submit
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| problemId | Long | 是 | 题目 ID |
| language | String | 是 | 语言：java/python/cpp/go/javascript |
| code | String | 是 | 代码内容 |
| contestId | Long | 否 | 赛事 ID（赛事提交时必填） |

**请求示例**：

```json
{
  "problemId": 1,
  "language": "java",
  "code": "import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int a = sc.nextInt();\n        int b = sc.nextInt();\n        System.out.println(a + b);\n    }\n}"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 1,
  "timestamp": 1710000000000
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 600 | 不支持的语言 |
| 600 | 题目不存在 |
| 600 | 赛事提交校验失败 |

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/oj/submit \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"problemId":1,"language":"java","code":"import java.util.Scanner;..."}'
```

### 2.3 获取提交详情

```
GET /oj/submissions/{id}
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 提交 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "problemId": 1,
    "problemTitle": "两数之和",
    "userId": 1,
    "language": "java",
    "code": "import java.util.Scanner;...",
    "status": "accepted",
    "statusText": "通过",
    "timeUsed": 100,
    "memoryUsed": 10240,
    "score": 100,
    "pointsEarned": 100,
    "submitTime": "2024-01-01 12:00:00",
    "judgeTime": "2024-01-01 12:00:01"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/oj/submissions/1" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 2.4 获取我的提交列表

```
GET /oj/submissions/my
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
| problemId | Long | 否 | 题目 ID |
| status | String | 否 | 状态筛选 |
| language | String | 否 | 语言筛选 |

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
        "problemId": 1,
        "problemTitle": "两数之和",
        "language": "java",
        "status": "accepted",
        "statusText": "通过",
        "timeUsed": 100,
        "memoryUsed": 10240,
        "submitTime": "2024-01-01 12:00:00"
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
curl "http://localhost:9999/api/oj/submissions/my?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 2.5 获取我的刷题统计

```
GET /oj/statistics/me
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
    "totalSolved": 50,
    "easySolved": 20,
    "mediumSolved": 25,
    "hardSolved": 5,
    "totalSubmitted": 100,
    "acceptRate": 50.0,
    "totalScore": 2000,
    "ranking": 100,
    "streak": 5
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/oj/statistics/me" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 三、赛事接口（OjContestPublicController）

**模块**：`xiaou-oj`
**路由前缀**：`/oj/contests`
**权限要求**：部分接口需用户登录

### 3.1 获取赛事列表

```
GET /oj/contests
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| status | String | 否 | 状态：upcoming/ongoing/ended |

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
        "title": "周赛 #1",
        "description": "每周算法竞赛",
        "startTime": "2024-01-20 10:00:00",
        "endTime": "2024-01-20 12:00:00",
        "status": "upcoming",
        "statusText": "未开始",
        "participantCount": 100,
        "problemCount": 4,
        "isRegistered": false
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
curl "http://localhost:9999/api/oj/contests?pageNum=1&pageSize=10"
```

### 3.2 获取赛事详情

```
GET /oj/contests/{id}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 赛事 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "title": "周赛 #1",
    "description": "每周算法竞赛",
    "startTime": "2024-01-20 10:00:00",
    "endTime": "2024-01-20 12:00:00",
    "status": "ongoing",
    "statusText": "进行中",
    "participantCount": 100,
    "problemCount": 4,
    "isRegistered": true,
    "problems": [
      {
        "id": 1,
        "title": "A. 两数之和",
        "difficulty": "easy",
        "acceptCount": 80,
        "submitCount": 100
      }
    ],
    "rules": {
      "type": "ACM",
      "penalty": 20,
      "freezeTime": 30
    }
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/oj/contests/1"
```

### 3.3 报名赛事

```
POST /oj/contests/{id}/register
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 赛事 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1710000000000
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 600 | 赛事不存在 |
| 600 | 赛事已结束 |
| 600 | 已经报名 |

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/oj/contests/1/register \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 3.4 获取赛事榜单

```
GET /oj/contests/{id}/ranking
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 赛事 ID |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 50） |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "contestId": 1,
    "contestTitle": "周赛 #1",
    "type": "ACM",
    "totalParticipants": 100,
    "ranking": [
      {
        "rank": 1,
        "userId": 1,
        "nickname": "算法大神",
        "avatar": "/files/avatar/1.png",
        "solvedCount": 4,
        "totalTime": 120,
        "penalty": 0,
        "problems": [
          {
            "problemId": 1,
            "status": "accepted",
            "attempts": 1,
            "time": 15
          }
        ]
      }
    ]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/oj/contests/1/ranking?pageNum=1&pageSize=50"
```

---

## 四、评论接口（OjCommentController）

**模块**：`xiaou-oj`
**路由前缀**：`/oj`
**权限要求**：部分接口需用户登录

### 4.1 获取题目评论

```
GET /oj/problems/{problemId}/comments
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| problemId | Long | 是 | 题目 ID |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |

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
        "id": 1,
        "problemId": 1,
        "userId": 1,
        "nickname": "用户A",
        "avatar": "/files/avatar/1.png",
        "content": "这道题可以用动态规划解决...",
        "likeCount": 10,
        "isLiked": false,
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
curl "http://localhost:9999/api/oj/problems/1/comments?pageNum=1&pageSize=10"
```

### 4.2 发表评论

```
POST /oj/problems/{problemId}/comments
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| problemId | Long | 是 | 题目 ID |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| content | String | 是 | 评论内容 |

**请求示例**：

```json
{
  "content": "这道题可以用动态规划解决，时间复杂度 O(n)..."
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "problemId": 1,
    "userId": 1,
    "nickname": "用户A",
    "avatar": "/files/avatar/1.png",
    "content": "这道题可以用动态规划解决，时间复杂度 O(n)...",
    "likeCount": 0,
    "isLiked": false,
    "createTime": "2024-01-01 12:00:00"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/oj/problems/1/comments \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"content":"这道题可以用动态规划解决..."}'
```

### 4.3 点赞/取消点赞评论

```
POST /oj/comments/{commentId}/like
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| commentId | Long | 是 | 评论 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": true,
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/oj/comments/1/like \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 五、管理端题目管理（OjProblemController）

**模块**：`xiaou-oj`
**路由前缀**：`/admin/oj/problems`
**权限要求**：需管理员登录

### 5.1 创建题目

```
POST /admin/oj/problems
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 是 | 题目标题 |
| description | String | 是 | 题目描述 |
| inputFormat | String | 否 | 输入格式说明 |
| outputFormat | String | 否 | 输出格式说明 |
| sampleInput | String | 否 | 样例输入 |
| sampleOutput | String | 否 | 样例输出 |
| difficulty | String | 是 | 难度：easy/medium/hard |
| timeLimit | Integer | 否 | 时间限制（毫秒，默认 1000） |
| memoryLimit | Integer | 否 | 内存限制（MB，默认 256） |
| tags | List&lt;Long&gt; | 否 | 标签 ID 列表 |
| hints | List&lt;String&gt; | 否 | 提示列表 |

**请求示例**：

```json
{
  "title": "两数之和",
  "description": "给定一个整数数组 nums 和一个目标值 target...",
  "inputFormat": "第一行包含数组长度 n...",
  "outputFormat": "输出两个数的下标...",
  "sampleInput": "[2, 7, 11, 15]\n9",
  "sampleOutput": "[0, 1]",
  "difficulty": "easy",
  "timeLimit": 1000,
  "memoryLimit": 256,
  "tags": [1, 2],
  "hints": ["可以使用哈希表存储已遍历的元素"]
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 1,
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/admin/oj/problems \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"title":"两数之和","description":"...","difficulty":"easy"}'
```

### 5.2 更新题目

```
PUT /admin/oj/problems/{id}
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 题目 ID |

**请求参数**：同创建题目

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
curl -X PUT http://localhost:9999/api/admin/oj/problems/1 \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"title":"两数之和（更新）","difficulty":"easy"}'
```

### 5.3 删除题目

```
DELETE /admin/oj/problems/{id}
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 题目 ID |

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
curl -X DELETE http://localhost:9999/api/admin/oj/problems/1 \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 六、管理端赛事管理（OjContestController）

**模块**：`xiaou-oj`
**路由前缀**：`/admin/oj/contests`
**权限要求**：需管理员登录

### 6.1 创建赛事

```
POST /admin/oj/contests
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 是 | 赛事标题 |
| description | String | 否 | 赛事描述 |
| startTime | String | 是 | 开始时间（yyyy-MM-dd HH:mm:ss） |
| endTime | String | 是 | 结束时间（yyyy-MM-dd HH:mm:ss） |
| type | String | 否 | 类型：ACM/OI（默认 ACM） |
| problemIds | List&lt;Long&gt; | 否 | 题目 ID 列表 |
| rules | Object | 否 | 赛事规则 |

**请求示例**：

```json
{
  "title": "周赛 #1",
  "description": "每周算法竞赛",
  "startTime": "2024-01-20 10:00:00",
  "endTime": "2024-01-20 12:00:00",
  "type": "ACM",
  "problemIds": [1, 2, 3, 4]
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 1,
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/admin/oj/contests \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"title":"周赛 #1","startTime":"2024-01-20 10:00:00","endTime":"2024-01-20 12:00:00"}'
```

---

## 相关文档

| 文档 | 说明 |
|------|------ |
| [OJ 判题系统](/modules/oj) | OJ 模块详解 |
| [积分与抽奖](/modules/points) | 首次 AC 积分奖励 |
| [响应体与错误码](/reference/response-errors) | 完整错误码列表 |
| [API 路由索引](/reference/api-routes) | 完整接口清单 |
