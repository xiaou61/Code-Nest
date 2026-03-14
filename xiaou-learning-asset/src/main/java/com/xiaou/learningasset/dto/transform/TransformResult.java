package com.xiaou.learningasset.dto.transform;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 转化结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransformResult {

    @Builder.Default
    private List<TransformCandidateDraft> candidates = new ArrayList<>();

    private String summary;
}
