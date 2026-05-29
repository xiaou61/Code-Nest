# 聊天室模块优化PRD v2.0.0

## 1. 文档信息
| 属性 | 内容 |
|------|------|
| 文档版本 | v2.0.0 |
| 创建日期 | 2024-12-22 |
| 文档作者 | Code-Nest Team |
| 状态 | 待评审 |

## 2. 背景与目标

### 2.1 背景
当前聊天室模块(v1.0.0)已实现基础的即时通讯功能，包括文本消息、图片消息、系统公告、在线用户管理等。但在用户体验和功能完整性方面存在较大优化空间。

### 2.2 现状分析

#### 2.2.1 UI层面问题
1. **界面风格单一**：当前为传统卡片式布局，视觉层次感不足
2. **表情功能缺失**：表情按钮已存在但功能未实现
3. **图片上传缺失**：仅支持显示图片消息，不支持用户上传
4. **消息气泡简单**：样式单调，缺乏现代IM应用设计感
5. **响应式不足**：移动端适配体验不佳
6. **用户列表简陋**：缺少搜索、分组等功能

#### 2.2.2 功能逻辑问题
1. **重连机制简单**：固定3秒重连，缺少指数退避
2. **缺少发送状态**：无发送中、已发送、发送失败状态
3. **无消息引用**：不支持回复指定消息
4. **无@提及功能**：不能@特定用户
5. **无输入状态**：看不到对方正在输入
6. **无消息搜索**：无法搜索历史消息
7. **无敏感词过滤**：缺少内容安全机制
8. **无防刷屏**：缺少发送频率限制

### 2.3 优化目标
- 打造更现代化、更友好的聊天界面
- 完善核心IM功能，提升用户体验
- 增强系统稳定性和安全性
- 提升管理后台的监控能力

## 3. 功能需求

### 3.1 用户端UI优化

#### 3.1.1 全新界面布局 【优先级：P0】
**现状**：
```
┌─────────────────────────────────┐
│ Header Card                     │
├─────────────────────────────────┤
│                                 │
│ Messages Card                   │
│                                 │
├─────────────────────────────────┤
│ Input Card                      │
└─────────────────────────────────┘
```

**目标设计**：
```
┌─────────────────────────────────────────────┐
│ ┌─────────┐ Code-Nest官方群组        👤 128 │
│ │ 🎯      │ 大家一起交流技术问题           │
│ └─────────┘                                 │
├──────────────────────────┬──────────────────┤
│                          │ 在线用户 (128)   │
│   消息区域               ├──────────────────┤
│                          │ 🔍 搜索用户      │
│   [头像] 用户名 时间     │                  │
│   ┌──────────────┐      │ 👤 张三 (活跃)   │
│   │ 消息气泡     │      │ 👤 李四          │
│   └──────────────┘      │ 👤 王五          │
│                          │ ...              │
├──────────────────────────┴──────────────────┤
│ 😊 📷 📎 │ 输入消息...              │ 发送 │
└─────────────────────────────────────────────┘
```

**设计要点**：
- 左侧为主消息区域，右侧为可折叠的用户列表
- 顶部显示群组信息和在线人数
- 底部工具栏集成表情、图片、附件等功能
- 支持明暗主题切换

#### 3.1.2 消息气泡优化 【优先级：P0】

**当前样式问题**：
- 气泡圆角过小
- 颜色单一
- 缺少发送状态图标
- 时间显示位置不合理

**优化方案**：

```scss
// 他人消息气泡
.message-bubble-other {
  background: linear-gradient(135deg, #f5f7fa 0%, #f0f2f5 100%);
  border-radius: 4px 18px 18px 18px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  
  // 带尾巴效果
  &::before {
    // 气泡尖角
  }
}

// 自己的消息气泡
.message-bubble-self {
  background: linear-gradient(135deg, #409eff 0%, #53a8ff 100%);
  border-radius: 18px 4px 18px 18px;
  
  // 发送状态图标
  .send-status {
    &.sending { /* 发送中动画 */ }
    &.sent { /* 已发送 ✓ */ }
    &.delivered { /* 已送达 ✓✓ */ }
    &.failed { /* 发送失败 ⚠ */ }
  }
}
```

