package com.xiaou.sre.mapper;

import com.xiaou.sre.domain.SreRcaEvaluationRun;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RCA 离线评测运行 Mapper。
 *
 * @author xiaou
 */
@Mapper
public interface SreRcaEvaluationRunMapper {

    int insert(SreRcaEvaluationRun run);

    SreRcaEvaluationRun selectById(@Param("id") Long id);

    List<Long> selectClaimableIds(@Param("now") LocalDateTime now, @Param("limit") int limit);

    int claim(@Param("id") Long id, @Param("claimedAt") LocalDateTime claimedAt);

    int updateHeartbeat(SreRcaEvaluationRun run);

    int requeue(@Param("id") Long id, @Param("nextAttemptAt") LocalDateTime nextAttemptAt);

    int failExpired(@Param("now") LocalDateTime now, @Param("failureCode") String failureCode);

    int failStale(@Param("staleBefore") LocalDateTime staleBefore,
                  @Param("maxAttempts") int maxAttempts,
                  @Param("failureCode") String failureCode);

    int recoverStale(@Param("staleBefore") LocalDateTime staleBefore,
                     @Param("maxAttempts") int maxAttempts,
                     @Param("nextAttemptAt") LocalDateTime nextAttemptAt);

    int updateCompletion(SreRcaEvaluationRun run);

    int updateFailed(@Param("id") Long id,
                     @Param("failureCode") String failureCode,
                     @Param("completedAt") LocalDateTime completedAt);

    List<SreRcaEvaluationRun> selectRecent(@Param("limit") int limit);
}
