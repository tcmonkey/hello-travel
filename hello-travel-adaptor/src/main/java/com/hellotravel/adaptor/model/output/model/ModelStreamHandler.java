package com.hellotravel.adaptor.model.output.model;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.chat.response.StreamingHandle;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * 累积增量文本并向工作流回调；取消时终止供应商流。
 *
 * @author AIGenerator
 */
public final class ModelStreamHandler implements StreamingChatResponseHandler {

    /**
     * 保存partial对应的有界运行状态。
     *
     * @author AIGenerator
     */
    private final Predicate<String> partial;

    /**
     * 保存complete对应的有界运行状态。
     *
     * @author AIGenerator
     */
    private final CompletableFuture<ChatResponse> complete;

    /**
     * 保存handle对应的有界运行状态。
     *
     * @author AIGenerator
     */
    private final AtomicReference<StreamingHandle> handle;

    /**
     * 保存text对应的有界运行状态。
     *
     * @author AIGenerator
     */
    private final StringBuilder text = new StringBuilder();

    /**
     * 建立ModelStreamHandler并保存明确业务依赖。
     *
     * @author AIGenerator
     * @param partial 有界草稿回调
     * @param complete 调用完成通知
     * @param handle 流式取消句柄
     */
    public ModelStreamHandler(
            Predicate<String> partial,
            CompletableFuture<ChatResponse> complete,
            AtomicReference<StreamingHandle> handle) {
        this.partial = partial;
        this.complete = complete;
        this.handle = handle;
    }

    /**
     * 累积流式文本并将完整草稿交由栅栏回调验证。
     *
     * @author AIGenerator
     * @param response HTTP响应
     * @param context 受控context参数
     */
    public void onPartialResponse(PartialResponse response, PartialResponseContext context) {
        // 1. 映射本段快照字段，业务状态规则不放入PO赋值。
        handle.set(context.streamingHandle());
        // 2. 执行append职责步骤，并把失败交给所属事务或入口处理。
        text.append(response.text());
        // 3. 在异常捕获或资源释放边界内完成本段处理，失败不得伪装为成功。
        try {
            if (text.length() > 262144 || (partial != null && !partial.test(text.toString()))) {
                context.streamingHandle().cancel();
                complete.completeExceptionally(new CancellationException());
            }
        } catch (RuntimeException exception) {
            context.streamingHandle().cancel();
            complete.completeExceptionally(exception);
        }
    }

    /**
     * 完成当前供应商调用的结果通知。
     *
     * @author AIGenerator
     * @param response HTTP响应
     */
    public void onCompleteResponse(ChatResponse response) {
        complete.complete(response);
    }

    /**
     * 将供应商失败通知等待方。
     *
     * @author AIGenerator
     * @param error 待分类的失败
     */
    public void onError(Throwable error) {
        complete.completeExceptionally(error);
    }
}
