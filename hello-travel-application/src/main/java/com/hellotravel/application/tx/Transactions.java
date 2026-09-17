package com.hellotravel.application.tx;

import com.hellotravel.application.persistence.TravelRepositories;
import com.hellotravel.domain.auth.model.aggregate.UserAccountAggregate;
import com.hellotravel.domain.auth.model.entity.UserAccountEntity;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 短事务和每账号CAS提交序列；异常必须越过事务边界，禁止失败Result偷偷提交。
 *
 * @author AIGenerator
 */
@Component
public final class Transactions {

    private final PlatformTransactionManager manager;

    private final TravelRepositories repositories;

    private final com.hellotravel.application.persistence.DomainWrites writes;

    public Transactions(
            com.hellotravel.application.persistence.DomainWrites writes,
            PlatformTransactionManager manager,
            TravelRepositories repositories) {
        this.manager = manager;
        this.writes = writes;
        this.repositories = repositories;
    }

    /**
     * 在独立短事务中执行操作，异常导致整体回滚。
     *
     * @author AIGenerator
     * @param operation 短事务操作，不包含外部等待
     * @param <T> 业务载荷类型
     * @return 当前操作的业务结果
     */
    public <T> T plain(Supplier<T> operation) {
        TransactionTemplate template = new TransactionTemplate(manager);
        template.setTimeout(10);
        return template.execute(status -> operation.get());
    }

    /**
     * 获取同一只读事务中的一致快照。
     *
     * @author AIGenerator
     * @param operation 短事务操作，不包含外部等待
     * @param <T> 业务载荷类型
     * @return 当前操作的业务结果
     */
    public <T> T snapshot(Supplier<T> operation) {
        TransactionTemplate template = new TransactionTemplate(manager);
        template.setReadOnly(true);
        template.setTimeout(5);
        template.setIsolationLevel(
                org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return template.execute(status -> operation.get());
    }

    /**
     * 通过账号版本锁顺序提交业务与同步事件。
     *
     * @author AIGenerator
     * @param userId 认证账号主键
     * @param operation 短事务操作，不包含外部等待
     * @param <T> 业务载荷类型
     * @return 当前操作的业务结果
     */
    public <T> T mutate(Long userId, Function<UserAccountEntity, T> operation) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return plain(
                        () -> {
                            UserAccountAggregate stored = repositories.userAccount.findById(userId);
                            if (stored == null || !"ACTIVE".equals(stored.entity().status())) {
                                throw new DomainException(DomainErrorCode.UNAUTHORIZED);
                            }
                            UserAccountEntity next = stored.entity().advanceSync();
                            require(writes.saveUserAccount(new UserAccountAggregate(next)));
                            return operation.apply(next);
                        });
            } catch (DomainException exception) {
                if (exception.errorCode() != DomainErrorCode.CONFLICT || attempt == 2) {
                    throw exception;
                }
            } catch (TransientDataAccessException exception) {
                if (attempt == 2) {
                    throw new DomainException(DomainErrorCode.CONFLICT);
                }
            }
        }
        throw new DomainException(DomainErrorCode.CONFLICT);
    }

    /**
     * 拒绝失败结果，交由事务边界回滚。
     *
     * @author AIGenerator
     * @param saved 受控saved参数
     */
    public static void require(Boolean saved) {
        if (!Boolean.TRUE.equals(saved)) {
            throw new DomainException(DomainErrorCode.CONFLICT);
        }
    }
}
