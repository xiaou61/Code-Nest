package io.github.xiaou61.knowledge.internal.assessment;

import java.time.Instant;

/**
 * 学习者对某条目的自评。
 *
 * <p>本项目**第一条用户私有的业务数据**。它只有两个状态，不做等级、不做权重、不做任何
 * 基于作答的推断——那是后面掌握度子系统的事。之所以先做这一小块，是因为它零 AI 依赖，
 * 却是"千人千面"的第一个数据源。
 */
public class SelfAssessment {

    public static final String LEVEL_UNDERSTOOD = "understood";
    public static final String LEVEL_UNSURE = "unsure";

    private Long id;
    /** 只从令牌取，不接受客户端传入。 */
    private Long userId;
    private Long entryId;
    private String level;
    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getEntryId() {
        return entryId;
    }

    public void setEntryId(Long entryId) {
        this.entryId = entryId;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
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
