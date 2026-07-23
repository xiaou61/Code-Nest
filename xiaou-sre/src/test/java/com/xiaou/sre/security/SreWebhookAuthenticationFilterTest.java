package com.xiaou.sre.security;

import com.xiaou.sre.config.SreWebhookProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class SreWebhookAuthenticationFilterTest {

    private static final String PATH = "/api/internal/sre/alertmanager/v1/alerts";

    @Test
    void disabledWebhookDoesNotRevealEndpoint() throws Exception {
        SreWebhookProperties properties = new SreWebhookProperties();
        SreWebhookAuthenticationFilter filter = new SreWebhookAuthenticationFilter(properties);
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(404);
        verifyNoInteractions(chain);
    }

    @Test
    void validBearerTokenPassesRequestToController() throws Exception {
        SreWebhookProperties properties = enabledProperties();
        SreWebhookAuthenticationFilter filter = new SreWebhookAuthenticationFilter(properties);
        MockHttpServletRequest request = request();
        request.addHeader("Authorization", "Bearer webhook-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidBearerTokenIsRejected() throws Exception {
        SreWebhookAuthenticationFilter filter = new SreWebhookAuthenticationFilter(enabledProperties());
        MockHttpServletRequest request = request();
        request.addHeader("Authorization", "Bearer wrong-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(chain);
    }

    @Test
    void oversizedRequestBodyIsRejectedBeforeController() throws Exception {
        SreWebhookProperties properties = enabledProperties();
        properties.setMaxBodyBytes(10);
        SreWebhookAuthenticationFilter filter = new SreWebhookAuthenticationFilter(properties);
        MockHttpServletRequest request = request();
        request.addHeader("Authorization", "Bearer webhook-secret");
        request.setContent(new byte[11]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        verifyNoInteractions(chain);
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/api");
        request.setRequestURI(PATH);
        return request;
    }

    private SreWebhookProperties enabledProperties() {
        SreWebhookProperties properties = new SreWebhookProperties();
        properties.setEnabled(true);
        properties.setToken("webhook-secret");
        return properties;
    }
}
