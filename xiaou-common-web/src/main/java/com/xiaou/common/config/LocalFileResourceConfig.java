package com.xiaou.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地文件资源映射配置
 *
 * @author xiaou
 */
@Slf4j
@Configuration
public class LocalFileResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadRoot = Paths.get(System.getProperty("user.dir"), "uploads")
            .toAbsolutePath()
            .normalize();

        registry.addResourceHandler("/files/**")
            .addResourceLocations(uploadRoot.toUri().toString());

        log.info("✅ 本地文件资源映射已注册: /files/** -> {}", uploadRoot);
    }
}
