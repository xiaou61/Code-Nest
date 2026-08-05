package com.xiaou.common.utils;

import cn.hutool.crypto.digest.BCrypt;
import lombok.extern.slf4j.Slf4j;

/**
 * 密码工具类
 * 使用hutool的BCrypt进行密码加密和验证
 *
 * @author xiaou
 */
@Slf4j
public class PasswordUtil {

    /**
     * 加密密码
     *
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public static String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        String hashedPassword = BCrypt.hashpw(rawPassword);
        log.debug("密码加密成功，原始密码长度: {}", rawPassword.length());
        return hashedPassword;
    }

    /**
     * 验证密码
     *
     * @param rawPassword     原始密码
     * @param encodedPassword 加密后的密码
     * @return 验证结果
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            log.warn("原始密码为空，验证失败");
            return false;
        }

        if (encodedPassword == null || encodedPassword.isEmpty()) {
            log.warn("加密密码为空，验证失败");
            return false;
        }

        boolean matches = BCrypt.checkpw(rawPassword, encodedPassword);
        log.debug("密码验证结果: {}", matches);
        return matches;
    }

    /**
     * 生成指定强度的密码哈希
     *
     * @param rawPassword 原始密码
     * @param rounds      加密轮数 (4-31之间，推荐10-12)
     * @return 加密后的密码
     */
    public static String encode(String rawPassword, int rounds) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        if (rounds < 4 || rounds > 31) {
            throw new IllegalArgumentException("加密轮数必须在4-31之间");
        }

        String salt = BCrypt.gensalt(rounds);
        String hashedPassword = BCrypt.hashpw(rawPassword, salt);
        log.debug("密码加密成功，轮数: {}, 原始密码长度: {}", rounds, rawPassword.length());
        return hashedPassword;
    }

    /**
     * 获取默认加密轮数
     *
     * @return 默认轮数 (10)
     */
    public static int getDefaultRounds() {
        return 10;
    }
}
