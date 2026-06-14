# 管理端 API - 平台能力

本文档详细说明管理端平台能力模块的 API 接口，包括文件管理、聊天管理、敏感词管理、通知管理等。

## 权限要求

所有管理端接口都需要管理员登录态，使用 `@RequireAdmin` 注解保护。

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {adminToken} |

---

## 一、文件管理

### 1.1 文件管理（AdminFileController）

**模块**：`xiaou-filestorage`
**路由前缀**：`/admin/file`

#### 1.1.1 文件列表

```
POST /admin/file/list
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| originalName | String | 否 | 原始文件名搜索 |
| moduleName | String | 否 | 模块名称筛选 |
| contentType | String | 否 | 文件类型筛选 |
| startDate | String | 否 | 开始日期（yyyy-MM-dd） |
| endDate | String | 否 | 结束日期（yyyy-MM-dd） |

**请求示例**：

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "moduleName": "user"
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
        "originalName": "avatar.png",
        "storedName": "user/avatar/1/abc123.png",
        "fileSize": 102400,
        "contentType": "image/png",
        "moduleName": "user",
        "businessType": "avatar",
        "accessUrl": "/files/user/avatar/1/abc123.png",
        "isPublic": 1,
        "status": 1,
        "uploadTime": "2024-01-01 12:00:00"
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
curl -X POST http://localhost:9999/api/admin/file/list \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"pageNum":1,"pageSize":10,"moduleName":"user"}'
```

#### 1.1.2 文件详情

```
GET /admin/file/{id}
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
    "originalName": "avatar.png",
    "storedName": "user/avatar/1/abc123.png",
    "fileSize": 102400,
    "contentType": "image/png",
    "md5Hash": "abc123def456",
    "moduleName": "user",
    "businessType": "avatar",
    "accessUrl": "/files/user/avatar/1/abc123.png",
    "isPublic": 1,
    "status": 1,
    "uploadTime": "2024-01-01 12:00:00"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/file/1 \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

#### 1.1.3 删除文件

```
DELETE /admin/file/{id}
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
  "data": null,
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X DELETE http://localhost:9999/api/admin/file/1 \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 1.2 存储配置管理（AdminStorageController）

**模块**：`xiaou-filestorage`
**路由前缀**：`/admin/storage`

#### 1.2.1 存储配置列表

```
GET /admin/storage/config/list
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "name": "本地存储",
      "type": "LOCAL",
      "endpoint": null,
      "bucket": "uploads",
      "accessKey": null,
      "isDefault": 1,
      "isEnabled": 1,
      "createTime": "2024-01-01 12:00:00"
    }
  ],
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/storage/config/list \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

#### 1.2.2 创建存储配置

```
POST /admin/storage/config/create
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 配置名称 |
| type | String | 是 | 存储类型：LOCAL/OSS/COS/KODO/OBS |
| endpoint | String | 否 | 服务端点 |
| bucket | String | 否 | 存储桶名称 |
| accessKey | String | 否 | 访问密钥 |
| secretKey | String | 否 | 秘密密钥 |
| region | String | 否 | 区域 |
| isDefault | Integer | 否 | 是否默认：0-否 1-是 |
| isEnabled | Integer | 否 | 是否启用：0-禁用 1-启用 |

**请求示例**：

```json
{
  "name": "阿里云 OSS",
  "type": "OSS",
  "endpoint": "oss-cn-hangzhou.aliyuncs.com",
  "bucket": "code-nest",
  "accessKey": "LTAI5t...",
  "secretKey": "...",
  "region": "cn-hangzhou",
  "isDefault": 0,
  "isEnabled": 1
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 2,
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/admin/storage/config/create \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"name":"阿里云 OSS","type":"OSS","endpoint":"oss-cn-hangzhou.aliyuncs.com","bucket":"code-nest"}'
```

#### 1.2.3 测试存储配置

