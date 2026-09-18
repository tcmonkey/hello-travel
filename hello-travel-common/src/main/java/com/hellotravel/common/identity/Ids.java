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
        // 1. 取得待校验的上传文件字节，供本段后续处理使用。
        byte[] bytes = new byte[16];
        // 2. 执行nextBytes职责步骤，并把失败交给所属事务或入口处理。
        RANDOM.nextBytes(bytes);
        // 3. 取得受限超时时长，供本段后续处理使用。
        long millis = System.currentTimeMillis();
        // 4. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
        for (int i = 5; i >= 0; i--) {
            bytes[i] = (byte) millis;
            millis >>>= 8;
        }
        // 5. 取得本次预算或解析后的业务值，供本段后续处理使用。
        BigInteger value = new BigInteger(1, bytes);
        char[] result = new char[26];
        // 6. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
        for (int i = 25; i >= 0; i--) {
            result[i] = ALPHABET.charAt(value.intValue() & 31);
            value = value.shiftRight(5);
        }
        // 7. 返回本段实际处理结果，保持本层输出契约。
        return new String(result);
    }

    /**
     * 生成密码学安全的随机凭据。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public static String token() {
        // 1. 取得待校验的上传文件字节，供本段后续处理使用。
        byte[] bytes = new byte[32];
        // 2. 执行nextBytes职责步骤，并把失败交给所属事务或入口处理。
        RANDOM.nextBytes(bytes);
        // 3. 返回本段实际处理结果，保持本层输出契约。
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
