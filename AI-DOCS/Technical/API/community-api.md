# 社区模块 API 文档

## 模块概述

社区模块负责帖子发布、评论互动、标签分类等功能。

## API 列表

### 1. 帖子管理

#### 1.1 帖子列表查询

- **URL**: `POST /api/community/posts/list`
- **认证**: 需要认证
- **描述**: 分页查询帖子列表

**请求参数**:

```json
{
  "current": 1,
  "size": 10,
  "categoryId": 1,
  "tagId": 1,
  "keyword": "string",
  "sortType": 1
}
```

**响应结果**:

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [
      {
        "id": 1,
        "title": "string",
        "content": "string",
        "summary": "string",
        "coverImage": "string",
        "userId": 1,
        "username": "string",
        "nickname": "string",
        "avatar": "string",
        "categoryId": 1,
        "categoryName": "string",
        "tags": [
          {
            "id": 1,
            "name": "string"
          }
        ],
        "viewCount": 100,
        "likeCount": 10,
        "commentCount": 5,
        "collectCount": 3,
        "isLiked": false,
        "isCollected": false,
        "createTime": "2026-01-01 00:00:00",
        "updateTime": "2026-01-01 00:00:00"
      }
    ],
    "total": 100,
    "current": 1,
    "size": 10,
    "pages": 10
  }
}
```

#### 1.2 帖子详情

- **URL**: `GET /api/community/posts/{id}`
- **认证**: 需要认证
- **描述**: 获取帖子详情

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 帖子ID |

**响应结果**:

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "id": 1,
    "title": "string",
    "content": "string",
    "summary": "string",
    "coverImage": "string",
    "userId": 1,
    "username": "string",
    "nickname": "string",
    "avatar": "string",
    "categoryId": 1,
    "categoryName": "string",
    "tags": [
      {
        "id": 1,
        "name": "string"
      }
    ],
    "viewCount": 100,
    "likeCount": 10,
    "commentCount": 5,
    "collectCount": 3,
    "isLiked": false,
    "isCollected": false,
    "createTime": "2026-01-01 00:00:00",
    "updateTime": "2026-01-01 00:00:00"
  }
}
```

#### 1.3 创建帖子

- **URL**: `POST /api/community/posts`
- **认证**: 需要认证
- **描述**: 创建新帖子

**请求参数**:

```json
{
  "title": "string",
  "content": "string",
  "categoryId": 1,
  "tagIds": [1, 2, 3],
  "coverImage": "string"
}
```

**响应结果**:

```json
{
  "code": 200,
  "message": "创建成功",
  "data": null
}
```

#### 1.4 点赞帖子

- **URL**: `POST /api/community/posts/{id}/like`
- **认证**: 需要认证
- **描述**: 点赞帖子

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 帖子ID |

**响应结果**:

```json
{
  "code": 200,
  "message": "点赞成功",
  "data": null
}
```

#### 1.5 取消点赞帖子

- **URL**: `DELETE /api/community/posts/{id}/like`
- **认证**: 需要认证
- **描述**: 取消点赞帖子

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 帖子ID |

**响应结果**:

```json
{
  "code": 200,
  "message": "取消点赞成功",
  "data": null
}
```

#### 1.6 收藏帖子

- **URL**: `POST /api/community/posts/{id}/collect`
- **认证**: 需要认证
- **描述**: 收藏帖子

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 帖子ID |

**响应结果**:

```json
{
  "code": 200,
  "message": "收藏成功",
  "data": null
}
```

#### 1.7 取消收藏帖子

- **URL**: `DELETE /api/community/posts/{id}/collect`
- **认证**: 需要认证
- **描述**: 取消收藏帖子

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 帖子ID |

**响应结果**:

```json
{
  "code": 200,
  "message": "取消收藏成功",
  "data": null
}
```

### 2. 评论管理

#### 2.1 评论列表查询

- **URL**: `GET /api/community/posts/{postId}/comments`
- **认证**: 需要认证
- **描述**: 查询帖子评论列表

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| postId | Long | 是 | 帖子ID |

**查询参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| current | Integer | 否 | 当前页，默认1 |
| size | Integer | 否 | 每页大小，默认10 |

**响应结果**:

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [
      {
        "id": 1,
        "content": "string",
        "userId": 1,
        "username": "string",
        "nickname": "string",
        "avatar": "string",
        "parentId": null,
        "replyUserId": null,
        "replyNickname": null,
        "likeCount": 5,
        "isLiked": false,
        "createTime": "2026-01-01 00:00:00"
      }
    ],
    "total": 50,
    "current": 1,
    "size": 10,
    "pages": 5
  }
}
```

#### 2.2 创建评论

- **URL**: `POST /api/community/posts/{postId}/comments`
- **认证**: 需要认证
- **描述**: 创建评论

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| postId | Long | 是 | 帖子ID |

**请求参数**:

```json
{
  "content": "string",
  "parentId": null,
  "replyUserId": null
}
```

**响应结果**:

```json
{
  "code": 200,
  "message": "评论成功",
  "data": null
}
```

### 3. 分类管理

#### 3.1 分类列表

- **URL**: `GET /api/community/categories`
- **认证**: 需要认证
- **description**: 获取所有分类

**响应结果**:

```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "id": 1,
      "name": "string",
      "description": "string",
      "icon": "string",
      "sort": 1,
      "postCount": 100
    }
  ]
}
```

### 4. 标签管理

#### 4.1 标签列表

- **URL**: `GET /api/community/tags`
- **认证**: 需要认证
- **描述**: 获取所有标签

**响应结果**:

```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "id": 1,
      "name": "string",
      "description": "string",
      "postCount": 50
    }
  ]
}
```

### 5. 热门帖子

#### 5.1 热门帖子列表

- **URL**: `GET /api/community/posts/hot`
- **认证**: 需要认证
- **描述**: 获取热门帖子列表

**查询参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| limit | Integer | 否 | 返回数量，默认10 |

**响应结果**:

```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "id": 1,
      "title": "string",
      "summary": "string",
      "viewCount": 1000,
      "likeCount": 100,
      "commentCount": 50
    }
  ]
}
```

## 错误码

| 错误码 | 说明 |
|--------|------|
| 30001 | 帖子不存在 |
| 30002 | 帖子已删除 |
| 30003 | 评论不存在 |
| 30004 | 分类不存在 |
| 30005 | 标签不存在 |

## 更新记录

| 版本 | 日期 | 更新内容 | 作者 |
|------|------|---------|------|
| v1.0.0 | 2026-05-29 | 初始版本 | AI Assistant |
