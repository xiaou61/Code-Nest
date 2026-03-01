package com.xiaou.mockinterview.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 求职闭环动作项
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class CareerLoopAction {

    private Long id;

    private Long sessionId;

    private String stage;

    private String actionType;

    private String title;

    private String description;

    private String priority;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate dueDate;

    private String source;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}

