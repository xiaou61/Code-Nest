package io.github.xiaou61.persistence;

import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 全局约定的唯一入口。新增约定都加在这里，避免配置散落。
 */
@Configuration(proxyBeanMethods = false)
public class MyBatisCustomization {

    @Bean
    ConfigurationCustomizer paideiaMyBatisCustomizer() {
        return configuration -> {
            // 列名 snake_case 与字段名 camelCase 的映射：数据库侧不迁就 Java 命名
            configuration.setMapUnderscoreToCamelCase(true);
        };
    }
}
