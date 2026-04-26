package com.xiaou.codepen.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 推荐请求
 * 
 * @author xiaou
 */
@Data
public class RecommendRequest {
    
    /**
     * 作品ID
     */
    @NotNull(message = "作品ID不能为空")
    @Positive(message = "作品ID必须为正数")
    private Long id;
    
    /**
     * 推荐过期时间（格式：yyyy-MM-dd HH:mm:ss）
     */
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$", message = "推荐过期时间格式必须为yyyy-MM-dd HH:mm:ss")
    private String expireTime;
}

