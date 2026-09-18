package com.hellotravel.application.file.assembler;

import com.hellotravel.application.file.command.FileCommand;
import com.hellotravel.application.knowledge.command.KnowledgeCommand;

import org.springframework.stereotype.Component;

/**
 * 文件能力用途映射，不让无关空参数污染业务流程。
 *
 * @author AIGenerator
 */
@Component
public final class FileCommandAssembler {

    /**
     * 将完整资料受理命令投影为文件存储请求。
     *
     * @param command 本次转换的command快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public FileCommand save(KnowledgeCommand command) {
        return new FileCommand("SAVE", command.filename(), command.bytes(), null);
    }

    /**
     * 构造DELETE文件能力请求。
     *
     * @param storageKey 本次转换的storageKey快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public FileCommand delete(String storageKey) {
        return new FileCommand("DELETE", null, null, storageKey);
    }

    /**
     * 构造SCAN文件能力请求。
     *
     * @param storageKey 本次转换的storageKey快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public FileCommand scan(String storageKey) {
        return new FileCommand("SCAN", null, null, storageKey);
    }
}
