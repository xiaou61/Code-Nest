package com.xiaou.oj.service;

import com.xiaou.oj.domain.OjTestCase;

import java.util.List;

/**
 * OJ测试用例服务接口
 *
 * @author xiaou
 */
public interface OjTestCaseService {

    Long addTestCase(OjTestCase testCase);

    void updateTestCase(Long id, OjTestCase testCase);

    void deleteTestCase(Long id);

    List<OjTestCase> getTestCasesByProblemId(Long problemId);
}
