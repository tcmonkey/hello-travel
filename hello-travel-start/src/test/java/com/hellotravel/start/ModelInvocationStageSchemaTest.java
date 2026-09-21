package com.hellotravel.start;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hellotravel.model.chat.ChatModelStage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/** 模型调用阶段与既有数据库升级迁移的约束一致性回归。 */
class ModelInvocationStageSchemaTest {

    @Test
    void invocationStageChecksAcceptEveryApplicationModelStage() throws IOException {
        // 1. 已发布基线不可回改，当前枚举契约由新增升级迁移承载。
        String upgrade = migration("V3__align_model_invocation_stages.sql");
        // 2. 枚举是应用层唯一阶段契约，每一项都必须能够通过升级后的数据库约束。
        for (ChatModelStage stage : ChatModelStage.values()) {
            String value = "'" + stage.name() + "'";
            assertTrue(upgrade.contains(value), "V3 missing " + stage.name());
        }
        // 3. 拒绝历史遗留的宽泛阶段名，防止新增迁移再次与当前枚举脱节。
        assertFalse(upgrade.contains("'COMPRESSION'"));
        assertFalse(upgrade.contains("'ANSWER'"));
    }

    private String migration(String name) throws IOException {
        // 1. 通过测试资源读取真实迁移文件，不复制SQL片段到断言中。
        try (InputStream input = getClass().getResourceAsStream("/db/migration/" + name)) {
            if (input == null) {
                throw new IOException("missing migration " + name);
            }
            // 2. 使用UTF-8保留SQL原文，供阶段契约逐项比对。
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
