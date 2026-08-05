package com.xiaou.common.satoken;

import lombok.extern.slf4j.Slf4j;

/**
 * Sa-Token 用户上下文工具类
 * 用于获取当前登录的管理员或用户信息
 * 
 * @author xiaou
 */
@Slf4j
public class SaTokenUserUtil {
    
    /**
     * Session 中存储用户信息的 key
     */
    private static final String USER_INFO_KEY = "userInfo";
    
    /**
     * Session 中存储用户名的 key
     */
    private static final String USERNAME_KEY = "username";

    /**
     * 用户资料解析器，由用户模块在运行时注册，避免 common 反向依赖业务模块。
     */
    private static volatile UserProfileResolver userProfileResolver;
    
    /**
     * 获取当前登录的管理员信息（泛型方法）
     * 
     * @param clazz 返回类型
     * @return 管理员信息，未登录返回null
     */
    public static <T> T getCurrentAdmin(Class<T> clazz) {
        try {
            if (!StpAdminUtil.isLogin()) {
                log.debug("管理员未登录");
                return null;
            }
            return StpAdminUtil.get(USER_INFO_KEY, clazz);
        } catch (Exception e) {
            log.error("获取当前管理员信息失败", e);
            return null;
        }
    }
    
    /**
     * 获取当前登录的管理员信息（Object 类型）
     * 
     * @return 管理员信息，未登录返回null
     */
    public static Object getCurrentAdmin() {
        try {
            if (!StpAdminUtil.isLogin()) {
                log.debug("管理员未登录");
                return null;
            }
            return StpAdminUtil.get(USER_INFO_KEY);
        } catch (Exception e) {
            log.error("获取当前管理员信息失败", e);
            return null;
        }
    }
    
    /**
     * 获取当前登录的管理员ID
     * 
     * @return 管理员ID，未登录返回null
     */
    public static Long getCurrentAdminId() {
        try {
            if (!StpAdminUtil.isLogin()) {
                return null;
            }
            return StpAdminUtil.getLoginIdAsLong();
        } catch (Exception e) {
            log.error("获取当前管理员ID失败", e);
            return null;
        }
    }
    
    /**
     * 获取当前登录的用户信息（泛型方法）
     * 
     * @param clazz 返回类型
     * @return 用户信息，未登录返回null
     */
    public static <T> T getCurrentUser(Class<T> clazz) {
        try {
            if (!StpUserUtil.isLogin()) {
                log.debug("用户未登录");
                return null;
            }
            return StpUserUtil.get(USER_INFO_KEY, clazz);
        } catch (Exception e) {
            log.error("获取当前用户信息失败", e);
            return null;
        }
    }
    
    /**
     * 获取当前登录的用户信息（Object 类型）
     * 
     * @return 用户信息，未登录返回null
     */
    public static Object getCurrentUser() {
        try {
            if (!StpUserUtil.isLogin()) {
                log.debug("用户未登录");
                return null;
            }
            return StpUserUtil.get(USER_INFO_KEY);
        } catch (Exception e) {
            log.error("获取当前用户信息失败", e);
            return null;
        }
    }
    
    /**
     * 获取当前登录的用户ID
     * 
     * @return 用户ID，未登录返回null
     */
    public static Long getCurrentUserId() {
        try {
            if (!StpUserUtil.isLogin()) {
                return null;
            }
            return StpUserUtil.getLoginIdAsLong();
        } catch (Exception e) {
            log.error("获取当前用户ID失败", e);
            return null;
        }
    }
    
    /**
     * 判断当前是否是管理员
     * 
     * @return true-是管理员，false-不是管理员
     */
    public static boolean isAdmin() {
        return StpAdminUtil.isLogin();
    }
    
    /**
     * 判断当前是否是普通用户
     * 
     * @return true-是普通用户，false-不是普通用户
     */
    public static boolean isUser() {
        return StpUserUtil.isLogin();
    }
    
    /**
     * 获取当前登录管理员的用户名
     * 
     * @return 用户名，未登录或获取失败返回 null
     */
    public static String getCurrentAdminUsername() {
        try {
            if (!StpAdminUtil.isLogin()) {
                return null;
            }
            Object username = StpAdminUtil.get(USERNAME_KEY);
            return username != null ? username.toString() : null;
        } catch (Exception e) {
            log.error("获取当前管理员用户名失败", e);
            return null;
        }
    }
    
    /**
     * 获取当前登录用户的用户名
     * 
     * @return 用户名，未登录或获取失败返回 null
     */
    public static String getCurrentUserUsername() {
        try {
            if (!StpUserUtil.isLogin()) {
                return null;
            }
            Object username = StpUserUtil.get(USERNAME_KEY);
            return username != null ? username.toString() : null;
        } catch (Exception e) {
            log.error("获取当前用户用户名失败", e);
            return null;
        }
    }
    
