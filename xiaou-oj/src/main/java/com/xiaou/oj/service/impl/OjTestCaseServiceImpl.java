package com.xiaou.oj.service.impl;

import com.xiaou.oj.domain.OjTestCase;
import com.xiaou.oj.mapper.OjTestCaseMapper;
import com.xiaou.oj.service.OjTestCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * OJ测试用例服务实现
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class OjTestCaseServiceImpl implements OjTestCaseService {

    private final OjTestCaseMapper testCaseMapper;

    @Override
    public Long addTestCase(OjTestCase testCase) {
        if (testCase.getSortOrder() == null) {
            int count = testCaseMapper.countByProblemId(testCase.getProblemId());
            testCase.setSortOrder(count + 1);
        }
        if (testCase.getIsSample() == null) {
            testCase.setIsSample(false);
        }
        testCaseMapper.insert(testCase);
        return testCase.getId();
    }

    @Override
    public void updateTestCase(Long id, OjTestCase testCase) {
        testCase.setId(id);
        testCaseMapper.updateById(testCase);
    }

    @Override
    public void deleteTestCase(Long id) {
        testCaseMapper.deleteById(id);
    }

    @Override
    public List<OjTestCase> getTestCasesByProblemId(Long problemId) {
        return testCaseMapper.selectByProblemId(problemId);
    }
}
