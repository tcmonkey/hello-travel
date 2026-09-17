package com.hellotravel.start.config;

import com.hellotravel.application.persistence.TravelRepositories;
import com.hellotravel.domain.persistence.service.TravelWriteDomainService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 领域模块没有Spring依赖，由启动模块显式装配领域服务。
 *
 * @author AIGenerator
 */
@Configuration
public class DomainConfiguration {

    /**
     * 显式装配不依赖Spring的领域写入服务。
     *
     * @author AIGenerator
     * @param repositories 受控repositories参数
     * @return 当前操作的业务结果
     */
    @Bean
    public TravelWriteDomainService travelWriteDomainService(TravelRepositories repositories) {
        return new TravelWriteDomainService(
                repositories.userAccount,
                repositories.device,
                repositories.loginSession,
                repositories.emailChallenge,
                repositories.conversation,
                repositories.message,
                repositories.chatRun,
                repositories.memorySummary,
                repositories.memoryFact,
                repositories.memoryFactSource,
                repositories.knowledgeDocument,
                repositories.knowledgeChunk,
                repositories.indexJob,
                repositories.syncEvent,
                repositories.outboxEvent,
                repositories.modelInvocation,
                repositories.refreshReceipt);
    }
}
