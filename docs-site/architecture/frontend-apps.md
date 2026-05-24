# 前端应用

Code-Nest 有两个独立的 Vue 3 前端应用：用户端和管理端。两者共享相似的技术栈但独立部署，通过各自的 Vite 代理连接后端 API。

## 技术栈对比

| 维度 | 用户端 (vue3-user-front) | 管理端 (vue3-admin-front) |
|------|-------------------------|--------------------------|
| 框架 | Vue 3 | Vue 3 |
| 构建工具 | Vite | Vite |
| UI 库 | Element Plus | Element Plus |
| 路由 | Vue Router 4 | Vue Router 4 |
| 状态管理 | Pinia | Pinia |
| HTTP 客户端 | Axios | Axios |
| 图表 | ECharts | ECharts |
| 代码编辑器 | CodeMirror 6 | — |
| API 前缀 | `/api/user/` | `/api/admin/` |
| 开发端口 | 5173 | 5174 |
| 认证方式 | StpUserUtil (用户 Token) | StpAdminUtil (管理 Token) |

## 用户端

### 项目结构

```text
vue3-user-front/
├── public/                  ← 静态资源
├── src/
│   ├── api/                 ← API 请求封装
│   │   ├── ai.js            ← AI 对话
│   │   ├── blog.js          ← 博客
│   │   ├── chat.js          ← 聊天室
│   │   ├── codepen.js       ← 代码工坊
│   │   ├── community.js     ← 社区
│   │   ├── file.js          ← 文件上传
│   │   ├── flashcard.js     ← 闪卡
│   │   ├── interview.js     ← 面试题
│   │   ├── knowledge.js     ← 知识库
│   │   ├── lottery.js       ← 抽奖
│   │   ├── mockInterview.js ← 模拟面试
│   │   ├── moment.js        ← 朋友圈
│   │   ├── moyu.js          ← 摸鱼
│   │   ├── notification.js  ← 通知
│   │   ├── oj.js            ← OJ 判题
│   │   ├── plan.js          ← 学习计划
│   │   ├── points.js        ← 积分
│   │   ├── resume.js        ← 简历
│   │   ├── sqlOptimizer.js  ← SQL 优化
│   │   ├── team.js          ← 学习小组
│   │   └── user.js          ← 用户/认证
│   ├── router/
│   │   └── index.js         ← 路由配置
│   ├── stores/              ← Pinia 状态管理
│   ├── utils/
│   │   └── request.js       ← Axios 实例 (Token 注入/拦截器)
│   ├── views/               ← 页面组件
│   │   ├── ai/              ← AI 对话
│   │   ├── blog/            ← 博客
│   │   ├── chat/            ← 聊天室
│   │   ├── codepen/         ← 代码工坊
│   │   ├── community/       ← 社区问答
│   │   ├── flashcard/       ← 闪卡
│   │   ├── home/            ← 首页
│   │   ├── interview/       ← 面试题
│   │   ├── knowledge/       ← 知识库
│   │   ├── lottery/         ← 抽奖
│   │   ├── mock-interview/  ← 模拟面试
│   │   ├── moment/          ← 朋友圈
│   │   ├── moyu/            ← 摸鱼
│   │   ├── oj/              ← OJ 判题
│   │   ├── plan/            ← 学习计划
│   │   ├── points/          ← 积分
│   │   ├── resume/          ← 简历
│   │   ├── sql-optimizer/   ← SQL 优化
│   │   ├── team/            ← 学习小组
│   │   └── user/            ← 用户中心
│   ├── App.vue
│   └── main.js
├── index.html
├── vite.config.js           ← Vite 配置 (代理/构建)
└── package.json
```

### Axios 封装

`src/utils/request.js` 封装了 Axios 实例，核心功能：

