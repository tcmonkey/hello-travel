package com.hellotravel.start;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hellotravel.application.chat.TravelContextService;
import com.hellotravel.application.chat.support.TravelConversationContext;
import com.hellotravel.application.exception.RunExecutionService;
import com.hellotravel.application.chat.TravelEvidenceCollectorAppService;
import com.hellotravel.application.chat.adaptor.TravelPlanGenerationAdaptor;
import com.hellotravel.application.chat.assembler.TravelPlanAppAssembler;
import com.hellotravel.application.chat.travel.TravelPlanningGraph;
import com.hellotravel.application.chat.travel.TravelPlanningContextRegistry;
import com.hellotravel.application.chat.travel.node.ClarifyTravelRequirementsNode;
import com.hellotravel.application.chat.travel.node.CollectDestinationEvidenceNode;
import com.hellotravel.application.chat.travel.node.CollectRealtimeFactsNode;
import com.hellotravel.application.chat.travel.node.GenerateTravelDraftNode;
import com.hellotravel.application.chat.travel.node.RenderTravelPlanNode;
import com.hellotravel.application.chat.travel.node.ReviseTravelDraftNode;
import com.hellotravel.application.chat.travel.node.ValidateTravelDraftNode;
import com.hellotravel.application.chat.travel.node.ValidateTravelRequestNode;
import com.hellotravel.domain.chat.model.entity.ChatRunEntity;
import com.hellotravel.domain.travel.model.param.TravelPlanDraftValidationParam;
import com.hellotravel.domain.travel.service.TravelPlanDomainService;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.chat.ChatModelDO;
import com.hellotravel.model.travel.TravelIntentDO;
import com.hellotravel.model.travel.TravelIntentMode;
import com.hellotravel.model.travel.TravelPlanDayDO;
import com.hellotravel.model.travel.TravelPlanDraftDO;
import com.hellotravel.model.travel.TravelPlanGenerationDO;
import com.hellotravel.model.travel.TravelPlanItemDO;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

/**
 * 旅行规划图的条件边和确定性领域规则回归。
 *
 * @author AIGenerator
 */
class TravelPlanningGraphTest {

    @Test
    void completePlanPassesEveryNodeAndRendersWithoutRevision() {
        // 1. 使用真实节点和领域服务组装完整图，只替换外部事实与模型边界。
        var fixture = fixture();
        var context = context(fixture.run(), completeIntent());
        when(fixture.plan().generate(any()))
                .thenReturn(Result.success(generation(validDraft(new BigDecimal("50")))));

        // 2. 执行完整图并核对有效草稿直接渲染，不进入修订节点。
        var result = fixture.graph().execute(context);

        assertTrue(result.success());
        assertTrue(result.data().answer().contains("景德镇一日行"));
        assertTrue(result.data().answer().contains("费用概览"));
        verify(fixture.evidence()).collectDestinationEvidence(context);
        verify(fixture.evidence()).collectRealtimeFacts(context);
        verify(fixture.plan(), never()).revise(any());
    }

    @Test
    void invalidDraftIsRevisedAndValidatedAgainBeforeRendering() {
        // 1. 第一版草稿超预算，修订端口返回通过确定性规则的新草稿。
        var fixture = fixture();
        var context = context(fixture.run(), completeIntent());
        when(fixture.plan().generate(any()))
                .thenReturn(Result.success(generation(validDraft(new BigDecimal("600")))));
        when(fixture.plan().revise(any()))
                .thenReturn(Result.success(generation(validDraft(new BigDecimal("50")))));

        // 2. 执行条件边并核对只修订一次，最终回答来自再次校验后的草稿。
        var result = fixture.graph().execute(context);

        assertTrue(result.success());
        assertTrue(result.data().answer().contains("景德镇一日行"));
        assertTrue(result.data().revisionCount() == 1);
        verify(fixture.plan()).revise(any());
    }

    @Test
    void twoInvalidRevisionsStopAtClarification() {
        // 1. 让生成和两次修订始终返回超预算草稿，验证图不会无界循环。
        var fixture = fixture();
        var context = context(fixture.run(), completeIntent());
        var invalid = Result.success(generation(validDraft(new BigDecimal("600"))));
        when(fixture.plan().generate(any())).thenReturn(invalid);
        when(fixture.plan().revise(any())).thenReturn(invalid);

        // 2. 执行完整条件边并核对达到上限后形成确定性说明。
        var result = fixture.graph().execute(context);

        assertTrue(result.success());
        assertTrue(result.data().revisionCount() == 2);
        assertTrue(result.data().answer().contains("没有通过可执行性校验"));
        verify(fixture.plan(), times(2)).revise(any());
    }

