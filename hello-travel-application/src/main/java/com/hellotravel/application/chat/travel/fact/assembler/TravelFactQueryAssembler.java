package com.hellotravel.application.chat.travel.fact.assembler;

import com.hellotravel.application.chat.travel.fact.command.TravelFactQueryCommand;
import com.hellotravel.model.travel.TravelIntentDO;
import com.hellotravel.model.travel.TravelIntentMode;

import org.springframework.stereotype.Component;

/**
 * 将结构化旅行意图投影为外部事实查询命令。
 *
 * @author AIGenerator
 */
@Component
public final class TravelFactQueryAssembler {

    /**
     * 将旅行意图转换为事实查询命令。
     *
     * @param intent 结构化旅行意图
     * @return 事实查询命令
     * @author AIGenerator
     */
    public TravelFactQueryCommand command(TravelIntentDO intent) {
        // 1. 完整旅行规划默认需要天气，并在起终点完整时查询参考路线。
        boolean planning = intent.mode() == TravelIntentMode.PLANNING;
        // 2. 优先使用明确城市，缺失时以目的地作为天气查询地点。
        return new TravelFactQueryCommand(
                intent.city() == null || intent.city().isBlank()
                        ? intent.destination()
                        : intent.city(),
                intent.origin(),
                intent.destination(),
                intent.weather() || planning,
                intent.route()
                        || (planning
                                && intent.origin() != null
                                && intent.destination() != null));
    }
}
