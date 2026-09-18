package com.hellotravel.domain.chat.model.entity;

import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.memory.model.value.ContextBudgetValue;

/**
 * 持久化归属与状态快照；变更须经过语义方法和版本检查。
 *
 * @param id 内部主键，不直接作为前端数值ID
 * @param publicId 对外ULID字符串标识
 * @param userId 账号归属
 * @param conversationId 对话归属
 * @param initiatingSessionId 发起时登录会话，仅作审计归属
 * @param requestKey 客户端幂等请求UUID
 * @param requestDigest 请求规范化正文摘要，禁止同键换内容
 * @param userMessageId 已持久化的输入消息
 * @param assistantMessageId 已预分配的回答消息
 * @param status 生成状态；终态不自动重复调用模型
 * @param attemptCount 显式重试代次，不重复创建用户消息
 * @param graphNode 最近完成或正在执行的图节点
 * @param graphRevision 工作流代码版本，恢复时检查兼容
 * @param memoryEpochAtStart 推理输入使用的记忆代次
 * @param stateJson 受限图状态及已脱敏工具结果，不保存密钥
 * @param contextSnapshotJson 本轮预算组成、估算方法及使用进度
 * @param leaseOwner 当前执行者标识
 * @param leaseFence 租约代次，旧执行者不可提交
 * @param leaseUntil 执行租约到期时间
 * @param errorCode 内部稳定错误码，不保存原始供应商错误
 * @param startedAt 生成开始时间
 * @param completedAt 终态时间
 * @param createdAt 创建时间，UTC
 * @param updatedAt 更新时间，UTC
 * @param version 乐观锁版本，更新时递增
 * @author AIGenerator
 */
