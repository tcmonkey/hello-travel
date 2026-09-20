package com.hellotravel.application.knowledge.adaptor;

import com.hellotravel.application.knowledge.command.VectorCommand;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.knowledge.VectorDO;

/**
 * 专用向量集合应用端口。
 *
 * @author AIGenerator
 */
public interface VectorOutAdaptor {

    /**
     * 在项目专用集合执行隔离向量操作。
     *
     * @author AIGenerator
     * @param vectorCommand 当前用例命令，归属来自服务端
     * @return 当前操作的业务结果
     */
    Result<VectorDO> index(VectorCommand vectorCommand);
}
