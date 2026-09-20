package com.hellotravel.application.chat.support;

import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.application.chat.assembler.SyncWriteAppAssembler;
import com.hellotravel.domain.sync.model.aggregate.OutboxEventAggregate;
import com.hellotravel.domain.sync.model.aggregate.SyncEventAggregate;
import com.hellotravel.domain.sync.service.SyncDomainService;

import org.springframework.stereotype.Component;

/**
 * 同步事务内部的领域写入协作；失败中断并交由所属应用入口捕获。
 *
 * @author AIGenerator
 */
@Component
public final class SyncWrites {

    private final SyncDomainService service;
    private final SyncWriteAppAssembler syncWriteAppAssembler;

    public SyncWrites(
            SyncDomainService service,
            SyncWriteAppAssembler syncWriteAppAssembler) {
        this.service = service;
        this.syncWriteAppAssembler = syncWriteAppAssembler;
    }

    /**
     * 校验SyncEvent完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveSyncEvent(SyncEventAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = syncWriteAppAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveSyncEvent(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return ApplicationFailures.required(result).saved();
    }

    /**
     * 清理SyncEvent的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeSyncEvent(Long id) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = syncWriteAppAssembler.removeSyncEvent(id);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.removeSyncEvent(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return ApplicationFailures.required(result).saved();
    }

    /**
     * 校验OutboxEvent完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveOutboxEvent(OutboxEventAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = syncWriteAppAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveOutboxEvent(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return ApplicationFailures.required(result).saved();
    }
}
