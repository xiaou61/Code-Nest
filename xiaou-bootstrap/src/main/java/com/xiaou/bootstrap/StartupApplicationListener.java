package com.xiaou.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 应用启动监听器
 * 在应用启动完成后显示重要信息
 *
 * @author xiaou
 */
@Slf4j
@Component
public class StartupApplicationListener implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String baseUrl = "http://localhost:" + serverPort + contextPath;

        // 显示启动成功信息
        log.info("");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🎉 Code-Nest 启动成功！");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("");

        // 运行环境（分行显示）
        log.info("🌐 运行环境:");
        log.info("环境: {}", activeProfile);
        log.info("端口: {}", serverPort);
        log.info("路径: {}", contextPath);
        log.info("");

        // 访问地址（分行显示）
        log.info("🌍 访问地址:");
        log.info("首页: {}", baseUrl);
        log.info("API文档: {}/swagger-ui.html", baseUrl);
        log.info("");

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("✨ Happy Coding! ✨");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("");
    }
}
