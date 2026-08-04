package com.xiaou.web.growthcoach.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 岗位准备闭环的只读状态。
 *
 * <p>将已保存的岗位匹配、补短板计划、模拟面试和用户自报投递事实串联起来，
 * 不创建面试、不自动投递，也不把不同来源的记录自动认定为同一岗位。</p>
 */
@Data
public class GrowthJobPreparationLoopResponse {

    private Long matchRecordId;
    private Long planRecordId;
    private Long mockInterviewSessionId;
    private String targetRole;
    private String stage;
    private String summary;
    private String recommendedDirection;
    private String recommendedDirectionName;
    private List<String> focusSkills = new ArrayList<>();
    private CurrentAction currentAction;
    private List<Step> steps = new ArrayList<>();
    private List<SourceReference> sourceRefs = new ArrayList<>();

    @Data
    public static class CurrentAction {
        private String title;
        private String description;
        private String routePath;
        private Integer expectedMinutes;
        private String expectedChange;
    }

    @Data
    public static class Step {
        private String key;
        private String title;
        /** completed / current / pending / observed */
        private String status;
        private String description;
        private LocalDateTime observedAt;
    }

    @Data
    public static class SourceReference {
        private String sourceType;
        private Long sourceId;
        private String label;
        private LocalDateTime observedAt;
    }
}