**消息布局结构**：
```
他人消息:
┌──────────────────────────────────────┐
│ [头像]  用户昵称                      │
│         ┌────────────────────┐       │
│         │ 消息内容           │       │
│         └────────────────────┘       │
│         10:30                        │
└──────────────────────────────────────┘

自己的消息:
┌──────────────────────────────────────┐
│                      ┌────────────┐  │
│                      │ 消息内容   │  │
│                      └────────────┘  │
│                      10:30 ✓✓ [头像] │
└──────────────────────────────────────┘
```

#### 3.1.3 表情选择器 【优先级：P1】

**功能描述**：
实现完整的表情选择器功能，支持常用表情快速选择。

**技术方案**：
- 使用 `emoji-mart-vue` 或自定义表情面板
- 支持常用表情分类（😊 表情、👍 手势、🎉 庆祝、🐱 动物等）
- 记住用户最近使用的表情
- 支持表情搜索

**UI设计**：
```
┌──────────────────────────────────┐
│ 🔍 搜索表情                      │
├──────────────────────────────────┤
│ 最近使用                         │
│ 😊 😂 👍 ❤️ 🎉 ...              │
├──────────────────────────────────┤
│ 😀 表情 │ 👋 手势 │ 🐱 动物     │
├──────────────────────────────────┤
│ 😀 😁 😂 🤣 😃 😄 😅 😆        │
│ 😉 😊 😋 😎 😍 😘 🥰 😗        │
│ ...                              │
└──────────────────────────────────┘
```

#### 3.1.4 图片上传功能 【优先级：P1】

**功能描述**：
支持用户上传图片到聊天室。

**功能特性**：
- 点击图片按钮选择本地图片
- 支持拖拽上传
- 支持粘贴上传（Ctrl+V）
- 上传前预览和编辑（裁剪）
- 显示上传进度
- 支持图片压缩（超过1MB自动压缩）
- 限制单张图片最大5MB

**上传流程**：
```
选择图片 → 预览确认 → 压缩处理 → 上传到服务器 → 获取URL → 发送图片消息
```

#### 3.1.5 消息引用/回复 【优先级：P2】

**功能描述**：
用户可以回复指定消息，形成消息引用关系。

**UI设计**：
```
┌──────────────────────────────────┐
│ 张三                             │
│ ┌────────────────────────────┐  │
│ │ ┌──────────────────────┐   │  │
│ │ │ 回复 李四:            │   │  │
│ │ │ 这个问题怎么解决?     │   │  │
│ │ └──────────────────────┘   │  │
│ │ 你试试这个方法...          │  │
│ └────────────────────────────┘  │
└──────────────────────────────────┘
```

**数据结构扩展**：
```java
// ChatMessage 新增字段
private Long replyToId;        // 回复的消息ID
private String replyToContent; // 回复的消息内容摘要
private String replyToUser;    // 被回复者昵称
```

#### 3.1.6 @提及功能 【优先级：P2】

**功能描述**：
输入@时弹出在线用户列表，选择后发送消息会通知被@的用户。

**交互流程**：
1. 用户在输入框输入`@`
2. 弹出在线用户列表（支持搜索过滤）
3. 选择用户后，输入框显示 `@用户名 `
4. 发送消息后，被@的用户收到特殊通知

**UI设计**：
```
输入: @张
┌─────────────────┐
│ 张三           │  ← 点击选中
│ 张四           │
│ 张五           │
└─────────────────┘
```

#### 3.1.7 输入状态提示 【优先级：P3】

**功能描述**：
当其他用户正在输入时，显示"XXX正在输入..."提示。

**实现方案**：
- 用户开始输入时发送TYPING事件
- 2秒内无输入则取消TYPING状态
- 限制同时显示最多3个正在输入的用户

**UI展示**：
```
┌───────────────────────────────────┐
│ 张三、李四 正在输入...            │
├───────────────────────────────────┤
│ 消息列表...                       │
```

