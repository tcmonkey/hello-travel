package com.hellotravel.domain.persistence.service;

import com.hellotravel.common.error.Failures;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.annotation.DomainService;
import com.hellotravel.domain.auth.repository.DeviceRepository;
import com.hellotravel.domain.auth.repository.EmailChallengeRepository;
import com.hellotravel.domain.auth.repository.LoginSessionRepository;
import com.hellotravel.domain.auth.repository.RefreshReceiptRepository;
import com.hellotravel.domain.auth.repository.UserAccountRepository;
import com.hellotravel.domain.chat.repository.ChatRunRepository;
import com.hellotravel.domain.chat.repository.ConversationRepository;
import com.hellotravel.domain.chat.repository.MessageRepository;
import com.hellotravel.domain.chat.repository.ModelInvocationRepository;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.knowledge.repository.IndexJobRepository;
import com.hellotravel.domain.knowledge.repository.KnowledgeChunkRepository;
import com.hellotravel.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.hellotravel.domain.memory.repository.MemoryFactRepository;
import com.hellotravel.domain.memory.repository.MemoryFactSourceRepository;
import com.hellotravel.domain.memory.repository.MemorySummaryRepository;
import com.hellotravel.domain.persistence.model.param.ChatRunRemoveParam;
import com.hellotravel.domain.persistence.model.param.ChatRunWriteParam;
import com.hellotravel.domain.persistence.model.param.ConversationRemoveParam;
import com.hellotravel.domain.persistence.model.param.ConversationWriteParam;
import com.hellotravel.domain.persistence.model.param.DeviceRemoveParam;
import com.hellotravel.domain.persistence.model.param.DeviceWriteParam;
import com.hellotravel.domain.persistence.model.param.EmailChallengeRemoveParam;
import com.hellotravel.domain.persistence.model.param.EmailChallengeWriteParam;
import com.hellotravel.domain.persistence.model.param.IndexJobRemoveParam;
import com.hellotravel.domain.persistence.model.param.IndexJobWriteParam;
import com.hellotravel.domain.persistence.model.param.KnowledgeChunkRemoveParam;
import com.hellotravel.domain.persistence.model.param.KnowledgeChunkWriteParam;
import com.hellotravel.domain.persistence.model.param.KnowledgeDocumentRemoveParam;
import com.hellotravel.domain.persistence.model.param.KnowledgeDocumentWriteParam;
import com.hellotravel.domain.persistence.model.param.LoginSessionRemoveParam;
import com.hellotravel.domain.persistence.model.param.LoginSessionWriteParam;
import com.hellotravel.domain.persistence.model.param.MemoryFactRemoveParam;
import com.hellotravel.domain.persistence.model.param.MemoryFactSourceRemoveParam;
import com.hellotravel.domain.persistence.model.param.MemoryFactSourceWriteParam;
import com.hellotravel.domain.persistence.model.param.MemoryFactWriteParam;
import com.hellotravel.domain.persistence.model.param.MemorySummaryRemoveParam;
import com.hellotravel.domain.persistence.model.param.MemorySummaryWriteParam;
import com.hellotravel.domain.persistence.model.param.MessageRemoveParam;
import com.hellotravel.domain.persistence.model.param.MessageWriteParam;
import com.hellotravel.domain.persistence.model.param.ModelInvocationRemoveParam;
import com.hellotravel.domain.persistence.model.param.ModelInvocationWriteParam;
import com.hellotravel.domain.persistence.model.param.OutboxEventRemoveParam;
import com.hellotravel.domain.persistence.model.param.OutboxEventWriteParam;
import com.hellotravel.domain.persistence.model.param.RefreshReceiptRemoveParam;
import com.hellotravel.domain.persistence.model.param.RefreshReceiptWriteParam;
import com.hellotravel.domain.persistence.model.param.SyncEventRemoveParam;
import com.hellotravel.domain.persistence.model.param.SyncEventWriteParam;
import com.hellotravel.domain.persistence.model.param.UserAccountRemoveParam;
import com.hellotravel.domain.persistence.model.param.UserAccountWriteParam;
import com.hellotravel.domain.sync.repository.OutboxEventRepository;
import com.hellotravel.domain.sync.repository.SyncEventRepository;
import com.hellotravel.model.persistence.WriteDO;

/**
 * 完整聚合写入关口：归属不可迁移、删除不可恢复、版本必须匹配。
 *
 * @author AIGenerator
 */
@DomainService
public final class TravelWriteDomainService {

