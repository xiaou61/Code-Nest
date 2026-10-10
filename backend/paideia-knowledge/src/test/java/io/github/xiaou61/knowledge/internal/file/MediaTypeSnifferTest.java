package io.github.xiaou61.knowledge.internal.file;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 魔数嗅探的白名单测试。
 *
 * <p>两条最要紧的：**SVG 必须落到"不识别"**（它是 XML，能内嵌脚本，一旦被当图片内联返回
 * 就是存储型 XSS），以及**内容与扩展名不符必须能被发现**（只查扩展名等于没查）。
 */
class MediaTypeSnifferTest {

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int index = 0; index < values.length; index++) {
            result[index] = (byte) values[index];
        }
        return result;
    }

    private static byte[] pdf() {
        // 魔数必须在偏移 0：真实 PDF 的文件头也在最前面。刻意不宽容"前面有垃圾字节"的写法——
        // 那种宽容换不来兼容性，只换来"什么都能通过"的判定
        return "%PDF-1.7\n%%EOF\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }

    @Test
    void recognisesPngJpegGifPdf() {
        assertThat(MediaTypeSniffer.sniff(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0)))
                .isEqualTo(MediaTypeSniffer.PNG);
        assertThat(MediaTypeSniffer.sniff(bytes(0xFF, 0xD8, 0xFF, 0xE0, 0, 0, 0, 0, 0, 0, 0, 0)))
                .isEqualTo(MediaTypeSniffer.JPEG);
        assertThat(MediaTypeSniffer.sniff("GIF89a............".getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
                .isEqualTo(MediaTypeSniffer.GIF);
        assertThat(MediaTypeSniffer.sniff(pdf())).isEqualTo(MediaTypeSniffer.PDF);
    }

    @Test
    void recognisesWebpOnlyWhenBothMarkersArePresent() {
        byte[] webp = bytes('R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 0, 0);
        assertThat(MediaTypeSniffer.sniff(webp)).isEqualTo(MediaTypeSniffer.WEBP);

        // 只有 RIFF 头（那是 wav/avi 之类）不能认成 webp
        byte[] riffOnly = bytes('R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E', 0, 0);
        assertThat(MediaTypeSniffer.sniff(riffOnly)).isNull();
    }

    @Test
    void rejectsSvgByDesign() {
        // SVG 是 XML，没有可用的魔数，因此自然落到"不识别"；这是安全上的刻意取舍，不是遗漏
        assertThat(MediaTypeSniffer.sniff("<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8))).isNull();
        assertThat(MediaTypeSniffer.sniff("<?xml version=\"1.0\"?><svg/>"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8))).isNull();
    }

    @Test
    void rejectsShortOrUnknownContent() {
        assertThat(MediaTypeSniffer.sniff(null)).isNull();
        assertThat(MediaTypeSniffer.sniff(new byte[0])).isNull();
        assertThat(MediaTypeSniffer.sniff(bytes('P', 'N', 'G'))).isNull();
        assertThat(MediaTypeSniffer.sniff("hello world!".getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
                .isNull();
    }

    @Test
    void allowedExtensionsMatchTheSniffedType() {
        assertThat(MediaTypeSniffer.allowedExtensions(MediaTypeSniffer.JPEG)).containsExactlyInAnyOrder("jpg", "jpeg");
        assertThat(MediaTypeSniffer.allowedExtensions(MediaTypeSniffer.PNG)).containsExactly("png");
        // 嗅探出 png 时不允许 .svg 后缀——这正是"内容与扩展名不符"要拦的情形
        assertThat(MediaTypeSniffer.allowedExtensions(MediaTypeSniffer.PNG)).doesNotContain("svg");
        assertThat(MediaTypeSniffer.allowedExtensions("image/svg+xml")).isEmpty();
    }
}
