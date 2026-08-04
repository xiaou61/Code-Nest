package com.xiaou.mockinterview.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 当前登录用户可读取的投递记录。
 */
@Data
public class CareerApplicationResponse {

    private Long id;
    private Long matchRecordId;
    private Long planRecordId;
    private Long mockInterviewSessionId;
    private String companyName;
    private String positionName;
    private String status;
    private String statusLabel;
    private LocalDate appliedDate;
    private LocalDate nextFollowUpDate;
    private String note;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
