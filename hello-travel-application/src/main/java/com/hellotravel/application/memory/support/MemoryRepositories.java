package com.hellotravel.application.memory.support;

import com.hellotravel.domain.memory.repository.MemoryFactRepository;
import com.hellotravel.domain.memory.repository.MemoryFactSourceRepository;
import com.hellotravel.domain.memory.repository.MemorySummaryRepository;

import org.springframework.stereotype.Component;

/**
 * 记忆应用读取所需的本域仓储端口，不聚合其他业务仓储。
 *
 * @author AIGenerator
 */
@Component
public final class MemoryRepositories {

    public final MemorySummaryRepository memorySummary;
    public final MemoryFactRepository memoryFact;
    public final MemoryFactSourceRepository memoryFactSource;

    public MemoryRepositories(
            MemorySummaryRepository memorySummary,
            MemoryFactRepository memoryFact,
            MemoryFactSourceRepository memoryFactSource) {
        this.memorySummary = memorySummary;
        this.memoryFact = memoryFact;
        this.memoryFactSource = memoryFactSource;
    }
}
