package com.xiaou.codepen.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.xiaou.codepen.config.CodePenProperties;
import com.xiaou.codepen.domain.CodePen;
import com.xiaou.codepen.domain.CodePenForkTransaction;
import com.xiaou.codepen.dto.*;
import com.xiaou.codepen.mapper.*;
import com.xiaou.codepen.service.CodePenService;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.points.domain.UserPointsBalance;
import com.xiaou.points.domain.UserPointsDetail;
import com.xiaou.points.enums.PointsType;
import com.xiaou.points.mapper.UserPointsBalanceMapper;
import com.xiaou.points.mapper.UserPointsDetailMapper;
import com.xiaou.user.api.UserInfoApiService;
import com.xiaou.user.api.dto.SimpleUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.xiaou.codepen.constant.CodePenConstants.CREATE_REWARD_POINTS;

/**
 * 代码作品Service实现
 * 
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodePenServiceImpl implements CodePenService {
    
    private final CodePenMapper codePenMapper;
    private final CodePenLikeMapper codePenLikeMapper;
    private final CodePenCollectMapper codePenCollectMapper;
    private final CodePenForkTransactionMapper forkTransactionMapper;
    private final UserPointsBalanceMapper pointsBalanceMapper;
    private final UserPointsDetailMapper pointsDetailMapper;
    private final UserInfoApiService userInfoApiService;
    private final CodePenProperties codePenProperties;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CodePenCreateResponse createOrSave(CodePenCreateRequest request, Long userId) {
        // 验证标题
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BusinessException("作品标题不能为空");
        }
        if (request.getTitle().length() > 100) {
            throw new BusinessException("作品标题不能超过100个字符");
        }
        
        // 验证至少有一种代码
        if ((request.getHtmlCode() == null || request.getHtmlCode().trim().isEmpty()) &&
            (request.getCssCode() == null || request.getCssCode().trim().isEmpty()) &&
            (request.getJsCode() == null || request.getJsCode().trim().isEmpty())) {
            throw new BusinessException("至少需要有一种代码（HTML/CSS/JS）");
        }
        
        // 验证Fork价格
        if (request.getForkPrice() != null && (request.getForkPrice() < 0 || request.getForkPrice() > 1000)) {
            throw new BusinessException("Fork价格范围为0-1000积分");
        }
        
        boolean isNewPublish = false;
        CodePen codePen;
        
        if (request.getId() != null) {
            // 更新已有作品
            codePen = codePenMapper.selectById(request.getId());
            if (codePen == null) {
                throw new BusinessException("作品不存在");
            }
            if (!codePen.getUserId().equals(userId)) {
                throw new BusinessException("无权操作");
            }
            
            // 如果是草稿首次发布，需要奖励积分
            if (codePen.getStatus() == 0 && request.getStatus() != null && request.getStatus() == 1) {
                isNewPublish = true;
            }
            
            updateCodePenFields(codePen, request);
        } else {
            // 新建作品
            codePen = buildCodePen(request, userId);
            
            // 如果状态是发布，标记为新发布
            if (request.getStatus() != null && request.getStatus() == 1) {
                isNewPublish = true;
            }
        }
        
        // 如果是新发布的作品，奖励积分
        Integer pointsTotal = null;
        if (isNewPublish) {
            // 确保用户有积分账户
            ensureUserPointsBalance(userId);
            
            // 增加积分
            pointsBalanceMapper.addPoints(userId, codePenProperties.getCreateRewardPoints());
            
            // 获取增加后的余额
            UserPointsBalance balance = pointsBalanceMapper.selectByUserId(userId);
            pointsTotal = balance.getTotalPoints();
            
            // 记录积分明细
            UserPointsDetail detail = new UserPointsDetail();
            detail.setUserId(userId);
            detail.setPointsChange(codePenProperties.getCreateRewardPoints());
            detail.setPointsType(PointsType.ADMIN_GRANT.getCode());
            detail.setDescription("创建代码作品");
            detail.setBalanceAfter(balance.getTotalPoints());
            detail.setCreateTime(LocalDateTime.now());
            pointsDetailMapper.insert(detail);
            
            // 设置发布时间
            codePen.setPublishTime(new Date());
        }
        
        // 保存作品
        if (request.getId() != null) {
            codePenMapper.updateById(codePen);
        } else {
            codePenMapper.insert(codePen);
        }
        
        // 构建响应
        CodePenCreateResponse response = CodePenCreateResponse.builder()
                .penId(codePen.getId())
                .status(codePen.getStatus())
                .createTime(codePen.getCreateTime())
                .build();
        
        if (isNewPublish) {
            response.setPointsAdded(codePenProperties.getCreateRewardPoints());
            response.setPointsTotal(pointsTotal);
            response.setShareUrl(codePenProperties.getShareBaseUrl() + codePen.getId());
        }
        
        log.info("用户{}{}作品成功，作品ID：{}", userId, 
                isNewPublish ? "发布" : "保存", codePen.getId());
        
        return response;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(CodePenCreateRequest request, Long userId) {
        if (request.getId() == null) {
            throw new BusinessException("作品ID不能为空");
        }
        
        CodePen codePen = codePenMapper.selectById(request.getId());
        if (codePen == null) {
            throw new BusinessException("作品不存在");
        }
        if (!codePen.getUserId().equals(userId)) {
            throw new BusinessException("无权操作");
        }
        
        updateCodePenFields(codePen, request);
        return codePenMapper.updateById(codePen) > 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id, Long userId) {
        CodePen codePen = codePenMapper.selectById(id);
        if (codePen == null) {
            throw new BusinessException("作品不存在");
        }
        if (!codePen.getUserId().equals(userId)) {
            throw new BusinessException("无权操作");
        }
        
        return codePenMapper.deleteById(id) > 0;
    }
    
    @Override
    public CodePenDetailResponse getDetail(Long id, Long currentUserId) {
        CodePen codePen = codePenMapper.selectById(id);
        if (codePen == null) {
            throw new BusinessException("作品不存在");
        }
        return buildDetailedResponses(List.of(codePen), currentUserId).get(0);
    }

    @Override
    public List<CodePenDetailResponse> getDetailsByIds(List<Long> ids, Long currentUserId) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        List<CodePen> codePens = codePenMapper.selectByIds(ids);
        if (codePens == null || codePens.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, CodePen> codePenMap = codePens.stream()
                .collect(Collectors.toMap(CodePen::getId, codePen -> codePen, (left, right) -> left));
        List<CodePen> orderedPens = ids.stream()
                .map(codePenMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return buildDetailedResponses(orderedPens, currentUserId);
    }
    
    @Override
    public PageResult<CodePenDetailResponse> getList(CodePenListRequest request, Long currentUserId) {
        // 使用PageHelper进行分页
        PageResult<CodePen> pageResult = com.xiaou.common.utils.PageHelper.doPage(
            request.getPageNum(), 
            request.getPageSize(), 
            () -> codePenMapper.selectList(request)
        );
        
        List<CodePen> records = pageResult.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(pageResult.getPageNum(), pageResult.getPageSize(), pageResult.getTotal(), new ArrayList<>());
        }
        
        // 提取所有作品ID
        List<Long> penIds = records.stream().map(CodePen::getId).collect(Collectors.toList());
        
        // 批量查询用户的点赞/收藏状态（一次查询代替 N 次）
        Set<Long> likedPenIds = Collections.emptySet();
        Set<Long> collectedPenIds = Collections.emptySet();
        if (currentUserId != null) {
            likedPenIds = codePenLikeMapper.selectLikedPenIdsByUserId(currentUserId, penIds);
            collectedPenIds = codePenCollectMapper.selectCollectedPenIdsByUserId(currentUserId, penIds);
            if (likedPenIds == null) likedPenIds = Collections.emptySet();
            if (collectedPenIds == null) collectedPenIds = Collections.emptySet();
        }
        
        // 在内存中进行匹配
        List<CodePenDetailResponse> responses = new ArrayList<>();
        for (CodePen codePen : records) {
            CodePenDetailResponse response = convertToDetailResponse(codePen);
            
            // 列表不返回代码
            response.setHtmlCode(null);
            response.setCssCode(null);
            response.setJsCode(null);
            
            // 设置互动状态（从内存中匹配，无需查询数据库）
            if (currentUserId != null) {
                response.setIsLiked(likedPenIds.contains(codePen.getId()));
                response.setIsCollected(collectedPenIds.contains(codePen.getId()));
            }
            
            responses.add(response);
        }
        
        // 构造返回结果，保持分页信息
        return PageResult.of(
            pageResult.getPageNum(),
            pageResult.getPageSize(),
            pageResult.getTotal(),
            responses
        );
    }
    
    @Override
    public List<CodePenDetailResponse> getMyList(Long userId, Integer status) {
        List<CodePen> list = codePenMapper.selectByUserId(userId, status);
        
        // 批量查询用户信息，避免N+1问题
        List<Long> userIds = list.stream()
                .map(CodePen::getUserId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, SimpleUserInfo> userInfoMap = userInfoApiService.getSimpleUserInfoBatch(userIds);
        
        List<CodePenDetailResponse> responses = new ArrayList<>();
        for (CodePen codePen : list) {
            CodePenDetailResponse response = convertToDetailResponseWithUserInfo(codePen, userInfoMap);
            response.setCanEdit(true);
            response.setCanDelete(true);
            responses.add(response);
        }
        
        return responses;
    }
    
    @Override
    public CheckForkPriceResponse checkForkPrice(Long penId, Long userId) {
        CodePen codePen = codePenMapper.selectById(penId);
        if (codePen == null) {
            throw new BusinessException("作品不存在");
        }
        
        // 获取用户积分
        UserPointsBalance balance = pointsBalanceMapper.selectByUserId(userId);
        int currentPoints = balance != null ? balance.getTotalPoints() : 0;
        
        boolean isFree = codePen.getIsFree() == 1;
        int forkPrice = codePen.getForkPrice() != null ? codePen.getForkPrice() : 0;
        boolean canFork = isFree || currentPoints >= forkPrice;
        
        // 获取作者名称
        String authorName = null;
        if (codePen.getUserId() != null) {
            SimpleUserInfo userInfo = userInfoApiService.getSimpleUserInfo(codePen.getUserId());
            if (userInfo != null) {
                authorName = userInfo.getDisplayName();
            }
        }
        
        CheckForkPriceResponse response = CheckForkPriceResponse.builder()
                .penId(penId)
                .isFree(isFree)
                .forkPrice(forkPrice)
                .currentPoints(currentPoints)
                .canFork(canFork)
                .authorName(authorName)
                .build();
        
        if (!isFree && !canFork) {
            response.setMessage("积分不足，还需要" + (forkPrice - currentPoints) + "积分");
        }
        
        return response;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ForkResponse forkPen(ForkRequest request, Long userId) {
        CodePen originalPen = codePenMapper.selectById(request.getPenId());
        if (originalPen == null) {
            throw new BusinessException("作品不存在");
        }
        
        boolean isFree = originalPen.getIsFree() == 1;
        int forkPrice = originalPen.getForkPrice() != null ? originalPen.getForkPrice() : 0;
        Integer remainingPoints = null;
        
        // 如果是付费作品，处理积分交易
        if (!isFree && forkPrice > 0) {
            // 检查积分是否充足
            UserPointsBalance balance = pointsBalanceMapper.selectByUserId(userId);
            if (balance == null || balance.getTotalPoints() < forkPrice) {
                int currentPoints = balance != null ? balance.getTotalPoints() : 0;
                throw new BusinessException("积分不足，当前积分：" + currentPoints + "，Fork需要" + forkPrice + "积分");
            }
            
            // 扣除Fork用户的积分
            int result = pointsBalanceMapper.subtractPoints(userId, forkPrice);
            if (result <= 0) {
                throw new BusinessException("积分扣除失败");
            }
            
            // 增加原作者的积分
            ensureUserPointsBalance(originalPen.getUserId());
            pointsBalanceMapper.addPoints(originalPen.getUserId(), forkPrice);
            
            // 更新原作品的收益
            codePenMapper.updateIncome(originalPen.getId(), forkPrice);
            
            // 获取扣除后的余额
            balance = pointsBalanceMapper.selectByUserId(userId);
            remainingPoints = balance.getTotalPoints();
            
            // 记录积分明细 - Fork用户扣除
            UserPointsDetail detail1 = new UserPointsDetail();
            detail1.setUserId(userId);
            detail1.setPointsChange(-forkPrice);
            detail1.setPointsType(PointsType.ADMIN_GRANT.getCode());
            detail1.setDescription("Fork付费作品：" + originalPen.getTitle());
            detail1.setBalanceAfter(balance.getTotalPoints());
            detail1.setCreateTime(LocalDateTime.now());
            pointsDetailMapper.insert(detail1);
            
            // 记录积分明细 - 原作者获得
            UserPointsBalance authorBalance = pointsBalanceMapper.selectByUserId(originalPen.getUserId());
            UserPointsDetail detail2 = new UserPointsDetail();
            detail2.setUserId(originalPen.getUserId());
            detail2.setPointsChange(forkPrice);
            detail2.setPointsType(PointsType.ADMIN_GRANT.getCode());
            detail2.setDescription("作品被Fork获得收益：" + originalPen.getTitle());
            detail2.setBalanceAfter(authorBalance.getTotalPoints());
            detail2.setCreateTime(LocalDateTime.now());
            pointsDetailMapper.insert(detail2);
        }
        
        // 复制作品
        CodePen forkedPen = new CodePen();
        BeanUtil.copyProperties(originalPen, forkedPen);
        forkedPen.setId(null);
        forkedPen.setUserId(userId);
        forkedPen.setTitle(originalPen.getTitle() + " (Fork)");
        forkedPen.setForkedFrom(originalPen.getId());
        forkedPen.setStatus(0); // 设为草稿
        forkedPen.setForkCount(0);
        forkedPen.setLikeCount(0);
        forkedPen.setCommentCount(0);
        forkedPen.setCollectCount(0);
        forkedPen.setViewCount(0);
        forkedPen.setTotalIncome(0);
        forkedPen.setPublishTime(null);
        
        codePenMapper.insert(forkedPen);
        
        // 更新原作品的Fork次数
        codePenMapper.incrementForkCount(originalPen.getId());
        
        // 记录交易
        CodePenForkTransaction transaction = new CodePenForkTransaction();
        transaction.setOriginalPenId(originalPen.getId());
        transaction.setForkedPenId(forkedPen.getId());
        transaction.setOriginalAuthorId(originalPen.getUserId());
        transaction.setForkUserId(userId);
        transaction.setForkPrice(forkPrice);
        transaction.setTransactionType(isFree ? 0 : 1);
        forkTransactionMapper.insert(transaction);
        
        log.info("用户{}Fork作品{}成功，{}Fork，新作品ID：{}", 
                userId, originalPen.getId(), isFree ? "免费" : "付费(" + forkPrice + "积分)", forkedPen.getId());
        
        // 获取作者名称
        String authorName = null;
        if (originalPen.getUserId() != null) {
            SimpleUserInfo userInfo = userInfoApiService.getSimpleUserInfo(originalPen.getUserId());
            if (userInfo != null) {
                authorName = userInfo.getDisplayName();
            }
        }
        
        return ForkResponse.builder()
                .newPenId(forkedPen.getId())
                .originalPenId(originalPen.getId())
                .forkPrice(forkPrice)
                .pointsRemaining(remainingPoints)
                .authorName(authorName)
                .createTime(forkedPen.getCreateTime())
                .build();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean like(Long penId, Long userId) {
        // 检查是否已点赞
        if (codePenLikeMapper.selectByPenIdAndUserId(penId, userId) != null) {
            throw new BusinessException("已经点赞过了");
        }
        
        // 插入点赞记录
        com.xiaou.codepen.domain.CodePenLike like = new com.xiaou.codepen.domain.CodePenLike();
        like.setPenId(penId);
        like.setUserId(userId);
        codePenLikeMapper.insert(like);
        
        // 增加点赞数
        codePenMapper.incrementLikeCount(penId);
        
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unlike(Long penId, Long userId) {
        codePenLikeMapper.delete(penId, userId);
        codePenMapper.decrementLikeCount(penId);
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean collect(Long penId, Long userId, Long folderId) {
        // 检查是否已收藏
        if (codePenCollectMapper.selectByPenIdAndUserId(penId, userId) != null) {
            throw new BusinessException("已经收藏过了");
        }
        
        // 插入收藏记录
        com.xiaou.codepen.domain.CodePenCollect collect = new com.xiaou.codepen.domain.CodePenCollect();
        collect.setPenId(penId);
        collect.setUserId(userId);
        collect.setFolderId(folderId);
        codePenCollectMapper.insert(collect);
        
        // 增加收藏数
        codePenMapper.incrementCollectCount(penId);
        
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean uncollect(Long penId, Long userId) {
        codePenCollectMapper.delete(penId, userId);
        codePenMapper.decrementCollectCount(penId);
        return true;
    }
    
    @Override
    public boolean incrementView(Long penId) {
        return codePenMapper.incrementViewCount(penId) > 0;
    }
    
    @Override
    public List<CodePenDetailResponse> getRecommendList(Integer limit) {
        List<CodePen> list = codePenMapper.selectRecommendList(limit);
        return convertToDetailResponseList(list);
    }
    
    @Override
    public List<CodePenDetailResponse> getHotList(Integer limit) {
        List<CodePen> list = codePenMapper.selectHotList(limit);
        return convertToDetailResponseList(list);
    }
    
    @Override
    public List<CodePen> getTemplateList() {
        return codePenMapper.selectTemplateList();
    }
    
    @Override
    public CodePenStatisticsResponse getStatistics() {
        return CodePenStatisticsResponse.builder()
                .totalPens(codePenMapper.selectTotalCount())
                .totalUsers(codePenMapper.selectUserCount())
                .todayNewPens(codePenMapper.selectTodayCount())
                .totalViews(codePenMapper.selectTotalViewCount())
                .totalLikes(codePenMapper.selectTotalLikeCount())
                .totalComments(codePenMapper.selectTotalCommentCount())
                .totalCollects(codePenMapper.selectTotalCollectCount())
                .totalForks(codePenMapper.selectTotalForkCount())
                .build();
    }
    
    @Override
    public IncomeStatsResponse getIncomeStats(Long userId) {
        List<CodePen> pens = codePenMapper.selectByUserId(userId, 1);
        
        int totalIncome = 0;
        int totalForks = 0;
        List<IncomeStatsResponse.PenIncomeDetail> details = new ArrayList<>();
        
        for (CodePen pen : pens) {
            if (pen.getTotalIncome() != null && pen.getTotalIncome() > 0) {
                totalIncome += pen.getTotalIncome();
                totalForks += pen.getForkCount();
                
                IncomeStatsResponse.PenIncomeDetail detail = IncomeStatsResponse.PenIncomeDetail.builder()
                        .penId(pen.getId())
                        .title(pen.getTitle())
                        .forkCount(pen.getForkCount())
                        .income(pen.getTotalIncome())
                        .forkPrice(pen.getForkPrice())
                        .build();
                details.add(detail);
            }
        }
        
        return IncomeStatsResponse.builder()
                .totalPens(pens.size())
                .totalForks(totalForks)
                .totalIncome(totalIncome)
                .penIncomes(details)
                .build();
    }
    
    // ========== 私有方法 ==========
    
    private void ensureUserPointsBalance(Long userId) {
        UserPointsBalance balance = pointsBalanceMapper.selectByUserId(userId);
        if (balance == null) {
            balance = new UserPointsBalance();
            balance.setUserId(userId);
            balance.setTotalPoints(0);
            balance.setCreateTime(LocalDateTime.now());
            balance.setUpdateTime(LocalDateTime.now());
            pointsBalanceMapper.insert(balance);
        }
    }
    
    private CodePen buildCodePen(CodePenCreateRequest request, Long userId) {
        CodePen codePen = new CodePen();
        codePen.setUserId(userId);
        updateCodePenFields(codePen, request);
        codePen.setForkCount(0);
        codePen.setLikeCount(0);
        codePen.setCommentCount(0);
        codePen.setCollectCount(0);
        codePen.setViewCount(0);
        codePen.setPointsReward(CREATE_REWARD_POINTS);
        codePen.setTotalIncome(0);
        return codePen;
    }
    
    private void updateCodePenFields(CodePen codePen, CodePenCreateRequest request) {
        if (request.getTitle() != null) codePen.setTitle(request.getTitle());
        if (request.getDescription() != null) codePen.setDescription(request.getDescription());
        if (request.getHtmlCode() != null) codePen.setHtmlCode(request.getHtmlCode());
        if (request.getCssCode() != null) codePen.setCssCode(request.getCssCode());
        if (request.getJsCode() != null) codePen.setJsCode(request.getJsCode());
        if (request.getPreviewImage() != null) codePen.setPreviewImage(request.getPreviewImage());
        if (request.getExternalCss() != null) codePen.setExternalCss(JSONUtil.toJsonStr(request.getExternalCss()));
        if (request.getExternalJs() != null) codePen.setExternalJs(JSONUtil.toJsonStr(request.getExternalJs()));
        if (request.getTags() != null) codePen.setTags(JSONUtil.toJsonStr(request.getTags()));
        if (request.getCategory() != null) codePen.setCategory(request.getCategory());
        if (request.getIsPublic() != null) codePen.setIsPublic(request.getIsPublic());
        if (request.getIsFree() != null) codePen.setIsFree(request.getIsFree());
        if (request.getForkPrice() != null) codePen.setForkPrice(request.getForkPrice());
        if (request.getStatus() != null) codePen.setStatus(request.getStatus());
    }

    private List<CodePenDetailResponse> buildDetailedResponses(List<CodePen> codePens, Long currentUserId) {
        if (codePens == null || codePens.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> penIds = codePens.stream()
                .map(CodePen::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, SimpleUserInfo> userInfoMap = getUserInfoMap(codePens);
        Set<Long> likedPenIds = getLikedPenIds(currentUserId, penIds);
        Set<Long> collectedPenIds = getCollectedPenIds(currentUserId, penIds);
        Set<Long> purchasedOriginalPenIds = getPurchasedOriginalPenIds(currentUserId, codePens);

        List<CodePenDetailResponse> responses = new ArrayList<>();
        for (CodePen codePen : codePens) {
            CodePenDetailResponse response = convertToDetailResponseWithUserInfo(codePen, userInfoMap);
            boolean canViewCode = canViewCode(codePen, currentUserId, purchasedOriginalPenIds);

            response.setCanViewCode(canViewCode);
            if (!canViewCode) {
                response.setHtmlCode(null);
                response.setCssCode(null);
                response.setJsCode(null);
                response.setCodeViewMessage("付费作品，Fork后可查看源码");
            }

            boolean isOwner = codePen.getUserId() != null && codePen.getUserId().equals(currentUserId);
            response.setCanEdit(isOwner);
            response.setCanDelete(isOwner);
            response.setIsLiked(currentUserId != null && likedPenIds.contains(codePen.getId()));
            response.setIsCollected(currentUserId != null && collectedPenIds.contains(codePen.getId()));
            response.setShareUrl(codePenProperties.getShareBaseUrl() + codePen.getId());
            responses.add(response);
        }
        return responses;
    }
    
    private CodePenDetailResponse convertToDetailResponse(CodePen codePen) {
        CodePenDetailResponse response = new CodePenDetailResponse();
        BeanUtil.copyProperties(codePen, response);
        
        // 获取用户信息
        if (codePen.getUserId() != null) {
            SimpleUserInfo userInfo = userInfoApiService.getSimpleUserInfo(codePen.getUserId());
            if (userInfo != null) {
                response.setUserNickname(userInfo.getDisplayName());
                response.setUserAvatar(userInfo.getAvatar());
            }
        }
        
        // 转换JSON字段
        if (codePen.getExternalCss() != null) {
            response.setExternalCss(JSONUtil.toList(codePen.getExternalCss(), String.class));
        }
        if (codePen.getExternalJs() != null) {
            response.setExternalJs(JSONUtil.toList(codePen.getExternalJs(), String.class));
        }
        if (codePen.getTags() != null) {
            response.setTags(JSONUtil.toList(codePen.getTags(), String.class));
        }
        
        response.setIsFree(codePen.getIsFree() == 1);
        
        return response;
    }

    private Map<Long, SimpleUserInfo> getUserInfoMap(List<CodePen> codePens) {
        List<Long> userIds = codePens.stream()
                .map(CodePen::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        return userInfoApiService.getSimpleUserInfoBatch(userIds);
    }

    private Set<Long> getLikedPenIds(Long currentUserId, List<Long> penIds) {
        if (currentUserId == null || penIds == null || penIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> likedPenIds = codePenLikeMapper.selectLikedPenIdsByUserId(currentUserId, penIds);
        return likedPenIds != null ? likedPenIds : Collections.emptySet();
    }

    private Set<Long> getCollectedPenIds(Long currentUserId, List<Long> penIds) {
        if (currentUserId == null || penIds == null || penIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> collectedPenIds = codePenCollectMapper.selectCollectedPenIdsByUserId(currentUserId, penIds);
        return collectedPenIds != null ? collectedPenIds : Collections.emptySet();
    }

    private Set<Long> getPurchasedOriginalPenIds(Long currentUserId, List<CodePen> codePens) {
        if (currentUserId == null || codePens == null || codePens.isEmpty()) {
            return Collections.emptySet();
        }

        List<Long> paidOriginalPenIds = codePens.stream()
                .filter(codePen -> codePen.getId() != null)
                .filter(codePen -> codePen.getIsFree() != null && codePen.getIsFree() == 0)
                .filter(codePen -> codePen.getUserId() == null || !codePen.getUserId().equals(currentUserId))
                .map(CodePen::getId)
                .distinct()
                .collect(Collectors.toList());
        if (paidOriginalPenIds.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Long> purchasedOriginalPenIds = forkTransactionMapper.selectPurchasedOriginalPenIdsByForkUserId(currentUserId, paidOriginalPenIds);
        return purchasedOriginalPenIds != null ? purchasedOriginalPenIds : Collections.emptySet();
    }

    private boolean canViewCode(CodePen codePen, Long currentUserId, Set<Long> purchasedOriginalPenIds) {
        if (codePen.getIsFree() == null || codePen.getIsFree() == 1) {
            return true;
        }
        if (codePen.getUserId() != null && codePen.getUserId().equals(currentUserId)) {
            return true;
        }
        return currentUserId != null && purchasedOriginalPenIds.contains(codePen.getId());
    }
    
    private List<CodePenDetailResponse> convertToDetailResponseList(List<CodePen> list) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 批量查询用户信息，避免N+1问题
        List<Long> userIds = list.stream()
                .map(CodePen::getUserId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, SimpleUserInfo> userInfoMap = userInfoApiService.getSimpleUserInfoBatch(userIds);
        
        List<CodePenDetailResponse> responses = new ArrayList<>();
        for (CodePen codePen : list) {
            responses.add(convertToDetailResponseWithUserInfo(codePen, userInfoMap));
        }
        return responses;
    }
    
    /**
     * 转换为详情响应（使用已查询的用户信息Map）
     */
    private CodePenDetailResponse convertToDetailResponseWithUserInfo(CodePen codePen, Map<Long, SimpleUserInfo> userInfoMap) {
        CodePenDetailResponse response = new CodePenDetailResponse();
        BeanUtil.copyProperties(codePen, response);
        
        // 获取用户信息
        if (codePen.getUserId() != null) {
            SimpleUserInfo userInfo = userInfoMap.get(codePen.getUserId());
            if (userInfo != null) {
                response.setUserNickname(userInfo.getDisplayName());
                response.setUserAvatar(userInfo.getAvatar());
            }
        }
        
        // 转换JSON字段
        if (codePen.getExternalCss() != null) {
            response.setExternalCss(JSONUtil.toList(codePen.getExternalCss(), String.class));
        }
        if (codePen.getExternalJs() != null) {
            response.setExternalJs(JSONUtil.toList(codePen.getExternalJs(), String.class));
        }
        if (codePen.getTags() != null) {
            response.setTags(JSONUtil.toList(codePen.getTags(), String.class));
        }
        
        response.setIsFree(codePen.getIsFree() == 1);
        
        return response;
    }
}

