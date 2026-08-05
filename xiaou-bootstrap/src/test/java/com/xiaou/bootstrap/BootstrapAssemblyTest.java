package com.xiaou.bootstrap;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.assertj.core.api.Assertions.assertThat;

class BootstrapAssemblyTest {

    @Test
    void exposesOneExplicitCompositionRoot() {
        SpringBootApplication application = CodeNestApplication.class
                .getAnnotation(SpringBootApplication.class);
        MapperScan mapperScan = CodeNestApplication.class.getAnnotation(MapperScan.class);

        assertThat(application).isNotNull();
        assertThat(application.scanBasePackages()).containsExactly("com.xiaou");
        assertThat(application.exclude()).contains(UserDetailsServiceAutoConfiguration.class);
        assertThat(CodeNestApplication.class).hasAnnotations(EnableAsync.class, EnableScheduling.class);
        assertThat(mapperScan.value()).containsExactly(
                "com.xiaou.*.mapper",
                "com.xiaou.web.growthcoach.mapper"
        );
    }
}
