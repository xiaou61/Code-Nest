package com.xiaou.sre.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * SRE 事故工作台汇总。
 *
 * @author xiaou
 */
@Data
public class SreIncidentSummary {

    private Long totalCount;
    private Long activeCount;
    private Long openCount;
    private Long acknowledgedCount;
    private Long criticalActiveCount;
    private Long warningActiveCount;
    private Long resolvedCount;
    private LocalDateTime lastObservedAt;
}
