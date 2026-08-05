package com.xiaou.notification.enums;

/**
 * 消息通知优先级枚举
 */
public enum NotificationPriorityEnum {

    /**
     * 低优先级
     */
    LOW("LOW", "低优先级", 1),

    /**
     * 中等优先级
     */
    MEDIUM("MEDIUM", "中等优先级", 2),

    /**
     * 高优先级
     */
    HIGH("HIGH", "高优先级", 3);

    private final String code;
    private final String description;
    private final int level;

    NotificationPriorityEnum(String code, String description, int level) {
        this.code = code;
        this.description = description;
        this.level = level;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public int getLevel() {
        return level;
    }

    /**
     * 根据代码获取枚举
     */
    public static NotificationPriorityEnum fromCode(String code) {
        for (NotificationPriorityEnum priority : values()) {
            if (priority.getCode().equals(code)) {
                return priority;
            }
        }
        throw new IllegalArgumentException("未知的消息优先级: " + code);
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
