package com.hellotravel.application.chat.adaptor;

import com.hellotravel.application.chat.command.TravelFactQueryCommand;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.travel.TravelDO;

/**
 * 天气与路线等外部旅行事实防腐端口。
 *
 * @author AIGenerator
 */
public interface TravelFactQueryAdaptor {

    /**
     * 查询天气与路线等有时效性的旅行事实。
     *
     * @param travelFactQueryCommand 事实查询命令
     * @return 带降级状态的旅行事实
     * @author AIGenerator
     */
    Result<TravelDO> query(TravelFactQueryCommand travelFactQueryCommand);
}
