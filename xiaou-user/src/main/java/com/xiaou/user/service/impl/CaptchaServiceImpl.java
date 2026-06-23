package com.xiaou.user.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import com.xiaou.common.utils.RedisUtil;
import com.xiaou.user.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务实现类
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private final RedisUtil redisUtil;

    /**
     * 验证码在Redis中的key前缀
     */
    private static final String CAPTCHA_KEY_PREFIX = "user:captcha:";

    /**
     * 验证码有效期（分钟）
     */
    private static final int CAPTCHA_EXPIRE_MINUTES = 5;

    /**
     * 验证码图片宽度
     */
    private static final int CAPTCHA_WIDTH = 120;

    /**
     * 验证码图片高度
     */
    private static final int CAPTCHA_HEIGHT = 40;

    /**
     * 验证码字符长度
     */
    private static final int CAPTCHA_CODE_COUNT = 4;

    /**
     * 干扰线数量
     */
    private static final int CAPTCHA_LINE_COUNT = 5;

    @Override
    public Map<String, Object> generateCaptcha() {
        try {
            // 生成线段干扰的验证码
            LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(
                    CAPTCHA_WIDTH, 
                    CAPTCHA_HEIGHT, 
                    CAPTCHA_CODE_COUNT, 
                    CAPTCHA_LINE_COUNT
            );

            // 生成唯一的验证码key
            String captchaKey = IdUtil.simpleUUID();
            
            // 获取验证码文本
            String captchaCode = lineCaptcha.getCode();
            
            // 将验证码存储到Redis中，保持原样（验证时忽略大小写）
            String redisKey = CAPTCHA_KEY_PREFIX + captchaKey;
            redisUtil.set(redisKey, captchaCode, CAPTCHA_EXPIRE_MINUTES * 60); // 转换为秒

            // 获取验证码图片的Base64编码
            String captchaImage = lineCaptcha.getImageBase64();

            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("captchaKey", captchaKey);
            result.put("captchaImage", "data:image/png;base64," + captchaImage);
            result.put("expiresIn", CAPTCHA_EXPIRE_MINUTES * 60); // 转换为秒

            log.info("生成验证码成功，key: {}", captchaKey);
            return result;

        } catch (Exception e) {
            log.error("生成验证码失败", e);
            throw new RuntimeException("生成验证码失败");
        }
    }

    @Override
    public boolean verifyCaptcha(String captchaKey, String captcha) {
        if (captchaKey == null || captcha == null) {
            log.warn("验证码参数为空");
            return false;
        }

        try {
            String redisKey = CAPTCHA_KEY_PREFIX + captchaKey;
            String storedCaptcha = (String) redisUtil.get(redisKey);
            
            if (storedCaptcha == null) {
                log.warn("验证码已过期或不存在，key: {}", captchaKey);
                return false;
            }

            // 忽略大小写比较
            boolean isValid = storedCaptcha.equalsIgnoreCase(captcha.trim());
            
            if (isValid) {
                log.info("验证码验证成功，key: {}", captchaKey);
                // 验证成功后删除验证码，防止重复使用
                deleteCaptcha(captchaKey);
            } else {
                log.warn("验证码验证失败，key: {}", captchaKey);
            }

            return isValid;

        } catch (Exception e) {
            log.error("验证码验证异常，key: {}", captchaKey, e);
            return false;
        }
    }

    @Override
    public void deleteCaptcha(String captchaKey) {
        if (captchaKey != null) {
            String redisKey = CAPTCHA_KEY_PREFIX + captchaKey;
            redisUtil.del(redisKey);
            log.debug("删除验证码成功，key: {}", captchaKey);
        }
    }
}
