package com.hellotravel.application.sync.service;

import com.hellotravel.application.auth.support.AuthRepositories;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.application.sync.assembler.SyncApplicationAssembler;
import com.hellotravel.application.sync.command.SyncCommand;
import com.hellotravel.application.sync.result.SyncResult;
import com.hellotravel.application.sync.support.SyncRepositories;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.query.model.value.QueryValue;

import org.springframework.stereotype.Service;

/**
 * 账号隔离的持久同步补齐，过期/缺口必须重建快照。
 *
 * @author AIGenerator
 */
@Service
public final class SyncApplication {

    private final SyncRepositories syncRepositories;
    private final AuthRepositories authRepositories;
    private final SyncApplicationAssembler syncApplicationAssembler;

    public SyncApplication(
            SyncRepositories syncRepositories,
            AuthRepositories authRepositories,
            SyncApplicationAssembler syncApplicationAssembler) {
        this.syncRepositories = syncRepositories;
        this.authRepositories = authRepositories;
        this.syncApplicationAssembler = syncApplicationAssembler;
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
            // 1. 核对分页游标与快照上界，防止越界或无法推进的恢复。
            if (syncCommand.limit() < 1
                    || syncCommand.limit() > 200
                    || syncCommand.afterSeq() < 0) {
                throw new ApplicationException(ApplicationErrorCode.INVALID);
            }
            // 2. 按可信内部标识读取账号当前快照。
            long high =
                    authRepositories.userAccount.findById(syncCommand.userId()).entity().syncSeq();
            // 3. 核对分页游标与快照上界，防止越界或无法推进的恢复。
            if (syncCommand.afterSeq() > high) {
                throw new ApplicationException(ApplicationErrorCode.SYNC_RESET_REQUIRED);
            }
            // 4. 读取同步事件，限定当前用户及查询窗口。
            var rows =
                    syncRepositories.syncEvent.query(
                            QueryValue.all("event_seq", syncCommand.limit())
                                    .where("user_id", "EQ", syncCommand.userId())
                                    .where("event_seq", "GT", syncCommand.afterSeq())
                                    .where("event_seq", "LE", high)
                                    .where(
                                            "expires_at",
                                            "GT",
                                            java.time.LocalDateTime.now(java.time.ZoneOffset.UTC)));
            // 5. 持久事件出现保留窗口缺口时要求客户端全量恢复，不能跳过丢失事件。
            if (high > syncCommand.afterSeq()
                    && (rows.isEmpty()
                            || rows.get(0).entity().eventSeq() != syncCommand.afterSeq() + 1)) {
                throw new ApplicationException(ApplicationErrorCode.SYNC_RESET_REQUIRED);
            }
            // 6. 将本层成功数据封装为标准结果，保持对外模型隔离。
            return Result.success(syncApplicationAssembler.page(rows, high));
        } catch (Exception exception) {
            return ApplicationFailures.capture(exception);
        }
    }
}
