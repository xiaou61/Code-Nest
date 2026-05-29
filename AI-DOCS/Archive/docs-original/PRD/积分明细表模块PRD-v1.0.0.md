# 积分明细表模块 PRD v1.0.0

## 📋 项目概述

### 🎯 项目背景
为了提升用户活跃度和参与度，需要建立一套完整的积分体系。用户可以通过每日打卡获得积分，管理员也可以后台发放积分作为奖励。积分系统采用1000积分=1元的兑换标准，为未来的积分商城功能奠定基础。

### 💡 核心价值
- **用户激励**: 通过积分奖励机制提升用户活跃度和粘性
- **行为引导**: 鼓励用户每日打卡，培养良好使用习惯
- **连续奖励**: 连续打卡递增奖励，提升用户持续参与的动力
- **管理灵活**: 管理员可灵活发放积分，用于活动奖励或补偿
- **数据透明**: 完整的积分明细记录，用户可随时查看积分变动
- **价值量化**: 1000积分=1元的标准，让积分具有实际价值感

## 🚀 功能需求

### 1. 积分类型管理

#### 1.1 积分类型枚举
```java
public enum PointsType {
    ADMIN_GRANT(1, "后台发放", "管理员手动发放的积分奖励"),
    CHECK_IN(2, "打卡积分", "用户每日打卡获得的积分");
    
    private final int code;
    private final String desc;
    private final String detail;
}
```

#### 1.2 积分汇率标准
- **基础汇率**: 1000积分 = 1元人民币
- **显示方式**: 在用户界面显示积分对应的人民币价值
- **计算精度**: 支持小数点后2位显示

### 2. 打卡积分功能

#### 2.1 基础打卡奖励
- **每日打卡**: 用户每天可进行一次打卡
- **基础积分**: 首次打卡或中断后重新开始，奖励50积分
- **打卡限制**: 每天只能打卡一次，重复打卡无效
- **打卡时间**: 按自然日计算，每天00:00-23:59为一个打卡周期

#### 2.2 连续打卡递增机制
**奖励规则：**
- 连续1天：50积分（基础奖励）
- 连续2天：60积分（+10积分递增）
- 连续3天：70积分（+10积分递增）
- 连续4天：80积分（+10积分递增）
- 连续5天：90积分（+10积分递增）
- 连续6天：100积分（+10积分递增）
- 连续7天：150积分（周奖励+50积分）
- 连续8天：60积分（重新开始递增）
- ...重复7天周期

**特殊规则：**
- 每满7天连续打卡，第7天额外奖励50积分
- 连续打卡中断后，重新从50积分开始
- 连续打卡天数无上限，按7天为一个周期循环

#### 2.3 打卡状态管理
- **今日状态**: 显示当前用户今日是否已打卡
- **连续天数**: 实时显示当前连续打卡天数
- **下次奖励**: 提示明天打卡可获得的积分数
- **打卡历史**: 展示用户历史打卡记录日历视图

### 3. 后台发放积分功能

#### 3.1 管理员发放功能
- **批量发放**: 支持选择多个用户批量发放积分
- **单个发放**: 针对特定用户发放积分奖励
- **发放原因**: 必须填写发放原因（活动奖励、bug反馈奖励等）
- **积分数量**: 支持自定义发放积分数量，最小1积分，最大10000积分
- **即时生效**: 发放后立即到账，用户可在积分明细中查看

### 4. 积分明细展示

#### 4.1 用户端明细查看
- **明细列表**: 按时间倒序显示积分变动明细
- **分页展示**: 支持分页查看，每页20条记录
- **明细内容**: 显示时间、积分变动、类型、原因/描述
- **余额显示**: 实时显示当前积分余额和对应人民币价值
- **筛选功能**: 支持按积分类型、时间范围筛选明细

#### 4.2 明细信息结构
- **变动时间**: 精确到秒的时间记录
- **积分变动**: 显示+xxx或-xxx积分（目前只有增加）
- **积分类型**: 显示类型标签（打卡积分/后台发放）
- **详细说明**: 
  - 打卡积分：连续第X天打卡
  - 后台发放：显示管理员填写的发放原因
- **余额快照**: 记录变动后的积分余额

### 5. 管理端功能

#### 5.1 积分管理界面
- **用户积分**: 查看所有用户的积分余额排行
- **明细管理**: 查看所有用户的积分明细记录
- **发放操作**: 快速发放积分给指定用户
- **统计分析**: 积分发放统计、打卡活跃度统计

