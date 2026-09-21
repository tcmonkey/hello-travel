package com.hellotravel.domain.travel.service;

import com.hellotravel.common.error.Failures;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.annotation.DomainService;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.travel.model.param.TravelPlanDraftValidationParam;
import com.hellotravel.domain.travel.model.param.TravelPlanRequestValidationParam;
import com.hellotravel.domain.travel.model.param.TravelPlanScheduleNormalizationParam;
import com.hellotravel.model.travel.TravelIntentDO;
import com.hellotravel.model.travel.TravelPlanDayDO;
import com.hellotravel.model.travel.TravelPlanDraftDO;
import com.hellotravel.model.travel.TravelPlanItemDO;
import com.hellotravel.model.travel.TravelPlanValidationDO;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 旅行计划的确定性规则；模型只生成草稿，不能绕过本服务形成可交付计划。
 *
 * @author AIGenerator
 */
@DomainService
public final class TravelPlanDomainService {

    /**
     * 统一输出模型时间字段的小时分钟格式。
     *
     * @author AIGenerator
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 核对进入规划图前的必要旅行信息。
     *
     * @param param 领域操作参数
     * @return 可规划结果或待补充问题
     * @author AIGenerator
     */
    public Result<TravelPlanValidationDO> validateRequest(
            TravelPlanRequestValidationParam param) {
        try {
            // 1. 拒绝空意图，防止将框架解析失败当作有效规划请求。
            if (param == null || param.intent() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            TravelIntentDO travelIntentDO = param.intent();
            // 2. 收集用户可直接回答的缺失信息，不让模型自行猜测。
            List<String> questions = new ArrayList<>();
            if (blank(travelIntentDO.destination())) {
                questions.add("请告诉我此次旅行的目的地。");
            }
            if (travelIntentDO.travelers() == null || travelIntentDO.travelers() < 1) {
                questions.add("请补充出行人数，以及是否有儿童、老人或其他特殊需求。");
            }
            boolean hasDates = !blank(travelIntentDO.startDate()) && !blank(travelIntentDO.endDate());
            boolean hasDays = travelIntentDO.days() != null && travelIntentDO.days() > 0;
            if (!hasDates && !hasDays) {
                questions.add("请补充出发和返程日期，或者计划游玩天数。");
            }
            // 3. 日期和人数超出一期可控边界时返回明确业务违规。
            List<String> violations = new ArrayList<>();
            validateRequestRange(travelIntentDO, hasDates, violations);
            if (!questions.isEmpty()) {
                return Result.success(new TravelPlanValidationDO(false, false, violations, questions));
            }
            // 4. 只有缺失信息解决且范围合法时才允许生成草稿。
            return Result.success(
                    new TravelPlanValidationDO(violations.isEmpty(), false, violations, List.of()));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 校验模型草稿的日期、时间、接驳、预算和事实标记。
     *
     * @param param 领域操作参数
     * @return 可交付状态或可修订违规项
     * @author AIGenerator
     */
    public Result<TravelPlanValidationDO> validateDraft(
            TravelPlanDraftValidationParam param) {
        try {
            // 1. 拒绝空草稿或空行程，避免把空模型输出当作规划成功。
            if (param == null || param.intent() == null || param.draft() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            TravelIntentDO travelIntentDO = param.intent();
            TravelPlanDraftDO travelPlanDraftDO = param.draft();
            List<String> violations = new ArrayList<>();
            if (blank(travelPlanDraftDO.title()) || blank(travelPlanDraftDO.summary())) {
                violations.add("计划必须包含标题和摘要。");
            }
            if (travelPlanDraftDO.days().isEmpty()) {
                violations.add("计划必须包含至少一天的行程。");
            }
            // 2. 逐日校验日期唯一性、活动数量、时间顺序和接驳预留。
            Set<LocalDate> dates = new HashSet<>();
            LocalDate previousDate = null;
            for (TravelPlanDayDO day : travelPlanDraftDO.days()) {
                LocalDate currentDate = validateDay(day, dates, violations);
                if (currentDate != null
                        && previousDate != null
                        && !currentDate.isAfter(previousDate)) {
                    violations.add("逐日行程必须按日期升序排列：" + day.date());
                }
                if (currentDate != null) {
                    previousDate = currentDate;
                }
            }
            // 3. 有明确日期或天数时核对草稿覆盖，防止少天或多天。
            validateDayCount(travelIntentDO, travelPlanDraftDO, violations);
            validateDateCoverage(travelIntentDO, dates, violations);
            // 4. 有明确预算时核对总额，不将超支草稿直接交付。
            validateCosts(travelPlanDraftDO, violations);
            if (travelIntentDO.budget() != null
                    && travelPlanDraftDO.estimatedTotal() == null) {
                violations.add("用户提供预算时，计划必须给出总费用估算或说明仍待核实。");
            } else if (travelIntentDO.budget() != null
                    && travelPlanDraftDO.estimatedTotal().compareTo(travelIntentDO.budget()) > 0) {
                violations.add("预估总费用超出用户预算。");
            }
            // 5. 草稿结构违规可交给模型有界修订，但最终仍必须再次通过本校验。
            return Result.success(
                    new TravelPlanValidationDO(
                            violations.isEmpty(), !violations.isEmpty(), violations, List.of()));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    /**
     * 仅顺延模型草稿中接驳时间不足的后续活动，保留活动内容、费用与事实标记。
     *
     * @param param 领域操作参数
     * @return 已规整的草稿；其他结构问题仍交由草稿校验和模型修订处理
     * @author AIGenerator
     */
    public Result<TravelPlanDraftDO> normalizeSchedule(
            TravelPlanScheduleNormalizationParam param) {
        try {
            // 1. 空草稿没有可规整的排程，交由既有草稿校验返回明确失败。
            if (param == null || param.draft() == null) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            TravelPlanDraftDO draft = param.draft();
            // 2. 逐日顺序规整时间，绝不改写地点、活动、费用或来源等模型事实内容。
            List<TravelPlanDayDO> normalizedDays = draft.days().stream()
                    .map(this::normalizeDaySchedule)
                    .toList();
            // 3. 返回不可变的新草稿，后续仍须经过完整领域校验才能向用户展示。
            return Result.success(new TravelPlanDraftDO(
                    draft.title(),
                    draft.summary(),
                    normalizedDays,
                    draft.estimatedTotal(),
                    draft.priorities(),
                    draft.reminders(),
                    draft.unverifiedItems()));
        } catch (Exception exception) {
            return Failures.capture(exception, DomainErrorCode.FAILED);
        }
    }

    private TravelPlanDayDO normalizeDaySchedule(TravelPlanDayDO day) {
        // 1. 不完整的日期对象保留原样，避免规整器掩盖必须由校验器发现的结构错误。
        if (day == null || day.items().isEmpty()) {
            return day;
        }
        LocalTime previousEnd = null;
        List<TravelPlanItemDO> normalizedItems = new ArrayList<>();
        for (TravelPlanItemDO item : day.items()) {
            // 2. 每项只在开始、结束和接驳分钟都可解析时顺延，其他问题保留给后续修订。
            ScheduleItem scheduleItem = normalizeItemSchedule(item, previousEnd);
            normalizedItems.add(scheduleItem.item());
            previousEnd = scheduleItem.endTime();
        }
        // 2. 使用规整后的活动替换当天列表，日期和城市保持模型原始语义。
        return new TravelPlanDayDO(day.date(), day.city(), normalizedItems);
    }

    private ScheduleItem normalizeItemSchedule(TravelPlanItemDO item, LocalTime previousEnd) {
        // 1. 首项或缺少接驳分钟的活动不擅自调整，其完整性仍由领域校验负责。
        if (item == null || previousEnd == null || item.transitMinutes() == null
                || item.transitMinutes() < 0) {
            return new ScheduleItem(item, parseEndTime(item));
        }
        // 2. 可规整活动才进入时间计算，解析失败时保持原值等待领域校验。
        try {
            // 1. 解析活动原始起止时间，无法解析时不掩盖模型草稿的问题。
            LocalTime start = LocalTime.parse(item.startTime());
            LocalTime end = LocalTime.parse(item.endTime());
            if (!end.isAfter(start)) {
                return new ScheduleItem(item, end);
            }
            // 2. 计算上一项结束加接驳后的最早开始时刻，只有不足时才整体顺延本项。
            LocalTime earliestStart = previousEnd.plusMinutes(item.transitMinutes());
            if (!start.isBefore(earliestStart)) {
                return new ScheduleItem(item, end);
            }
            LocalTime normalizedEnd = earliestStart.plusMinutes(
                    Duration.between(start, end).toMinutes());
            if (normalizedEnd.isBefore(earliestStart)) {
                return new ScheduleItem(item, end);
            }
            // 3. 仅替换时间字段，防止排程规整器越界修改模型输出的业务内容。
            return new ScheduleItem(
                    new TravelPlanItemDO(
                            TIME_FORMATTER.format(earliestStart),
                            TIME_FORMATTER.format(normalizedEnd),
                            item.place(),
                            item.activity(),
                            item.transport(),
                            item.transitMinutes(),
                            item.estimatedCost(),
                            item.sourceId(),
                            item.verified(),
                            item.verificationNote()),
                    normalizedEnd);
        } catch (DateTimeParseException | NullPointerException exception) {
            return new ScheduleItem(item, parseEndTime(item));
        }
    }

    private LocalTime parseEndTime(TravelPlanItemDO item) {
        // 1. 无法解析的结束时间不能成为下一项排程基线，保留原草稿等待领域校验。
        try {
            return item == null ? null : LocalTime.parse(item.endTime());
        } catch (DateTimeParseException | NullPointerException exception) {
            return null;
        }
    }

    private record ScheduleItem(TravelPlanItemDO item, LocalTime endTime) {
    }

    private void validateRequestRange(
            TravelIntentDO travelIntentDO, boolean hasDates, List<String> violations) {
        // 1. 人数和天数超出一期服务范围时返回明确边界。
        if (travelIntentDO.travelers() != null && travelIntentDO.travelers() > 50) {
            violations.add("一期单次规划最多支持50人。");
        }
        if (travelIntentDO.days() != null
                && (travelIntentDO.days() < 1 || travelIntentDO.days() > 30)) {
            violations.add("一期单次规划天数必须在1至30天之间。");
        }
        if (!hasDates) {
            return;
        }
        // 2. 解析明确日期并校验先后顺序与最长时间窗。
        try {
            // 1. 将开始和结束日期解析为不可变日期值。
            LocalDate start = LocalDate.parse(travelIntentDO.startDate());
            LocalDate end = LocalDate.parse(travelIntentDO.endDate());
            // 2. 核对包含首尾日期的完整天数是否落在一期边界内。
            long days = ChronoUnit.DAYS.between(start, end) + 1;
            if (days < 1 || days > 30) {
                violations.add("出发日期必须不晚于返程日期，且行程不得超过30天。");
            }
        } catch (DateTimeParseException exception) {
            violations.add("出发和返程日期必须使用YYYY-MM-DD格式。");
        }
    }

    private LocalDate validateDay(
            TravelPlanDayDO day, Set<LocalDate> dates, List<String> violations) {
        // 1. 校验日期和城市存在且日期在草稿中唯一。
        if (day == null || blank(day.date()) || blank(day.city())) {
            violations.add("每日行程必须包含日期和城市。");
            return null;
        }
        LocalDate currentDate = null;
        try {
            // 1. 解析并登记每日日期，阻止跨日计划出现重复日期。
            LocalDate date = LocalDate.parse(day.date());
            currentDate = date;
            // 2. 同一日期重复出现时登记领域违规，后续草稿必须定向修订。
            if (!dates.add(date)) {
                violations.add("逐日行程不得出现重复日期：" + day.date());
            }
        } catch (DateTimeParseException exception) {
            violations.add("行程日期格式错误：" + day.date());
        }
        // 2. 限制单日活动数量，并逐项校验时间与接驳。
        if (day.items().isEmpty() || day.items().size() > 20) {
            violations.add("每日必须包含1至20个活动：" + day.date());
            return currentDate;
        }
        LocalTime previousEnd = null;
        for (TravelPlanItemDO item : day.items()) {
            previousEnd = validateItem(day.date(), item, previousEnd, violations);
        }
        return currentDate;
    }

    private LocalTime validateItem(
            String date,
            TravelPlanItemDO item,
            LocalTime previousEnd,
            List<String> violations) {
        // 1. 地点与活动是可执行行程的必要语义，空值不进入后续计算。
        if (item == null || blank(item.place()) || blank(item.activity())) {
            violations.add("行程活动必须包含地点和内容：" + date);
            return previousEnd;
        }
        // 2. 校验开始、结束和前后活动时间，防止倒序或重叠。
        try {
            // 1. 解析活动起止时间并核对活动自身时间顺序。
            LocalTime start = LocalTime.parse(item.startTime());
            LocalTime end = LocalTime.parse(item.endTime());
            if (!end.isAfter(start)) {
                violations.add("活动结束时间必须晚于开始时间：" + item.place());
            }
            if (previousEnd != null && start.isBefore(previousEnd)) {
                violations.add("同日活动时间重叠：" + item.place());
            }
            if (previousEnd != null
                    && (blank(item.transport()) || item.transitMinutes() == null)) {
                violations.add("相邻活动必须说明接驳方式和预留时间：" + item.place());
            }
            if (item.transitMinutes() != null && item.transitMinutes() < 0) {
                violations.add("接驳时间不得为负数：" + item.place());
            }
            if (previousEnd != null
                    && item.transitMinutes() != null
                    && item.transitMinutes() >= 0
                    && Duration.between(previousEnd, start).toMinutes() < item.transitMinutes()) {
                violations.add("相邻活动间隔不足以覆盖接驳时间：" + item.place());
            }
            // 2. 未核实时效性事实必须带复核提示，不能伪装为实时结论。
            if (!item.verified() && blank(item.verificationNote())) {
                violations.add("未核实活动必须说明出行前如何复核：" + item.place());
            }
            if (item.verified() && blank(item.sourceId())) {
                violations.add("标记为已核实的活动必须关联来源：" + item.place());
            }
            return end;
        } catch (DateTimeParseException | NullPointerException exception) {
            violations.add("活动时间必须使用HH:mm格式：" + item.place());
            return previousEnd;
        }
    }

    private void validateDayCount(
            TravelIntentDO travelIntentDO,
            TravelPlanDraftDO travelPlanDraftDO,
            List<String> violations) {
        // 1. 优先使用明确日期计算期望天数，否则使用用户指定天数。
        Integer expected = travelIntentDO.days();
        try {
            if (!blank(travelIntentDO.startDate()) && !blank(travelIntentDO.endDate())) {
                expected = Math.toIntExact(
                        ChronoUnit.DAYS.between(
                                        LocalDate.parse(travelIntentDO.startDate()),
                                        LocalDate.parse(travelIntentDO.endDate()))
                                + 1);
            }
        } catch (DateTimeParseException | ArithmeticException exception) {
            violations.add("无法根据日期核对计划天数。");
        }
        // 2. 有可靠期望天数时要求草稿完整覆盖。
        if (expected != null && expected > 0 && travelPlanDraftDO.days().size() != expected) {
            violations.add("计划天数与用户需求不一致。");
        }
    }

    private void validateDateCoverage(
            TravelIntentDO travelIntentDO,
            Set<LocalDate> actualDates,
            List<String> violations) {
        // 1. 只有起止日期同时存在时才能验证逐日计划是否连续完整。
        if (blank(travelIntentDO.startDate()) || blank(travelIntentDO.endDate())) {
            return;
        }
        // 2. 解析合法范围并核对草稿日期集合，格式错误保留为可修订违规。
        try {
            // 1. 解析用户指定的起止日期，倒序范围已由请求校验负责提示。
            LocalDate start = LocalDate.parse(travelIntentDO.startDate());
            LocalDate end = LocalDate.parse(travelIntentDO.endDate());
            if (end.isBefore(start)) {
                return;
            }
            // 2. 逐日构造期望日期集合，拒绝数量相同但日期错位或跳日的草稿。
            Set<LocalDate> expectedDates = new HashSet<>();
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                expectedDates.add(date);
            }
            if (!actualDates.equals(expectedDates)) {
                violations.add("逐日行程必须连续覆盖用户指定的起止日期。");
            }
        } catch (DateTimeParseException exception) {
            // 请求校验已经记录日期格式问题，草稿校验只保留一条可修订提示。
            violations.add("无法根据用户日期核对逐日行程范围。");
        }
    }

    private void validateCosts(
            TravelPlanDraftDO travelPlanDraftDO, List<String> violations) {
        // 1. 汇总模型明确提供的活动费用，负数费用一律视为非法草稿。
        BigDecimal knownTotal = BigDecimal.ZERO;
        boolean hasUnverifiedItem = false;
        for (TravelPlanDayDO day : travelPlanDraftDO.days()) {
            if (day == null) {
                continue;
            }
            for (TravelPlanItemDO item : day.items()) {
                if (item == null) {
                    continue;
                }
                if (item.estimatedCost() != null) {
                    if (item.estimatedCost().signum() < 0) {
                        violations.add("活动费用不得为负数：" + item.place());
                    } else {
                        knownTotal = knownTotal.add(item.estimatedCost());
                    }
                }
                hasUnverifiedItem = hasUnverifiedItem || !item.verified();
            }
        }
        // 2. 总额不能小于已知明细，也不能使用负数掩盖预算超支。
        if (travelPlanDraftDO.estimatedTotal() != null) {
            if (travelPlanDraftDO.estimatedTotal().signum() < 0) {
                violations.add("计划总费用不得为负数。");
            } else if (travelPlanDraftDO.estimatedTotal().compareTo(knownTotal) < 0) {
                violations.add("计划总费用不得小于已知活动费用合计。");
            }
        }
        // 3. 有未核实活动时必须在计划级清单中集中提示，渲染后用户才能统一复核。
        if (hasUnverifiedItem && travelPlanDraftDO.unverifiedItems().isEmpty()) {
            violations.add("计划存在未核实活动时必须提供出行前待核实清单。");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
