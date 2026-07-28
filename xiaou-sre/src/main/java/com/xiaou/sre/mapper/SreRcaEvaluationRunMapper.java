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

    int updateCompletion(SreRcaEvaluationRun run);

    int updateFailed(@Param("id") Long id,
                     @Param("failureCode") String failureCode,
                     @Param("completedAt") LocalDateTime completedAt);

    List<SreRcaEvaluationRun> selectRecent(@Param("limit") int limit);
}
