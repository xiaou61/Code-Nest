package com.xiaou.common.utils;

import com.github.pagehelper.Page;
import com.xiaou.common.core.domain.PageResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageHelperTest {

    @Test
    void shouldKeepTotalWhenConverterReturnsNewList() {
        Page<String> page = new Page<>(1, 2);
        page.setTotal(42);
        page.addAll(List.of("a", "b"));

        PageResult<String> result = PageHelper.doPageAndConvert(
                1,
                2,
                () -> page,
                rows -> rows.stream().map(String::toUpperCase).toList()
        );

        assertThat(result.getTotal()).isEqualTo(42);
        assertThat(result.getRecords()).containsExactly("A", "B");
    }
}
