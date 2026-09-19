package com.hellotravel.adaptor.knowledge.output.file;

import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.adaptor.knowledge.output.file.converter.FileOutputConverter;
import com.hellotravel.adaptor.knowledge.output.file.model.BoundedTextWriter;
import com.hellotravel.application.knowledge.document.file.adaptor.FileOutAdaptor;
import com.hellotravel.application.knowledge.document.file.command.FileCommand;
import com.hellotravel.common.error.Failures;
import com.hellotravel.common.identity.Ids;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.knowledge.FileDO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * 受控路径和有界TXT/Markdown/PDF解析；不执行附件或访问附件中的链接。
 *
 * @author AIGenerator
 */
@Component
public final class FileOutAdaptorImpl implements FileOutAdaptor {

    private final Environment environment;

    /**
     * 有界附件解析并发，保护单机内存。
     *
     * @author AIGenerator
     */
    private final java.util.concurrent.Semaphore parsing = new java.util.concurrent.Semaphore(2);

    private final FileOutputConverter fileOutputConverter;

    public FileOutAdaptorImpl(Environment environment, FileOutputConverter fileOutputConverter) {
        this.environment = environment;
        this.fileOutputConverter = fileOutputConverter;
    }

    /**
     * 处理有界私有资料并校验受控存储键。
     *
     * @author AIGenerator
     * @param fileCommand 当前用例命令，归属来自服务端
     * @return 当前操作的业务结果
     */
    public Result<FileDO> store(FileCommand fileCommand) {
        try {
            // 1. 解析并发槽已满时立即返回繁忙结果，避免附件占满本机资源。
            if (!parsing.tryAcquire()) {
                return Result.failure(AdaptorErrorCode.RATE_LIMITED);
            }
            // 2. 在异常捕获或资源释放边界内完成本段处理，失败不得伪装为成功。
            try {
                // 1. 取得配置的私有文件根目录，供本段后续处理使用。
                Path root =
                        Path.of(environment.getProperty("FILES_DIR", "var/files"))
                                .toAbsolutePath()
                                .normalize();
                // 2. 执行createDirectories职责步骤，并把失败交给所属事务或入口处理。
                Files.createDirectories(root);
                // 3. 按私有文件扫描场景进入对应职责分支。
                if ("SCAN".equals(fileCommand.action())) {
                    return scan(root, fileCommand);
                }
                // 4. 按删除补偿场景进入对应职责分支。
                if ("DELETE".equals(fileCommand.action())) {
                    if (fileCommand.storageKey() == null
                            || !fileCommand.storageKey().matches("[0-9A-HJKMNP-TV-Z]{26}\\.bin")) {
                        return Result.failure(AdaptorErrorCode.INVALID);
                    }
                    Files.deleteIfExists(root.resolve(fileCommand.storageKey()));
                    return Result.success(fileOutputConverter.deleted());
                }
                // 5. 取得待校验的上传文件字节，供本段后续处理使用。
                byte[] bytes = fileCommand.bytes();
                String name = fileCommand.filename();
                // 6. 拒绝空文件、超过10MB的附件及超长文件名，再进入解析与落盘。
                if (bytes == null
                        || bytes.length == 0
                        || bytes.length > 10485760
                        || name == null
                        || name.length() > 255) {
                    return Result.failure(AdaptorErrorCode.INVALID);
                }
                // 7. 解析受限附件，格式与容量约束由解析步骤核对。
                ParsedFile parsed = parse(bytes, name);
                String text = parsed.text();
                String mime = parsed.mime();
                String key = Ids.next() + ".bin";
                Path temporary = Files.createTempFile(root, "upload-", ".tmp");
                // 8. 在异常捕获或资源释放边界内完成本段处理，失败不得伪装为成功。
                try {
                    // 1. 执行write职责步骤，并把失败交给所属事务或入口处理。
                    Files.write(temporary, bytes);
                    // 2. 执行move职责步骤，并把失败交给所属事务或入口处理。
                    Files.move(temporary, root.resolve(key), StandardCopyOption.ATOMIC_MOVE);
                } finally {
                    Files.deleteIfExists(temporary);
                }
                // 9. 将本层成功数据封装为标准结果，保持对外模型隔离。
                return Result.success(fileOutputConverter.stored(fileCommand, key, mime, text));
            } catch (Exception exception) {
                return Failures.capture(exception, AdaptorErrorCode.INVALID);
            } finally {
                parsing.release();
            }
        } catch (Exception exception) {
            return Failures.capture(exception, AdaptorErrorCode.FAILED);
        }
    }