```
POST /admin/storage/config/{id}/test
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 配置 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "success": true,
    "message": "连接成功",
    "latency": 100
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/admin/storage/config/1/test \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 1.3 文件迁移（AdminMigrationController）

**模块**：`xiaou-filestorage`
**路由前缀**：`/admin/migration`

#### 1.3.1 创建迁移任务

```
POST /admin/migration/create
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sourceConfigId | Long | 是 | 源存储配置 ID |
| targetConfigId | Long | 是 | 目标存储配置 ID |
| fileIds | List&lt;Long&gt; | 否 | 指定文件 ID 列表（空表示全部） |

**请求示例**：

```json
{
  "sourceConfigId": 1,
  "targetConfigId": 2,
  "fileIds": []
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskId": 1,
    "status": "pending",
    "totalFiles": 100,
    "message": "迁移任务已创建"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/admin/migration/create \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"sourceConfigId":1,"targetConfigId":2}'
```

#### 1.3.2 获取迁移任务状态

```
GET /admin/migration/{taskId}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | Long | 是 | 任务 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskId": 1,
    "status": "completed",
    "totalFiles": 100,
    "successCount": 98,
    "failCount": 2,
    "startTime": "2024-01-01 12:00:00",
    "endTime": "2024-01-01 12:05:00",
    "failedFiles": [
      {
        "fileId": 50,
        "error": "文件不存在"
      }
    ]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/migration/1 \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 二、聊天管理（ChatAdminController）

**模块**：`xiaou-chat`
**路由前缀**：`/admin/chat`

### 2.1 消息列表

```
POST /admin/chat/messages/list
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| userId | Long | 否 | 用户 ID 筛选 |
| messageType | Integer | 否 | 消息类型：1-文本 2-图片 |
| keyword | String | 否 | 内容关键词 |
| startDate | String | 否 | 开始日期（yyyy-MM-dd） |
| endDate | String | 否 | 结束日期（yyyy-MM-dd） |

**请求示例**：

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "messageType": 1
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
        "id": 1,
        "roomId": 1,
        "userId": 1,
        "userNickname": "用户A",
        "userAvatar": "/files/avatar/1.png",
        "messageType": 1,
        "messageTypeText": "文本",
        "content": "大家好！",
        "imageUrl": null,
        "isDeleted": 0,
        "ip": "127.0.0.1",
        "device": "Chrome",
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
curl -X POST http://localhost:9999/api/admin/chat/messages/list \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"pageNum":1,"pageSize":10,"messageType":1}'
```

### 2.2 删除消息

```
DELETE /admin/chat/messages/{id}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 消息 ID |

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
curl -X DELETE http://localhost:9999/api/admin/chat/messages/1 \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 2.3 批量删除消息

