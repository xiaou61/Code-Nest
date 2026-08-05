package com.xiaou.notification.enums;

/**
 * 消息通知状态枚举
 */
public enum NotificationStatusEnum {

    /**
     * 未读
     */
    UNREAD("UNREAD", "未读"),

    /**
     * 已读
     */
    READ("READ", "已读"),

    /**
     * 已删除
     */
    DELETED("DELETED", "已删除");

    private final String code;
    private final String description;

    NotificationStatusEnum(String code, String description) {
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
    public static NotificationStatusEnum fromCode(String code) {
        for (NotificationStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的消息状态: " + code);
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
