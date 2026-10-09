package io.github.xiaou61.persistence;

/** 测试用示例实体。字段刻意只保留分页与归属断言需要的最小集合。 */
public record ExampleItem(Long id, String ownerId, String title) {
}
