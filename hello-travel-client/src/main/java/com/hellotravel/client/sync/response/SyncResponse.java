package com.hellotravel.client.sync.response;

/**
 * 事件补齐结果。
 *
 * @param items 持久事件
 * @param highWater 当前用户序号
 * @param hasMore 是否还有下一页
 * @author AIGenerator
 */
public record SyncResponse(
        java.util.List<SyncEventResponse> items, long highWater, boolean hasMore) {
}
