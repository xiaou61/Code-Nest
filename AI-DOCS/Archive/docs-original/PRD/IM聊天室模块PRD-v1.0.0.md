## IM聊天室模块PRD-v1.0.0

## 1. 项目基本信息

**项目名称：** IM即时聊天室  
**版本：** v1.0.0  
**技术栈：** Spring Boot + MyBatis + WebSocket + Vue3  
**适用端：** PC端 + 移动端

## 2. 背景与目标

### 2.1 背景
平台需要一个实时交流功能，让所有用户能够在统一的官方群组中进行即时交流，增强社区互动氛围。当前版本采用简化设计，仅支持单一官方群组，降低系统复杂度。

### 2.2 产品目标
- **核心目标：** 提供稳定可靠的实时聊天功能
- **用户目标：** 用户可以实时发送和接收消息，与其他用户互动交流
- **业务目标：** 提升用户粘性，营造活跃的社区氛围

## 3. 功能需求

### 3.1 用户端功能

#### 3.1.1 聊天室入口
- 在用户端导航栏提供"聊天室"入口
- 显示在线人数统计
- 显示未读消息数量（红点提醒）

#### 3.1.2 消息发送
**基础功能：**
- 文本消息发送（支持最多500字符）
- 支持emoji表情输入
- 支持图片发送（单张最大5MB）
- Enter键快速发送，Shift+Enter换行

**发送限制：**
- 同一用户1秒内只能发送1条消息（防刷屏）
- 文本消息不能为空或纯空格
- 图片格式限制：jpg、png、gif
- 敏感词自动过滤（发送前检测）

#### 3.1.3 消息接收与展示
**消息列表：**
- 时间正序展示（旧消息在上，新消息在下）
- 自动滚动到最新消息
- 支持历史消息加载（上滑加载更多）
- 每次加载20条历史消息

**消息卡片显示：**
- 用户头像（点击可查看用户信息）
- 用户昵称
- 消息内容（文本/图片）
- 发送时间（相对时间显示，如"3分钟前"）
- 自己发送的消息右侧对齐，他人消息左侧对齐

**消息状态：**
- 发送中（loading状态）
- 发送成功（正常显示）
- 发送失败（显示重发按钮）

#### 3.1.4 在线状态
- 实时显示在线人数
- 用户进入/离开聊天室的系统提示（可配置）
- 在线用户列表（侧边栏展示）

#### 3.1.5 消息互动
- 图片预览功能（点击图片放大查看）
- @提及功能（@用户昵称）
- 复制消息内容
- 撤回消息（仅限2分钟内自己发送的消息）

#### 3.1.6 聊天设置
- 消息通知开关
- 声音提醒开关
- 屏蔽某个用户（仅前端不显示该用户消息）

### 3.2 管理端功能

#### 3.2.1 消息管理
**消息列表：**
- 分页展示所有聊天记录
- 支持按用户ID、昵称筛选
- 支持按时间范围筛选
- 支持按关键词搜索消息内容
- 显示敏感词检测结果标记

**消息操作：**
- 删除单条消息（所有用户端实时同步删除）
- 批量删除消息
- 查看消息详细信息（发送者IP、设备信息等）

#### 3.2.2 用户管理
**在线用户监控：**
- 实时查看在线用户列表
- 显示用户连接时间、IP地址
- 踢出指定用户（强制断开连接）

**用户行为统计：**
- 用户发言频率统计
- 活跃用户排行榜
- 违规记录查询

**禁言管理：**
- 对指定用户禁言（支持时长设置）
- 禁言原因记录
- 自动到期解禁
- 手动解除禁言

#### 3.2.3 系统消息推送
- 管理员可发送系统公告（全员广播）
- 系统消息特殊样式展示（居中显示）
- 支持定时推送功能

#### 3.2.4 数据统计
**基础统计：**
- 今日消息总数
- 今日活跃用户数
- 历史消息总量
- 峰值在线人数

