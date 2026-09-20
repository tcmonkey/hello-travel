package com.hellotravel.application.knowledge.assembler;

import com.hellotravel.application.knowledge.command.KnowledgeCommand;
import com.hellotravel.application.knowledge.policy.KnowledgeIndexPolicy;
import com.hellotravel.application.knowledge.result.DocumentAppResult;
import com.hellotravel.application.knowledge.result.KnowledgeAppResult;
import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeDocumentAggregate;
import com.hellotravel.domain.knowledge.model.entity.KnowledgeDocumentEntity;
import com.hellotravel.model.knowledge.FileDO;

import org.springframework.stereotype.Component;

/**
 * 知识持久快照及分页到应用视图的投影。
 *
 * @author AIGenerator
 */
@Component
public final class KnowledgeAppAssembler {

    /**
     * 资料列表预览字符上限。
     *
     * @author AIGenerator
     */
    private static final int PREVIEW_CHARACTERS = 240;

    /**
     * 转换文档视图，列表正文仅展示有界摘要。
     *
     * @param entity 本次转换的entity快照
     * @param full 本次转换的full快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public DocumentAppResult document(KnowledgeDocumentEntity entity, boolean full) {
        return new DocumentAppResult(
                entity.publicId(),
                entity.title(),
                entity.status(),
                entity.version(),
                entity.sourceUrl(),
                entity.errorCode(),
                full
                        ? entity.extractedText()
                        : entity.extractedText() == null
                                ? null
                                : entity.extractedText()
                                        .substring(
                                                0,
                                                Math.min(
                                                        PREVIEW_CHARACTERS,
                                                        entity.extractedText().length())));
    }

    /**
     * 转换单份资料操作结果，默认游标不散落在业务分支。
     *
     * @param entity 本次转换的entity快照
     * @param full 本次转换的full快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public KnowledgeAppResult single(KnowledgeDocumentEntity entity, boolean full) {
        return new KnowledgeAppResult(java.util.List.of(document(entity, full)), 0, false);
    }

    /**
     * 转换持久资料页并计算可推进游标。
     *
     * @param rows 本次转换的rows快照
     * @param command 本次转换的command快照
     * @param limit 本次转换的limit快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public KnowledgeAppResult page(
            java.util.List<KnowledgeDocumentAggregate> rows, KnowledgeCommand command, int limit) {
        return new KnowledgeAppResult(
                rows.stream().map(x -> document(x.entity(), false)).toList(),
                rows.isEmpty() ? command.after() : rows.get(rows.size() - 1).entity().id(),
                rows.size() == limit);
    }

    /**
     * 将受理命令和落盘结果整体投影为领域文档，固定索引配置来自选定策略。
     *
     * @param command 本次转换的command快照
     * @param parsed 本次转换的parsed快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public KnowledgeDocumentAggregate received(KnowledgeCommand command, FileDO parsed) {
        // 1. 本次入库使用同一个UTC时点，避免分别取得时间形成无意义偏差。
        var now = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
        // 2. 映射完整受理资料，实体负责初始状态，供应商元数据由应用策略选择。
        return KnowledgeDocumentAggregate.received(
                command.userId(),
                command.filename().substring(0, Math.min(200, command.filename().length())),
                command.filename(),
                parsed.mime(),
                parsed.storageKey(),
                (long) command.bytes().length,
                parsed.hash(),
                parsed.text(),
                command.sourceUrl(),
                KnowledgeIndexPolicy.profile(),
                now,
                now);
    }
}
