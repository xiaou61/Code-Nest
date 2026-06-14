# 用户模块 API 文档

## 模块概述

用户模块负责用户注册、登录、资料管理等功能。

## API 列表

### 1. 用户认证

#### 1.1 用户登录

- **URL**: `POST /api/user/auth/login`
- **认证**: 无需认证
- **描述**: 用户登录，获取Token

**请求参数**:

```json
{
  "username": "string",
  "password": "string"
}
```

**响应结果**:

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "string",
    "userId": 1,
    "username": "string",
    "nickname": "string",
    "avatar": "string"
  }
}
```

#### 1.2 用户注册

- **URL**: `POST /api/user/auth/register`
- **认证**: 无需认证
- **描述**: 用户注册

**请求参数**:

```json
{
  "username": "string",
  "password": "string",
  "email": "string",
  "nickname": "string"
}
```

**响应结果**:

```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": 1,
    "username": "string"
  }
}
```

#### 1.3 刷新Token

- **URL**: `POST /api/user/auth/refresh`
- **认证**: 需要认证
- **description**: 刷新用户Token

**响应结果**:

```json
{
  "code": 200,
  "message": "刷新成功",
  "data": {
    "token": "string"
  }
}
```

#### 1.4 用户登出

- **URL**: `POST /api/user/auth/logout`
- **认证**: 需要认证
- **描述**: 用户登出

**响应结果**:

```json
{
  "code": 200,
  "message": "登出成功",
  "data": null
}
```

### 2. 用户信息

#### 2.1 获取用户信息

- **URL**: `GET /api/user/{userId}`
- **认证**: 需要认证
- **描述**: 根据用户ID获取用户信息

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户ID |

**响应结果**:

```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "userId": 1,
    "username": "string",
    "nickname": "string",
    "avatar": "string",
    "email": "string",
    "phone": "string",
    "gender": 0,
    "birthday": "2000-01-01",
    "bio": "string",
    "createTime": "2026-01-01 00:00:00",
    "updateTime": "2026-01-01 00:00:00"
  }
}
```

#### 2.2 获取当前用户信息

- **URL**: `GET /api/user/profile`
- **认证**: 需要认证
- **描述**: 获取当前登录用户信息

**响应结果**: 同 2.1

#### 2.3 更新用户信息

- **URL**: `PUT /api/user/{userId}`
- **认证**: 需要认证
- **描述**: 更新用户信息

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户ID |

**请求参数**:

```json
{
  "nickname": "string",
  "avatar": "string",
  "email": "string",
  "phone": "string",
  "gender": 0,
  "birthday": "2000-01-01",
  "bio": "string"
}
```

**响应结果**:

```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "userId": 1,
    "username": "string",
    "nickname": "string",
    "avatar": "string",
    "email": "string",
    "phone": "string",
    "gender": 0,
    "birthday": "2000-01-01",
    "bio": "string",
    "createTime": "2026-01-01 00:00:00",
    "updateTime": "2026-01-01 00:00:00"
  }
}
```

#### 2.4 更新当前用户信息

- **URL**: `PUT /api/user/profile`
- **认证**: 需要认证
- **描述**: 更新当前登录用户信息

**请求参数**: 同 2.3

**响应结果**: 同 2.3

### 3. 密码管理

#### 3.1 修改密码

- **URL**: `PUT /api/user/{userId}/password`
- **认证**: 需要认证
- **描述**: 修改用户密码

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 用户ID |

**请求参数**:

```json
{
  "oldPassword": "string",
  "newPassword": "string",
  "confirmPassword": "string"
}
```

**响应结果**:

```json
{
  "code": 200,
  "message": "密码修改成功",
  "data": null
}
```

#### 3.2 修改当前用户密码

- **URL**: `PUT /api/user/password`
- **认证**: 需要认证
- **描述**: 修改当前登录用户密码

**请求参数**: 同 3.1

**响应结果**: 同 3.1

### 4. 头像上传

#### 4.1 上传头像

- **URL**: `POST /api/user/avatar`
- **认证**: 需要认证
- **Content-Type**: `multipart/form-data`
- **描述**: 上传用户头像

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | MultipartFile | 是 | 头像文件 |

**响应结果**:

```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "url": "string",
    "filename": "string"
  }
}
```

## 错误码

| 错误码 | 说明 |
|--------|------|
| 20001 | 用户已存在 |
| 20002 | 用户不存在 |
| 20003 | 密码错误 |
| 20004 | 邮箱已存在 |
| 20005 | 用户名格式错误 |
| 20006 | 密码格式错误 |

## 更新记录

| 版本 | 日期 | 更新内容 | 作者 |
|------|------|---------|------|
| v1.0.0 | 2026-05-29 | 初始版本 | AI Assistant |
