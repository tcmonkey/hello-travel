package com.hellotravel.application.chat.travel.planning.command;

/**
 * 旅行计划草稿生成命令。
 *
 * @param requirements 结构化用户需求
 * @param evidence 已核验的资料与实时事实
 * @param instructions 服务端可信规划规则
 * @author AIGenerator
 */
public record TravelPlanGenerationCommand(
        String requirements, String evidence, String instructions) {

    /**
     * 阻止诊断输出泄漏用户需求和资料证据。
     *
     * @return 脱敏类型
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "TravelPlanGenerationCommand{redacted}";
    }
}
