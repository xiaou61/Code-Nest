# 个人计划打卡模块 PRD v1.0.0

## 📋 项目概述

### 🎯 项目背景
程序员在日常工作学习中，经常需要制定各种计划任务，如刷题计划、阅读计划、学习计划等。为了帮助用户更好地管理个人目标、养成良好习惯，需要建立一套完整的个人计划打卡系统。通过任务提醒、进度追踪、数据统计等功能，提升用户的任务完成率和学习效率。

### 💡 核心价值
- **目标管理**: 帮助用户制定和管理个人学习/工作计划
- **习惯养成**: 通过打卡机制培养用户的持续执行习惯
- **及时提醒**: 多渠道任务提醒，确保用户不错过重要任务
- **进度追踪**: 可视化展示任务完成进度和历史记录
- **数据分析**: 统计分析用户的计划执行情况，提供改进建议
- **积分激励**: 与积分系统打通，完成计划可获得额外积分奖励

### 🏠 模块定位
建议将该模块作为独立的 `xiaou-plan` 模块，同时在前端 `vue3-user-front` 的摸鱼工具或独立菜单中展示。该模块与以下模块存在关联：
- **xiaou-points**: 完成计划任务可获得积分奖励
- **xiaou-notification**: 复用通知模块发送任务提醒
- **xiaou-interview**: 可创建与刷题相关的计划任务

## 🚀 功能需求

### 1. 计划管理

#### 1.1 创建计划
- **计划类型**: 支持多种类型（刷题计划、学习计划、阅读计划、运动计划、自定义计划）
- **计划名称**: 用户自定义计划名称，如"每日刷3道算法题"
- **计划描述**: 可选的详细描述说明
- **目标设定**: 
  - 数量目标：如"完成10道题"
  - 时长目标：如"学习2小时"
  - 次数目标：如"打卡7天"
- **时间设置**:
  - 开始日期：计划生效日期
  - 结束日期：计划截止日期（可选，支持长期计划）
  - 每日开始时间：当日任务的预计开始时间
  - 每日截止时间：当日任务的截止时间
- **重复规则**:
  - 每日：每天都需要完成
  - 工作日：仅周一至周五
  - 周末：仅周六周日
  - 自定义：选择特定星期几
- **提醒设置**:
  - 提前提醒时间：任务开始前N分钟提醒
  - 截止提醒：任务截止前N分钟再次提醒
  - 提醒渠道：站内通知/浏览器推送/邮件

#### 1.2 计划列表
- **状态筛选**: 进行中/已完成/已过期/已暂停
- **类型筛选**: 按计划类型筛选
- **排序方式**: 按创建时间/截止时间/完成率排序
- **搜索功能**: 支持按计划名称搜索
- **快捷操作**: 暂停/恢复/删除/编辑计划

#### 1.3 计划详情
- **基本信息**: 计划名称、描述、时间范围、重复规则
- **进度展示**: 已完成/总目标的进度条
- **打卡日历**: 日历视图展示每日打卡情况
- **历史记录**: 所有打卡记录列表

### 2. 打卡功能

#### 2.1 今日任务
- **任务列表**: 展示当日需要完成的所有计划任务
- **任务状态**: 待完成/进行中/已完成/已过期
- **倒计时**: 显示距离任务截止的剩余时间
- **快捷打卡**: 一键完成简单任务的打卡

#### 2.2 打卡记录
- **打卡时间**: 精确到秒的打卡时间记录
- **完成内容**: 记录具体完成的内容（可选）
- **完成数量**: 记录完成的数量（如刷了几道题）
- **心得备注**: 用户可添加学习心得或备注
- **附件上传**: 支持上传截图作为完成证明（可选）

#### 2.3 补打卡
- **补卡规则**: 允许补打最近N天内未完成的任务
- **补卡限制**: 每月最多补卡N次
- **补卡标记**: 补卡记录会有特殊标记

### 3. 提醒系统

#### 3.1 站内通知
- **实现方式**: 复用现有 `xiaou-notification` 模块
- **通知内容**: 任务标题、开始/截止时间、快捷打卡链接
- **通知时机**: 
  - 任务开始前提醒
  - 任务即将截止提醒
  - 连续打卡里程碑提醒
- **通知类型**: 使用 `SYSTEM` 类型的个人消息

### 4. 数据统计

