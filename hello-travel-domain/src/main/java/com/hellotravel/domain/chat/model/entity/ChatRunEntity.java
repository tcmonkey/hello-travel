package com.hellotravel.domain.chat.model.entity;

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
     * 校验不可变值的边界并防御性复制输入集合。
     *
     * @author AIGenerator
     */
    public ChatRunEntity {
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
        if (!"ACCEPTED".equals(status)) {
            throw new IllegalStateException("run not accepted");
        }
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
        if (!java.util.Set.of("FAILED", "INTERRUPTED", "CANCELLED").contains(status)) {
            throw new IllegalStateException("not retryable");
        }
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
        return requestDigest == null ? null : requestDigest.clone();
    }
}
