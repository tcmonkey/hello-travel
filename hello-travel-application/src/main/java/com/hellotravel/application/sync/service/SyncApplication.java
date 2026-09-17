package com.hellotravel.application.sync.service;

import com.hellotravel.application.persistence.TravelRepositories;
import com.hellotravel.application.sync.command.SyncCommand;
import com.hellotravel.application.sync.result.SyncEventResult;
import com.hellotravel.application.sync.result.SyncResult;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.query.model.value.QueryValue;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 账号隔离的持久同步补齐，过期/缺口必须重建快照。
 *
 * @author AIGenerator
 */
@Service
public final class SyncApplication {

    private final TravelRepositories repositories;

    public SyncApplication(TravelRepositories repositories) {
        this.repositories = repositories;
    }

    /**
     * 读取当前账号连续同步事件，缺口要求重建快照。
     *
     * @author AIGenerator
     * @param syncCommand 当前用例命令，归属来自服务端
     * @return 归属和状态校验后的业务快照
     */
    public Result<SyncResult> synchronize(SyncCommand syncCommand) {
        try {
            if (syncCommand.limit() < 1
                    || syncCommand.limit() > 200
                    || syncCommand.afterSeq() < 0) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            long high = repositories.userAccount.findById(syncCommand.userId()).entity().syncSeq();
            if (syncCommand.afterSeq() > high) {
                throw new DomainException(DomainErrorCode.SYNC_RESET_REQUIRED);
            }
            var rows =
                    repositories.syncEvent.query(
                            QueryValue.all("event_seq", syncCommand.limit())
                                    .where("user_id", "EQ", syncCommand.userId())
                                    .where("event_seq", "GT", syncCommand.afterSeq())
                                    .where("event_seq", "LE", high)
                                    .where(
                                            "expires_at",
                                            "GT",
                                            java.time.LocalDateTime.now(java.time.ZoneOffset.UTC)));
            if (high > syncCommand.afterSeq()
                    && (rows.isEmpty()
                            || rows.get(0).entity().eventSeq() != syncCommand.afterSeq() + 1)) {
                throw new DomainException(DomainErrorCode.SYNC_RESET_REQUIRED);
            }
            List<SyncEventResult> result =
                    rows.stream()
                            .map(
                                    x ->
                                            new SyncEventResult(
                                                    x.entity().eventSeq(),
                                                    x.entity().eventType(),
                                                    x.entity().aggregatePublicId(),
                                                    x.entity().aggregateVersion(),
                                                    x.entity().targetSessionPublicId(),
                                                    x.entity().payloadJson()))
                            .toList();
            return Result.success(
                    new SyncResult(
                            result,
                            high,
                            !result.isEmpty() && result.get(result.size() - 1).seq() < high));
        } catch (Exception exception) {
            return com.hellotravel.application.support.ApplicationFailures.capture(exception);
        }
    }
}
