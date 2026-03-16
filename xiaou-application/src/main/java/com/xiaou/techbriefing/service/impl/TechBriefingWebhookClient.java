package com.xiaou.techbriefing.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.techbriefing.domain.TechBriefingSubscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * 飞书 / 钉钉 Webhook 客户端
 */
@Slf4j
@Component
public class TechBriefingWebhookClient {

    public void sendTestMessage(TechBriefingSubscription subscription, String content) {
        if (subscription == null) {
            throw new BusinessException("订阅配置不存在");
        }
        String channelType = StrUtil.blankToDefault(subscription.getChannelType(), "");
        if ("FEISHU".equalsIgnoreCase(channelType)) {
            sendFeishu(subscription, content);
            return;
        }
        if ("DINGTALK".equalsIgnoreCase(channelType)) {
            sendDingTalk(subscription, content);
            return;
        }
        throw new BusinessException("暂不支持的渠道类型：" + channelType);
    }

    private void sendFeishu(TechBriefingSubscription subscription, String content) {
        long timestamp = System.currentTimeMillis() / 1000;
        cn.hutool.json.JSONObject payload = JSONUtil.createObj()
                .set("msg_type", "text")
                .set("content", JSONUtil.createObj().set("text", content));
        if (StrUtil.isNotBlank(subscription.getWebhookSecret())) {
            payload.set("timestamp", String.valueOf(timestamp));
            payload.set("sign", buildBase64Hmac(timestamp + "\n" + subscription.getWebhookSecret(), subscription.getWebhookSecret()));
        }
        execute(subscription.getWebhookUrl(), payload);
    }

    private void sendDingTalk(TechBriefingSubscription subscription, String content) {
        String url = subscription.getWebhookUrl();
        if (StrUtil.isNotBlank(subscription.getWebhookSecret())) {
            long timestamp = System.currentTimeMillis();
            String sign = buildBase64Hmac(timestamp + "\n" + subscription.getWebhookSecret(), subscription.getWebhookSecret());
            url = url + (url.contains("?") ? "&" : "?")
                    + "timestamp=" + timestamp
                    + "&sign=" + URLEncodeUtil.encode(sign, StandardCharsets.UTF_8);
        }
        cn.hutool.json.JSONObject payload = JSONUtil.createObj()
                .set("msgtype", "text")
                .set("text", JSONUtil.createObj().set("content", content));
        execute(url, payload);
    }

    private void execute(String url, cn.hutool.json.JSONObject payload) {
        try (HttpResponse response = HttpRequest.post(url)
                .timeout(5000)
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(payload.toString())
                .execute()) {
            if (!response.isOk()) {
                throw new BusinessException("Webhook 发送失败，状态码：" + response.getStatus());
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Webhook 发送异常", ex);
            throw new BusinessException("Webhook 发送失败");
        }
    }

    private String buildBase64Hmac(String text, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.encode(mac.doFinal(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BusinessException("生成签名失败");
        }
    }
}
