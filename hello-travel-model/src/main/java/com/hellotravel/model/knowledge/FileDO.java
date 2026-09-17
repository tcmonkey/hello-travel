package com.hellotravel.model.knowledge;

/**
 * 已提取的资料快照。
 *
 * @param storageKey 受控文件键
 * @param mime 检测类型
 * @param hash 文件摘要
 * @param text 提取明文
 * @author AIGenerator
 */
public record FileDO(String storageKey, String mime, byte[] hash, String text) {
}
