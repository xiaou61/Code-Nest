# 平台能力 API

本文档详细说明平台能力模块的 API 接口，包括积分、文件存储、通知、敏感词、聊天等。

## 路由约定

| 前缀 | 使用场景 | 登录域 |
| --- | --- | --- |
| `/user/points` | 用户端积分接口 | 用户登录态 |
| `/user/lottery` | 用户端抽奖接口 | 用户登录态 |
| `/file` | 文件接口 | 按接口判断 |
| `/notification` | 通知接口 | 用户登录态 |
| `/sensitive` | 敏感词接口 | 按接口判断 |
| `/user/chat` | 用户端聊天接口 | 用户登录态 |
| `/admin/**` | 管理端接口 | 管理员登录态 |

---

## 一、积分（UserPointsController）

**模块**：`xiaou-points`
**路由前缀**：`/user/points`
**权限要求**：需用户登录

### 1.1 查询积分余额

```
GET /user/points/balance
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
    "userId": 1,
    "totalPoints": 1000,
    "totalPointsYuan": "1.00",
    "createTime": "2024-01-01 12:00:00"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/user/points/balance" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 1.2 每日签到

```
POST /user/points/checkin
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
    "checkinDate": "2024-01-15",
    "continuousDays": 5,
    "totalCheckinDays": 30,
    "pointsEarned": 90,
    "isTodayFirst": true
  },
  "timestamp": 1710000000000
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 600 | 今日已打卡，请勿重复操作 |

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/user/points/checkin \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 1.3 获取积分明细

```
GET /user/points/detail
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
| pointsType | Integer | 否 | 积分类型：1-后台发放 2-打卡 3-抽奖消耗 4-抽奖奖励 5-OJ通过 |

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
        "userId": 1,
        "pointsChange": 50,
        "pointsType": 2,
        "pointsTypeText": "打卡积分",
        "description": "每日签到",
        "balanceAfter": 1050,
        "createTime": "2024-01-15 12:00:00"
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
curl "http://localhost:9999/api/user/points/detail?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 1.4 获取签到日历

```
GET /user/points/checkin-calendar
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| yearMonth | String | 否 | 年月（yyyy-MM，默认当月） |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "yearMonth": "2024-01",
    "checkinDays": [1, 2, 3, 5, 7, 8, 10, 15],
    "checkinBitmap": 16843,
    "continuousDays": 5,
    "totalCheckinDays": 8,
    "lastCheckinDate": "2024-01-15",
    "monthlyStats": {
      "totalDays": 31,
      "checkinRate": "25.81%",
      "pointsEarned": 400
    }
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/user/points/checkin-calendar?yearMonth=2024-01" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 二、抽奖（UserLotteryController）

**模块**：`xiaou-points`
**路由前缀**：`/user/lottery`
**权限要求**：需用户登录

### 2.1 获取可抽奖品列表

```
GET /user/lottery/prizes
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
  "data": [
    {
      "id": 1,
      "name": "一等奖",
      "description": "1000 积分",
      "prizeLevel": 1,
      "prizeLevelText": "一等奖",
      "prizePoints": 1000,
      "totalStock": 10,
      "currentStock": 5,
      "baseProbability": 0.01,
      "currentProbability": 0.01
    }
  ],
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/user/lottery/prizes" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 2.2 抽奖

```
POST /user/lottery/draw
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| strategyType | String | 否 | 策略类型（默认 ALIAS_METHOD） |

**请求示例**：

```json
{
  "strategyType": "ALIAS_METHOD"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "recordId": 1,
    "prizeId": 1,
    "prizeName": "一等奖",
    "prizeLevel": 1,
    "prizeLevelText": "一等奖",
    "prizePoints": 1000,
    "isWin": true,
    "pointsCost": 100,
    "pointsRemaining": 900,
    "drawTime": "2024-01-15 12:00:00"
  },
  "timestamp": 1710000000000
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 600 | 今日抽奖次数已用完 |
| 600 | 积分不足 |
| 600 | 操作过于频繁，请稍后再试 |
| 600 | 奖品库存不足 |
| 600 | 系统维护中，暂停抽奖服务 |

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/user/lottery/draw \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"strategyType":"ALIAS_METHOD"}'
```