**趋势分析：**
- 每日消息数量趋势图
- 每日活跃用户趋势图
- 每小时消息分布图

**数据导出：**
- 支持聊天记录导出（Excel格式）
- 支持自定义时间范围导出

## 4. 核心流程

### 4.1 用户聊天流程
1. **进入聊天室：** 登录 → 点击聊天室入口 → 建立WebSocket连接 → 加载历史消息
2. **发送消息：** 输入内容 → 敏感词检测 → 频率检查 → 发送到服务器 → 广播到所有在线用户
3. **接收消息：** WebSocket接收 → 消息展示 → 自动滚动到最新
4. **离开聊天室：** 关闭页面/切换页面 → 断开WebSocket连接 → 系统提示

### 4.2 管理员操作流程
1. **消息管理：** 登录后台 → 查看消息列表 → 筛选/搜索 → 执行管理操作（删除等）
2. **用户管理：** 查看在线用户 → 分析用户行为 → 执行管理操作（禁言/踢出等）
3. **系统公告：** 编辑公告内容 → 选择推送时间 → 发送全员广播

### 4.3 系统自动化流程
1. **禁言到期：** 定时任务检查 → 自动解除禁言
2. **消息清理：** 定时清理超过90天的历史消息（可配置）
3. **在线状态同步：** 心跳检测 → 自动断开超时连接

## 5. 技术方案

### 5.1 数据库设计

#### 5.1.1 聊天室表（chat_rooms）
```sql
CREATE TABLE chat_rooms (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '聊天室ID',
    room_name VARCHAR(100) NOT NULL COMMENT '聊天室名称',
    room_type TINYINT DEFAULT 1 COMMENT '类型：1官方群组',
    description VARCHAR(500) COMMENT '聊天室描述',
    max_users INT DEFAULT 0 COMMENT '最大人数限制，0表示不限制',
    status TINYINT DEFAULT 1 COMMENT '状态：1正常 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '聊天室表';
```

#### 5.1.2 聊天消息表（chat_messages）
```sql
CREATE TABLE chat_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    room_id BIGINT NOT NULL COMMENT '聊天室ID',
    user_id BIGINT NOT NULL COMMENT '发送者用户ID',
    message_type TINYINT NOT NULL COMMENT '消息类型：1文本 2图片 3系统消息',
    content TEXT NOT NULL COMMENT '消息内容',
    image_url VARCHAR(500) COMMENT '图片URL（消息类型为图片时）',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除：0否 1是',
    ip_address VARCHAR(50) COMMENT '发送者IP地址',
    device_info VARCHAR(200) COMMENT '设备信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_room_id (room_id),
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time DESC),
    INDEX idx_room_time (room_id, create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '聊天消息表';
```

#### 5.1.3 用户禁言表（chat_user_bans）
```sql
CREATE TABLE chat_user_bans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '禁言记录ID',
    user_id BIGINT NOT NULL COMMENT '被禁言用户ID',
    room_id BIGINT NOT NULL COMMENT '聊天室ID',
    ban_reason VARCHAR(500) COMMENT '禁言原因',
    ban_start_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '禁言开始时间',
    ban_end_time DATETIME COMMENT '禁言结束时间，NULL表示永久',
    operator_id BIGINT COMMENT '操作人ID',
    status TINYINT DEFAULT 1 COMMENT '状态：1生效中 0已解除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_room_id (room_id),
    INDEX idx_status (status),
    INDEX idx_end_time (ban_end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '用户禁言表';
```

#### 5.1.4 在线用户表（chat_online_users）
```sql
CREATE TABLE chat_online_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    room_id BIGINT NOT NULL COMMENT '聊天室ID',
    session_id VARCHAR(100) NOT NULL COMMENT 'WebSocket会话ID',
    ip_address VARCHAR(50) COMMENT '用户IP地址',
    device_info VARCHAR(200) COMMENT '设备信息',
    connect_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '连接时间',
    last_heartbeat_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后心跳时间',
    
    UNIQUE KEY uk_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_room_id (room_id),
    INDEX idx_heartbeat (last_heartbeat_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '在线用户表';
```

