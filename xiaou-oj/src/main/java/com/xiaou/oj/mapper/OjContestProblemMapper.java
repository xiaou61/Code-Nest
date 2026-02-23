package com.xiaou.oj.mapper;

import com.xiaou.oj.domain.OjContestProblem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * OJ 赛事题目关联 Mapper
 *
 * @author xiaou
 */
@Mapper
public interface OjContestProblemMapper {

    int deleteByContestId(@Param("contestId") Long contestId);

    int insertBatch(@Param("contestId") Long contestId, @Param("problemIds") List<Long> problemIds);

    List<Long> selectProblemIdsByContestId(@Param("contestId") Long contestId);

    boolean existsProblemInContest(@Param("contestId") Long contestId, @Param("problemId") Long problemId);

    List<OjContestProblem> selectByContestId(@Param("contestId") Long contestId);
}
