package com.xiaou.web.growthcoach.intent;

/**
 * 计划调整意图解析端口。
 */
public interface GrowthCoachIntentResolver {

    GrowthCoachIntent resolve(String message);
}
