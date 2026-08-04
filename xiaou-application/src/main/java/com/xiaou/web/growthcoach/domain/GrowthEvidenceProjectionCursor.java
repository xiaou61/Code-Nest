package com.xiaou.web.growthcoach.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单用户、单来源的成长证据增量投影游标。
 */
@Data
public class GrowthEvidenceProjectionCursor {

    private Long id;
    private Long userId;
    private String sourceKey;
    private LocalDateTime cursorTime;
    private Long cursorSourceId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