### 5.2 接口设计

#### 5.2.1 HTTP接口（/api/chat）

**用户端接口：**
```
POST /user/chat/history              # 获取历史消息
POST /user/chat/online-count         # 获取在线人数
POST /user/chat/online-users         # 获取在线用户列表
POST /user/chat/message/recall       # 撤回消息
POST /user/chat/upload-image         # 上传聊天图片
```

**管理端接口：**
```
POST /admin/chat/messages/list       # 获取消息列表（管理端）
DELETE /admin/chat/messages/{id}     # 删除消息
POST /admin/chat/messages/batch-delete  # 批量删除消息
POST /admin/chat/users/online        # 获取在线用户列表（管理端）
POST /admin/chat/users/kick          # 踢出用户
POST /admin/chat/users/ban           # 禁言用户
POST /admin/chat/users/unban         # 解除禁言
POST /admin/chat/announcement        # 发送系统公告
POST /admin/chat/statistics          # 获取统计数据
POST /admin/chat/export              # 导出聊天记录
```

#### 5.2.2 WebSocket接口（/ws/chat）

**连接地址：**
```
ws://domain/ws/chat?token={JWT_TOKEN}
```

**消息类型（type字段）：**
- `CONNECT` - 连接成功
- `MESSAGE` - 普通聊天消息
- `SYSTEM` - 系统消息
- `ONLINE_COUNT` - 在线人数更新
- `USER_JOIN` - 用户加入
- `USER_LEAVE` - 用户离开
- `MESSAGE_RECALL` - 消息撤回
- `MESSAGE_DELETE` - 消息删除（管理员操作）
- `KICK_OUT` - 被踢出
- `HEARTBEAT` - 心跳包

**客户端发送消息格式：**
```json
{
    "type": "MESSAGE",
    "content": "消息内容",
    "messageType": 1,
    "imageUrl": null
}
```

**服务端推送消息格式：**
```json
{
    "type": "MESSAGE",
    "data": {
        "id": 123,
        "userId": 456,
        "userNickname": "用户A",
        "userAvatar": "avatar_url",
        "messageType": 1,
        "content": "消息内容",
        "imageUrl": null,
        "createTime": "2024-01-01 12:00:00"
    }
}
```

**在线人数更新格式：**
```json
{
    "type": "ONLINE_COUNT",
    "data": {
        "count": 128
    }
}
```

**系统消息格式：**
```json
{
    "type": "SYSTEM",
    "data": {
        "content": "系统公告内容",
        "createTime": "2024-01-01 12:00:00"
    }
}
```

### 5.3 请求/响应示例

**获取历史消息：**
```json
// 请求
POST /user/chat/history
{
    "lastMessageId": 0,
    "pageSize": 20
}

// 响应
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "messages": [
            {
                "id": 123,
                "userId": 456,
                "userNickname": "用户A",
                "userAvatar": "avatar_url",
                "messageType": 1,
                "content": "大家好",
                "imageUrl": null,
                "canRecall": false,
                "createTime": "2024-01-01 12:00:00"
            }
        ],
        "hasMore": true
    }
}
```

**获取在线人数：**
```json
// 请求
POST /user/chat/online-count

// 响应
{
    "code": 200,
    "message": "获取成功",
    "data": 128
}
```

**撤回消息：**
```json
// 请求
POST /user/chat/message/recall
{
    "messageId": 123
}

// 响应
{
    "code": 200,
    "message": "撤回成功",
    "data": true
}
```

**管理端获取消息列表：**
```json
// 请求
POST /admin/chat/messages/list
{
    "pageNum": 1,
    "pageSize": 20,
    "userId": null,
    "keyword": null,
    "startTime": "2024-01-01 00:00:00",
    "endTime": "2024-12-31 23:59:59"
}

// 响应
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "records": [
            {
                "id": 123,
                "userId": 456,
                "userNickname": "用户A",
                "messageType": 1,
                "content": "消息内容",
                "imageUrl": null,
                "ipAddress": "192.168.1.1",
                "deviceInfo": "Chrome/Windows",
                "hasSensitiveWord": false,
                "createTime": "2024-01-01 12:00:00"
            }
        ],
        "total": 1000,
        "current": 1,
        "size": 20
    }
}
```

