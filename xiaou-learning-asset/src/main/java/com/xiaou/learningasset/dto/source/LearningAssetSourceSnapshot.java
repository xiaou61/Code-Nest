package com.xiaou.learningasset.dto.source;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 来源快照
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningAssetSourceSnapshot {

    private String sourceType;

    private Long sourceId;

    private String title;

    private String summary;

    private String content;

    private List<String> tags;
}
