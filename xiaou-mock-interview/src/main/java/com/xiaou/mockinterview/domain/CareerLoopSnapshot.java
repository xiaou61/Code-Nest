package com.xiaou.mockinterview.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 求职闭环快照
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class CareerLoopSnapshot {

    private Long id;

    private Long sessionId;

    private Integer planProgress;

    private Integer mockCount;

    private Integer latestMockScore;

    private Integer reviewCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime resumeUpdatedAt;

    private String riskFlagsJson;

    private String nextSuggestionJson;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}

