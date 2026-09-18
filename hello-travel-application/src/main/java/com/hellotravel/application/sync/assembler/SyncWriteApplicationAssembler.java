package com.hellotravel.application.sync.assembler;

import com.hellotravel.domain.sync.model.aggregate.OutboxEventAggregate;
import com.hellotravel.domain.sync.model.aggregate.SyncEventAggregate;
import com.hellotravel.domain.sync.model.param.OutboxEventWriteParam;
import com.hellotravel.domain.sync.model.param.SyncEventRemoveParam;
import com.hellotravel.domain.sync.model.param.SyncEventWriteParam;

import org.springframework.stereotype.Component;

/**
 * 同步应用写入到本域参数的转换，不承担其他业务的映射。
 *
 * @author AIGenerator
 */
@Component
public final class SyncWriteApplicationAssembler {

    /**
     * 投影SyncEvent写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public SyncEventWriteParam write(SyncEventAggregate aggregate) {
        return new SyncEventWriteParam(aggregate);
    }

    /**
     * 投影SyncEvent删除参数。
     *
     * @param id 本次转换的id快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public SyncEventRemoveParam removeSyncEvent(Long id) {
        return new SyncEventRemoveParam(id);
    }

    /**
     * 投影OutboxEvent写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public OutboxEventWriteParam write(OutboxEventAggregate aggregate) {
        return new OutboxEventWriteParam(aggregate);
    }
}
