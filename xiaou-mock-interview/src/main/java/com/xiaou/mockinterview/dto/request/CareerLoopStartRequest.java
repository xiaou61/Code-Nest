package com.xiaou.mockinterview.dto.request;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 求职闭环启动请求
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class CareerLoopStartRequest {

    /**
     * 目标岗位
     */
    private String targetRole;

    /**
     * 目标公司类型
     */
    private String targetCompanyType;
}

