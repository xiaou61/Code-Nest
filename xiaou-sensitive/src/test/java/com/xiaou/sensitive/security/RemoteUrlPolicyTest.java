package com.xiaou.sensitive.security;

import com.xiaou.sensitive.config.SensitiveSourceProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemoteUrlPolicyTest {

    private final RemoteUrlPolicy policy = new RemoteUrlPolicy(new SensitiveSourceProperties());

    @Test
    void rejectsLoopbackAndPrivateAddresses() {
        assertThatThrownBy(() -> policy.validate("http://127.0.0.1/internal"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.validate("http://192.168.1.10/internal"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.validate("http://169.254.169.254/latest/meta-data"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnsupportedSchemesAndGithubHostConfusion() {
        assertThatThrownBy(() -> policy.validate("file:///etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.validate("https://evil.example/github.com/file", true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
