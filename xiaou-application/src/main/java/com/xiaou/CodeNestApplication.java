package com.xiaou;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableAsync
@EnableScheduling
@MapperScan({"com.xiaou.*.mapper", "com.xiaou.web.growthcoach.mapper"})
public class CodeNestApplication {
    public static void main(String[] args) {
        SpringApplication.run(CodeNestApplication.class, args);
    }
}
