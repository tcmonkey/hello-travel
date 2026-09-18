package com.hellotravel.application.knowledge.policy;

import com.hellotravel.domain.knowledge.model.value.EmbeddingProfileValue;

/**
 * 单一一期知识索引配置，文档元数据、嵌入请求和向量schema保持一致。
 *
 * @author AIGenerator
 */
public final class KnowledgeIndexPolicy {
    /**
     * 冻结的一期索引协议，迁移改版前不得改变集合和维度。
     *
     * @author AIGenerator
     */
    private static final EmbeddingProfileValue PROFILE =
            EmbeddingProfileValue.selected("text-embedding-v4", 1024, "hello_travel_kb_v1_d1024");

    /**
     * 禁止实例化无状态转换器。
     *
     * @author AIGenerator
     */
    private KnowledgeIndexPolicy() {
    }

    /**
     * 取得一期专属索引model配置。
     *
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public static String model() {
        return PROFILE.model();
    }

    /**
     * 取得一期专属索引dimensions配置。
     *
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public static int dimensions() {
        return PROFILE.dimensions();
    }

    /**
     * 取得一期专属索引collection配置。
     *
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public static String collection() {
        return PROFILE.collection();
    }

    /**
     * 提供要记录到文档的不可变索引元数据。
     *
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public static EmbeddingProfileValue profile() {
        return PROFILE;
    }
}
