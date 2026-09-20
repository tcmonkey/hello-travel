package com.hellotravel.adaptor.chat.input.assembler;

import com.hellotravel.adaptor.common.HttpProtocol;
import com.hellotravel.application.chat.command.SyncCommand;
import com.hellotravel.application.chat.result.SyncEventAppResult;
import com.hellotravel.application.chat.result.SyncAppResult;
import com.hellotravel.client.sync.response.SyncEventResponse;
import com.hellotravel.client.sync.response.SyncResponse;

import org.springframework.stereotype.Component;

/**
 * 持久事件补齐协议映射。
 *
 * @author AIGenerator
 */
@Component
public final class SyncAssembler {

    /**
     * 将当前用户及游标绑定为默认页大小的同步命令。
     *
     * @param userId 本次转换的userId快照
     * @param after 本次转换的after快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public SyncCommand toCommand(Long userId, long after) {
        return new SyncCommand(userId, after, HttpProtocol.DEFAULT_PAGE_SIZE);
    }

    /**
     * 投影持久同步事件的公开字段。
     *
     * @param value 本次转换的value快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public SyncEventResponse event(SyncEventAppResult value) {
        return new SyncEventResponse(
                value.seq(),
                value.type(),
                value.id(),
                value.version(),
                value.targetSid(),
                value.payload());
    }

    /**
     * 转换同步页及高水位，保留补齐状态。
     *
     * @param result 本次转换的result快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public SyncResponse toResponse(SyncAppResult result) {
        return new SyncResponse(
                result.items().stream().map(this::event).toList(),
                result.highWater(),
                result.hasMore());
    }
}
