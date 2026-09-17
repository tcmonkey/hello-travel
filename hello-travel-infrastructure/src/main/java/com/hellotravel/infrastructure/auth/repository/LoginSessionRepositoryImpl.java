package com.hellotravel.infrastructure.auth.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hellotravel.domain.auth.model.aggregate.LoginSessionAggregate;
import com.hellotravel.domain.auth.model.entity.LoginSessionEntity;
import com.hellotravel.domain.auth.repository.LoginSessionRepository;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.infrastructure.TravelBaseRepository;
import com.hellotravel.infrastructure.auth.mysql.mapper.LoginSessionMapper;
import com.hellotravel.infrastructure.auth.mysql.pojo.LoginSessionPO;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * ht_login_session仓储，标准安全条件构造器与显式版本CAS，不使用自定义SQL。
 *
 * @author AIGenerator
 */
@Repository
public class LoginSessionRepositoryImpl
        extends TravelBaseRepository<LoginSessionMapper, LoginSessionPO>
        implements LoginSessionRepository {

    /**
     * 按内部主键恢复完整聚合；不存在时返回空值。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public LoginSessionAggregate findById(Long id) {
        LoginSessionPO po = getById(id);
        return po == null ? null : restore(po);
    }

    /**
     * 按可信条件读取有界聚合集合。
     *
     * @author AIGenerator
     * @param queryValue 可信有界查询条件
     * @return 当前操作的业务结果
     */
    public List<LoginSessionAggregate> query(QueryValue queryValue) {
        QueryWrapper<LoginSessionPO> wrapper =
                conditions(
                        queryValue,
                        Set.of(
                                "id",
                                "public_id",
                                "user_id",
                                "device_id",
                                "access_token_hash",
                                "refresh_token_hash",
                                "csrf_token_hash",
                                "auth_epoch",
                                "status",
                                "revoke_reason",
                                "access_expires_at",
                                "refresh_expires_at",
                                "last_seen_at",
                                "revoked_at",
                                "created_at",
                                "updated_at",
                                "version"));
        Page<LoginSessionPO> page = new Page<>(1, queryValue.limit(), false);
        return page(page, wrapper).getRecords().stream().map(this::restore).toList();
    }

    /**
     * 保存完整聚合，更新采用版本比较并交换。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean save(LoginSessionAggregate aggregate) {
        try {
            LoginSessionPO po = new LoginSessionPO();
            po.setId(aggregate.entity().id());
            po.setPublicId(aggregate.entity().publicId());
            po.setUserId(aggregate.entity().userId());
            po.setDeviceId(aggregate.entity().deviceId());
            po.setAccessTokenHash(aggregate.entity().accessTokenHash());
            po.setRefreshTokenHash(aggregate.entity().refreshTokenHash());
            po.setCsrfTokenHash(aggregate.entity().csrfTokenHash());
            po.setAuthEpoch(aggregate.entity().authEpoch());
            po.setStatus(aggregate.entity().status());
            po.setRevokeReason(aggregate.entity().revokeReason());
            po.setAccessExpiresAt(aggregate.entity().accessExpiresAt());
            po.setRefreshExpiresAt(aggregate.entity().refreshExpiresAt());
            po.setLastSeenAt(aggregate.entity().lastSeenAt());
            po.setRevokedAt(aggregate.entity().revokedAt());
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
                    new QueryWrapper<LoginSessionPO>()
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

    private LoginSessionAggregate restore(LoginSessionPO po) {
        return new LoginSessionAggregate(
                new LoginSessionEntity(
                        po.getId(),
                        po.getPublicId(),
                        po.getUserId(),
                        po.getDeviceId(),
                        po.getAccessTokenHash(),
                        po.getRefreshTokenHash(),
                        po.getCsrfTokenHash(),
                        po.getAuthEpoch(),
                        po.getStatus(),
                        po.getRevokeReason(),
                        po.getAccessExpiresAt(),
                        po.getRefreshExpiresAt(),
                        po.getLastSeenAt(),
                        po.getRevokedAt(),
                        po.getCreatedAt(),
                        po.getUpdatedAt(),
                        po.getVersion()));
    }
}