**禁言用户：**
```json
// 请求
POST /admin/chat/users/ban
{
    "userId": 456,
    "banReason": "违规发言",
    "banDuration": 3600
}

// 响应
{
    "code": 200,
    "message": "禁言成功",
    "data": true
}
```

**发送系统公告：**
```json
// 请求
POST /admin/chat/announcement
{
    "content": "系统维护通知：今晚22:00-23:00进行系统维护"
}

// 响应
{
    "code": 200,
    "message": "发送成功",
    "data": true
}
```

**获取统计数据：**
```json
// 请求
POST /admin/chat/statistics
{
    "startTime": "2024-01-01 00:00:00",
    "endTime": "2024-01-31 23:59:59"
}

// 响应
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "totalMessages": 10000,
        "todayMessages": 500,
        "activeUsers": 300,
        "peakOnlineUsers": 150,
        "dailyStats": [
            {
                "date": "2024-01-01",
                "messageCount": 500,
                "activeUserCount": 100
            }
        ],
        "hourlyStats": [
            {
                "hour": 0,
                "messageCount": 20
            }
        ]
    }
}
```

### 5.4 DTO结构设计

#### 5.4.1 请求DTO

**历史消息查询请求（ChatHistoryRequest）：**
```java
{
    "lastMessageId": "Long（最后一条消息ID，0表示最新，必填）",
    "pageSize": "Integer（每页大小，默认20）"
}
```

**消息撤回请求（ChatRecallRequest）：**
```java
{
    "messageId": "Long（消息ID，必填）"
}
```

**管理端消息查询请求（AdminChatMessageListRequest）：**
```java
{
    "pageNum": "Integer（页码，默认1）",
    "pageSize": "Integer（每页大小，默认20）",
    "userId": "Long（用户ID筛选，选填）",
    "keyword": "String（关键词搜索，选填）",
    "startTime": "String（开始时间，选填）",
    "endTime": "String（结束时间，选填）"
}
```

**禁言请求（ChatBanUserRequest）：**
```java
{
    "userId": "Long（用户ID，必填）",
    "banReason": "String（禁言原因，必填）",
    "banDuration": "Integer（禁言时长/秒，0表示永久，必填）"
}
```

**系统公告请求（ChatAnnouncementRequest）：**
```java
{
    "content": "String（公告内容，必填）"
}
```

#### 5.4.2 响应DTO

**聊天消息响应（ChatMessageResponse）：**
```java
{
    "id": "Long（消息ID）",
    "userId": "Long（用户ID）",
    "userNickname": "String（用户昵称）",
    "userAvatar": "String（用户头像URL）",
    "messageType": "Integer（消息类型：1文本 2图片 3系统消息）",
    "content": "String（消息内容）",
    "imageUrl": "String（图片URL）",
    "canRecall": "Boolean（是否可撤回）",
    "createTime": "String（创建时间）"
}
```

**在线用户响应（ChatOnlineUserResponse）：**
```java
{
    "userId": "Long（用户ID）",
    "userNickname": "String（用户昵称）",
    "userAvatar": "String（用户头像URL）",
    "connectTime": "String（连接时间）",
    "ipAddress": "String（IP地址）",
    "deviceInfo": "String（设备信息）"
}
```

**统计数据响应（ChatStatisticsResponse）：**
```java
{
    "totalMessages": "Long（消息总数）",
    "todayMessages": "Long（今日消息数）",
    "activeUsers": "Long（活跃用户数）",
    "peakOnlineUsers": "Integer（峰值在线人数）",
    "dailyStats": "List<DailyChatStatistics>（每日统计）",
    "hourlyStats": "List<HourlyChatStatistics>（每小时统计）"
}
```