    private final UserAccountRepository userAccount;
    private final DeviceRepository device;
    private final LoginSessionRepository loginSession;
    private final EmailChallengeRepository emailChallenge;
    private final ConversationRepository conversation;
    private final MessageRepository message;
    private final ChatRunRepository chatRun;
    private final MemorySummaryRepository memorySummary;
    private final MemoryFactRepository memoryFact;
    private final MemoryFactSourceRepository memoryFactSource;
    private final KnowledgeDocumentRepository knowledgeDocument;
    private final KnowledgeChunkRepository knowledgeChunk;
    private final IndexJobRepository indexJob;
    private final SyncEventRepository syncEvent;
    private final OutboxEventRepository outboxEvent;
    private final ModelInvocationRepository modelInvocation;
    private final RefreshReceiptRepository refreshReceipt;

    public TravelWriteDomainService(
            UserAccountRepository userAccount,
            DeviceRepository device,
            LoginSessionRepository loginSession,
            EmailChallengeRepository emailChallenge,
            ConversationRepository conversation,
            MessageRepository message,
            ChatRunRepository chatRun,
            MemorySummaryRepository memorySummary,
            MemoryFactRepository memoryFact,
            MemoryFactSourceRepository memoryFactSource,
            KnowledgeDocumentRepository knowledgeDocument,
            KnowledgeChunkRepository knowledgeChunk,
            IndexJobRepository indexJob,
            SyncEventRepository syncEvent,
            OutboxEventRepository outboxEvent,
            ModelInvocationRepository modelInvocation,
            RefreshReceiptRepository refreshReceipt) {
        this.userAccount = userAccount;
        this.device = device;
        this.loginSession = loginSession;
        this.emailChallenge = emailChallenge;
        this.conversation = conversation;
        this.message = message;
        this.chatRun = chatRun;
        this.memorySummary = memorySummary;
        this.memoryFact = memoryFact;
        this.memoryFactSource = memoryFactSource;
        this.knowledgeDocument = knowledgeDocument;
        this.knowledgeChunk = knowledgeChunk;
        this.indexJob = indexJob;
        this.syncEvent = syncEvent;
        this.outboxEvent = outboxEvent;
        this.modelInvocation = modelInvocation;
        this.refreshReceipt = refreshReceipt;
    }

