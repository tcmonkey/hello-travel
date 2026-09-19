package com.hellotravel.application.auth.email.adaptor;

import com.hellotravel.application.auth.email.command.MailCommand;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.auth.MailDO;

/**
 * 承载MailOutAdaptor的受控业务契约。
 *
 * @author AIGenerator
 */
public interface MailOutAdaptor {

    /**
     * 发送真实验证码邮件，未知结果不自动重复发送。
     *
     * @author AIGenerator
     * @param mailCommand 当前用例命令，归属来自服务端
     * @return 当前操作的业务结果
     */
    Result<MailDO> deliver(MailCommand mailCommand);
}
