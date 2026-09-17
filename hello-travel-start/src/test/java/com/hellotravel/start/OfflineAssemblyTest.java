package com.hellotravel.start;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.hellotravel.application.auth.service.AuthApplication;
import com.hellotravel.application.knowledge.service.KnowledgeApplication;
import com.hellotravel.application.travel.workflow.TravelGraph;
import com.hellotravel.domain.persistence.service.TravelWriteDomainService;

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
        assertNotNull(context.getBean(TravelWriteDomainService.class));
        assertNotNull(context.getBean(TravelGraph.class));
        assertNotNull(context.getBean(AuthApplication.class));
        assertNotNull(context.getBean(KnowledgeApplication.class));
    }
}
