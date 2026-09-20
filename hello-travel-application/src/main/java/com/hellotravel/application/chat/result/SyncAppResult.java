package com.hellotravel.application.chat.result;

/**
 * 事件补齐结果。
 *
 * @param items 持久事件
 * @param highWater 当前用户序号
 * @param hasMore 是否还有下一页
 * @author AIGenerator
 */
public record SyncAppResult(java.util.List<SyncEventAppResult> items, long highWater, boolean hasMore) {
}
