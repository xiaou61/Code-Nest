# 工具与运营 API

本文档详细说明工具与运营模块的 API 接口，包括开发者工具、摸鱼工具、版本历史、简历等。

## 路由约定

| 前缀 | 使用场景 | 登录域 |
| --- | --- | --- |
| `/dev-tools` | 开发者工具接口 | 无需登录 |
| `/user/moyu` | 用户端摸鱼工具 | 按接口判断 |
| `/resume` | 简历接口 | 用户登录态 |
| `/version` | 版本历史接口 | 按接口判断 |
| `/admin/**` | 管理端接口 | 管理员登录态 |

---

## 一、开发者工具（DevToolsController）

**模块**：前端实现（纯前端）
**路由前缀**：`/dev-tools`
**权限要求**：无需登录

### 1.1 JSON 工具

**路由**：`/dev-tools/json`

**功能**：
- JSON 格式化
- JSON 压缩
- JSON 语法校验
- JSON 统计信息
- 历史记录（本地存储）

**使用方式**：
- 纯前端实现，无需后端接口
- 输入 JSON 文本，点击格式化/压缩按钮
- 支持文件导入和结果下载

### 1.2 文本比对

**路由**：`/dev-tools/text-diff`

**功能**：
- 行级比对
- 词级比对
- 字符级比对
- 忽略空白、忽略大小写
- 并排视图、统一视图
- 导出 HTML

**使用方式**：
- 纯前端实现，无需后端接口
- 在左右两个输入框输入文本
- 选择比对模式和选项
- 支持文件导入

### 1.3 聚合翻译

**路由**：`/dev-tools/translation`

**功能**：
- 嵌入百度翻译 iframe
- 新窗口打开翻译结果

**使用方式**：
- 纯前端实现，无需后端接口
- 输入文本后自动加载百度翻译
- 支持新窗口打开完整翻译页面

---

## 二、摸鱼工具（HotTopicController）

**模块**：`xiaou-moyu`
**路由前缀**：`/user/moyu`
**权限要求**：按接口判断

### 2.1 获取热榜分类

```
GET /user/moyu/hot-topic/categories
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "name": "知乎热榜",
      "platform": "zhihu",
      "icon": "/icons/zhihu.png"
    },
    {
      "id": 2,
      "name": "微博热搜",
      "platform": "weibo",
      "icon": "/icons/weibo.png"
    }
  ],
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/user/moyu/hot-topic/categories"
```

### 2.2 获取指定平台热榜

```
GET /user/moyu/hot-topic/data/{platform}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| platform | String | 是 | 平台标识（如 zhihu、weibo） |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "platform": "zhihu",
    "platformName": "知乎热榜",
    "updateTime": "2024-01-15 12:00:00",
    "items": [
      {
        "rank": 1,
        "title": "如何看待 AI 的发展？",
        "url": "https://www.zhihu.com/question/123",
        "hotValue": 1000000,
        "description": "讨论 AI 的未来发展趋势..."
      }
    ]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/user/moyu/hot-topic/data/zhihu"
```

### 2.3 获取所有平台热榜

```
GET /user/moyu/hot-topic/data/all
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "platform": "zhihu",
      "platformName": "知乎热榜",
      "updateTime": "2024-01-15 12:00:00",
      "items": [...]
    },
    {
      "platform": "weibo",
      "platformName": "微博热搜",
      "updateTime": "2024-01-15 12:00:00",
      "items": [...]
    }
  ],
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/user/moyu/hot-topic/data/all"
```

---

## 三、每日内容（DailyContentController）

**模块**：`xiaou-moyu`
**路由前缀**：`/user/moyu/daily-content`
**权限要求**：按接口判断

### 3.1 获取今日内容

```
GET /user/moyu/daily-content/today
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "date": "2024-01-15",
    "quote": {
      "content": "代码是写给人看的，顺便让机器执行。",
      "author": "Harold Abelson"
    },
    "tip": {
      "content": "使用 Git 时，建议先 commit 再 pull，避免冲突。",
      "category": "Git"
    },
    "codeSnippet": {
      "title": "Python 列表推导式",
      "code": "squares = [x**2 for x in range(10)]",
      "language": "python"
    },
    "history": {
      "year": 1991,
      "content": "Linux 0.01 版本发布"
    }
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/user/moyu/daily-content/today"
```

### 3.2 按类型获取内容

