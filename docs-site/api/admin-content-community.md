# 管理端 API - 内容社区

本文档详细说明管理端内容社区模块的 API 接口，包括社区管理、博客管理、代码工坊管理、动态管理等。

## 权限要求

所有管理端接口都需要管理员登录态，使用 `@RequireAdmin` 注解保护。

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {adminToken} |

---

## 一、社区管理

### 1.1 帖子管理（CommunityPostAdminController）

**模块**：`xiaou-community`
**路由前缀**：`/admin/community/posts`

#### 1.1.1 帖子列表

```
POST /admin/community/posts/list
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| userNickname | String | 否 | 作者昵称筛选 |
| status | Integer | 否 | 状态：0-已删除 1-正常 2-审核中 |
| categoryId | Long | 否 | 分类 ID |
| startDate | String | 否 | 开始日期（yyyy-MM-dd） |
| endDate | String | 否 | 结束日期（yyyy-MM-dd） |

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
    "total": 100,
    "totalPages": 10,
    "records": [
      {
        "id": 1,
        "title": "如何学习 Java？",
        "contentPreview": "这是一篇关于 Java 学习的文章...",
        "authorId": 1,
        "authorName": "用户A",
        "categoryId": 1,
        "categoryName": "Java",
        "tags": ["Java", "学习"],
        "viewCount": 100,
        "likeCount": 20,
        "commentCount": 10,
        "status": 1,
        "statusDesc": "正常",
        "isTop": 0,
        "hasSensitiveWord": false,
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
curl -X POST http://localhost:9999/api/admin/community/posts/list \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"pageNum":1,"pageSize":10,"status":1}'
```

#### 1.1.2 帖子详情

```
GET /admin/community/posts/{id}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 帖子 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "title": "如何学习 Java？",
    "content": "这是一篇关于 Java 学习的文章...",
    "authorId": 1,
    "authorName": "用户A",
    "categoryId": 1,
    "categoryName": "Java",
    "tags": ["Java", "学习"],
    "viewCount": 100,
    "likeCount": 20,
    "commentCount": 10,
    "collectCount": 5,
    "status": 1,
    "statusDesc": "正常",
    "isTop": 0,
    "hasSensitiveWord": false,
    "createTime": "2024-01-01 12:00:00"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/community/posts/1 \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

#### 1.1.3 置顶/取消置顶

```
POST /admin/community/posts/{id}/top
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 帖子 ID |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| isTop | Integer | 是 | 是否置顶：0-取消置顶 1-置顶 |
| duration | Integer | 否 | 置顶时长（小时，默认 24） |

**请求示例**：

```json
{
  "isTop": 1,
  "duration": 48
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
curl -X POST http://localhost:9999/api/admin/community/posts/1/top \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"isTop":1,"duration":48}'
```

#### 1.1.4 删除帖子

