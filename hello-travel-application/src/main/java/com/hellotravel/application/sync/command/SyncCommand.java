package com.hellotravel.application.sync.command;

/**
 * 同账号持久事件补齐命令。
 *
 * @param userId 认证账号
 * @param afterSeq 已确认事件游标
 * @param limit 有界页大小
 * @author AIGenerator
 */
public record SyncCommand(Long userId, Long afterSeq, int limit) {
}
