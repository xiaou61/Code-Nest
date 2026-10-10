/**
 * 账号与认证模块。
 *
 * <p><b>职责</b>：账号实体（`users`）、注册与登录用例、图形验证码与邮箱验证码、
 * refresh 令牌的状态与轮换。
 *
 * <p><b>对外契约</b>只在包根（见 {@link io.github.xiaou61.account.AccountApi}）；
 * `internal` 子包是内部实现，其它模块不得访问——由 Spring Modulith 在构建期拦截。
 *
 * <p><b>依赖方向是单向的</b>：本模块依赖 `paideia-security` 的 `AuthPort` 来签发令牌，
 * 而 `security` 不认识账号表。这样把"身份怎么来的"与"令牌怎么签发"分开，
 * 将来加第三方登录或改令牌机制都不会互相牵扯。
 *
 * <p><b>凭据纪律</b>：密码、refresh 令牌、邮箱验证码、图形验证码答案都只存哈希；
 * 任何明文不得进入日志、异常消息与响应体。
 */
package io.github.xiaou61.account;
