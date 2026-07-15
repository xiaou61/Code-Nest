package com.xiaou.moment.task;

import com.xiaou.moment.domain.Moment;
import com.xiaou.moment.mapper.MomentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.time.Duration;

/**
 * 热门动态计算定时任务
 * 定时计算热门动态并缓存到Redis Sorted Set
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotMomentCalculateTask {
    
    private final RedissonClient redissonClient;
    private final MomentMapper momentMapper;
    
    /**
     * Redis Key
     */
    private static final String HOT_MOMENTS_KEY = "moment:hot:list";
    
    /**
     * 缓存过期时间（10分钟）
     */
    private static final long CACHE_EXPIRE_SECONDS = 600;
    
    /**
     * 每10分钟执行一次，计算热门动态
     * cron表达式：秒 分 时 日 月 周
     * 0 星号/10 星号 星号 星号 问号 表示每10分钟执行
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void calculateHotMoments() {
        log.info("定时任务：开始计算热门动态");
        try {
            // 1. 从数据库查询最近24小时的热门动态（按热度排序，取前50条）
            List<Moment> hotMoments = momentMapper.selectHotMoments(50);
            
            if (hotMoments.isEmpty()) {
                log.info("定时任务：暂无热门动态");
                return;
            }
            
            // 2. 获取Redisson的Sorted Set
            RScoredSortedSet<Long> hotMomentsSet = redissonClient.getScoredSortedSet(HOT_MOMENTS_KEY);
            
            // 3. 清空旧数据
            hotMomentsSet.clear();
            
            // 4. 计算热度分数并存入Sorted Set
            for (Moment moment : hotMoments) {
                // 热度分数 = 点赞数*2 + 评论数*3 + 浏览数*0.1
                double score = calculateHotScore(moment);
                hotMomentsSet.add(score, moment.getId());
            }
            
            // 5. 设置过期时间（10分钟）
            hotMomentsSet.expire(Duration.ofSeconds(CACHE_EXPIRE_SECONDS));
            
            log.info("定时任务：热门动态计算完成，共计算 {} 条热门动态", hotMoments.size());
        } catch (Exception e) {
            log.error("定时任务：热门动态计算失败", e);
        }
    }
    
    /**
     * 计算热度分数
     * 公式：点赞数*2 + 评论数*3 + 浏览数*0.1
     */
    private double calculateHotScore(Moment moment) {
        int likeCount = moment.getLikeCount() != null ? moment.getLikeCount() : 0;
        int commentCount = moment.getCommentCount() != null ? moment.getCommentCount() : 0;
        int viewCount = moment.getViewCount() != null ? moment.getViewCount() : 0;
        
        return likeCount * 2.0 + commentCount * 3.0 + viewCount * 0.1;
    }
}

