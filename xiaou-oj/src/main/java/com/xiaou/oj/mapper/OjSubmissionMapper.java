package com.xiaou.oj.mapper;

import com.xiaou.oj.domain.OjSubmission;
import com.xiaou.oj.dto.SubmissionQueryRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * OJ提交记录Mapper
 *
 * @author xiaou
 */
@Mapper
public interface OjSubmissionMapper {

    int insert(OjSubmission submission);

    int updateById(OjSubmission submission);

    OjSubmission selectById(Long id);

    List<OjSubmission> selectPage(@Param("request") SubmissionQueryRequest request);

    long countPage(@Param("request") SubmissionQueryRequest request);

    /**
     * 统计用户总提交数
     */
    int countByUserId(Long userId);

    /**
     * 统计用户AC的不同题目数
     */
    int countAcceptedProblemsByUserId(Long userId);

    /**
     * 统计用户尝试过的不同题目数
     */
    int countAttemptedProblemsByUserId(Long userId);

    /**
     * 按难度统计用户AC题目数
     */
    int countAcceptedByDifficulty(@Param("userId") Long userId, @Param("difficulty") String difficulty);

    /**
     * 检查用户是否已经AC过某题
     */
    boolean existsAccepted(@Param("userId") Long userId, @Param("problemId") Long problemId);

    /**
     * 排行榜：总榜
     */
    List<com.xiaou.oj.dto.RankingItem> selectRankingAll(@Param("limit") int limit);

    /**
     * 排行榜：周榜
     */
    List<com.xiaou.oj.dto.RankingItem> selectRankingWeekly(@Param("weekStart") String weekStart, @Param("limit") int limit);

    /**
     * 检查是否为用户对某题的首次AC（无更早的accepted提交）
     */
    boolean isFirstAccepted(@Param("userId") Long userId, @Param("problemId") Long problemId, @Param("submissionId") Long submissionId);
}
