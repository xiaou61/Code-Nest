package com.xiaou.techbriefing.mapper;

import com.xiaou.techbriefing.domain.TechBriefingArticleContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TechBriefingArticleContentMapper {

    TechBriefingArticleContent selectByArticleId(@Param("articleId") Long articleId);

    int insert(TechBriefingArticleContent content);

    int updateByArticleId(TechBriefingArticleContent content);
}
