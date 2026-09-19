package com.hellotravel.application.knowledge.document.file.command;

/**
 * 有界资料文件处理命令。
 *
 * @param action 存储操作
 * @param filename 展示文件名
 * @param bytes 有界上传字节
 * @param storageKey 受控文件键
 * @author AIGenerator
 */
public record FileCommand(String action, String filename, byte[] bytes, String storageKey) {
}
