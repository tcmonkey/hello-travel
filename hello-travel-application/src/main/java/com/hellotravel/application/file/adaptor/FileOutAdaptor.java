package com.hellotravel.application.file.adaptor;

import com.hellotravel.application.file.command.FileCommand;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.knowledge.FileDO;

/**
 * 承载FileOutAdaptor的受控业务契约。
 *
 * @author AIGenerator
 */
public interface FileOutAdaptor {

    /**
     * 处理有界私有资料并校验受控存储键。
     *
     * @author AIGenerator
     * @param fileCommand 当前用例命令，归属来自服务端
     * @return 当前操作的业务结果
     */
    Result<FileDO> store(FileCommand fileCommand);
}
