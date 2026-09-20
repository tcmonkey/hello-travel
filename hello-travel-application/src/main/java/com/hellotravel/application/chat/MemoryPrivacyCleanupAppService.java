package com.hellotravel.application.chat;

import com.hellotravel.application.chat.support.ChatWrites;
import com.hellotravel.util.JsonUtil;
import com.hellotravel.application.chat.support.SyncEventPublisher;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.domain.chat.model.aggregate.MessageAggregate;
import com.hellotravel.domain.chat.repository.ConversationRepository;
import com.hellotravel.domain.chat.repository.MessageRepository;
import com.hellotravel.domain.memory.model.aggregate.MemoryFactAggregate;
import com.hellotravel.domain.memory.model.aggregate.MemorySummaryAggregate;
import com.hellotravel.domain.memory.repository.MemoryFactRepository;
import com.hellotravel.domain.memory.repository.MemoryFactSourceRepository;
import com.hellotravel.domain.memory.repository.MemorySummaryRepository;
import com.hellotravel.domain.query.model.value.QueryValue;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 删除代次先使记忆失效，再分批清除派生正文与证据，不恢复已删除对话。
 *
 * @author AIGenerator
 */
@Component
public final class MemoryPrivacyCleanupAppService {

    private final MemorySummaryRepository memorySummaryRepository;
    private final MemoryFactRepository memoryFactRepository;
    private final MemoryFactSourceRepository memoryFactSourceRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MemoryWriteAppService memoryWriteAppService;
    private final ChatWrites chatWrites;
    private final Transactions transactions;
    private final SyncEventPublisher events;

    public MemoryPrivacyCleanupAppService(
            MemoryWriteAppService memoryWriteAppService,
            ChatWrites chatWrites,
            MemorySummaryRepository memorySummaryRepository,
            MemoryFactRepository memoryFactRepository,
            MemoryFactSourceRepository memoryFactSourceRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            Transactions transactions,
            SyncEventPublisher events) {
        this.memoryWriteAppService = memoryWriteAppService;
        this.chatWrites = chatWrites;
        this.memorySummaryRepository = memorySummaryRepository;
        this.memoryFactRepository = memoryFactRepository;
        this.memoryFactSourceRepository = memoryFactSourceRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.transactions = transactions;
        this.events = events;
    }

    /**
     * 执行后台任务并保留租约与代次保护。
     *
     * @author AIGenerator
     * @param conversationId 受控conversationId参数
     */
    public void execute(Long conversationId) {
        // 1. 按可信内部标识读取对话当前快照。
        var stored = conversationRepository.findById(conversationId);
        // 2. 清理对象已不存在则安全结束，不重建被删除的记录。
        if (stored == null) {
            return;
        }
        // 3. 取得当前用例已核验的对话快照，供本段后续处理使用。
        var conversation = stored.entity();
        // 4. 进入受控事务处理，结果与回滚责任保持清晰。
        transactions.mutate(
                conversation.userId(),
                account -> {
                    // 1. 按可信内部标识读取对话当前快照。
                    var c = conversationRepository.findById(conversationId).entity();
                    // 2. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
                    for (var row :
                            memorySummaryRepository.query(
                                    QueryValue.all("id", 500)
                                            .where("conversation_id", "EQ", c.id())
                                            .where("memory_epoch", "LT", c.memoryEpoch())
                                            .where("status", "EQ", "ACTIVE"))) {
                        var old = row.entity();
                        var next = old.redacted();
                        Transactions.require(
                                memoryWriteAppService.saveMemorySummary(new MemorySummaryAggregate(next)));
                    }
                    // 3. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
                    for (var row :
                            memoryFactRepository.query(
                                    QueryValue.all("id", 500)
                                            .where("conversation_id", "EQ", c.id())
                                            .where("memory_epoch", "LT", c.memoryEpoch())
                                            .where("status", "EQ", "ACTIVE"))) {
                        var old = row.entity();
                        for (var source :
                                memoryFactSourceRepository.query(
                                        QueryValue.all("id", 1000)
                                                .where("fact_id", "EQ", old.id()))) {
                            Transactions.require(
                                    memoryWriteAppService.removeMemoryFactSource(source.entity().id()));
                        }
                        var next = old.redacted();
                        Transactions.require(
                                memoryWriteAppService.saveMemoryFact(new MemoryFactAggregate(next)));
                    }
                    // 4. 依据删除状态与当前记忆代次处理分支，避免继续使用无效数据。
                    if (c.deletedAt() != null) {
                        for (var row :
                                messageRepository.query(
                                        QueryValue.all("id", 500)
                                                .where("conversation_id", "EQ", c.id())
                                                .where("deleted_at", "NULL", null))) {
                            Transactions.require(
                                    chatWrites.saveMessage(
                                            new MessageAggregate(row.entity()).erase()));
                        }
                    }
                    // 5. 读取消息，按当前用例条件限定查询窗口。
                    boolean remaining =
                            !memoryFactRepository
                                            .query(
                                                    QueryValue.all("id", 1)
                                                            .where("conversation_id", "EQ", c.id())
                                                            .where(
                                                                    "memory_epoch",
                                                                    "LT",
                                                                    c.memoryEpoch())
                                                            .where("status", "EQ", "ACTIVE"))
                                            .isEmpty()
                                    || !memorySummaryRepository
                                            .query(
                                                    QueryValue.all("id", 1)
                                                            .where("conversation_id", "EQ", c.id())
                                                            .where(
                                                                    "memory_epoch",
                                                                    "LT",
                                                                    c.memoryEpoch())
                                                            .where("status", "EQ", "ACTIVE"))
                                            .isEmpty()
                                    || (c.deletedAt() != null
                                            && !messageRepository
                                                    .query(
                                                            QueryValue.all("id", 1)
                                                                    .where(
                                                                            "conversation_id",
                                                                            "EQ",
                                                                            c.id())
                                                                    .where(
                                                                            "deleted_at",
                                                                            "NULL",
                                                                            null))
                                                    .isEmpty());
                    // 6. 仍有待清理对象时重新登记任务，清理窗口保持有界且可恢复。
                    if (remaining) {
                        events.outbox(
                                c.userId(),
                                "MEMORY_REBUILD",
                                c.publicId() + ":" + c.memoryEpoch() + ":" + account.syncSeq(),
                                JsonUtil.encode(Map.of("conversationId", c.id())));
                    }
                    // 7. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                    events.append(account, "memory.cleaned", c.publicId(), c.version(), null, "{}");
                    // 8. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return null;
                });
    }
}
