package com.hellotravel.start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.adaptor.exception.AdaptorException;
import com.hellotravel.adaptor.auth.input.assembler.AuthAssembler;
import com.hellotravel.adaptor.auth.input.AuthController;
import com.hellotravel.adaptor.chat.input.assembler.ChatAssembler;
import com.hellotravel.adaptor.knowledge.input.assembler.KnowledgeAssembler;
import com.hellotravel.adaptor.knowledge.input.KnowledgeController;
import com.hellotravel.adaptor.knowledge.output.VectorOutAdaptorImpl;
import com.hellotravel.adaptor.common.AuthCookies;
import com.hellotravel.adaptor.common.HttpResults;
import com.hellotravel.adaptor.knowledge.output.converter.VectorConverter;
import com.hellotravel.adaptor.chat.output.TravelDialogueAdaptorImpl;
import com.hellotravel.adaptor.chat.output.aiservice.TravelDialogueAiService;
import com.hellotravel.adaptor.chat.output.TravelIntentRecognitionAdaptorImpl;
import com.hellotravel.adaptor.chat.output.aiservice.TravelIntentAiService;
import com.hellotravel.adaptor.chat.output.converter.ChatModelConverter;
import com.hellotravel.application.auth.result.AuthAppResult;
import com.hellotravel.application.auth.AuthAppService;
import com.hellotravel.application.auth.ChallengeStatusAppService;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.knowledge.assembler.KnowledgeAppAssembler;
import com.hellotravel.application.knowledge.policy.KnowledgeIndexPolicy;
import com.hellotravel.application.knowledge.command.VectorCommand;
import com.hellotravel.application.knowledge.command.VectorItemCommand;
import com.hellotravel.application.knowledge.KnowledgeAppService;
import com.hellotravel.application.chat.assembler.MemoryContextAppAssembler;
import com.hellotravel.application.chat.context.policy.ChatContextPolicy;
import com.hellotravel.application.chat.MemoryContextAppService;
import com.hellotravel.application.chat.TravelContextService;
import com.hellotravel.application.chat.support.TravelConversationContext;
import com.hellotravel.application.chat.command.TravelDialogueCommand;
import com.hellotravel.application.exception.RunExecutionService;
import com.hellotravel.application.chat.assembler.TravelIntentAppAssembler;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.client.auth.request.AuthRequest;
import com.hellotravel.client.chat.request.ChatRequest;
import com.hellotravel.client.knowledge.request.KnowledgeRequest;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.chat.model.entity.MessageEntity;
import com.hellotravel.domain.chat.repository.MessageRepository;
import com.hellotravel.model.chat.ChatModelStage;

import com.hellotravel.util.JsonUtil;
import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 映射边界真实协议回归；不使用外部模型、数据库或邮件服务。
 *
 * @author AIGenerator
 */
class MappingBoundaryTest {

    private static final String OWNER = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    private static final String DOCUMENT = "01ARZ3NDEKTSV4RRFFQ69G5FAW";

    @AfterEach
    void clearHttpContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void authenticationBindsCredentialsFromTheirTrustedSources() {
        var http = new MockHttpServletRequest();
        http.addHeader("Authorization", "Bearer access");
        http.addHeader("X-Session-ID", "page-sid");
        http.addHeader("X-CSRF-Token", "csrf");
        http.addHeader("ht_refresh", "forged-header");
        http.setCookies(new Cookie("ht_refresh", "real-cookie"));
        var command =
                new AuthAssembler()
                        .toCommand(
                                new AuthAssembler().action("refresh"),
                                new AuthRequest(null, null, null, null, null),
                                http,
                                "signed-device");
        assertEquals("REFRESH", command.action());
        assertEquals("page-sid", command.expectedSid());
        assertEquals("access", command.accessToken());
        assertEquals("real-cookie", command.refreshToken());
        assertEquals("signed-device", command.deviceKey());
        assertEquals("csrf", command.csrf());
    }

    @Test
    void unknownRoutesAreAdaptorErrorsAcrossEveryCommandEndpoint() {
        var http = new MockHttpServletRequest();
        var auth =
                assertThrows(
                        AdaptorException.class, () -> new AuthAssembler().action("unknown"));
        var chat =
                assertThrows(
                        AdaptorException.class,
                        () ->
                                new ChatAssembler()
                                        .toCommand("unknown", chatRequest(null, null), http));
        var knowledge =
                assertThrows(
                        AdaptorException.class,
                        () ->
                                new KnowledgeAssembler()
                                        .toCommand(
                                                "unknown",
                                                new KnowledgeRequest(null, null, null, null),
                                                http));
        assertEquals(AdaptorErrorCode.NOT_FOUND, auth.errorCode());
        assertEquals(AdaptorErrorCode.NOT_FOUND, chat.errorCode());
        assertEquals(AdaptorErrorCode.NOT_FOUND, knowledge.errorCode());
    }

