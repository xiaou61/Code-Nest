package com.xiaou.sre.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 一次不可变的 SRE 调查人工反馈修订。
 *
 * @author xiaou
 */
@Data
public class SreInvestigationFeedback {

    private Long id;
    private Long runId;
    private String accuracy;
    private String gapType;
    private String note;
    private String expectedConclusion;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createTime;
}
