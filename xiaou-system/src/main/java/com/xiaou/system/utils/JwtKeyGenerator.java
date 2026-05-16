package com.xiaou.system.utils;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * JWT密钥生成器
 * 用于生成符合HS512要求的安全密钥
 *
 * @author xiaou
 */
public final class JwtKeyGenerator {

    private static final int HS512_MIN_BYTES = 64;

    private JwtKeyGenerator() {
    }

    public static void main(String[] args) {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());

        writer.println("=== JWT密钥生成器 ===");
        writer.println("生成符合 HS512 算法要求的安全密钥");
        writer.println();
        writer.println("生成的密钥（Base64编码）：");
        writer.println(base64Key);
        writer.println();
        writer.printf("密钥长度：%d 字节（%d 位）%n", key.getEncoded().length, key.getEncoded().length * 8);
        writer.printf("是否符合 HS512 要求：%s%n", key.getEncoded().length >= HS512_MIN_BYTES ? "是" : "否");
        writer.println();
        writer.println("推荐以环境变量方式配置，避免将真实密钥提交到仓库：");
        writer.println("PowerShell:");
        writer.printf("  $env:XIAOU_JWT_SECRET=\"%s\"%n", base64Key);
        writer.println("Linux/macOS:");
        writer.printf("  export XIAOU_JWT_SECRET=\"%s\"%n", base64Key);
    }
}
