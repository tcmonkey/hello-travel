package com.hellotravel.common.identity;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 无框架依赖的时间有序ULID及高熵随机令牌生成。
 *
 * @author AIGenerator
 */
public final class Ids {

    /**
     * 保存RANDOM对应的有界运行状态。
     *
     * @author AIGenerator
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 保存ALPHABET对应的有界运行状态。
     *
     * @author AIGenerator
     */
    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

    /**
     * 建立Ids并保存明确业务依赖。
     *
     * @author AIGenerator
     */
    private Ids() {
    }

    /**
     * 生成带随机熵的公开ULID。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public static String next() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        long millis = System.currentTimeMillis();
        for (int i = 5; i >= 0; i--) {
            bytes[i] = (byte) millis;
            millis >>>= 8;
        }
        BigInteger value = new BigInteger(1, bytes);
        char[] result = new char[26];
        for (int i = 25; i >= 0; i--) {
            result[i] = ALPHABET.charAt(value.intValue() & 31);
            value = value.shiftRight(5);
        }
        return new String(result);
    }

    /**
     * 生成密码学安全的随机凭据。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public static String token() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 计算UTF8文本的SHA256摘要。
     *
     * @author AIGenerator
     * @param text 有界文本内容
     * @return 当前操作的业务结果
     */
    public static byte[] hash(String text) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
