package com.xiaou.web.growthcoach.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 基于用户自有投递记录形成的只读求职进展摘要。
 */
@Data
public class GrowthApplicationOutcomeResponse {

    private Integer totalCount;
    private Integer activeCount;
    private Integer appliedCount;
    private Integer interviewingCount;
    private Integer offerCount;
    private Integer rejectedCount;
    private Integer dueFollowUpCount;
    private LocalDate nextFollowUpDate;
    private LocalDateTime latestUpdatedAt;
    private NextAction nextAction;

    @Data
    public static class NextAction {
        private String title;
        private String description;
        private String routePath;
        private String expectedChange;
        private LocalDate dueDate;
    }
}