    /**
     * 校验UserAccount完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveUserAccount(UserAccountWriteParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.aggregate() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 准备当前变更后的不可变快照，旧版本保留给仓储CAS。
            var next = param.aggregate();
            // 3. 校验聚合完整性，空实体不能进入仓储。
            next.assertComplete();
            // 4. 更新前加载已持久化聚合，新增快照无需虚构旧记录。
            if (next.idForPersistence() != null) {
                var stored = userAccount.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(userAccount.save(next));
            // 6. 拒绝未写入的CAS结果，使所属事务回滚而非继续发布事件。
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            // 7. 返回实际成功写入标记，版本或归属校验失败不会走到此处。
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 清理UserAccount的指定失效记录。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> removeUserAccount(UserAccountRemoveParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 返回实际删除标记，不把空匹配伪造成已删除记录。
            return Result.success(new WriteDO(Boolean.TRUE.equals(userAccount.remove(param.id()))));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 校验Device完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveDevice(DeviceWriteParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.aggregate() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 准备当前变更后的不可变快照，旧版本保留给仓储CAS。
            var next = param.aggregate();
            // 3. 校验聚合完整性，空实体不能进入仓储。
            next.assertComplete();
            // 4. 更新前加载已持久化聚合，新增快照无需虚构旧记录。
            if (next.idForPersistence() != null) {
                var stored = device.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(device.save(next));
            // 6. 拒绝未写入的CAS结果，使所属事务回滚而非继续发布事件。
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            // 7. 返回实际成功写入标记，版本或归属校验失败不会走到此处。
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 清理Device的指定失效记录。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> removeDevice(DeviceRemoveParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 返回实际删除标记，不把空匹配伪造成已删除记录。
            return Result.success(new WriteDO(Boolean.TRUE.equals(device.remove(param.id()))));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 校验LoginSession完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveLoginSession(LoginSessionWriteParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.aggregate() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 准备当前变更后的不可变快照，旧版本保留给仓储CAS。
            var next = param.aggregate();
            // 3. 校验聚合完整性，空实体不能进入仓储。
            next.assertComplete();
            // 4. 更新前加载已持久化聚合，新增快照无需虚构旧记录。
            if (next.idForPersistence() != null) {
                var stored = loginSession.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(loginSession.save(next));
            // 6. 拒绝未写入的CAS结果，使所属事务回滚而非继续发布事件。
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            // 7. 返回实际成功写入标记，版本或归属校验失败不会走到此处。
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 清理LoginSession的指定失效记录。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> removeLoginSession(LoginSessionRemoveParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 返回实际删除标记，不把空匹配伪造成已删除记录。
            return Result.success(
                    new WriteDO(Boolean.TRUE.equals(loginSession.remove(param.id()))));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 校验EmailChallenge完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveEmailChallenge(EmailChallengeWriteParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.aggregate() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 准备当前变更后的不可变快照，旧版本保留给仓储CAS。
            var next = param.aggregate();
            // 3. 校验聚合完整性，空实体不能进入仓储。
            next.assertComplete();
            // 4. 更新前加载已持久化聚合，新增快照无需虚构旧记录。
            if (next.idForPersistence() != null) {
                var stored = emailChallenge.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(emailChallenge.save(next));
            // 6. 拒绝未写入的CAS结果，使所属事务回滚而非继续发布事件。
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            // 7. 返回实际成功写入标记，版本或归属校验失败不会走到此处。
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 清理EmailChallenge的指定失效记录。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> removeEmailChallenge(EmailChallengeRemoveParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 返回实际删除标记，不把空匹配伪造成已删除记录。
            return Result.success(
                    new WriteDO(Boolean.TRUE.equals(emailChallenge.remove(param.id()))));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 校验Conversation完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveConversation(ConversationWriteParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.aggregate() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 准备当前变更后的不可变快照，旧版本保留给仓储CAS。
            var next = param.aggregate();
            // 3. 校验聚合完整性，空实体不能进入仓储。
            next.assertComplete();
            // 4. 更新前加载已持久化聚合，新增快照无需虚构旧记录。
            if (next.idForPersistence() != null) {
                var stored = conversation.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(conversation.save(next));
            // 6. 拒绝未写入的CAS结果，使所属事务回滚而非继续发布事件。
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            // 7. 返回实际成功写入标记，版本或归属校验失败不会走到此处。
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 清理Conversation的指定失效记录。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> removeConversation(ConversationRemoveParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 返回实际删除标记，不把空匹配伪造成已删除记录。
            return Result.success(
                    new WriteDO(Boolean.TRUE.equals(conversation.remove(param.id()))));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 校验Message完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveMessage(MessageWriteParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.aggregate() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 准备当前变更后的不可变快照，旧版本保留给仓储CAS。
            var next = param.aggregate();
            // 3. 校验聚合完整性，空实体不能进入仓储。
            next.assertComplete();
            // 4. 更新前加载已持久化聚合，新增快照无需虚构旧记录。
            if (next.idForPersistence() != null) {
                var stored = message.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(message.save(next));
            // 6. 拒绝未写入的CAS结果，使所属事务回滚而非继续发布事件。
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            // 7. 返回实际成功写入标记，版本或归属校验失败不会走到此处。
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 清理Message的指定失效记录。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> removeMessage(MessageRemoveParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 返回实际删除标记，不把空匹配伪造成已删除记录。
            return Result.success(new WriteDO(Boolean.TRUE.equals(message.remove(param.id()))));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 校验ChatRun完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveChatRun(ChatRunWriteParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.aggregate() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 准备当前变更后的不可变快照，旧版本保留给仓储CAS。
            var next = param.aggregate();
            // 3. 校验聚合完整性，空实体不能进入仓储。
            next.assertComplete();
            // 4. 更新前加载已持久化聚合，新增快照无需虚构旧记录。
            if (next.idForPersistence() != null) {
                var stored = chatRun.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(chatRun.save(next));
            // 6. 拒绝未写入的CAS结果，使所属事务回滚而非继续发布事件。
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            // 7. 返回实际成功写入标记，版本或归属校验失败不会走到此处。
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 清理ChatRun的指定失效记录。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> removeChatRun(ChatRunRemoveParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 返回实际删除标记，不把空匹配伪造成已删除记录。
            return Result.success(new WriteDO(Boolean.TRUE.equals(chatRun.remove(param.id()))));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 校验MemorySummary完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveMemorySummary(MemorySummaryWriteParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.aggregate() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 准备当前变更后的不可变快照，旧版本保留给仓储CAS。
            var next = param.aggregate();
            // 3. 校验聚合完整性，空实体不能进入仓储。
            next.assertComplete();
            // 4. 更新前加载已持久化聚合，新增快照无需虚构旧记录。
            if (next.idForPersistence() != null) {
                var stored = memorySummary.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(memorySummary.save(next));
            // 6. 拒绝未写入的CAS结果，使所属事务回滚而非继续发布事件。
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            // 7. 返回实际成功写入标记，版本或归属校验失败不会走到此处。
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 清理MemorySummary的指定失效记录。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> removeMemorySummary(MemorySummaryRemoveParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 返回实际删除标记，不把空匹配伪造成已删除记录。
            return Result.success(
                    new WriteDO(Boolean.TRUE.equals(memorySummary.remove(param.id()))));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 校验MemoryFact完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveMemoryFact(MemoryFactWriteParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.aggregate() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 准备当前变更后的不可变快照，旧版本保留给仓储CAS。
            var next = param.aggregate();
            // 3. 校验聚合完整性，空实体不能进入仓储。
            next.assertComplete();
            // 4. 更新前加载已持久化聚合，新增快照无需虚构旧记录。
            if (next.idForPersistence() != null) {
                var stored = memoryFact.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(memoryFact.save(next));
            // 6. 拒绝未写入的CAS结果，使所属事务回滚而非继续发布事件。
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            // 7. 返回实际成功写入标记，版本或归属校验失败不会走到此处。
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 清理MemoryFact的指定失效记录。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> removeMemoryFact(MemoryFactRemoveParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 返回实际删除标记，不把空匹配伪造成已删除记录。
            return Result.success(new WriteDO(Boolean.TRUE.equals(memoryFact.remove(param.id()))));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 校验MemoryFactSource完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveMemoryFactSource(MemoryFactSourceWriteParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.aggregate() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 准备当前变更后的不可变快照，旧版本保留给仓储CAS。
            var next = param.aggregate();
            // 3. 校验聚合完整性，空实体不能进入仓储。
            next.assertComplete();
            // 4. 更新前加载已持久化聚合，新增快照无需虚构旧记录。
            if (next.idForPersistence() != null) {
                var stored = memoryFactSource.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(memoryFactSource.save(next));
            // 6. 拒绝未写入的CAS结果，使所属事务回滚而非继续发布事件。
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            // 7. 返回实际成功写入标记，版本或归属校验失败不会走到此处。
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 清理MemoryFactSource的指定失效记录。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> removeMemoryFactSource(MemoryFactSourceRemoveParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 返回实际删除标记，不把空匹配伪造成已删除记录。
            return Result.success(
                    new WriteDO(Boolean.TRUE.equals(memoryFactSource.remove(param.id()))));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 校验KnowledgeDocument完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveKnowledgeDocument(KnowledgeDocumentWriteParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.aggregate() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 准备当前变更后的不可变快照，旧版本保留给仓储CAS。
            var next = param.aggregate();
            // 3. 校验聚合完整性，空实体不能进入仓储。
            next.assertComplete();
            // 4. 更新前加载已持久化聚合，新增快照无需虚构旧记录。
            if (next.idForPersistence() != null) {
                var stored = knowledgeDocument.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(knowledgeDocument.save(next));
            // 6. 拒绝未写入的CAS结果，使所属事务回滚而非继续发布事件。
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            // 7. 返回实际成功写入标记，版本或归属校验失败不会走到此处。
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 清理KnowledgeDocument的指定失效记录。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> removeKnowledgeDocument(KnowledgeDocumentRemoveParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 返回实际删除标记，不把空匹配伪造成已删除记录。
            return Result.success(
                    new WriteDO(Boolean.TRUE.equals(knowledgeDocument.remove(param.id()))));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 校验KnowledgeChunk完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveKnowledgeChunk(KnowledgeChunkWriteParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.aggregate() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 准备当前变更后的不可变快照，旧版本保留给仓储CAS。
            var next = param.aggregate();
            // 3. 校验聚合完整性，空实体不能进入仓储。
            next.assertComplete();
            // 4. 更新前加载已持久化聚合，新增快照无需虚构旧记录。
            if (next.idForPersistence() != null) {
                var stored = knowledgeChunk.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(knowledgeChunk.save(next));
            // 6. 拒绝未写入的CAS结果，使所属事务回滚而非继续发布事件。
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            // 7. 返回实际成功写入标记，版本或归属校验失败不会走到此处。
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 清理KnowledgeChunk的指定失效记录。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> removeKnowledgeChunk(KnowledgeChunkRemoveParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 返回实际删除标记，不把空匹配伪造成已删除记录。
            return Result.success(
                    new WriteDO(Boolean.TRUE.equals(knowledgeChunk.remove(param.id()))));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 校验IndexJob完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveIndexJob(IndexJobWriteParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.aggregate() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 准备当前变更后的不可变快照，旧版本保留给仓储CAS。
            var next = param.aggregate();
            // 3. 校验聚合完整性，空实体不能进入仓储。
            next.assertComplete();
            // 4. 更新前加载已持久化聚合，新增快照无需虚构旧记录。
            if (next.idForPersistence() != null) {
                var stored = indexJob.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(indexJob.save(next));
            // 6. 拒绝未写入的CAS结果，使所属事务回滚而非继续发布事件。
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            // 7. 返回实际成功写入标记，版本或归属校验失败不会走到此处。
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 清理IndexJob的指定失效记录。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> removeIndexJob(IndexJobRemoveParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 返回实际删除标记，不把空匹配伪造成已删除记录。
            return Result.success(new WriteDO(Boolean.TRUE.equals(indexJob.remove(param.id()))));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 校验SyncEvent完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveSyncEvent(SyncEventWriteParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.aggregate() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 准备当前变更后的不可变快照，旧版本保留给仓储CAS。
            var next = param.aggregate();
            // 3. 校验聚合完整性，空实体不能进入仓储。
            next.assertComplete();
            // 4. 更新前加载已持久化聚合，新增快照无需虚构旧记录。
            if (next.idForPersistence() != null) {
                var stored = syncEvent.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(syncEvent.save(next));
            // 6. 拒绝未写入的CAS结果，使所属事务回滚而非继续发布事件。
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            // 7. 返回实际成功写入标记，版本或归属校验失败不会走到此处。
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 清理SyncEvent的指定失效记录。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> removeSyncEvent(SyncEventRemoveParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 返回实际删除标记，不把空匹配伪造成已删除记录。
            return Result.success(new WriteDO(Boolean.TRUE.equals(syncEvent.remove(param.id()))));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 校验OutboxEvent完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveOutboxEvent(OutboxEventWriteParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.aggregate() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 准备当前变更后的不可变快照，旧版本保留给仓储CAS。
            var next = param.aggregate();
            // 3. 校验聚合完整性，空实体不能进入仓储。
            next.assertComplete();
            // 4. 更新前加载已持久化聚合，新增快照无需虚构旧记录。
            if (next.idForPersistence() != null) {
                var stored = outboxEvent.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(outboxEvent.save(next));
            // 6. 拒绝未写入的CAS结果，使所属事务回滚而非继续发布事件。
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            // 7. 返回实际成功写入标记，版本或归属校验失败不会走到此处。
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 清理OutboxEvent的指定失效记录。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> removeOutboxEvent(OutboxEventRemoveParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 返回实际删除标记，不把空匹配伪造成已删除记录。
            return Result.success(new WriteDO(Boolean.TRUE.equals(outboxEvent.remove(param.id()))));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 校验ModelInvocation完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveModelInvocation(ModelInvocationWriteParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.aggregate() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 准备当前变更后的不可变快照，旧版本保留给仓储CAS。
            var next = param.aggregate();
            // 3. 校验聚合完整性，空实体不能进入仓储。
            next.assertComplete();
            // 4. 更新前加载已持久化聚合，新增快照无需虚构旧记录。
            if (next.idForPersistence() != null) {
                var stored = modelInvocation.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(modelInvocation.save(next));
            // 6. 拒绝未写入的CAS结果，使所属事务回滚而非继续发布事件。
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            // 7. 返回实际成功写入标记，版本或归属校验失败不会走到此处。
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 清理ModelInvocation的指定失效记录。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> removeModelInvocation(ModelInvocationRemoveParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 返回实际删除标记，不把空匹配伪造成已删除记录。
            return Result.success(
                    new WriteDO(Boolean.TRUE.equals(modelInvocation.remove(param.id()))));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 校验RefreshReceipt完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveRefreshReceipt(RefreshReceiptWriteParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.aggregate() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 准备当前变更后的不可变快照，旧版本保留给仓储CAS。
            var next = param.aggregate();
            // 3. 校验聚合完整性，空实体不能进入仓储。
            next.assertComplete();
            // 4. 更新前加载已持久化聚合，新增快照无需虚构旧记录。
            if (next.idForPersistence() != null) {
                var stored = refreshReceipt.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(refreshReceipt.save(next));
            // 6. 拒绝未写入的CAS结果，使所属事务回滚而非继续发布事件。
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            // 7. 返回实际成功写入标记，版本或归属校验失败不会走到此处。
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 清理RefreshReceipt的指定失效记录。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> removeRefreshReceipt(RefreshReceiptRemoveParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 返回实际删除标记，不把空匹配伪造成已删除记录。
            return Result.success(
                    new WriteDO(Boolean.TRUE.equals(refreshReceipt.remove(param.id()))));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }
}
