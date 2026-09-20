package com.hellotravel.application.chat.travel;

import com.hellotravel.application.chat.support.TravelConversationContext;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 将LangGraph4j可序列化状态键映射到一次执行的框架无关业务上下文。
 *
 * @author AIGenerator
 */
@Component
public final class TravelPlanningContextRegistry {

    /**
     * 当前进程内仍在执行的规划上下文。
     *
     * @author AIGenerator
     */
    private final ConcurrentMap<String, TravelConversationContext> contexts =
            new ConcurrentHashMap<>();

    /**
     * 注册一次同步规划执行上下文。
     *
     * @param context 框架无关业务上下文
     * @return 可安全放入图状态的执行键
     * @author AIGenerator
     */
    public String register(TravelConversationContext context) {
        // 1. 以任务公开标识和随机后缀隔离并发或重放执行。
        String key = context.run().publicId() + ":" + UUID.randomUUID();
        // 2. 原子登记上下文，极小概率冲突也不得覆盖正在执行的实例。
        if (contexts.putIfAbsent(key, context) != null) {
            throw new IllegalStateException("travel planning context collision");
        }
        return key;
    }

    /**
     * 读取当前规划执行的业务上下文。
     *
     * @param key 图状态中的执行键
     * @return 框架无关业务上下文
     * @author AIGenerator
     */
    public TravelConversationContext require(String key) {
        // 1. 根据图状态中的执行键读取当前进程内业务上下文。
        TravelConversationContext context = contexts.get(key);
        // 2. 执行已结束或键无效时立即拒绝，节点不能继续使用无归属状态。
        if (context == null) {
            throw new IllegalStateException("travel planning context missing");
        }
        return context;
    }

    /**
     * 在图执行终止后清理业务上下文引用。
     *
     * @param key 图状态中的执行键
     * @author AIGenerator
     */
    public void remove(String key) {
        contexts.remove(key);
    }
}
