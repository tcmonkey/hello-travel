package com.hellotravel.adaptor.knowledge.output.file.converter;

import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.adaptor.exception.AdaptorException;
import com.hellotravel.application.knowledge.document.file.command.FileCommand;
import com.hellotravel.application.support.Json;
import com.hellotravel.model.knowledge.FileDO;

import org.springframework.stereotype.Component;

/**
 * 文件外部能力结果投影。
 *
 * @author AIGenerator
 */
@Component
public final class FileOutputConverter {

    /**
     * 返回已完成删除的文件结果。
     *
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public FileDO deleted() {
        return new FileDO(null, null, null, null);
    }

    /**
     * 转换有界扫描列表。
     *
     * @param entries 本次转换的entries快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public FileDO scanned(java.util.List<java.util.Map<String, Object>> entries) {
        return new FileDO(null, null, null, Json.encode(entries));
    }

    /**
     * 投影落盘结果，摘要始终来自原始附件字节。
     *
     * @param command 本次转换的command快照
     * @param key 本次转换的key快照
     * @param mime 本次转换的mime快照
     * @param text 本次转换的text快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public FileDO stored(FileCommand command, String key, String mime, String text) {
        // 1. 计算原始附件摘要，算法不可用时使用本层技术失败。
        try {
            return new FileDO(
                    key,
                    mime,
                    java.security.MessageDigest.getInstance("SHA-256").digest(command.bytes()),
                    text);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new AdaptorException(AdaptorErrorCode.FAILED);
        }
    }
}
