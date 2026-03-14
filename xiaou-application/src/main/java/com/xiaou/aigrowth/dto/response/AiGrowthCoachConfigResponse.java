package com.xiaou.aigrowth.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI成长教练配置响应
 */
@Data
public class AiGrowthCoachConfigResponse {

    private Long id;

    private String configKey;

    private String configValue;

    private String remark;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
