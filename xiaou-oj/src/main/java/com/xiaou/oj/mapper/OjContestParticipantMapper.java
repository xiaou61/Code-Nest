package com.xiaou.oj.mapper;

import com.xiaou.oj.domain.OjContestParticipant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * OJ 赛事参赛者 Mapper
 *
 * @author xiaou
 */
@Mapper
public interface OjContestParticipantMapper {

    int insert(OjContestParticipant participant);

    boolean existsByContestIdAndUserId(@Param("contestId") Long contestId, @Param("userId") Long userId);

    List<OjContestParticipant> selectByContestId(@Param("contestId") Long contestId);

    int countByContestId(@Param("contestId") Long contestId);

    int deleteByContestId(@Param("contestId") Long contestId);
}
