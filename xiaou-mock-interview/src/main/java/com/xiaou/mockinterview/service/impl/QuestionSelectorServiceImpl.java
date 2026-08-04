package com.xiaou.mockinterview.service.impl;

import cn.hutool.core.util.StrUtil;
import com.xiaou.ai.service.AiInterviewService;
import com.xiaou.interview.domain.InterviewQuestion;
import com.xiaou.interview.mapper.InterviewQuestionMapper;
import com.xiaou.mockinterview.domain.MockInterviewDirection;
import com.xiaou.mockinterview.enums.InterviewLevelEnum;
import com.xiaou.mockinterview.mapper.MockInterviewDirectionMapper;
import com.xiaou.mockinterview.mapper.MockInterviewQAMapper;
import com.xiaou.mockinterview.service.QuestionSelectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 题目选择服务实现
 *
 * @author xiaou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionSelectorServiceImpl implements QuestionSelectorService {

    private final InterviewQuestionMapper questionMapper;
    private final MockInterviewDirectionMapper directionMapper;
    private final MockInterviewQAMapper qaMapper;
    private final AiInterviewService aiInterviewService;

    @Override
    public List<InterviewQuestion> selectQuestions(String direction, Integer level, Integer questionCount, Long userId) {
        // 1. 获取方向关联的分类
        List<Long> categoryIds = getCategoryIds(direction);
        if (categoryIds.isEmpty()) {
            log.warn("方向{}没有关联的分类，将从所有题目中选择", direction);
        }

        // 2. 获取用户历史已答题目（避免重复）
        Set<Long> answeredIds = new HashSet<>();
        if (userId != null) {
            List<Long> answered = qaMapper.selectAnsweredQuestionIdsByUserId(userId);
            if (answered != null) {
                answeredIds.addAll(answered);
            }
        }

        // 3. 获取候选题目
        List<InterviewQuestion> allQuestions;
        if (categoryIds.isEmpty()) {
            // 如果没有分类，获取所有题目
            allQuestions = questionMapper.selectByQuestionSetIds(null);
        } else {
            // 根据分类获取题目
            allQuestions = questionMapper.selectByQuestionSetIds(categoryIds);
        }

        // 4. 过滤已答题目
        List<InterviewQuestion> candidateQuestions = allQuestions.stream()
                .filter(q -> !answeredIds.contains(q.getId()))
                .collect(Collectors.toList());

        // 如果过滤后题目不足，则使用全部题目
        if (candidateQuestions.size() < questionCount) {
            log.info("过滤后题目不足，使用全部题目（包含已答过的）");
            candidateQuestions = new ArrayList<>(allQuestions);
        }

        // 5. 随机选择指定数量的题目
        Collections.shuffle(candidateQuestions, new Random());
        int selectCount = Math.min(questionCount, candidateQuestions.size());

        return candidateQuestions.subList(0, selectCount);
    }

    @Override
    public List<Long> getCategoryIds(String direction) {
        MockInterviewDirection directionConfig = directionMapper.selectByCode(direction);
        if (directionConfig == null || StrUtil.isBlank(directionConfig.getCategoryIds())) {
            return Collections.emptyList();
        }

        try {
            return Arrays.stream(directionConfig.getCategoryIds().split(","))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("解析分类ID失败: {}", directionConfig.getCategoryIds(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<GeneratedQuestion> generateQuestionsByAI(
            String direction,
            Integer level,
            Integer questionCount,
            String specializedTopic
    ) {
        InterviewLevelEnum levelEnum = InterviewLevelEnum.getByCode(level);
        String levelName = levelEnum != null ? levelEnum.getName() : "中级";

        try {
            // 调用AI服务
            List<com.xiaou.ai.dto.interview.GeneratedQuestion> aiQuestions = 
                    aiInterviewService.generateQuestions(direction, levelName, questionCount, specializedTopic);

            if (aiQuestions != null && !aiQuestions.isEmpty()) {
                // 转换为业务DTO
                return aiQuestions.stream().map(q -> {
                    GeneratedQuestion gq = new GeneratedQuestion();
                    gq.setQuestionContent(q.getQuestion());
                    gq.setReferenceAnswer(q.getAnswer());
                    gq.setKnowledgePoints(q.getKnowledgePoints());
                    return gq;
                }).collect(Collectors.toList());
            }

            // AI服务返回空，使用本地生成
            log.warn("AI服务返回空结果，使用本地生成题目");
            return generateLocalQuestions(direction, levelName, questionCount);

        } catch (Exception e) {
            log.error("AI出题失败，使用本地生成", e);
            return generateLocalQuestions(direction, levelName, questionCount);
        }
    }

    /**
     * 本地生成题目（当AI不可用时的降级方案）
     */
    private List<GeneratedQuestion> generateLocalQuestions(String direction, String level, int count) {
        List<GeneratedQuestion> questions = new ArrayList<>();

        // 根据方向生成预设题目
        Map<String, List<String[]>> questionBank = getLocalQuestionBank();
        List<String[]> directionQuestions = questionBank.getOrDefault(direction, questionBank.get("java"));

        // 随机选择题目
        Collections.shuffle(directionQuestions, new Random());
        int selectCount = Math.min(count, directionQuestions.size());

        for (int i = 0; i < selectCount; i++) {
            String[] qa = directionQuestions.get(i);
            GeneratedQuestion q = new GeneratedQuestion();
            q.setQuestionContent(qa[0]);
            q.setReferenceAnswer(qa[1]);
            q.setKnowledgePoints(qa.length > 2 ? qa[2] : direction);
            questions.add(q);
        }

        return questions;
    }

    @Override
    public List<InterviewQuestion> selectQuestionsFromSets(List<Long> questionSetIds, Integer questionCount, Long userId) {
        if (questionSetIds == null || questionSetIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 获取用户历史已答题目（避免重复）
        Set<Long> answeredIds = new HashSet<>();
        if (userId != null) {
            List<Long> answered = qaMapper.selectAnsweredQuestionIdsByUserId(userId);
            if (answered != null) {
                answeredIds.addAll(answered);
            }
        }

        // 2. 从指定题库中获取题目
        List<InterviewQuestion> allQuestions = questionMapper.selectByQuestionSetIds(questionSetIds);

        // 3. 过滤已答题目
        List<InterviewQuestion> candidateQuestions = allQuestions.stream()
                .filter(q -> !answeredIds.contains(q.getId()))
                .collect(Collectors.toList());

        // 如果过滤后题目不足，则使用全部题目
        if (candidateQuestions.size() < questionCount) {
            log.info("过滤后题目不足，使用全部题目（包含已答过的）");
            candidateQuestions = new ArrayList<>(allQuestions);
        }

        // 4. 随机选择指定数量的题目
        Collections.shuffle(candidateQuestions, new Random());
        int selectCount = Math.min(questionCount, candidateQuestions.size());

        return candidateQuestions.subList(0, selectCount);
    }

    /**
     * 本地题库（降级方案）
     */
    private Map<String, List<String[]>> getLocalQuestionBank() {
        Map<String, List<String[]>> bank = new HashMap<>();

        // Java题目
        bank.put("java", Arrays.asList(
            new String[]{"请介绍一下HashMap的底层实现原理", "HashMap基于数组+链表/红黑树实现。JDK1.8中当链表长度超过8且数组长度>=64时转为红黑树...", "HashMap,数据结构"},
            new String[]{"什么是JVM垃圾回收机制？有哪些垃圾回收算法？", "JVM垃圾回收用于自动管理内存。常见算法有标记-清除、复制、标记-整理、分代收集等...", "JVM,GC"},
            new String[]{"请解释Spring IOC和AOP的原理", "IOC控制反转，将对象创建权交给容器。AOP面向切面编程，基于动态代理实现...", "Spring,IOC,AOP"},
            new String[]{"synchronized和ReentrantLock有什么区别？", "synchronized是JVM层面的锁，ReentrantLock是API层面的锁。ReentrantLock更灵活，支持公平锁、可中断、超时等...", "并发,锁"},
            new String[]{"请介绍MySQL索引的原理和优化", "MySQL索引基于B+树实现。优化包括：选择合适字段建索引、遵循最左前缀原则、避免索引失效等...", "MySQL,索引"},
            new String[]{"Redis有哪些数据结构？各有什么使用场景？", "String、List、Hash、Set、ZSet。String用于缓存，List用于消息队列，Hash用于对象存储...", "Redis"},
            new String[]{"什么是分布式事务？有哪些解决方案？", "分布式事务保证多个服务操作的原子性。方案有2PC、3PC、TCC、Saga、消息最终一致性等...", "分布式"},
            new String[]{"请介绍微服务架构的优缺点", "优点：独立部署、技术异构、可扩展。缺点：复杂度高、分布式事务、服务治理等挑战...", "微服务"}
        ));

        // 前端题目
        bank.put("frontend", Arrays.asList(
            new String[]{"请解释JavaScript的事件循环机制", "JS是单线程的，通过事件循环处理异步。宏任务(setTimeout)和微任务(Promise)的执行顺序...", "JavaScript,事件循环"},
            new String[]{"Vue的响应式原理是什么？", "Vue2使用Object.defineProperty，Vue3使用Proxy实现数据劫持。结合发布订阅模式实现响应式...", "Vue,响应式"},
            new String[]{"请介绍React的虚拟DOM和Diff算法", "虚拟DOM是JS对象描述真实DOM。Diff算法通过同层比较、key优化等策略减少DOM操作...", "React,虚拟DOM"},
            new String[]{"什么是跨域？如何解决？", "同源策略限制导致跨域。解决方案：CORS、JSONP、代理服务器、WebSocket等...", "跨域,网络"},
            new String[]{"请介绍前端性能优化的方法", "资源压缩、懒加载、缓存策略、CDN、减少重绘回流、代码分割、SSR等...", "性能优化"}
        ));

        // Python题目
        bank.put("python", Arrays.asList(
            new String[]{"Python的GIL是什么？有什么影响？", "GIL全局解释器锁，同一时间只有一个线程执行Python字节码。影响多线程CPU密集任务性能...", "Python,GIL"},
            new String[]{"请解释Python的装饰器原理", "装饰器本质是闭包，接受函数作为参数，返回增强后的函数。用于AOP、权限校验、日志等...", "Python,装饰器"},
            new String[]{"Django和Flask有什么区别？", "Django是全功能框架，自带ORM、Admin等。Flask是微框架，轻量灵活，需要自己组装组件...", "Django,Flask"}
        ));

        return bank;
    }
}
