package com.xiaou.web.growthcoach.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页唯一今日动作。
 */
@Data
public class GrowthCoachTodayActionResponse {

    private Long taskId;
    private String title;
    private Integer plannedMinutes;
    private String reason;
    private String expectedChange;
    private String startRoute;
    private Integer selectionVersion;
    private List<GrowthEvidenceReference> evidenceRefs = new ArrayList<>();
}
