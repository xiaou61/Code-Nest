package com.xiaou.points.service;

/**
 * 抽奖库存管理服务接口
 * 
 * @author xiaou
 */
public interface LotteryStockService {
    
    /**
     * 扣减奖品库存
     * 
     * @param prizeId 奖品ID
     * @return true-扣减成功，false-库存不足
     */
    boolean deductStock(Long prizeId);
    
    /**
     * 回滚奖品库存（异常时使用）
     * 
     * @param prizeId 奖品ID
     */
    void rollbackStock(Long prizeId);
    
    /**
     * 获取奖品库存
     * 
     * @param prizeId 奖品ID
     * @return 当前库存数量
     */
    Integer getStock(Long prizeId);

    /**
     * 清理奖品库存缓存，下次读取时从数据库重新初始化
     *
     * @param prizeId 奖品ID
     */
    void evictStockCache(Long prizeId);
    
    /**
     * 同步Redis库存到数据库
     */
    void syncStockToDatabase();
}

