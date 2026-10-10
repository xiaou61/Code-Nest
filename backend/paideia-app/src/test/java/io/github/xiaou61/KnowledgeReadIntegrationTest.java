package io.github.xiaou61;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.xiaou61.security.AuthPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 知识库只读端点的集成测试（真实 MySQL）。
 *
 * <p><b>数据来自 e2e 种子</b>：本类在自己的 `properties` 里把 `db/devdata` 加进 Flyway
 * locations，与端到端测试用的是同一份数据、同一种加载方式——种子本来就是为"可复现地覆盖
 * 每条查询路径"而写的（两棵子树、三条已发布、一条草稿、两条关系）。这样不必为了测试再造
 * 一份夹具，避免两处数据各说各话。
 *
 * <p>测试库不需要真实账号：读取只要求"已认证"，因此令牌用 {@code AuthPort} 直接签发。
 * 这也顺带证明了一件事——**读路径不依赖账号数据**（本模块没有 account 依赖）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "PAIDEIA_TEST_DB_PASSWORD", matches = ".+")
class KnowledgeReadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthPort authPort;

    private String learner() {
        return "Bearer " + authPort.issue(new AuthPort.Subject("1", AuthPort.ROLE_LEARNER)).value();
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge/entries"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void categoryTreeIsNested() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge/categories").header("Authorization", learner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                // 根：后端基础（下有 Java 并发）与架构
                .andExpect(jsonPath("$.data[?(@.slug=='backend-basics')]").exists())
                .andExpect(jsonPath("$.data[?(@.slug=='architecture')]").exists())
                .andExpect(jsonPath("$.data[?(@.slug=='backend-basics')].children[0].slug")
                        .value("java-concurrency"));
    }

    @Test
    void listReturnsPublishedEntriesOnly() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge/entries?size=50").header("Authorization", learner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id==1)]").exists())
                .andExpect(jsonPath("$.data.items[?(@.id==2)]").exists())
                .andExpect(jsonPath("$.data.items[?(@.id==3)]").exists())
                // 种子里的 4 是草稿，学习者侧任何列表都不该出现
                .andExpect(jsonPath("$.data.items[?(@.id==4)]").doesNotExist());
    }

    @Test
    void browsingAParentCategoryIncludesItsDescendants() throws Exception {
        // 分类 2（Java 并发）是分类 1（后端基础）的子分类；按 1 过滤必须能拿到分类 2 下的条目
        mockMvc.perform(get("/api/v1/knowledge/entries?categoryId=1&size=50")
                        .header("Authorization", learner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.categoryId==2)]").exists());
    }

    @Test
    void unknownCategoryYieldsEmptyPageInsteadOfError() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge/entries?categoryId=999999").header("Authorization", learner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void chineseKeywordSearchHitsThroughFulltextIndex() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge/entries?q=依赖注入").header("Authorization", learner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id==1)]").exists());
    }

    @Test
    void singleCharacterQueryStillFindsResultsThroughLikeFallback() throws Exception {
        // ngram 分词长度是 2，"锁" 这个单字查询在全文索引上召回为零，必须靠 LIKE 兜住
        mockMvc.perform(get("/api/v1/knowledge/entries?q=锁").header("Authorization", learner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id==2)]").exists());
    }

    @Test
    void searchNeverReturnsDrafts() throws Exception {
        // 关键词必须是**只出现在草稿里**的：ngram 按 bigram 切片，"虚拟线程"的 bigram 里有"线程"，
        // 而条目 2 的正文也含"线程"，用它做断言测不出状态过滤（会命中条目 2 而不是草稿）。
        // sql 里写死 status = 'published'，因此草稿永远不会出现在结果里。
        mockMvc.perform(get("/api/v1/knowledge/entries?q=草稿").header("Authorization", learner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void likeWildcardsInQueryAreTreatedLiterally() throws Exception {
        // "%" 若被当成通配符会匹配全部条目；转义之后应当一条也匹配不到
        mockMvc.perform(get("/api/v1/knowledge/entries?q=%25").header("Authorization", learner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void entryDetailCarriesBodyCategoryAndRelations() throws Exception {
        // 种子关系：条目 1 → 条目 3，类型 prerequisite（"1 是 3 的前置"）。
        // 因此条目 3 的前置是 1，而条目 1 的反向链接是 3——两个方向都断言：
        // 入边/出边很容易写反，写反之后两个方向各自都返回"看起来合理"的结果。
        mockMvc.perform(get("/api/v1/knowledge/entries/3").header("Authorization", learner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.title").value("模块化单体的边界"))
                .andExpect(jsonPath("$.data.categoryName").value("架构"))
                .andExpect(jsonPath("$.data.body").isNotEmpty())
                .andExpect(jsonPath("$.data.prerequisites[?(@.id==1)]").exists())
                .andExpect(jsonPath("$.data.dependents").isEmpty())
                // 关系 2 → 3 是 related（无向），因此第 3 条的相关内容里有条目 2
                .andExpect(jsonPath("$.data.related[?(@.id==2)]").exists());

        mockMvc.perform(get("/api/v1/knowledge/entries/1").header("Authorization", learner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.prerequisites").isEmpty())
                .andExpect(jsonPath("$.data.dependents[?(@.id==3)]").exists());
    }

    @Test
    void draftDetailIsNotFound() throws Exception {
        // 草稿对学习者按"不存在"处理：既看不到，也问不出"这里是否有个未发布的条目"
        mockMvc.perform(get("/api/v1/knowledge/entries/4").header("Authorization", learner()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
    }

    @Test
    void missingEntryIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge/entries/999999").header("Authorization", learner()))
                .andExpect(status().isNotFound());
    }
}
