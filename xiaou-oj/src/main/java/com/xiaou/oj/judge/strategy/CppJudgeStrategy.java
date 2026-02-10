package com.xiaou.oj.judge.strategy;

import com.xiaou.oj.enums.JudgeLanguage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * C++判题策略
 *
 * @author xiaou
 */
@Component
public class CppJudgeStrategy implements JudgeStrategy {

    @Override
    public JudgeLanguage getLanguage() {
        return JudgeLanguage.CPP;
    }

    @Override
    public String getSourceFileName() {
        return "main.cpp";
    }

    @Override
    public List<String> getCompileArgs() {
        return List.of("/usr/bin/g++", "-o", "main", "main.cpp", "-O2");
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
