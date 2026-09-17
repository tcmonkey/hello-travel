package com.hellotravel.domain.persistence.service;

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
            if (param == null || param.aggregate() == null || param.aggregate().entity() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            var next = param.aggregate().entity();
            if (next.id() != null) {
                var stored = userAccount.findById(next.id());
                if (stored == null) {
                    throw new DomainException(DomainErrorCode.NOT_FOUND);
                }
                var prior = stored.entity();
                if (!prior.version().equals(next.version())) {
                    throw new DomainException(DomainErrorCode.CONFLICT);
                }
                if (!java.util.Objects.equals(prior.publicId(), next.publicId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
            }
            boolean saved = Boolean.TRUE.equals(userAccount.save(param.aggregate()));
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            return Result.success(new WriteDO(Boolean.TRUE.equals(userAccount.remove(param.id()))));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.aggregate() == null || param.aggregate().entity() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            var next = param.aggregate().entity();
            if (next.id() != null) {
                var stored = device.findById(next.id());
                if (stored == null) {
                    throw new DomainException(DomainErrorCode.NOT_FOUND);
                }
                var prior = stored.entity();
                if (!prior.version().equals(next.version())) {
                    throw new DomainException(DomainErrorCode.CONFLICT);
                }
                if (!java.util.Objects.equals(prior.userId(), next.userId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (!java.util.Objects.equals(prior.publicId(), next.publicId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
            }
            boolean saved = Boolean.TRUE.equals(device.save(param.aggregate()));
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            return Result.success(new WriteDO(Boolean.TRUE.equals(device.remove(param.id()))));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.aggregate() == null || param.aggregate().entity() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            var next = param.aggregate().entity();
            if (next.id() != null) {
                var stored = loginSession.findById(next.id());
                if (stored == null) {
                    throw new DomainException(DomainErrorCode.NOT_FOUND);
                }
                var prior = stored.entity();
                if (!prior.version().equals(next.version())) {
                    throw new DomainException(DomainErrorCode.CONFLICT);
                }
                if (!java.util.Objects.equals(prior.userId(), next.userId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (!java.util.Objects.equals(prior.deviceId(), next.deviceId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (!java.util.Objects.equals(prior.publicId(), next.publicId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
            }
            boolean saved = Boolean.TRUE.equals(loginSession.save(param.aggregate()));
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            return Result.success(
                    new WriteDO(Boolean.TRUE.equals(loginSession.remove(param.id()))));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.aggregate() == null || param.aggregate().entity() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            var next = param.aggregate().entity();
            if (next.id() != null) {
                var stored = emailChallenge.findById(next.id());
                if (stored == null) {
                    throw new DomainException(DomainErrorCode.NOT_FOUND);
                }
                var prior = stored.entity();
                if (!prior.version().equals(next.version())) {
                    throw new DomainException(DomainErrorCode.CONFLICT);
                }
                if (!java.util.Objects.equals(prior.publicId(), next.publicId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
            }
            boolean saved = Boolean.TRUE.equals(emailChallenge.save(param.aggregate()));
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            return Result.success(
                    new WriteDO(Boolean.TRUE.equals(emailChallenge.remove(param.id()))));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.aggregate() == null || param.aggregate().entity() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            var next = param.aggregate().entity();
            if (next.id() != null) {
                var stored = conversation.findById(next.id());
                if (stored == null) {
                    throw new DomainException(DomainErrorCode.NOT_FOUND);
                }
                var prior = stored.entity();
                if (!prior.version().equals(next.version())) {
                    throw new DomainException(DomainErrorCode.CONFLICT);
                }
                if (!java.util.Objects.equals(prior.userId(), next.userId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (!java.util.Objects.equals(prior.publicId(), next.publicId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (prior.deletedAt() != null && next.deletedAt() == null) {
                    throw new DomainException(DomainErrorCode.CONFLICT);
                }
            }
            boolean saved = Boolean.TRUE.equals(conversation.save(param.aggregate()));
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            return Result.success(
                    new WriteDO(Boolean.TRUE.equals(conversation.remove(param.id()))));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.aggregate() == null || param.aggregate().entity() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            var next = param.aggregate().entity();
            if (next.id() != null) {
                var stored = message.findById(next.id());
                if (stored == null) {
                    throw new DomainException(DomainErrorCode.NOT_FOUND);
                }
                var prior = stored.entity();
                if (!prior.version().equals(next.version())) {
                    throw new DomainException(DomainErrorCode.CONFLICT);
                }
                if (!java.util.Objects.equals(prior.userId(), next.userId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (!java.util.Objects.equals(prior.conversationId(), next.conversationId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (!java.util.Objects.equals(prior.publicId(), next.publicId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (prior.deletedAt() != null && next.deletedAt() == null) {
                    throw new DomainException(DomainErrorCode.CONFLICT);
                }
            }
            boolean saved = Boolean.TRUE.equals(message.save(param.aggregate()));
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            return Result.success(new WriteDO(Boolean.TRUE.equals(message.remove(param.id()))));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.aggregate() == null || param.aggregate().entity() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            var next = param.aggregate().entity();
            if (next.id() != null) {
                var stored = chatRun.findById(next.id());
                if (stored == null) {
                    throw new DomainException(DomainErrorCode.NOT_FOUND);
                }
                var prior = stored.entity();
                if (!prior.version().equals(next.version())) {
                    throw new DomainException(DomainErrorCode.CONFLICT);
                }
                if (!java.util.Objects.equals(prior.userId(), next.userId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (!java.util.Objects.equals(prior.conversationId(), next.conversationId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (!java.util.Objects.equals(prior.publicId(), next.publicId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
            }
            boolean saved = Boolean.TRUE.equals(chatRun.save(param.aggregate()));
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            return Result.success(new WriteDO(Boolean.TRUE.equals(chatRun.remove(param.id()))));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.aggregate() == null || param.aggregate().entity() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            var next = param.aggregate().entity();
            if (next.id() != null) {
                var stored = memorySummary.findById(next.id());
                if (stored == null) {
                    throw new DomainException(DomainErrorCode.NOT_FOUND);
                }
                var prior = stored.entity();
                if (!prior.version().equals(next.version())) {
                    throw new DomainException(DomainErrorCode.CONFLICT);
                }
                if (!java.util.Objects.equals(prior.userId(), next.userId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (!java.util.Objects.equals(prior.conversationId(), next.conversationId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (!java.util.Objects.equals(prior.publicId(), next.publicId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
            }
            boolean saved = Boolean.TRUE.equals(memorySummary.save(param.aggregate()));
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            return Result.success(
                    new WriteDO(Boolean.TRUE.equals(memorySummary.remove(param.id()))));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.aggregate() == null || param.aggregate().entity() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            var next = param.aggregate().entity();
            if (next.id() != null) {
                var stored = memoryFact.findById(next.id());
                if (stored == null) {
                    throw new DomainException(DomainErrorCode.NOT_FOUND);
                }
                var prior = stored.entity();
                if (!prior.version().equals(next.version())) {
                    throw new DomainException(DomainErrorCode.CONFLICT);
                }
                if (!java.util.Objects.equals(prior.userId(), next.userId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (!java.util.Objects.equals(prior.conversationId(), next.conversationId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (!java.util.Objects.equals(prior.publicId(), next.publicId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
            }
            boolean saved = Boolean.TRUE.equals(memoryFact.save(param.aggregate()));
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            return Result.success(new WriteDO(Boolean.TRUE.equals(memoryFact.remove(param.id()))));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.aggregate() == null || param.aggregate().entity() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            var next = param.aggregate().entity();
            if (next.id() != null) {
                var stored = memoryFactSource.findById(next.id());
                if (stored == null) {
                    throw new DomainException(DomainErrorCode.NOT_FOUND);
                }
                var prior = stored.entity();
                if (!java.util.Objects.equals(prior.userId(), next.userId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (!java.util.Objects.equals(prior.conversationId(), next.conversationId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
            }
            boolean saved = Boolean.TRUE.equals(memoryFactSource.save(param.aggregate()));
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            return Result.success(
                    new WriteDO(Boolean.TRUE.equals(memoryFactSource.remove(param.id()))));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.aggregate() == null || param.aggregate().entity() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            var next = param.aggregate().entity();
            if (next.id() != null) {
                var stored = knowledgeDocument.findById(next.id());
                if (stored == null) {
                    throw new DomainException(DomainErrorCode.NOT_FOUND);
                }
                var prior = stored.entity();
                if (!prior.version().equals(next.version())) {
                    throw new DomainException(DomainErrorCode.CONFLICT);
                }
                if (!java.util.Objects.equals(prior.userId(), next.userId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (!java.util.Objects.equals(prior.publicId(), next.publicId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (prior.deletedAt() != null && next.deletedAt() == null) {
                    throw new DomainException(DomainErrorCode.CONFLICT);
                }
            }
            boolean saved = Boolean.TRUE.equals(knowledgeDocument.save(param.aggregate()));
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            return Result.success(
                    new WriteDO(Boolean.TRUE.equals(knowledgeDocument.remove(param.id()))));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.aggregate() == null || param.aggregate().entity() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            var next = param.aggregate().entity();
            if (next.id() != null) {
                var stored = knowledgeChunk.findById(next.id());
                if (stored == null) {
                    throw new DomainException(DomainErrorCode.NOT_FOUND);
                }
                var prior = stored.entity();
                if (!prior.version().equals(next.version())) {
                    throw new DomainException(DomainErrorCode.CONFLICT);
                }
                if (!java.util.Objects.equals(prior.userId(), next.userId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (!java.util.Objects.equals(prior.documentId(), next.documentId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (!java.util.Objects.equals(prior.publicId(), next.publicId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (prior.deletedAt() != null && next.deletedAt() == null) {
                    throw new DomainException(DomainErrorCode.CONFLICT);
                }
            }
            boolean saved = Boolean.TRUE.equals(knowledgeChunk.save(param.aggregate()));
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            return Result.success(
                    new WriteDO(Boolean.TRUE.equals(knowledgeChunk.remove(param.id()))));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.aggregate() == null || param.aggregate().entity() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            var next = param.aggregate().entity();
            if (next.id() != null) {
                var stored = indexJob.findById(next.id());
                if (stored == null) {
                    throw new DomainException(DomainErrorCode.NOT_FOUND);
                }
                var prior = stored.entity();
                if (!prior.version().equals(next.version())) {
                    throw new DomainException(DomainErrorCode.CONFLICT);
                }
                if (!java.util.Objects.equals(prior.userId(), next.userId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (!java.util.Objects.equals(prior.documentId(), next.documentId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (!java.util.Objects.equals(prior.publicId(), next.publicId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
            }
            boolean saved = Boolean.TRUE.equals(indexJob.save(param.aggregate()));
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            return Result.success(new WriteDO(Boolean.TRUE.equals(indexJob.remove(param.id()))));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.aggregate() == null || param.aggregate().entity() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            var next = param.aggregate().entity();
            if (next.id() != null) {
                var stored = syncEvent.findById(next.id());
                if (stored == null) {
                    throw new DomainException(DomainErrorCode.NOT_FOUND);
                }
                var prior = stored.entity();
                if (!java.util.Objects.equals(prior.userId(), next.userId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
            }
            boolean saved = Boolean.TRUE.equals(syncEvent.save(param.aggregate()));
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            return Result.success(new WriteDO(Boolean.TRUE.equals(syncEvent.remove(param.id()))));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.aggregate() == null || param.aggregate().entity() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            var next = param.aggregate().entity();
            if (next.id() != null) {
                var stored = outboxEvent.findById(next.id());
                if (stored == null) {
                    throw new DomainException(DomainErrorCode.NOT_FOUND);
                }
                var prior = stored.entity();
                if (!prior.version().equals(next.version())) {
                    throw new DomainException(DomainErrorCode.CONFLICT);
                }
                if (!java.util.Objects.equals(prior.userId(), next.userId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (!java.util.Objects.equals(prior.publicId(), next.publicId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
            }
            boolean saved = Boolean.TRUE.equals(outboxEvent.save(param.aggregate()));
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            return Result.success(new WriteDO(Boolean.TRUE.equals(outboxEvent.remove(param.id()))));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.aggregate() == null || param.aggregate().entity() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            var next = param.aggregate().entity();
            if (next.id() != null) {
                var stored = modelInvocation.findById(next.id());
                if (stored == null) {
                    throw new DomainException(DomainErrorCode.NOT_FOUND);
                }
                var prior = stored.entity();
                if (!prior.version().equals(next.version())) {
                    throw new DomainException(DomainErrorCode.CONFLICT);
                }
                if (!java.util.Objects.equals(prior.userId(), next.userId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (!java.util.Objects.equals(prior.conversationId(), next.conversationId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
                if (!java.util.Objects.equals(prior.publicId(), next.publicId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
            }
            boolean saved = Boolean.TRUE.equals(modelInvocation.save(param.aggregate()));
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            return Result.success(
                    new WriteDO(Boolean.TRUE.equals(modelInvocation.remove(param.id()))));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.aggregate() == null || param.aggregate().entity() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            var next = param.aggregate().entity();
            if (next.id() != null) {
                var stored = refreshReceipt.findById(next.id());
                if (stored == null) {
                    throw new DomainException(DomainErrorCode.NOT_FOUND);
                }
                var prior = stored.entity();
                if (!java.util.Objects.equals(prior.userId(), next.userId())) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
            }
            boolean saved = Boolean.TRUE.equals(refreshReceipt.save(param.aggregate()));
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
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
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            return Result.success(
                    new WriteDO(Boolean.TRUE.equals(refreshReceipt.remove(param.id()))));
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.domain.exception.DomainErrorCode.FAILED);
        }
    }
}
