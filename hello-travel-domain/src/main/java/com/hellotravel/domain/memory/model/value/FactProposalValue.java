package com.hellotravel.domain.memory.model.value;

import java.util.Optional;
import java.util.Set;

/**
 * 已核验的显式长期事实提案；只采纳用户原文连续摘录，拒绝模型推测。
 *
 * @param key 稳定事实键
 * @param category 已允许的旅行事实类别
 * @param excerpt 用户明确确认的原文摘录
 * @author AIGenerator
 */
public record FactProposalValue(String key, String category, String excerpt) {
    /**
     * 校验提案边界，禁止通过公开构造器绕过值对象自身规则。
     *
     * @author AIGenerator
     */
    public FactProposalValue {
        // 1. 固定允许的事实类别及文本边界，任何构造入口均不能绕过。
        if (key == null
                || !key.matches("[a-z0-9._-]{1,60}")
                || excerpt == null
                || excerpt.isBlank()
                || excerpt.length() > 300
                || category == null
                || !Set.of("PREFERENCE", "TRAVEL_CONSTRAINT", "CONFIRMED_PLAN")
                        .contains(category)) {
            throw new IllegalArgumentException("invalid fact proposal");
        }
    }

    /**
     * 同时核验结构约束与原文来源；不合法的模型条目被丢弃。
     *
     * @param key 模型返回的稳定键
     * @param category 模型返回的事实类别
     * @param excerpt 模型返回的摘录
     * @param original 原始用户消息
     * @return 已核验提案或空结果
     * @author AIGenerator
     */
    public static Optional<FactProposalValue> accept(
            String key, String category, String excerpt, String original) {
        try {
            // 1. 先由值对象校验稳定键、类别和摘录长度，结构不合法则丢弃。
            FactProposalValue proposal = new FactProposalValue(key, category, excerpt);
            // 2. 核验摘录确为用户原文的连续片段，未能溯源的模型提案不入记忆。
            return original != null && original.contains(excerpt)
                    ? Optional.of(proposal)
                    : Optional.empty();
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
