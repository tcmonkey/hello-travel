package com.hellotravel.start;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 17份实际持久化映射往返检查，防止职责拆分遗漏归属、版本、删除代次或凭据字段。
 *
 * @author AIGenerator
 */
class PersistenceMappingTest {
    @Test
    void everyPersistenceConverterRetainsAllStoredFacts() throws Exception {
        int checked = 0;
        for (var area :
                List.of(
                        List.of(
                                "auth",
                                "UserAccount",
                                "Device",
                                "LoginSession",
                                "EmailChallenge",
                                "RefreshReceipt"),
                        List.of("chat", "Conversation", "Message", "ChatRun", "ModelInvocation"),
                        List.of("knowledge", "KnowledgeDocument", "KnowledgeChunk", "IndexJob"),
                        List.of("memory", "MemorySummary", "MemoryFact", "MemoryFactSource"),
                        List.of("sync", "SyncEvent", "OutboxEvent"))) {
            // 1. 对每个实际领域记录构造带区分值的完整数据库事实快照。
            String subdomain = area.get(0);
            for (String model : area.subList(1, area.size())) {
                Class<?> entityType =
                        Class.forName(
                                "com.hellotravel.domain."
                                        + subdomain
                                        + ".model.entity."
                                        + model
                                        + "Entity");
                RecordComponent[] fields = entityType.getRecordComponents();
                Class<?>[] types = new Class<?>[fields.length];
                Object[] values = new Object[fields.length];
                for (int index = 0; index < fields.length; index++) {
                    types[index] = fields[index].getType();
                    values[index] = sample(types[index], fields[index].getName(), index);
                }
                Object entity = entityType.getConstructor(types).newInstance(values);
                Class<?> aggregateType =
                        Class.forName(
                                "com.hellotravel.domain."
                                        + subdomain
                                        + ".model.aggregate."
                                        + model
                                        + "Aggregate");
                Object aggregate = aggregateType.getConstructor(entityType).newInstance(entity);
                // 2. 使用真实converter完成领域快照→PO→领域快照往返，不依赖数据库。
                Class<?> converterType =
                        Class.forName(
                                "com.hellotravel.infrastructure."
                                        + subdomain
                                        + ".converter."
                                        + model
                                        + "PersistenceConverter");
                Object converter = converterType.getConstructor().newInstance();
                Object po =
                        converterType
                                .getMethod("toPersistence", aggregateType)
                                .invoke(converter, aggregate);
                Object restored =
                        converterType.getMethod("restore", po.getClass()).invoke(converter, po);
                Object restoredEntity = aggregateType.getMethod("entity").invoke(restored);
                // 3. 逐个记录组件对比，数组比较内容，普通字段比较实际值。
                for (RecordComponent field : fields) {
                    Object before = field.getAccessor().invoke(entity);
                    Object after = field.getAccessor().invoke(restoredEntity);
                    if (field.getType() == byte[].class) {
                        assertArrayEquals(
                                (byte[]) before, (byte[]) after, model + "." + field.getName());
                    } else {
                        assertEquals(before, after, model + "." + field.getName());
                    }
                }
                checked++;
            }
        }
        assertEquals(17, checked);
    }

    private Object sample(Class<?> type, String field, int index) {
        // 1. 每个字段使用不同值，避免字段交叉赋值仍被相同示例值掩盖。
        if (type == Long.class || type == long.class) return 100L + index;
        if (type == Integer.class || type == int.class) return 100 + index;
        if (type == String.class) return "stored_" + field;
        if (type == byte[].class) return new byte[] {(byte) index, 1, 2};
        if (type == LocalDateTime.class)
            return LocalDateTime.of(2026, 9, 18, 12, 0).plusSeconds(index);
        // 2. 未覆盖的模型字段类型明确失败，防止默认为空绕过验证。
        throw new IllegalArgumentException("Uncovered record field type " + type.getSimpleName());
    }
}