#### 3.1.8 主题切换 【优先级：P3】

**功能描述**：
支持明暗主题切换。

**设计规范**：
```scss
// 明亮主题
:root {
  --chat-bg: #f5f7fa;
  --message-bg-other: #ffffff;
  --message-bg-self: #409eff;
  --text-primary: #303133;
  --text-secondary: #909399;
}

// 暗黑主题
:root[data-theme="dark"] {
  --chat-bg: #1a1a1a;
  --message-bg-other: #2c2c2c;
  --message-bg-self: #1890ff;
  --text-primary: #e5e5e5;
  --text-secondary: #8c8c8c;
}
```

### 3.2 功能逻辑优化

#### 3.2.1 WebSocket重连优化 【优先级：P0】

**当前问题**：
固定3秒重连，网络波动时可能导致频繁重连消耗资源。

**优化方案 - 指数退避重连**：
```javascript
const reconnect = () => {
  const maxRetries = 10;
  const baseDelay = 1000;
  const maxDelay = 30000;
  
  let retryCount = 0;
  
  const attemptReconnect = () => {
    if (retryCount >= maxRetries) {
      showReconnectFailed();
      return;
    }
    
    // 指数退避: 1s, 2s, 4s, 8s... 最大30s
    const delay = Math.min(baseDelay * Math.pow(2, retryCount), maxDelay);
    // 添加随机抖动避免惊群效应
    const jitter = Math.random() * 1000;
    
    setTimeout(() => {
      connectWebSocket();
      retryCount++;
    }, delay + jitter);
  };
  
  attemptReconnect();
};
```

#### 3.2.2 消息发送状态管理 【优先级：P1】

**状态定义**：
- `SENDING`: 发送中
- `SENT`: 已发送到服务器
- `DELIVERED`: 已送达（可选，需服务端ACK）
- `FAILED`: 发送失败

**实现方案**：
```javascript
const sendMessage = async (content) => {
  const tempId = generateTempId();
  
  // 1. 本地先展示消息（SENDING状态）
  messages.value.push({
    id: tempId,
    content,
    status: 'SENDING',
    createTime: new Date()
  });
  
  // 2. 发送到服务器
  try {
    const result = await ws.send({ type: 'MESSAGE', data: { content, tempId }});
    // 3. 更新为已发送状态
    updateMessageStatus(tempId, result.messageId, 'SENT');
  } catch (error) {
    // 4. 标记为失败，支持重试
    updateMessageStatus(tempId, null, 'FAILED');
  }
};
```

#### 3.2.3 消息发送频率限制 【优先级：P1】

**后端实现**：
```java
@Service
public class RateLimiterService {
    
    // 滑动窗口限流: 每用户每分钟最多30条消息
    private final Map<Long, Deque<Long>> userMessageTimes = new ConcurrentHashMap<>();
    
    public boolean isAllowed(Long userId) {
        Deque<Long> times = userMessageTimes.computeIfAbsent(userId, k -> new LinkedList<>());
        long now = System.currentTimeMillis();
        long windowStart = now - 60000; // 1分钟窗口
        
        // 清理过期记录
        while (!times.isEmpty() && times.peekFirst() < windowStart) {
            times.pollFirst();
        }
        
        if (times.size() >= 30) {
            return false; // 超过限制
        }
        
        times.addLast(now);
        return true;
    }
}
```

**前端提示**：
```javascript
// 发送频率限制提示
if (response.code === 'RATE_LIMITED') {
  ElMessage.warning('发送太频繁了，请稍后再试');
  // 显示冷却倒计时
  startCooldown(response.retryAfter);
}
```

#### 3.2.4 敏感词过滤 【优先级：P1】

**实现方案**：
- 使用DFA算法（确定有限自动机）实现高效敏感词匹配
- 支持动态添加/删除敏感词
- 命中敏感词时替换为`*`或拒绝发送

