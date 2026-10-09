package io.github.xiaou61.persistence;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * 集成测试用的数据库装配。
 *
 * <p>连接信息只从环境变量读取，仓库里不放任何凭据：
 * {@code PAIDEIA_TEST_DB_URL}、{@code PAIDEIA_TEST_DB_USER}、{@code PAIDEIA_TEST_DB_PASSWORD}。
 * 默认 URL 指向本机 {@code 127.0.0.1:3307} 的 SSH 隧道（服务器上的 {@code paideia_test}）。
 *
 * <p>没有配置口令时调用方应跳过测试并如实报告为未运行，而不是让整个构建红掉。
 */
final class TestDatabase {

    static final String URL_ENV = "PAIDEIA_TEST_DB_URL";
    static final String USER_ENV = "PAIDEIA_TEST_DB_USER";
    static final String PASSWORD_ENV = "PAIDEIA_TEST_DB_PASSWORD";

    private static final String DEFAULT_URL =
            "jdbc:mysql://127.0.0.1:3307/paideia_test"
                    + "?useSSL=false&allowPublicKeyRetrieval=true&connectionTimeZone=UTC&characterEncoding=utf8";
    private static final String DEFAULT_USER = "paideia";

    private TestDatabase() {
    }

    static String password() {
        return System.getenv(PASSWORD_ENV);
    }

    static boolean isConfigured() {
        String password = password();
        return password != null && !password.isBlank();
    }

    static DataSource dataSource() {
        return new DriverManagerDataSource(url(), user(), password());
    }

    static void migrate(DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/testdata")
                .load()
                .migrate();
    }

    private static String url() {
        String value = System.getenv(URL_ENV);
        return value == null || value.isBlank() ? DEFAULT_URL : value;
    }

    private static String user() {
        String value = System.getenv(USER_ENV);
        return value == null || value.isBlank() ? DEFAULT_USER : value;
    }
}
