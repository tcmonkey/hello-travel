package com.hellotravel.domain.sync.model.entity;

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
}