```
GET /user/moyu/daily-content/type/{contentType}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| contentType | String | 是 | 内容类型：quote/tip/code/history |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "contentType": "quote",
      "content": "代码是写给人看的，顺便让机器执行。",
      "author": "Harold Abelson",
      "language": null
    }
  ],
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/user/moyu/daily-content/type/quote"
```

### 3.3 点赞内容

```
POST /user/moyu/daily-content/{contentId}/like
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| contentId | Long | 是 | 内容 ID |

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
curl -X POST http://localhost:9999/api/user/moyu/daily-content/1/like \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 3.4 收藏/取消收藏

```
POST /user/moyu/daily-content/{contentId}/toggle-collection
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| contentId | Long | 是 | 内容 ID |

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
curl -X POST http://localhost:9999/api/user/moyu/daily-content/1/toggle-collection \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 四、版本历史（VersionHistoryController）

**模块**：`xiaou-version`
**路由前缀**：`/version`
**权限要求**：按接口判断

### 4.1 获取已发布版本列表

```
GET /version/list
```

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
    "total": 10,
    "totalPages": 1,
    "records": [
      {
        "id": 1,
        "versionNumber": "v2.2.0",
        "title": "v2.2.0 版本更新",
        "description": "新增 AI Runtime、SQL 优化工作台等功能",
        "updateType": 2,
        "updateTypeText": "功能更新",
        "releaseTime": "2024-01-15 12:00:00",
        "viewCount": 100
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
curl "http://localhost:9999/api/version/list?pageNum=1&pageSize=10"
```

### 4.2 获取版本详情

```
GET /version/{id}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 版本 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "versionNumber": "v2.2.0",
    "title": "v2.2.0 版本更新",
    "description": "新增 AI Runtime、SQL 优化工作台等功能",
    "content": "## 新增功能\n\n- AI Runtime 治理底座\n- SQL 优化工作台\n\n## 优化\n\n- 性能优化\n- UI 改进",
    "updateType": 2,
    "updateTypeText": "功能更新",
    "releaseTime": "2024-01-15 12:00:00",
    "viewCount": 101,
    "prdLink": "https://github.com/xiaou61/Code-Nest/pull/123"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/version/1"
```

---

## 五、简历（ResumeUserController）

**模块**：`xiaou-resume`
**路由前缀**：`/resume`
**权限要求**：需用户登录

### 5.1 创建简历

```
POST /resume
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| resumeName | String | 是 | 简历名称 |
| templateId | Long | 是 | 模板 ID |
| summary | String | 否 | 个人简介 |
| status | Integer | 否 | 状态：0-草稿 1-发布（默认 1） |
| visibility | Integer | 否 | 可见性：0-私有 1-公开（默认 0） |
| sections | List<Object> | 否 | 简历板块列表 |

**请求示例**：

```json
{
  "resumeName": "Java 开发工程师简历",
  "templateId": 1,
  "summary": "3 年 Java 开发经验...",
  "status": 1,
  "visibility": 0,
  "sections": [
    {
      "sectionType": "education",
      "title": "教育经历",
      "content": "..."
    },
    {
      "sectionType": "experience",
      "title": "工作经历",
      "content": "..."
    }
  ]
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
curl -X POST http://localhost:9999/api/resume \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"resumeName":"Java 开发工程师简历","templateId":1,"status":1}'
```

### 5.2 获取简历列表

```
GET /resume/list
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
    "total": 3,
    "totalPages": 1,
    "records": [
      {
        "id": 1,
        "resumeName": "Java 开发工程师简历",
        "templateId": 1,
        "templateName": "简约模板",
        "summary": "3 年 Java 开发经验...",
        "version": 1,
        "status": 1,
        "statusText": "已发布",
        "visibility": 0,
        "visibilityText": "私有",
        "viewCount": 0,
        "exportCount": 0,
        "shareCount": 0,
        "createTime": "2024-01-01 12:00:00",
        "updateTime": "2024-01-01 12:00:00"
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
curl "http://localhost:9999/api/resume/list?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 5.3 预览简历

```
GET /resume/{id}/preview
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 简历 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "resume": {
      "id": 1,
      "resumeName": "Java 开发工程师简历",
      "summary": "3 年 Java 开发经验...",
      "version": 1
    },
    "template": {
      "id": 1,
      "name": "简约模板"
    },
    "sections": [
      {
        "sectionType": "education",
        "title": "教育经历",
        "content": "...",
        "sortOrder": 1
      }
    ]
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/resume/1/preview" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 5.4 导出简历

```
POST /resume/{id}/export
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 简历 ID |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| format | String | 否 | 导出格式：PDF/WORD/HTML（默认 PDF） |

**请求示例**：

```json
{
  "format": "PDF"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "format": "PDF",
    "fileName": "Java开发工程师简历.pdf",
    "downloadUrl": "/files/resume/export/1/abc123.pdf",
    "previewContent": "...",
    "versionNumber": 1,
    "generatedAt": "2024-01-15 12:00:00"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/resume/1/export \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"format":"PDF"}'
```

### 5.5 创建分享链接

```
POST /resume/{id}/share
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 简历 ID |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "shareCode": "ABC123XY",
    "shareUrl": "/resume/share/ABC123XY",
    "expireTime": "2024-01-22 12:00:00",
    "accessCount": 0
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/resume/1/share \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 六、程序员日历（DeveloperCalendarController）

**模块**：`xiaou-moyu`
**路由前缀**：`/user/moyu/developer-calendar`
**权限要求**：按接口判断

### 6.1 获取今日日历事件

```
GET /user/moyu/developer-calendar/today
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "title": "程序员节",
      "description": "1024 程序员节",
      "eventDate": "10-24",
      "eventType": 1,
      "eventTypeName": "节日",
      "isMajor": true
    }
  ],
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/user/moyu/developer-calendar/today"
```

### 6.2 获取月度事件

```
GET /user/moyu/developer-calendar/month/{year}/{month}
```

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份 |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "title": "程序员节",
      "description": "1024 程序员节",
      "eventDate": "10-24",
      "eventType": 1,
      "eventTypeName": "节日",
      "isMajor": true
    }
  ],
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/user/moyu/developer-calendar/month/2024/10"
```

### 6.3 切换事件收藏

```
POST /user/moyu/developer-calendar/events/{eventId}/toggle-collection
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| eventId | Long | 是 | 事件 ID |

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
curl -X POST http://localhost:9999/api/user/moyu/developer-calendar/events/1/toggle-collection \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

