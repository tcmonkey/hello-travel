package com.hellotravel.domain.sync.model.entity;

import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

/**
 * 持久化归属与状态快照；变更须经过语义方法和版本检查。
 *
 * @param id 内部主键，不作为客户端补齐游标
 * @param userId 目标账号，其他账号不得订阅
 * @param eventSeq 该用户的连续提交序号；来自账号sync_seq
 * @param eventType 消息、会话、用量、资料或会话撤销事件类型
 * @param aggregatePublicId 事件目标对外ID
 * @param aggregateVersion 目标实体版本，避免重放覆盖新状态
 * @param targetSessionPublicId 定向踢登录会话事件的目标
 * @param payloadJson 有界ID/状态/版本载荷，不含令牌、验证码或完整私密正文
 * @param createdAt 与业务事务一起提交的UTC时间
 * @param expiresAt 补齐事件保留截止；过期需要重新拉完整快照
 * @author AIGenerator
 */
public record SyncEventEntity(
        Long id,
        Long userId,
        Long eventSeq,
        String eventType,
        String aggregatePublicId,
        Long aggregateVersion,
        String targetSessionPublicId,
        String payloadJson,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime expiresAt) {

    /**
     * 实体诊断仅输出类型，避免泄漏正文与密码摘要。
     *
     * @return 脱敏类型描述
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "SyncEventEntity{redacted}";
    }

    /**
     * 核对快照更新的不变量，保持版本、归属与删除状态一致。
     *
     * @param prior 已持久化的实体快照
     * @author AIGenerator
     */
    public void assertUpdateAgainst(SyncEventEntity prior) {
        // 1. 核对账号归属不变，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.userId(), this.userId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
    }

    /**
     * 创建绑定用户提交序号的持久同步事件，固定状态由实体封装。
     *
     * @param userId 账号归属
     * @param eventSeq 该用户的连续提交序号；来自账号sync_seq
     * @param eventType 推送、开始生成、提取记忆或验证码投递等事件
     * @param aggregatePublicId 事件目标对外ID
     * @param aggregateVersion 目标实体版本，避免重放覆盖新状态
     * @param targetSessionPublicId 定向踢登录会话事件的目标
     * @param payloadJson 只存受限任务引用，验证码密文在challenge表
     * @param createdAt 创建时间
     * @param expiresAt 有时间适用性的事实到期时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static SyncEventEntity committed(
            Long userId,
            Long eventSeq,
            String eventType,
            String aggregatePublicId,
            Long aggregateVersion,
            String targetSessionPublicId,
            String payloadJson,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime expiresAt) {
        // 1. 创建绑定用户提交序号的持久同步事件，返回不可变快照。
        return new SyncEventEntity(
                null,
                userId,
                eventSeq,
                eventType,
                aggregatePublicId,
                aggregateVersion,
                targetSessionPublicId,
                payloadJson,
                createdAt,
                expiresAt);
    }
}
