# 内容社区 API

本文档详细说明内容社区模块的 API 接口，包括社区帖子、动态广场、博客、代码工坊等。

## 路由约定

| 前缀 | 使用场景 | 登录域 |
| --- | --- | --- |
| `/community/**` | 社区帖子接口 | 按接口判断 |
| `/user/moments` | 用户端动态广场 | 用户登录态 |
| `/user/blog` | 用户端博客 | 用户登录态 |
| `/user/code-pen` | 用户端代码工坊 | 用户登录态 |
| `/admin/**` | 管理端接口 | 管理员登录态 |

---

## 一、社区帖子（CommunityPostController）

**模块**：`xiaou-community`
**路由前缀**：`/community/posts`
**权限要求**：部分接口需用户登录

### 1.1 获取帖子列表

```
GET /community/posts
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |
| categoryId | Long | 否 | 分类 ID |
| tagId | Long | 否 | 标签 ID |
| keyword | String | 否 | 关键词搜索 |
| sortBy | String | 否 | 排序：latest/hot |

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
        "content": "这是一篇关于 Java 学习的文章...",
        "contentPreview": "这是一篇关于 Java 学习的文章...",
        "authorId": 1,
        "authorName": "用户A",
        "authorAvatar": "/files/avatar/1.png",
        "categoryId": 1,
        "categoryName": "Java",
        "tags": ["Java", "学习"],
        "viewCount": 100,
        "likeCount": 20,
        "commentCount": 10,
        "collectCount": 5,
        "isTop": 0,
        "isLiked": false,
        "isCollected": false,
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
curl "http://localhost:9999/api/community/posts?pageNum=1&pageSize=10&categoryId=1"
```

### 1.2 获取帖子详情

```
GET /community/posts/{id}
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
    "authorAvatar": "/files/avatar/1.png",
    "categoryId": 1,
    "categoryName": "Java",
    "tags": ["Java", "学习"],
    "viewCount": 101,
    "likeCount": 20,
    "commentCount": 10,
    "collectCount": 5,
    "isTop": 0,
    "isLiked": false,
    "isCollected": false,
    "canDelete": false,
    "createTime": "2024-01-01 12:00:00",
    "aiSummary": "本文介绍了 Java 学习的几种方法..."
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/community/posts/1"
```

### 1.3 发布帖子

```
POST /community/posts
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 是 | 帖子标题（最多 100 字） |
| content | String | 是 | 帖子内容 |
| categoryId | Long | 否 | 分类 ID |
| tagIds | List<Long> | 否 | 标签 ID 列表（最多 5 个） |

**请求示例**：

```json
{
  "title": "如何学习 Java？",
  "content": "这是一篇关于 Java 学习的文章...",
  "categoryId": 1,
  "tagIds": [1, 2]
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
| 600 | 所选分类已被禁用 |
| 600 | 内容包含敏感词，禁止发布 |
| 600 | 发布过于频繁，请稍后再试 |

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/community/posts \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"title":"如何学习 Java？","content":"...","categoryId":1}'
```

### 1.4 点赞/取消点赞帖子

```
POST /community/posts/{id}/like
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 帖子 ID |

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
curl -X POST http://localhost:9999/api/community/posts/1/like \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 1.5 收藏/取消收藏帖子

```
POST /community/posts/{id}/collect
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 帖子 ID |

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
curl -X POST http://localhost:9999/api/community/posts/1/collect \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 1.6 获取热门帖子

```
GET /community/posts/hot
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| limit | Integer | 否 | 数量限制（默认 10） |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "title": "如何学习 Java？",
      "authorName": "用户A",
      "viewCount": 1000,
      "likeCount": 200,
      "commentCount": 50,
      "hotScore": 500.0
    }
  ],
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/community/posts/hot?limit=10"
```

### 1.7 生成 AI 摘要

```
POST /community/posts/{id}/generate-summary
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

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
    "summary": "本文介绍了 Java 学习的几种方法...",
    "keywords": ["Java", "学习", "编程"]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/community/posts/1/generate-summary \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 二、社区评论（CommunityCommentController）

**模块**：`xiaou-community`
**路由前缀**：`/community`
**权限要求**：部分接口需用户登录

### 2.1 获取帖子评论

