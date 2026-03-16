package com.xiaou.techbriefing.mapper;

import com.xiaou.techbriefing.domain.TechBriefingFetchLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TechBriefingFetchLogMapper {

    int insert(TechBriefingFetchLog fetchLog);

    List<TechBriefingFetchLog> selectLatest(@Param("limit") int limit);
}
