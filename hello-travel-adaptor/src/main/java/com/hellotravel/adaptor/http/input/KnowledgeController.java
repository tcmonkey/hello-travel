package com.hellotravel.adaptor.http.input;

import com.hellotravel.adaptor.http.support.ApiViews;
import com.hellotravel.adaptor.http.support.HttpIdentity;
import com.hellotravel.adaptor.http.support.HttpResults;
import com.hellotravel.application.knowledge.command.KnowledgeCommand;
import com.hellotravel.application.knowledge.service.KnowledgeApplication;
import com.hellotravel.client.knowledge.request.KnowledgeRequest;
import com.hellotravel.client.knowledge.response.KnowledgeResponse;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 私有资料上传、分页原文与显式重试入口。
 *
 * @author AIGenerator
 */
@RestController
@RequestMapping("/api/v1/knowledge")
public final class KnowledgeController {

    private final KnowledgeApplication application;

    private final ApiViews views;

    public KnowledgeController(KnowledgeApplication application, ApiViews views) {
        this.application = application;
        this.views = views;
    }

    /**
     * 接收有界私有附件并提交异步索引。
     *
     * @author AIGenerator
     * @param file 受控file参数
     * @param sourceUrl 受控sourceUrl参数
     * @param httpServletRequest HTTP请求及认证上下文
     * @return 当前操作的业务结果
     */
    @PostMapping("/upload")
    public Result<KnowledgeResponse> upload(
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String sourceUrl,
            HttpServletRequest httpServletRequest) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (file.isEmpty() || file.getSize() > 10485760) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 取得本段结果并准备本层转换，随后显式核对成功状态。
            var result =
                    application.manage(
                            new KnowledgeCommand(
                                    "UPLOAD",
                                    HttpIdentity.user(httpServletRequest),
                                    null,
                                    file.getOriginalFilename(),
                                    file.getBytes(),
                                    sourceUrl,
                                    null,
                                    0,
                                    100));
            // 3. 返回本段实际处理结果，保持本层输出契约。
            return views.respond(result, KnowledgeResponse.class);
        } catch (Exception exception) {
            return HttpResults.capture(exception);
        }
    }

    /**
     * 执行指定归属下的业务用例。
     *
     * @author AIGenerator
     * @param action 受控action参数
     * @param knowledgeRequest 已验证的公开请求参数
     * @param httpServletRequest HTTP请求及认证上下文
     * @return 当前操作的业务结果
     */
    @PostMapping("/{action}")
    public Result<KnowledgeResponse> manage(
            @PathVariable String action,
            @Valid @RequestBody KnowledgeRequest knowledgeRequest,
            HttpServletRequest httpServletRequest) {
        try {
            // 1. 取得候选任务快照，领取时再次核验，供本段后续处理使用。
            String selected =
                    switch (action) {
                        case "list" -> "LIST";
                        case "read" -> "READ";
                        case "retry" -> "RETRY";
                        case "delete" -> "DELETE";
                        default -> throw new DomainException(DomainErrorCode.NOT_FOUND);
                    };
            var result =
                    application.manage(
                            new KnowledgeCommand(
                                    selected,
                                    HttpIdentity.user(httpServletRequest),
                                    knowledgeRequest.documentId(),
                                    null,
                                    null,
                                    null,
                                    knowledgeRequest.expectedVersion(),
                                    knowledgeRequest.after() == null ? 0 : knowledgeRequest.after(),
                                    knowledgeRequest.limit() == null
                                            ? 100
                                            : knowledgeRequest.limit()));
            // 2. 返回本段实际处理结果，保持本层输出契约。
            return views.respond(result, KnowledgeResponse.class);
        } catch (Exception exception) {
            return HttpResults.capture(exception);
        }
    }
}