### 5.5 前端组件设计

#### 5.5.1 核心组件
- `ChatRoom`：聊天室主容器组件
- `MessageList`：消息列表组件
- `MessageItem`：单条消息展示组件
- `MessageInput`：消息输入框组件
- `OnlineUserPanel`：在线用户面板组件
- `EmojiPicker`：表情选择器组件
- `ImagePreview`：图片预览组件

#### 5.5.2 状态管理
使用Pinia管理全局状态：
- `chatStore`：聊天消息、WebSocket连接状态管理
- `userStore`：用户信息管理

#### 5.5.3 WebSocket管理
```javascript
// WebSocket连接管理
class ChatWebSocket {
    constructor() {
        this.ws = null;
        this.reconnectTimer = null;
        this.heartbeatTimer = null;
    }
    
    // 连接
    connect(token) { ... }
    
    // 发送消息
    sendMessage(data) { ... }
    
    // 断开连接
    disconnect() { ... }
    
    // 自动重连
    reconnect() { ... }
    
    // 心跳检测
    startHeartbeat() { ... }
}
```

### 5.6 业务规则与验证

#### 5.6.1 消息发送限制
- **内容验证：** 消息内容不能为空或纯空格，最多500字符
- **频率控制：** 同一用户1秒内只能发送1条消息
- **禁言检查：** 发送前检查用户是否被禁言
- **敏感词过滤：** 发送前进行敏感词检测和过滤

#### 5.6.2 权限控制
- **发言权限：** 登录用户且未被禁言可发送消息
- **撤回权限：** 用户只能撤回自己2分钟内发送的消息
- **管理权限：** 管理员可删除任意消息、禁言用户、踢出用户

#### 5.6.3 连接管理
- **心跳检测：** 客户端每30秒发送一次心跳包
- **超时断开：** 服务端60秒未收到心跳则断开连接
- **自动重连：** 客户端断开后3秒自动重连，最多重试5次

## 6. 页面设计

### 6.1 用户端聊天室页面

#### 6.1.1 页面布局
```
┌──────────────────────────────────────────────┐
│ 聊天室 · Code-Nest官方群组        在线：128人│
├──────────────────────────────────────────────┤
│                                              │
│  ┌────┐                                     │
│  │头像│ 用户A  12:00                         │
│  └────┘ 大家好，很高兴加入这个社区！         │
│                                              │
│                         12:05  用户B  ┌────┐│
│                         我也是新人！  │头像││
│                                       └────┘│
│                                              │
│  ┌────┐                                     │
│  │头像│ 用户C  12:10                         │
│  └────┘ [图片]                               │
│                                              │
├──────────────────────────────────────────────┤
│ [😊] 输入消息...                      [📷][发送]│
└──────────────────────────────────────────────┘
```

#### 6.1.2 交互规范
- **消息加载：** 进入聊天室自动加载最新20条消息
- **历史消息：** 向上滚动自动加载更多历史消息
- **实时更新：** 新消息自动追加，列表自动滚动到底部
- **消息操作：** 长按/右键消息显示操作菜单（撤回、复制）
- **图片预览：** 点击图片全屏预览，支持缩放

### 6.2 管理端聊天管理页面

#### 6.2.1 消息管理页面
**页面功能：**
- 消息列表展示（分页）
- 筛选功能：用户ID、关键词、时间范围
- 敏感词标记显示
- 单个/批量删除操作
- 消息详情查看（IP、设备信息等）

#### 6.2.2 在线用户管理页面
**页面功能：**
- 在线用户实时列表
- 用户连接信息展示（连接时间、IP、设备）
- 踢出用户操作
- 禁言操作（弹窗设置禁言时长和原因）

#### 6.2.3 数据统计页面
**页面功能：**
- 核心指标卡片：今日消息数、活跃用户数、在线人数、消息总量
- 消息趋势图表（ECharts）
- 活跃度分析图表
- 数据导出功能

