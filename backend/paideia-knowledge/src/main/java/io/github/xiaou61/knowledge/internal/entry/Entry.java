package io.github.xiaou61.knowledge.internal.entry;

import java.time.Instant;

/**
 * 知识条目。
 *
 * <p>一行一个条目、正文是 markdown 源码（渲染在前端做）。可变类的原因同 {@code Category}：
 * 本项目用 {@code useGeneratedKeys} 回写自增主键。
 *
 * <p>查询时会**按需只填部分字段**：列表与相邻条目查询不取 {@code body}（正文可能有几十 KB，
 * 列表里取回来纯属浪费），此时 {@code body} 为 null。判断"有没有正文"不能靠它。
 */
public class Entry {

    private Long id;
    private Long categoryId;
    private String title;
    private String body;
    /** draft | published */
    private String status;
    private Instant publishedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
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
