package com.xiaou.techbriefing.dto.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RSS 解析后的文章项
 */
@Data
@Accessors(chain = true)
public class TechBriefingFeedItem {

    private String title;

    private String link;

    private String summary;

    private String content;

    private String imageUrl;

    private LocalDateTime publishTime;

    private List<String> categories;
}
