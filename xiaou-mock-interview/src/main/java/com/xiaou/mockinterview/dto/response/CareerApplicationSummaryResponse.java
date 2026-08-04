package com.xiaou.mockinterview.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 投递记录的最小汇总，供求职闭环和成长教练读取。
 */
@Data
public class CareerApplicationSummaryResponse {

    private Integer totalCount;
    private Integer preparingCount;
    private Integer appliedCount;
    private Integer interviewingCount;
    private Integer offerCount;
    private Integer rejectedCount;
    private Integer withdrawnCount;
    private Integer activeCount;
    private Integer dueFollowUpCount;
    private LocalDate nextFollowUpDate;
    private LocalDateTime latestUpdatedAt;
}
