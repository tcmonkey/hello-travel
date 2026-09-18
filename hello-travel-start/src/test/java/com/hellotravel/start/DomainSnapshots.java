package com.hellotravel.start;

import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** 领域行为测试共享的完整事实快照，不进入运行源码。 */
final class DomainSnapshots {
    private static final LocalDateTime TIME = LocalDateTime.of(2026, 9, 18, 8, 0);

    @SuppressWarnings("unchecked")
    static <T> T snapshot(Class<T> type, Map<String, Object> replacements) throws Exception {
        RecordComponent[] fields = type.getRecordComponents();
        Object[] values = new Object[fields.length];
        for (int i = 0; i < fields.length; i++) {
            var field = fields[i];
            Object value =
                    field.getType() == Long.class
                            ? 1L
                            : field.getType() == Integer.class
                                    ? 1
                                    : field.getType() == String.class
                                            ? "ACTIVE"
                                            : field.getType() == byte[].class
                                                    ? new byte[32]
                                                    : field.getType() == LocalDateTime.class
                                                            ? TIME
                                                            : null;
            if (field.getName().equals("version")) value = 0L;
            if (List.of("deletedAt", "revokedAt").contains(field.getName())) value = null;
            values[i] = replacements.getOrDefault(field.getName(), value);
        }
        return (T)
                type.getDeclaredConstructor(
                                Arrays.stream(fields)
                                        .map(RecordComponent::getType)
                                        .toArray(Class<?>[]::new))
                        .newInstance(values);
    }

    @SuppressWarnings("unchecked")
    static <T> T copy(T source, Map<String, Object> replacements) throws Exception {
        var type = source.getClass();
        var fields = type.getRecordComponents();
        Object[] values = new Object[fields.length];
        for (int i = 0; i < fields.length; i++)
            values[i] =
                    replacements.getOrDefault(
                            fields[i].getName(), fields[i].getAccessor().invoke(source));
        return (T)
                type.getDeclaredConstructor(
                                Arrays.stream(fields)
                                        .map(RecordComponent::getType)
                                        .toArray(Class<?>[]::new))
                        .newInstance(values);
    }
}
