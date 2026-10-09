/**
 * 认证授权基础设施。
 *
 * <p>对外只暴露 {@link io.github.xiaou61.security.AuthPort}：业务代码依赖这个端口，
 * 不依赖 JWT 细节。将来换成 OIDC 等机制时替换端口实现与解码器配置即可，业务代码不动。
 *
 * <p>本期实现是自签 JWT（HS256）。签名密钥只从配置读取，**不设默认值**：
 * 缺密钥就在启动时失败并给出明确提示，避免用一个默认密钥悄悄跑起来。
 */
package io.github.xiaou61.security;
