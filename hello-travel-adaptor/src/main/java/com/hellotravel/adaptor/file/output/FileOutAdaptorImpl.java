package com.hellotravel.adaptor.file.output;

import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.application.file.adaptor.FileOutAdaptor;
import com.hellotravel.application.file.command.FileCommand;
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

    public FileOutAdaptorImpl(Environment environment) {
        this.environment = environment;
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
            if (!parsing.tryAcquire()) {
                return Result.failure(AdaptorErrorCode.RATE_LIMITED);
            }
            try {
                Path root =
                        Path.of(environment.getProperty("FILES_DIR", "var/files"))
                                .toAbsolutePath()
                                .normalize();
                Files.createDirectories(root);
                if ("SCAN".equals(fileCommand.action())) {
                    String after = fileCommand.storageKey() == null ? "" : fileCommand.storageKey();
                    java.util.List<java.util.Map<String, Object>> entries =
                            new java.util.ArrayList<>();
                    try (var paths = Files.list(root)) {
                        for (var path :
                                paths.filter(
                                                value ->
                                                        Files.isRegularFile(
                                                                value,
                                                                java.nio.file.LinkOption
                                                                        .NOFOLLOW_LINKS))
                                        .filter(
                                                value ->
                                                        value.getFileName()
                                                                .toString()
                                                                .matches(
                                                                        "[0-9A-HJKMNP-TV-Z]{26}\\.bin"))
                                        .filter(
                                                value ->
                                                        value.getFileName()
                                                                        .toString()
                                                                        .compareTo(after)
                                                                > 0)
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
                    return Result.success(
                            new FileDO(
                                    null,
                                    null,
                                    null,
                                    com.hellotravel.application.support.Json.encode(entries)));
                }
                if ("DELETE".equals(fileCommand.action())) {
                    if (fileCommand.storageKey() == null
                            || !fileCommand.storageKey().matches("[0-9A-HJKMNP-TV-Z]{26}\\.bin")) {
                        return Result.failure(AdaptorErrorCode.INVALID);
                    }
                    Files.deleteIfExists(root.resolve(fileCommand.storageKey()));
                    return Result.success(new FileDO(null, null, null, null));
                }
                byte[] bytes = fileCommand.bytes();
                String name = fileCommand.filename();
                if (bytes == null
                        || bytes.length == 0
                        || bytes.length > 10485760
                        || name == null
                        || name.length() > 255) {
                    return Result.failure(AdaptorErrorCode.INVALID);
                }
                String text;
                String mime;
                if (name.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                    if (bytes.length < 5
                            || !new String(bytes, 0, 5, StandardCharsets.US_ASCII)
                                    .equals("%PDF-")) {
                        return Result.failure(AdaptorErrorCode.INVALID);
                    }
                    try (PDDocument document = Loader.loadPDF(bytes)) {
                        if (document.isEncrypted() || document.getNumberOfPages() > 50) {
                            return Result.failure(AdaptorErrorCode.INVALID);
                        }
                        PDFTextStripper stripper = new PDFTextStripper();
                        java.io.Writer bounded =
                                new com.hellotravel.adaptor.file.output.model.BoundedTextWriter();
                        stripper.writeText(document, bounded);
                        text = bounded.toString();
                    }
                    mime = "application/pdf";
                } else {
                    if (!name.toLowerCase(Locale.ROOT).matches(".*\\.(txt|md)")) {
                        return Result.failure(AdaptorErrorCode.INVALID);
                    }
                    text =
                            StandardCharsets.UTF_8
                                    .newDecoder()
                                    .onMalformedInput(CodingErrorAction.REPORT)
                                    .decode(ByteBuffer.wrap(bytes))
                                    .toString();
                    mime = "text/plain";
                }
                if (text.isBlank()
                        || text.indexOf('\0') >= 0
                        || text.getBytes(StandardCharsets.UTF_8).length > 2097152) {
                    return Result.failure(AdaptorErrorCode.INVALID);
                }
                String key = Ids.next() + ".bin";
                Path temporary = Files.createTempFile(root, "upload-", ".tmp");
                try {
                    Files.write(temporary, bytes);
                    Files.move(temporary, root.resolve(key), StandardCopyOption.ATOMIC_MOVE);
                } finally {
                    Files.deleteIfExists(temporary);
                }
                return Result.success(
                        new FileDO(
                                key,
                                mime,
                                java.security.MessageDigest.getInstance("SHA-256").digest(bytes),
                                text));
            } catch (Exception exception) {
                return Result.failure(AdaptorErrorCode.INVALID);
            } finally {
                parsing.release();
            }
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.adaptor.exception.AdaptorErrorCode.FAILED);
        }
    }
}
