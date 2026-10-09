package io.github.xiaou61.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    @DisplayName("无上游标识时生成新的追踪标识，并写入请求属性与响应头")
    void generatesTraceIdWhenAbsent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/probe");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String traceId = (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
        assertThat(traceId).isNotBlank();
        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo(traceId);
    }

    @Test
    @DisplayName("沿用合法的上游追踪标识")
    void reusesValidIncomingTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/probe");
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "upstream-trace_01");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE)).isEqualTo("upstream-trace_01");
    }

    @Test
    @DisplayName("含非法字符或超长的上游标识被丢弃：它会被回写到响应头")
    void rejectsUnsafeIncomingTraceId() throws Exception {
        for (String unsafe : new String[] {"bad value", "bad\nvalue", "bad;value", "x".repeat(65)}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/probe");
            request.addHeader(TraceIdFilter.TRACE_ID_HEADER, unsafe);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, new MockFilterChain());

            assertThat(request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE))
                    .as("上游标识 %s 不应被沿用", unsafe)
                    .isNotEqualTo(unsafe);
        }
    }

    @Test
    @DisplayName("请求结束后清理 MDC，避免线程复用时串号")
    void clearsMdcAfterRequest() throws Exception {
        filter.doFilter(new MockHttpServletRequest("GET", "/probe"), new MockHttpServletResponse(),
                new MockFilterChain());

        assertThat(MDC.get("traceId")).isNull();
    }
}
