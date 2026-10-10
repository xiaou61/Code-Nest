package io.github.xiaou61.knowledge.internal.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 检索判定与 LIKE 转义的纯逻辑测试。
 *
 * <p>两条必须钉住的边界：**单个汉字搜不到**（ngram 的固有行为，必须退回 LIKE）与
 * **通配符被转义**（用户输入的 `%` 是字面量，不能当通配符用）。
 */
class SearchQueryTest {

    @Test
    void normalizeTrimsAndCollapsesBlankToNull() {
        assertThat(SearchQuery.normalize(null)).isNull();
        assertThat(SearchQuery.normalize("")).isNull();
        assertThat(SearchQuery.normalize("   ")).isNull();
        assertThat(SearchQuery.normalize("  并发  ")).isEqualTo("并发");
    }

    @Test
    void singleChineseCharacterFallsBackToLike() {
        // ngram 分词长度是 2，单字查询在全文索引上永远召回为零——必须走 LIKE
        assertThat(SearchQuery.usesFulltext("锁")).isFalse();
        assertThat(SearchQuery.usesFulltext("a")).isFalse();
        assertThat(SearchQuery.usesFulltext(null)).isFalse();
    }

    @Test
    void twoCharacterQueryUsesFulltext() {
        assertThat(SearchQuery.usesFulltext("并发")).isTrue();
        assertThat(SearchQuery.usesFulltext("ab")).isTrue();
    }

    @Test
    void likePatternEscapesWildcardsAndBackslash() {
        assertThat(SearchQuery.likePattern("100%")).isEqualTo("%100\\%%");
        assertThat(SearchQuery.likePattern("a_b")).isEqualTo("%a\\_b%");
        assertThat(SearchQuery.likePattern("a\\b")).isEqualTo("%a\\\\b%");
        assertThat(SearchQuery.likePattern("并发")).isEqualTo("%并发%");
    }
}
