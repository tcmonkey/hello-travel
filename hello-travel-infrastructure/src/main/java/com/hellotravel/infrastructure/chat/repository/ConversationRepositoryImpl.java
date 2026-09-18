package com.hellotravel.infrastructure.chat.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hellotravel.domain.chat.model.aggregate.ConversationAggregate;
import com.hellotravel.domain.chat.repository.ConversationRepository;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.infrastructure.TravelBaseRepository;
import com.hellotravel.infrastructure.chat.converter.ConversationPersistenceConverter;
import com.hellotravel.infrastructure.chat.mysql.mapper.ConversationMapper;
import com.hellotravel.infrastructure.chat.mysql.pojo.ConversationPO;
import com.hellotravel.infrastructure.exception.InfrastructureErrorCode;
import com.hellotravel.infrastructure.exception.InfrastructureException;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * ht_conversation仓储，标准安全条件构造器与显式版本CAS，不使用自定义SQL。
 *
 * @author AIGenerator
 */
@Repository
public class ConversationRepositoryImpl
        extends TravelBaseRepository<ConversationMapper, ConversationPO>
        implements ConversationRepository {

    private final ConversationPersistenceConverter conversationPersistenceConverter;

    /**
     * 按内部主键恢复完整聚合；不存在时返回空值。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public ConversationAggregate findById(Long id) {
        // 1. 转换完整聚合为本仓储PO，映射与状态决策分开。
        ConversationPO po = getById(id);
        // 2. 显式处理不存在的记录，并恢复聚合快照。
        return po == null ? null : conversationPersistenceConverter.restore(po);
    }

    /**
     * 按可信条件读取有界聚合集合。
     *
     * @author AIGenerator
     * @param queryValue 可信有界查询条件
     * @return 当前操作的业务结果
     */
    public List<ConversationAggregate> query(QueryValue queryValue) {
        // 1. 按字段白名单组装参数绑定条件，禁止任意列或拼接SQL。
        QueryWrapper<ConversationPO> wrapper =
                conditions(
                        queryValue,
                        Set.of(
                                "id",
                                "public_id",
                                "user_id",
                                "title",
                                "last_message_seq",
                                "history_epoch",
                                "memory_epoch",
                                "last_activity_at",
                                "deleted_at",
                                "created_at",
                                "updated_at",
                                "version"));
        Page<ConversationPO> page = new Page<>(1, queryValue.limit(), false);
        // 2. 读取有界PO集合并恢复完整聚合，不向上暴露ORM对象。
        return page(page, wrapper).getRecords().stream()
                .map(conversationPersistenceConverter::restore)
                .toList();
    }

    /**
     * 保存完整聚合，更新采用版本比较并交换。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean save(ConversationAggregate aggregate) {
        try {
            // 1. 转换完整聚合为本仓储PO，映射与状态决策分开。
            ConversationPO po = conversationPersistenceConverter.toPersistence(aggregate);
            // 2. 区分新快照新增与已保存快照的版本CAS更新。
            if (po.getId() == null) {
                return super.save(po);
            }
            // 3. 映射本段快照字段，业务状态规则不放入PO赋值。
            po.setUpdatedAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
            po.setVersion(aggregate.entity().version() + 1);
            // 4. 按内部主键及原版本执行CAS更新，零匹配由上层处理为冲突。
            return super.update(
                    po,
                    new QueryWrapper<ConversationPO>()
                            .eq("id", po.getId())
                            .eq("version", aggregate.entity().version()));
        } catch (org.springframework.dao.TransientDataAccessException
                | org.springframework.dao.DataIntegrityViolationException exception) {
            throw new InfrastructureException(InfrastructureErrorCode.CONFLICT);
        } catch (org.springframework.dao.DataAccessResourceFailureException exception) {
            throw new InfrastructureException(InfrastructureErrorCode.UNAVAILABLE);
        }
    }

    /**
     * 物理清理指定记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean remove(Long id) {
        return super.removeById(id);
    }

    public ConversationRepositoryImpl(
            ConversationPersistenceConverter conversationPersistenceConverter) {
        this.conversationPersistenceConverter = conversationPersistenceConverter;
    }
}
