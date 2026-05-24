package com.xiaou.codepen.mapper;

import com.xiaou.codepen.domain.CodePen;
import com.xiaou.codepen.dto.CodePenListRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 代码作品Mapper
 * 
 * @author xiaou
 */
@Mapper
public interface CodePenMapper {
    
    /**
     * 插入作品
     */
    int insert(CodePen codePen);
    
    /**
     * 根据ID查询作品
     */
    CodePen selectById(Long id);

    /**
     * 根据ID列表批量查询作品
     */
    List<CodePen> selectByIds(@Param("ids") List<Long> ids);
    
    /**
     * 根据ID查询作品（不包含代码，用于列表展示）
     */
    CodePen selectByIdWithoutCode(Long id);
    
    /**
     * 更新作品
     */
    int updateById(CodePen codePen);
    
    /**
     * 根据ID删除作品（软删除，更新status=3）
     */
    int deleteById(Long id);
    
    /**
     * 分页查询作品列表
     */
    List<CodePen> selectList(CodePenListRequest request);
    
    /**
     * 更新作品状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    /**
     * 更新发布时间
     */
    int updatePublishTime(@Param("id") Long id);
    
    /**
     * 增加Fork次数
     */
    int incrementForkCount(Long id);
    
    /**
     * 增加点赞数
     */
    int incrementLikeCount(Long id);
    
    /**
     * 减少点赞数
     */
    int decrementLikeCount(Long id);
    
    /**
     * 增加收藏数
     */
    int incrementCollectCount(Long id);
    
    /**
     * 减少收藏数
     */
    int decrementCollectCount(Long id);
    
    /**
     * 增加评论数
     */
    int incrementCommentCount(Long id);
    
    /**
     * 减少评论数
     */
    int decrementCommentCount(Long id);
    
    /**
     * 增加浏览数
     */
    int incrementViewCount(Long id);
    
    /**
     * 更新作品收益
     */
    int updateIncome(@Param("id") Long id, @Param("income") Integer income);
    
    /**
     * 设置推荐
     */
    int setRecommend(@Param("id") Long id, @Param("expireTime") String expireTime);
    
    /**
     * 取消推荐
     */
    int cancelRecommend(Long id);
    
    /**
     * 查询用户作品列表
     */
    List<CodePen> selectByUserId(@Param("userId") Long userId, @Param("status") Integer status);
    
    /**
     * 查询推荐作品列表
     */
    List<CodePen> selectRecommendList(@Param("limit") Integer limit);
    
    /**
     * 查询热门作品列表
     */
    List<CodePen> selectHotList(@Param("limit") Integer limit);
    
    /**
     * 查询系统模板列表
     */
    List<CodePen> selectTemplateList();
    
    /**
     * 查询作品总数
     */
    Long selectTotalCount();
    
    /**
     * 查询今日新增作品数
     */
    Long selectTodayCount();
    
    /**
     * 查询创建过作品的用户数
     */
    Long selectUserCount();
    
    /**
     * 查询总浏览数
     */
    Long selectTotalViewCount();
    
    /**
     * 查询总点赞数
     */
    Long selectTotalLikeCount();
    
    /**
     * 查询总评论数
     */
    Long selectTotalCommentCount();
    
    /**
     * 查询总收藏数
     */
    Long selectTotalCollectCount();
    
    /**
     * 查询总Fork数
     */
    Long selectTotalForkCount();
}

