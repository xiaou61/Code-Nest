package com.xiaou.sre.mapper;

import com.xiaou.sre.domain.SreOutboxEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SRE outbox Mapper。
 *
 * @author xiaou
 */
@Mapper
public interface SreOutboxEventMapper {

    int insert(SreOutboxEvent event);

    List<Long> selectPendingIds(@Param("limit") int limit);

    long countPending();

    SreOutboxEvent selectById(Long id);

    int claim(@Param("id") Long id);

    int recoverStaleProcessing(@Param("staleBefore") LocalDateTime staleBefore,
                               @Param("maxAttempts") int maxAttempts);

    int failStaleProcessing(@Param("staleBefore") LocalDateTime staleBefore,
                            @Param("maxAttempts") int maxAttempts);

    int markSucceeded(Long id);

    int markRetry(@Param("id") Long id, @Param("nextAttemptAt") LocalDateTime nextAttemptAt);

    int markFailed(Long id);
}
