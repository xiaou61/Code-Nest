package io.github.xiaou61;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import io.github.xiaou61.account.AccountAuthTestSupport;
import io.github.xiaou61.account.internal.credential.CaptchaImageRenderer;
import io.github.xiaou61.account.internal.mail.InMemoryMailPort;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 账号与认证的端到端集成测试（AC-001、AC-002、AC-003、AC-004、AC-005、AC-008）。
 *
 * <p>用完整上下文与真实安全过滤器链、真实 MySQL，但用 MockMvc 而不是真实端口：
 * 仍然经过过滤器、控制器、mapper 与数据库，省掉 HTTP 客户端的编解码噪音。
 *
 * <p><b>图形验证码怎么被验证</b>：答案只存在于图片里，测试无从读取——所以把出图器换成
 * mock，让随机码固定为 {@link #CAPTCHA_CODE}。"发信必须先过图形验证码"这条规则仍然走
 * 真实逻辑（一次性、过期、答案比对），只是随机源被钉住。
 *
 * <p><b>验证码怎么被读到</b>：邮件端口被换成内存信箱（来自账号模块的 test-jar），
 * 因此注册全链路的验证**不依赖真实 SMTP**。
 *
 * <p>需要真实 MySQL 与 {@code PAIDEIA_TEST_DB_PASSWORD}；未配置时整个类被跳过并计入 Skipped，
 * 不让"没跑"看起来像"通过"。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AccountAuthTestSupport.class)
@EnabledIfEnvironmentVariable(named = "PAIDEIA_TEST_DB_PASSWORD", matches = ".+")
class AccountAuthIntegrationTest {

    private static final String CAPTCHA_CODE = "ABCD";
    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    MockMvc mvc;

    @Autowired
    InMemoryMailPort mailbox;

    @MockitoBean
    CaptchaImageRenderer captchaImageRenderer;

    /** 每个用例用独立的来源地址，否则"同来源限流"会把互不相干的用例串起来。 */
    private String sourceAddress;

    @BeforeEach
    void setUp() {
        when(captchaImageRenderer.randomCode(anyInt())).thenReturn(CAPTCHA_CODE);
        when(captchaImageRenderer.renderPng(anyString())).thenReturn(new byte[] {1, 2, 3});
        mailbox.clear();
        sourceAddress = "10.0." + ThreadLocalRandom.current().nextInt(1, 250)
                + "." + ThreadLocalRandom.current().nextInt(1, 250);
    }

    // ---------------------------------------------------------------- AC-001

    @Test
    @DisplayName("AC-001 迁移已建立账号表，用户名唯一索引真的生效")
    void accountTableIsMigratedWithUniqueUsername() throws Exception {
        String username = unique("dup");
        String email = uniqueEmail("dup");
        registerNewAccount(username, email);

        String secondEmail = uniqueEmail("dup2");
        String code = issueEmailCode(secondEmail);
        String response = postJson("/api/v1/auth/register", """
                {"username":"%s","email":"%s","password":"%s","code":"%s"}
                """.formatted(username, secondEmail, PASSWORD, code));

        assertThat(codeOf(response)).as("唯一索引没有拦住重复用户名：%s", response).isEqualTo(40900);
        assertThat(messageOf(response)).contains("用户名");
    }

    // ---------------------------------------------------------------- AC-002

    @Test
    @DisplayName("AC-002 没有有效的图形验证码时不会发出邮件验证码")
    void emailCodeIsNotSentWithoutValidCaptcha() throws Exception {
        String email = uniqueEmail("nocaptcha");

        String response = postJson("/api/v1/auth/email-code", """
                {"email":"%s","captchaId":"not-a-real-captcha-id","captchaAnswer":"ABCD"}
                """.formatted(email));

        assertThat(codeOf(response)).isEqualTo(40000);
        assertThat(mailbox.latestCodeFor(email)).as("没过图形验证码就发了信").isEmpty();
    }

