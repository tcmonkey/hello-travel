package com.hellotravel.domain.auth.model.value;

/**
 * 同一次登录的会话标识与凭据摘要，防御性复制避免外部改变已签发状态。
 *
 * @param publicId 对外会话标识
 * @param accessHash 访问令牌摘要
 * @param refreshHash 刷新令牌摘要
 * @param csrfHash CSRF令牌摘要
 * @author AIGenerator
 */
public record SessionCredentialsValue(
        String publicId, byte[] accessHash, byte[] refreshHash, byte[] csrfHash) {
    /**
     * 校验完整凭据并复制摘要，不保存原始令牌。
     *
     * @author AIGenerator
     */
    public SessionCredentialsValue {
        // 1. 拒绝不完整的会话标识和凭据摘要。
        if (publicId == null || accessHash == null || refreshHash == null || csrfHash == null) {
            throw new IllegalArgumentException("incomplete session credentials");
        }
        // 2. 复制全部摘要，调用者随后修改输入数组不会改变已签发凭据。
        accessHash = accessHash.clone();
        refreshHash = refreshHash.clone();
        csrfHash = csrfHash.clone();
    }

    /**
     * 返回访问摘要副本。
     *
     * @return 访问摘要的独立副本
     * @author AIGenerator
     */
    public byte[] accessHash() {
        return accessHash.clone();
    }

    /**
     * 返回刷新摘要副本。
     *
     * @return 刷新摘要的独立副本
     * @author AIGenerator
     */
    public byte[] refreshHash() {
        return refreshHash.clone();
    }

    /**
     * 返回CSRF摘要副本。
     *
     * @return CSRF摘要的独立副本
     * @author AIGenerator
     */
    public byte[] csrfHash() {
        return csrfHash.clone();
    }

    /**
     * 诊断不输出凭据摘要。
     *
     * @return 脱敏描述
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "SessionCredentialsValue{redacted}";
    }
}