#### 5.2 数据统计功能
- **总体统计**: 系统总积分发放量、活跃打卡用户数
- **发放统计**: 按类型统计积分发放量（打卡vs后台发放）
- **用户排名**: 积分排行榜，显示Top 100用户
- **趋势分析**: 每日积分发放量趋势图表

## 🔧 技术实现方案

### 1. 数据库设计

#### 1.1 用户积分余额表（user_points_balance）
```sql
CREATE TABLE user_points_balance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    total_points INT DEFAULT 0 COMMENT '总积分余额',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_user_id (user_id),
    INDEX idx_points_desc (total_points DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '用户积分余额表';
```

#### 1.2 积分明细表（user_points_detail）
```sql
CREATE TABLE user_points_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '明细ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    points_change INT NOT NULL COMMENT '积分变动数量（正数为增加）',
    points_type TINYINT NOT NULL COMMENT '积分类型：1-后台发放 2-打卡积分',
    description VARCHAR(500) COMMENT '变动描述/原因',
    balance_after INT NOT NULL COMMENT '变动后余额',
    admin_id BIGINT COMMENT '操作管理员ID（后台发放时记录）',
    continuous_days INT COMMENT '连续打卡天数（打卡积分时记录）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time DESC),
    INDEX idx_user_time (user_id, create_time DESC),
    INDEX idx_points_type (points_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '积分明细表';
```

#### 1.3 用户打卡记录优化方案（位图存储）

**方案对比分析：**

**传统方案问题：**
- 每个用户每天一条记录，1万用户一年产生365万条记录
- 存储空间占用大，查询性能随数据量增长下降
- 连续天数计算需要扫描历史记录

**位图优化方案：**
```sql
CREATE TABLE user_checkin_bitmap (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    year_month VARCHAR(7) NOT NULL COMMENT '年月，格式：YYYY-MM',
    checkin_bitmap BIGINT DEFAULT 0 COMMENT '打卡位图，每位代表当月某天',
    continuous_days INT DEFAULT 0 COMMENT '当前连续打卡天数',
    last_checkin_date DATE COMMENT '最后打卡日期',
    total_checkin_days INT DEFAULT 0 COMMENT '当月总打卡天数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_user_year_month (user_id, year_month),
    INDEX idx_user_id (user_id),
    INDEX idx_last_checkin (last_checkin_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '用户打卡位图表';
```

**位图存储优势：**
- **空间效率**: 1万用户一年仅需12万条记录（按月存储）
- **查询性能**: 位运算操作，毫秒级响应
- **扩展性强**: 支持大规模用户和长期数据存储
- **统计便利**: 可快速统计任意时间段打卡情况

**性能对比分析：**

| 指标 | 传统方案 | 位图方案 | 提升比例 |
|------|----------|----------|----------|
| **存储空间** | 1万用户/年 = 365万条记录 | 1万用户/年 = 12万条记录 | **96.7%减少** |
| **查询连续天数** | 需扫描N天历史记录 | O(1)直接读取缓存值 | **99%+性能提升** |
| **月度统计** | COUNT查询扫描全表 | Long.bitCount()位运算 | **1000%+性能提升** |
| **跨月查询** | 多表JOIN查询 | 最多查询3条记录 | **90%+性能提升** |
| **并发处理** | 大量INSERT操作 | 少量UPDATE操作 | **减少锁冲突** |

**位图实现细节：**
```java
// 例如：用户在2025年1月的1,3,5,7,15,20天打卡
// 位图二进制表示：00000000000000010000100101010101
// 对应十进制：1085781
// 存储空间：仅需一个BIGINT字段（8字节）
// 传统方案需要：6条记录 × 每条约50字节 = 300字节
// 空间节省：95%+

// 快速操作示例
long bitmap = 1085781L;
// 检查第15天是否打卡：(bitmap & (1L << 14)) != 0  -> true
// 统计打卡天数：Long.bitCount(bitmap)  -> 6
// 设置第16天打卡：bitmap | (1L << 15)  -> 新的位图值
```

### 2. 后端接口设计

#### 2.1 用户端积分接口（/user/points）
```
GET /user/points/balance              # 获取积分余额
POST /user/points/checkin             # 每日打卡
GET /user/points/checkin-status       # 获取打卡状态
POST /user/points/detail              # 获取积分明细
GET /user/points/checkin-calendar     # 获取打卡日历（基于位图）
GET /user/points/checkin-statistics  # 获取打卡统计数据
```