**后端实现**：
```java
@Service
public class SensitiveWordService {
    
    private DFAFilter dfaFilter;
    
    @PostConstruct
    public void init() {
        // 从数据库加载敏感词库
        List<String> words = sensitiveWordMapper.selectAll();
        dfaFilter = new DFAFilter(words);
    }
    
    public String filter(String content) {
        return dfaFilter.filter(content, '*');
    }
    
    public boolean containsSensitiveWord(String content) {
        return dfaFilter.contains(content);
    }
}
```

#### 3.2.5 消息搜索功能 【优先级：P2】

**功能特性**：
- 全文搜索历史消息
- 支持按用户筛选
- 支持按时间范围筛选
- 高亮显示搜索结果

**API设计**：
```
POST /user/chat/search
{
  "keyword": "关键词",
  "userId": 123,           // 可选
  "startTime": "2024-01-01",  // 可选
  "endTime": "2024-12-31",    // 可选
  "pageNum": 1,
  "pageSize": 20
}
```

#### 3.2.6 心跳机制优化 【优先级：P2】

**当前问题**：
固定30秒心跳间隔，不够灵活。

**优化方案**：
```javascript
// 自适应心跳
const heartbeat = {
  interval: 30000,  // 初始30秒
  minInterval: 10000,
  maxInterval: 60000,
  missedCount: 0,
  maxMissed: 3,
  
  start() {
    this.timer = setInterval(() => {
      if (this.missedCount >= this.maxMissed) {
        // 连续未响应，主动重连
        this.stop();
        reconnect();
        return;
      }
      
      this.missedCount++;
      ws.send({ type: 'PING', timestamp: Date.now() });
    }, this.interval);
  },
  
  onPong(latency) {
    this.missedCount = 0;
    // 根据延迟调整心跳间隔
    if (latency < 100) {
      this.interval = Math.min(this.interval * 1.5, this.maxInterval);
    } else if (latency > 1000) {
      this.interval = Math.max(this.interval / 2, this.minInterval);
    }
  }
};
```

### 3.3 管理后台优化

#### 3.3.1 实时消息监控 【优先级：P2】

**功能描述**：
管理员可实时查看聊天室消息流。

**UI设计**：
```
┌───────────────────────────────────────────────┐
│ 实时消息监控              🔴 Live    ⏸️ 暂停 │
├───────────────────────────────────────────────┤
│ 过滤: [用户ID] [关键词] [消息类型▼]          │
├───────────────────────────────────────────────┤
│ 14:23:45 张三(ID:123): 大家好                │
│ 14:23:47 李四(ID:456): 你好啊                │
│ 14:23:50 王五(ID:789): 有问题想请教          │
│ ...                                          │
└───────────────────────────────────────────────┘
```

#### 3.3.2 敏感词管理 【优先级：P2】

**功能特性**：
- 敏感词列表维护（增删改查）
- 敏感词分类管理
- 导入/导出敏感词库
- 匹配规则配置（精确/模糊）

#### 3.3.3 用户禁言管理优化 【优先级：P3】

**新增功能**：
- 禁言列表查看
- 批量解除禁言
- 禁言到期自动解除通知
- IP禁言支持

## 4. 数据库设计变更

### 4.1 消息表扩展
```sql
ALTER TABLE chat_message ADD COLUMN reply_to_id BIGINT COMMENT '回复的消息ID';
ALTER TABLE chat_message ADD COLUMN reply_to_content VARCHAR(100) COMMENT '被回复消息摘要';
ALTER TABLE chat_message ADD COLUMN reply_to_user VARCHAR(50) COMMENT '被回复者昵称';
ALTER TABLE chat_message ADD COLUMN mentions VARCHAR(500) COMMENT '@提及的用户ID,逗号分隔';
```

### 4.2 敏感词表
```sql
CREATE TABLE chat_sensitive_word (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    word VARCHAR(100) NOT NULL COMMENT '敏感词',
    category VARCHAR(50) COMMENT '分类',
    match_type TINYINT DEFAULT 1 COMMENT '匹配类型:1精确,2模糊',
    status TINYINT DEFAULT 1 COMMENT '状态:0禁用,1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_word (word),
    INDEX idx_status (status)
) COMMENT '敏感词表';
```

