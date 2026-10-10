package io.github.xiaou61.knowledge.internal.admin;

import io.github.xiaou61.knowledge.internal.category.Category;
import io.github.xiaou61.knowledge.internal.category.CategoryMapper;
import io.github.xiaou61.knowledge.internal.category.CategoryTree;
import io.github.xiaou61.knowledge.internal.entry.EntryMapper;
import io.github.xiaou61.platform.BizException;
import io.github.xiaou61.platform.ErrorCode;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 分类的管理端写入用例。
 *
 * <p>两条校验是这里的重点，都是"数据库拦不住、只能应用层判"的：
 *
 * <ul>
 *   <li><b>换父不能成环</b>——把节点挂到自己的后代下会让整棵子树从根不可达（装树时直接
 *       不出现），而外键对此毫无意见。这是唯一能产生环的入口，必须拦在写路径上。</li>
 *   <li><b>删除要拒绝，不能级联</b>——分类自引用带上 {@code ON DELETE CASCADE} 会一次删掉
 *       整棵子树，那是静默的数据丢失。数据库层因此刻意不加级联，由这里给出明确原因。</li>
 * </ul>
 */
@Service
public class CategoryAdminService {

    private final CategoryMapper categoryMapper;
    private final EntryMapper entryMapper;

    CategoryAdminService(CategoryMapper categoryMapper, EntryMapper entryMapper) {
        this.categoryMapper = categoryMapper;
        this.entryMapper = entryMapper;
    }

    @Transactional
    public Category create(Long parentId, String name, String slug, Integer sortOrder) {
        String trimmedName = requireText(name, "分类名称不能为空");
        String trimmedSlug = requireSlug(slug);
        requireParent(parentId, null);
        if (categoryMapper.countSiblingsWithName(parentId, trimmedName, null) > 0) {
            // 同级重名不是数据错误，但会让管理员在界面上分不清点的是哪个——早点拒绝
            throw conflict("同一级下已经有同名分类");
        }

        Category category = new Category();
        category.setParentId(parentId);
        category.setName(trimmedName);
        category.setSlug(trimmedSlug);
        category.setSortOrder(sortOrder == null ? 0 : sortOrder);
        try {
            categoryMapper.insert(category);
        } catch (DuplicateKeyException exception) {
            // 并发下两个请求同时通过上面的检查；唯一索引是最后一道闸
            throw conflict("分类标识已被占用");
        }
        return category;
    }

    @Transactional
    public void update(Long id, Long parentId, String name, String slug, Integer sortOrder) {
        Category existing = categoryMapper.findById(id);
        if (existing == null) {
            throw notFound("分类不存在");
        }
        String trimmedName = requireText(name, "分类名称不能为空");
        String trimmedSlug = requireSlug(slug);
        requireParent(parentId, id);
        if (categoryMapper.countSiblingsWithName(parentId, trimmedName, id) > 0) {
            throw conflict("同一级下已经有同名分类");
        }

        existing.setParentId(parentId);
        existing.setName(trimmedName);
        existing.setSlug(trimmedSlug);
        existing.setSortOrder(sortOrder == null ? 0 : sortOrder);
        try {
            categoryMapper.update(existing);
        } catch (DuplicateKeyException exception) {
            throw conflict("分类标识已被占用");
        }
    }

    @Transactional
    public void delete(Long id) {
        if (categoryMapper.findById(id) == null) {
            throw notFound("分类不存在");
        }
        if (categoryMapper.countChildren(id) > 0) {
            throw conflict("该分类下还有子分类，请先移动或删除它们");
        }
        if (entryMapper.countByCategory(id) > 0) {
            throw conflict("该分类下还有条目，请先把它们移到别的分类");
        }
        categoryMapper.deleteById(id);
    }

    /** 父必须真实存在；{@code selfId} 非空时还要保证新父不在自己的子树里（否则成环）。 */
    private void requireParent(Long parentId, Long selfId) {
        if (parentId == null) {
            return;
        }
        if (selfId != null && parentId.equals(selfId)) {
            throw invalid("分类不能以自己为父");
        }
        List<Category> all = categoryMapper.findAll();
        if (all.stream().noneMatch(category -> category.getId().equals(parentId))) {
            throw invalid("父分类不存在");
        }
        if (selfId != null) {
            List<Long> subtree = CategoryTree.subtreeIds(all, selfId);
            if (subtree.contains(parentId)) {
                throw invalid("不能把分类挂到它自己的子分类下");
            }
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw invalid(message);
        }
        return value.trim();
    }

    private static String requireSlug(String slug) {
        String trimmed = requireText(slug, "分类标识不能为空");
        if (!trimmed.matches("[a-z0-9-]{1,64}")) {
            throw invalid("分类标识只能用小写字母、数字与连字符");
        }
        return trimmed;
    }

    private static BizException invalid(String message) {
        return new BizException(ErrorCode.INVALID_ARGUMENT, message);
    }

    private static BizException conflict(String message) {
        return new BizException(ErrorCode.CONFLICT, message);
    }

    private static BizException notFound(String message) {
        return new BizException(ErrorCode.NOT_FOUND, message);
    }
}
