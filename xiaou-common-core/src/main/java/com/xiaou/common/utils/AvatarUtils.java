package com.xiaou.common.utils;

import cn.hutool.core.util.StrUtil;

/**
 * 头像工具类
 *
 * @author xiaou
 */
public class AvatarUtils {

    /**
     * 默认头像URL
     */
    private static final String DEFAULT_AVATAR_URL = "https://cn.cravatar.com/avatar/e1e7ba949ade0936e071132d2edd3b3c.png";

    /**
     * 获取默认头像URL
     *
     * @param userId 用户ID（此参数保留为了兼容性，实际不使用）
     * @return 默认头像URL
     */
    public static String getDefaultAvatar(Long userId) {
        return DEFAULT_AVATAR_URL;
    }

    /**
     * 获取用户头像URL，如果为空则返回默认头像
     *
     * @param avatar 用户头像URL
     * @param userId 用户ID
     * @return 头像URL
     */
    public static String getUserAvatar(String avatar, Long userId) {
        if (StrUtil.isNotBlank(avatar)) {
            return avatar;
        }
        return getDefaultAvatar(userId);
    }

    /**
     * 检查是否为默认头像
     *
     * @param avatar 头像URL
     * @return 是否为默认头像
     */
    public static boolean isDefaultAvatar(String avatar) {
        return StrUtil.isBlank(avatar) || DEFAULT_AVATAR_URL.equals(avatar);
    }

    /**
     * 获取默认头像URL
     *
     * @return 默认头像URL
     */
    public static String getDefaultAvatarUrl() {
        return DEFAULT_AVATAR_URL;
    }
}
