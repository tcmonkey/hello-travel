package com.hellotravel.application.knowledge.result;

/**
 * 资料展示快照。
 *
 * @param id 公开标识
 * @param title 资料标题
 * @param status 处理状态
 * @param version 并发版本
 * @param sourceUrl 引用来源
 * @param error 安全错误
 * @param text 单份资料原文
 * @author AIGenerator
 */
public record DocumentResult(
        String id,
        String title,
        String status,
        Long version,
        String sourceUrl,
        String error,
        String text) {
}
