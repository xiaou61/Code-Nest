package com.xiaou.codepen.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Fork作品请求
 * 
 * @author xiaou
 */
@Data
public class ForkRequest {
    
    /**
     * 作品ID
     */
    @NotNull(message = "作品ID不能为空")
    @Positive(message = "作品ID必须为正数")
    private Long penId;
}

