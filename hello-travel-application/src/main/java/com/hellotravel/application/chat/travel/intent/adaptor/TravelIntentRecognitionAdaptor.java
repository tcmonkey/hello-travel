package com.hellotravel.application.chat.travel.intent.adaptor;

import com.hellotravel.application.chat.travel.intent.command.TravelIntentCommand;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.travel.TravelIntentDO;

/**
 * 旅行意图识别防腐端口；应用层不感知LangChain4j、Dify或其他模型框架。
 *
 * @author AIGenerator
 */
public interface TravelIntentRecognitionAdaptor {

    /**
     * 识别本轮旅行诉求并抽取业务字段。
     *
     * @param travelIntentCommand 意图识别命令
     * @return 结构化旅行意图
     * @author AIGenerator
     */
    Result<TravelIntentDO> recognize(TravelIntentCommand travelIntentCommand);
}
