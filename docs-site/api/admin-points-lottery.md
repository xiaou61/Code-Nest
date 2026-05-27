# 管理端 API - 积分与抽奖

本文档详细说明管理端积分与抽奖模块的 API 接口，包括积分发放、抽奖管理、奖品配置、风控管理等。

## 权限要求

所有管理端接口都需要管理员登录态，使用 `@RequireAdmin` 注解保护。

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {adminToken} |

---

## 一、积分管理（AdminPointsController）

**模块**：`xiaou-points`
**路由前缀**：`/admin/points`

### 1.1 发放积分

```
POST /admin/points/grant
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户 ID |
| points | Integer | 是 | 积分数量（大于 0） |
| reason | String | 是 | 发放原因 |

**请求示例**：

```json
{
  "userId": 1,
  "points": 100,
  "reason": "活动奖励"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "detailId": 1,
    "userBalance": 1100,
    "balanceYuan": "1.10",
    "userName": "用户A"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/admin/points/grant \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"points":100,"reason":"活动奖励"}'
```

### 1.2 批量发放积分

```
POST /admin/points/batch-grant
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userIds | List<Long> | 是 | 用户 ID 列表 |
| points | Integer | 是 | 积分数量（大于 0） |
| reason | String | 是 | 发放原因 |

**请求示例**：

```json
{
  "userIds": [1, 2, 3],
  "points": 50,
  "reason": "签到活动奖励"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "successCount": 3,
    "failCount": 0,
    "totalPointsGranted": 150,
    "failedUsers": []
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/admin/points/batch-grant \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"userIds":[1,2,3],"points":50,"reason":"签到活动奖励"}'
```

### 1.3 积分明细列表

```
POST /admin/points/detail-list
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| userId | Long | 否 | 用户 ID 筛选 |
| pointsType | Integer | 否 | 积分类型：1-后台发放 2-打卡 3-抽奖消耗 4-抽奖奖励 5-OJ通过 |
| startDate | String | 否 | 开始日期（yyyy-MM-dd） |
| endDate | String | 否 | 结束日期（yyyy-MM-dd） |

**请求示例**：

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "pointsType": 1
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
    "total": 100,
    "totalPages": 10,
    "records": [
      {
        "id": 1,
        "userId": 1,
        "userName": "用户A",
        "pointsChange": 100,
        "pointsType": 1,
        "pointsTypeText": "后台发放",
        "description": "活动奖励",
        "balanceAfter": 1100,
        "adminId": 1,
        "adminName": "管理员",
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
curl -X POST http://localhost:9999/api/admin/points/detail-list \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"pageNum":1,"pageSize":10,"pointsType":1}'
```

### 1.4 用户积分列表（排行）

```
POST /admin/points/user-list
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| keyword | String | 否 | 用户名/邮箱搜索 |
| minPoints | Integer | 否 | 最小积分 |
| maxPoints | Integer | 否 | 最大积分 |
| sortBy | String | 否 | 排序字段：points/desc |

**请求示例**：

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "sortBy": "points"
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
    "total": 500,
    "totalPages": 50,
    "records": [
      {
        "userId": 1,
        "userName": "用户A",
        "userEmail": "usera@example.com",
        "totalPoints": 5000,
        "totalPointsYuan": "5.00",
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
curl -X POST http://localhost:9999/api/admin/points/user-list \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"pageNum":1,"pageSize":10,"sortBy":"points"}'
```

### 1.5 积分统计

```
GET /admin/points/statistics
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "totalPointsIssued": 1000000,
    "totalPointsIssuedYuan": "1000.00",
    "activeUserCount": 500,
    "todayIssued": 10000,
    "todayIssuedYuan": "10.00",
    "averagePointsPerUser": 2000
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/points/statistics \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 1.6 用户积分详情

```
GET /admin/points/user-info/{userId}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 1,
    "userName": "用户A",
    "totalPoints": 5000,
    "totalPointsYuan": "5.00",
    "createTime": "2024-01-01 12:00:00",
    "recentDetails": [
      {
        "id": 1,
        "pointsChange": 100,
        "pointsType": 1,
        "pointsTypeText": "后台发放",
        "description": "活动奖励",
        "createTime": "2024-01-15 12:00:00"
      }
    ]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/points/user-info/1 \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 二、抽奖管理（AdminLotteryController）

