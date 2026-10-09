package io.github.xiaou61.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.xiaou61.platform.PageQuery;
import io.github.xiaou61.platform.PageResult;
import java.util.List;
import javax.sql.DataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 分页与归属过滤在真实 MySQL 上的集成测试。
 *
 * <p>自建 SqlSessionFactory 而不是启动 Spring 上下文：这里要验证的是 SQL 与映射本身，
 * 应用级装配（数据源、Flyway、mapper 扫描）由 paideia-app 的测试覆盖。
 *
 * <p>未配置 {@code PAIDEIA_TEST_DB_PASSWORD} 时跳过并如实报告为未运行——集成测试依赖外部数据库，
 * 不该让没有该环境的机器构建失败；但也不能悄悄算作通过。
 */
class PaginationIntegrationTest {

    private static SqlSessionFactory sessionFactory;

    @BeforeAll
    static void setUpDatabase() {
        if (!TestDatabase.isConfigured()) {
            // 明确喊出来：跳过时 surefire 只报 "Tests run: 0"，很容易被误读成通过
            System.err.println("""
                    [Paideia] 跳过 PaginationIntegrationTest：未设置 %s。
                    这些用例需要真实 MySQL（默认指向 127.0.0.1:3307 的 SSH 隧道到服务器上的 paideia_test）。
                    本次构建未验证分页与归属过滤的 SQL。
                    """.formatted(TestDatabase.PASSWORD_ENV));
        }
        assumeTrue(TestDatabase.isConfigured(),
                "未设置 " + TestDatabase.PASSWORD_ENV + "，跳过需要真实 MySQL 的集成测试");

        DataSource dataSource = TestDatabase.dataSource();
        TestDatabase.migrate(dataSource);

        Configuration configuration = new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(ExampleItemMapper.class);
        sessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    private SqlSession openSession() {
        return sessionFactory.openSession(true);
    }

    private void seed(SqlSession session, String ownerId, int count) {
        ExampleItemMapper mapper = session.getMapper(ExampleItemMapper.class);
        for (int index = 1; index <= count; index++) {
            mapper.insert(ownerId, ownerId + "-item-" + index);
        }
    }

    @Test
    @DisplayName("分页返回正确的页大小与总数，且翻页不重不漏")
    void pagesCorrectly() {
        try (SqlSession session = openSession()) {
            ExampleItemMapper mapper = session.getMapper(ExampleItemMapper.class);
            mapper.deleteAll();
            seed(session, "learner-a", 7);

            PageQuery first = PageQuery.of(1, 3, null);
            List<ExampleItem> firstPage = mapper.pageByOwner("learner-a", first.limit(), first.offset());
            long total = mapper.countByOwner("learner-a");

            PageResult<ExampleItem> result = PageResult.of(total, first, firstPage);

            assertThat(result.total()).isEqualTo(7);
            assertThat(result.page()).isEqualTo(1);
            assertThat(result.size()).isEqualTo(3);
            assertThat(result.items()).hasSize(3);
            assertThat(result.items()).extracting(ExampleItem::title)
                    .containsExactly("learner-a-item-1", "learner-a-item-2", "learner-a-item-3");

            PageQuery third = PageQuery.of(3, 3, null);
            List<ExampleItem> lastPage = mapper.pageByOwner("learner-a", third.limit(), third.offset());

            assertThat(lastPage).hasSize(1);
            assertThat(lastPage.getFirst().title()).isEqualTo("learner-a-item-7");
        }
    }

    @Test
    @DisplayName("越界的页码返回空页而不是报错或回退到第一页")
    void outOfRangePageReturnsEmpty() {
        try (SqlSession session = openSession()) {
            ExampleItemMapper mapper = session.getMapper(ExampleItemMapper.class);
            mapper.deleteAll();
            seed(session, "learner-a", 2);

            PageQuery beyond = PageQuery.of(99, 10, null);

            assertThat(mapper.pageByOwner("learner-a", beyond.limit(), beyond.offset())).isEmpty();
            assertThat(mapper.countByOwner("learner-a")).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("归属过滤只返回本用户的行；不带过滤的对照查询证明这不是查询本身为空")
    void ownershipFilterIsolatesRows() {
        try (SqlSession session = openSession()) {
            ExampleItemMapper mapper = session.getMapper(ExampleItemMapper.class);
            mapper.deleteAll();
            seed(session, "learner-a", 3);
            seed(session, "learner-b", 2);

            PageQuery query = PageQuery.of(1, 20, null);

            assertThat(mapper.pageByOwner("learner-a", query.limit(), query.offset()))
                    .extracting(ExampleItem::ownerId)
                    .containsOnly("learner-a");
            assertThat(mapper.countByOwner("learner-a")).isEqualTo(3);
            assertThat(mapper.countByOwner("learner-b")).isEqualTo(2);

            // 对照：不走归属过滤时会看到两个用户的数据，说明过滤条件是真正起作用的那一步
            assertThat(mapper.pageAll(query.limit(), query.offset())).hasSize(5);
        }
    }

    @Test
    @DisplayName("页大小超过上限时被 PageQuery 收敛，不会拖回整表")
    void oversizedPageIsClamped() {
        PageQuery query = PageQuery.of(1, 100_000, null);

        assertThat(query.limit()).isEqualTo(PageQuery.MAX_SIZE);
    }
}