### 2.3 获取抽奖记录

```
GET /user/lottery/records
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
        "userId": 1,
        "prizeId": 1,
        "prizeName": "一等奖",
        "prizeLevel": 1,
        "prizePoints": 1000,
        "isWin": true,
        "pointsCost": 100,
        "drawTime": "2024-01-15 12:00:00"
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
curl "http://localhost:9999/api/user/lottery/records?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 2.4 获取今日剩余抽奖次数

```
GET /user/lottery/remaining-count
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
    "maxDailyCount": 10,
    "todayUsedCount": 3,
    "remainingCount": 7
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/user/lottery/remaining-count" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 三、文件存储（FileController）

**模块**：`xiaou-filestorage`
**路由前缀**：`/file`
**权限要求**：按接口判断

### 3.1 上传文件

```
POST /file/upload/single
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |
| Content-Type | multipart/form-data |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 文件 |
| moduleName | String | 是 | 模块名称（如 user、community、blog） |
| businessType | String | 否 | 业务类型（如 avatar、image、attachment） |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "success": true,
    "storagePath": "user/avatar/1/abc123.png",
    "accessUrl": "/files/user/avatar/1/abc123.png",
    "fileSize": 102400,
    "errorMessage": null
  },
  "timestamp": 1710000000000
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 801 | 文件不能为空 |
| 804 | 不支持的文件类型 |
| 805 | 文件大小超出限制（最大 100MB） |

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/file/upload/single \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -F "file=@/path/to/image.png" \
  -F "moduleName=user" \
  -F "businessType=avatar"
```

### 3.2 下载文件

```
GET /file/download/{id}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 文件 ID |

**响应**：返回文件流

**curl 示例**：

```bash
curl "http://localhost:9999/api/file/download/1" --output file.png
```

### 3.3 获取文件信息

```
GET /file/info/{id}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 文件 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "originalName": "image.png",
    "fileSize": 102400,
    "contentType": "image/png",
    "uploadTime": "2024-01-01 12:00:00",
    "accessUrl": "/files/user/avatar/1/abc123.png"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/file/info/1"
```

---

## 四、通知（NotificationController）

**模块**：`xiaou-notification`
**路由前缀**：`/notification`
**权限要求**：需用户登录

### 4.1 获取通知列表

```
POST /notification/list
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
| status | String | 否 | 状态：UNREAD/READ |
| type | String | 否 | 类型：ANNOUNCEMENT/PERSONAL/TEMPLATE |

**请求示例**：

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "status": "UNREAD"
}
```

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
        "title": "欢迎加入 Code Nest",
        "content": "感谢您注册 Code Nest 学习社区！",
        "type": "PERSONAL",
        "typeText": "个人消息",
        "status": "UNREAD",
        "statusText": "未读",
        "priority": "LOW",
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
curl -X POST http://localhost:9999/api/notification/list \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"pageNum":1,"pageSize":10,"status":"UNREAD"}'
```

### 4.2 获取未读数量

```
GET /notification/unread-count
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
  "data": 5,
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/notification/unread-count" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 4.3 标记已读

```
POST /notification/mark-read
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| notificationIds | List&lt;Long&gt; | 否 | 通知 ID 列表 |
| markAll | Boolean | 否 | 是否标记全部已读（默认 false） |

**请求示例**：

```json
{
  "notificationIds": [1, 2, 3]
}
```

或

```json
{
  "markAll": true
}
```

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
curl -X POST http://localhost:9999/api/notification/mark-read \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"notificationIds":[1,2,3]}'
```

### 4.4 删除通知

```
POST /notification/delete
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| notificationIds | List&lt;Long&gt; | 是 | 通知 ID 列表 |

**请求示例**：

