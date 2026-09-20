package com.hellotravel.start;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.hellotravel.application.auth.AuthAppService;
import com.hellotravel.application.knowledge.KnowledgeAppService;
import com.hellotravel.application.chat.travel.TravelPlanningGraph;
import com.hellotravel.application.chat.travel.TravelAppService;
import com.hellotravel.domain.auth.service.AuthDomainService;
import com.hellotravel.domain.chat.service.ChatDomainService;
import com.hellotravel.domain.knowledge.service.KnowledgeDomainService;
import com.hellotravel.domain.memory.service.MemoryDomainService;
import com.hellotravel.domain.sync.service.SyncDomainService;
import com.hellotravel.domain.travel.service.TravelPlanDomainService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * 禁用调度与迁移，只检查真实八模块Bean能完整装配，不访问数据库或收费接口。
 *
 * @author AIGenerator
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "hello-travel.workers.enabled=false",
            "spring.flyway.enabled=false",
            "spring.datasource.url=jdbc:mysql://127.0.0.1:1/hello-travel",
            "spring.datasource.username=offline-check",
            "spring.datasource.password=offline-check",
            "spring.jmx.enabled=false"
        })
class OfflineAssemblyTest {

    /**
     * 保存context对应的有界运行状态。
     *
     * @author AIGenerator
     */
    private final ApplicationContext context;

    /**
     * 建立OfflineAssemblyTest并保存明确业务依赖。
     *
     * @author AIGenerator
     * @param context 受控context参数
     */
    @Autowired
    OfflineAssemblyTest(ApplicationContext context) {
        this.context = context;
    }

    @Test
    void realWiringIncludesDomainAndModelGraph() {
        assertNotNull(context.getBean(AuthDomainService.class));
        assertNotNull(context.getBean(ChatDomainService.class));
        assertNotNull(context.getBean(MemoryDomainService.class));
        assertNotNull(context.getBean(KnowledgeDomainService.class));
        assertNotNull(context.getBean(SyncDomainService.class));
        assertNotNull(context.getBean(TravelPlanDomainService.class));
        assertNotNull(context.getBean(TravelPlanningGraph.class));
        assertNotNull(context.getBean(TravelAppService.class));
        assertNotNull(context.getBean(AuthAppService.class));
        assertNotNull(context.getBean(KnowledgeAppService.class));
    }
}