    @Test
    @DisplayName("AC-002 通过图形验证码后收到验证码，并据此完成注册")
    void registersAfterSolvingCaptchaAndEmailCode() throws Exception {
        String username = unique("reg");
        String email = uniqueEmail("reg");
        String code = issueEmailCode(email);

        String data = register(username, email, code);

        assertThat(stringAt(data, "$.data.user.username")).isEqualTo(username);
        assertThat(stringAt(data, "$.data.user.role")).isEqualTo("learner");
        assertThat(stringAt(data, "$.data.accessToken")).isNotBlank();
        assertThat(stringAt(data, "$.data.refreshToken")).isNotBlank();
        // 对外的用户视图不得带出口令摘要
        assertThat(hasPath(data, "$.data.user.passwordHash")).isFalse();
    }

    @Test
    @DisplayName("AC-002 验证码错误被拒；同一个验证码只能用一次")
    void rejectsWrongAndReusedEmailCode() throws Exception {
        String email = uniqueEmail("code-err");
        String code = issueEmailCode(email);

        String wrong = postJson("/api/v1/auth/register", """
                {"username":"%s","email":"%s","password":"%s","code":"000000"}
                """.formatted(unique("code-err-u"), email, PASSWORD));
        assertThat(messageOf(wrong)).contains("验证码不正确");

        // 打错不会作废验证码，所以这一次能成功
        String first = postJson("/api/v1/auth/register", """
                {"username":"%s","email":"%s","password":"%s","code":"%s"}
                """.formatted(unique("code-err-u2"), email, PASSWORD, code));
        assertThat(codeOf(first)).isZero();

        // 同一个验证码再来一次：必须失败（一次性）
        String second = postJson("/api/v1/auth/register", """
                {"username":"%s","email":"%s","password":"%s","code":"%s"}
                """.formatted(unique("code-err-u3"), email, PASSWORD, code));
        assertThat(messageOf(second)).contains("验证码不存在或已过期");
    }

    @Test
    @DisplayName("AC-002 验证码未通过时不会泄露邮箱是否已被注册")
    void doesNotLeakRegistrationStatusBeforeEmailIsProven() throws Exception {
        String email = uniqueEmail("occupied");
        registerNewAccount(unique("occupied-user"), email);

        // 同一个邮箱再来一次，但带一个不存在的验证码：必须先报验证码问题，而不是"已被注册"
        String response = postJson("/api/v1/auth/register", """
                {"username":"%s","email":"%s","password":"%s","code":"123456"}
                """.formatted(unique("occupied-user2"), email, PASSWORD));

        assertThat(codeOf(response)).isEqualTo(40000);
        assertThat(messageOf(response)).contains("验证码");
        assertThat(messageOf(response)).doesNotContain("已被注册");
    }

    // ---------------------------------------------------------------- AC-003

    @Test
    @DisplayName("AC-003 用户名与邮箱两种标识都能登录")
    void logsInWithEitherIdentifier() throws Exception {
        String username = unique("either");
        String email = uniqueEmail("either");
        registerNewAccount(username, email);

        assertThat(stringAt(login(username), "$.data.accessToken")).isNotBlank();
        assertThat(stringAt(login(email), "$.data.accessToken")).isNotBlank();
    }

    @Test
    @DisplayName("AC-003 密码错误与账号不存在返回完全相同的错误码与文案")
    void doesNotDistinguishUnknownAccountFromWrongPassword() throws Exception {
        String wrongPassword = postJson("/api/v1/auth/login", """
                {"identifier":"%s","password":"definitely-wrong"}
                """.formatted(unique("nosuch")));
        String unknownAccount = postJson("/api/v1/auth/login", """
                {"identifier":"%s","password":"definitely-wrong"}
                """.formatted(unique("not-registered")));

        assertThat(codeOf(wrongPassword)).isEqualTo(40100);
        assertThat(codeOf(unknownAccount)).isEqualTo(40100);
        assertThat(messageOf(wrongPassword)).isEqualTo(messageOf(unknownAccount));
    }

    // ---------------------------------------------------------------- AC-004 / AC-005