```
DELETE /admin/community/posts/{id}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 帖子 ID |

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
curl -X DELETE http://localhost:9999/api/admin/community/posts/1 \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 1.2 评论管理（CommunityCommentAdminController）

**模块**：`xiaou-community`
**路由前缀**：`/admin/community/comments`

#### 1.2.1 评论列表

```
POST /admin/community/comments/list
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| postId | Long | 否 | 帖子 ID 筛选 |
| userNickname | String | 否 | 评论者昵称筛选 |
| contentKeyword | String | 否 | 内容关键词 |
| status | Integer | 否 | 状态：0-已删除 1-正常 |
| startDate | String | 否 | 开始日期（yyyy-MM-dd） |
| endDate | String | 否 | 结束日期（yyyy-MM-dd） |

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
    "total": 200,
    "totalPages": 20,
    "records": [
      {
        "id": 1,
        "postId": 1,
        "postTitle": "如何学习 Java？",
        "authorId": 1,
        "authorName": "用户B",
        "content": "写得很好！",
        "likeCount": 5,
        "replyCount": 2,
        "status": 1,
        "statusDesc": "正常",
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
curl -X POST http://localhost:9999/api/admin/community/comments/list \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"pageNum":1,"pageSize":10,"status":1}'
```

#### 1.2.2 删除评论

```
DELETE /admin/community/comments/{id}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 评论 ID |

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
curl -X DELETE http://localhost:9999/api/admin/community/comments/1 \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 1.3 用户管理（CommunityUserAdminController）

**模块**：`xiaou-community`
**路由前缀**：`/admin/community/users`

#### 1.3.1 社区用户列表

```
POST /admin/community/users/list
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| userNickname | String | 否 | 用户昵称筛选 |
| isBanned | Integer | 否 | 是否封禁：0-正常 1-封禁 |

**请求示例**：

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "isBanned": 0
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
        "userNickname": "用户A",
        "userAvatar": "/files/avatar/1.png",
        "postCount": 20,
        "commentCount": 100,
        "likeCount": 500,
        "isBanned": 0,
        "isBannedDesc": "正常",
        "banReason": null,
        "banExpireTime": null,
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
curl -X POST http://localhost:9999/api/admin/community/users/list \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"pageNum":1,"pageSize":10,"isBanned":0}'
```

#### 1.3.2 封禁用户

```
POST /admin/community/users/{userId}/ban
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户 ID |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reason | String | 是 | 封禁原因 |
| duration | Integer | 否 | 封禁时长（小时，默认 24） |

**请求示例**：

```json
{
  "reason": "发布违规内容",
  "duration": 48
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
curl -X POST http://localhost:9999/api/admin/community/users/1/ban \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"reason":"发布违规内容","duration":48}'
```

#### 1.3.3 解封用户

```
POST /admin/community/users/{userId}/unban
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
  "data": null,
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/admin/community/users/1/unban \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 二、博客管理（BlogAdminController）

**模块**：`xiaou-blog`
**路由前缀**：`/admin/blog`

### 2.1 博客统计

```
GET /admin/blog/statistics
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "totalBlogs": 100,
    "totalArticles": 500,
    "totalViews": 10000,
    "totalLikes": 2000,
    "todayNewArticles": 10,
    "todayNewBlogs": 2
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/blog/statistics \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 2.2 文章列表

```
POST /admin/blog/article/list
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| title | String | 否 | 标题搜索 |
| status | Integer | 否 | 状态：0-草稿 1-已发布 2-回收站 |
| categoryId | Long | 否 | 分类 ID |
| userId | Long | 否 | 作者 ID |
| startDate | String | 否 | 开始日期（yyyy-MM-dd） |
| endDate | String | 否 | 结束日期（yyyy-MM-dd） |

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
    "total": 200,
    "totalPages": 20,
    "records": [
      {
        "id": 1,
        "title": "Java 多态详解",
        "contentPreview": "多态是指...",
        "coverImage": "/files/images/cover.png",
        "authorId": 1,
        "authorName": "用户A",
        "categoryId": 1,
        "categoryName": "Java",
        "tags": ["Java", "多态"],
        "articleType": 1,
        "articleTypeText": "原创",
        "status": 1,
        "statusText": "已发布",
        "viewCount": 100,
        "likeCount": 20,
        "commentCount": 10,
        "isTop": 0,
        "publishTime": "2024-01-01 12:00:00",
        "createTime": "2024-01-01 11:00:00"
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
curl -X POST http://localhost:9999/api/admin/blog/article/list \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"pageNum":1,"pageSize":10,"status":1}'
```

### 2.3 置顶/取消置顶

```
POST /admin/blog/article/top
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| articleId | Long | 是 | 文章 ID |
| isTop | Integer | 是 | 是否置顶：0-取消置顶 1-置顶 |

**请求示例**：

```json
{
  "articleId": 1,
  "isTop": 1
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
curl -X POST http://localhost:9999/api/admin/blog/article/top \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"articleId":1,"isTop":1}'
```

### 2.4 更新文章状态

```
POST /admin/blog/article/update-status
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| articleId | Long | 是 | 文章 ID |
| status | Integer | 是 | 状态：0-草稿 1-已发布 2-回收站 |

**请求示例**：

```json
{
  "articleId": 1,
  "status": 2
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
curl -X POST http://localhost:9999/api/admin/blog/article/update-status \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"articleId":1,"status":2}'
```

### 2.5 删除文章

```
DELETE /admin/blog/article/{id}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 文章 ID |

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
curl -X DELETE http://localhost:9999/api/admin/blog/article/1 \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 2.6 分类管理

#### 2.6.1 分类列表

```
GET /admin/blog/category/list
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "name": "Java",
      "description": "Java 技术文章",
      "articleCount": 50,
      "status": 1,
      "createTime": "2024-01-01 12:00:00"
    }
  ],
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/blog/category/list \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

#### 2.6.2 创建分类

```
POST /admin/blog/category/create
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 分类名称 |
| description | String | 否 | 分类描述 |
| status | Integer | 否 | 状态：0-禁用 1-启用（默认 1） |

**请求示例**：

```json
{
  "name": "Spring Boot",
  "description": "Spring Boot 技术文章"
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
curl -X POST http://localhost:9999/api/admin/blog/category/create \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"name":"Spring Boot","description":"Spring Boot 技术文章"}'
```

#### 2.6.3 删除分类

```
DELETE /admin/blog/category/{id}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 分类 ID |

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
| 600 | 分类下还有文章，无法删除 |

**curl 示例**：

```bash
curl -X DELETE http://localhost:9999/api/admin/blog/category/1 \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 2.7 标签管理

#### 2.7.1 标签列表

```
GET /admin/blog/tag/list
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "name": "Java",
      "useCount": 100,
      "createTime": "2024-01-01 12:00:00"
    }
  ],
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/blog/tag/list \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

#### 2.7.2 合并标签

```
POST /admin/blog/tag/merge
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sourceTagIds | List<Long> | 是 | 源标签 ID 列表 |
| targetTagId | Long | 是 | 目标标签 ID |

**请求示例**：

```json
{
  "sourceTagIds": [2, 3],
  "targetTagId": 1
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
curl -X POST http://localhost:9999/api/admin/blog/tag/merge \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"sourceTagIds":[2,3],"targetTagId":1}'
```

---

## 三、代码工坊管理（CodePenAdminController）

**模块**：`xiaou-codepen`
**路由前缀**：`/admin/code-pen`

### 3.1 作品列表

```
POST /admin/code-pen/list
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| title | String | 否 | 标题搜索 |
| status | Integer | 否 | 状态：0-草稿 1-已发布 |
| userId | Long | 否 | 作者 ID |
| startDate | String | 否 | 开始日期（yyyy-MM-dd） |
| endDate | String | 否 | 结束日期（yyyy-MM-dd） |

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
    "total": 100,
    "totalPages": 10,
    "records": [
      {
        "id": 1,
        "title": "CSS 动画示例",
        "description": "一个简单的 CSS 动画",
        "coverImage": "/files/images/cover.png",
        "userId": 1,
        "userName": "用户A",
        "tags": ["CSS", "动画"],
        "forkPrice": 10,
        "forkCount": 5,
        "likeCount": 20,
        "collectCount": 10,
        "viewCount": 100,
        "status": 1,
        "statusText": "已发布",
        "isRecommend": false,
        "publishTime": "2024-01-01 12:00:00",
        "createTime": "2024-01-01 11:00:00"
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
curl -X POST http://localhost:9999/api/admin/code-pen/list \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"pageNum":1,"pageSize":10,"status":1}'
```

### 3.2 作品详情

```
GET /admin/code-pen/{id}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 作品 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "title": "CSS 动画示例",
    "description": "一个简单的 CSS 动画",
    "htmlCode": "<div class='box'></div>",
    "cssCode": ".box { width: 100px; height: 100px; background: red; }",
    "jsCode": "",
    "coverImage": "/files/images/cover.png",
    "userId": 1,
    "userName": "用户A",
    "userAvatar": "/files/avatar/1.png",
    "tags": ["CSS", "动画"],
    "forkPrice": 10,
    "forkCount": 5,
    "likeCount": 20,
    "collectCount": 10,
    "viewCount": 100,
    "status": 1,
    "statusText": "已发布",
    "isRecommend": false,
    "sourcePenId": null,
    "publishTime": "2024-01-01 12:00:00",
    "createTime": "2024-01-01 11:00:00"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/code-pen/1 \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 3.3 更新作品状态

```
POST /admin/code-pen/update-status
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| penId | Long | 是 | 作品 ID |
| status | Integer | 是 | 状态：0-草稿 1-已发布 |

**请求示例**：

```json
{
  "penId": 1,
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
curl -X POST http://localhost:9999/api/admin/code-pen/update-status \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"penId":1,"status":0}'
```

### 3.4 设置/取消推荐

```
POST /admin/code-pen/recommend
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| penId | Long | 是 | 作品 ID |
| isRecommend | Boolean | 是 | 是否推荐 |

**请求示例**：

```json
{
  "penId": 1,
  "isRecommend": true
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
curl -X POST http://localhost:9999/api/admin/code-pen/recommend \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"penId":1,"isRecommend":true}'
```

### 3.5 删除作品

```
DELETE /admin/code-pen/{id}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 作品 ID |

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
curl -X DELETE http://localhost:9999/api/admin/code-pen/1 \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 3.6 统计数据

```
GET /admin/code-pen/statistics
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "totalPens": 500,
    "totalViews": 50000,
    "totalLikes": 10000,
    "totalForks": 2000,
    "todayNewPens": 10,
    "todayViews": 1000,
    "topPens": [
      {
        "id": 1,
        "title": "CSS 动画示例",
        "viewCount": 1000,
        "likeCount": 200
      }
    ]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl http://localhost:9999/api/admin/code-pen/statistics \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 四、动态管理（AdminMomentController）

**模块**：`xiaou-moment`
**路由前缀**：`/admin/moments`

### 4.1 动态列表

```
POST /admin/moments/list
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| userNickname | String | 否 | 用户昵称筛选 |
| status | Integer | 否 | 状态：0-已删除 1-正常 2-审核中 |
| startDate | String | 否 | 开始日期（yyyy-MM-dd） |
| endDate | String | 否 | 结束日期（yyyy-MM-dd） |

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
        "userId": 1,
        "userName": "用户A",
        "content": "今天学习了 Java...",
        "images": ["/files/images/1.png"],
        "likeCount": 10,
        "commentCount": 5,
        "viewCount": 50,
        "status": 1,
        "statusDesc": "正常",
        "hasSensitiveWord": false,
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
curl -X POST http://localhost:9999/api/admin/moments/list \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"pageNum":1,"pageSize":10,"status":1}'
```

### 4.2 批量删除

```
POST /admin/moments/batch-delete
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| momentIds | List<Long> | 是 | 动态 ID 列表 |

**请求示例**：

```json
{
  "momentIds": [1, 2, 3]
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
curl -X POST http://localhost:9999/api/admin/moments/batch-delete \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"momentIds":[1,2,3]}'
```

### 4.3 评论列表

```
POST /admin/moments/comments/list
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| momentId | Long | 否 | 动态 ID 筛选 |
| userNickname | String | 否 | 评论者昵称筛选 |
| status | Integer | 否 | 状态：0-已删除 1-正常 |

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
    "total": 100,
    "totalPages": 10,
    "records": [
      {
        "id": 1,
        "momentId": 1,
        "momentContent": "今天学习了 Java...",
        "userId": 1,
        "userName": "用户B",
        "content": "不错！",
        "status": 1,
        "statusDesc": "正常",
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
curl -X POST http://localhost:9999/api/admin/moments/comments/list \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"pageNum":1,"pageSize":10,"status":1}'
```

### 4.4 删除评论

```
DELETE /admin/moments/comments/{id}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 评论 ID |

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
curl -X DELETE http://localhost:9999/api/admin/moments/comments/1 \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 4.5 数据统计

```
POST /admin/moments/statistics
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| startDate | String | 否 | 开始日期（yyyy-MM-dd） |
| endDate | String | 否 | 结束日期（yyyy-MM-dd） |

**请求示例**：

```json
{
  "startDate": "2024-01-01",
  "endDate": "2024-01-31"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "totalMoments": 1000,
    "totalLikes": 5000,
    "totalComments": 2000,
    "activeUsers": 300,
    "dailyStats": [
      {
        "date": "2024-01-01",
        "momentCount": 50,
        "likeCount": 200,
        "commentCount": 100
      }
    ]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/admin/moments/statistics \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"startDate":"2024-01-01","endDate":"2024-01-31"}'
```

---

## 相关文档

| 文档 | 说明 |
| --- | --- |
| [社区帖子](/modules/community) | 社区模块详解 |
| [博客](/modules/blog) | 博客模块详解 |
| [代码工坊](/modules/codepen) | 代码工坊详解 |
| [动态广场](/modules/moments) | 动态模块详解 |
| [响应体与错误码](/reference/response-errors) | 完整错误码列表 |
| [API 路由索引](/reference/api-routes) | 完整接口清单 |
| [用户端 API - 内容社区](/api/content-community) | 用户端内容社区接口 |
