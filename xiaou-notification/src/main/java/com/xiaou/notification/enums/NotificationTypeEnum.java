package com.xiaou.notification.enums;

/**
 * 消息通知类型枚举
 */
public enum NotificationTypeEnum {

    /**
     * 个人消息
     */
    PERSONAL("PERSONAL", "个人消息"),

    /**
     * 系统公告
     */
    ANNOUNCEMENT("ANNOUNCEMENT", "系统公告"),

    /**
     * 社区互动
     */
    COMMUNITY_INTERACTION("COMMUNITY_INTERACTION", "社区互动"),

    /**
     * 面试题提醒
     */
    INTERVIEW_REMINDER("INTERVIEW_REMINDER", "面试题提醒"),

    /**
     * 系统通知
     */
    SYSTEM("SYSTEM", "系统通知"),

    /**
     * 审核结果
     */
    AUDIT_RESULT("AUDIT_RESULT", "审核结果"),

    /**
     * 活动通知
     */
    ACTIVITY_NOTIFICATION("ACTIVITY_NOTIFICATION", "活动通知"),

    /**
     * 模板消息
     */
    TEMPLATE("TEMPLATE", "模板消息");

    private final String code;
    private final String description;

    NotificationTypeEnum(String code, String description) {
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
    public static NotificationTypeEnum fromCode(String code) {
        for (NotificationTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的消息类型: " + code);
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
