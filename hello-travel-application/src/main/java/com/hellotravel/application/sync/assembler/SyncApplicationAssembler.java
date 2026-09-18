package com.hellotravel.application.sync.assembler;

import com.hellotravel.application.sync.result.SyncEventResult;
import com.hellotravel.application.sync.result.SyncResult;
import com.hellotravel.domain.sync.model.aggregate.SyncEventAggregate;
import com.hellotravel.domain.sync.model.entity.SyncEventEntity;

import org.springframework.stereotype.Component;

/**
 * 持久同步事件到应用结果的整体映射。
 *
 * @author AIGenerator
 */
@Component
public final class SyncApplicationAssembler {

    /**
     * 转换持久事件元数据，不携带消息私密正文。
     *
     * @param entity 本次转换的entity快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public SyncEventResult event(SyncEventEntity entity) {
        return new SyncEventResult(
                entity.eventSeq(),
                entity.eventType(),
                entity.aggregatePublicId(),
                entity.aggregateVersion(),
                entity.targetSessionPublicId(),
                entity.payloadJson());
    }

    /**
     * 转换已校验连续性的事件页及高水位。
     *
     * @param rows 本次转换的rows快照
     * @param highWater 本次转换的highWater快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public SyncResult page(java.util.List<SyncEventAggregate> rows, long highWater) {
        // 1. 投影持久事件元数据，应用入口已经校验归属及连续性。
        var items = rows.stream().map(x -> event(x.entity())).toList();
        // 2. 依据最后事件序号判断是否继续补齐，保持原有空页语义。
        return new SyncResult(
                items,
                highWater,
                !items.isEmpty() && items.get(items.size() - 1).seq() < highWater);
    }
}
