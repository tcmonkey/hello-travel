package com.hellotravel.model.travel;

import java.util.List;

/**
 * 旅行需求或计划草稿的领域校验结果。
 *
 * @param valid 是否满足当前阶段规则
 * @param revisable 违规项是否可通过重写草稿修正
 * @param violations 结构化违规说明
 * @param questions 需要用户补充的信息
 * @author AIGenerator
 */
public record TravelPlanValidationDO(
        boolean valid,
        boolean revisable,
        List<String> violations,
        List<String> questions) {

    /**
     * 将校验集合规范为不可变空集合。
     *
     * @author AIGenerator
     */
    public TravelPlanValidationDO {
        violations = violations == null ? List.of() : List.copyOf(violations);
        questions = questions == null ? List.of() : List.copyOf(questions);
    }
}
