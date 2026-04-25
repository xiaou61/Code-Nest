package com.xiaou.ai.structured;

import java.util.function.Consumer;

/**
 * 结构化输出数组契约。
 *
 * @author xiaou
 */
public interface AiStructuredArrayContract {

    AiStructuredArrayContract requireObjectItems(Consumer<AiStructuredObjectContract> itemValidator);

    AiStructuredArrayContract requireStringItems();
}
