package io.github.xiaou61.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlatformContractTest {

    @Test
    @DisplayName("分页参数缺省或非法时收敛到默认值")
    void pageQueryFallsBackToDefaults() {
        assertThat(PageQuery.of(null, null, null)).isEqualTo(new PageQuery(1, 20, null));
        assertThat(PageQuery.of(0, 0, "   ")).isEqualTo(new PageQuery(1, 20, null));
        assertThat(PageQuery.of(-5, -1, null)).isEqualTo(new PageQuery(1, 20, null));
    }

    @Test
    @DisplayName("页大小超限时收敛到上限而不是报错")
    void pageQueryClampsOversizedPage() {
        assertThat(PageQuery.of(2, 10_000, null).size()).isEqualTo(PageQuery.MAX_SIZE);
        assertThat(PageQuery.of(2, PageQuery.MAX_SIZE + 1, null).size()).isEqualTo(PageQuery.MAX_SIZE);
    }

    @Test
    @DisplayName("偏移量按从 1 开始的页码计算，且接近页码上限时不溢出 int")
    void pageQueryComputesOffsetWithoutOverflow() {
        assertThat(PageQuery.of(1, 20, null).offset()).isZero();
        assertThat(PageQuery.of(3, 20, null).offset()).isEqualTo(40);
        assertThat(PageQuery.of(Integer.MAX_VALUE, PageQuery.MAX_SIZE, null).offset())
                .isEqualTo((long) (Integer.MAX_VALUE - 1) * PageQuery.MAX_SIZE);
    }

    @Test
    @DisplayName("排序字段去除首尾空白，纯空白归一为 null")
    void pageQueryNormalizesSort() {
        assertThat(PageQuery.of(1, 20, "  created_at desc  ").sort()).isEqualTo("created_at desc");
        assertThat(PageQuery.of(1, 20, "").sort()).isNull();
    }

    @Test
    @DisplayName("分页结果的元素列表不可变")
    void pageResultItemsAreImmutable() {
        PageResult<String> result = PageResult.of(2, PageQuery.of(1, 10, null), new java.util.ArrayList<>(List.of("a")));

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.items()).containsExactly("a");
        assertThatThrownBy(() -> result.items().add("b"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("统一返回结构携带错误码与消息")
    void apiResponseCarriesErrorCode() {
        ApiResponse<String> ok = ApiResponse.ok("payload");
        assertThat(ok.code()).isEqualTo(ErrorCode.OK.code());
        assertThat(ok.data()).isEqualTo("payload");

        ApiResponse<String> failed = ApiResponse.failure(ErrorCode.FORBIDDEN);
        assertThat(failed.code()).isEqualTo(ErrorCode.FORBIDDEN.code());
        assertThat(failed.message()).isEqualTo(ErrorCode.FORBIDDEN.defaultMessage());
        assertThat(failed.data()).isNull();

        assertThat(failed.withTraceId("t-1").traceId()).isEqualTo("t-1");
    }

    @Test
    @DisplayName("业务异常携带错误码，且自定义消息覆盖默认消息")
    void bizExceptionCarriesErrorCode() {
        BizException defaulted = new BizException(ErrorCode.NOT_FOUND);
        assertThat(defaulted.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(defaulted.getMessage()).isEqualTo(ErrorCode.NOT_FOUND.defaultMessage());

        BizException customized = new BizException(ErrorCode.CONFLICT, "已存在同名资源");
        assertThat(customized.getMessage()).isEqualTo("已存在同名资源");
    }
}
