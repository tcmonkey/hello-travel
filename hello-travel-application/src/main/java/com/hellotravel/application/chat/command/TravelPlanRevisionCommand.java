package com.hellotravel.application.chat.command;

/**
 * 旅行计划有界修订命令。
 *
 * @param requirements 结构化用户需求
 * @param evidence 已核验的资料与实时事实
 * @param draft 当前结构化草稿
 * @param violations 确定性领域规则发现的问题
 * @param instructions 服务端可信修订规则
 * @author AIGenerator
 */
public record TravelPlanRevisionCommand(
        String requirements,
        String evidence,
        String draft,
        String violations,
        String instructions) {

    /**
     * 阻止诊断输出泄漏用户需求、证据和计划草稿。
     *
     * @return 脱敏类型
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "TravelPlanRevisionCommand{redacted}";
    }
}