```json
{
  "notificationIds": [1, 2, 3]
}
```

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
curl -X POST http://localhost:9999/api/notification/delete \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"notificationIds":[1,2,3]}'
```

---

## 五、聊天（ChatUserController）

**模块**：`xiaou-chat`
**路由前缀**：`/user/chat`
**权限要求**：需用户登录

### 5.1 获取 WebSocket 票据

```
POST /user/chat/ws-ticket
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
    "ticket": "abc123xyz",
    "expireTime": "2024-01-01 12:01:00"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/user/chat/ws-ticket \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 5.2 获取历史消息

```
GET /user/chat/messages
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 50） |
| beforeId | Long | 否 | 消息 ID（获取此 ID 之前的消息） |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "pageNum": 1,
    "pageSize": 50,
    "total": 100,
    "totalPages": 2,
    "records": [
      {
        "id": 1,
        "roomId": 1,
        "userId": 1,
        "userNickname": "用户A",
        "userAvatar": "/files/avatar/1.png",
        "messageType": 1,
        "content": "大家好！",
        "imageUrl": null,
        "replyToId": null,
        "replyToUser": null,
        "replyToContent": null,
        "isDeleted": 0,
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
curl "http://localhost:9999/api/user/chat/messages?pageNum=1&pageSize=50" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 5.3 获取在线人数

```
GET /user/chat/online-count
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
  "data": 25,
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/user/chat/online-count" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 5.4 撤回消息

```
DELETE /user/chat/messages/{messageId}
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| messageId | Long | 是 | 消息 ID |

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
| 600 | 消息不存在 |
| 600 | 只能撤回自己的消息 |
| 600 | 超过 2 分钟无法撤回 |

**curl 示例**：

```bash
curl -X DELETE http://localhost:9999/api/user/chat/messages/1 \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 六、敏感词检测（SensitiveWordController）

**模块**：`xiaou-sensitive`
**路由前缀**：`/sensitive`
**权限要求**：按接口判断

### 6.1 检测文本

```
POST /sensitive/check
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| text | String | 是 | 待检测文本 |
| module | String | 否 | 模块名称（如 community、blog、moment） |
| businessId | Long | 否 | 业务 ID |
| userId | Long | 否 | 用户 ID |

**请求示例**：

```json
{
  "text": "这是一段测试文本",
  "module": "community",
  "businessId": 1,
  "userId": 1
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "hit": false,
    "hitWords": [],
    "processedText": "这是一段测试文本",
    "riskLevel": 0,
    "action": "none",
    "allowed": true
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/sensitive/check \
  -H "Content-Type: application/json" \
  -d '{"text":"这是一段测试文本","module":"community"}'
```

### 6.2 批量检测

```
POST /sensitive/check/batch
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| texts | List&lt;String&gt; | 是 | 待检测文本列表 |
| module | String | 否 | 模块名称 |

**请求示例**：

```json
{
  "texts": ["文本1", "文本2", "文本3"],
  "module": "community"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "total": 3,
    "hitCount": 1,
    "results": [
      {
        "index": 0,
        "text": "文本1",
        "hit": false,
        "allowed": true
      },
      {
        "index": 1,
        "text": "文本2",
        "hit": true,
        "hitWords": ["敏感词"],
        "allowed": false
      },
      {
        "index": 2,
        "text": "文本3",
        "hit": false,
        "allowed": true
      }
    ]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/sensitive/check/batch \
  -H "Content-Type: application/json" \
  -d '{"texts":["文本1","文本2","文本3"],"module":"community"}'
```

---

## 相关文档

| 文档 | 说明 |
| --- | --- |
| [积分与抽奖](/modules/points) | 积分模块详解 |
| [文件存储](/modules/file-storage) | 文件存储详解 |
| [通知中心](/modules/notification) | 通知模块详解 |
| [敏感词风控](/modules/sensitive) | 敏感词模块详解 |
| [IM 聊天室](/modules/chat) | 聊天模块详解 |
| [响应体与错误码](/reference/response-errors) | 完整错误码列表 |
| [API 路由索引](/reference/api-routes) | 完整接口清单 |