| 功能 | 实现 |
|------|------|
| Token 注入 | 请求拦截器从 localStorage 读取 Token，注入 `Authorization` Header |
| 统一错误处理 | 响应拦截器处理 401/701/702 跳转登录、其他错误提示 |
| 基础 URL | `/api` 前缀，开发环境由 Vite 代理 |
| 请求超时 | 默认 30 秒 |

### Vite 代理配置

```javascript
// vite.config.js
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:9999',
      changeOrigin: true
    }
  }
}
```

### 页面模块

| 页面目录 | 路由路径 | 对应后端模块 |
|----------|----------|-------------|
| `home/` | `/` | — |
| `ai/` | `/ai` | xiaou-ai |
| `blog/` | `/blog` | xiaou-blog |
| `chat/` | `/chat` | xiaou-chat |
| `codepen/` | `/codepen` | xiaou-codepen |
| `community/` | `/community` | xiaou-community |
| `flashcard/` | `/flashcard` | xiaou-flashcard |
| `interview/` | `/interview` | xiaou-interview |
| `knowledge/` | `/knowledge` | xiaou-knowledge |
| `lottery/` | `/lottery` | xiaou-points |
| `mock-interview/` | `/mock-interview` | xiaou-mock-interview |
| `moment/` | `/moment` | xiaou-moment |
| `moyu/` | `/moyu` | xiaou-moyu |
| `oj/` | `/oj` | xiaou-oj |
| `plan/` | `/plan` | xiaou-plan |
| `points/` | `/points` | xiaou-points |
| `resume/` | `/resume` | xiaou-resume |
| `sql-optimizer/` | `/sql-optimizer` | xiaou-sql-optimizer |
| `team/` | `/team` | xiaou-team |
| `user/` | `/user` | xiaou-user |

### 特色功能

| 功能 | 页面 | 技术要点 |
|------|------|----------|
| AI 对话 | `/ai` | 流式响应 (SSE)、Markdown 渲染 |
| 代码工坊 | `/codepen` | CodeMirror 6 编辑器、多语言切换 |
| 聊天室 | `/chat` | WebSocket + STOMP、乐观消息、心跳 |
| OJ 判题 | `/oj` | 代码提交、实时状态轮询 |
| 学习小组 | `/team` | 签到日历、讨论区、任务板 |
| 抽奖 | `/lottery` | 转盘动画、概率展示 |
| 学习计划 | `/plan` | AI 生成计划、签到追踪 |

## 管理端

### 项目结构

```text
vue3-admin-front/
├── src/
│   ├── api/                 ← API 请求封装
│   ├── router/
│   │   └── index.js         ← 路由配置
│   ├── stores/              ← Pinia 状态管理
│   ├── utils/
│   │   └── request.js       ← Axios 实例 (Admin Token)
│   ├── views/               ← 页面组件
│   │   ├── blog/            ← 博客管理
│   │   ├── chat/            ← 聊天管理
│   │   ├── codepen/         ← 代码工坊管理
│   │   ├── community/       ← 社区管理
│   │   ├── dashboard/       ← 仪表盘
│   │   ├── interview/       ← 面试题管理
│   │   ├── lottery/         ← 抽奖管理/监控/风控
│   │   ├── login/           ← 管理员登录
│   │   ├── moyu/            ← 摸鱼管理
│   │   ├── notification/    ← 通知管理
│   │   ├── oj/              ← OJ 题目管理
│   │   ├── points/          ← 积分管理
│   │   ├── system/          ← 系统管理 (日志/AI配置)
│   │   └── user/            ← 用户管理
│   ├── App.vue
│   └── main.js
├── vite.config.js
└── package.json
```

### 页面模块

