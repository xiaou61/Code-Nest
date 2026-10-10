package io.github.xiaou61.knowledge.internal.category;

import java.time.Instant;

/**
 * 层级分类。
 *
 * <p>刻意是可变类而不是 record：本项目的 MyBatis 约定用
 * {@code @Options(useGeneratedKeys = true, keyProperty = "id")} 回写自增主键，
 * 那要求实体能写入字段。
 */
public class Category {

    private Long id;
    /** null = 根分类。 */
    private Long parentId;
    private String name;
    private String slug;
    private int sortOrder;
    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