#### 4.1 个人统计
- **总览数据**: 
  - 累计创建计划数
  - 累计打卡次数
  - 总完成率
  - 最长连续打卡天数
- **周期统计**:
  - 本周完成情况
  - 本月完成情况
  - 自定义时间段统计
- **趋势图表**:
  - 每日打卡趋势折线图
  - 各类型计划饼图
  - 完成率变化曲线

#### 4.2 成就系统
- **连续打卡成就**: 连续7天/30天/100天打卡徽章
- **完成数量成就**: 累计完成100/500/1000次任务
- **早起打卡成就**: 早上6点前完成打卡
- **全勤成就**: 某月全勤打卡

### 5. 积分联动

#### 5.1 积分奖励规则
- **单次打卡**: 每次完成打卡获得10积分
- **连续打卡**: 连续7天额外奖励50积分
- **计划完成**: 完成整个计划额外奖励100积分
- **成就解锁**: 解锁成就获得对应积分奖励

#### 5.2 积分类型扩展
```java
public enum PointsType {
    ADMIN_GRANT(1, "后台发放"),
    CHECK_IN(2, "打卡积分"),
    PLAN_COMPLETE(3, "计划打卡"), // 新增
    ACHIEVEMENT(4, "成就奖励");  // 新增
}
```

## 🔧 技术实现方案

### 1. 数据库设计

#### 1.1 计划表（user_plan）
```sql
CREATE TABLE user_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '计划ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    plan_name VARCHAR(100) NOT NULL COMMENT '计划名称',
    plan_desc VARCHAR(500) COMMENT '计划描述',
    plan_type TINYINT NOT NULL COMMENT '计划类型：1-刷题 2-学习 3-阅读 4-运动 5-自定义',
    target_type TINYINT NOT NULL COMMENT '目标类型：1-数量 2-时长 3-次数',
    target_value INT NOT NULL COMMENT '目标值',
    target_unit VARCHAR(20) COMMENT '目标单位（道/小时/次）',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE COMMENT '结束日期（NULL表示长期）',
    daily_start_time TIME COMMENT '每日开始时间',
    daily_end_time TIME COMMENT '每日截止时间',
    repeat_type TINYINT NOT NULL DEFAULT 1 COMMENT '重复类型：1-每日 2-工作日 3-周末 4-自定义',
    repeat_days VARCHAR(20) COMMENT '自定义重复日（如：1,2,3,4,5）',
    remind_before INT DEFAULT 30 COMMENT '提前提醒分钟数',
    remind_deadline INT DEFAULT 10 COMMENT '截止提醒分钟数',
    remind_channels VARCHAR(50) DEFAULT 'notification' COMMENT '提醒渠道：notification,webpush,email',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-已删除 1-进行中 2-已暂停 3-已完成 4-已过期',
    total_checkin_days INT DEFAULT 0 COMMENT '累计打卡天数',
    current_streak INT DEFAULT 0 COMMENT '当前连续打卡天数',
    max_streak INT DEFAULT 0 COMMENT '最长连续打卡天数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_user_status (user_id, status),
    INDEX idx_start_date (start_date),
    INDEX idx_end_date (end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户计划表';
```

#### 1.2 打卡记录表（plan_checkin_record）
```sql
CREATE TABLE plan_checkin_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    plan_id BIGINT NOT NULL COMMENT '计划ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    checkin_date DATE NOT NULL COMMENT '打卡日期',
    checkin_time DATETIME NOT NULL COMMENT '打卡时间',
    complete_value INT COMMENT '完成数量',
    complete_content VARCHAR(500) COMMENT '完成内容描述',
    remark VARCHAR(500) COMMENT '心得备注',
    attachment_url VARCHAR(500) COMMENT '附件URL',
    is_supplement TINYINT DEFAULT 0 COMMENT '是否补卡：0-否 1-是',
    points_earned INT DEFAULT 0 COMMENT '获得积分',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY uk_plan_date (plan_id, checkin_date),
    INDEX idx_user_id (user_id),
    INDEX idx_plan_id (plan_id),
    INDEX idx_checkin_date (checkin_date),
    INDEX idx_user_date (user_id, checkin_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计划打卡记录表';
```

