package com.xiaou.codepen.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 更新状态请求
 * 
 * @author xiaou
 */
@Data
public class UpdateStatusRequest {
    
    /**
     * 作品ID
     */
    @NotNull(message = "作品ID不能为空")
    @Positive(message = "作品ID必须为正数")
    private Long id;
    
    /**
     * 状态：0-草稿 1-已发布 2-已下架 3-已删除
     */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态参数错误")
    @Max(value = 3, message = "状态参数错误")
    private Integer status;
}

