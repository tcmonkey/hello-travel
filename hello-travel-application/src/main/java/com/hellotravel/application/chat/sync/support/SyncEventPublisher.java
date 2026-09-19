package com.hellotravel.application.chat.sync.support;

import com.hellotravel.application.support.Json;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.domain.auth.model.entity.UserAccountEntity;
import com.hellotravel.domain.sync.model.aggregate.OutboxEventAggregate;
import com.hellotravel.domain.sync.model.aggregate.SyncEventAggregate;
import com.hellotravel.domain.sync.model.entity.OutboxEventEntity;
import com.hellotravel.domain.sync.model.entity.SyncEventEntity;

import org.springframework.stereotype.Component;

/**
 * 业务短事务内写有序事件和可靠发件箱；推送不是唯一事实源。
 *
 * @author AIGenerator
 */
@Component
public final class SyncEventPublisher {

    private final SyncWrites syncWrites;

    public SyncEventPublisher(SyncWrites syncWrites) {
        this.syncWrites = syncWrites;
    }

    /**
     * 在业务事务内保存账号连续事件及发件箱。
     *
     * @author AIGenerator
     * @param account 受控account参数
     * @param type 目标协议类型
     * @param publicId 受控publicId参数
     * @param version 受控version参数
     * @param target 受控target参数
     * @param payload 受控payload参数
     */
    public void append(
            UserAccountEntity account,
            String type,
            String publicId,
            long version,
            String target,
            String payload) {
        // 1. 通过领域聚合语义准备业务快照，固定状态由实体封装。
        SyncEventEntity event =
                SyncEventAggregate.committed(
                                account.id(),
                                account.syncSeq(),
                                type,
                                publicId,
                                version,
                                target,
                                payload,
                                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusHours(72))
                        .entity();
        // 2. 持久化当前完整聚合，失败必须中断事务而非继续提交。
        Transactions.require(syncWrites.saveSyncEvent(new SyncEventAggregate(event)));
        // 3. 同事务登记可恢复后台任务，外部调用在提交之后执行。
        outbox(
                account.id(),
                "SYNC",
                account.publicId() + ":" + account.syncSeq(),
                Json.encode(java.util.Map.of("seq", account.syncSeq())));
    }

    /**
     * 在当前事务内保存受限任务引用。
     *
     * @author AIGenerator
     * @param userId 认证账号主键
     * @param type 目标协议类型
     * @param key 受控key参数
     * @param payload 受控payload参数
     */
    public void outbox(Long userId, String type, String key, String payload) {
        // 1. 通过领域聚合语义准备业务快照，固定状态由实体封装。
        OutboxEventEntity event =
                OutboxEventAggregate.pending(
                                userId,
                                type,
                                key,
                                payload,
                                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC))
                        .entity();
        // 2. 持久化当前完整聚合，失败必须中断事务而非继续提交。
        Transactions.require(syncWrites.saveOutboxEvent(new OutboxEventAggregate(event)));
    }
}
