package com.xiaou.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.xiaou.common.core.domain.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能体审计查询请求
 *
 * @author xiaou
 */
@Data
@Schema(description = "智能体审计查询请求")
public class AgentAuditQueryRequest implements PageRequest {

    private String intent;

    private String actionId;

    private String riskCategory;

    private String status;

    private String operatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    @Override
    public AgentAuditQueryRequest setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
        return this;
    }

    @Override
    public AgentAuditQueryRequest setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        return this;
    }
}