    @Test
    void pagingDefaultsPreserveTrustedIdentityAndExplicitBounds() {
        var http = new MockHttpServletRequest();
        http.setAttribute("ht.user", 23L);
        http.setAttribute("ht.session", 47L);
        http.addHeader("ht.user", "999");
        var first = new ChatAssembler().toCommand("history", chatRequest(null, null), http);
        var next = new ChatAssembler().toCommand("history", chatRequest(30L, 20), http);
        assertEquals(23L, first.userId());
        assertEquals(47L, first.sessionId());
        assertEquals(0, first.after());
        assertEquals(100, first.limit());
        assertEquals(30, next.after());
        assertEquals(20, next.limit());
        assertEquals(300L, next.maxSeq());
        assertEquals(4L, next.historyEpoch());
    }

    @Test
    void checkDoesNotInheritSharedRefreshCredentials() {
        var http = new MockHttpServletRequest();
        http.addHeader("X-Session-ID", "old-page");
        http.addHeader("Authorization", "Bearer old-access");
        http.setCookies(new Cookie("ht_refresh", "new-page-refresh"));
        var check = new AuthAssembler().check(http);
        assertEquals("old-page", check.expectedSid());
        assertEquals("old-access", check.accessToken());
        assertNull(check.refreshToken());
        assertNull(check.deviceKey());
    }

    @Test
    void authenticationResponseExcludesInternalIdsAndRefreshSecret() throws Exception {
        var result =
                new AuthAppResult(
                        11L,
                        "public",
                        "a@example.com",
                        "sid",
                        22L,
                        "access",
                        "refresh-secret",
                        "csrf",
                        null);
        var json = new ObjectMapper().valueToTree(new AuthAssembler().toResponse(result));
        assertEquals(6, json.size());
        assertTrue(json.path("userId").isTextual());
        assertEquals("public", json.path("userId").asText());
        assertFalse(json.has("sessionId"));
        assertFalse(json.has("refreshToken"));
        assertEquals("access", json.path("accessToken").asText());
    }

