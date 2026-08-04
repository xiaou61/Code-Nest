package com.xiaou.mockinterview.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 新增或更新用户投递记录的请求。
 */
@Data
public class CareerApplicationUpsertRequest {

    @Positive(message = "岗位匹配记录ID不合法")
    private Long matchRecordId;

    @Positive(message = "补短板计划记录ID不合法")
    private Long planRecordId;

    @Positive(message = "模拟面试会话ID不合法")
    private Long mockInterviewSessionId;

    @NotBlank(message = "公司名称不能为空")
    @Size(max = 120, message = "公司名称不能超过120个字符")
    private String companyName;

    @NotBlank(message = "岗位名称不能为空")
    @Size(max = 120, message = "岗位名称不能超过120个字符")
    private String positionName;

    @NotBlank(message = "投递状态不能为空")
    @Pattern(
            regexp = "PREPARING|APPLIED|INTERVIEWING|OFFER|REJECTED|WITHDRAWN",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "投递状态不合法"
    )
    private String status;

    @PastOrPresent(message = "投递日期不能晚于今天")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate appliedDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate nextFollowUpDate;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String note;
}