#### 1.3 提醒任务表（plan_remind_task）
```sql
CREATE TABLE plan_remind_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    plan_id BIGINT NOT NULL COMMENT '计划ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    remind_type TINYINT NOT NULL COMMENT '提醒类型：1-开始提醒 2-截止提醒',
    remind_time DATETIME NOT NULL COMMENT '提醒时间',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待发送 1-已发送 2-已取消',
    send_time DATETIME COMMENT '实际发送时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_remind_time (remind_time),
    INDEX idx_status (status),
    INDEX idx_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计划提醒任务表';
```

### 2. 后端接口设计

#### 2.1 计划管理接口（/user/plan）
```
POST /user/plan/create              # 创建计划
PUT  /user/plan/update              # 更新计划
DELETE /user/plan/{id}              # 删除计划
GET  /user/plan/{id}                # 获取计划详情
POST /user/plan/list                # 获取计划列表（分页）
PUT  /user/plan/{id}/pause          # 暂停计划
PUT  /user/plan/{id}/resume         # 恢复计划
```

#### 2.2 打卡接口（/user/plan/checkin）
```
POST /user/plan/checkin             # 执行打卡
POST /user/plan/checkin/supplement  # 补打卡
GET  /user/plan/{id}/checkin/list   # 获取打卡记录列表
GET  /user/plan/today-tasks         # 获取今日任务列表
GET  /user/plan/checkin/calendar    # 获取打卡日历数据
```

#### 2.3 统计接口（/user/plan/stats）
```
GET  /user/plan/stats/overview      # 获取统计总览
GET  /user/plan/stats/trend         # 获取趋势数据
GET  /user/plan/stats/achievements  # 获取成就列表
```

#### 2.4 提醒设置接口（/user/plan/remind）
```
POST /user/plan/remind/subscribe    # 订阅 Web Push
DELETE /user/plan/remind/unsubscribe # 取消订阅
PUT  /user/plan/remind/settings     # 更新提醒设置
GET  /user/plan/remind/settings     # 获取提醒设置
```

### 3. 提醒服务实现

#### 3.1 定时任务调度
```java
@Component
public class PlanRemindScheduler {
    
    @Autowired
    private PlanRemindService remindService;
    
    /**
     * 每分钟扫描待发送的提醒任务
     */
    @Scheduled(cron = "0 * * * * ?")
    public void scanRemindTasks() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMinuteLater = now.plusMinutes(1);
        
        // 查询即将到期的提醒任务
        List<PlanRemindTask> tasks = remindService.getPendingTasks(now, oneMinuteLater);
        
        for (PlanRemindTask task : tasks) {
            remindService.sendRemind(task);
        }
    }
    
    /**
     * 每天凌晨生成当日的提醒任务
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void generateDailyRemindTasks() {
        remindService.generateTodayTasks();
    }
}
```

#### 3.2 Web Push 服务
```java
@Service
public class WebPushService {
    
    private final String publicKey = "VAPID_PUBLIC_KEY";
    private final String privateKey = "VAPID_PRIVATE_KEY";
    
    /**
     * 发送 Web Push 通知
     */
    public void sendPushNotification(Long userId, PushMessage message) {
        List<UserPushSubscription> subscriptions = getActiveSubscriptions(userId);
        
        for (UserPushSubscription sub : subscriptions) {
            try {
                Notification notification = new Notification(
                    sub.getEndpoint(),
                    sub.getP256dhKey(),
                    sub.getAuthKey(),
                    message.toJson()
                );
                
                PushService pushService = new PushService(publicKey, privateKey);
                pushService.send(notification);
            } catch (Exception e) {
                // 推送失败，标记订阅失效
                markSubscriptionInvalid(sub.getId());
            }
        }
    }
}
```

#### 3.3 多渠道提醒策略
```java
@Service
public class PlanRemindService {
    
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private WebPushService webPushService;
    @Autowired
    private EmailService emailService;
    
    public void sendRemind(PlanRemindTask task) {
        String[] channels = task.getRemindChannels().split(",");
        UserPlan plan = getPlanById(task.getPlanId());
        
        RemindContent content = buildRemindContent(plan, task);
        
        for (String channel : channels) {
            switch (channel) {
                case "notification":
                    // 站内通知
                    notificationService.sendSystemNotification(
                        task.getUserId(),
                        content.getTitle(),
                        content.getBody(),
                        "/plan/" + plan.getId()
                    );
                    break;
                case "webpush":
                    // 浏览器推送
                    webPushService.sendPushNotification(
                        task.getUserId(),
                        new PushMessage(content.getTitle(), content.getBody(), "/plan")
                    );
                    break;
                case "email":
                    // 邮件提醒
                    emailService.sendPlanRemind(task.getUserId(), content);
                    break;
            }
        }
        
        // 更新任务状态
        updateTaskStatus(task.getId(), RemindStatus.SENT);
    }
}
```

