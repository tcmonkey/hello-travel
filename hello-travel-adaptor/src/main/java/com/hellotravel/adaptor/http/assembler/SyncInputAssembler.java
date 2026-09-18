package com.hellotravel.adaptor.http.assembler;

import com.hellotravel.adaptor.http.support.HttpProtocol;
import com.hellotravel.application.sync.command.SyncCommand;
import com.hellotravel.application.sync.result.SyncEventResult;
import com.hellotravel.application.sync.result.SyncResult;
import com.hellotravel.client.sync.response.SyncEventResponse;
import com.hellotravel.client.sync.response.SyncResponse;

import org.springframework.stereotype.Component;

/**
 * 持久事件补齐协议映射。
 *
 * @author AIGenerator
 */
@Component
public final class SyncInputAssembler {

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
    public SyncEventResponse event(SyncEventResult value) {
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
    public SyncResponse toResponse(SyncResult result) {
        return new SyncResponse(
                result.items().stream().map(this::event).toList(),
                result.highWater(),
                result.hasMore());
    }
}
