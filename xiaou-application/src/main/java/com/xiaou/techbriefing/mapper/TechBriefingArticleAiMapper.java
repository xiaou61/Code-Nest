package com.xiaou.techbriefing.mapper;

import com.xiaou.techbriefing.domain.TechBriefingArticleAi;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TechBriefingArticleAiMapper {

    TechBriefingArticleAi selectByArticleId(@Param("articleId") Long articleId);

    List<TechBriefingArticleAi> selectByArticleIds(@Param("articleIds") List<Long> articleIds);

    int insert(TechBriefingArticleAi articleAi);

    int updateByArticleId(TechBriefingArticleAi articleAi);
}