    private Result<FileDO> scan(Path root, FileCommand fileCommand) throws java.io.IOException {
        // 1. 取得文件扫描的恢复游标，供本段后续处理使用。
        String after = fileCommand.storageKey() == null ? "" : fileCommand.storageKey();
        java.util.List<java.util.Map<String, Object>> entries = new java.util.ArrayList<>();
        // 2. 在异常捕获或资源释放边界内完成本段处理，失败不得伪装为成功。
        try (var paths = Files.list(root)) {
            for (var path :
                    paths.filter(
                                    value ->
                                            Files.isRegularFile(
                                                    value, java.nio.file.LinkOption.NOFOLLOW_LINKS))
                            .filter(
                                    value ->
                                            value.getFileName()
                                                    .toString()
                                                    .matches("[0-9A-HJKMNP-TV-Z]{26}\\.bin"))
                            .filter(value -> value.getFileName().toString().compareTo(after) > 0)
                            .sorted(
                                    java.util.Comparator.comparing(
                                            value -> value.getFileName().toString()))
                            .limit(100)
                            .toList()) {
                entries.add(
                        java.util.Map.of(
                                "key",
                                path.getFileName().toString(),
                                "modified",
                                Files.getLastModifiedTime(path).toMillis()));
            }
        }
        // 3. 将本层成功数据封装为标准结果，保持对外模型隔离。
        return Result.success(fileOutputConverter.scanned(entries));
    }

    private ParsedFile parse(byte[] bytes, String name) throws java.io.IOException {
        // 1. 准备当前操作的正文或受限拼接容器。
        String text;
        String mime;
        // 2. 按PDF或UTF-8文本分别解析，格式处理不混入文件保存职责。
        if (name.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            if (bytes.length < 5
                    || !new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) {
                throw new IllegalArgumentException("invalid attachment");
            }
            try (PDDocument document = Loader.loadPDF(bytes)) {
                // 1. 拒绝加密PDF和超过50页的文档，限制解析工作量。
                if (document.isEncrypted() || document.getNumberOfPages() > 50) {
                    throw new IllegalArgumentException("invalid attachment");
                }
                // 2. 取得受正文容量限制的PDF解析器，供本段后续处理使用。
                PDFTextStripper stripper = new PDFTextStripper();
                java.io.Writer bounded = new BoundedTextWriter();
                // 3. 执行writeText职责步骤，并把失败交给所属事务或入口处理。
                stripper.writeText(document, bounded);
                // 4. 更新本次处理的局部数据或上下文，后续步骤读取同一快照。
                text = bounded.toString();
            }
            mime = "application/pdf";
        } else {
            if (!name.toLowerCase(Locale.ROOT).matches(".*\\.(txt|md)")) {
                throw new IllegalArgumentException("invalid attachment");
            }
            text =
                    StandardCharsets.UTF_8
                            .newDecoder()
                            .onMalformedInput(CodingErrorAction.REPORT)
                            .decode(ByteBuffer.wrap(bytes))
                            .toString();
            mime = "text/plain";
        }
        // 3. 依据格式、长度或数量边界处理分支，避免继续使用无效数据。
        if (text.isBlank()
                || text.indexOf('\0') >= 0
                || text.getBytes(StandardCharsets.UTF_8).length > 2097152) {
            throw new IllegalArgumentException("invalid attachment");
        }
        // 4. 返回本段实际处理结果，保持本层输出契约。
        return new ParsedFile(text, mime);
    }

    private record ParsedFile(String text, String mime) {
    }
}
