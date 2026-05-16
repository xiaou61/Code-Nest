package com.xiaou.common.satoken;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;

import java.util.List;

/**
 * 用户账号体系工具类
 * 使用独立的 StpLogic，loginType = "user"
 *
 * @author xiaou
 */
public class StpUserUtil {
    
    /**
     * 使用独立的 StpLogic，loginType = "user"
     */
    public static final StpLogic stpLogic = new StpLogic("user");
    
    // ========== 登录相关 ==========
    
    /**
     * 登录，并指定登录ID
     */
    public static void login(Object id) {
        stpLogic.login(id);
    }
    
    /**
     * 登录，并指定登录ID和设备类型
     */
    public static void login(Object id, String device) {
        stpLogic.login(id, device);
    }
    
    /**
     * 登录，并指定登录配置
     */
    public static void login(Object id, SaLoginParameter loginParameter) {
        stpLogic.login(id, loginParameter);
    }
    
    /**
     * 注销当前账号
     */
    public static void logout() {
        stpLogic.logout();
    }
    
    /**
     * 注销指定账号
     */
    public static void logout(Object loginId) {
        stpLogic.logout(loginId);
    }
    
    /**
     * 注销指定账号的指定设备
     */
    public static void logout(Object loginId, String device) {
        stpLogic.logout(loginId, device);
    }
    
    // ========== 会话查询 ==========
    
    /**
     * 判断当前是否登录
     */
    public static boolean isLogin() {
        return stpLogic.isLogin();
    }
    
    /**
     * 检查当前是否登录，如未登录则抛出异常
     */
    public static void checkLogin() {
        stpLogic.checkLogin();
    }
    
    /**
     * 获取当前登录ID
     */
    public static Object getLoginId() {
        return stpLogic.getLoginId();
    }
    
    /**
     * 获取当前登录ID（转Long）
     */
    public static long getLoginIdAsLong() {
        return stpLogic.getLoginIdAsLong();
    }
    
    /**
     * 获取当前登录ID（转String）
     */
    public static String getLoginIdAsString() {
        return stpLogic.getLoginIdAsString();
    }
    
    /**
     * 获取当前Token值
     */
    public static String getTokenValue() {
        return stpLogic.getTokenValue();
    }
    
    /**
     * 获取当前Token信息
     */
    public static Object getTokenInfo() {
        return stpLogic.getTokenInfo();
    }
    
    // ========== Token 操作 ==========
    
    /**
     * 获取当前Token的剩余有效期（秒）
     */
    public static long getTokenTimeout() {
        return stpLogic.getTokenTimeout();
    }
    
    /**
     * 根据登录ID获取Token值
     */
    public static String getTokenValueByLoginId(Object loginId) {
        return stpLogic.getTokenValueByLoginId(loginId);
    }
    
    /**
     * 根据登录ID获取Token值
     */
    public static String getTokenValueByLoginId(Object loginId, String device) {
        return stpLogic.getTokenValueByLoginId(loginId, device);
    }
    
    /**
     * 获取指定账号的所有Token值
     */
    public static List<String> getTokenValueListByLoginId(Object loginId) {
        return stpLogic.getTokenValueListByLoginId(loginId);
    }
    
    /**
     * 踢人下线（指定账号的所有登录）
     */
    public static void kickout(Object loginId) {
        stpLogic.kickout(loginId);
    }
    
    /**
     * 踢人下线（指定账号的指定设备）
     */
    public static void kickout(Object loginId, String device) {
        stpLogic.kickout(loginId, device);
    }
    
    /**
     * 踢人下线（指定Token）
     */
    public static void kickoutByTokenValue(String tokenValue) {
        stpLogic.kickoutByTokenValue(tokenValue);
    }
    
    // ========== Session 操作 ==========
    
    /**
     * 获取当前账号的Session
     */
    public static SaSession getSession() {
        return stpLogic.getSession();
    }
    
    /**
     * 获取当前账号的Session，如未登录则返回null
     */
    public static SaSession getSessionByLoginId(Object loginId) {
        return stpLogic.getSessionByLoginId(loginId);
    }
    
    /**
     * 获取当前Token的Session
     */
    public static SaSession getTokenSession() {
        return stpLogic.getTokenSession();
    }
    
    /**
     * 在当前Session中设置数据
     */
    public static void set(String key, Object value) {
        stpLogic.getSession().set(key, value);
    }
    
    /**
     * 从当前Session中获取数据
     */
    public static Object get(String key) {
        return stpLogic.getSession().get(key);
    }
    
    /**
     * 从当前Session中获取数据（指定类型）
     */
    public static <T> T get(String key, Class<T> cs) {
        return stpLogic.getSession().getModel(key, cs);
    }
    
    /**
     * 判断当前Session中是否存在指定key
     */
    public static boolean has(String key) {
        return stpLogic.getSession().has(key);
    }
    
    /**
     * 从当前Session中删除数据
     */
    public static void delete(String key) {
        stpLogic.getSession().delete(key);
    }
    
    /**
     * 清空当前Session的所有数据
     */
    public static void clearSession() {
        stpLogic.getSession().clear();
    }
    
    // ========== 权限验证 ==========
    
    /**
     * 判断当前账号是否拥有指定角色
     */
    public static boolean hasRole(String role) {
        return stpLogic.hasRole(role);
    }
    
    /**
     * 判断当前账号是否拥有指定角色（指定账号ID）
     */
    public static boolean hasRole(Object loginId, String role) {
        return stpLogic.hasRole(loginId, role);
    }
    
    /**
     * 校验当前账号是否拥有指定角色，如不拥有则抛出异常
     */
    public static void checkRole(String role) {
        stpLogic.checkRole(role);
    }
    
    /**
     * 判断当前账号是否拥有指定权限
     */
    public static boolean hasPermission(String permission) {
        return stpLogic.hasPermission(permission);
    }
    
    /**
     * 判断当前账号是否拥有指定权限（指定账号ID）
     */
    public static boolean hasPermission(Object loginId, String permission) {
        return stpLogic.hasPermission(loginId, permission);
    }
    
    /**
     * 校验当前账号是否拥有指定权限，如不拥有则抛出异常
     */
    public static void checkPermission(String permission) {
        stpLogic.checkPermission(permission);
    }
    
    // ========== 账号封禁 ==========
    
    /**
     * 封禁指定账号
     * @param loginId 账号ID
     * @param disableTime 封禁时长（秒），-1表示永久封禁
     */
    public static void disable(Object loginId, long disableTime) {
        stpLogic.disable(loginId, disableTime);
    }
    
    /**
     * 判断指定账号是否已被封禁
     */
    public static boolean isDisable(Object loginId) {
        return stpLogic.isDisable(loginId);
    }
    
    /**
     * 校验指定账号是否已被封禁，如已封禁则抛出异常
     */
    public static void checkDisable(Object loginId) {
        stpLogic.checkDisable(loginId);
    }
    
    /**
     * 解除指定账号的封禁
     */
    public static void untieDisable(Object loginId) {
        stpLogic.untieDisable(loginId);
    }
    
    /**
     * 获取指定账号的封禁剩余时间（秒）
     */
    public static long getDisableTime(Object loginId) {
        return stpLogic.getDisableTime(loginId);
    }
}
