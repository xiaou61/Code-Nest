package io.github.xiaou61.account.internal;

import io.github.xiaou61.account.AccountApi;
import io.github.xiaou61.account.internal.user.UserMapper;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * {@link AccountApi} 的实现。放在 internal 里，其它模块只能看到包根的接口。
 *
 * <p>参数是字符串形式的 id（与令牌的 `sub` 一致），所以要处理非法数字的情况——
 * 调用方传进来的可能是任何东西，不能让它变成 500。
 */
@Component
class AccountApiImplementation implements AccountApi {

    private final UserMapper userMapper;

    AccountApiImplementation(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public Optional<String> findRoleById(String userId) {
        try {
            return userMapper.findRoleById(Long.valueOf(userId));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
