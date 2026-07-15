package com.xiaou.points.cache;

import com.xiaou.common.cache.RedisValueStore;
import com.xiaou.points.domain.LotteryPrizeConfig;
import com.xiaou.points.mapper.LotteryPrizeConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.time.Duration;

/**
 * 抽奖缓存预热器
 * 系统启动时预热奖池配置到Redis
 * 
 * @author xiaou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LotteryCacheWarmer implements CommandLineRunner {
    
    private final LotteryPrizeConfigMapper prizeConfigMapper;
    private final RedisValueStore redisValueStore;
    
    /**
     * 缓存Key前缀
     */
    private static final String PRIZE_CONFIG_KEY = "lottery:prize:config:";
    private static final String PRIZE_STOCK_KEY = "lottery:prize:stock:";
    private static final String PRIZE_LIST_KEY = "lottery:prize:list";
    
    @Override
    public void run(String... args) {
        log.info("开始预热抽奖缓存...");
        
        try {
            // 预热奖品配置
            warmUpPrizeConfig();
            
            // 预热奖品库存
            warmUpPrizeStock();
            
            log.info("抽奖缓存预热完成");
        } catch (Exception e) {
            log.error("抽奖缓存预热失败", e);
        }
    }
    
    /**
     * 预热奖品配置
     */
    private void warmUpPrizeConfig() {
        List<LotteryPrizeConfig> prizes = prizeConfigMapper.selectAllActive();
        
        if (prizes.isEmpty()) {
            log.warn("没有启用的奖品，跳过预热");
            return;
        }
        
        for (LotteryPrizeConfig prize : prizes) {
            // 缓存单个奖品配置，1小时 = 3600秒
            String prizeKey = PRIZE_CONFIG_KEY + prize.getId();
            redisValueStore.put(prizeKey, prize, Duration.ofHours(1));
            
            log.debug("预热奖品配置：{} - {}", prize.getId(), prize.getPrizeName());
        }
        
        // 缓存奖品列表，1小时 = 3600秒
        redisValueStore.put(PRIZE_LIST_KEY, prizes, Duration.ofHours(1));
        
        log.info("预热奖品配置完成，奖品数量：{}", prizes.size());
    }
    
    /**
     * 预热奖品库存
     */
    private void warmUpPrizeStock() {
        List<LotteryPrizeConfig> prizes = prizeConfigMapper.selectAllActive();
        
        int count = 0;
        for (LotteryPrizeConfig prize : prizes) {
            // 跳过无限制库存的奖品
            if (prize.getTotalStock() == null || prize.getTotalStock() < 0) {
                continue;
            }
            
            // 缓存奖品库存
            String stockKey = PRIZE_STOCK_KEY + prize.getId();
            Integer currentStock = prize.getCurrentStock() != null ? prize.getCurrentStock() : prize.getTotalStock();
            redisValueStore.put(stockKey, currentStock);
            
            count++;
            log.debug("预热奖品库存：{} - {} (库存：{})", prize.getId(), prize.getPrizeName(), currentStock);
        }
        
        log.info("预热奖品库存完成，有库存限制的奖品数量：{}", count);
    }
    
    /**
     * 手动刷新缓存
     */
    public void refreshCache() {
        log.info("手动刷新抽奖缓存");
        warmUpPrizeConfig();
        warmUpPrizeStock();
    }
}

