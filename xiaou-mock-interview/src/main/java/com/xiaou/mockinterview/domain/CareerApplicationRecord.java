package com.xiaou.mockinterview.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户在求职闭环中主动记录的一条投递进展。
 *
 * 该记录描述用户自报的投递事实，不等同于平台验证的能力或 Offer 结果。
 */
@Data
@Accessors(chain = true)
public class CareerApplicationRecord {

    private Long id;

    private Long userId;

    private Long sessionId;

    private Long matchRecordId;

    private Long planRecordId;

    private Long mockInterviewSessionId;

    private String companyName;

    private String positionName;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate appliedDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate nextFollowUpDate;

    private String note;

    private Integer deleted;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
