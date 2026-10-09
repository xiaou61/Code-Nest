/**
 * 通用契约模块。
 *
 * <p>本模块是**普通（闭包）模块**，不是开放模块：其他模块只能使用本包根下的类型，
 * 子包（例如 {@code internal}）视为内部实现，被外部引用时 Spring Modulith 会让构建失败。
 *
 * <p>这里刻意不用 {@code @ApplicationModule(type = OPEN)}。开放模块会放行外部对本模块内部
 * 的访问，而当前对外契约全部位于本包根，开放只会白白丢掉这条边界检查。
 */
package io.github.xiaou61.platform;