### 4. 前端实现要点

#### 4.1 Service Worker 注册（Web Push）
```javascript
// public/sw.js
self.addEventListener('push', function(event) {
  const data = event.data.json();
  
  const options = {
    body: data.body,
    icon: '/logo.png',
    badge: '/badge.png',
    data: {
      url: data.url
    },
    actions: [
      { action: 'checkin', title: '立即打卡' },
      { action: 'later', title: '稍后提醒' }
    ]
  };
  
  event.waitUntil(
    self.registration.showNotification(data.title, options)
  );
});

self.addEventListener('notificationclick', function(event) {
  event.notification.close();
  
  if (event.action === 'checkin') {
    event.waitUntil(
      clients.openWindow(event.notification.data.url)
    );
  }
});
```

#### 4.2 订阅 Web Push
```javascript
// utils/webpush.js
export async function subscribePush() {
  if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
    console.warn('浏览器不支持 Web Push');
    return null;
  }
  
  const registration = await navigator.serviceWorker.ready;
  
  const subscription = await registration.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: urlBase64ToUint8Array(VAPID_PUBLIC_KEY)
  });
  
  // 将订阅信息发送到服务器保存
  await api.post('/user/plan/remind/subscribe', {
    endpoint: subscription.endpoint,
    p256dh: btoa(String.fromCharCode(...new Uint8Array(subscription.getKey('p256dh')))),
    auth: btoa(String.fromCharCode(...new Uint8Array(subscription.getKey('auth'))))
  });
  
  return subscription;
}
```

## 📱 用户体验流程

### 1. 创建计划流程
1. 用户点击"创建计划"按钮
2. 选择计划类型（刷题/学习/自定义等）
3. 填写计划名称和目标
4. 设置时间范围和重复规则
5. 配置提醒方式和时间
6. 确认创建，系统生成提醒任务

### 2. 日常打卡流程
1. 用户收到任务开始提醒
2. 进入今日任务页面查看待完成任务
3. 完成任务后点击打卡
4. 填写完成内容/数量（可选）
5. 系统记录打卡并发放积分
6. 更新连续打卡天数和统计数据

### 3. 查看统计流程
1. 进入统计页面查看总览数据
2. 查看打卡日历和趋势图表
3. 查看已解锁的成就徽章
4. 查看各计划的完成率排名

## 📐 前端界面设计

### 1. 今日任务页面
```
┌─────────────────────────────────────────┐
│ 📅 今日任务                    2025-12-10 │
├─────────────────────────────────────────┤
│ ┌─────────────────────────────────────┐ │
│ │ 🔥 每日刷3道算法题                   │ │
│ │ ⏰ 09:00 - 12:00  剩余 2小时15分     │ │
│ │ 📊 目标：3道  已完成：1道            │ │
│ │                         [打卡]       │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ ✅ 阅读技术文档30分钟    已完成      │ │
│ │ ⏰ 14:00 - 15:00                    │ │
│ │ 📝 完成内容：阅读了Vue3组合式API文档  │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ ⏳ 背诵10个英语单词                  │ │
│ │ ⏰ 20:00 - 21:00  尚未开始           │ │
│ │ 📊 目标：10个                        │ │
│ └─────────────────────────────────────┘ │
├─────────────────────────────────────────┤
│ 📈 今日进度：2/3 完成  🔥 连续打卡：5天  │
└─────────────────────────────────────────┘
```

