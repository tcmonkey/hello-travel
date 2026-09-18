package com.hellotravel.domain.memory.service;

import com.hellotravel.common.error.Failures;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.annotation.DomainService;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.memory.model.param.MemoryFactSourceRemoveParam;
import com.hellotravel.domain.memory.model.param.MemoryFactSourceWriteParam;
import com.hellotravel.domain.memory.model.param.MemoryFactWriteParam;
import com.hellotravel.domain.memory.model.param.MemorySummaryWriteParam;
import com.hellotravel.domain.memory.repository.MemoryFactRepository;
import com.hellotravel.domain.memory.repository.MemoryFactSourceRepository;
import com.hellotravel.domain.memory.repository.MemorySummaryRepository;
import com.hellotravel.model.persistence.WriteDO;

/**
 * 记忆业务的写入协调，仅使用本域聚合和仓储；事务由应用层管理。
 *
 * @author AIGenerator
 */
@DomainService
public final class MemoryDomainService {

    private final MemorySummaryRepository memorySummary;
    private final MemoryFactRepository memoryFact;
    private final MemoryFactSourceRepository memoryFactSource;

    public MemoryDomainService(
            MemorySummaryRepository memorySummary,
            MemoryFactRepository memoryFact,
            MemoryFactSourceRepository memoryFactSource) {
        this.memorySummary = memorySummary;
        this.memoryFact = memoryFact;
        this.memoryFactSource = memoryFactSource;
    }

    /**
     * 校验MemorySummary完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveMemorySummary(MemorySummaryWriteParam param) {
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
                var stored = memorySummary.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(memorySummary.save(next));
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
     * 校验MemoryFact完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveMemoryFact(MemoryFactWriteParam param) {
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
                var stored = memoryFact.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(memoryFact.save(next));
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
     * 校验MemoryFactSource完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveMemoryFactSource(MemoryFactSourceWriteParam param) {
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
                var stored = memoryFactSource.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(memoryFactSource.save(next));
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
     * 清理MemoryFactSource的指定失效记录。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> removeMemoryFactSource(MemoryFactSourceRemoveParam param) {
        try {
            // 1. 核对输入或读取结果的存在性，失败中止当前处理。
            if (param == null || param.id() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            // 2. 返回实际删除标记，不把空匹配伪造成已删除记录。
            return Result.success(
                    new WriteDO(Boolean.TRUE.equals(memoryFactSource.remove(param.id()))));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }
}