    @Test
    void insufficientTransitGapIsNormalizedBeforePlanValidation() {
        // 1. 构造活动内容和事实标记均有效、仅接驳时间不足的模型草稿。
        var fixture = fixture();
        var context = context(fixture.run(), completeIntent());
        var first = new TravelPlanItemDO(
                "09:00", "10:00", "陶瓷博物馆", "参观", "步行", 10,
                new BigDecimal("20"), "official-1", true, "");
        var second = new TravelPlanItemDO(
                "10:05", "11:05", "御窑博物馆", "参观", "驾车", 20,
                new BigDecimal("30"), "official-2", true, "");
        var draft = new TravelPlanDraftDO(
                "景德镇一日行",
                "陶瓷文化主题路线",
                List.of(new TravelPlanDayDO("2026-10-01", "景德镇", List.of(first, second))),
                new BigDecimal("50"),
                List.of("提前预约"),
                List.of("注意天气变化"),
                List.of());
        when(fixture.plan().generate(any())).thenReturn(Result.success(generation(draft)));

        // 2. 执行规划图并核对第二项仅顺延时间后可直接渲染，无须模型重试。
        var result = fixture.graph().execute(context);

        assertTrue(result.success());
        assertTrue(result.data().answer().contains("10:20-11:20"));
        verify(fixture.plan(), never()).revise(any());
    }

    @Test
    void missingRequirementsRouteToClarificationBeforeExternalEvidence() {
        var contextService = mock(TravelContextService.class);
        var domainService = new TravelPlanDomainService();
        var assembler = new TravelPlanAppAssembler(contextService);
        var registry = new TravelPlanningContextRegistry();
        var validateRequest =
                new ValidateTravelRequestNode(
                        domainService, contextService, assembler, registry);
        var clarify = new ClarifyTravelRequirementsNode(assembler, contextService, registry);
        var evidence = mock(CollectDestinationEvidenceNode.class);
        var graph =
                new TravelPlanningGraph(
                        validateRequest,
                        evidence,
                        mock(CollectRealtimeFactsNode.class),
                        mock(GenerateTravelDraftNode.class),
                        mock(ValidateTravelDraftNode.class),
                        mock(ReviseTravelDraftNode.class),
                        mock(RenderTravelPlanNode.class),
                        clarify,
                        registry);
        var context = new TravelConversationContext(mock(ChatRunEntity.class));
        context.intent(
                new TravelIntentDO(
                        TravelIntentMode.PLANNING,
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
                        true,
                        false,
                        true));

        var result = graph.execute(context);

        assertTrue(result.success());
        assertTrue(result.data().answer().contains("目的地"));
        verifyNoInteractions(evidence);
    }

    @Test
    void overlappingAndUnverifiedDraftCannotBypassDomainValidation() {
        var intent =
                new TravelIntentDO(
                        TravelIntentMode.PLANNING,
                        "景德镇",
                        "南昌",
                        "景德镇",
                        "2026-10-01",
                        "2026-10-01",
                        1,
                        2,
                        new BigDecimal("100"),
                        List.of("陶瓷"),
                        List.of(),
                        true,
                        true,
                        true);
        var first =
                new TravelPlanItemDO(
                        "09:00", "11:00", "陶瓷博物馆", "参观", "步行", 10,
                        new BigDecimal("80"), null, false, "");
        var second =
                new TravelPlanItemDO(
                        "10:30", "12:00", "御窑博物馆", "参观", "步行", 10,
                        new BigDecimal("80"), null, true, "");
        var draft =
                new TravelPlanDraftDO(
                        "景德镇一日行",
                        "陶瓷主题路线",
                        List.of(new TravelPlanDayDO("2026-10-01", "景德镇", List.of(first, second))),
                        new BigDecimal("160"),
                        List.of(),
                        List.of(),
                        List.of());

        var result = new TravelPlanDomainService().validateDraft(
                new TravelPlanDraftValidationParam(intent, draft));

        assertTrue(result.success());
        assertFalse(result.data().valid());
        assertTrue(result.data().revisable());
        assertTrue(result.data().violations().stream().anyMatch(value -> value.contains("重叠")));
        assertTrue(result.data().violations().stream().anyMatch(value -> value.contains("核实")));
        assertTrue(result.data().violations().stream().anyMatch(value -> value.contains("预算")));
    }

    @Test
    void dateTransitCostAndSourceRulesCannotBeBypassed() {
        // 1. 构造数量正确但日期错位、交通不足、费用异常且来源缺失的草稿。
        var intent = completeIntent();
        var first = new TravelPlanItemDO(
                "09:00", "10:00", "陶瓷博物馆", "参观", "步行", 10,
                new BigDecimal("-1"), null, true, "");
        var second = new TravelPlanItemDO(
                "10:05", "11:00", "御窑博物馆", "参观", "驾车", 20,
                new BigDecimal("100"), null, false, "出发前查看官方公告");
        var draft = new TravelPlanDraftDO(
                "异常计划",
                "用于验证领域规则",
                List.of(
                        new TravelPlanDayDO("2026-10-02", "景德镇", List.of(first, second))),
                new BigDecimal("50"),
                List.of(),
                List.of(),
                List.of());

        // 2. 执行领域校验并核对每类反例均形成可定向修订的违规项。
        var result = new TravelPlanDomainService().validateDraft(
                new TravelPlanDraftValidationParam(intent, draft));

        assertTrue(result.success());
        assertFalse(result.data().valid());
        assertTrue(result.data().violations().stream().anyMatch(value -> value.contains("起止日期")));
        assertTrue(result.data().violations().stream().anyMatch(value -> value.contains("接驳时间")));
        assertTrue(result.data().violations().stream().anyMatch(value -> value.contains("负数")));
        assertTrue(result.data().violations().stream().anyMatch(value -> value.contains("关联来源")));
        assertTrue(result.data().violations().stream().anyMatch(value -> value.contains("待核实清单")));
    }