**模块**：`xiaou-points`
**路由前缀**：`/admin/lottery`

### 2.1 保存奖品配置

```
POST /admin/lottery/prize/save
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 否 | 奖品 ID（更新时） |
| name | String | 是 | 奖品名称 |
| description | String | 否 | 奖品描述 |
| prizeLevel | Integer | 是 | 奖品等级：1-特等奖 2-一等奖 ... 8-未中奖 |
| prizePoints | Integer | 否 | 奖品积分（默认 0） |
| totalStock | Integer | 否 | 总库存（null 表示无限制） |
| currentStock | Integer | 否 | 当前库存 |
| baseProbability | Double | 是 | 基础概率 |
| currentProbability | Double | 否 | 当前概率（默认等于基础概率） |
| adjustStrategy | String | 否 | 调整策略：AUTO/MANUAL（默认 MANUAL） |
| targetReturnRate | Double | 否 | 目标回报率 |
| maxReturnRate | Double | 否 | 最大回报率 |
| minReturnRate | Double | 否 | 最小回报率 |
| status | Integer | 否 | 状态：0-禁用 1-启用（默认 1） |

**请求示例**：

```json
{
  "name": "一等奖",
  "description": "1000 积分",
  "prizeLevel": 1,
  "prizePoints": 1000,
  "totalStock": 10,
  "currentStock": 10,
  "baseProbability": 0.01,
  "adjustStrategy": "AUTO",
  "targetReturnRate": 0.8,
  "maxReturnRate": 1.0,
  "minReturnRate": 0.6
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
curl -X POST http://localhost:9999/api/admin/lottery/prize/save \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"name":"一等奖","prizeLevel":1,"prizePoints":1000,"baseProbability":0.01}'
```

### 2.2 奖品列表

```
GET /admin/lottery/prize/list
```

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
      "currentProbability": 0.01,
      "adjustStrategy": "AUTO",
      "targetReturnRate": 0.8,
      "actualReturnRate": 0.75,
      "status": 1,
      "statusText": "启用",
      "isSuspended": false,
      "suspendUntil": null,
      "drawCount": 1000,
      "winCount": 10,
      "createTime": "2024-01-01 12:00:00"
    }
  ],
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/lottery/prize/list \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 2.3 启用/禁用奖品

```
POST /admin/lottery/prize/toggle-status
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| prizeId | Long | 是 | 奖品 ID |
| status | Integer | 是 | 状态：0-禁用 1-启用 |

**请求示例**：

```json
{
  "prizeId": 1,
  "status": 0
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
curl -X POST http://localhost:9999/api/admin/lottery/prize/toggle-status \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"prizeId":1,"status":0}'
```

### 2.4 暂停/恢复奖品

```
POST /admin/lottery/prize/suspend
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| prizeId | Long | 是 | 奖品 ID |
| suspendMinutes | Integer | 是 | 暂停分钟数（0 表示立即恢复） |

**请求示例**：

```json
{
  "prizeId": 1,
  "suspendMinutes": 60
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
curl -X POST http://localhost:9999/api/admin/lottery/prize/suspend \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"prizeId":1,"suspendMinutes":60}'
```

### 2.5 手动调整概率

```
POST /admin/lottery/prize/adjust-probability
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| prizeId | Long | 是 | 奖品 ID |
| newProbability | Double | 是 | 新概率 |
| reason | String | 否 | 调整原因 |

**请求示例**：

```json
{
  "prizeId": 1,
  "newProbability": 0.02,
  "reason": "提升中奖率"
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
curl -X POST http://localhost:9999/api/admin/lottery/prize/adjust-probability \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"prizeId":1,"newProbability":0.02,"reason":"提升中奖率"}'
```

### 2.6 实时监控

```
GET /admin/lottery/monitor/realtime
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "todayDrawCount": 1000,
    "todayWinCount": 50,
    "todayPointsCost": 100000,
    "todayPointsReward": 50000,
    "winRate": 5.0,
    "prizeStats": [
      {
        "prizeId": 1,
        "prizeName": "一等奖",
        "prizeLevel": 1,
        "todayWinCount": 5,
        "currentStock": 5,
        "actualReturnRate": 0.75
      }
    ]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/lottery/monitor/realtime \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 2.7 单品监控

```
GET /admin/lottery/monitor/prize/{prizeId}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| prizeId | Long | 是 | 奖品 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "prizeId": 1,
    "prizeName": "一等奖",
    "prizeLevel": 1,
    "currentProbability": 0.01,
    "actualReturnRate": 0.75,
    "targetReturnRate": 0.8,
    "currentStock": 5,
    "totalStock": 10,
    "todayDrawCount": 1000,
    "todayWinCount": 5,
    "totalDrawCount": 10000,
    "totalWinCount": 100,
    "isSuspended": false,
    "suspendUntil": null,
    "adjustHistory": [
      {
        "id": 1,
        "adjustType": "AUTO",
        "oldProbability": 0.01,
        "newProbability": 0.012,
        "reason": "回报率过低",
        "operator": "SYSTEM",
        "createTime": "2024-01-15 12:00:00"
      }
    ]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/lottery/monitor/prize/1 \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 2.8 抽奖记录

```
POST /admin/lottery/records
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| userId | Long | 否 | 用户 ID 筛选 |
| prizeId | Long | 否 | 奖品 ID 筛选 |
| isWin | Boolean | 否 | 是否中奖筛选 |
| startDate | String | 否 | 开始日期（yyyy-MM-dd） |
| endDate | String | 否 | 结束日期（yyyy-MM-dd） |

**请求示例**：

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "isWin": true
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
    "total": 100,
    "totalPages": 10,
    "records": [
      {
        "id": 1,
        "userId": 1,
        "userName": "用户A",
        "prizeId": 1,
        "prizeName": "一等奖",
        "prizeLevel": 1,
        "prizePoints": 1000,
        "isWin": true,
        "pointsCost": 100,
        "ip": "127.0.0.1",
        "device": "Chrome",
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
curl -X POST http://localhost:9999/api/admin/lottery/records \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"pageNum":1,"pageSize":10,"isWin":true}'
```

### 2.9 风险用户列表

```
POST /admin/lottery/user/risk-list
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| riskLevel | Integer | 否 | 风险等级：0-正常 1-低风险 2-中风险 3-高风险 |

**请求示例**：

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "riskLevel": 2
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
        "userId": 1,
        "userName": "用户A",
        "todayDrawCount": 150,
        "totalDrawCount": 5000,
        "totalWinCount": 500,
        "winRate": 10.0,
        "riskLevel": 2,
        "riskLevelText": "中风险",
        "isBlacklist": false,
        "lastDrawTime": "2024-01-15 12:00:00"
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
curl -X POST http://localhost:9999/api/admin/lottery/user/risk-list \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"pageNum":1,"pageSize":10,"riskLevel":2}'
```

### 2.10 设置黑名单

```
POST /admin/lottery/user/blacklist
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户 ID |
| isBlacklist | Boolean | 是 | 是否加入黑名单 |

**请求示例**：

```json
{
  "userId": 1,
  "isBlacklist": true
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
curl -X POST http://localhost:9999/api/admin/lottery/user/blacklist \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"isBlacklist":true}'
```

### 2.11 手动熔断

```
POST /admin/lottery/emergency/circuit-break
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reason | String | 否 | 熔断原因 |

**请求示例**：

```json
{
  "reason": "发现异常抽奖行为"
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
curl -X POST http://localhost:9999/api/admin/lottery/emergency/circuit-break \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"reason":"发现异常抽奖行为"}'
```

### 2.12 恢复服务

```
POST /admin/lottery/emergency/resume
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
curl -X POST http://localhost:9999/api/admin/lottery/emergency/resume \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 相关文档

| 文档 | 说明 |
| --- | --- |
| [积分与抽奖](/modules/points) | 积分模块详解 |
| [响应体与错误码](/reference/response-errors) | 完整错误码列表 |
| [API 路由索引](/reference/api-routes) | 完整接口清单 |
| [用户端 API - 平台能力](/api/platform) | 用户端积分抽奖接口 |
