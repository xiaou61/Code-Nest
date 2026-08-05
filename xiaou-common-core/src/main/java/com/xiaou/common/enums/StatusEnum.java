package com.xiaou.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 状态枚举类
 *
 * @author xiaou
 */
@Getter
@AllArgsConstructor
public enum StatusEnum {

    /**
     * 通用状态
     */
    NORMAL(0, "正常"),
    DISABLED(1, "禁用"),
    DELETED(2, "已删除"),

    /**
     * 用户状态
     */
    USER_ACTIVE(0, "激活"),
    USER_INACTIVE(1, "未激活"),
    USER_LOCKED(2, "锁定"),
    USER_EXPIRED(3, "过期"),

    /**
     * 订单状态
     */
    ORDER_PENDING(0, "待处理"),
    ORDER_CONFIRMED(1, "已确认"),
    ORDER_SHIPPED(2, "已发货"),
    ORDER_DELIVERED(3, "已送达"),
    ORDER_CANCELLED(4, "已取消"),
    ORDER_REFUNDED(5, "已退款"),

    /**
     * 审核状态
     */
    AUDIT_PENDING(0, "待审核"),
    AUDIT_APPROVED(1, "审核通过"),
    AUDIT_REJECTED(2, "审核拒绝"),

    /**
     * 发布状态
     */
    PUBLISH_DRAFT(0, "草稿"),
    PUBLISH_PUBLISHED(1, "已发布"),
    PUBLISH_OFFLINE(2, "已下线"),

    /**
     * 性别
     */
    GENDER_UNKNOWN(0, "未知"),
    GENDER_MALE(1, "男"),
    GENDER_FEMALE(2, "女"),

    /**
     * 是否标识
     */
    NO(0, "否"),
    YES(1, "是");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 根据状态码获取枚举
     */
    public static StatusEnum getByCode(Integer code) {
        for (StatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 根据状态码获取描述
     */
    public static String getDescriptionByCode(Integer code) {
        StatusEnum status = getByCode(code);
        return status != null ? status.getDescription() : "未知状态";
    }

    /**
     * 判断状态码是否有效
     */
    public static boolean isValid(Integer code) {
        return getByCode(code) != null;
    }
}
