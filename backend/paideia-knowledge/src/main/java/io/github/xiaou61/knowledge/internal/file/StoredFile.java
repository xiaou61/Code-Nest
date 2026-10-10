package io.github.xiaou61.knowledge.internal.file;

import java.time.Instant;

/**
 * 附件记录。
 *
 * <p>主键就是 UUID 本身（表里 {@code id CHAR(36)}），**没有自增列**：这个端点是全项目唯一
 * 免鉴权的读接口，自增主键等于把全部附件按序号列出来。
 *
 * <p>与其它实体不同，这里**不用** {@code useGeneratedKeys}——id 由存储实现生成后再写入，
 * 而不是让数据库生成。
 */
public class StoredFile {

    /** 对外标识（UUID），同时是磁盘文件名。 */
    private String id;
    private String originalName;
    /** **入库时嗅探所得**的类型，不是客户端声明的。 */
    private String contentType;
    private long sizeBytes;
    private Long uploaderId;
    private Instant createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Long getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(Long uploaderId) {
        this.uploaderId = uploaderId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