public record ChatRunEntity(
        Long id,
        String publicId,
        Long userId,
        Long conversationId,
        Long initiatingSessionId,
        String requestKey,
        byte[] requestDigest,
        Long userMessageId,
        Long assistantMessageId,
        String status,
        Integer attemptCount,
        String graphNode,
        String graphRevision,
        Long memoryEpochAtStart,
        String stateJson,
        String contextSnapshotJson,
        String leaseOwner,
        Long leaseFence,
        java.time.LocalDateTime leaseUntil,
        String errorCode,
        java.time.LocalDateTime startedAt,
        java.time.LocalDateTime completedAt,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt,
        Long version) {

    /**
     * 防御性复制敏感字节字段，外部数组修改不能影响实体快照。
     *
     * @author AIGenerator
     */
    public ChatRunEntity {
        // 1. 复制输入摘要或加密载荷，外部数组修改不能改变实体快照。
        requestDigest = requestDigest == null ? null : requestDigest.clone();
    }

    /**
     * 领取待执行任务并提高租约栅栏。
     *
     * @author AIGenerator
     * @param owner 受控owner参数
     * @return 当前操作的业务结果
     */
    public ChatRunEntity claim(String owner) {
        // 1. 依据实体当前状态与允许的操作处理分支，避免继续使用无效数据。
        if (!"ACCEPTED".equals(status)) {
            throw new IllegalStateException("run not accepted");
        }
        // 2. 领取待执行任务并提高租约栅栏，返回不可变快照。
        return new ChatRunEntity(
                this.id(),
                this.publicId(),
                this.userId(),
                this.conversationId(),
                this.initiatingSessionId(),
                this.requestKey(),
                this.requestDigest(),
                this.userMessageId(),
                this.assistantMessageId(),
                "RUNNING",
                attemptCount == 0 ? 1 : attemptCount,
                this.graphNode(),
                this.graphRevision(),
                this.memoryEpochAtStart(),
                this.stateJson(),
                this.contextSnapshotJson(),
                owner,
                Math.addExact(leaseFence, 1),
                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusMinutes(3),
                this.errorCode(),
                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                this.completedAt(),
                this.createdAt(),
                this.updatedAt(),
                this.version());
    }

    /**
     * 记录任务终态并释放租约。
     *
     * @author AIGenerator
     * @param terminal 受控terminal参数
     * @param error 待分类的失败
     * @return 当前操作的业务结果
     */
    public ChatRunEntity finish(String terminal, String error) {
        // 1. 结束当前生成尝试，终态不自动重复调用模型，返回不可变快照。
        return new ChatRunEntity(
                this.id(),
                this.publicId(),
                this.userId(),
                this.conversationId(),
                this.initiatingSessionId(),
                this.requestKey(),
                this.requestDigest(),
                this.userMessageId(),
                this.assistantMessageId(),
                terminal,
                this.attemptCount(),
                this.graphNode(),
                this.graphRevision(),
                this.memoryEpochAtStart(),
                this.stateJson(),
                this.contextSnapshotJson(),
                this.leaseOwner(),
                this.leaseFence(),
                null,
                error,
                this.startedAt(),
                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                this.createdAt(),
                this.updatedAt(),
                this.version());
    }

    /**
     * 提高尝试代次并重新接收已终止的生成任务。
     *
     * @author AIGenerator
     * @param epoch 受控epoch参数
     * @return 当前操作的业务结果
     */
    public ChatRunEntity retry(long epoch) {
        // 1. 依据实体当前状态与允许的操作处理分支，避免继续使用无效数据。
        if (!java.util.Set.of("FAILED", "INTERRUPTED", "CANCELLED").contains(status)) {
            throw new IllegalStateException("not retryable");
        }
        // 2. 启动显式重试代次，复用原始消息而不再次提交用户输入，返回不可变快照。
        return new ChatRunEntity(
                this.id(),
                this.publicId(),
                this.userId(),
                this.conversationId(),
                this.initiatingSessionId(),
                this.requestKey(),
                this.requestDigest(),
                this.userMessageId(),
                this.assistantMessageId(),
                "ACCEPTED",
                Math.addExact(attemptCount, 1),
                this.graphNode(),
                this.graphRevision(),
                epoch,
                null,
                this.contextSnapshotJson(),
                this.leaseOwner(),
                Math.addExact(leaseFence, 1),
                this.leaseUntil(),
                null,
                this.startedAt(),
                null,
                this.createdAt(),
                this.updatedAt(),
                this.version());
    }

    /**
     * 保存当前步骤与有界上下文预算。
     *
     * @author AIGenerator
     * @param node 当前工作流步骤
     * @param data 受控业务载荷
     * @param budget 有界上下文预算JSON
     * @return 当前操作的业务结果
     */
    public ChatRunEntity checkpoint(String node, String data, String budget) {
        // 1. 保留当前图节点与预算，旧执行者须通过栅栏检查，返回不可变快照。
        return new ChatRunEntity(
                this.id(),
                this.publicId(),
                this.userId(),
                this.conversationId(),
                this.initiatingSessionId(),
                this.requestKey(),
                this.requestDigest(),
                this.userMessageId(),
                this.assistantMessageId(),
                this.status(),
                this.attemptCount(),
                node,
                this.graphRevision(),
                this.memoryEpochAtStart(),
                data,
                budget,
                this.leaseOwner(),
                this.leaseFence(),
                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusMinutes(3),
                this.errorCode(),
                this.startedAt(),
                this.completedAt(),
                this.createdAt(),
                this.updatedAt(),
                this.version());
    }

    /**
     * 实体诊断仅输出类型，避免泄漏正文与密码摘要。
     *
     * @return 脱敏类型描述
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "ChatRunEntity{redacted}";
    }

    /**
     * 处理requestDigest对应的受控业务操作。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public byte[] requestDigest() {
        // 1. 交付摘要的独立副本，调用者修改返回数组不会改变实体快照。
        return requestDigest == null ? null : requestDigest.clone();
    }

    /**
     * 接受绑定消息对及记忆代次的新任务，初始尝试与租约不能由调用方任意设置。
     *
     * @param conversation 任务绑定的会话
     * @param sessionId 发起登录会话
     * @param requestKey 请求幂等键
     * @param digest 请求正文摘要
     * @param input 已持久化的用户消息
     * @param output 已持久化的助手占位消息
     * @param time 当前UTC时间
     * @return 待执行任务
     * @author AIGenerator
     */
    public static ChatRunEntity accepted(
            ConversationEntity conversation,
            Long sessionId,
            String requestKey,
            byte[] digest,
            MessageEntity input,
            MessageEntity output,
            java.time.LocalDateTime time) {
        // 1. 核对关联对象归属与角色，拒绝跨账号或跨会话关联。
        if (input.id() == null
                || output.id() == null
                || !conversation.id().equals(input.conversationId())
                || !conversation.id().equals(output.conversationId())
                || !conversation.userId().equals(input.userId())
                || !conversation.userId().equals(output.userId())
                || !"USER".equals(input.role())
                || !"ASSISTANT".equals(output.role())) {
            throw new IllegalArgumentException("message ownership");
        }
        // 2. 生成本次业务的公开标识，内部数据库主键保持由仓储分配。
        String publicId = Ids.next();
        // 3. 接受绑定消息对、幂等键和记忆代次的生成任务，返回不可变快照。
        return new ChatRunEntity(
                null,
                publicId,
                conversation.userId(),
                conversation.id(),
                sessionId,
                requestKey,
                digest,
                input.id(),
                output.id(),
                "ACCEPTED",
                0,
                null,
                "travel-v1",
                conversation.memoryEpoch(),
                null,
                null,
                null,
                0L,
                null,
                null,
                null,
                null,
                time,
                time,
                0L);
    }

    /**
     * 核对快照更新的不变量，保持版本、归属与删除状态一致。
     *
     * @param prior 已持久化的实体快照
     * @author AIGenerator
     */
    public void assertUpdateAgainst(ChatRunEntity prior) {
        // 1. 核对数据库版本未发生并发变化，不满足时拒绝本次更新。
        if (!prior.version().equals(this.version())) {
            throw new DomainException(DomainErrorCode.CONFLICT);
        }
        // 2. 核对账号归属不变，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.userId(), this.userId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        // 3. 核对会话归属不变，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.conversationId(), this.conversationId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        // 4. 核对公开标识不被改写，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.publicId(), this.publicId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
    }

    /**
     * 创建本次生成任务的上下文预算，容量和预留不变量由领域值对象控制。
     *
     * @param window 应用模型窗口
     * @param input 当前完整输入的保守估算
     * @param output 输出预留
     * @param safety 安全预留
     * @return 本次任务预算
     * @author AIGenerator
     */
    public ContextBudgetValue contextBudget(int window, int input, int output, int safety) {
        // 1. 封装当前任务的预算，应用编排只读取容量与压缩决策。
        return new ContextBudgetValue(window, input, output, safety);
    }
}
