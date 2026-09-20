package com.hellotravel.start;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hellotravel.adaptor.knowledge.input.assembler.KnowledgeAssembler;
import com.hellotravel.adaptor.knowledge.input.KnowledgeController;
import com.hellotravel.application.auth.AuthAppService;
import com.hellotravel.application.chat.ChatAppService;
import com.hellotravel.application.knowledge.KnowledgeAppService;
import com.hellotravel.application.chat.SyncAppService;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.auth.service.AuthDomainService;
import com.hellotravel.domain.chat.service.ChatDomainService;
import com.hellotravel.domain.knowledge.service.KnowledgeDomainService;
import com.hellotravel.domain.memory.service.MemoryDomainService;
import com.hellotravel.domain.sync.service.SyncDomainService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

/**
 * 四层入口修订的有限失败检查，不访问数据库、模型或SMTP。
 *
 * @author AIGenerator
 */
class BoundaryFailureTest {

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void uploadIOExceptionReturnsFailureWithoutLeakingOrThrowing() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenThrow(new IOException("private-storage-path"));
        var controller =
                new KnowledgeController(
                        mock(KnowledgeAppService.class), new KnowledgeAssembler());
        var result = assertDoesNotThrow(() -> controller.upload(file, null, request));
        assertFalse(result.success());
        assertEquals("FAILED", result.code());
        assertEquals(500, response.getStatus());
        assertFalse(result.message().contains("private-storage-path"));
    }

    @Test
    void invalidUploadReturns400AtControllerBoundary() {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);
        var controller =
                new KnowledgeController(
                        mock(KnowledgeAppService.class), new KnowledgeAssembler());
        var result = assertDoesNotThrow(() -> controller.upload(file, null, request));
        assertEquals("INVALID", result.code());
        assertEquals(400, response.getStatus());
    }

    @Test
    void allApplicationAndDomainEntriesContainCollaboratorFailures() throws Exception {
        int checked = 0;
        for (Class<?> type :
                List.of(
                        AuthAppService.class,
                        ChatAppService.class,
                        KnowledgeAppService.class,
                        SyncAppService.class,
                        AuthDomainService.class,
                        ChatDomainService.class,
                        MemoryDomainService.class,
                        KnowledgeDomainService.class,
                        SyncDomainService.class)) {
            var constructor = type.getConstructors()[0];
            Object[] dependencies =
                    Arrays.stream(constructor.getParameterTypes())
                            .map(BoundaryFailureTest::failingDependency)
                            .toArray();
            Object service = constructor.newInstance(dependencies);
            for (var method : type.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers())
                        && method.getReturnType() == Result.class) {
                    Result<?> result =
                            (Result<?>)
                                    method.invoke(service, new Object[method.getParameterCount()]);
                    assertFalse(result.success(), type.getSimpleName() + "." + method.getName());
                    assertFalse(result.message().contains("private-collaborator-error"));
                    checked++;
                }
            }
        }
        assertEquals(24, checked);
    }

    private static Object failingDependency(Class<?> type) {
        if (type == List.class) {
            return List.of();
        }
        return mock(
                type,
                invocation -> {
                    throw new IllegalStateException("private-collaborator-error");
                });
    }
}
