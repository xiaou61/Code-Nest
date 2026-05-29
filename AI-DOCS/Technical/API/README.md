# API 文档

## 目录说明

本目录存放各模块的 API 接口文档。

## API 设计规范

### 基础规范

- **协议**: HTTPS
- **基础路径**: `/api`
- **认证方式**: Sa-Token (JWT)
- **数据格式**: JSON
- **字符编码**: UTF-8

### 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| URL | 小写，连字符分隔 | `/api/user-profiles` |
| 参数 | 小驼峰 | `userId` |
| 响应字段 | 小驼峰 | `createTime` |

### 状态码

| 状态码 | 含义 | 说明 |
|--------|------|------|
| 200 | 成功 | 请求成功 |
| 400 | 请求错误 | 参数校验失败 |
| 401 | 未认证 | 未登录或Token失效 |
| 403 | 无权限 | 无访问权限 |
| 404 | 未找到 | 资源不存在 |
| 500 | 服务器错误 | 服务器内部错误 |

### 响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 分页响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "current": 1,
    "size": 10,
    "pages": 10
  }
}
```

## 模块 API 列表

| 模块 | 文档 | 说明 |
|------|------|------|
| 用户模块 | [用户模块API](user-api.md) | 用户注册、登录、资料管理 |
| 社区模块 | [社区模块API](community-api.md) | 帖子、评论、标签管理 |
| 面试模块 | [面试模块API](interview-api.md) | 面试题库、学习记录 |
| OJ模块 | [OJ模块API](oj-api.md) | 题目、提交、竞赛管理 |
| 博客模块 | [博客模块API](blog-api.md) | 文章、分类、标签管理 |
| 聊天模块 | [聊天模块API](chat-api.md) | IM聊天、消息管理 |
| 积分模块 | [积分模块API](points-api.md) | 积分、签到、抽奖管理 |
| 知识图谱 | [知识图谱API](knowledge-api.md) | 知识点、图谱管理 |
| 简历模块 | [简历模块API](resume-api.md) | 简历制作、模板管理 |
| 计划模块 | [计划模块API](plan-api.md) | 计划、打卡、统计 |
| 团队模块 | [团队模块API](team-api.md) | 团队、成员、讨论 |
| 闪卡模块 | [闪卡模块API](flashcard-api.md) | 闪卡、学习、复习 |
| 代码工坊 | [代码工坊API](codepen-api.md) | 代码片段、分享 |
| 摸鱼模块 | [摸鱼模块API](moyu-api.md) | 工具、日历、统计 |
| AI模块 | [AI模块API](ai-api.md) | AI配置、治理、运行 |
| 文件存储 | [文件存储API](filestorage-api.md) | 文件上传、管理 |
| 敏感词 | [敏感词API](sensitive-api.md) | 敏感词、策略管理 |
| SQL优化 | [SQL优化API](sql-optimizer-api.md) | SQL分析、优化 |
| 通知模块 | [通知模块API](notification-api.md) | 消息、通知管理 |
| 版本管理 | [版本管理API](version-api.md) | 版本、更新管理 |

## 认证说明

### 管理端认证

- **登录**: `POST /api/auth/login`
- **注册**: `POST /api/auth/register`
- **刷新**: `POST /api/auth/refresh`
- **登出**: `POST /api/auth/logout`

### 用户端认证

- **登录**: `POST /api/user/auth/login`
- **注册**: `POST /api/user/auth/register`
- **刷新**: `POST /api/user/auth/refresh`
- **登出**: `POST /api/user/auth/logout`

## 错误码

| 错误码 | 含义 | 说明 |
|--------|------|------|
| 10001 | 参数错误 | 请求参数校验失败 |
| 10002 | 未认证 | 未登录或Token失效 |
| 10003 | 无权限 | 无访问权限 |
| 10004 | 未找到 | 资源不存在 |
| 10005 | 服务器错误 | 服务器内部错误 |
| 20001 | 用户已存在 | 注册时用户已存在 |
| 20002 | 用户不存在 | 登录时用户不存在 |
| 20003 | 密码错误 | 登录时密码错误 |
| 30001 | 积分不足 | 积分操作时积分不足 |
| 30002 | 抽奖失败 | 抽奖操作失败 |

## 更新记录

| 版本 | 日期 | 更新内容 | 作者 |
|------|------|---------|------|
| v1.0.0 | 2026-05-29 | 初始版本 | AI Assistant |
