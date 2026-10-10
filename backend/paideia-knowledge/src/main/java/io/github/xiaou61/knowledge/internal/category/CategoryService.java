package io.github.xiaou61.knowledge.internal.category;

import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 分类的读取用例。
 *
 * <p>写用例（新建／改名／换父／删除）在 `admin` 侧单独一个服务——读路径与写路径的校验
 * 完全不同，混在一起会让"读"也被迫依赖管理员才有的东西。
 */
@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    /** 扁平全量，按展示顺序。装树、取子树、取分类名都基于这一份。 */
    public List<Category> all() {
        return categoryMapper.findAll();
    }

    public List<CategoryTree.Node> tree() {
        return CategoryTree.assemble(categoryMapper.findAll());
    }
}
