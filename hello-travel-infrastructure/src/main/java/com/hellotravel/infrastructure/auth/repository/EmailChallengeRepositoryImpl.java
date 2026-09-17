package com.hellotravel.infrastructure.auth.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hellotravel.domain.auth.model.aggregate.EmailChallengeAggregate;
import com.hellotravel.domain.auth.model.entity.EmailChallengeEntity;
import com.hellotravel.domain.auth.repository.EmailChallengeRepository;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.infrastructure.TravelBaseRepository;
import com.hellotravel.infrastructure.auth.mysql.mapper.EmailChallengeMapper;
import com.hellotravel.infrastructure.auth.mysql.pojo.EmailChallengePO;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * ht_email_challenge仓储，标准安全条件构造器与显式版本CAS，不使用自定义SQL。
 *
 * @author AIGenerator
 */
@Repository
public class EmailChallengeRepositoryImpl
        extends TravelBaseRepository<EmailChallengeMapper, EmailChallengePO>
        implements EmailChallengeRepository {

    /**
     * 按内部主键恢复完整聚合；不存在时返回空值。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public EmailChallengeAggregate findById(Long id) {
        EmailChallengePO po = getById(id);
        return po == null ? null : restore(po);
    }

    /**
     * 按可信条件读取有界聚合集合。
     *
     * @author AIGenerator
     * @param queryValue 可信有界查询条件
     * @return 当前操作的业务结果
     */
    public List<EmailChallengeAggregate> query(QueryValue queryValue) {
        QueryWrapper<EmailChallengePO> wrapper =
                conditions(
                        queryValue,
                        Set.of(
                                "id",
                                "public_id",
                                "email_normalized",
                                "purpose",
                                "code_hmac",
                                "code_key_version",
                                "delivery_ciphertext",
                                "delivery_key_version",
                                "status",
                                "attempts",
                                "max_attempts",
                                "expires_at",
                                "consumed_at",
                                "created_at",
                                "updated_at",
                                "version"));
        Page<EmailChallengePO> page = new Page<>(1, queryValue.limit(), false);
        return page(page, wrapper).getRecords().stream().map(this::restore).toList();
    }

    /**
     * 保存完整聚合，更新采用版本比较并交换。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean save(EmailChallengeAggregate aggregate) {
        try {
            EmailChallengePO po = new EmailChallengePO();
            po.setId(aggregate.entity().id());
            po.setPublicId(aggregate.entity().publicId());
            po.setEmailNormalized(aggregate.entity().emailNormalized());
            po.setPurpose(aggregate.entity().purpose());
            po.setCodeHmac(aggregate.entity().codeHmac());
            po.setCodeKeyVersion(aggregate.entity().codeKeyVersion());
            po.setDeliveryCiphertext(aggregate.entity().deliveryCiphertext());
            po.setDeliveryKeyVersion(aggregate.entity().deliveryKeyVersion());
            po.setStatus(aggregate.entity().status());
            po.setAttempts(aggregate.entity().attempts());
            po.setMaxAttempts(aggregate.entity().maxAttempts());
            po.setExpiresAt(aggregate.entity().expiresAt());
            po.setConsumedAt(aggregate.entity().consumedAt());
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
                    new QueryWrapper<EmailChallengePO>()
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

    private EmailChallengeAggregate restore(EmailChallengePO po) {
        return new EmailChallengeAggregate(
                new EmailChallengeEntity(
                        po.getId(),
                        po.getPublicId(),
                        po.getEmailNormalized(),
                        po.getPurpose(),
                        po.getCodeHmac(),
                        po.getCodeKeyVersion(),
                        po.getDeliveryCiphertext(),
                        po.getDeliveryKeyVersion(),
                        po.getStatus(),
                        po.getAttempts(),
                        po.getMaxAttempts(),
                        po.getExpiresAt(),
                        po.getConsumedAt(),
                        po.getCreatedAt(),
                        po.getUpdatedAt(),
                        po.getVersion()));
    }
}
