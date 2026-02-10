package com.xiaou.oj.judge.strategy;

import com.xiaou.oj.enums.JudgeLanguage;

import java.util.List;

/**
 * 判题策略接口
 * 不同编程语言有不同的编译和运行命令
 *
 * @author xiaou
 */
public interface JudgeStrategy {

    /**
     * 支持的语言
     */
    JudgeLanguage getLanguage();

    /**
     * 获取源代码文件名
     */
    String getSourceFileName();

    /**
     * 获取编译命令 (不需要编译的语言返回null)
     */
    List<String> getCompileArgs();

    /**
     * 获取编译产出的文件名列表 (用于 copyOutCached)
     * 不需要编译的语言返回 null
     */
    default List<String> getCompiledFileNames() {
        return null;
    }

    /**
     * 获取运行命令
     */
    List<String> getRunArgs();
}