```
GET /community/posts/{postId}/comments
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| postId | Long | 是 | 帖子 ID |

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
        "postId": 1,
        "parentId": null,
        "authorId": 1,
        "authorName": "用户B",
        "authorAvatar": "/files/avatar/2.png",
        "content": "写得很好！",
        "likeCount": 5,
        "replyCount": 2,
        "isLiked": false,
        "canDelete": false,
        "createTime": "2024-01-01 12:00:00",
        "replies": [
          {
            "id": 2,
            "parentId": 1,
            "replyToId": 1,
            "replyToName": "用户B",
            "authorId": 2,
            "authorName": "用户C",
            "content": "谢谢！"
          }
        ]
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
curl "http://localhost:9999/api/community/posts/1/comments?pageNum=1&pageSize=10"
```

### 2.2 发表评论

```
POST /community/posts/{postId}/comments/create
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| postId | Long | 是 | 帖子 ID |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| content | String | 是 | 评论内容 |
| parentId | Long | 否 | 父评论 ID（回复时） |

**请求示例**：

```json
{
  "content": "写得很好！",
  "parentId": null
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
curl -X POST http://localhost:9999/api/community/posts/1/comments/create \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"content":"写得很好！"}'
```

### 2.3 删除评论

```
DELETE /community/comments/{id}
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

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

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 600 | 评论不存在 |
| 600 | 无权限删除该评论 |

**curl 示例**：

```bash
curl -X DELETE http://localhost:9999/api/community/comments/1 \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 三、动态广场（UserMomentController）

**模块**：`xiaou-moment`
**路由前缀**：`/user/moments`
**权限要求**：部分接口需用户登录

### 3.1 发布动态

```
POST /user/moments/publish
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| content | String | 是 | 动态内容 |
| images | List<String> | 否 | 图片 URL 列表 |

**请求示例**：

```json
{
  "content": "今天学习了 Java 多态...",
  "images": ["/files/images/1.png", "/files/images/2.png"]
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
| 600 | 内容包含敏感词，禁止发布 |
| 600 | 发布过于频繁，请稍后再试 |

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/user/moments/publish \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"content":"今天学习了 Java 多态...","images":[]}'
```

### 3.2 获取动态列表

```
GET /user/moments/list
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
    "total": 50,
    "totalPages": 5,
    "records": [
      {
        "id": 1,
        "userId": 1,
        "userNickname": "用户A",
        "userAvatar": "/files/avatar/1.png",
        "content": "今天学习了 Java 多态...",
        "images": ["/files/images/1.png"],
        "likeCount": 10,
        "commentCount": 5,
        "favoriteCount": 2,
        "viewCount": 50,
        "isLiked": false,
        "isFavorited": false,
        "canDelete": true,
        "createTime": "2024-01-01 12:00:00",
        "recentComments": []
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
curl "http://localhost:9999/api/user/moments/list?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 3.3 点赞/取消点赞动态

```
POST /user/moments/{momentId}/like
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| momentId | Long | 是 | 动态 ID |

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
curl -X POST http://localhost:9999/api/user/moments/1/like \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 3.4 收藏/取消收藏动态

```
POST /user/moments/{momentId}/favorite
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| momentId | Long | 是 | 动态 ID |

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
curl -X POST http://localhost:9999/api/user/moments/1/favorite \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 3.5 发表评论

```
POST /user/moments/comment
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| momentId | Long | 是 | 动态 ID |
| content | String | 是 | 评论内容 |

**请求示例**：

```json
{
  "momentId": 1,
  "content": "不错！"
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
curl -X POST http://localhost:9999/api/user/moments/comment \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"momentId":1,"content":"不错！"}'
```

### 3.6 获取热门动态

```
GET /user/moments/hot
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| limit | Integer | 否 | 数量限制（默认 10） |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "userNickname": "用户A",
      "userAvatar": "/files/avatar/1.png",
      "content": "今天学习了 Java 多态...",
      "likeCount": 100,
      "commentCount": 50,
      "viewCount": 500,
      "hotScore": 200.0
    }
  ],
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/user/moments/hot?limit=10"
```

---

## 四、博客（BlogUserController）

**模块**：`xiaou-blog`
**路由前缀**：`/user/blog`
**权限要求**：需用户登录

### 4.1 开通博客

```
POST /user/blog/open
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
    "id": 1,
    "userId": 1,
    "blogName": "用户A 的博客",
    "totalArticles": 0,
    "createTime": "2024-01-01 12:00:00"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/user/blog/open \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 4.2 发布文章

```
POST /user/blog/articles
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 是 | 文章标题 |
| content | String | 是 | 文章内容（Markdown） |
| categoryId | Long | 否 | 分类 ID |
| tags | List<String> | 否 | 标签列表（最多 5 个） |
| coverImage | String | 否 | 封面图片 URL |
| articleType | Integer | 否 | 文章类型：1-原创 2-转载 3-翻译（默认 1） |
| status | Integer | 否 | 状态：0-草稿 1-发布（默认 1） |

**请求示例**：

```json
{
  "title": "Java 多态详解",
  "content": "# Java 多态\n\n多态是指...",
  "categoryId": 1,
  "tags": ["Java", "多态"],
  "coverImage": "/files/images/cover.png",
  "articleType": 1,
  "status": 1
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "articleId": 1,
    "pointsRemaining": 980,
    "publishTime": "2024-01-01 12:00:00"
  },
  "timestamp": 1710000000000
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 600 | 请先开通博客 |
| 600 | 积分不足，发布文章需要 20 积分 |
| 600 | 文章标题包含敏感词 |
| 600 | 文章内容包含敏感词 |

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/user/blog/articles \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"title":"Java 多态详解","content":"# Java 多态\n\n多态是指...","status":1}'
```

### 4.3 获取文章列表

```
GET /user/blog/articles
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
| status | Integer | 否 | 状态筛选：0-草稿 1-已发布 |
| categoryId | Long | 否 | 分类筛选 |
| tag | String | 否 | 标签筛选 |

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
        "title": "Java 多态详解",
        "contentPreview": "多态是指...",
        "coverImage": "/files/images/cover.png",
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
curl "http://localhost:9999/api/user/blog/articles?pageNum=1&pageSize=10&status=1" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 五、代码工坊（CodePenUserController）

**模块**：`xiaou-codepen`
**路由前缀**：`/user/code-pen`
**权限要求**：需用户登录

### 5.1 创建/保存作品

```
POST /user/code-pen
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 否 | 作品 ID（更新时） |
| title | String | 是 | 作品标题（最多 100 字） |
| description | String | 否 | 作品描述 |
| htmlCode | String | 否 | HTML 代码 |
| cssCode | String | 否 | CSS 代码 |
| jsCode | String | 否 | JavaScript 代码 |
| coverImage | String | 否 | 封面图片 URL |
| tags | List<String> | 否 | 标签列表 |
| forkPrice | Integer | 否 | Fork 价格（积分，默认 0） |
| status | Integer | 否 | 状态：0-草稿 1-发布 |

**请求示例**：

```json
{
  "title": "CSS 动画示例",
  "description": "一个简单的 CSS 动画",
  "htmlCode": "<div class='box'></div>",
  "cssCode": ".box { width: 100px; height: 100px; background: red; }",
  "jsCode": "",
  "tags": ["CSS", "动画"],
  "forkPrice": 10,
  "status": 1
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "penId": 1,
    "status": 1,
    "pointsAdded": 10,
    "pointsTotal": 1010,
    "shareUrl": "https://code-nest.example.com/codepen/1",
    "createTime": "2024-01-01 12:00:00"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/user/code-pen \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"title":"CSS 动画示例","htmlCode":"<div></div>","status":1}'
```

### 5.2 Fork 作品

```
POST /user/code-pen/{id}/fork
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

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
    "penId": 2,
    "sourcePenId": 1,
    "pointsDeducted": 10,
    "pointsRemaining": 990,
    "createTime": "2024-01-01 12:00:00"
  },
  "timestamp": 1710000000000
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 600 | 作品不存在 |
| 600 | 不能 Fork 自己的作品 |
| 600 | 积分不足 |

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/user/code-pen/1/fork \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 5.3 获取作品详情

```
GET /user/code-pen/{id}
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
    "userId": 1,
    "userNickname": "用户A",
    "userAvatar": "/files/avatar/1.png",
    "tags": ["CSS", "动画"],
    "forkPrice": 10,
    "forkCount": 5,
    "likeCount": 20,
    "collectCount": 10,
    "viewCount": 100,
    "isLiked": false,
    "isCollected": false,
    "isOwner": true,
    "sourcePenId": null,
    "status": 1,
    "publishTime": "2024-01-01 12:00:00",
    "createTime": "2024-01-01 11:00:00"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/user/code-pen/1" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 相关文档

| 文档 | 说明 |
| --- | --- |
| [社区帖子](/modules/community) | 社区模块详解 |
| [动态广场](/modules/moments) | 动态模块详解 |
| [博客](/modules/blog) | 博客模块详解 |
| [代码工坊](/modules/codepen) | 代码工坊详解 |
| [社区与内容矩阵](/modules/community-content) | 内容模块概览 |
| [敏感词风控](/modules/sensitive) | 敏感词检测详解 |
| [响应体与错误码](/reference/response-errors) | 完整错误码列表 |
| [API 路由索引](/reference/api-routes) | 完整接口清单 |
