package com.xiaou.codepen.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 检查Fork价格请求
 * 
 * @author xiaou
 */
@Data
public class CheckForkPriceRequest {
    
    /**
     * 作品ID
     */
    @NotNull(message = "作品ID不能为空")
    @Positive(message = "作品ID必须为正数")
    private Long penId;
}

