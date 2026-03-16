package com.xiaou.techbriefing.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 科技热点来源
 */
@Data
@Accessors(chain = true)
public class TechBriefingSource {

    private Long id;

    private String sourceName;

    private String sourceType;

    private String fetchType;

    private String baseUrl;

    private String rssUrl;

    private String status;

    private Integer weight;

    private String cronExpr;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
