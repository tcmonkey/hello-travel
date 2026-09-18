package com.hellotravel.domain.sync.service;

import com.hellotravel.common.error.Failures;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.annotation.DomainService;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.sync.model.param.OutboxEventWriteParam;
import com.hellotravel.domain.sync.model.param.SyncEventRemoveParam;
import com.hellotravel.domain.sync.model.param.SyncEventWriteParam;
import com.hellotravel.domain.sync.repository.OutboxEventRepository;
import com.hellotravel.domain.sync.repository.SyncEventRepository;
import com.hellotravel.model.persistence.WriteDO;

/**
 * 同步业务的写入协调，仅使用本域聚合和仓储；事务由应用层管理。
 *
 * @author AIGenerator
 */
@DomainService
public final class SyncDomainService {

    private final SyncEventRepository syncEvent;
    private final OutboxEventRepository outboxEvent;

    public SyncDomainService(
            SyncEventRepository syncEvent, OutboxEventRepository outboxEvent) {
        this.syncEvent = syncEvent;
        this.outboxEvent = outboxEvent;
    }

    /**
     * 校验SyncEvent完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveSyncEvent(SyncEventWriteParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.aggregate() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 准备当前变更后的不可变快照，旧版本保留给仓储CAS。
            var next = param.aggregate();
            // 3. 校验聚合完整性，空实体不能进入仓储。
            next.assertComplete();
            // 4. 更新前加载已持久化聚合，新增快照无需虚构旧记录。
            if (next.idForPersistence() != null) {
                var stored = syncEvent.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(syncEvent.save(next));
            // 6. 拒绝未写入的CAS结果，使所属事务回滚而非继续发布事件。
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            // 7. 返回实际成功写入标记，版本或归属校验失败不会走到此处。
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 清理SyncEvent的指定失效记录。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> removeSyncEvent(SyncEventRemoveParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 返回实际删除标记，不把空匹配伪造成已删除记录。
            return Result.success(new WriteDO(Boolean.TRUE.equals(syncEvent.remove(param.id()))));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 校验OutboxEvent完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveOutboxEvent(OutboxEventWriteParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.aggregate() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 准备当前变更后的不可变快照，旧版本保留给仓储CAS。
            var next = param.aggregate();
            // 3. 校验聚合完整性，空实体不能进入仓储。
            next.assertComplete();
            // 4. 更新前加载已持久化聚合，新增快照无需虚构旧记录。
            if (next.idForPersistence() != null) {
                var stored = outboxEvent.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(outboxEvent.save(next));
            // 6. 拒绝未写入的CAS结果，使所属事务回滚而非继续发布事件。
            if (!saved) {
                throw new DomainException(DomainErrorCode.CONFLICT);
            }
            // 7. 返回实际成功写入标记，版本或归属校验失败不会走到此处。
            return Result.success(new WriteDO(true));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }
}
