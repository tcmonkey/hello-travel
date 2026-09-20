package com.hellotravel.adaptor.knowledge.input;

import com.hellotravel.adaptor.knowledge.input.assembler.KnowledgeAssembler;
import com.hellotravel.adaptor.common.HttpResults;
import com.hellotravel.application.knowledge.KnowledgeAppService;
import com.hellotravel.client.knowledge.request.KnowledgeRequest;
import com.hellotravel.client.knowledge.response.KnowledgeResponse;
import com.hellotravel.common.result.Result;

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

    private final KnowledgeAppService application;
    private final KnowledgeAssembler knowledgeAssembler;

    public KnowledgeController(
            KnowledgeAppService application, KnowledgeAssembler knowledgeAssembler) {
        this.application = application;
        this.knowledgeAssembler = knowledgeAssembler;
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
            // 1. 检查有界上传输入并转换用例命令。
            var command = knowledgeAssembler.upload(file, sourceUrl, httpServletRequest);
            // 2. 提交资料用例并保留下层失败分类。
            var result = application.manage(command);
            if (!result.success()) {
                return HttpResults.failure(result);
            }
            // 3. 投影公开资料结果。
            return Result.success(knowledgeAssembler.toResponse(result.data()));
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
            // 1. 将协议路由和完整请求转换为具有可信身份的用例命令。
            var command =
                    knowledgeAssembler.toCommand(action, knowledgeRequest, httpServletRequest);
            // 2. 执行应用入口，保留失败分类且不向外暴露内部载荷。
            var result = application.manage(command);
            if (!result.success()) {
                return HttpResults.failure(result);
            }
            // 3. 通过本层assembler投影公开响应。
            return Result.success(knowledgeAssembler.toResponse(result.data()));
        } catch (Exception exception) {
            return HttpResults.capture(exception);
        }
    }
}
