package com.xiaou.mockinterview.mapper;

import com.xiaou.mockinterview.domain.CareerApplicationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户投递记录 Mapper。
 */
@Mapper
public interface CareerApplicationRecordMapper {

    int insert(CareerApplicationRecord record);

    int updateOwned(CareerApplicationRecord record);

    int logicalDeleteOwned(@Param("id") Long id, @Param("userId") Long userId);

    CareerApplicationRecord selectByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    List<CareerApplicationRecord> selectByUser(@Param("userId") Long userId,
                                                @Param("status") String status);

    List<CareerApplicationRecord> selectChangedForEvidence(@Param("userId") Long userId,
                                                            @Param("afterUpdateTime") LocalDateTime afterUpdateTime,
                                                            @Param("afterSourceId") Long afterSourceId,
                                                            @Param("limit") int limit);

    List<Long> selectUserIdsChangedForEvidence(@Param("updatedAfter") LocalDateTime updatedAfter,
                                                @Param("limit") int limit);
}