---

## 七、薪资计算器（SalaryCalculatorController）

**模块**：`xiaou-moyu`
**路由前缀**：`/user/moyu/salary-calculator`
**权限要求**：需用户登录

### 7.1 获取薪资配置

```
GET /user/moyu/salary-calculator/config
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
    "monthlySalary": 20000,
    "workDaysPerMonth": 22,
    "workHoursPerDay": 8,
    "startTime": "09:00",
    "endTime": "18:00",
    "createTime": "2024-01-01 12:00:00"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl "http://localhost:9999/api/user/moyu/salary-calculator/config" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
```

### 7.2 保存薪资配置

```
POST /user/moyu/salary-calculator/config
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| monthlySalary | Integer | 是 | 月薪（元） |
| workDaysPerMonth | Integer | 否 | 每月工作天数（默认 22） |
| workHoursPerDay | Integer | 否 | 每天工作小时数（默认 8） |
| startTime | String | 否 | 上班时间（默认 09:00） |
| endTime | String | 否 | 下班时间（默认 18:00） |

**请求示例**：

```json
{
  "monthlySalary": 20000,
  "workDaysPerMonth": 22,
  "workHoursPerDay": 8,
  "startTime": "09:00",
  "endTime": "18:00"
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
curl -X POST http://localhost:9999/api/user/moyu/salary-calculator/config \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"monthlySalary":20000,"workDaysPerMonth":22,"workHoursPerDay":8}'
```

### 7.3 记录工作时间

```
POST /user/moyu/salary-calculator/work-time
```

**请求头**：

| 参数 | 说明 |
|------|------|
| Authorization | Bearer {accessToken} |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| action | String | 是 | 动作：start/pause/resume/end |

**请求示例**：

```json
{
  "action": "start"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "action": "start",
    "startTime": "2024-01-15 09:00:00",
    "status": "working"
  },
  "timestamp": 1710000000000
}
```

**curl 示例**：

```bash
curl -X POST http://localhost:9999/api/user/moyu/salary-calculator/work-time \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"action":"start"}'
```

---

## 相关文档

| 文档 | 说明 |
| --- | --- |
| [开发者工具](/modules/dev-tools) | 开发者工具详解 |
| [摸鱼工具](/modules/moyu) | 摸鱼工具详解 |
| [版本历史](/modules/version-history) | 版本历史详解 |
| [简历系统](/modules/resume) | 简历模块详解 |
| [工具、摸鱼与版本](/modules/tools-moyu-version) | 工具模块概览 |
| [响应体与错误码](/reference/response-errors) | 完整错误码列表 |
| [API 路由索引](/reference/api-routes) | 完整接口清单 |
