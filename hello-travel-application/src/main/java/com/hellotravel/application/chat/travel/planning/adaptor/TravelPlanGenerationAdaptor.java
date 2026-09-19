package com.hellotravel.application.chat.travel.planning.adaptor;

import com.hellotravel.application.chat.travel.planning.command.TravelPlanGenerationCommand;
import com.hellotravel.application.chat.travel.planning.command.TravelPlanRevisionCommand;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.travel.TravelPlanGenerationDO;

/**
 * 旅行计划草稿生成防腐端口；模型框架只负责实现，不拥有规划流程。
 *
 * @author AIGenerator
 */
public interface TravelPlanGenerationAdaptor {

    /**
     * 生成第一版结构化计划草稿。
     *
     * @param travelPlanGenerationCommand 草稿生成命令
     * @return 草稿及模型调用证据
     * @author AIGenerator
     */
    Result<TravelPlanGenerationDO> generate(
            TravelPlanGenerationCommand travelPlanGenerationCommand);

    /**
     * 根据领域违规项修订结构化计划草稿。
     *
     * @param travelPlanRevisionCommand 草稿修订命令
     * @return 修订草稿及模型调用证据
     * @author AIGenerator
     */
    Result<TravelPlanGenerationDO> revise(TravelPlanRevisionCommand travelPlanRevisionCommand);
}
