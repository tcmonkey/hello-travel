package com.hellotravel.application.chat.sync.support;

import com.hellotravel.domain.sync.repository.OutboxEventRepository;
import com.hellotravel.domain.sync.repository.SyncEventRepository;

import org.springframework.stereotype.Component;

/**
 * 同步应用读取所需的本域仓储端口，不聚合其他业务仓储。
 *
 * @author AIGenerator
 */
@Component
public final class SyncRepositories {

    public final SyncEventRepository syncEvent;
    public final OutboxEventRepository outboxEvent;

    public SyncRepositories(SyncEventRepository syncEvent, OutboxEventRepository outboxEvent) {
        this.syncEvent = syncEvent;
        this.outboxEvent = outboxEvent;
    }
}