```
POST /admin/chat/messages/batch-delete
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| messageIds | List&lt;Long&gt; | 是 | 消息 ID 列表 |

**请求示例**：

```json
{
  "messageIds": [1, 2, 3]
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
curl -X POST http://localhost:9999/api/admin/chat/messages/batch-delete \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"messageIds":[1,2,3]}'
```

### 2.4 在线用户列表

```
POST /admin/chat/users/online
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| roomId | Long | 否 | 房间 ID（默认官方聊天室） |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "totalOnline": 25,
    "users": [
      {
        "userId": 1,
        "userNickname": "用户A",
        "userAvatar": "/files/avatar/1.png",
        "sessionId": "abc123",
        "ip": "127.0.0.1",
        "device": "Chrome",
        "connectTime": "2024-01-01 12:00:00",
        "lastHeartbeat": "2024-01-01 12:05:00"
      }
    ]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/admin/chat/users/online \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 2.5 踢出用户

```
POST /admin/chat/users/kick
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户 ID |

**请求示例**：

```json
{
  "userId": 1
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
curl -X POST http://localhost:9999/api/admin/chat/users/kick \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"userId":1}'
```

### 2.6 禁言用户

```
POST /admin/chat/users/ban
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户 ID |
| duration | Integer | 否 | 禁言时长（分钟，默认 60） |
| reason | String | 否 | 禁言原因 |

**请求示例**：

```json
{
  "userId": 1,
  "duration": 120,
  "reason": "发布不当言论"
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
curl -X POST http://localhost:9999/api/admin/chat/users/ban \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"duration":120,"reason":"发布不当言论"}'
```

### 2.7 解除禁言

```
POST /admin/chat/users/unban
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户 ID |

**请求示例**：

```json
{
  "userId": 1
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
curl -X POST http://localhost:9999/api/admin/chat/users/unban \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"userId":1}'
```

### 2.8 发布公告

```
POST /admin/chat/announcement
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| content | String | 是 | 公告内容 |

**请求示例**：

```json
{
  "content": "系统将于今晚 22:00 进行维护，预计耗时 2 小时。"
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
curl -X POST http://localhost:9999/api/admin/chat/announcement \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"content":"系统将于今晚 22:00 进行维护"}'
```

---

## 三、敏感词管理

### 3.1 词汇管理（SensitiveWordAdminController）

**模块**：`xiaou-sensitive`
**路由前缀**：`/admin/sensitive`

#### 3.1.1 敏感词列表

```
POST /admin/sensitive/words/list
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| word | String | 否 | 敏感词搜索 |
| categoryId | Long | 否 | 分类 ID |
| level | Integer | 否 | 等级：1-低 2-中 3-高 |
| action | Integer | 否 | 动作：1-拦截 2-替换 3-审核 |
| status | Integer | 否 | 状态：0-禁用 1-启用 |

**请求示例**：

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "status": 1
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
        "id": 1,
        "word": "敏感词",
        "categoryId": 1,
        "categoryName": "政治",
        "level": 2,
        "levelText": "中",
        "action": 2,
        "actionText": "替换",
        "replacement": "***",
        "status": 1,
        "statusText": "启用",
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
curl -X POST http://localhost:9999/api/admin/sensitive/words/list \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"pageNum":1,"pageSize":10,"status":1}'
```

#### 3.1.2 添加敏感词

```
POST /admin/sensitive/words/add
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| word | String | 是 | 敏感词 |
| categoryId | Long | 否 | 分类 ID |
| level | Integer | 否 | 等级：1-低 2-中 3-高 |
| action | Integer | 否 | 动作：1-拦截 2-替换 3-审核 |
| replacement | String | 否 | 替换文本（默认 ***） |
| status | Integer | 否 | 状态：0-禁用 1-启用（默认 1） |

**请求示例**：

```json
{
  "word": "新敏感词",
  "categoryId": 1,
  "level": 2,
  "action": 2,
  "replacement": "***"
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
curl -X POST http://localhost:9999/api/admin/sensitive/words/add \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"word":"新敏感词","categoryId":1,"level":2,"action":2}'
```

#### 3.1.3 删除敏感词

```
DELETE /admin/sensitive/words/{id}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 敏感词 ID |

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
curl -X DELETE http://localhost:9999/api/admin/sensitive/words/1 \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

#### 3.1.4 批量导入

```
POST /admin/sensitive/words/import
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| words | List&lt;String&gt; | 是 | 敏感词列表 |
| categoryId | Long | 否 | 分类 ID |
| level | Integer | 否 | 等级 |
| action | Integer | 否 | 动作 |

**请求示例**：

```json
{
  "words": ["敏感词1", "敏感词2", "敏感词3"],
  "categoryId": 1,
  "level": 2,
  "action": 2
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "totalCount": 3,
    "successCount": 3,
    "duplicateCount": 0,
    "invalidCount": 0
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/admin/sensitive/words/import \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"words":["敏感词1","敏感词2","敏感词3"],"categoryId":1,"level":2,"action":2}'
```

### 3.2 白名单管理（SensitiveWhitelistController）

**模块**：`xiaou-sensitive`
**路由前缀**：`/sensitive/whitelist`

#### 3.2.1 白名单列表

```
GET /sensitive/whitelist/list
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| module | String | 否 | 模块筛选 |

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
        "word": "Java",
        "module": "community",
        "status": 1,
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
curl "http://localhost:9999/api/sensitive/whitelist/list?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

#### 3.2.2 添加白名单

```
POST /sensitive/whitelist/add
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| word | String | 是 | 白名单词汇 |
| module | String | 否 | 模块名称（空表示全局） |

**请求示例**：

```json
{
  "word": "Java",
  "module": "community"
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
curl -X POST http://localhost:9999/api/sensitive/whitelist/add \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"word":"Java","module":"community"}'
```

#### 3.2.3 删除白名单

```
DELETE /sensitive/whitelist/{id}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 白名单 ID |

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
curl -X DELETE http://localhost:9999/api/sensitive/whitelist/1 \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 3.3 统计服务（SensitiveStatisticsController）

**模块**：`xiaou-sensitive`
**路由前缀**：`/sensitive/statistics`

#### 3.3.1 统计概览

```
GET /sensitive/statistics/overview
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "totalWords": 1000,
    "enabledWords": 950,
    "totalWhitelist": 50,
    "todayHits": 100,
    "todayBlocked": 20,
    "todayReplaced": 80,
    "topHitWords": [
      {
        "word": "敏感词1",
        "hitCount": 50
      }
    ],
    "topHitModules": [
      {
        "module": "community",
        "hitCount": 60
      }
    ]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/sensitive/statistics/overview \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 四、通知管理（AdminNotificationController）

**模块**：`xiaou-notification`
**路由前缀**：`/admin/notification`

### 4.1 发送系统公告

```
POST /admin/notification/announcement
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 是 | 公告标题 |
| content | String | 是 | 公告内容 |
| priority | String | 否 | 优先级：HIGH/MEDIUM/LOW（默认 LOW） |

**请求示例**：

```json
{
  "title": "系统维护通知",
  "content": "系统将于今晚 22:00 进行维护，预计耗时 2 小时。",
  "priority": "HIGH"
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
curl -X POST http://localhost:9999/api/admin/notification/announcement \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"title":"系统维护通知","content":"系统将于今晚 22:00 进行维护","priority":"HIGH"}'
```

### 4.2 发送批量消息

```
POST /admin/notification/batch
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userIds | List&lt;Long&gt; | 是 | 用户 ID 列表 |
| title | String | 是 | 消息标题 |
| content | String | 是 | 消息内容 |
| type | String | 否 | 消息类型（默认 PERSONAL） |

**请求示例**：

```json
{
  "userIds": [1, 2, 3],
  "title": "积分到账通知",
  "content": "您获得 100 积分奖励！",
  "type": "POINTS"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "successCount": 3,
    "failCount": 0
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/admin/notification/batch \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"userIds":[1,2,3],"title":"积分到账通知","content":"您获得 100 积分奖励！"}'
```

### 4.3 通知统计

```
GET /admin/notification/statistics
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "totalNotifications": 10000,
    "totalAnnouncements": 50,
    "todaySent": 100,
    "unreadRate": 0.35,
    "typeDistribution": [
      {
        "type": "ANNOUNCEMENT",
        "count": 50
      },
      {
        "type": "PERSONAL",
        "count": 5000
      },
      {
        "type": "TEMPLATE",
        "count": 4950
      }
    ]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/notification/statistics \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 相关文档

| 文档 | 说明 |
| --- | --- |
| [文件存储](/modules/file-storage) | 文件存储模块详解 |
| [IM 聊天室](/modules/chat) | 聊天模块详解 |
| [敏感词风控](/modules/sensitive) | 敏感词模块详解 |
| [通知中心](/modules/notification) | 通知模块详解 |
| [响应体与错误码](/reference/response-errors) | 完整错误码列表 |
| [API 路由索引](/reference/api-routes) | 完整接口清单 |
| [用户端 API - 平台能力](/api/platform) | 用户端平台能力接口 |
