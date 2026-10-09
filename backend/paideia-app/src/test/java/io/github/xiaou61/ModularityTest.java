package io.github.xiaou61;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * 模块边界的构建期门槛：出现循环依赖，或跨模块访问对方内部实现包时，构建失败。
 *
 * <p>模块之间的依赖方向另由 Maven 保证——未在 pom.xml 声明依赖的模块连编译都过不去。
 * 这里补的是编译器管不到的那一半：Maven artifact 会导出全部包，所以"不该被外部使用的
 * 内部包"只能靠本校验拦截。
 */
class ModularityTest {

    private static final ApplicationModules MODULES = ApplicationModules.of(PaideiaApplication.class);

    @Test
    @DisplayName("模块结构满足 Spring Modulith 的边界约束")
    void verifiesModuleStructure() {
        MODULES.verify();
    }
}
