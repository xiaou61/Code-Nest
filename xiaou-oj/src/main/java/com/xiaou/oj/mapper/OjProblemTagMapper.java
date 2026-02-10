package com.xiaou.oj.mapper;

import com.xiaou.oj.domain.OjProblemTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * OJ题目标签Mapper
 *
 * @author xiaou
 */
@Mapper
public interface OjProblemTagMapper {

    int insert(OjProblemTag tag);

    int deleteById(Long id);

    OjProblemTag selectById(Long id);

    List<OjProblemTag> selectAll();

    List<OjProblemTag> selectByIds(@Param("ids") List<Long> ids);
}
