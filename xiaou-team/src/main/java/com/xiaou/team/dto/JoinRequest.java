package com.xiaou.team.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 加入小组请求DTO
 * 
 * @author xiaou
 */
@Data
public class JoinRequest {
    
    /**
     * 小组ID
     */
    @Positive(message = "小组ID必须大于0")
    private Long teamId;
    
    /**
     * 申请理由
     */
    @Size(max = 200, message = "申请理由不能超过200个字符")
    private String applyReason;
    
    /**
     * 邀请码（通过邀请码加入时使用）
     */
    @Size(max = 32, message = "邀请码长度不合法")
    private String inviteCode;
}