| 页面目录 | 功能 | 对应后端 |
|----------|------|----------|
| `dashboard/` | 仪表盘统计 | xiaou-system |
| `login/` | 管理员登录 | xiaou-system |
| `user/` | 用户管理 | xiaou-user |
| `blog/` | 博客管理 | xiaou-blog |
| `chat/` | 聊天管理/禁言/公告 | xiaou-chat |
| `codepen/` | 代码工坊管理 | xiaou-codepen |
| `community/` | 社区管理 | xiaou-community |
| `interview/` | 面试题管理 | xiaou-interview |
| `lottery/` | 抽奖配置/监控/风控/统计 | xiaou-points |
| `moyu/` | Bug 商店管理 | xiaou-moyu |
| `notification/` | 通知管理 | xiaou-notification |
| `oj/` | OJ 题目管理 | xiaou-oj |
| `points/` | 积分管理 | xiaou-points |
| `system/` | 系统日志/AI 配置 | xiaou-system |

### 管理端特色

| 功能 | 说明 |
|------|------|
| 仪表盘 | 用户/帖子/提交等全局统计 |
| 抽奖监控 | 实时监控、风控检测、奖品调整历史 |
| AI 配置 | Prompt 管理、模型配置、RAG 管理 |
| 操作日志 | 管理员操作审计日志 |

## 双端认证差异

| 维度 | 用户端 | 管理端 |
|------|--------|--------|
| 登录接口 | `/api/user/auth/login` | `/api/admin/auth/login` |
| Token 存储 | localStorage (user token) | localStorage (admin token) |
| Token 前缀 | 用户端 Token | 管理端 Token |
| Header 注入 | `Authorization: user-token` | `Authorization: admin-token` |
| 401 处理 | 跳转用户端登录页 | 跳转管理端登录页 |
| 权限校验 | 后端 StpUserUtil | 后端 @RequireAdmin |

## 构建与部署

### 开发环境

```bash
# 用户端
cd vue3-user-front
npm install
npm run dev2    # 端口 5173

# 管理端
cd vue3-admin-front
npm install
npm run dev     # 端口 5174
```

### 生产构建

```bash
# 用户端
cd vue3-user-front
npm run build   # 产物 → dist/

# 管理端
cd vue3-admin-front
npm run build   # 产物 → dist/
```

### Nginx 部署示例

```nginx
server {
    listen 80;
    server_name example.com;

    # 用户端
    location / {
        root /var/www/user-front;
        try_files $uri $uri/ /index.html;
    }

    # 管理端
    location /admin {
        alias /var/www/admin-front;
        try_files $uri $uri/ /admin/index.html;
    }

    # API 代理
    location /api {
        proxy_pass http://127.0.0.1:9999;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # WebSocket 代理
    location /api/ws {
        proxy_pass http://127.0.0.1:9999;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

## 前端开发约定

### API 调用模式

每个模块在 `src/api/` 下有对应的 JS 文件，导出封装好的 API 函数：

```javascript
// src/api/team.js
import request from '@/utils/request'

export function getTeamList(params) {
  return request({ url: '/api/team/list', method: 'get', params })
}

export function createTeam(data) {
  return request({ url: '/api/team/create', method: 'post', data })
}
```

### 页面组件结构

每个页面目录下通常包含：

```text
views/{module}/
├── Index.vue              ← 主页面
└── components/            ← 页面私有组件
    ├── ListTable.vue
    ├── FormDialog.vue
    └── DetailPanel.vue
```

## 源码导航

| 路径 | 说明 |
|------|------|
| `vue3-user-front/package.json` | 用户端依赖与脚本 |
| `vue3-user-front/vite.config.js` | 用户端 Vite 配置 |
| `vue3-user-front/src/router/index.js` | 用户端路由 |
| `vue3-user-front/src/utils/request.js` | 用户端 Axios 封装 |
| `vue3-user-front/src/views/` | 用户端页面组件 |
| `vue3-user-front/src/api/` | 用户端 API 服务 |
| `vue3-admin-front/package.json` | 管理端依赖与脚本 |
| `vue3-admin-front/vite.config.js` | 管理端 Vite 配置 |
| `vue3-admin-front/src/router/index.js` | 管理端路由 |
| `vue3-admin-front/src/utils/request.js` | 管理端 Axios 封装 |
| `vue3-admin-front/src/views/` | 管理端页面组件 |
