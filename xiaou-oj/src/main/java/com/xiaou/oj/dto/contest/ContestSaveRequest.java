package com.xiaou.oj.dto.contest;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 赛事保存请求
 *
 * @author xiaou
 */
@Data
public class ContestSaveRequest {

    @NotBlank(message = "赛事标题不能为空")
    private String title;

    private String description;

    /**
     * 赛事类型 (weekly/challenge)
     */
    @NotBlank(message = "赛事类型不能为空")
    private String contestType;

    /**
     * 赛事状态 (0=草稿,1=即将开始,2=进行中,3=已结束)
     */
    private Integer status;

    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    @NotEmpty(message = "赛事至少需要1道题目")
    private List<Long> problemIds;
}
