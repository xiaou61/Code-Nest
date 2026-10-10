package io.github.xiaou61.knowledge.internal.assessment;

import java.time.Instant;

/** 「我标记过的条目」列表行：条目 + 我给的标记。 */
public class AssessedEntry {

    private Long entryId;
    private String title;
    private String level;
    private Instant updatedAt;

    public Long getEntryId() {
        return entryId;
    }

    public void setEntryId(Long entryId) {
        this.entryId = entryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
