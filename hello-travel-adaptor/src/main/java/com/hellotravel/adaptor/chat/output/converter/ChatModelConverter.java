package com.hellotravel.adaptor.chat.output.converter;

import com.hellotravel.model.chat.ChatModelDO;

import org.springframework.stereotype.Component;

/**
 * 将模型框架结果转换为稳定内部模型；缺失usage时保持未知。
 *
 * @author AIGenerator
 */
@Component
public final class ChatModelConverter {

    /**
     * 转换模型正文和实际模型名，未知usage保持为空。
     *
     * @param text 模型正文
     * @param model 实际模型名
     * @return 稳定模型响应
     * @author AIGenerator
     */
    public ChatModelDO response(String text, String model) {
        return new ChatModelDO(text, null, null, null, model);
    }
}