    /**
     * 获取当前登录管理员的用户名（带默认值）
     * 
     * @param defaultValue 默认值
     * @return 用户名，未登录或获取失败返回默认值
     */
    public static String getCurrentAdminUsername(String defaultValue) {
        String username = getCurrentAdminUsername();
        return username != null ? username : defaultValue;
    }
    
    /**
     * 获取当前登录用户的用户名（带默认值）
     * 
     * @param defaultValue 默认值
     * @return 用户名，未登录或获取失败返回默认值
     */
    public static String getCurrentUserUsername(String defaultValue) {
        String username = getCurrentUserUsername();
        return username != null ? username : defaultValue;
    }

    /**
     * 注册用户资料解析器。
     *
     * @param resolver 用户资料解析器
     */
    public static void registerUserProfileResolver(UserProfileResolver resolver) {
        if (resolver == null) {
            log.warn("注册用户资料解析器失败：resolver 不能为空");
            return;
        }
        userProfileResolver = resolver;
        log.info("用户资料解析器已注册：{}", resolver.getClass().getName());
    }

    /**
     * 注销指定用户资料解析器。
     *
     * @param resolver 待注销的解析器
     */
    public static void clearUserProfileResolver(UserProfileResolver resolver) {
        if (resolver != null && userProfileResolver == resolver) {
            userProfileResolver = null;
            log.info("用户资料解析器已注销：{}", resolver.getClass().getName());
        }
    }

    /**
     * 清空用户资料解析器，主要用于测试或应用关闭。
     */
    public static void clearUserProfileResolver() {
        userProfileResolver = null;
    }
    
    /**
     * 根据用户ID获取用户名（带默认值）
     * 注意：此方法仅返回默认值，具体用户名需要从用户服务或数据库获取
     * 
     * @param userId 用户ID
     * @param defaultValue 默认值
     * @return 用户名，获取失败返回默认值
     */
    public static String getUsernameById(Long userId, String defaultValue) {
        try {
            UserProfile userProfile = resolveUserProfile(userId);
            return firstText(userProfile != null ? userProfile.getUsername() : null, defaultValue);
        } catch (Exception e) {
            log.error("获取用户名失败，用户ID: {}", userId, e);
            return defaultValue;
        }
    }
    
    /**
     * 根据用户ID获取用户头像（带默认值）
     * 注意：此方法仅返回默认值，具体头像需要从用户服务或数据库获取
     * 
     * @param userId 用户ID
     * @param defaultValue 默认值
     * @return 用户头像URL，获取失败返回默认值
     */
    public static String getUserAvatarById(Long userId, String defaultValue) {
        try {
            UserProfile userProfile = resolveUserProfile(userId);
            return firstText(userProfile != null ? userProfile.getAvatar() : null, defaultValue);
        } catch (Exception e) {
            log.error("获取用户头像失败，用户ID: {}", userId, e);
            return defaultValue;
        }
    }
    
    /**
     * 根据用户ID获取用户简介（带默认值）
     * 注意：此方法仅返回默认值，具体简介需要从用户服务或数据库获取
     * 
     * @param userId 用户ID
     * @param defaultValue 默认值
     * @return 用户简介，获取失败返回默认值
     */
    public static String getUserBioById(Long userId, String defaultValue) {
        try {
            UserProfile userProfile = resolveUserProfile(userId);
            return firstText(userProfile != null ? userProfile.getBio() : null, defaultValue);
        } catch (Exception e) {
            log.error("获取用户简介失败，用户ID: {}", userId, e);
            return defaultValue;
        }
    }

    private static UserProfile resolveUserProfile(Long userId) {
        if (userId == null) {
            return null;
        }
        UserProfileResolver resolver = userProfileResolver;
        if (resolver == null) {
            return null;
        }
        return resolver.resolve(userId);
    }

    private static String firstText(String value, String defaultValue) {
        return hasText(value) ? value : defaultValue;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 跨模块用户资料解析器。
     */
    @FunctionalInterface
    public interface UserProfileResolver {
        /**
         * 根据用户ID解析用户资料。
         *
         * @param userId 用户ID
         * @return 用户资料，未找到返回 null
         */
        UserProfile resolve(Long userId);
    }

    /**
     * 公共模块可安全消费的用户资料。
     */
    public static class UserProfile {

        /**
         * 展示名称，通常为真实姓名、昵称或用户名。
         */
        private final String username;

        /**
         * 用户头像URL。
         */
        private final String avatar;

        /**
         * 用户简介。
         */
        private final String bio;

        public UserProfile(String username, String avatar, String bio) {
            this.username = username;
            this.avatar = avatar;
            this.bio = bio;
        }

        public String getUsername() {
            return username;
        }

        public String getAvatar() {
            return avatar;
        }

        public String getBio() {
            return bio;
        }
    }
}
