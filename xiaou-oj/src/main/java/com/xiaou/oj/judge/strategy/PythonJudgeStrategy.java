package com.xiaou.oj.judge.strategy;

import com.xiaou.oj.enums.JudgeLanguage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Python判题策略
 *
 * @author xiaou
 */
@Component
public class PythonJudgeStrategy implements JudgeStrategy {

    @Override
    public JudgeLanguage getLanguage() {
        return JudgeLanguage.PYTHON;
    }

    @Override
    public String getSourceFileName() {
        return "main.py";
    }

    @Override
    public List<String> getCompileArgs() {
        return null; // Python不需要编译
    }

    @Override
    public List<String> getRunArgs() {
        return List.of("/usr/bin/python3", "main.py");
    }
}
