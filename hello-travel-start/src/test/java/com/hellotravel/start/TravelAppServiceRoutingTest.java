package com.hellotravel.start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hellotravel.application.chat.assembler.TravelAppAssembler;
import com.hellotravel.application.chat.command.TravelGenerateCommand;
import com.hellotravel.application.chat.TravelContextService;
import com.hellotravel.application.chat.support.TravelConversationContext;
import com.hellotravel.application.chat.TravelDialogueAppService;
import com.hellotravel.application.exception.RunExecutionService;
import com.hellotravel.application.chat.adaptor.TravelIntentRecognitionAdaptor;
import com.hellotravel.application.chat.assembler.TravelIntentAppAssembler;
import com.hellotravel.application.chat.travel.TravelPlanningGraph;
import com.hellotravel.application.chat.travel.TravelAppService;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.chat.model.entity.ChatRunEntity;
import com.hellotravel.model.travel.TravelIntentDO;
import com.hellotravel.model.travel.TravelIntentMode;
import com.hellotravel.model.chat.ChatModelStage;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

/**
 * 旅行总用例三路直接分支回归。
 *
 * @author AIGenerator
 */
class TravelAppServiceRoutingTest {

    @org.junit.jupiter.api.Test
    void preservesApplicationFailureClassificationAndMarksClaimedRun() {
        // 1. 让上下文准备返回预算超限，验证总用例不会把已分类失败压成通用错误。
        RunExecutionService execution = mock(RunExecutionService.class);
        TravelContextService contextService = mock(TravelContextService.class);
        TravelIntentRecognitionAdaptor intentAdaptor = mock(TravelIntentRecognitionAdaptor.class);
        TravelDialogueAppService dialogue = mock(TravelDialogueAppService.class);
        TravelPlanningGraph planning = mock(TravelPlanningGraph.class);
        ChatRunEntity run = mock(ChatRunEntity.class);
        when(execution.claim("run-limit")).thenReturn(run);
        when(contextService.prepare(run)).thenReturn(Result.failure(ApplicationErrorCode.CONTEXT_LIMIT));
        var application = new TravelAppService(
                execution,
                contextService,
                intentAdaptor,
                new TravelIntentAppAssembler(),
                dialogue,
                planning,
                new TravelAppAssembler());

        // 2. 执行任务并核对返回值与持久任务终态使用相同稳定错误分类。
        var result = application.generate(new TravelGenerateCommand("run-limit"));

        assertTrue(!result.success());
        assertEquals(ApplicationErrorCode.CONTEXT_LIMIT.code(), result.code());
        verify(execution).fail(run, ApplicationErrorCode.CONTEXT_LIMIT.code());
        verifyNoInteractions(intentAdaptor, dialogue, planning);
    }

    @ParameterizedTest
    @EnumSource(TravelIntentMode.class)
    void routesOnlyToTheSelectedBusinessCapability(TravelIntentMode mode) {
        // 1. 准备已领取任务、隔离上下文和结构化意图，不依赖字符串动作。
        RunExecutionService execution = mock(RunExecutionService.class);
        TravelContextService contextService = mock(TravelContextService.class);
        TravelIntentRecognitionAdaptor intentAdaptor = mock(TravelIntentRecognitionAdaptor.class);
        TravelDialogueAppService dialogue = mock(TravelDialogueAppService.class);
        TravelPlanningGraph planning = mock(TravelPlanningGraph.class);
        ChatRunEntity run = mock(ChatRunEntity.class);
        TravelConversationContext context = new TravelConversationContext(run);
        context.load("请帮我处理旅行需求", List.of(), "", "");
        TravelIntentDO intent = intent(mode);
        when(execution.claim("run-1")).thenReturn(run);
        when(execution.invocation(
                        eq(run), eq(ChatModelStage.INTENT.name()), eq(1), anyInt()))
                .thenReturn("invocation-1");
        when(contextService.prepare(run)).thenReturn(Result.success(context));
        when(contextService.budgetJson(context)).thenReturn("{}");
        when(intentAdaptor.recognize(any())).thenReturn(Result.success(intent));
        when(dialogue.answer(eq(context), eq(false))).thenReturn(Result.success(context));
        when(dialogue.answer(eq(context), eq(true))).thenReturn(Result.success(context));
        when(planning.execute(context)).thenReturn(Result.success(context));
        var application = new TravelAppService(
                execution,
                contextService,
                intentAdaptor,
                new TravelIntentAppAssembler(),
                dialogue,
                planning,
                new TravelAppAssembler());

        // 2. 执行总用例并核对只调用当前意图对应的一个业务能力。
        var result = application.generate(new TravelGenerateCommand("run-1"));

        assertTrue(result.success());
        assertTrue(result.data().completed());
        if (mode == TravelIntentMode.PLANNING) {
            verify(planning).execute(context);
            verify(dialogue, never()).answer(any(), eq(false));
            verify(dialogue, never()).answer(any(), eq(true));
        } else if (mode == TravelIntentMode.FACT_QUERY) {
            verify(dialogue).answer(context, true);
            verify(planning, never()).execute(any());
        } else {
            verify(dialogue).answer(context, false);
            verify(planning, never()).execute(any());
        }
        verify(execution).finalizeAnswer(eq(run), any(), any(), any(), eq("{}"));
    }

    private TravelIntentDO intent(TravelIntentMode mode) {
        // 1. 为路由测试建立最小结构化意图，其余字段不参与分支选择。
        return new TravelIntentDO(
                mode,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                false,
                false,
                false);
    }
}
