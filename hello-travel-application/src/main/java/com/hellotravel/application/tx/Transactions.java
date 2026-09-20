package com.hellotravel.application.tx;

import com.hellotravel.application.auth.AuthWriteAppService;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.domain.auth.model.aggregate.UserAccountAggregate;
import com.hellotravel.domain.auth.model.entity.UserAccountEntity;
import com.hellotravel.domain.auth.repository.UserAccountRepository;

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
    private final UserAccountRepository userAccountRepository;
    private final AuthWriteAppService authWriteAppService;

    public Transactions(
            AuthWriteAppService authWriteAppService,
            PlatformTransactionManager manager,
            UserAccountRepository userAccountRepository) {
        this.manager = manager;
        this.authWriteAppService = authWriteAppService;
        this.userAccountRepository = userAccountRepository;
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
        // 1. 取得已装配的事务模板，供本段后续处理使用。
        TransactionTemplate template = new TransactionTemplate(manager);
        // 2. 映射本段快照字段，业务状态规则不放入PO赋值。
        template.setTimeout(10);
        // 3. 返回本段实际处理结果，保持本层输出契约。
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
        // 1. 取得已装配的事务模板，供本段后续处理使用。
        TransactionTemplate template = new TransactionTemplate(manager);
        // 2. 映射本段快照字段，业务状态规则不放入PO赋值。
        template.setReadOnly(true);
        template.setTimeout(5);
        template.setIsolationLevel(
                org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
        // 3. 返回本段实际处理结果，保持本层输出契约。
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
        // 1. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return plain(
                        () -> {
                            // 1. 按可信内部标识读取账号当前快照。
                            UserAccountAggregate stored =
                                    userAccountRepository.findById(userId);
                            // 2. 账号不存在或不活动时拒绝进入写事务，防止停用后的请求继续提交。
                            if (stored == null || !"ACTIVE".equals(stored.entity().status())) {
                                throw new ApplicationException(ApplicationErrorCode.UNAUTHORIZED);
                            }
                            // 3. 通过领域聚合语义准备业务快照，固定状态由实体封装。
                            UserAccountEntity next =
                                    new UserAccountAggregate(stored.entity())
                                            .advanceSync()
                                            .entity();
                            // 4. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                            require(authWriteAppService.saveUserAccount(new UserAccountAggregate(next)));
                            // 5. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                            return operation.apply(next);
                        });
            } catch (ApplicationException exception) {
                if (exception.errorCode() != ApplicationErrorCode.CONFLICT || attempt == 2) {
                    throw exception;
                }
            } catch (TransientDataAccessException exception) {
                if (attempt == 2) {
                    throw new ApplicationException(ApplicationErrorCode.CONFLICT);
                }
            }
        }
        // 2. 以稳定异常中断当前内部处理，由所属入口转换安全失败。
        throw new ApplicationException(ApplicationErrorCode.CONFLICT);
    }

    /**
     * 拒绝失败结果，交由事务边界回滚。
     *
     * @author AIGenerator
     * @param saved 受控saved参数
     */
    public static void require(Boolean saved) {
        // 1. 未实际写入视为并发冲突，抛给所属事务以回滚整组状态变更。
        if (!Boolean.TRUE.equals(saved)) {
            throw new ApplicationException(ApplicationErrorCode.CONFLICT);
        }
    }
}
