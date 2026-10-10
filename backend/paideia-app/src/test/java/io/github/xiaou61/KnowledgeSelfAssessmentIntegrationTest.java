package io.github.xiaou61;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.xiaou61.security.AuthPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 学习者自评的集成测试，重点是**隔离**。
 *
 * <p>为什么单独一个类、为什么两个方向都查：自评是本项目第一条用户私有业务数据，隔离写错
 * 会串号。而"能标记、能看到自己的"这类断言**在串号时照样通过**——所以真正要钉住的是
 * "B 看不到 A 的标记"，且必须**同时覆盖列表与详情两条读路径**（它们是两条不同的查询）。
 *
 * <p>两个用户用 101／102 两个令牌主体即可：本表刻意不对 accounts 建外键（知识模块不拥有账号），
 * 归属只由令牌决定。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "PAIDEIA_TEST_DB_PASSWORD", matches = ".+")
class KnowledgeSelfAssessmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthPort authPort;

    private String token(String subject) {
        return "Bearer " + authPort.issue(new AuthPort.Subject(subject, AuthPort.ROLE_LEARNER)).value();
    }

    private void clear(String subject, long entryId) throws Exception {
        mockMvc.perform(delete("/api/v1/knowledge/entries/" + entryId + "/self-assessment")
                .header("Authorization", token(subject)));
    }

    private void mark(String subject, long entryId, String level) throws Exception {
        mockMvc.perform(put("/api/v1/knowledge/entries/" + entryId + "/self-assessment")
                        .header("Authorization", token(subject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"level\":\"" + level + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCannotMark() throws Exception {
        mockMvc.perform(put("/api/v1/knowledge/entries/1/self-assessment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"level\":\"understood\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void markingIsVisibleOnlyToTheMarkerOnBothReadPaths() throws Exception {
        // 从干净状态开始：这个类会真的写数据，重跑时不能让上一次的残留影响断言
        clear("101", 1L);
        clear("102", 1L);

        mark("101", 1L, "understood");

        // 详情路径：A 看得到自己的标记
        mockMvc.perform(get("/api/v1/knowledge/entries/1").header("Authorization", token("101")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selfAssessment").value("understood"));

        // 详情路径：B 看到的是 null——这是最容易漏的一条（列表过滤了、详情忘了）
        mockMvc.perform(get("/api/v1/knowledge/entries/1").header("Authorization", token("102")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selfAssessment").doesNotExist());

        // 列表路径：A 有、B 没有
        mockMvc.perform(get("/api/v1/knowledge/self-assessments").header("Authorization", token("101")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.entryId==1)]").exists());
        mockMvc.perform(get("/api/v1/knowledge/self-assessments").header("Authorization", token("102")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        clear("101", 1L);
    }

    @Test
    void markingAgainUpdatesInsteadOfInsertingASecondRow() throws Exception {
        clear("101", 2L);
        mark("101", 2L, "understood");
        mark("101", 2L, "unsure");

        mockMvc.perform(get("/api/v1/knowledge/entries/2").header("Authorization", token("101")))
                .andExpect(jsonPath("$.data.selfAssessment").value("unsure"));
        // 唯一键 (user_id, entry_id) 让它是一次更新；列表里也只该出现一条。
        // 用 Jayway 读出命中项再数，而不是把 length() 写进 JsonPath 表达式——后者在这里
        // 返回的是整个数组的长度，语义对不上（实测会给出假绿/假红）。
        String body = mockMvc.perform(get("/api/v1/knowledge/self-assessments")
                        .header("Authorization", token("101")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        java.util.List<Integer> matched = com.jayway.jsonpath.JsonPath.read(body, "$.data[?(@.entryId==2)].entryId");
        org.assertj.core.api.Assertions.assertThat(matched).hasSize(1);

        clear("101", 2L);
    }

    @Test
    void clearingTwiceIsIdempotent() throws Exception {
        clear("101", 1L);
        mark("101", 1L, "unsure");

        // 第一次真的删掉了，第二次本来就没有——两次都该是成功（用户点两下不该看到报错）
        clear("101", 1L);
        mockMvc.perform(delete("/api/v1/knowledge/entries/1/self-assessment")
                        .header("Authorization", token("101")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/knowledge/entries/1").header("Authorization", token("101")))
                .andExpect(jsonPath("$.data.selfAssessment").doesNotExist());
    }

    @Test
    void listingCanBeFilteredByLevel() throws Exception {
        clear("101", 1L);
        clear("101", 2L);
        mark("101", 1L, "understood");
        mark("101", 2L, "unsure");

        mockMvc.perform(get("/api/v1/knowledge/self-assessments?level=unsure")
                        .header("Authorization", token("101")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.entryId==2)]").exists())
                .andExpect(jsonPath("$.data[?(@.entryId==1)]").doesNotExist());

        clear("101", 1L);
        clear("101", 2L);
    }

    @Test
    void draftCannotBeMarked() throws Exception {
        // 条目 4 是种子里的草稿。允许标记草稿等于告诉他"这里有未发布的内容"
        mockMvc.perform(put("/api/v1/knowledge/entries/4/self-assessment")
                        .header("Authorization", token("101"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"level\":\"understood\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidLevelIsRejected() throws Exception {
        mockMvc.perform(put("/api/v1/knowledge/entries/1/self-assessment")
                        .header("Authorization", token("101"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"level\":\"maybe\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }
}
