package com.xiaou.interview.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.satoken.SaTokenUserUtil;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.interview.domain.InterviewQuestion;
import com.xiaou.interview.domain.InterviewQuestionSet;
import com.xiaou.interview.dto.InterviewQuestionSetQueryRequest;
import com.xiaou.interview.dto.InterviewQuestionSetRequest;
import com.xiaou.interview.dto.MarkdownImportRequest;
import com.xiaou.interview.mapper.InterviewCategoryMapper;
import com.xiaou.interview.mapper.InterviewQuestionMapper;
import com.xiaou.interview.mapper.InterviewQuestionSetMapper;
import com.xiaou.interview.service.InterviewCategoryService;
import com.xiaou.interview.service.InterviewQuestionSetService;
import com.xiaou.interview.utils.MarkdownParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 面试题单服务实现类
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewQuestionSetServiceImpl implements InterviewQuestionSetService {

    private final InterviewQuestionSetMapper questionSetMapper;
    private final InterviewQuestionMapper questionMapper;
    private final InterviewCategoryMapper categoryMapper;
    private final InterviewCategoryService categoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createQuestionSet(InterviewQuestionSetRequest request) {
        // 验证分类是否存在
        if (categoryMapper.selectById(request.getCategoryId()) == null) {
            throw new BusinessException("分类不存在");
        }

        // 创建人优先取管理员登录态（管理端创建），兼容普通用户登录态（后续开放用户创建时可复用）
        Long creatorId = SaTokenUserUtil.getCurrentAdminId();
        String creatorName = SaTokenUserUtil.getCurrentAdminUsername();
        if (creatorId == null) {
            creatorId = SaTokenUserUtil.getCurrentUserId();
            creatorName = SaTokenUserUtil.getCurrentUserUsername();
        }
        if (creatorId == null) {
            throw new BusinessException("请先登录");
        }
        if (StrUtil.isBlank(creatorName)) {
            creatorName = "系统";
        }

        // 创建题单对象
        InterviewQuestionSet questionSet = new InterviewQuestionSet()
                .setTitle(request.getTitle())
                .setDescription(request.getDescription())
                .setCategoryId(request.getCategoryId())
                .setType(request.getType() != null ? request.getType() : 2) // 默认用户创建
                .setVisibility(request.getVisibility() != null ? request.getVisibility() : 1) // 默认公开
                .setQuestionCount(0)
                .setViewCount(0)
                .setFavoriteCount(0)
                .setStatus(request.getStatus() != null ? request.getStatus() : 0) // 默认草稿
                .setCreatorId(creatorId)
                .setCreatorName(creatorName)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());

        int result = questionSetMapper.insert(questionSet);
        if (result <= 0) {
            throw new BusinessException("创建题单失败");
        }

        // 更新分类的题单数量统计
        categoryService.updateQuestionSetCount(request.getCategoryId());

        log.info("创建题单成功: {}", questionSet.getTitle());
        return questionSet.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuestionSet(Long id, InterviewQuestionSetRequest request) {
        InterviewQuestionSet existingQuestionSet = questionSetMapper.selectById(id);
        if (existingQuestionSet == null) {
            throw new BusinessException("题单不存在");
        }

        // 验证分类是否存在
        if (categoryMapper.selectById(request.getCategoryId()) == null) {
            throw new BusinessException("分类不存在");
        }

        // 记录旧的分类ID，用于后续更新统计
        Long oldCategoryId = existingQuestionSet.getCategoryId();

        // 更新题单对象
        InterviewQuestionSet questionSet = new InterviewQuestionSet()
                .setId(id)
                .setTitle(request.getTitle())
                .setDescription(request.getDescription())
                .setCategoryId(request.getCategoryId())
                .setType(request.getType())
                .setVisibility(request.getVisibility())
                .setStatus(request.getStatus())
                .setUpdateTime(LocalDateTime.now());

        int result = questionSetMapper.updateById(questionSet);
        if (result <= 0) {
            throw new BusinessException("更新题单失败");
        }

        // 如果更换了分类，需要更新两个分类的题单数量统计
        if (!oldCategoryId.equals(request.getCategoryId())) {
            categoryService.updateQuestionSetCount(oldCategoryId);
            categoryService.updateQuestionSetCount(request.getCategoryId());
        }

        log.info("更新题单成功: {}", questionSet.getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQuestionSet(Long id) {
        InterviewQuestionSet questionSet = questionSetMapper.selectById(id);
        if (questionSet == null) {
            throw new BusinessException("题单不存在");
        }

        // 删除题单下的所有题目
        questionMapper.deleteByQuestionSetId(id);

        // 删除题单
        int result = questionSetMapper.deleteById(id);
        if (result <= 0) {
            throw new BusinessException("删除题单失败");
        }

        // 更新分类的题单数量统计
        categoryService.updateQuestionSetCount(questionSet.getCategoryId());

        log.info("删除题单成功: {}", questionSet.getTitle());
    }

    @Override
    public InterviewQuestionSet getQuestionSetById(Long id) {
        InterviewQuestionSet questionSet = questionSetMapper.selectById(id);
        if (questionSet == null) {
            throw new BusinessException("题单不存在");
        }
        return questionSet;
    }

    @Override
    public PageResult<InterviewQuestionSet> getQuestionSets(InterviewQuestionSetQueryRequest request) {
        return PageHelper.doPage(request.getPageNum(), request.getPageSize(), () -> 
            questionSetMapper.selectPage(request)
        );
    }

    @Override
    public List<InterviewQuestionSet> getUserQuestionSets(Long userId) {
        return questionSetMapper.selectByCreatorId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importMarkdownQuestions(MarkdownImportRequest request) {
        // 验证题单是否存在
        InterviewQuestionSet questionSet = questionSetMapper.selectById(request.getQuestionSetId());
        if (questionSet == null) {
            throw new BusinessException("题单不存在");
        }

        // 解析Markdown内容
        MarkdownParser.ParseResult parseResult = MarkdownParser.parseMarkdown(
                request.getMarkdownContent(), request.getQuestionSetId());

        if (parseResult.getSuccessCount() == 0) {
            throw new BusinessException("解析失败: " + String.join(", ", parseResult.getErrors()));
        }

        // 如果是覆盖模式，先删除现有题目
        if (Boolean.TRUE.equals(request.getOverwrite())) {
            questionMapper.deleteByQuestionSetId(request.getQuestionSetId());
        }

        // 批量插入题目
        List<InterviewQuestion> questions = parseResult.getQuestions();
        for (InterviewQuestion question : questions) {
            question.setCreateTime(LocalDateTime.now())
                   .setUpdateTime(LocalDateTime.now());
        }

        int insertCount = 0;
        if (!questions.isEmpty()) {
            insertCount = questionMapper.batchInsert(questions);
        }

        // 更新题单的题目数量
        updateQuestionCount(request.getQuestionSetId());

        log.info("导入题目成功: 题单ID={}, 成功{}题, 失败{}题", 
                request.getQuestionSetId(), parseResult.getSuccessCount(), parseResult.getFailureCount());

        return insertCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseViewCount(Long id) {
        int result = questionSetMapper.increaseViewCount(id);
        if (result <= 0) {
            log.warn("增加题单浏览次数失败: id={}", id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuestionCount(Long id) {
        int count = questionMapper.countByQuestionSetId(id);
        int result = questionSetMapper.updateQuestionCount(id, count);
        if (result <= 0) {
            log.warn("更新题单题目数量失败: id={}", id);
        }
    }

    @Override
    public boolean hasAccessPermission(Long questionSetId, Long userId) {
        if (questionSetId == null || userId == null) {
            return false;
        }
        return questionSetMapper.hasAccessPermission(questionSetId, userId);
    }

    @Override
    public PageResult<InterviewQuestionSet> getPublicQuestionSets(Long categoryId, int page, int size) {
        return PageHelper.doPage(page, size, () -> 
            questionSetMapper.selectPublicQuestionSets(categoryId)
        );
    }

    @Override
    public PageResult<InterviewQuestionSet> searchQuestionSets(String keyword, int page, int size) {
        if (StrUtil.isBlank(keyword)) {
            return PageResult.of(page, size, 0L, Collections.emptyList());
        }

        return PageHelper.doPage(page, size, () -> 
            questionSetMapper.searchQuestionSets(keyword)
        );
    }
} 
