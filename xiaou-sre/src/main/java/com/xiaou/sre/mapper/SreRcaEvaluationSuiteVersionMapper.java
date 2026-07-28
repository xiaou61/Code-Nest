package com.xiaou.sre.mapper;

import com.xiaou.sre.domain.SreRcaEvaluationSuiteVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Immutable RCA evaluation suite version Mapper.
 *
 * @author xiaou
 */
@Mapper
public interface SreRcaEvaluationSuiteVersionMapper {

    int insert(SreRcaEvaluationSuiteVersion version);

    SreRcaEvaluationSuiteVersion selectById(@Param("id") Long id);

    SreRcaEvaluationSuiteVersion selectBySuiteIdAndManifestSha256(
            @Param("suiteId") Long suiteId,
            @Param("manifestSha256") String manifestSha256);

    Integer selectLatestVersionNo(@Param("suiteId") Long suiteId);

    List<SreRcaEvaluationSuiteVersion> selectBySuiteId(
            @Param("suiteId") Long suiteId,
            @Param("limit") int limit);
}