    private Fixture fixture() {
        // 1. 创建外部边界替身，并为预算、证据和调用登记提供成功结果。
        TravelContextService contextService = mock(TravelContextService.class);
        TravelEvidenceCollectorAppService evidence = mock(TravelEvidenceCollectorAppService.class);
        TravelPlanGenerationAdaptor plan = mock(TravelPlanGenerationAdaptor.class);
        RunExecutionService execution = mock(RunExecutionService.class);
        ChatRunEntity run = mock(ChatRunEntity.class);
        when(run.publicId()).thenReturn("run-graph");
        when(contextService.trustedContext(any())).thenReturn("可信上下文");
        when(contextService.ensureBudget(any(), any())).thenReturn(Result.success(Boolean.TRUE));
        when(evidence.collectDestinationEvidence(any())).thenReturn(Result.success(Boolean.TRUE));
        when(evidence.collectRealtimeFacts(any())).thenReturn(Result.success(Boolean.TRUE));
        when(execution.invocation(any(), any(), anyInt(), anyInt())).thenReturn("invocation");
        // 2. 使用真实 assembler、领域服务和八个节点形成可执行图。
        TravelPlanAppAssembler assembler = new TravelPlanAppAssembler(contextService);
        TravelPlanningContextRegistry registry = new TravelPlanningContextRegistry();
        var graph = new TravelPlanningGraph(
                new ValidateTravelRequestNode(
                        new TravelPlanDomainService(), contextService, assembler, registry),
                new CollectDestinationEvidenceNode(evidence, contextService, registry),
                new CollectRealtimeFactsNode(evidence, contextService, registry),
                new GenerateTravelDraftNode(plan, assembler, execution, registry),
                new ValidateTravelDraftNode(
                        new TravelPlanDomainService(), contextService, assembler, registry),
                new ReviseTravelDraftNode(plan, assembler, execution, registry),
                new RenderTravelPlanNode(assembler, contextService, registry),
                new ClarifyTravelRequirementsNode(assembler, contextService, registry),
                registry);
        return new Fixture(graph, evidence, plan, run);
    }

    private TravelConversationContext context(ChatRunEntity run, TravelIntentDO intent) {
        // 1. 建立已准备且完成意图识别的规划上下文。
        var context = new TravelConversationContext(run);
        context.load("请规划景德镇行程", List.of(), "", "");
        context.intent(intent);
        return context;
    }

    private TravelIntentDO completeIntent() {
        // 1. 建立一天、两人且有明确预算的完整旅行规划需求。
        return new TravelIntentDO(
                TravelIntentMode.PLANNING,
                "景德镇",
                "南昌",
                "景德镇",
                "2026-10-01",
                "2026-10-01",
                1,
                2,
                new BigDecimal("500"),
                List.of("陶瓷文化"),
                List.of(),
                true,
                true,
                true);
    }

    private TravelPlanDraftDO validDraft(BigDecimal total) {
        // 1. 建立具备来源、交通缓冲和费用明细的领域有效草稿。
        var first = new TravelPlanItemDO(
                "09:00", "10:00", "陶瓷博物馆", "参观", "步行", 10,
                new BigDecimal("20"), "official-1", true, "");
        var second = new TravelPlanItemDO(
                "10:30", "12:00", "御窑博物馆", "参观", "步行", 15,
                new BigDecimal("30"), "official-2", true, "");
        return new TravelPlanDraftDO(
                "景德镇一日行",
                "陶瓷文化主题路线",
                List.of(new TravelPlanDayDO("2026-10-01", "景德镇", List.of(first, second))),
                total,
                List.of("提前预约"),
                List.of("注意天气变化"),
                List.of());
    }

    private TravelPlanGenerationDO generation(TravelPlanDraftDO draft) {
        // 1. 组合草稿与不虚构usage的模型调用证据。
        return new TravelPlanGenerationDO(
                draft, new ChatModelDO("", null, null, null, "test-model"));
    }

    private record Fixture(
            TravelPlanningGraph graph,
            TravelEvidenceCollectorAppService evidence,
            TravelPlanGenerationAdaptor plan,
            ChatRunEntity run) {
    }
}
