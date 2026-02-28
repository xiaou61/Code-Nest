package com.xiaou.oj.judge.strategy;

import com.xiaou.oj.enums.JudgeLanguage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * C语言判题策略
 *
 * @author xiaou
 */
@Component
public class CJudgeStrategy implements JudgeStrategy {

    @Override
    public JudgeLanguage getLanguage() {
        return JudgeLanguage.C;
    }

    @Override
    public String getSourceFileName() {
        return "main.c";
    }

    @Override
    public List<String> getCompileArgs() {
        return List.of("/usr/bin/gcc", "-o", "main", "main.c", "-O2");
    }

    @Override
    public List<String> getCompiledFileNames() {
        return List.of("main");
    }

    @Override
    public List<String> getRunArgs() {
        return List.of("main");
    }
}
