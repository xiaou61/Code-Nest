package io.github.xiaou61.knowledge.internal.category;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 分类树的纯逻辑测试。
 *
 * <p>没有数据库也能跑——这正是把装树从服务里拎出来的原因：顺序、孤儿、环这三类问题
 * 一旦漏到集成测试才发现，排查成本高得多。
 */
class CategoryTreeTest {

    private static Category row(Long id, Long parentId, String name) {
        Category category = new Category();
        category.setId(id);
        category.setParentId(parentId);
        category.setName(name);
        category.setSlug("s" + id);
        return category;
    }

    @Test
    void assemblesHierarchyKeepingRowOrder() {
        List<CategoryTree.Node> tree = CategoryTree.assemble(List.of(
                row(1L, null, "后端基础"),
                row(2L, 1L, "Java 并发"),
                row(3L, 1L, "JVM"),
                row(4L, null, "架构")));

        assertThat(tree).extracting(CategoryTree.Node::name).containsExactly("后端基础", "架构");
        assertThat(tree.get(0).children()).extracting(CategoryTree.Node::name)
                .containsExactly("Java 并发", "JVM");
        assertThat(tree.get(1).children()).isEmpty();
    }

    @Test
    void orphanWithMissingParentFallsBackToRoot() {
        // 父被直接删掉的行不能连整棵子树一起消失，挂到根至少看得见
        List<CategoryTree.Node> tree = CategoryTree.assemble(List.of(
                row(9L, 404L, "父已不存在的分类")));

        assertThat(tree).extracting(CategoryTree.Node::name).containsExactly("父已不存在的分类");
    }

    @Test
    void cycleDoesNotHangAndDropsUnreachableNodes() {
        // 1 -> 2 -> 1 成环：两个节点都不可达根，结果是"不出现"而不是死循环
        List<CategoryTree.Node> tree = CategoryTree.assemble(List.of(
                row(1L, 2L, "环上的甲"),
                row(2L, 1L, "环上的乙"),
                row(3L, null, "正常")));

        assertThat(tree).extracting(CategoryTree.Node::name).containsExactly("正常");
    }

    @Test
    void subtreeIdsIncludesTheNodeItselfAndAllDescendants() {
        List<Category> rows = List.of(
                row(1L, null, "后端基础"),
                row(2L, 1L, "Java 并发"),
                row(3L, 2L, "虚拟线程"),
                row(4L, null, "架构"));

        assertThat(CategoryTree.subtreeIds(rows, 1L)).containsExactly(1L, 2L, 3L);
        assertThat(CategoryTree.subtreeIds(rows, 2L)).containsExactly(2L, 3L);
        assertThat(CategoryTree.subtreeIds(rows, 4L)).containsExactly(4L);
    }

    @Test
    void subtreeIdsOfUnknownCategoryIsEmpty() {
        assertThat(CategoryTree.subtreeIds(List.of(row(1L, null, "甲")), 999L)).isEmpty();
        assertThat(CategoryTree.subtreeIds(List.of(row(1L, null, "甲")), null)).isEmpty();
    }

    @Test
    void subtreeIdsTerminatesOnCycles() {
        List<Category> rows = List.of(row(1L, 2L, "甲"), row(2L, 1L, "乙"));

        assertThat(CategoryTree.subtreeIds(rows, 1L)).containsExactly(1L, 2L);
    }

    @Test
    void ancestorIdsWalksUpToTheRoot() {
        List<Category> rows = List.of(
                row(1L, null, "后端基础"),
                row(2L, 1L, "Java 并发"),
                row(3L, 2L, "虚拟线程"));

        assertThat(CategoryTree.ancestorIds(rows, 3L)).containsExactly(2L, 1L);
        assertThat(CategoryTree.ancestorIds(rows, 1L)).isEmpty();
    }
}
