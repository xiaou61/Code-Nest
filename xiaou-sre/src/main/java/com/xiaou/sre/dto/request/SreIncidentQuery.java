package com.xiaou.sre.dto.request;

import com.xiaou.common.core.domain.PageRequest;
import lombok.Data;

/**
 * SRE 事故查询条件。
 *
 * @author xiaou
 */
@Data
public class SreIncidentQuery implements PageRequest {

    private String state;
    private String severity;
    private String service;
    private Integer pageNum = 1;
    private Integer pageSize = 20;

    @Override
    public SreIncidentQuery setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
        return this;
    }

    @Override
    public SreIncidentQuery setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        return this;
    }
}
