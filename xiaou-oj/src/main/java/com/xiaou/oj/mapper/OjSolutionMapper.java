package com.xiaou.oj.mapper;

import com.xiaou.oj.domain.OjSolution;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * OJ标准答案Mapper
 *
 * @author xiaou
 */
@Mapper
public interface OjSolutionMapper {

    int insert(OjSolution solution);

    int updateById(OjSolution solution);

    int deleteById(Long id);

    int deleteByProblemId(Long problemId);

    OjSolution selectById(Long id);

    List<OjSolution> selectByProblemId(Long problemId);

    int countByProblemId(Long problemId);
}