    @Test
    void controllerPreservesLowerFailureAndDoesNotRotateCookies() {
        var http = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(http, response));
        var application = mock(AuthAppService.class);
        var cookies = mock(AuthCookies.class);
        when(cookies.device(http, response)).thenReturn("device");
        when(application.authenticate(any()))
                .thenReturn(Result.failure(DomainErrorCode.CONTEXT_LIMIT));
        var result =
                new AuthController(
                                application,
                                mock(ChallengeStatusAppService.class),
                                cookies,
                                new AuthAssembler())
                        .authenticate(
                                "login",
                                new AuthRequest("a@example.com", "password", null, null, null),
                                http,
                                response);
        assertFalse(result.success());
        assertEquals("CONTEXT_LIMIT", result.code());
        assertEquals(422, response.getStatus());
        verify(cookies, never()).writeSession(any(), any(), any());
    }

    @Test
    void layerOwnedFailuresAndLowerDomainFailuresRemainClassified() {
        assertEquals(
                "INVALID",
                HttpResults.capture(new AdaptorException(AdaptorErrorCode.INVALID)).code());
        assertEquals(
                "CONFLICT",
                ApplicationFailures.capture(new ApplicationException(ApplicationErrorCode.CONFLICT))
                        .code());
        assertEquals(
                "BUSY",
                ApplicationFailures.capture(new DomainException(DomainErrorCode.BUSY)).code());
        assertEquals(
                "FAILED",
                HttpResults.capture(new RuntimeException("private-technical-detail")).code());
        assertFalse(
                HttpResults.capture(new RuntimeException("private-technical-detail"))
                        .message()
                        .contains("private"));
    }

    @Test
    void oversizedUploadIsRejectedBeforeReadingBytesOrCallingApplication() throws IOException {
        var file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(10L * 1024 * 1024 + 1);
        var application = mock(KnowledgeAppService.class);
        var result =
                new KnowledgeController(application, new KnowledgeAssembler())
                        .upload(file, null, new MockHttpServletRequest());
        assertFalse(result.success());
        assertEquals("INVALID", result.code());
        verify(file, never()).getBytes();
        verifyNoInteractions(application);
    }

    @Test
    void emptyOrUnreadableUploadsUseAdaptorClassification() throws IOException {
        var http = new MockHttpServletRequest();
        assertEquals(
                AdaptorErrorCode.INVALID,
                assertThrows(
                                AdaptorException.class,
                                () ->
                                        new KnowledgeAssembler()
                                                .upload(
                                                        new MockMultipartFile("file", new byte[0]),
                                                        null,
                                                        http))
                        .errorCode());
        var file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(10L);
        when(file.getBytes()).thenThrow(new IOException("local-sensitive-path"));
        assertEquals(
                AdaptorErrorCode.FAILED,
                assertThrows(
                                AdaptorException.class,
                                () -> new KnowledgeAssembler().upload(file, null, http))
                        .errorCode());
    }

    @Test
    void vectorSearchAndCleanupAlwaysKeepOwnerAndGenerationFences() {
        var search =
                new VectorCommand(
                        "SEARCH",
                        OWNER,
                        null,
                        0L,
                        List.of(),
                        java.util.Collections.nCopies(VectorConverter.DIMENSIONS, 0.1F));
        assertEquals(
                "owner_user_id == \"" + OWNER + "\"",
                new VectorConverter()
                        .search(VectorConverter.COLLECTION, search)
                        .getFilter());
        var cleanup = new VectorCommand("RECONCILE", OWNER, DOCUMENT, 3L, List.of(), null);
        var filter =
                new VectorConverter()
                        .delete(VectorConverter.COLLECTION, cleanup)
                        .getFilter();
        assertTrue(filter.contains(OWNER));
        assertTrue(filter.contains(DOCUMENT));
        assertTrue(filter.endsWith("index_generation < 3"));
        var forged =
                new VectorCommand("DELETE", OWNER + "\" or true", DOCUMENT, 3L, List.of(), null);
        assertThrows(
                AdaptorException.class,
                () -> new VectorConverter().delete(VectorConverter.COLLECTION, forged));
    }

    @Test
    void vectorBatchRejectsCrossDocumentAndNonFiniteValues() {
        var item =
                new VectorItemCommand(
                        OWNER,
                        OWNER,
                        2L,
                        0,
                        "a".repeat(64),
                        java.util.Collections.nCopies(VectorConverter.DIMENSIONS, 0.1F));
        var cross = new VectorCommand("UPSERT", OWNER, DOCUMENT, 2L, List.of(item), null);
        assertThrows(
                AdaptorException.class,
                () -> new VectorConverter().upsert(VectorConverter.COLLECTION, cross));
        var bad =
                new VectorCommand(
                        "SEARCH",
                        OWNER,
                        null,
                        0L,
                        List.of(),
                        java.util.Collections.nCopies(VectorConverter.DIMENSIONS, Float.NaN));
        assertThrows(
                AdaptorException.class,
                () -> new VectorConverter().search(VectorConverter.COLLECTION, bad));
    }

    @Test
    void travelPlanningContextUsesExplicitHistoryProjection() {
        var time = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
        var message =
                new MessageEntity(
                        1L,
                        "message",
                        2L,
                        3L,
                        4L,
                        "USER",
                        "COMPLETED",
                        "景德镇",
                        null,
                        null,
                        time,
                        time,
                        0L);
        var travelContext = new TravelConversationContext(mock(com.hellotravel.domain.chat.model.entity.ChatRunEntity.class));
        travelContext.load("景德镇", List.of(message), "trusted-rule", "");
        var contextService =
                new TravelContextService(
                        mock(MessageRepository.class),
                        mock(RunExecutionService.class),
                        mock(MemoryContextAppService.class),
                        mock(ChatContextPolicy.class),
                        mock(MemoryContextAppAssembler.class));
        String context = contextService.trustedContext(travelContext);
        assertTrue(context.contains("会话摘要：trusted-rule"));
        assertTrue(context.contains("历史-USER: 景德镇"));
        assertFalse(context.contains("MessageEntity{redacted}"));
    }

    @Test
    void invalidAuthRouteDoesNotIssueDeviceOrCallApplication() {
        var application = mock(AuthAppService.class);
        var cookies = mock(AuthCookies.class);
        var result =
                new AuthController(
                                application,
                                mock(ChallengeStatusAppService.class),
                                cookies,
                                new AuthAssembler())
                        .authenticate(
                                "unknown",
                                new AuthRequest(null, null, null, null, null),
                                new MockHttpServletRequest(),
                                new MockHttpServletResponse());
        assertEquals("NOT_FOUND", result.code());
        verifyNoInteractions(application, cookies);
    }

    @Test
    void configuredBudgetDisplayAndEnforcementUseOnePolicy() {
        var policy = new ChatContextPolicy(20000, 3000, 2000, 10000);
        var budget = policy.budget(15000);
        assertTrue(budget.fits());
        assertFalse(policy.budget(15001).fits());
        var initial =
                JsonUtil.read(
                        new MemoryContextAppAssembler().initial(policy));
        assertEquals(20000, initial.path("window").asInt());
        assertEquals(policy.outputReserve(), initial.path("outputReserve").asInt());
        assertEquals(2000, initial.path("safetyReserve").asInt());
        assertEquals(0, initial.path("inputEstimate").asInt());
        assertFalse(initial.has("actualInputTokens"));
        assertThrows(
                ApplicationException.class,
                () ->
                        new ChatContextPolicy(20000, 3000, 2000, 19000));
    }

    @Test
    void knowledgeMetadataAndBothProviderConvertersUseOneIndexProfile() {
        var command =
                new com.hellotravel.application.knowledge.command.KnowledgeCommand(
                        "UPLOAD",
                        23L,
                        null,
                        "guide.txt",
                        new byte[] {1},
                        "https://example.com",
                        null,
                        0,
                        100);
        var parsed =
                new com.hellotravel.model.knowledge.FileDO(
                        "file-key", "text/plain", new byte[] {2}, "policy");
        var document = new KnowledgeAppAssembler().received(command, parsed).entity();
        assertEquals(KnowledgeIndexPolicy.model(), document.embeddingModel());
        assertEquals(KnowledgeIndexPolicy.dimensions(), document.embeddingDimension());
        assertEquals(VectorConverter.COLLECTION, document.collectionName());
        assertEquals(document.createdAt(), document.updatedAt());
    }

    @Test
    void configuredBudgetValuesRemainAnImmutablePolicy() {
        var policy = new ChatContextPolicy(20000, 3000, 2000, 10000);
        assertEquals(20000, policy.budget(0).window());
        assertEquals(3000, policy.outputReserve());
        assertEquals(10000, policy.compressionInputLimit());
    }

    @Test
    void outputBoundaryKeepsConverterClassificationAndRejectsBeforeIO() {
        var vector =
                new VectorOutAdaptorImpl(
                        "http://127.0.0.1:19530", "", new VectorConverter());
        var result =
                vector.index(
                        new VectorCommand("DELETE", "forged-id", DOCUMENT, 1L, List.of(), null));
        assertEquals("INVALID", result.code());
        var aiService = mock(TravelIntentAiService.class);
        var agent =
                new TravelIntentRecognitionAdaptorImpl(
                        aiService, new ChatContextPolicy(131072, 8192, 4096, 96000));
        var rejected =
                agent.recognize(new TravelIntentAppAssembler().command("X".repeat(131072)));
        assertEquals("CONTEXT_LIMIT", rejected.code());
        verifyNoInteractions(aiService);
    }

    @Test
    void oversizedChatAndIntentAreRejectedBeforePaidProviderIO() {
        var policy = new ChatContextPolicy(131072, 8192, 4096, 96000);
        var aiService = mock(TravelIntentAiService.class);
        var provider = new TravelIntentRecognitionAdaptorImpl(aiService, policy);
        assertEquals(
                "CONTEXT_LIMIT",
                provider.recognize(new TravelIntentAppAssembler().command("X".repeat(131072))).code());
        verifyNoInteractions(aiService);
        var small = new ChatContextPolicy(131072, 1000, 4096, 96000);
        assertEquals(1000, small.outputLimit(ChatModelStage.INTENT));
    }

    @Test
    void travelPlanningAiServicePreservesAccumulatedStreamingProgress() {
        var aiService = mock(TravelDialogueAiService.class);
        when(aiService.answer("景德镇", "instructions", "context"))
                .thenReturn(reactor.core.publisher.Flux.just("你", "好"));
        var latest = new java.util.concurrent.atomic.AtomicReference<String>();
        var agent =
                new TravelDialogueAdaptorImpl(
                        aiService,
                        new ChatContextPolicy(131072, 8192, 4096, 96000),
                        new ChatModelConverter(),
                        "qwen-plus");

        var result =
                agent.answer(
                        new TravelDialogueCommand(
                                "景德镇",
                                "instructions",
                                "context",
                                text -> {
                                    latest.set(text);
                                    return true;
                                }));

        assertTrue(result.success());
        assertEquals("你好", result.data().text());
        assertEquals("你好", latest.get());
    }

    private ChatRequest chatRequest(Long after, Integer limit) {
        return new ChatRequest(
                "conversation", null, null, null, null, null, null, 300L, 4L, after, limit);
    }
}