### 2. 计划详情页面
```
┌─────────────────────────────────────────┐
│ ← 每日刷3道算法题                        │
├─────────────────────────────────────────┤
│ 📊 完成进度                              │
│ ████████████░░░░░░░░░░ 60%              │
│ 已完成 18/30 天                          │
├─────────────────────────────────────────┤
│ 🔥 连续打卡                              │
│ 当前：5天  最长：12天                    │
├─────────────────────────────────────────┤
│ 📅 打卡日历  < 2025年12月 >              │
│ 日 一 二 三 四 五 六                     │
│  1  2  3  4  5  6  7                    │
│ ✓  ✓  ✓  -  ✓  ✓  ✓                    │
│  8  9 10 11 12 13 14                    │
│ ✓  ✓  ○                                │
├─────────────────────────────────────────┤
│ ⏰ 提醒设置                              │
│ 开始提醒：08:45  截止提醒：11:50         │
│ 渠道：站内通知 ✓  浏览器推送 ✓  邮件 ○   │
├─────────────────────────────────────────┤
│ [编辑计划]  [暂停计划]  [删除计划]       │
└─────────────────────────────────────────┘
```

### 3. 统计总览页面
```
┌─────────────────────────────────────────┐
│ 📊 我的计划统计                          │
├─────────────────────────────────────────┤
│ ┌─────────┐ ┌─────────┐ ┌─────────┐    │
│ │  5      │ │  128    │ │  86%    │    │
│ │ 进行中  │ │ 累计打卡│ │ 完成率  │    │
│ └─────────┘ └─────────┘ └─────────┘    │
├─────────────────────────────────────────┤
│ 📈 本周打卡趋势                          │
│     7 ─┐                                │
│     5 ─┼─────●─────●                    │
│     3 ─┼──●─────●─────●                 │
│     1 ─┼─────────────────●              │
│        └──周一─周二─周三─周四─周五──    │
├─────────────────────────────────────────┤
│ 🏆 成就墙                                │
│ [🔥7天] [🌟30天] [⭐100次] [🌅早起]      │
│                                         │
│ 下一个成就：连续打卡30天  还需25天       │
└─────────────────────────────────────────┘
```

## ✅ 实现范围

### 第一期功能（v1.0.0）
- ✅ **计划CRUD**: 创建、编辑、删除、查询计划
- ✅ **基础打卡**: 今日任务展示和打卡功能
- ✅ **站内通知**: 复用notification模块发送提醒
- ✅ **连续打卡**: 连续打卡天数统计和显示
- ✅ **基础统计**: 完成率、打卡天数等基础统计
- ✅ **积分联动**: 打卡获得积分奖励

### 第二期功能（v1.1.0）
- 🔲 **Web Push**: 浏览器推送通知功能
- 🔲 **打卡日历**: 日历视图展示打卡情况
- 🔲 **补打卡**: 支持补打最近N天的任务
- 🔲 **邮件提醒**: 可选的邮件提醒功能
- 🔲 **趋势图表**: 打卡趋势和统计图表

### 第三期功能（v1.2.0）
- 🔲 **成就系统**: 成就徽章和解锁奖励
- 🔲 **数据导出**: 导出打卡记录和统计报表
- 🔲 **计划模板**: 提供常用计划模板一键创建
- 🔲 **社交分享**: 分享打卡成果到社区/朋友圈

## 📝 附录

### A. 计划类型枚举
```java
public enum PlanType {
    CODING(1, "刷题计划", "💻"),
    STUDY(2, "学习计划", "📚"),
    READING(3, "阅读计划", "📖"),
    EXERCISE(4, "运动计划", "🏃"),
    CUSTOM(5, "自定义", "✏️");
}
```

### B. 成就类型定义
```java
public enum AchievementType {
    STREAK_7("streak_7", "连续打卡7天", "🔥", 50),
    STREAK_30("streak_30", "连续打卡30天", "🌟", 200),
    STREAK_100("streak_100", "连续打卡100天", "👑", 500),
    TOTAL_100("total_100", "累计打卡100次", "⭐", 100),
    TOTAL_500("total_500", "累计打卡500次", "💎", 300),
    EARLY_BIRD("early_bird", "早起打卡(6点前)", "🌅", 30),
    FULL_MONTH("full_month", "月度全勤", "🏆", 150);
}
```

### C. 提醒消息模板
```
【开始提醒】
标题：📋 {计划名称} 即将开始
内容：您的任务「{计划名称}」将在{开始时间}开始，目标：{目标描述}。加油！

【截止提醒】
标题：⏰ {计划名称} 即将截止
内容：您的任务「{计划名称}」将在{截止时间}截止，还未完成哦~

【连续打卡里程碑】
标题：🎉 恭喜达成{天数}天连续打卡！
内容：您已连续打卡{天数}天，获得{积分}积分奖励！继续保持！
```
