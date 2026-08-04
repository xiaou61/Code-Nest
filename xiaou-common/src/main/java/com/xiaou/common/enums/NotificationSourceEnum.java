package com.xiaou.common.enums;

/**
 * 消息通知来源模块枚举
 */
public enum NotificationSourceEnum {
    
    /**
     * 系统模块
     */
    SYSTEM("system", "系统模块"),
    
    /**
     * 社区模块
     */
    COMMUNITY("community", "社区模块"),
    
    /**
     * 面试题模块
     */
    INTERVIEW("interview", "面试题模块"),
    
    /**
     * 用户模块
     */
    USER("user", "用户模块"),
    
    /**
     * 文件存储模块
     */
    FILESTORAGE("filestorage", "文件存储模块"),
    
    /**
     * 监控模块
     */
    MONITOR("monitor", "监控模块"),

    /**
     * AI 成长教练模块
     */
    GROWTH_COACH("growth_coach", "AI成长教练");
    
    private final String code;
    private final String description;
    
    NotificationSourceEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据代码获取枚举
     */
    public static NotificationSourceEnum fromCode(String code) {
        for (NotificationSourceEnum source : values()) {
            if (source.getCode().equals(code)) {
                return source;
            }
        }
        throw new IllegalArgumentException("未知的消息来源模块: " + code);
    }
    
    /**
     * 检查代码是否有效
     */
    public static boolean isValidCode(String code) {
        try {
            fromCode(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
