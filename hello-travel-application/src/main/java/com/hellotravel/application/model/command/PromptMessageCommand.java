package com.hellotravel.application.model.command;

/**
 * 模型输入片段，资料不是系统授权。
 *
 * @param role USER或ASSISTANT历史角色
 * @param text 有界文本
 * @author AIGenerator
 */
public record PromptMessageCommand(String role, String text) {
}
