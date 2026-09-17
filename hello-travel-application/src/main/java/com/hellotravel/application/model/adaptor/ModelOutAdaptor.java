package com.hellotravel.application.model.adaptor;

import com.hellotravel.application.model.command.ModelCommand;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.travel.ModelDO;

/**
 * LangChain4j模型能力的应用端口。
 *
 * @author AIGenerator
 */
public interface ModelOutAdaptor {

    /**
     * 调用有界模型接口，失败返回明确不可用分类。
     *
     * @author AIGenerator
     * @param modelCommand 当前用例命令，归属来自服务端
     * @return 当前操作的业务结果
     */
    Result<ModelDO> generate(ModelCommand modelCommand);
}
