package com.hellotravel.start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hellotravel.util.file.FileStorageException;
import com.hellotravel.util.file.FileStorageUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 本地文件工具的受控路径、内容提取和幂等清理回归。
 *
 * @author AIGenerator
 */
class FileStorageUtilTest {

    @TempDir
    Path directory;

    @Test
    void storesScansAndDeletesBoundedTextFiles() throws Exception {
        // 1. 使用临时受控目录保存 UTF-8 文本，避免测试写入真实资料目录。
        FileStorageUtil files = new FileStorageUtil(directory.toString());
        var stored = files.store("guide.txt", "景德镇旅行资料".getBytes(StandardCharsets.UTF_8));

        // 2. 保存结果包含稳定文件键、摘要和可供业务层持久化的提取正文。
        assertTrue(stored.storageKey().matches("[0-9A-HJKMNP-TV-Z]{26}\\.bin"));
        assertEquals("text/plain", stored.mime());
        assertEquals("景德镇旅行资料", stored.text());
        assertTrue(Files.isRegularFile(directory.resolve(stored.storageKey())));

        // 3. 扫描和重复删除仅作用于受控文件键，缺失文件仍保持幂等成功。
        assertEquals(stored.storageKey(), files.scan("").get(0).storageKey());
        files.delete(stored.storageKey());
        files.delete(stored.storageKey());
        assertFalse(Files.exists(directory.resolve(stored.storageKey())));
    }

    @Test
    void rejectsUnsupportedOrUnsafeFileInput() {
        // 1. 创建受控本地工具并分别传入未支持格式和越界存储键。
        FileStorageUtil files = new FileStorageUtil(directory.toString());

        // 2. 工具仅返回稳定技术失败，调用层可据此映射自己的错误协议。
        assertEquals(
                FileStorageException.Reason.INVALID,
                assertThrows(
                                FileStorageException.class,
                                () -> files.store("guide.exe", new byte[] {1}))
                        .reason());
        assertEquals(
                FileStorageException.Reason.INVALID,
                assertThrows(FileStorageException.class, () -> files.delete("../unsafe")).reason());
    }
}
