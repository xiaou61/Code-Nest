package com.xiaou.oj.mapper;

import com.xiaou.oj.domain.OjProblem;
import com.xiaou.oj.dto.ProblemQueryRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * OJ题目Mapper
 *
 * @author xiaou
 */
@Mapper
public interface OjProblemMapper {

    int insert(OjProblem problem);

    int updateById(OjProblem problem);

    int deleteById(Long id);

    OjProblem selectById(Long id);

    List<OjProblem> selectPage(@Param("request") ProblemQueryRequest request);

    long countPage(@Param("request") ProblemQueryRequest request);

    int increaseSubmitCount(Long id);

    int increaseAcceptedCount(Long id);

    /**
     * 插入题目-标签关联
     */
    int insertProblemTag(@Param("problemId") Long problemId, @Param("tagId") Long tagId);

    /**
     * 删除题目的所有标签关联
     */
    int deleteProblemTags(Long problemId);

    /**
     * 查询题目的标签ID列表
     */
    List<Long> selectTagIdsByProblemId(Long problemId);
}
