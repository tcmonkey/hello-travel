package com.hellotravel.application.auth.security.adaptor;

import com.hellotravel.application.auth.security.command.SecurityCommand;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.security.SecurityDO;

/**
 * 安全能力及敏感入口频控端口。
 *
 * @author AIGenerator
 */
public interface SecurityOutAdaptor {

    /**
     * 处理process对应的受控业务操作。
     *
     * @author AIGenerator
     * @param securityCommand 当前用例命令，归属来自服务端
     * @return 当前操作的业务结果
     */
    Result<SecurityDO> process(SecurityCommand securityCommand);
}
