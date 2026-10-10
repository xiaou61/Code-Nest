package io.github.xiaou61.knowledge.internal.relation;

import java.time.Instant;

/** 条目之间的一条关系。可变类的原因同其它实体：本项目用 {@code useGeneratedKeys} 回写主键。 */
public class Relation {

    /** 前置：{@code fromEntryId} 是 {@code toEntryId} 的前置。 */
    public static final String TYPE_PREREQUISITE = "prerequisite";
    /** 关联：无向语义，只存一条边。 */
    public static final String TYPE_RELATED = "related";

    private Long id;
    private Long fromEntryId;
    private Long toEntryId;
    private String relationType;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFromEntryId() {
        return fromEntryId;
    }

    public void setFromEntryId(Long fromEntryId) {
        this.fromEntryId = fromEntryId;
    }

    public Long getToEntryId() {
        return toEntryId;
    }

    public void setToEntryId(Long toEntryId) {
        this.toEntryId = toEntryId;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
