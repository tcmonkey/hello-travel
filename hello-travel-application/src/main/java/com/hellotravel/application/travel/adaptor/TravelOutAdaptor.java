package com.hellotravel.application.travel.adaptor;

import com.hellotravel.application.travel.command.TravelCommand;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.travel.TravelDO;

/**
 * 承载TravelOutAdaptor的受控业务契约。
 *
 * @author AIGenerator
 */
public interface TravelOutAdaptor {

    /**
     * 查询实时旅行事实并明确降级状态。
     *
     * @author AIGenerator
     * @param travelCommand 当前用例命令，归属来自服务端
     * @return 当前操作的业务结果
     */
    Result<TravelDO> consult(TravelCommand travelCommand);
}