#### 2.2 管理端积分接口（/admin/points）
```
POST /admin/points/grant              # 发放积分
POST /admin/points/user-list          # 获取用户积分列表
POST /admin/points/detail-list        # 获取积分明细列表
GET /admin/points/statistics          # 获取积分统计数据
POST /admin/points/batch-grant        # 批量发放积分
```

#### 2.3 接口请求/响应示例

**用户打卡：**
```json
// 请求
POST /user/points/checkin

// 响应
{
    "code": 200,
    "message": "打卡成功",
    "data": {
        "pointsEarned": 70,
        "continuousDays": 3,
        "nextDayPoints": 80,
        "totalBalance": 1520,
        "balanceYuan": "1.52"
    }
}
```

**获取积分明细：**
```json
// 请求
POST /user/points/detail
{
    "pageNum": 1,
    "pageSize": 20,
    "pointsType": null,
    "startTime": "2025-01-01 00:00:00",
    "endTime": "2025-12-31 23:59:59"
}

// 响应
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "records": [
            {
                "id": 123,
                "pointsChange": 70,
                "pointsType": 2,
                "pointsTypeDesc": "打卡积分",
                "description": "连续第3天打卡",
                "balanceAfter": 1520,
                "continuousDays": 3,
                "createTime": "2025-09-23 08:30:00"
            },
            {
                "id": 122,
                "pointsChange": 500,
                "pointsType": 1,
                "pointsTypeDesc": "后台发放",
                "description": "积极参与社区活动奖励",
                "balanceAfter": 1450,
                "adminName": "管理员",
                "createTime": "2025-09-22 15:20:00"
            }
        ],
        "total": 50,
        "current": 1,
        "size": 20
    }
}
```

**管理员发放积分：**
```json
// 请求
POST /admin/points/grant
{
    "userId": 12345,
    "points": 500,
    "reason": "积极参与社区活动奖励"
}

// 响应
{
    "code": 200,
    "message": "积分发放成功",
    "data": {
        "detailId": 789,
        "userBalance": 1950,
        "balanceYuan": "1.95"
    }
}
```

**获取打卡日历（位图优化版）：**
```json
// 请求
GET /user/points/checkin-calendar?yearMonth=2025-01

// 响应
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "yearMonth": "2025-01",
        "checkinBitmap": 1085781,
        "checkinDays": [1, 3, 5, 7, 15, 20],
        "totalCheckinDays": 6,
        "continuousDays": 3,
        "lastCheckinDate": "2025-01-20",
        "monthlyStats": {
            "totalDays": 31,
            "checkinRate": "19.35%",
            "pointsEarned": 420
        },
        "calendarGrid": {
            "1": { "checked": true, "points": 50 },
            "2": { "checked": false, "points": 0 },
            "3": { "checked": true, "points": 60 },
            "4": { "checked": false, "points": 0 },
            "5": { "checked": true, "points": 70 },
            "6": { "checked": false, "points": 0 },
            "7": { "checked": true, "points": 80 },
            "15": { "checked": true, "points": 50 },
            "20": { "checked": true, "points": 60 }
        }
    }
}
```

**获取打卡统计数据：**
```json
// 请求
GET /user/points/checkin-statistics?months=3

// 响应
{
    "code": 200,
    "message": "获取成功", 
    "data": {
        "totalCheckinDays": 45,
        "currentContinuousDays": 5,
        "maxContinuousDays": 12,
        "totalPointsFromCheckin": 2850,
        "averagePointsPerDay": 63.33,
        "monthlyStats": [
            {
                "yearMonth": "2025-01",
                "checkinDays": 20,
                "pointsEarned": 1200,
                "checkinRate": "64.52%"
            },
            {
                "yearMonth": "2024-12", 
                "checkinDays": 15,
                "pointsEarned": 950,
                "checkinRate": "48.39%"
            },
            {
                "yearMonth": "2024-11",
                "checkinDays": 10,
                "pointsEarned": 700,
                "checkinRate": "33.33%"
            }
        ]
    }
}
```

### 3. 业务逻辑实现

