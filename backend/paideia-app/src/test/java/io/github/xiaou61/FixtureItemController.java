package io.github.xiaou61;

import io.github.xiaou61.persistence.ExampleItem;
import io.github.xiaou61.persistence.ExampleItemMapper;
import io.github.xiaou61.platform.ApiResponse;
import io.github.xiaou61.platform.BizException;
import io.github.xiaou61.platform.ErrorCode;
import io.github.xiaou61.platform.PageQuery;
import io.github.xiaou61.platform.PageResult;
import io.github.xiaou61.security.CurrentUser;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仅测试使用的受保护资源，用于验证授权隔离。
 *
 * <p>刻意放在根包（而不是子包）里：子包会被 Spring Modulith 当成一个模块，
 * 而它只是测试夹具，不该出现在模块模型里。
 */
@RestController
@RequestMapping("/api/v1/fixture/items")
class FixtureItemController {

    private final ExampleItemMapper mapper;

    FixtureItemController(ExampleItemMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 归属只取自令牌。{@code owner} 参数存在只为验证"客户端试图指定别人的归属时必须被拒"，
     * 它不参与查询条件——查询条件永远来自令牌。
     */
    @GetMapping
    ApiResponse<PageResult<ExampleItem>> list(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(required = false) String owner) {
        String subject = CurrentUser.subjectId();
        if (owner != null && !owner.equals(subject)) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能读取其他用户的数据");
        }

        PageQuery query = PageQuery.of(page, size, null);
        long total = mapper.countByOwner(subject);
        List<ExampleItem> items = mapper.pageByOwner(subject, query.limit(), query.offset());
        return ApiResponse.ok(PageResult.of(total, query, items));
    }
}
