package com.xiaou.user.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简单用户信息DTO - 供其他模块使用
 * 避免循环依赖，只包含基本用户信息
 * 
 * @author xiaou
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleUserInfo {
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 真实姓名
     */
    private String realName;
    
    /**
     * 头像
     */
    private String avatar;

    /**
     * 用户简介
     */
    private String bio;
    
    /**
     * 获取显示名称（优先级：真实姓名 > 昵称 > 用户名）
     */
    public String getDisplayName() {
        if (realName != null && !realName.trim().isEmpty()) {
            return realName;
        }
        if (nickname != null && !nickname.trim().isEmpty()) {
            return nickname;
        }
        if (username != null && !username.trim().isEmpty()) {
            return username;
        }
        return "用户" + id;
    }
}
