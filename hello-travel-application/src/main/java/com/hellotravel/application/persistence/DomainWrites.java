package com.hellotravel.application.persistence;

import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.persistence.assembler.TravelWriteApplicationAssembler;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.auth.model.aggregate.DeviceAggregate;
import com.hellotravel.domain.auth.model.aggregate.EmailChallengeAggregate;
import com.hellotravel.domain.auth.model.aggregate.LoginSessionAggregate;
import com.hellotravel.domain.auth.model.aggregate.RefreshReceiptAggregate;
import com.hellotravel.domain.auth.model.aggregate.UserAccountAggregate;
import com.hellotravel.domain.chat.model.aggregate.ChatRunAggregate;
import com.hellotravel.domain.chat.model.aggregate.ConversationAggregate;
import com.hellotravel.domain.chat.model.aggregate.MessageAggregate;
import com.hellotravel.domain.chat.model.aggregate.ModelInvocationAggregate;
import com.hellotravel.domain.knowledge.model.aggregate.IndexJobAggregate;
import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeChunkAggregate;
import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeDocumentAggregate;
import com.hellotravel.domain.memory.model.aggregate.MemoryFactAggregate;
import com.hellotravel.domain.memory.model.aggregate.MemoryFactSourceAggregate;
import com.hellotravel.domain.memory.model.aggregate.MemorySummaryAggregate;
import com.hellotravel.domain.persistence.service.TravelWriteDomainService;
import com.hellotravel.domain.sync.model.aggregate.OutboxEventAggregate;
import com.hellotravel.domain.sync.model.aggregate.SyncEventAggregate;
import com.hellotravel.model.persistence.WriteDO;

import org.springframework.stereotype.Component;

/**
 * 写用例必须经过领域服务；失败抛出事务边界以保证整体回滚。
 *
 * @author AIGenerator
 */
@Component
public final class DomainWrites {

    private final TravelWriteDomainService service;

    private final TravelWriteApplicationAssembler travelWriteApplicationAssembler;

    public DomainWrites(
            TravelWriteDomainService service,
            TravelWriteApplicationAssembler travelWriteApplicationAssembler) {
        this.service = service;
        this.travelWriteApplicationAssembler = travelWriteApplicationAssembler;
    }

    /**
     * 校验UserAccount完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveUserAccount(UserAccountAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveUserAccount(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 清理UserAccount的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeUserAccount(Long id) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.removeUserAccount(id);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.removeUserAccount(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 校验Device完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveDevice(DeviceAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveDevice(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 清理Device的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeDevice(Long id) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.removeDevice(id);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.removeDevice(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 校验LoginSession完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveLoginSession(LoginSessionAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveLoginSession(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 清理LoginSession的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeLoginSession(Long id) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.removeLoginSession(id);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.removeLoginSession(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 校验EmailChallenge完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveEmailChallenge(EmailChallengeAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveEmailChallenge(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 清理EmailChallenge的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeEmailChallenge(Long id) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.removeEmailChallenge(id);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.removeEmailChallenge(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 校验Conversation完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveConversation(ConversationAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveConversation(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 清理Conversation的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeConversation(Long id) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.removeConversation(id);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.removeConversation(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 校验Message完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveMessage(MessageAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveMessage(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 清理Message的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeMessage(Long id) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.removeMessage(id);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.removeMessage(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 校验ChatRun完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveChatRun(ChatRunAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveChatRun(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 清理ChatRun的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeChatRun(Long id) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.removeChatRun(id);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.removeChatRun(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
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
        var param = travelWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveMemorySummary(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 清理MemorySummary的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeMemorySummary(Long id) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.removeMemorySummary(id);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.removeMemorySummary(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
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
        var param = travelWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveMemoryFact(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 清理MemoryFact的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeMemoryFact(Long id) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.removeMemoryFact(id);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.removeMemoryFact(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
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
        var param = travelWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveMemoryFactSource(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
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
        var param = travelWriteApplicationAssembler.removeMemoryFactSource(id);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.removeMemoryFactSource(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 校验KnowledgeDocument完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveKnowledgeDocument(KnowledgeDocumentAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveKnowledgeDocument(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 清理KnowledgeDocument的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeKnowledgeDocument(Long id) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.removeKnowledgeDocument(id);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.removeKnowledgeDocument(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 校验KnowledgeChunk完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveKnowledgeChunk(KnowledgeChunkAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveKnowledgeChunk(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 清理KnowledgeChunk的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeKnowledgeChunk(Long id) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.removeKnowledgeChunk(id);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.removeKnowledgeChunk(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 校验IndexJob完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveIndexJob(IndexJobAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveIndexJob(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 清理IndexJob的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeIndexJob(Long id) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.removeIndexJob(id);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.removeIndexJob(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
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
        var param = travelWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveSyncEvent(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
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
        var param = travelWriteApplicationAssembler.removeSyncEvent(id);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.removeSyncEvent(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
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
        var param = travelWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveOutboxEvent(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 清理OutboxEvent的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeOutboxEvent(Long id) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.removeOutboxEvent(id);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.removeOutboxEvent(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 校验ModelInvocation完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveModelInvocation(ModelInvocationAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveModelInvocation(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 清理ModelInvocation的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeModelInvocation(Long id) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.removeModelInvocation(id);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.removeModelInvocation(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 校验RefreshReceipt完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveRefreshReceipt(RefreshReceiptAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveRefreshReceipt(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    /**
     * 清理RefreshReceipt的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeRefreshReceipt(Long id) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = travelWriteApplicationAssembler.removeRefreshReceipt(id);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.removeRefreshReceipt(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return required(result).saved();
    }

    private static WriteDO required(Result<WriteDO> result) {
        // 1. 核对下层标准结果的成功状态，失败中止当前处理。
        if (!result.success()) {
            throw new ApplicationException(ApplicationErrorCode.valueOf(result.code()));
        }
        // 2. 返回由领域服务成功结果转换的写标记，调用者据此确认持久化。
        return result.data();
    }
}
