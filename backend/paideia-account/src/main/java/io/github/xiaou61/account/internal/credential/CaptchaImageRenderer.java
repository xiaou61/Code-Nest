package io.github.xiaou61.account.internal.credential;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

/**
 * 图形验证码的出图与随机码生成。零外部依赖（JDK 自带的 Java2D）。
 *
 * <p><b>为什么要显式设 headless</b>：服务器上没有显示设备，Java2D 必须走无头模式。
 * Spring Boot 默认会设 {@code java.awt.headless=true}，但这里再兜一层是没有代价的保险——
 * 无头环境下初始化 AWT 失败会抛 {@code HeadlessException}，那种错误在启动时才暴露最好。
 *
 * <p><b>字体只用 JDK 内置的逻辑字体</b>：容器化的 JRE 常常没有 fontconfig 与字体文件，
 * 指定具体字体名会出图失败。逻辑字体 SansSerif 由 JDK 自带的字体资源解析。
 *
 * <p><b>字符集去掉了易混字符</b>（0/O、1/I/l、5/S、8/B）：否则用户会因为"看错字母"
 * 反复失败，而那是体验问题不是安全问题。
 *
 * <p><b>它挡不住打码平台</b>：图形验证码的作用是提高自动化成本、并保护发信接口，
 * 不是消灭机器人。真正的防线是限流。
 */
@Component
public final class CaptchaImageRenderer {

    /** 去掉易混字符后的字符集。 */
    private static final char[] ALPHABET = "ACDEFGHJKMNPQRTUVWXY234679".toCharArray();

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int NOISE_LINES = 6;
    private static final int NOISE_DOTS = 60;

    private final SecureRandom random = new SecureRandom();

    static {
        // 在类加载时就定下来：无头环境下的 AWT 初始化只做一次
        System.setProperty("java.awt.headless", "true");
    }

    public String randomCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int index = 0; index < length; index += 1) {
            code.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return code.toString();
    }

    /** 出 PNG 字节。答案不画进图片之外的任何地方，也不写日志。 */
    public byte[] renderPng(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线：不做太重，否则人眼也认不出
            graphics.setStroke(new BasicStroke(1.2f));
            for (int index = 0; index < NOISE_LINES; index += 1) {
                graphics.setColor(new Color(random.nextInt(160), random.nextInt(160), random.nextInt(160), 120));
                graphics.drawLine(
                        random.nextInt(WIDTH), random.nextInt(HEIGHT),
                        random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }

            // 噪点
            for (int index = 0; index < NOISE_DOTS; index += 1) {
                graphics.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
                int x = random.nextInt(WIDTH);
                int y = random.nextInt(HEIGHT);
                graphics.fillRect(x, y, 1, 1);
            }

            // 每个字符独立旋转与偏移，避免整齐划一被模板匹配
            int step = (WIDTH - 16) / Math.max(1, code.length());
            for (int index = 0; index < code.length(); index += 1) {
                graphics.setColor(new Color(20 + random.nextInt(60), 20 + random.nextInt(60), 20 + random.nextInt(60)));
                graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24 + random.nextInt(4)));
                double angle = Math.toRadians(random.nextInt(41) - 20);
                int x = 10 + index * step;
                int y = 28 + random.nextInt(4);
                graphics.rotate(angle, x, y);
                graphics.drawString(String.valueOf(code.charAt(index)), x, y);
                graphics.rotate(-angle, x, y);
            }
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException exception) {
            // 写到内存流上的 PNG 编码不会因为磁盘问题失败；失败说明运行环境异常
            throw new IllegalStateException("生成图形验证码失败", exception);
        }
    }
}
