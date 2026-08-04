package com.xiaou.web.growthcoach.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 用户当前求职闭环中最应执行的一项已存在动作。
 */
@Data
public class GrowthCareerNextActionResponse {

    private Long actionId;
    private String stage;
    private String actionType;
    private String title;
    private String description;
    private String priority;
    private String status;
    private LocalDate dueDate;
    private String routePath;
    private String expectedChange;
}