### 4.3 发送频率记录表
```sql
CREATE TABLE chat_rate_limit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    action_type VARCHAR(20) NOT NULL COMMENT '动作类型:MESSAGE,IMAGE',
    action_time DATETIME NOT NULL COMMENT '动作时间',
    INDEX idx_user_time (user_id, action_time)
) COMMENT '发送频率记录';
```

## 5. API接口变更

### 5.1 新增接口

| 接口 | 方法 | 描述 |
|------|------|------|
| /user/chat/upload-image | POST | 上传聊天图片 |
| /user/chat/search | POST | 搜索历史消息 |
| /admin/chat/sensitive-word/list | GET | 敏感词列表 |
| /admin/chat/sensitive-word/add | POST | 添加敏感词 |
| /admin/chat/sensitive-word/delete | POST | 删除敏感词 |
| /admin/chat/sensitive-word/import | POST | 导入敏感词 |
| /admin/chat/monitor/realtime | WS | 实时消息监控 |

### 5.2 WebSocket消息类型扩展

```javascript
// 新增消息类型
const MessageType = {
  // 已有
  CONNECT: 'CONNECT',
  MESSAGE: 'MESSAGE',
  SYSTEM: 'SYSTEM',
  ONLINE_COUNT: 'ONLINE_COUNT',
  MESSAGE_RECALL: 'MESSAGE_RECALL',
  MESSAGE_DELETE: 'MESSAGE_DELETE',
  KICK_OUT: 'KICK_OUT',
  ERROR: 'ERROR',
  HEARTBEAT: 'HEARTBEAT',
  
  // 新增
  TYPING: 'TYPING',          // 正在输入
  STOP_TYPING: 'STOP_TYPING', // 停止输入
  MESSAGE_ACK: 'MESSAGE_ACK', // 消息确认
  MENTION: 'MENTION',         // @提及通知
  PONG: 'PONG',              // 心跳响应
  RATE_LIMITED: 'RATE_LIMITED' // 频率限制
};
```

## 6. 非功能需求

### 6.1 性能要求
- 消息发送延迟 < 200ms
- 页面首次加载时间 < 2s
- WebSocket重连成功率 > 99%
- 支持单聊天室1000+并发用户

### 6.2 兼容性要求
- 支持Chrome、Firefox、Safari、Edge最新两个大版本
- 支持移动端浏览器（响应式设计）
- WebSocket降级方案（Long Polling备选）

### 6.3 安全要求
- 所有接口需要登录验证
- 敏感词过滤覆盖率 > 99%
- 防XSS注入
- 防消息重放攻击

## 7. 开发排期

### 第一阶段（P0 - 2周）
- [ ] 全新界面布局重构
- [ ] 消息气泡样式优化
- [ ] WebSocket重连机制优化
- [ ] 基础响应式适配

### 第二阶段（P1 - 2周）
- [ ] 表情选择器实现
- [ ] 图片上传功能
- [ ] 消息发送状态管理
- [ ] 发送频率限制
- [ ] 敏感词过滤

### 第三阶段（P2 - 2周）
- [ ] 消息引用/回复功能
- [ ] @提及功能
- [ ] 消息搜索功能
- [ ] 心跳机制优化
- [ ] 管理后台实时监控
- [ ] 敏感词管理后台

### 第四阶段（P3 - 1周）
- [ ] 输入状态提示
- [ ] 主题切换
- [ ] 用户禁言管理优化
- [ ] 性能优化与测试

## 8. 风险评估

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| WebSocket兼容性问题 | 中 | 提供Long Polling降级方案 |
| 敏感词过滤误杀 | 低 | 支持白名单，人工审核 |
| 高并发性能问题 | 中 | 压测优化，分布式部署 |
| 图片存储成本 | 低 | 图片压缩，定期清理 |

## 9. 附录

### 9.1 参考资料
- Element Plus组件库文档
- WebSocket RFC 6455规范
- DFA敏感词过滤算法

### 9.2 术语表
- **DFA**: Deterministic Finite Automaton，确定有限自动机
- **ACK**: Acknowledgement，消息确认
- **Long Polling**: 长轮询，WebSocket降级方案
