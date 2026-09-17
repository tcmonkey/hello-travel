package com.hellotravel.infrastructure.auth.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hellotravel.domain.auth.model.aggregate.RefreshReceiptAggregate;
import com.hellotravel.domain.auth.model.entity.RefreshReceiptEntity;
import com.hellotravel.domain.auth.repository.RefreshReceiptRepository;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.infrastructure.TravelBaseRepository;
import com.hellotravel.infrastructure.auth.mysql.mapper.RefreshReceiptMapper;
import com.hellotravel.infrastructure.auth.mysql.pojo.RefreshReceiptPO;

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

    /**
     * 按内部主键恢复完整聚合；不存在时返回空值。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public RefreshReceiptAggregate findById(Long id) {
        RefreshReceiptPO po = getById(id);
        return po == null ? null : restore(po);
    }

    /**
     * 按可信条件读取有界聚合集合。
     *
     * @author AIGenerator
     * @param queryValue 可信有界查询条件
     * @return 当前操作的业务结果
     */
    public List<RefreshReceiptAggregate> query(QueryValue queryValue) {
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
        return page(page, wrapper).getRecords().stream().map(this::restore).toList();
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
            RefreshReceiptPO po = new RefreshReceiptPO();
            po.setId(aggregate.entity().id());
            po.setUserId(aggregate.entity().userId());
            po.setSessionId(aggregate.entity().sessionId());
            po.setTokenHash(aggregate.entity().tokenHash());
            po.setCreatedAt(aggregate.entity().createdAt());
            po.setExpiresAt(aggregate.entity().expiresAt());
            if (po.getId() == null) {
                return super.save(po);
            }
            return super.update(po, new QueryWrapper<RefreshReceiptPO>().eq("id", po.getId()));
        } catch (org.springframework.dao.TransientDataAccessException
                | org.springframework.dao.DataIntegrityViolationException exception) {
            throw new com.hellotravel.infrastructure.exception.InfrastructureException(
                    com.hellotravel.infrastructure.exception.InfrastructureErrorCode.CONFLICT);
        } catch (org.springframework.dao.DataAccessResourceFailureException exception) {
            throw new com.hellotravel.infrastructure.exception.InfrastructureException(
                    com.hellotravel.infrastructure.exception.InfrastructureErrorCode.UNAVAILABLE);
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

    private RefreshReceiptAggregate restore(RefreshReceiptPO po) {
        return new RefreshReceiptAggregate(
                new RefreshReceiptEntity(
                        po.getId(),
                        po.getUserId(),
                        po.getSessionId(),
                        po.getTokenHash(),
                        po.getCreatedAt(),
                        po.getExpiresAt()));
    }
}
