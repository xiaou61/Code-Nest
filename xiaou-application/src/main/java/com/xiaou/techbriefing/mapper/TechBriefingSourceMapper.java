package com.xiaou.techbriefing.mapper;

import com.xiaou.techbriefing.domain.TechBriefingSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TechBriefingSourceMapper {

    List<TechBriefingSource> selectEnabledSources();

    List<TechBriefingSource> selectAll();

    TechBriefingSource selectById(@Param("id") Long id);

    int insert(TechBriefingSource source);

    int update(TechBriefingSource source);

    int deleteById(@Param("id") Long id);
}
