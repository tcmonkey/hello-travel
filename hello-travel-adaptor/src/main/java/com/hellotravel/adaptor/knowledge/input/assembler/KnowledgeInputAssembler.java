package com.hellotravel.adaptor.knowledge.input.assembler;

import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.adaptor.exception.AdaptorException;
import com.hellotravel.adaptor.web.support.HttpIdentity;
import com.hellotravel.adaptor.web.support.HttpProtocol;
import com.hellotravel.application.knowledge.command.KnowledgeCommand;
import com.hellotravel.application.knowledge.result.DocumentResult;
import com.hellotravel.application.knowledge.result.KnowledgeResult;
import com.hellotravel.client.knowledge.request.KnowledgeRequest;
import com.hellotravel.client.knowledge.response.DocumentResponse;
import com.hellotravel.client.knowledge.response.KnowledgeResponse;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 本层knowledge协议双向映射。
 *
 * @author AIGenerator
 */
@Component
public final class KnowledgeInputAssembler {

    /**
     * 整体绑定请求与可信身份，集中解释协议路由及分页默认值。
     *
     * @param action 本次转换的action快照
     * @param request 本次转换的request快照
     * @param http 本次转换的http快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public KnowledgeCommand toCommand(
            String action, KnowledgeRequest request, HttpServletRequest http) {
        // 1. 解释允许的协议路由，未知动作在本层拒绝。
        String selected =
                switch (action) {
                    case "list" -> "LIST";
                    case "read" -> "READ";
                    case "retry" -> "RETRY";
                    case "delete" -> "DELETE";
                    default -> throw new AdaptorException(AdaptorErrorCode.NOT_FOUND);
                };
        // 2. 将协议字段投影为用例命令，内部归属只来自认证上下文。
        return new KnowledgeCommand(
                selected,
                HttpIdentity.user(http),
                request.documentId(),
                null,
                null,
                null,
                request.expectedVersion(),
                HttpProtocol.cursor(request.after()),
                HttpProtocol.pageSize(request.limit()));
    }

    /**
     * 上传前检查协议容量，再读取附件形成有界用例命令。
     *
     * @param file 本次转换的file快照
     * @param sourceUrl 本次转换的sourceUrl快照
     * @param http 本次转换的http快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public KnowledgeCommand upload(MultipartFile file, String sourceUrl, HttpServletRequest http) {
        // 1. 在读取附件之前拒绝空文件和超限文件。
        if (file.isEmpty() || file.getSize() > HttpProtocol.MAX_UPLOAD_BYTES) {
            throw new AdaptorException(AdaptorErrorCode.INVALID);
        }
        // 2. 仅在通过容量检查后读取附件字节，读取失败使用本层错误。
        try {
            return new KnowledgeCommand(
                    "UPLOAD",
                    HttpIdentity.user(http),
                    null,
                    file.getOriginalFilename(),
                    file.getBytes(),
                    sourceUrl,
                    null,
                    HttpProtocol.INITIAL_CURSOR,
                    HttpProtocol.DEFAULT_PAGE_SIZE);
        } catch (IOException exception) {
            throw new AdaptorException(AdaptorErrorCode.FAILED);
        }
    }

    /**
     * 投影资料公开字段。
     *
     * @param value 本次转换的value快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public DocumentResponse document(DocumentResult value) {
        return new DocumentResponse(
                value.id(),
                value.title(),
                value.status(),
                value.version(),
                value.sourceUrl(),
                value.error(),
                value.text());
    }

    /**
     * 转换资料列表及稳定游标。
     *
     * @param result 本次转换的result快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public KnowledgeResponse toResponse(KnowledgeResult result) {
        return new KnowledgeResponse(
                result.items().stream().map(this::document).toList(),
                result.nextCursor(),
                result.hasMore());
    }
}
