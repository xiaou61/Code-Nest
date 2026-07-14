package com.xiaou.community.dto;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 用户封禁请求
 * 
 * @author xiaou
 */
@Data
public class UserBanRequest {
    
    /**
     * 封禁原因
     */
    @NotBlank(message = "封禁原因不能为空")
    @Size(max = 200, message = "封禁原因长度不能超过200字符")
    private String reason;
    
    /**
     * 封禁时长（小时）
     */
    @NotNull(message = "封禁时长不能为空")
    @Min(value = 1, message = "封禁时长必须在1-8760小时之间")
    @Max(value = 8760, message = "封禁时长必须在1-8760小时之间")
    private Integer duration;
}
