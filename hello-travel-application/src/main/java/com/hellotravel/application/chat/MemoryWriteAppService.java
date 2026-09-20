package com.hellotravel.application.chat;

import com.hellotravel.application.chat.assembler.MemoryWriteAppAssembler;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.domain.memory.model.aggregate.MemoryFactAggregate;
import com.hellotravel.domain.memory.model.aggregate.MemoryFactSourceAggregate;
import com.hellotravel.domain.memory.model.aggregate.MemorySummaryAggregate;
import com.hellotravel.domain.memory.service.MemoryDomainService;

import org.springframework.stereotype.Component;

/**
 * 记忆事务内部的领域写入协作；失败中断并交由所属应用入口捕获。
 *
 * @author AIGenerator
 */
@Component
public final class MemoryWriteAppService {

    private final MemoryDomainService service;
    private final MemoryWriteAppAssembler memoryWriteAppAssembler;

    public MemoryWriteAppService(
            MemoryDomainService service,
            MemoryWriteAppAssembler memoryWriteAppAssembler) {
        this.service = service;
        this.memoryWriteAppAssembler = memoryWriteAppAssembler;
    }

    /**
     * 校验MemorySummary完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveMemorySummary(MemorySummaryAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = memoryWriteAppAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveMemorySummary(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return ApplicationFailures.required(result).saved();
    }

    /**
     * 校验MemoryFact完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveMemoryFact(MemoryFactAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = memoryWriteAppAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveMemoryFact(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return ApplicationFailures.required(result).saved();
    }

    /**
     * 校验MemoryFactSource完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveMemoryFactSource(MemoryFactSourceAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = memoryWriteAppAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveMemoryFactSource(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return ApplicationFailures.required(result).saved();
    }

    /**
     * 清理MemoryFactSource的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeMemoryFactSource(Long id) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = memoryWriteAppAssembler.removeMemoryFactSource(id);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.removeMemoryFactSource(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return ApplicationFailures.required(result).saved();
    }
}
