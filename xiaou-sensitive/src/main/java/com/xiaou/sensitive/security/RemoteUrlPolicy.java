package com.xiaou.sensitive.security;

import com.xiaou.sensitive.config.SensitiveSourceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Validates operator-configured remote source URLs before the server opens a
 * connection. DNS results are checked as well as the textual host to reduce
 * SSRF exposure through private and metadata networks.
 */
@Component
@RequiredArgsConstructor
public class RemoteUrlPolicy {

    private final SensitiveSourceProperties properties;

    public URI validate(String rawUrl) {
        return validate(rawUrl, false);
    }

    public URI validate(String rawUrl, boolean githubSource) {
        if (!StringUtils.hasText(rawUrl)) {
            throw new IllegalArgumentException("remote URL is empty");
        }

        final URI uri;
        try {
            uri = new URI(rawUrl.trim());
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("remote URL is invalid", exception);
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"https".equals(scheme) && !(properties.isAllowHttp() && "http".equals(scheme))) {
            throw new IllegalArgumentException("remote URL scheme is not allowed");
        }
        if (githubSource && !"https".equals(scheme)) {
            throw new IllegalArgumentException("GitHub source must use HTTPS");
        }
        if (!StringUtils.hasText(uri.getHost()) || uri.getUserInfo() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("remote URL host is invalid");
        }
        if (uri.getPort() != -1 && uri.getPort() != 80 && uri.getPort() != 443) {
            throw new IllegalArgumentException("remote URL port is not allowed");
        }

        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (githubSource && !("github.com".equals(host) || "raw.githubusercontent.com".equals(host))) {
            throw new IllegalArgumentException("GitHub source host is not allowed");
        }
        if (isBlockedHost(host)) {
            throw new IllegalArgumentException("remote URL resolves to a private network");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                throw new IllegalArgumentException("remote URL host has no address");
            }
            for (InetAddress address : addresses) {
                if (isBlockedAddress(address)) {
                    throw new IllegalArgumentException("remote URL resolves to a private network");
                }
            }
        } catch (java.net.UnknownHostException exception) {
            throw new IllegalArgumentException("remote URL host cannot be resolved", exception);
        }
        return uri;
    }

    private boolean isBlockedHost(String host) {
        return "localhost".equals(host)
                || host.endsWith(".localhost")
                || host.endsWith(".local")
                || host.endsWith(".internal")
                || host.equals("metadata.google.internal");
    }

    private boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;
            int third = bytes[2] & 0xff;
            return (first == 0)
                    || (first == 10)
                    || (first == 100 && second >= 64 && second <= 127)
                    || (first == 127)
                    || (first == 169 && second == 254)
                    || (first == 172 && second >= 16 && second <= 31)
                    || (first == 192 && second == 168)
                    || (first == 192 && second == 0 && third == 0)
                    || (first == 198 && (second == 18 || second == 19));
        }
        return (bytes[0] & 0xff) == 0xfc || (bytes[0] & 0xff) == 0xfd;
    }
}
