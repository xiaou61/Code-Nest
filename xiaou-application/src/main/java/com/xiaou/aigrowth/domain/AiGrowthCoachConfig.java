package com.xiaou.aigrowth.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * AI成长教练配置
 */
@Data
@Accessors(chain = true)
public class AiGrowthCoachConfig {

    private Long id;

    private String configKey;

    private String configValue;

    private String remark;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
