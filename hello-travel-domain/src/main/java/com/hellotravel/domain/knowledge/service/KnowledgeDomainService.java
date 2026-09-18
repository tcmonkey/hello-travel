package com.hellotravel.domain.knowledge.service;

import com.hellotravel.common.error.Failures;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.annotation.DomainService;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.knowledge.model.param.IndexJobWriteParam;
import com.hellotravel.domain.knowledge.model.param.KnowledgeChunkWriteParam;
import com.hellotravel.domain.knowledge.model.param.KnowledgeDocumentWriteParam;
import com.hellotravel.domain.knowledge.repository.IndexJobRepository;
import com.hellotravel.domain.knowledge.repository.KnowledgeChunkRepository;
import com.hellotravel.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.hellotravel.model.persistence.WriteDO;

/**
 * 知识库业务的写入协调，仅使用本域聚合和仓储；事务由应用层管理。
 *
 * @author AIGenerator
 */
@DomainService
public final class KnowledgeDomainService {

    private final KnowledgeDocumentRepository knowledgeDocument;
    private final KnowledgeChunkRepository knowledgeChunk;
    private final IndexJobRepository indexJob;

    public KnowledgeDomainService(
            KnowledgeDocumentRepository knowledgeDocument,
            KnowledgeChunkRepository knowledgeChunk,
            IndexJobRepository indexJob) {
        this.knowledgeDocument = knowledgeDocument;
        this.knowledgeChunk = knowledgeChunk;
        this.indexJob = indexJob;
    }

    /**
     * 校验KnowledgeDocument完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveKnowledgeDocument(KnowledgeDocumentWriteParam param) {
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
                var stored = knowledgeDocument.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(knowledgeDocument.save(next));
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
     * 校验KnowledgeChunk完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveKnowledgeChunk(KnowledgeChunkWriteParam param) {
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
                var stored = knowledgeChunk.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(knowledgeChunk.save(next));
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
     * 校验IndexJob完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param param 领域操作参数
     * @return 当前操作的业务结果
     */
    public Result<WriteDO> saveIndexJob(IndexJobWriteParam param) {
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
                var stored = indexJob.findById(next.idForPersistence());
                next.assertWritableAgainst(stored);
            }
            // 5. 保存完整聚合并检查仓储CAS结果，冲突不能作为成功提交。
            boolean saved = Boolean.TRUE.equals(indexJob.save(next));
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
