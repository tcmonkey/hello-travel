package com.hellotravel.infrastructure.auth.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hellotravel.domain.auth.model.aggregate.UserAccountAggregate;
import com.hellotravel.domain.auth.model.entity.UserAccountEntity;
import com.hellotravel.domain.auth.repository.UserAccountRepository;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.infrastructure.TravelBaseRepository;
import com.hellotravel.infrastructure.auth.mysql.mapper.UserAccountMapper;
import com.hellotravel.infrastructure.auth.mysql.pojo.UserAccountPO;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * ht_user_account仓储，标准安全条件构造器与显式版本CAS，不使用自定义SQL。
 *
 * @author AIGenerator
 */
@Repository
public class UserAccountRepositoryImpl
        extends TravelBaseRepository<UserAccountMapper, UserAccountPO>
        implements UserAccountRepository {

    /**
     * 按内部主键恢复完整聚合；不存在时返回空值。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public UserAccountAggregate findById(Long id) {
        UserAccountPO po = getById(id);
        return po == null ? null : restore(po);
    }

    /**
     * 按可信条件读取有界聚合集合。
     *
     * @author AIGenerator
     * @param queryValue 可信有界查询条件
     * @return 当前操作的业务结果
     */
    public List<UserAccountAggregate> query(QueryValue queryValue) {
        QueryWrapper<UserAccountPO> wrapper =
                conditions(
                        queryValue,
                        Set.of(
                                "id",
                                "public_id",
                                "email_normalized",
                                "password_hash",
                                "email_verified_at",
                                "status",
                                "auth_epoch",
                                "sync_seq",
                                "created_at",
                                "updated_at",
                                "version"));
        Page<UserAccountPO> page = new Page<>(1, queryValue.limit(), false);
        return page(page, wrapper).getRecords().stream().map(this::restore).toList();
    }

    /**
     * 保存完整聚合，更新采用版本比较并交换。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean save(UserAccountAggregate aggregate) {
        try {
            UserAccountPO po = new UserAccountPO();
            po.setId(aggregate.entity().id());
            po.setPublicId(aggregate.entity().publicId());
            po.setEmailNormalized(aggregate.entity().emailNormalized());
            po.setPasswordHash(aggregate.entity().passwordHash());
            po.setEmailVerifiedAt(aggregate.entity().emailVerifiedAt());
            po.setStatus(aggregate.entity().status());
            po.setAuthEpoch(aggregate.entity().authEpoch());
            po.setSyncSeq(aggregate.entity().syncSeq());
            po.setCreatedAt(aggregate.entity().createdAt());
            po.setUpdatedAt(aggregate.entity().updatedAt());
            po.setVersion(aggregate.entity().version());
            if (po.getId() == null) {
                return super.save(po);
            }
            po.setUpdatedAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
            po.setVersion(aggregate.entity().version() + 1);
            return super.update(
                    po,
                    new QueryWrapper<UserAccountPO>()
                            .eq("id", po.getId())
                            .eq("version", aggregate.entity().version()));
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

    private UserAccountAggregate restore(UserAccountPO po) {
        return new UserAccountAggregate(
                new UserAccountEntity(
                        po.getId(),
                        po.getPublicId(),
                        po.getEmailNormalized(),
                        po.getPasswordHash(),
                        po.getEmailVerifiedAt(),
                        po.getStatus(),
                        po.getAuthEpoch(),
                        po.getSyncSeq(),
                        po.getCreatedAt(),
                        po.getUpdatedAt(),
                        po.getVersion()));
    }
}