#### 3.1 打卡积分计算逻辑
```java
public class CheckinPointsCalculator {
    
    private static final int BASE_POINTS = 50;      // 基础积分
    private static final int INCREMENT_POINTS = 10; // 递增积分
    private static final int WEEK_BONUS = 50;       // 周奖励
    
    public int calculatePoints(int continuousDays) {
        int cycleDay = (continuousDays - 1) % 7 + 1; // 1-7的循环
        
        if (cycleDay == 7) {
            // 第7天：基础+递增+周奖励
            return BASE_POINTS + (cycleDay - 1) * INCREMENT_POINTS + WEEK_BONUS;
        } else {
            // 非第7天：基础+递增
            return BASE_POINTS + (cycleDay - 1) * INCREMENT_POINTS;
        }
    }
}
```

#### 3.2 位图存储业务逻辑实现

**位图操作工具类：**
```java
public class CheckinBitmapUtil {
    
    /**
     * 设置某天的打卡状态
     * @param bitmap 当前位图
     * @param day 当月第几天（1-31）
     * @return 更新后的位图
     */
    public static long setBit(long bitmap, int day) {
        return bitmap | (1L << (day - 1));
    }
    
    /**
     * 检查某天是否已打卡
     * @param bitmap 位图
     * @param day 当月第几天（1-31）
     * @return 是否已打卡
     */
    public static boolean isCheckedIn(long bitmap, int day) {
        return (bitmap & (1L << (day - 1))) != 0;
    }
    
    /**
     * 计算当月打卡天数
     * @param bitmap 位图
     * @return 打卡天数
     */
    public static int countCheckinDays(long bitmap) {
        return Long.bitCount(bitmap);
    }
    
    /**
     * 获取位图的二进制字符串（用于调试）
     * @param bitmap 位图
     * @return 二进制字符串
     */
    public static String toBinaryString(long bitmap) {
        return String.format("%31s", Long.toBinaryString(bitmap)).replace(' ', '0');
    }
}
```

**连续打卡天数计算优化：**
```java
public class OptimizedContinuousCalculator {
    
    public int calculateContinuousDays(Long userId, LocalDate today) {
        // 1. 查询用户当前月份的位图记录
        String currentYearMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        UserCheckinBitmap currentRecord = getBitmapRecord(userId, currentYearMonth);
        
        if (currentRecord == null) {
            return 1; // 首次打卡
        }
        
        // 2. 检查是否连续打卡
        LocalDate lastCheckinDate = currentRecord.getLastCheckinDate();
        if (lastCheckinDate == null) {
            return 1; // 首次打卡
        }
        
        LocalDate yesterday = today.minusDays(1);
        
        if (lastCheckinDate.equals(yesterday)) {
            // 连续打卡，直接返回缓存的连续天数+1
            return currentRecord.getContinuousDays() + 1;
        } else if (lastCheckinDate.equals(today)) {
            // 今天已经打过卡
            return currentRecord.getContinuousDays();
        } else {
            // 中断了，重新开始
            return 1;
        }
    }
    
    /**
     * 跨月连续打卡天数计算（如果需要精确计算跨月情况）
     */
    public int calculateCrossMonthContinuous(Long userId, LocalDate today) {
        int continuousDays = 0;
        LocalDate checkDate = today;
        
        // 最多检查3个月的数据（优化性能）
        for (int monthCount = 0; monthCount < 3; monthCount++) {
            String yearMonth = checkDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            UserCheckinBitmap record = getBitmapRecord(userId, yearMonth);
            
            if (record == null || record.getCheckinBitmap() == 0) {
                break; // 该月没有打卡记录，中断
            }
            
            // 从当前日期向前检查连续性
            int dayOfMonth = checkDate.getDayOfMonth();
            for (int day = dayOfMonth; day >= 1; day--) {
                if (CheckinBitmapUtil.isCheckedIn(record.getCheckinBitmap(), day)) {
                    continuousDays++;
                    checkDate = checkDate.minusDays(1);
                } else {
                    return continuousDays; // 遇到未打卡的日期，结束
                }
                
                // 如果检查到月初，需要检查上个月
                if (day == 1) {
                    checkDate = checkDate.minusDays(1); // 跳到上个月最后一天
                    break;
                }
            }
        }
        
        return continuousDays;
    }
}
```