    @Test
    @DisplayName("AC-004 轮换后旧 refresh 失效；重用旧 refresh 会终止整条链")
    void detectsRefreshReuseAndKillsTheWholeFamily() throws Exception {
        String firstRefresh = stringAt(registerNewAccount(unique("rotate"), uniqueEmail("rotate")),
                "$.data.refreshToken");

        // 第一次轮换：拿到新的 refresh
        String rotated = postJson("/api/v1/auth/refresh", """
                {"refreshToken":"%s"}
                """.formatted(firstRefresh));
        assertThat(codeOf(rotated)).as("轮换失败：%s", rotated).isZero();
        String secondRefresh = stringAt(rotated, "$.data.refreshToken");
        assertThat(secondRefresh).isNotBlank().isNotEqualTo(firstRefresh);

        // 再用被用过的那个 refresh：必须失败（它已经不可用）
        String reuse = postJson("/api/v1/auth/refresh", """
                {"refreshToken":"%s"}
                """.formatted(firstRefresh));
        assertThat(codeOf(reuse)).isEqualTo(40100);

        // 关键：重用被检测到之后整条链都失效——连最新那个 refresh 也不能再用
        String afterReuse = postJson("/api/v1/auth/refresh", """
                {"refreshToken":"%s"}
                """.formatted(secondRefresh));
        assertThat(codeOf(afterReuse)).as("整条链未被终止：%s", afterReuse).isEqualTo(40100);
    }

    @Test
    @DisplayName("AC-005 登出后该 refresh 不可再用")
    void logoutRevokesTheRefresh() throws Exception {
        String refresh = stringAt(registerNewAccount(unique("logout"), uniqueEmail("logout")),
                "$.data.refreshToken");

        String loggedOut = postJson("/api/v1/auth/logout", """
                {"refreshToken":"%s"}
                """.formatted(refresh));
        assertThat(codeOf(loggedOut)).isZero();

        String afterLogout = postJson("/api/v1/auth/refresh", """
                {"refreshToken":"%s"}
                """.formatted(refresh));
        assertThat(codeOf(afterLogout)).isEqualTo(40100);
    }

    // ---------------------------------------------------------------- AC-008

    @Test
    @DisplayName("AC-008 同一邮箱 60 秒内第二次请求发信被限流")
    void rateLimitsEmailSendingPerAddress() throws Exception {
        String email = uniqueEmail("throttle");
        issueEmailCode(email);

        String second = postJson("/api/v1/auth/email-code", """
                {"email":"%s","captchaId":"%s","captchaAnswer":"%s"}
                """.formatted(email, freshCaptchaId(), CAPTCHA_CODE));

        assertThat(codeOf(second)).as("第二次发信未被限流：%s", second).isEqualTo(42900);
        assertThat(messageOf(second)).contains("秒后重试");
    }

    @Test
    @DisplayName("AC-008 验证码尝试次数超限后作废，必须重新获取")
    void rateLimitsVerificationCodeAttempts() throws Exception {
        String email = uniqueEmail("attempts");
        String code = issueEmailCode(email);

        for (int attempt = 0; attempt < 5; attempt += 1) {
            String wrong = postJson("/api/v1/auth/register", """
                    {"username":"%s","email":"%s","password":"%s","code":"000000"}
                    """.formatted(unique("attempts-u"), email, PASSWORD));
            assertThat(codeOf(wrong)).isEqualTo(40000);
        }

        // 第 6 次即使给对了码也应被拒：超限即作废
        String overLimit = postJson("/api/v1/auth/register", """
                {"username":"%s","email":"%s","password":"%s","code":"%s"}
                """.formatted(unique("attempts-u2"), email, PASSWORD, code));
        assertThat(messageOf(overLimit)).contains("次数过多");
    }

    @Test
    @DisplayName("AC-008 连续登录失败超限后被拒（此时连正确密码也进不去）")
    void rateLimitsFailedLogins() throws Exception {
        String username = unique("brute");
        registerNewAccount(username, uniqueEmail("brute"));

        for (int attempt = 0; attempt < 5; attempt += 1) {
            String failed = postJson("/api/v1/auth/login", """
                    {"identifier":"%s","password":"wrong-password"}
                    """.formatted(username));
            assertThat(codeOf(failed)).isEqualTo(40100);
        }

        String blocked = postJson("/api/v1/auth/login", """
                {"identifier":"%s","password":"%s"}
                """.formatted(username, PASSWORD));
        assertThat(codeOf(blocked)).as("失败次数超限后未被限流：%s", blocked).isEqualTo(42900);
    }

