package com.xiaou.sre.mapper;

import com.xiaou.sre.domain.SreRcaEvaluationSuite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Stable RCA evaluation suite Mapper.
 *
 * @author xiaou
 */
@Mapper
public interface SreRcaEvaluationSuiteMapper {

    int insert(SreRcaEvaluationSuite suite);

    SreRcaEvaluationSuite selectById(@Param("id") Long id);

    SreRcaEvaluationSuite selectByIdForUpdate(@Param("id") Long id);

    SreRcaEvaluationSuite selectBySuiteKey(@Param("suiteKey") String suiteKey);

    List<SreRcaEvaluationSuite> selectRecent(@Param("limit") int limit);
}