**打卡业务逻辑：**
```java
@Service
public class CheckinService {
    
    @Transactional
    public CheckinResult doCheckin(Long userId) {
        LocalDate today = LocalDate.now();
        String yearMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        int dayOfMonth = today.getDayOfMonth();
        
        // 1. 获取或创建当月位图记录
        UserCheckinBitmap record = getBitmapRecord(userId, yearMonth);
        if (record == null) {
            record = createNewBitmapRecord(userId, yearMonth);
        }
        
        // 2. 检查今天是否已打卡
        if (CheckinBitmapUtil.isCheckedIn(record.getCheckinBitmap(), dayOfMonth)) {
            throw new BusinessException("今日已打卡，请勿重复操作");
        }
        
        // 3. 计算连续打卡天数
        int continuousDays = calculateContinuousDays(userId, today);
        
        // 4. 计算积分奖励
        int pointsEarned = CheckinPointsCalculator.calculatePoints(continuousDays);
        
        // 5. 更新位图记录
        long newBitmap = CheckinBitmapUtil.setBit(record.getCheckinBitmap(), dayOfMonth);
        record.setCheckinBitmap(newBitmap);
        record.setContinuousDays(continuousDays);
        record.setLastCheckinDate(today);
        record.setTotalCheckinDays(CheckinBitmapUtil.countCheckinDays(newBitmap));
        
        // 6. 保存打卡记录和积分记录
        updateBitmapRecord(record);
        addPointsRecord(userId, pointsEarned, continuousDays);
        
        return CheckinResult.builder()
                .pointsEarned(pointsEarned)
                .continuousDays(continuousDays)
                .nextDayPoints(CheckinPointsCalculator.calculatePoints(continuousDays + 1))
                .build();
    }
}
```

### 4. 前端界面设计

#### 4.1 用户端积分页面
```
┌─────────────────────────────────────────┐
│ 我的积分                                  │
├─────────────────────────────────────────┤
│ 🎯 积分余额：1,520积分 (≈1.52元)          │
│                                         │
│ 📅 今日打卡                              │
│ ┌─────────────────┐                     │
│ │   连续打卡 3天   │  [立即打卡] 获得70积分  │
│ │   明天可得 80积分 │                     │
│ └─────────────────┘                     │
├─────────────────────────────────────────┤
│ 📊 积分明细                              │
│ [全部] [打卡积分] [后台发放]              │
│                                         │
│ +70积分  打卡积分  连续第3天打卡           │
│ 09-23 08:30  余额：1,520积分              │
│                                         │
│ +500积分  后台发放  社区活动奖励           │
│ 09-22 15:20  余额：1,450积分              │
│                                         │
│ [加载更多]                               │
└─────────────────────────────────────────┘
```

#### 4.2 管理端积分管理页面
```
┌─────────────────────────────────────────┐
│ 积分管理                                  │
├─────────────────────────────────────────┤
│ 📊 数据概览                              │
│ 总积分发放：125,600  打卡积分：78,400     │
│ 后台发放：47,200    活跃用户：156人       │
├─────────────────────────────────────────┤
│ 🎁 发放积分                              │
│ 用户ID：[12345] 积分：[500] 原因：[___]    │
│ [发放积分] [批量发放]                     │
├─────────────────────────────────────────┤
│ 👥 用户积分排行                          │
│ 1. 张三    2,850积分  连续打卡15天        │
│ 2. 李四    2,340积分  连续打卡8天         │
│ 3. 王五    1,920积分  连续打卡12天        │
│ [查看更多]                               │
└─────────────────────────────────────────┘
```

## 📱 用户体验流程

### 1. 用户打卡流程
1. 用户进入积分页面
2. 查看当前积分余额和连续打卡天数
3. 点击"立即打卡"按钮
4. 系统验证是否今日已打卡
5. 计算连续天数和应得积分
6. 更新积分余额和打卡记录
7. 显示打卡成功和获得积分数

### 2. 管理员发放流程
1. 管理员进入积分管理页面
2. 输入用户ID、积分数量和发放原因
3. 点击发放积分按钮
4. 系统验证用户存在性和积分数量
5. 创建积分明细记录
6. 更新用户积分余额
7. 显示发放成功提示

## ✅ 实现范围

### 第一期功能（v1.4.0）
- ✅ **积分类型枚举**: 后台发放和打卡积分两种类型
- ✅ **基础打卡**: 每日打卡功能，50积分起始
- ✅ **连续打卡奖励**: 7天周期的递增奖励机制
- ✅ **位图存储优化**: 用位图替代传统打卡记录，96.7%存储空间节省
- ✅ **高性能查询**: O(1)连续天数查询，位运算统计打卡数据
- ✅ **积分明细**: 完整的积分变动记录和查询
- ✅ **管理员发放**: 后台积分发放功能
- ✅ **余额管理**: 用户积分余额统计和显示
- ✅ **汇率显示**: 1000积分=1元的价值显示
- ✅ **打卡日历**: 基于位图的月度打卡日历展示

