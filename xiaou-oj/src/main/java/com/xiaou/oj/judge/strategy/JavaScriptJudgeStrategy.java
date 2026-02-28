package com.xiaou.oj.judge.strategy;

import com.xiaou.oj.enums.JudgeLanguage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * JavaScript (Node.js) 判题策略
 *
 * @author xiaou
 */
@Component
public class JavaScriptJudgeStrategy implements JudgeStrategy {

    @Override
    public JudgeLanguage getLanguage() {
        return JudgeLanguage.JAVASCRIPT;
    }

    @Override
    public String getSourceFileName() {
        return "main.js";
    }

    @Override
    public List<String> getCompileArgs() {
        return null; // JavaScript 不需要编译
    }

    @Override
    public List<String> getRunArgs() {
        return List.of("/usr/bin/node", "main.js");
    }
}
