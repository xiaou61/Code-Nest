package com.xiaou.web.growthcoach.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Growth Coach 主动提醒的幂等记录。
 */
@Data
public class GrowthCoachNudge {

    private Long id;
    private Long userId;
    private String nudgeKey;
    private String nudgeType;
    private String level;
    private String title;
    private String content;
    private String routePath;
    private String status;
    private Long notificationId;
    private LocalDateTime sentAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
