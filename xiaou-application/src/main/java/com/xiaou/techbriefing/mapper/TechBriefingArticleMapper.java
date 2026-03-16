package com.xiaou.techbriefing.mapper;

import com.xiaou.techbriefing.domain.TechBriefingArticle;
import com.xiaou.techbriefing.dto.request.TechBriefingArticleQueryRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TechBriefingArticleMapper {

    long countPublic(TechBriefingArticleQueryRequest queryRequest);

    List<TechBriefingArticle> selectPublicList(@Param("query") TechBriefingArticleQueryRequest queryRequest,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    List<TechBriefingArticle> selectHighlights(@Param("limit") int limit);

    List<String> selectDistinctCategories();

    TechBriefingArticle selectById(@Param("id") Long id);

    TechBriefingArticle selectBySourceUrl(@Param("sourceUrl") String sourceUrl);

    int insert(TechBriefingArticle article);

    int updateById(TechBriefingArticle article);

    int updatePinStatus(@Param("id") Long id, @Param("isPinned") Boolean isPinned);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    List<TechBriefingArticle> selectAdminList();
}
