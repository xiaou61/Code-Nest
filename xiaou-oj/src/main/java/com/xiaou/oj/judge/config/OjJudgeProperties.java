package com.xiaou.oj.judge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OJ判题配置
 *
 * @author xiaou
 */
@Data
@Component
@ConfigurationProperties(prefix = "oj.judge")
public class OjJudgeProperties {

    /**
     * go-judge 服务地址
     */
    private String goJudgeUrl = "http://localhost:5050";

    /**
     * 最大编译时间 (ms)
     */
    private Long maxCompileTime = 10000L;

    /**
     * 默认运行时间限制 (ms)
     */
    private Long defaultTimeLimit = 2000L;

    /**
     * 默认内存限制 (MB)
     */
    private Long defaultMemoryLimit = 256L;
}
