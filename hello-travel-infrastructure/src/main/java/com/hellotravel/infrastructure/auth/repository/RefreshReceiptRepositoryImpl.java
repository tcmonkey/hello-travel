package com.hellotravel.infrastructure.auth.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hellotravel.domain.auth.model.aggregate.RefreshReceiptAggregate;
import com.hellotravel.domain.auth.repository.RefreshReceiptRepository;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.infrastructure.TravelBaseRepository;
import com.hellotravel.infrastructure.auth.converter.RefreshReceiptPersistenceConverter;
import com.hellotravel.infrastructure.auth.mysql.mapper.RefreshReceiptMapper;
import com.hellotravel.infrastructure.auth.mysql.pojo.RefreshReceiptPO;
import com.hellotravel.infrastructure.exception.InfrastructureErrorCode;
import com.hellotravel.infrastructure.exception.InfrastructureException;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * ht_refresh_receipt仓储，标准安全条件构造器与显式版本CAS，不使用自定义SQL。
 *
 * @author AIGenerator
 */
@Repository
public class RefreshReceiptRepositoryImpl
        extends TravelBaseRepository<RefreshReceiptMapper, RefreshReceiptPO>
        implements RefreshReceiptRepository {

    private final RefreshReceiptPersistenceConverter refreshReceiptPersistenceConverter;

    /**
     * 按内部主键恢复完整聚合；不存在时返回空值。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public RefreshReceiptAggregate findById(Long id) {
        // 1. 转换完整聚合为本仓储PO，映射与状态决策分开。
        RefreshReceiptPO po = getById(id);
        // 2. 显式处理不存在的记录，并恢复聚合快照。
        return po == null ? null : refreshReceiptPersistenceConverter.restore(po);
    }

    /**
     * 按可信条件读取有界聚合集合。
     *
     * @author AIGenerator
     * @param queryValue 可信有界查询条件
     * @return 当前操作的业务结果
     */
    public List<RefreshReceiptAggregate> query(QueryValue queryValue) {
        // 1. 按字段白名单组装参数绑定条件，禁止任意列或拼接SQL。
        QueryWrapper<RefreshReceiptPO> wrapper =
                conditions(
                        queryValue,
                        Set.of(
                                "id",
                                "user_id",
                                "session_id",
                                "token_hash",
                                "created_at",
                                "expires_at"));
        Page<RefreshReceiptPO> page = new Page<>(1, queryValue.limit(), false);
        // 2. 读取有界PO集合并恢复完整聚合，不向上暴露ORM对象。
        return page(page, wrapper).getRecords().stream()
                .map(refreshReceiptPersistenceConverter::restore)
                .toList();
    }

    /**
     * 保存完整聚合，更新采用版本比较并交换。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean save(RefreshReceiptAggregate aggregate) {
        try {
            // 1. 转换完整聚合为本仓储PO，映射与状态决策分开。
            RefreshReceiptPO po = refreshReceiptPersistenceConverter.toPersistence(aggregate);
            // 2. 区分新快照新增与已保存快照的版本CAS更新。
            if (po.getId() == null) {
                return super.save(po);
            }
            // 3. 按内部主键及原版本执行CAS更新，零匹配由上层处理为冲突。
            return super.update(po, new QueryWrapper<RefreshReceiptPO>().eq("id", po.getId()));
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

    public RefreshReceiptRepositoryImpl(
            RefreshReceiptPersistenceConverter refreshReceiptPersistenceConverter) {
        this.refreshReceiptPersistenceConverter = refreshReceiptPersistenceConverter;
    }
}
