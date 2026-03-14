package com.xiaou.mockinterview.dto.request;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 闭环会话目标配置更新请求
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class CareerLoopProfileUpdateRequest {

    /**
     * 目标岗位（传空字符串表示清空）
     */
    private String targetRole;

    /**
     * 每周投入时长（小时）
     */
    private Integer weeklyHours;
}
