package io.github.xiaou61.web;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * paideia-web 的切片测试启动配置。
 *
 * <p>真正的 {@code @SpringBootApplication} 在 paideia-app 模块，而 paideia-web 不能依赖它
 * （会形成模块循环）。这里在同一个包下提供一份最小配置，让 {@code @WebMvcTest} 的组件过滤
 * 在本模块范围内生效——它会扫到 {@link ProbeController}、{@link TraceIdFilter}、
 * {@link ApiResponseBodyAdvice}、{@link GlobalExceptionHandler} 与 {@link WebCorsConfiguration}，
 * 与实际运行时的装配一致。
 *
 * <p>必须用 {@code @SpringBootApplication} 而不是 {@code @SpringBootConfiguration}：后者不带
 * 组件扫描，会导致什么控制器都注册不上、所有请求都返回 404。
 */
@SpringBootApplication
class WebSliceTestApplication {
}