    // ---------------------------------------------------------------- AC-009

    @Test
    @DisplayName("AC-009 那个不校验凭据的裸签发端点已经不存在")
    void bareTokenEndpointIsGone() throws Exception {
        // 老端点可以为任意主体签发令牌、不校验任何凭据。有了真实登录后连开发期都不再需要它，
        // 所以是删除而不是"只在生产禁用"——一个不该存在的东西留着就是隐患。
        String body = postJson("/api/v1/auth/token", """
                {"subject":"anyone-at-all"}
                """);

        assertThat(codeOf(body)).as("该路径不该再返回成功：%s", body).isEqualTo(40400);
        assertThat(body).as("响应里不该出现任何令牌").doesNotContain("accessToken").doesNotContain("refreshToken");
    }

    // ---------------------------------------------------------------- 辅助

    private String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** 邮箱必须真的是邮箱：接口上有 {@code @Email} 校验，随便拼一个串会被 400 挡掉。 */
    private String uniqueEmail(String prefix) {
        return unique(prefix) + "@example.test";
    }

    private String freshCaptchaId() throws Exception {
        String body = postJson("/api/v1/auth/captcha", "{}");
        return stringAt(body, "$.data.captchaId");
    }

    /** 走完整前置：取图形验证码 → 发信 → 从内存信箱读回明文。 */
    private String issueEmailCode(String email) throws Exception {
        String response = postJson("/api/v1/auth/email-code", """
                {"email":"%s","captchaId":"%s","captchaAnswer":"%s"}
                """.formatted(email, freshCaptchaId(), CAPTCHA_CODE));
        assertThat(codeOf(response)).as("发信失败：%s", response).isZero();
        return mailbox.latestCodeFor(email).orElseThrow();
    }

    private String registerNewAccount(String username, String email) throws Exception {
        return register(username, email, issueEmailCode(email));
    }

    /** @return 完整响应体（JSON 文本），调用方按 {@code $.data.*} 取字段 */
    private String register(String username, String email, String code) throws Exception {
        String response = postJson("/api/v1/auth/register", """
                {"username":"%s","email":"%s","password":"%s","code":"%s"}
                """.formatted(username, email, PASSWORD, code));
        assertThat(codeOf(response)).as("注册失败：%s", response).isZero();
        return response;
    }

    /** @return 完整响应体（JSON 文本），调用方按 {@code $.data.*} 取字段 */
    private String login(String identifier) throws Exception {
        String response = postJson("/api/v1/auth/login", """
                {"identifier":"%s","password":"%s"}
                """.formatted(identifier, PASSWORD));
        assertThat(codeOf(response)).as("登录失败：%s", response).isZero();
        return response;
    }

    private String postJson(String path, String payload) throws Exception {
        MvcResult result = mvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .with(request -> {
                    request.setRemoteAddr(sourceAddress);
                    return request;
                })).andReturn();
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static int codeOf(String body) {
        return ((Number) JsonPath.read(body, "$.code")).intValue();
    }

    private static String messageOf(String body) {
        return JsonPath.read(body, "$.message").toString();
    }

    private static String stringAt(String body, String path) {
        try {
            Object value = JsonPath.read(body, path);
            return value instanceof String text ? text : String.valueOf(value);
        } catch (RuntimeException exception) {
            // 把原始响应体带进失败信息：否则"解析失败"无法区分是响应非法还是断言路径写错
            throw new AssertionError("从响应里取 " + path + " 失败，原始响应体长度=" + body.length()
                    + "，内容=" + body, exception);
        }
    }

    private static boolean hasPath(String body, String path) {
        try {
            JsonPath.read(body, path);
            return true;
        } catch (PathNotFoundException exception) {
            return false;
        }
    }
}
