package com.xiaou.oj.mapper;

import com.xiaou.oj.domain.OjContest;
import com.xiaou.oj.dto.contest.ContestQueryRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * OJ 赛事 Mapper
 *
 * @author xiaou
 */
@Mapper
public interface OjContestMapper {

    int insert(OjContest contest);

    int updateById(OjContest contest);

    int deleteById(Long id);

    OjContest selectById(Long id);

    List<OjContest> selectPage(@Param("request") ContestQueryRequest request);

    long countPage(@Param("request") ContestQueryRequest request);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
