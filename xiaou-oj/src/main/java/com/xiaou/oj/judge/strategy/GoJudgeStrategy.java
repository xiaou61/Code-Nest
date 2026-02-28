package com.xiaou.oj.judge.strategy;

import com.xiaou.oj.enums.JudgeLanguage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Go语言判题策略
 *
 * @author xiaou
 */
@Component
public class GoJudgeStrategy implements JudgeStrategy {

    @Override
    public JudgeLanguage getLanguage() {
        return JudgeLanguage.GO;
    }

    @Override
    public String getSourceFileName() {
        return "main.go";
    }

    @Override
    public List<String> getCompileArgs() {
        return List.of("/usr/bin/go", "build", "-o", "main", "main.go");
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
