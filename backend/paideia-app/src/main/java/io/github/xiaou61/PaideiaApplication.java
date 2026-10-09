package io.github.xiaou61;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 主类位于基础包根：Spring Modulith 以它的直接子包为模块边界，因此业务与技术模块都应位于
 * {@code io.github.xiaou61} 之下。
 */
@SpringBootApplication
public class PaideiaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaideiaApplication.class, args);
    }
}
