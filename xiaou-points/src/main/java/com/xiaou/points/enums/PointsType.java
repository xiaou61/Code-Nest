package com.xiaou.points.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 积分类型枚举
 * 
 * @author xiaou
 */
@Getter
@AllArgsConstructor
public enum PointsType {
    
    ADMIN_GRANT(1, "后台发放", "管理员手动发放的积分奖励"),
    CHECK_IN(2, "打卡积分", "用户每日打卡获得的积分"),
    LOTTERY_COST(3, "抽奖消耗", "参与积分抽奖消耗的积分"),
    LOTTERY_REWARD(4, "抽奖奖励", "积分抽奖获得的奖励"),
    OJ_AC(5, "OJ通过", "首次通过OJ题目获得的积分");
    
    private final int code;
    private final String desc;
    private final String detail;
    
    /**
     * 根据code获取枚举
     */
    public static PointsType getByCode(int code) {
        for (PointsType type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * 根据code获取描述
     */
    public static String getDescByCode(int code) {
        PointsType type = getByCode(code);
        return type != null ? type.getDesc() : "未知类型";
    }
}
