package com.xiaou.oj.mapper;

import com.xiaou.oj.domain.OjTestCase;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * OJ测试用例Mapper
 *
 * @author xiaou
 */
@Mapper
public interface OjTestCaseMapper {

    int insert(OjTestCase testCase);

    int updateById(OjTestCase testCase);

    int deleteById(Long id);

    int deleteByProblemId(Long problemId);

    OjTestCase selectById(Long id);

    List<OjTestCase> selectByProblemId(Long problemId);

    int countByProblemId(Long problemId);

    /**
     * 查询题目的示例测试用例 (is_sample=1)
     */
    List<OjTestCase> selectSampleByProblemId(Long problemId);
}
