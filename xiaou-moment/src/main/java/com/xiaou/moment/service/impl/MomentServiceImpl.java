package com.xiaou.moment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.common.utils.SensitiveWordUtils;
import com.xiaou.moment.service.MomentViewService;
import com.xiaou.notification.api.NotificationCommand;
import com.xiaou.notification.api.NotificationPublisher;
import com.xiaou.user.api.UserInfoApiService;
import com.xiaou.user.api.dto.SimpleUserInfo;
import com.xiaou.moment.domain.Moment;
import com.xiaou.moment.domain.MomentComment;
import com.xiaou.moment.domain.MomentFavorite;
import com.xiaou.moment.domain.MomentLike;
import com.xiaou.moment.dto.*;
import com.xiaou.moment.enums.CommentStatus;
import com.xiaou.moment.enums.MomentStatus;
import com.xiaou.moment.mapper.MomentCommentMapper;
import com.xiaou.moment.mapper.MomentFavoriteMapper;
import com.xiaou.moment.mapper.MomentLikeMapper;
import com.xiaou.moment.mapper.MomentMapper;
import com.xiaou.moment.service.MomentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MomentServiceImpl implements MomentService {
    
    private final MomentMapper momentMapper;
    private final MomentLikeMapper momentLikeMapper;
    private final MomentCommentMapper momentCommentMapper;
    private final MomentFavoriteMapper momentFavoriteMapper;
    private final UserInfoApiService userInfoApiService;
    private final MomentViewService momentViewService;
    private final NotificationPublisher notificationPublisher;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishMoment(MomentPublishRequest request) {
        StpUserUtil.checkLogin();
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        
        // 检查发布频率限制 - 5分钟内最多3条
        checkPublishFrequency(currentUserId);
        
        // 敏感词检测
        SensitiveWordUtils.SensitiveCheckResult checkResult = SensitiveWordUtils.checkText(
            request.getContent(), 
            "moment", 
            null, 
            currentUserId
        );
        
        if (!checkResult.getAllowed()) {
            throw new BusinessException("内容包含敏感词，禁止发布");
        }
        
        Moment moment = new Moment();
        moment.setUserId(currentUserId);
        // 使用敏感词过滤后的内容
        moment.setContent(checkResult.getProcessedText());
        
        // 处理图片URLs
        if (CollUtil.isNotEmpty(request.getImages())) {
            moment.setImages(JSONUtil.toJsonStr(request.getImages()));
        }
        
        moment.setLikeCount(0);
        moment.setCommentCount(0);
        moment.setStatus(MomentStatus.NORMAL.getCode());
        
        momentMapper.insert(moment);
        return moment.getId();
    }
    
    @Override
    public PageResult<MomentListResponse> getMomentList(UserMomentListRequest request) {
        return PageHelper.doPageAndConvert(
                request.getPageNum(),
                request.getPageSize(),
                () -> momentMapper.selectList(new HashMap<>()),
                moments -> {
                    recordMomentViews(moments);
                    return convertToMomentListResponseBatch(moments);
                }
        );
    }
    
    @Override
        @Transactional(rollbackFor = Exception.class)
    public void deleteMoment(Long momentId) {
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }
        
        Moment moment = momentMapper.selectById(momentId);
        if (moment == null) {
            throw new BusinessException("动态不存在");
        }
        
        // 只能删除自己的动态
        if (!moment.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权限删除该动态");
        }
        
        // 删除动态（软删除）
        momentMapper.deleteById(momentId);
        
        // 删除相关评论（软删除）
        momentCommentMapper.deleteByMomentId(momentId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean toggleLike(Long momentId) {
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }
        
        // 检查动态是否存在
        Moment moment = momentMapper.selectById(momentId);
        if (moment == null) {
            throw new BusinessException("动态不存在");
        }
        
        // 检查是否已点赞
        MomentLike existingLike = momentLikeMapper.selectByMomentIdAndUserId(momentId, currentUserId);
        
        if (existingLike != null) {
            // 取消点赞
            int deleted = momentLikeMapper.deleteByMomentIdAndUserId(momentId, currentUserId);
            if (deleted > 0) {
                momentMapper.decrementLikeCount(momentId);
            }
            return false;
        } else {
            // 点赞
            MomentLike momentLike = new MomentLike();
            momentLike.setMomentId(momentId);
            momentLike.setUserId(currentUserId);
            int inserted = momentLikeMapper.insertIfAbsent(momentLike);
            if (inserted == 0) {
                return true;
            }
            momentMapper.incrementLikeCount(momentId);
            
            // 发送消息通知：通知动态作者
            if (!currentUserId.equals(moment.getUserId())) {
                try {
                    SimpleUserInfo currentUser = userInfoApiService.getSimpleUserInfo(currentUserId);
                    String userName = currentUser != null ? currentUser.getDisplayName() : "用户" + currentUserId;
                    
                    notificationPublisher.publish(NotificationCommand.personal(
                        moment.getUserId(),
                        "您的动态收到新点赞",
                        "用户 " + userName + " 点赞了您的动态"
                    ));
                } catch (Exception e) {
                    log.warn("发送动态点赞通知失败，用户ID: {}, 动态ID: {}, 错误: {}", 
                            moment.getUserId(), momentId, e.getMessage());
                }
            }
            
            return true;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishComment(CommentPublishRequest request) {
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }
        
        // 检查动态是否存在
        Moment moment = momentMapper.selectById(request.getMomentId());
        if (moment == null) {
            throw new BusinessException("动态不存在");
        }
        
        // 敏感词检测
        SensitiveWordUtils.SensitiveCheckResult checkResult = SensitiveWordUtils.checkText(
            request.getContent(), 
            "moment_comment", 
            request.getMomentId(), 
            currentUserId
        );
        
        if (!checkResult.getAllowed()) {
            throw new BusinessException("评论内容包含敏感词，禁止发布");
        }
        
        MomentComment comment = new MomentComment();
        comment.setMomentId(request.getMomentId());
        comment.setUserId(currentUserId);
        comment.setContent(checkResult.getProcessedText()); // 使用敏感词过滤后的内容
        comment.setStatus(CommentStatus.NORMAL.getCode());
        
        momentCommentMapper.insert(comment);
        
        // 增加评论数
        momentMapper.incrementCommentCount(request.getMomentId());
        
        // 发送消息通知：通知动态作者
        if (!currentUserId.equals(moment.getUserId())) {
            try {
                SimpleUserInfo currentUser = userInfoApiService.getSimpleUserInfo(currentUserId);
                String userName = currentUser != null ? currentUser.getDisplayName() : "用户" + currentUserId;
                
                notificationPublisher.publish(NotificationCommand.personal(
                    moment.getUserId(),
                    "您的动态收到新评论",
                    "用户 " + userName + " 评论了您的动态：" + checkResult.getProcessedText()
                ));
            } catch (Exception e) {
                log.warn("发送动态评论通知失败，用户ID: {}, 动态ID: {}, 错误: {}", 
                        moment.getUserId(), request.getMomentId(), e.getMessage());
            }
        }
        
        return comment.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId) {
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }
        
        MomentComment comment = momentCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        
        // 获取动态信息
        Moment moment = momentMapper.selectById(comment.getMomentId());
        if (moment == null) {
            throw new BusinessException("动态不存在");
        }
        
        // 检查权限：评论者本人或动态发布者可删除
        if (!comment.getUserId().equals(currentUserId) && !moment.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权限删除该评论");
        }
        
        // 删除评论（软删除）
        momentCommentMapper.deleteById(commentId);
        
        // 减少评论数
        momentMapper.decrementCommentCount(comment.getMomentId());
    }
    
    @Override
    public PageResult<AdminMomentListResponse> getAdminMomentList(AdminMomentListRequest request) {
        return PageHelper.doPageAndConvert(
                request.getPageNum(),
                request.getPageSize(),
                () -> momentMapper.selectList(buildAdminMomentQuery(request)),
                moments -> moments.stream()
                        .map(this::convertToAdminMomentListResponse)
                        .collect(Collectors.toList())
        );
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteMoments(List<Long> momentIds) {
        if (CollUtil.isEmpty(momentIds)) {
            return;
        }
        
        // 使用批量删除，避免N+1问题
        momentMapper.deleteBatchIds(momentIds);
        momentCommentMapper.deleteByMomentIds(momentIds);
    }
    
    /**
     * 转换为动态列表响应
     */
    private MomentListResponse convertToMomentListResponse(Moment moment) {
        MomentListResponse response = BeanUtil.copyProperties(moment, MomentListResponse.class);
        
        // 处理图片URLs
        if (StrUtil.isNotBlank(moment.getImages())) {
            List<String> imageList = JSONUtil.toList(moment.getImages(), String.class);
            response.setImages(imageList);
        } else {
            response.setImages(new ArrayList<>());
        }
        
        // 设置用户信息
        SimpleUserInfo userInfo = userInfoApiService.getSimpleUserInfo(moment.getUserId());
        if (userInfo != null) {
            response.setUserNickname(userInfo.getDisplayName());
            response.setUserAvatar(userInfo.getAvatar());
        } else {
            response.setUserNickname("用户" + moment.getUserId());
            response.setUserAvatar("");
        }
        
        // 设置浏览数和收藏数
        response.setViewCount(moment.getViewCount() != null ? moment.getViewCount() : 0);
        response.setFavoriteCount(moment.getFavoriteCount() != null ? moment.getFavoriteCount() : 0);
        
        // 设置是否已点赞和是否已收藏
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        if (currentUserId != null) {
            MomentLike like = momentLikeMapper.selectByMomentIdAndUserId(moment.getId(), currentUserId);
            response.setIsLiked(like != null);
            
            MomentFavorite favorite = momentFavoriteMapper.selectByMomentIdAndUserId(moment.getId(), currentUserId);
            response.setIsFavorited(favorite != null);
            
            response.setCanDelete(moment.getUserId().equals(currentUserId));
        } else {
            response.setIsLiked(false);
            response.setIsFavorited(false);
            response.setCanDelete(false);
        }
        
        // 获取最新3条评论
        List<MomentComment> comments = momentCommentMapper.selectByMomentId(moment.getId(), 3);
        List<CommentResponse> commentResponses = comments.stream()
                .map(this::convertToCommentResponse)
                .collect(Collectors.toList());
        response.setRecentComments(commentResponses);
        
        return response;
    }
    
    /**
     * 转换为管理端动态列表响应
     */
    private AdminMomentListResponse convertToAdminMomentListResponse(Moment moment) {
        AdminMomentListResponse response = BeanUtil.copyProperties(moment, AdminMomentListResponse.class);
        
        // 处理图片URLs
        if (StrUtil.isNotBlank(moment.getImages())) {
            response.setImages(JSONUtil.toList(moment.getImages(), String.class));
        } else {
            response.setImages(new ArrayList<>());
        }
        
        // 设置状态描述
        if (moment.getStatus().equals(MomentStatus.NORMAL.getCode())) {
            response.setStatusDesc(MomentStatus.NORMAL.getDesc());
        } else if (moment.getStatus().equals(MomentStatus.DELETED.getCode())) {
            response.setStatusDesc(MomentStatus.DELETED.getDesc());
        } else if (moment.getStatus().equals(MomentStatus.REVIEWING.getCode())) {
            response.setStatusDesc(MomentStatus.REVIEWING.getDesc());
        }
        
        // 设置用户信息
        response.setUserNickname("用户" + moment.getUserId());
        
        // 检查敏感词（暂时设为false，后续可接入敏感词检测服务）
        response.setHasSensitiveWord(false);
        
        return response;
    }
    
    /**
     * 转换为评论响应
     */
    private CommentResponse convertToCommentResponse(MomentComment comment) {
        CommentResponse response = BeanUtil.copyProperties(comment, CommentResponse.class);
        
        // 设置用户信息
        SimpleUserInfo userInfo = userInfoApiService.getSimpleUserInfo(comment.getUserId());
        if (userInfo != null) {
            response.setUserNickname(userInfo.getDisplayName());
            response.setUserAvatar(userInfo.getAvatar());
        } else {
            response.setUserNickname("用户" + comment.getUserId());
            response.setUserAvatar("");
        }
        
        // 设置是否可删除
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        if (currentUserId != null) {
            // 评论者本人或动态发布者可删除
            Moment moment = momentMapper.selectById(comment.getMomentId());
            response.setCanDelete(comment.getUserId().equals(currentUserId) || 
                                (moment != null && moment.getUserId().equals(currentUserId)));
        } else {
            response.setCanDelete(false);
        }
        
        return response;
    }
    
    /**
     * 检查发布频率限制
     */
    private void checkPublishFrequency(Long userId) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("startDate", LocalDateTime.now().minusMinutes(5));
        
        Long count = momentMapper.selectCount(params);
        if (count >= 3) {
            throw new BusinessException("发布过于频繁，请稍后再试");
        }
    }
    
    @Override
    public PageResult<AdminCommentListResponse> getAdminCommentList(AdminCommentListRequest request) {
        return PageHelper.doPageAndConvert(
                request.getPageNum(),
                request.getPageSize(),
                () -> momentCommentMapper.selectList(buildAdminCommentQuery(request)),
                comments -> comments.stream()
                        .map(this::convertToAdminCommentListResponse)
                        .collect(Collectors.toList())
        );
    }

    private Map<String, Object> buildAdminMomentQuery(AdminMomentListRequest request) {
        Map<String, Object> params = new HashMap<>();
        if (StrUtil.isNotBlank(request.getUserNickname())) {
            params.put("userNickname", request.getUserNickname());
        }
        if (request.getStatus() != null) {
            params.put("status", request.getStatus());
        }
        if (StrUtil.isNotBlank(request.getStartDate())) {
            params.put("startDate", request.getStartDate());
        }
        if (StrUtil.isNotBlank(request.getEndDate())) {
            params.put("endDate", request.getEndDate());
        }
        if (request.getHasSensitiveWord() != null) {
            params.put("hasSensitiveWord", request.getHasSensitiveWord());
        }
        return params;
    }

    private Map<String, Object> buildAdminCommentQuery(AdminCommentListRequest request) {
        Map<String, Object> params = new HashMap<>();
        if (request.getMomentId() != null) {
            params.put("momentId", request.getMomentId());
        }
        if (StrUtil.isNotBlank(request.getUserNickname())) {
            params.put("userNickname", request.getUserNickname());
        }
        if (StrUtil.isNotBlank(request.getContentKeyword())) {
            params.put("contentKeyword", "%" + request.getContentKeyword() + "%");
        }
        if (request.getStatus() != null) {
            params.put("status", request.getStatus());
        }
        if (StrUtil.isNotBlank(request.getStartDate())) {
            params.put("startDate", request.getStartDate());
        }
        if (StrUtil.isNotBlank(request.getEndDate())) {
            params.put("endDate", request.getEndDate());
        }
        return params;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteComment(Long commentId) {
        MomentComment comment = momentCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        
        // 删除评论（软删除）
        momentCommentMapper.deleteById(commentId);
        
        // 减少评论数
        momentMapper.decrementCommentCount(comment.getMomentId());
    }
    
    @Override
    public PageResult<CommentResponse> getMomentComments(UserMomentCommentsRequest request) {
        // 检查动态是否存在
        Moment moment = momentMapper.selectById(request.getMomentId());
        if (moment == null) {
            throw new BusinessException("动态不存在");
        }
        
        return PageHelper.doPageAndConvert(
                request.getPageNum(),
                request.getPageSize(),
                () -> momentCommentMapper.selectByMomentId(request.getMomentId(), null),
                comments -> {
                    List<Long> userIds = comments.stream()
                            .map(MomentComment::getUserId)
                            .distinct()
                            .collect(Collectors.toList());
                    Map<Long, SimpleUserInfo> userInfoMap = userInfoApiService.getSimpleUserInfoBatch(userIds);
                    Long currentUserId = StpUserUtil.getLoginIdAsLong();
                    return comments.stream()
                            .map(comment -> convertToCommentResponseWithUserInfo(comment, userInfoMap, currentUserId))
                            .collect(Collectors.toList());
                }
        );
    }
    
    @Override
    public MomentStatisticsResponse getStatistics(StatisticsRequest request) {
        MomentStatisticsResponse response = new MomentStatisticsResponse();
        
        // 统计总数据
        Map<String, Object> params = new HashMap<>();
        if (StrUtil.isNotBlank(request.getStartDate())) {
            // 转换为完整的时间格式
            params.put("startDate", request.getStartDate() + " 00:00:00");
        }
        if (StrUtil.isNotBlank(request.getEndDate())) {
            // 转换为完整的时间格式
            params.put("endDate", request.getEndDate() + " 23:59:59");
        }
        
        // 总动态数
        response.setTotalMoments(momentMapper.selectCount(params));
        
        // 总点赞数（需要新增方法统计）
        response.setTotalLikes(getTotalLikes(params));
        
        // 总评论数
        response.setTotalComments(momentCommentMapper.selectCount(params));
        
        // 活跃用户数（发布过动态的用户数）
        response.setActiveUsers(getActiveUsersCount(params));
        
        // 每日统计数据
        response.setDailyStats(getDailyStatistics(request));
        
        return response;
    }
    
    /**
     * 转换为管理端评论列表响应
     */
    private AdminCommentListResponse convertToAdminCommentListResponse(MomentComment comment) {
        AdminCommentListResponse response = BeanUtil.copyProperties(comment, AdminCommentListResponse.class);
        
        // 设置状态描述
        if (comment.getStatus().equals(CommentStatus.NORMAL.getCode())) {
            response.setStatusDesc(CommentStatus.NORMAL.getDesc());
        } else if (comment.getStatus().equals(CommentStatus.DELETED.getCode())) {
            response.setStatusDesc(CommentStatus.DELETED.getDesc());
        }
        
        // 设置用户信息
        response.setUserNickname("用户" + comment.getUserId());
        
        // 设置动态内容摘要
        Moment moment = momentMapper.selectById(comment.getMomentId());
        if (moment != null) {
            String content = moment.getContent();
            if (StrUtil.length(content) > 50) {
                content = StrUtil.sub(content, 0, 50) + "...";
            }
            response.setMomentContentSummary(content);
        }
        
        return response;
    }
    
    /**
     * 获取总点赞数
     */
    private Long getTotalLikes(Map<String, Object> params) {
        return momentMapper.selectTotalLikes(params);
    }
    
    /**
     * 获取活跃用户数
     */
    private Long getActiveUsersCount(Map<String, Object> params) {
        return momentMapper.selectActiveUsersCount(params);
    }
    
    /**
     * 获取每日统计数据
     */
    private List<DailyStatistics> getDailyStatistics(StatisticsRequest request) {
        List<DailyStatistics> dailyStats = new ArrayList<>();
        
        // 如果没有指定日期范围，默认返回最近7天的数据
        String endDate = StrUtil.isNotBlank(request.getEndDate()) ? request.getEndDate() : 
                        LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String startDate = StrUtil.isNotBlank(request.getStartDate()) ? request.getStartDate() : 
                          LocalDateTime.now().minusDays(6).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        try {
            LocalDateTime start = LocalDateTime.parse(startDate + " 00:00:00", 
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LocalDateTime end = LocalDateTime.parse(endDate + " 23:59:59", 
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            // 按天循环统计
            LocalDateTime current = start;
            while (!current.isAfter(end)) {
                String dateStr = current.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                
                // 统计当天的数据
                Map<String, Object> dayParams = new HashMap<>();
                dayParams.put("startDate", dateStr + " 00:00:00");
                dayParams.put("endDate", dateStr + " 23:59:59");
                
                DailyStatistics dayStat = new DailyStatistics();
                dayStat.setDate(dateStr);
                dayStat.setMomentCount(momentMapper.selectCount(dayParams));
                dayStat.setLikeCount(momentMapper.selectTotalLikes(dayParams));
                dayStat.setCommentCount(momentCommentMapper.selectCount(dayParams));
                dayStat.setActiveUserCount(momentMapper.selectActiveUsersCount(dayParams));
                
                dailyStats.add(dayStat);
                current = current.plusDays(1);
            }
        } catch (Exception e) {
            log.error("获取每日统计数据失败", e);
        }
        
        return dailyStats;
    }
    
    @Override
    public List<MomentListResponse> getHotMoments(HotMomentRequest request) {
        List<Moment> moments = momentMapper.selectHotMoments(request.getLimit());
        return convertToMomentListResponseBatch(moments);
    }
    
    @Override
    public PageResult<MomentListResponse> searchMoments(MomentSearchRequest request) {
        return PageHelper.doPageAndConvert(
                request.getPageNum(),
                request.getPageSize(),
                () -> momentMapper.searchMoments(request.getKeyword()),
                moments -> {
                    recordMomentViews(moments);
                    return convertToMomentListResponseBatch(moments);
                }
        );
    }
    
    @Override
    public PageResult<MomentListResponse> getUserMomentList(Long userId, Integer pageNum, Integer pageSize) {
        return PageHelper.doPageAndConvert(
                pageNum,
                pageSize,
                () -> momentMapper.selectByUserId(userId),
                moments -> {
                    recordMomentViews(moments);
                    return convertToMomentListResponseBatch(moments);
                }
        );
    }
    
    @Override
    public UserMomentInfoResponse getUserMomentInfo(Long userId) {
        UserMomentInfoResponse response = new UserMomentInfoResponse();
        response.setUserId(userId);
        
        // 统计数据
        response.setTotalMoments(momentMapper.countByUserId(userId));
        response.setTotalLikes(momentMapper.countTotalLikesByUserId(userId));
        response.setTotalComments(momentMapper.countTotalCommentsByUserId(userId));
        
        // 获取用户昵称和头像
        SimpleUserInfo userInfo = userInfoApiService.getSimpleUserInfo(userId);
        if (userInfo != null) {
            response.setNickname(userInfo.getDisplayName());
            response.setAvatar(userInfo.getAvatar());
        } else {
            response.setNickname("用户" + userId);
            response.setAvatar("");
        }
        
        return response;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean toggleFavorite(Long momentId) {
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }
        
        // 检查动态是否存在
        Moment moment = momentMapper.selectById(momentId);
        if (moment == null) {
            throw new BusinessException("动态不存在");
        }
        
        // 检查是否已收藏
        MomentFavorite existingFavorite =
            momentFavoriteMapper.selectByMomentIdAndUserId(momentId, currentUserId);
        
        if (existingFavorite != null) {
            // 取消收藏
            int deleted = momentFavoriteMapper.delete(momentId, currentUserId);
            if (deleted > 0) {
                momentMapper.decrementFavoriteCount(momentId);
            }
            return false;
        } else {
            // 收藏
            MomentFavorite favorite = new MomentFavorite();
            favorite.setMomentId(momentId);
            favorite.setUserId(currentUserId);
            int inserted = momentFavoriteMapper.insertIfAbsent(favorite);
            if (inserted == 0) {
                return true;
            }
            momentMapper.incrementFavoriteCount(momentId);
            
            // 发送消息通知：通知动态作者
            if (!currentUserId.equals(moment.getUserId())) {
                try {
                    SimpleUserInfo currentUser = userInfoApiService.getSimpleUserInfo(currentUserId);
                    String userName = currentUser != null ? currentUser.getDisplayName() : "用户" + currentUserId;
                    
                    notificationPublisher.publish(NotificationCommand.personal(
                        moment.getUserId(),
                        "您的动态收到新收藏",
                        "用户 " + userName + " 收藏了您的动态"
                    ));
                } catch (Exception e) {
                    log.warn("发送动态收藏通知失败，用户ID: {}, 动态ID: {}, 错误: {}", 
                            moment.getUserId(), momentId, e.getMessage());
                }
            }
            
            return true;
        }
    }
    
    @Override
    public PageResult<MomentListResponse> getMyFavorites(Integer pageNum, Integer pageSize) {
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }
        
        return PageHelper.doPageAndConvert(
                pageNum,
                pageSize,
                () -> momentFavoriteMapper.selectFavoriteMomentIdsByUserId(currentUserId),
                momentIds -> {
                    if (CollUtil.isEmpty(momentIds)) {
                        return Collections.emptyList();
                    }
                    List<Moment> moments = momentMapper.selectByIds(momentIds);
                    recordMomentViews(moments);
                    return convertToMomentListResponseBatch(moments);
                }
        );
    }
    
    /**
     * 批量记录动态浏览数
     */
    private void recordMomentViews(List<Moment> moments) {
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        moments.forEach(moment -> {
            try {
                momentViewService.recordView(moment.getId(), currentUserId);
            } catch (Exception e) {
                log.debug("记录动态浏览失败: momentId={}", moment.getId(), e);
            }
        });
    }
    
    /**
     * 批量转换为动态列表响应（性能优化版本）
     * 批量查询用户信息、点赞状态、收藏状态，避免N+1查询问题
     */
    private List<MomentListResponse> convertToMomentListResponseBatch(List<Moment> moments) {
        if (CollUtil.isEmpty(moments)) {
            return Collections.emptyList();
        }
        
        // 1. 收集所有用户ID
        Set<Long> userIds = moments.stream()
                .map(Moment::getUserId)
                .collect(Collectors.toSet());
        
        // 2. 批量查询用户信息，避免N+1问题
        Map<Long, SimpleUserInfo> userInfoMap = userInfoApiService.getSimpleUserInfoBatch(
                new ArrayList<>(userIds));
        
        // 3. 获取当前用户ID
        Long currentUserId = StpUserUtil.getLoginIdAsLong();
        
        // 4. 批量查询点赞状态
        Set<Long> likedMomentIds = new HashSet<>();
        if (currentUserId != null) {
            List<Long> momentIds = moments.stream().map(Moment::getId).collect(Collectors.toList());
            List<Long> liked = momentLikeMapper.selectLikedMomentIds(momentIds, currentUserId);
            if (CollUtil.isNotEmpty(liked)) {
                likedMomentIds.addAll(liked);
            }
        }
        
        // 5. 批量查询收藏状态
        Set<Long> favoritedMomentIds = new HashSet<>();
        if (currentUserId != null) {
            List<Long> momentIds = moments.stream().map(Moment::getId).collect(Collectors.toList());
            List<Long> favorited = momentFavoriteMapper.selectFavoritedMomentIds(momentIds, currentUserId);
            if (CollUtil.isNotEmpty(favorited)) {
                favoritedMomentIds.addAll(favorited);
            }
        }
        
        // 6. 批量获取所有动态的评论，并收集评论用户ID
        List<Long> momentIds = moments.stream().map(Moment::getId).collect(Collectors.toList());
        Map<Long, List<MomentComment>> momentCommentsMap = new HashMap<>();
        Set<Long> commentUserIds = new HashSet<>();
        
        for (Long momentId : momentIds) {
            List<MomentComment> comments = momentCommentMapper.selectByMomentId(momentId, 3);
            momentCommentsMap.put(momentId, comments);
            comments.forEach(c -> commentUserIds.add(c.getUserId()));
        }
        
        // 7. 批量查询评论用户信息
        Map<Long, SimpleUserInfo> commentUserInfoMap = new HashMap<>();
        if (!commentUserIds.isEmpty()) {
            commentUserInfoMap = userInfoApiService.getSimpleUserInfoBatch(new ArrayList<>(commentUserIds));
        }
        final Map<Long, SimpleUserInfo> finalCommentUserInfoMap = commentUserInfoMap;
        
        // 8. 批量转换
        return moments.stream().map(moment -> {
            MomentListResponse response = BeanUtil.copyProperties(moment, MomentListResponse.class);
            
            // 处理图片URLs
            if (StrUtil.isNotBlank(moment.getImages())) {
                List<String> imageList = JSONUtil.toList(moment.getImages(), String.class);
                response.setImages(imageList);
            } else {
                response.setImages(new ArrayList<>());
            }
            
            // 设置用户信息
            SimpleUserInfo userInfo = userInfoMap.get(moment.getUserId());
            if (userInfo != null) {
                response.setUserNickname(userInfo.getDisplayName());
                response.setUserAvatar(userInfo.getAvatar());
            } else {
                response.setUserNickname("用户" + moment.getUserId());
                response.setUserAvatar("");
            }
            
            // 设置浏览数和收藏数
            response.setViewCount(moment.getViewCount() != null ? moment.getViewCount() : 0);
            response.setFavoriteCount(moment.getFavoriteCount() != null ? moment.getFavoriteCount() : 0);
            
            // 设置是否已点赞和是否已收藏
            response.setIsLiked(likedMomentIds.contains(moment.getId()));
            response.setIsFavorited(favoritedMomentIds.contains(moment.getId()));
            response.setCanDelete(currentUserId != null && moment.getUserId().equals(currentUserId));
            
            // 转换评论（使用已批量查询的用户信息）
            List<MomentComment> comments = momentCommentsMap.getOrDefault(moment.getId(), Collections.emptyList());
            List<CommentResponse> commentResponses = comments.stream()
                    .map(comment -> convertToCommentResponseWithUserInfo(comment, finalCommentUserInfoMap, currentUserId))
                    .collect(Collectors.toList());
            response.setRecentComments(commentResponses);
            
            return response;
        }).collect(Collectors.toList());
    }
    
    /**
     * 转换评论响应（使用已查询的用户信息Map）
     */
    private CommentResponse convertToCommentResponseWithUserInfo(MomentComment comment, 
                                                                  Map<Long, SimpleUserInfo> userInfoMap,
                                                                  Long currentUserId) {
        CommentResponse response = BeanUtil.copyProperties(comment, CommentResponse.class);
        
        // 设置用户信息
        SimpleUserInfo userInfo = userInfoMap.get(comment.getUserId());
        if (userInfo != null) {
            response.setUserNickname(userInfo.getDisplayName());
            response.setUserAvatar(userInfo.getAvatar());
        } else {
            response.setUserNickname("用户" + comment.getUserId());
            response.setUserAvatar("");
        }
        
        // 设置是否可删除（简化处理，仅评论者本人可删除）
        response.setCanDelete(currentUserId != null && comment.getUserId().equals(currentUserId));
        
        return response;
    }
}
