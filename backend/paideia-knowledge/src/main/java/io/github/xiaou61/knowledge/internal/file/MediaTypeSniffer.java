package io.github.xiaou61.knowledge.internal.file;

/**
 * 按魔数判断附件真实类型。
 *
 * <p><b>为什么不信任客户端声明的 Content-Type，也不用扩展名</b>：两者都是客户端说了算的，
 * 把 {@code .svg} 改名成 {@code .png} 再声明成 {@code image/png} 是最基本的绕过手法。
 * 魔数是文件自己带的内容，改不掉（除非真的换一种文件）。
 *
 * <p>自己写而不用 {@code URLConnection.guessContentTypeFromStream}：后者支持的类型集合
 * 由 JDK 版本决定、且对 PDF 之类不稳，而我们要的是一份**可预测、可测试**的白名单。
 *
 * <p><b>白名单里没有 SVG 是刻意的</b>：SVG 是 XML，能内嵌脚本，一旦被当图片直接内联返回
 * 就是存储型 XSS。它认不出魔数，因此会自然落到"未知类型 → 拒绝"这一支。
 */
final class MediaTypeSniffer {

    static final String PNG = "image/png";
    static final String JPEG = "image/jpeg";
    static final String GIF = "image/gif";
    static final String WEBP = "image/webp";
    static final String PDF = "application/pdf";

    private MediaTypeSniffer() {
    }

    /** 认不出或不在白名单里时返回 null。 */
    static String sniff(byte[] content) {
        if (content == null || content.length < 12) {
            return null;
        }
        if (startsWith(content, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
            return PNG;
        }
        if (startsWith(content, 0xFF, 0xD8, 0xFF)) {
            return JPEG;
        }
        if (startsWith(content, 'G', 'I', 'F', '8')) {
            return GIF;
        }
        if (startsWith(content, 'R', 'I', 'F', 'F') && matches(content, 8, 'W', 'E', 'B', 'P')) {
            return WEBP;
        }
        if (startsWith(content, '%', 'P', 'D', 'F')) {
            return PDF;
        }
        return null;
    }

    /** 该类型允许的扩展名（小写，不含点）。 */
    static java.util.Set<String> allowedExtensions(String contentType) {
        return switch (contentType) {
            case PNG -> java.util.Set.of("png");
            case JPEG -> java.util.Set.of("jpg", "jpeg");
            case GIF -> java.util.Set.of("gif");
            case WEBP -> java.util.Set.of("webp");
            case PDF -> java.util.Set.of("pdf");
            default -> java.util.Set.of();
        };
    }

    private static boolean startsWith(byte[] content, int... expected) {
        return matches(content, 0, expected);
    }

    private static boolean matches(byte[] content, int offset, int... expected) {
        if (content.length < offset + expected.length) {
            return false;
        }
        for (int index = 0; index < expected.length; index++) {
            if ((content[offset + index] & 0xFF) != (expected[index] & 0xFF)) {
                return false;
            }
        }
        return true;
    }
}
