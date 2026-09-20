package com.hellotravel.application.chat;

import com.hellotravel.application.chat.support.TravelConversationContext;
import com.hellotravel.application.chat.adaptor.TravelFactQueryAdaptor;
import com.hellotravel.application.chat.assembler.TravelFactQueryAppAssembler;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.knowledge.RagRetrievalAppService;
import com.hellotravel.common.error.Failures;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.travel.TravelIntentMode;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 编排知识库证据与外部实时事实，供事实问答和规划图共同复用。
 *
 * @author AIGenerator
 */
@Component
public final class TravelEvidenceCollectorAppService {

    private final TravelFactQueryAdaptor travelFactQueryAdaptor;
    private final TravelFactQueryAppAssembler travelFactQueryAppAssembler;
    private final RagRetrievalAppService ragRetrievalAppService;

    public TravelEvidenceCollectorAppService(
            TravelFactQueryAdaptor travelFactQueryAdaptor,
            TravelFactQueryAppAssembler travelFactQueryAppAssembler,
            RagRetrievalAppService ragRetrievalAppService) {
        this.travelFactQueryAdaptor = travelFactQueryAdaptor;
        this.travelFactQueryAppAssembler = travelFactQueryAppAssembler;
        this.ragRetrievalAppService = ragRetrievalAppService;
    }

    /**
     * 收集当前用户知识库中的目的地资料证据。
     *
     * @param context 当前旅行上下文
     * @return 收集结果
     * @author AIGenerator
     */
    public Result<Boolean> collectDestinationEvidence(TravelConversationContext context) {
        try {
            // 1. 只有用户诉求需要资料证据时才调用私有知识库检索。
            if (context.intent().knowledge()
                    || context.intent().mode() == TravelIntentMode.PLANNING) {
                context.sources(ragRetrievalAppService.retrieve(context.run().userId(), context.input()));
            } else {
                context.sources(List.of());
            }
            // 2. 资料库暂不可用时由调用方决定降级或失败，不伪造政策证据。
            return Result.success(Boolean.TRUE);
        } catch (Exception exception) {
            return Failures.capture(exception, ApplicationErrorCode.UNAVAILABLE);
        }
    }

    /**
     * 收集天气与路线等外部实时事实。
     *
     * @param context 当前旅行上下文
     * @return 收集结果
     * @author AIGenerator
     */
    public Result<Boolean> collectRealtimeFacts(TravelConversationContext context) {
        try {
            // 1. 将已识别的城市、起终点和事实类型整体转换为工具命令。
            var command = travelFactQueryAppAssembler.command(context.intent());
            if (!command.requested()) {
                context.realtimeFacts("");
                return Result.success(Boolean.TRUE);
            }
            var response = travelFactQueryAdaptor.query(command);
            // 2. 外部工具失败时返回受控失败，模型不得自行补齐实时事实。
            if (!response.success() || response.data() == null) {
                return Result.failure(ApplicationErrorCode.UNAVAILABLE);
            }
            // 3. 保存带时效说明的事实或明确降级文本，后续模型只能据此回答。
            context.realtimeFacts(response.data().facts());
            return Result.success(Boolean.TRUE);
        } catch (Exception exception) {
            return Failures.capture(exception, ApplicationErrorCode.UNAVAILABLE);
        }
    }
}
