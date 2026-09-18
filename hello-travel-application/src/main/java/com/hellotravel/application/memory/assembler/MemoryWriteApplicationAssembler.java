package com.hellotravel.application.memory.assembler;

import com.hellotravel.domain.memory.model.aggregate.MemoryFactAggregate;
import com.hellotravel.domain.memory.model.aggregate.MemoryFactSourceAggregate;
import com.hellotravel.domain.memory.model.aggregate.MemorySummaryAggregate;
import com.hellotravel.domain.memory.model.param.MemoryFactSourceRemoveParam;
import com.hellotravel.domain.memory.model.param.MemoryFactSourceWriteParam;
import com.hellotravel.domain.memory.model.param.MemoryFactWriteParam;
import com.hellotravel.domain.memory.model.param.MemorySummaryWriteParam;

import org.springframework.stereotype.Component;

/**
 * 记忆应用写入到本域参数的转换，不承担其他业务的映射。
 *
 * @author AIGenerator
 */
@Component
public final class MemoryWriteApplicationAssembler {

    /**
     * 投影MemorySummary写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public MemorySummaryWriteParam write(MemorySummaryAggregate aggregate) {
        return new MemorySummaryWriteParam(aggregate);
    }

    /**
     * 投影MemoryFact写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public MemoryFactWriteParam write(MemoryFactAggregate aggregate) {
        return new MemoryFactWriteParam(aggregate);
    }

    /**
     * 投影MemoryFactSource写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public MemoryFactSourceWriteParam write(MemoryFactSourceAggregate aggregate) {
        return new MemoryFactSourceWriteParam(aggregate);
    }

    /**
     * 投影MemoryFactSource删除参数。
     *
     * @param id 本次转换的id快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public MemoryFactSourceRemoveParam removeMemoryFactSource(Long id) {
        return new MemoryFactSourceRemoveParam(id);
    }
}
