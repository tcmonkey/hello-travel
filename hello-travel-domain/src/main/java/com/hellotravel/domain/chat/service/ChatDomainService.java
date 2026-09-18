package com.hellotravel.domain.chat.service;

import com.hellotravel.common.error.Failures;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.annotation.DomainService;
import com.hellotravel.domain.chat.model.param.ChatRunWriteParam;
import com.hellotravel.domain.chat.model.param.ConversationWriteParam;
import com.hellotravel.domain.chat.model.param.MessageWriteParam;
import com.hellotravel.domain.chat.model.param.ModelInvocationWriteParam;
import com.hellotravel.domain.chat.repository.ChatRunRepository;
import com.hellotravel.domain.chat.repository.ConversationRepository;
import com.hellotravel.domain.chat.repository.MessageRepository;
import com.hellotravel.domain.chat.repository.ModelInvocationRepository;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.model.persistence.WriteDO;

/**
 * 对话业务的写入协调，仅使用本域聚合和仓储；事务由应用层管理。
 *
 * @author AIGenerator
 */
@DomainService
public final class ChatDomainService {

    private final ConversationRepository conversation;
    private final MessageRepository message;
    private final ChatRunRepository chatRun;
    private final ModelInvocationRepository modelInvocation;

    public ChatDomainService(
            ConversationRepository conversation,
            MessageRepository message,
            ChatRunRepository chatRun,
            ModelInvocationRepository modelInvocation) {
        this.conversation = conversation;
        this.message = message;
        this.chatRun = chatRun;
        this.modelInvocation = modelInvocation;
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
}
