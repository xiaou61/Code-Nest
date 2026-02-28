package com.xiaou.oj.judge.strategy;

import com.xiaou.oj.enums.JudgeLanguage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Java判题策略
 *
 * @author xiaou
 */
@Component
public class JavaJudgeStrategy implements JudgeStrategy {

    @Override
    public JudgeLanguage getLanguage() {
        return JudgeLanguage.JAVA;
    }

    @Override
    public String getSourceFileName() {
        return "Main.java";
    }

    @Override
    public List<String> getCompileArgs() {
        return List.of("/usr/bin/javac", "Main.java");
    }

    @Override
    public List<String> getCompiledFileNames() {
        return List.of("Main.class");
    }

    @Override
    public List<String> getRunArgs() {
        return List.of("/usr/bin/java", "Main");
    }
}
