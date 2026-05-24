package com.xiaou.codepen.mapper;

import com.xiaou.codepen.domain.CodePenForkTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * Fork交易记录Mapper
 * 
 * @author xiaou
 */
@Mapper
public interface CodePenForkTransactionMapper {
    
    /**
     * 插入交易记录
     */
    int insert(CodePenForkTransaction transaction);
    
    /**
     * 根据ID查询交易记录
     */
    CodePenForkTransaction selectById(Long id);
    
    /**
     * 查询原作者的收益记录
     */
    List<CodePenForkTransaction> selectByAuthorId(@Param("authorId") Long authorId);
    
    /**
     * 查询用户的Fork记录
     */
    List<CodePenForkTransaction> selectByForkUserId(@Param("forkUserId") Long forkUserId);
    
    /**
     * 查询作品的Fork交易记录
     */
    List<CodePenForkTransaction> selectByOriginalPenId(@Param("originalPenId") Long originalPenId);
    
    /**
     * 查询用户是否已Fork过指定作品
     */
    CodePenForkTransaction selectByOriginalPenIdAndForkUserId(@Param("originalPenId") Long originalPenId, @Param("forkUserId") Long forkUserId);

    /**
     * 批量查询用户已购买的原作品ID
     */
    Set<Long> selectPurchasedOriginalPenIdsByForkUserId(@Param("forkUserId") Long forkUserId,
                                                        @Param("originalPenIds") List<Long> originalPenIds);
}

