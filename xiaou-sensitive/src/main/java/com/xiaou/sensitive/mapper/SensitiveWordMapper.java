package com.xiaou.sensitive.mapper;

import com.xiaou.sensitive.domain.SensitiveWord;
import com.xiaou.sensitive.dto.SensitiveWordDTO;
import com.xiaou.sensitive.dto.SensitiveWordQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 敏感词数据访问接口
 */
@Mapper
public interface SensitiveWordMapper {

    /**
     * 分页查询敏感词列表
     * @param query 查询条件
     * @return 敏感词列表
     */
    List<SensitiveWordDTO> selectWordList(SensitiveWordQuery query);

    /**
     * 根据ID查询敏感词
     * @param id 敏感词ID
     * @return 敏感词信息
     */
    SensitiveWord selectWordById(@Param("id") Long id);

    /**
     * 新增敏感词
     * @param word 敏感词信息
     * @return 影响行数
     */
    int insertWord(SensitiveWord word);

    /**
     * 更新敏感词
     * @param word 敏感词信息
     * @return 影响行数
     */
    int updateWord(SensitiveWord word);

    /**
     * 删除敏感词
     * @param id 敏感词ID
     * @return 影响行数
     */
    int deleteWordById(@Param("id") Long id);

    /**
     * 批量删除敏感词
     * @param ids 敏感词ID列表
     * @return 影响行数
     */
    int deleteWordByIds(@Param("ids") List<Long> ids);

    /**
     * 查询所有启用的敏感词
     * @return 敏感词列表
     */
    List<String> selectEnabledWords();

    /**
     * 根据词汇查询是否存在
     * @param word 敏感词
     * @return 敏感词信息
     */
    SensitiveWord selectWordByWord(@Param("word") String word);

    /**
     * 批量插入敏感词
     * @param words 敏感词列表
     * @return 影响行数
     */
    int batchInsertWords(@Param("words") List<SensitiveWord> words);

    /**
     * 批量查询已存在的敏感词
     *
     * @param words 词列表
     * @return 已存在的敏感词列表
     */
    List<SensitiveWord> selectWordsByWords(@Param("words") List<String> words);

    /**
     * 按词汇批量启用敏感词
     *
     * @param words 词列表
     * @return 影响行数
     */
    int batchEnableByWords(@Param("words") List<String> words);

    /**
     * 统计敏感词数量
     * @param query 查询条件
     * @return 数量
     */
    int countWords(SensitiveWordQuery query);
} 
