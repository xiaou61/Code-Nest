package com.xiaou.ai.regression;

import java.util.List;

/**
 * AI 黄金样例回归服务。
 *
 * @author xiaou
 */
public interface AiRegressionService {

    /**
     * 获取回归用例目录。
     */
    List<AiRegressionCaseCatalogItem> listCases();

    /**
     * 执行回归用例。
     */
    AiRegressionRunSummary run(String scenario, String caseId);
}
