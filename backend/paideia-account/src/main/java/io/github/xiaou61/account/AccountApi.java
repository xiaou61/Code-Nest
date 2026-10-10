package io.github.xiaou61.account;

import java.util.Optional;

/**
 * 账号模块的对外契约。
 *
 * <p>其它业务模块（将来的 learning、content 等）要按 `userId` 关联数据时，
 * 依赖的是这里的方法，而不是账号表或 `internal` 里的实现。本项只暴露确实被需要的一件事：
 * 按 id 读角色。等第一个真实消费者出现时再加方法，不提前铺开。
 */
public interface AccountApi {

    /**
     * 读某个账号的角色。
     *
     * @param userId 账号 id（字符串形式，与令牌里的 `sub` 一致）
     * @return 角色名（`admin` / `learner`）；账号不存在时返回空
     */
    Optional<String> findRoleById(String userId);
}
