package com.hellotravel.util.file;

import com.hellotravel.common.identity.Ids;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;

/**
 * 供项目内部复用的本地文件存储与受限文本提取工具。
 *
 * <p>它只处理受控目录、文件名、容量和解析上限，不承载知识库等业务动作。</p>
 *
 * @author AIGenerator
 */
@Component
public final class FileStorageUtil {

    /**
     * 文件原始字节的最大允许容量。
     *
     * @author AIGenerator
     */
    private static final int MAX_FILE_BYTES = 10 * 1024 * 1024;
    /**
     * 原始文件名的最大允许长度。
     *
     * @author AIGenerator
     */
    private static final int MAX_FILENAME_LENGTH = 255;
    /**
     * 提取文本的最大 UTF-8 字节数。
     *
     * @author AIGenerator
     */
    private static final int MAX_TEXT_BYTES = 2 * 1024 * 1024;
    /**
     * 单份 PDF 的最大允许页数。
     *
     * @author AIGenerator
     */
    private static final int MAX_PDF_PAGES = 50;
    /**
     * PDF 文本提取器允许写入的最大字符数。
     *
     * @author AIGenerator
     */
    private static final int MAX_PDF_CHARACTERS = 512 * 1024;
    /**
     * 仅接受工具生成的 ULID 文件键。
     *
     * @author AIGenerator
     */
    private static final String STORAGE_KEY_PATTERN = "[0-9A-HJKMNP-TV-Z]{26}\\.bin";

    /**
     * 已规范化的本地文件根目录。
     *
     * @author AIGenerator
     */
    private final Path root;
    /**
     * 单机附件解析并发保护。
     *
     * @author AIGenerator
     */
    private final Semaphore parsing = new Semaphore(2);

    /**
     * 从应用配置建立本地文件工具。
     *
     * @param filesDirectory 本地受控文件根目录
     * @author AIGenerator
     */
    public FileStorageUtil(@Value("${travel.knowledge.files-directory}") String filesDirectory) {
        this.root = Path.of(filesDirectory).toAbsolutePath().normalize();
    }

    /**
     * 校验、解析并原子保存一个本地文件。
     *
     * @param filename 仅用于格式识别和展示的原始文件名
     * @param bytes 已由调用方取得的有界文件内容
     * @return 保存结果和受限提取文本
     * @author AIGenerator
     */
    public StoredFile store(String filename, byte[] bytes) {
        // 1. 限制并发解析量，避免单机被大文件处理任务耗尽内存。
        if (!parsing.tryAcquire()) {
            throw new FileStorageException(FileStorageException.Reason.BUSY);
        }
        // 2. 在同一失败边界内执行校验、解析和原子落盘。
        try {
            // 1. 在落盘前完成文件格式、大小和可读性校验。
            validateUpload(filename, bytes);
            // 2. 提取受限文本并准备受控存储目录与稳定文件键。
            ParsedText parsed = parse(bytes, filename);
            Path directory = ensureRoot();
            String storageKey = Ids.next() + ".bin";
            Path temporary = Files.createTempFile(directory, "upload-", ".tmp");
            // 3. 使用同目录临时文件完成原子写入，避免暴露半成品文件。
            try {
                // 1. 先写入临时文件，保留原始字节内容。
                Files.write(temporary, bytes);
                // 2. 原子替换为稳定文件键，随后由finally清理残留临时文件。
                Files.move(temporary, directory.resolve(storageKey), StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temporary);
            }
            // 4. 返回技术处理结果，业务层自行决定如何持久化和使用它。
            return new StoredFile(storageKey, parsed.mime(), sha256(bytes), parsed.text());
        } catch (FileStorageException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw new FileStorageException(FileStorageException.Reason.INVALID, exception);
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new FileStorageException(FileStorageException.Reason.FAILED, exception);
        } finally {
            parsing.release();
        }
    }

    /**
     * 删除受控存储键对应的本地文件；文件已不存在视为删除完成。
     *
     * @param storageKey 由本工具生成的稳定文件键
     * @author AIGenerator
     */
    public void delete(String storageKey) {
        try {
            // 1. 只允许工具生成的文件键进入路径解析，阻止目录穿越。
            validateStorageKey(storageKey);
            // 2. 删除目标文件，缺失文件按幂等删除处理。
            Files.deleteIfExists(ensureRoot().resolve(storageKey));
        } catch (FileStorageException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw new FileStorageException(FileStorageException.Reason.INVALID, exception);
        } catch (IOException exception) {
            throw new FileStorageException(FileStorageException.Reason.FAILED, exception);
        }
    }

    /**
     * 按受控文件键读取一批本地文件元数据，供后台清理等技术任务使用。
     *
     * @param after 上一次处理的文件键；为空时从头扫描
     * @return 最多一百项的稳定排序结果
     * @author AIGenerator
     */
    public List<StoredFileEntry> scan(String after) {
        try {
            // 1. 校验扫描游标，游标只能是空值或本工具生成的文件键。
            if (after != null && !after.isBlank()) {
                validateStorageKey(after);
            }
            String cursor = after == null ? "" : after;
            // 2. 以稳定文件键排序并限制批量大小，避免后台清理无限占用资源。
            try (var paths = Files.list(ensureRoot())) {
                return paths.filter(path -> Files.isRegularFile(path, java.nio.file.LinkOption.NOFOLLOW_LINKS))
                        .filter(path -> path.getFileName().toString().matches(STORAGE_KEY_PATTERN))
                        .filter(path -> path.getFileName().toString().compareTo(cursor) > 0)
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .limit(100)
                        .map(this::entry)
                        .toList();
            }
        } catch (FileStorageException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw new FileStorageException(FileStorageException.Reason.INVALID, exception);
        } catch (IOException exception) {
            throw new FileStorageException(FileStorageException.Reason.FAILED, exception);
        }
    }

    private Path ensureRoot() throws IOException {
        // 1. 确保配置目录存在，目录创建是幂等操作。
        Files.createDirectories(root);
        // 2. 返回已规范化的固定根目录，调用方不能传入任意路径。
        return root;
    }

    private StoredFileEntry entry(Path path) {
        try {
            return new StoredFileEntry(path.getFileName().toString(), Files.getLastModifiedTime(path).toMillis());
        } catch (IOException exception) {
            throw new FileStorageException(FileStorageException.Reason.FAILED, exception);
        }
    }

    private static void validateUpload(String filename, byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_FILE_BYTES || filename == null
                || filename.isBlank() || filename.length() > MAX_FILENAME_LENGTH) {
            throw new IllegalArgumentException("invalid file");
        }
    }

    private static void validateStorageKey(String storageKey) {
        if (storageKey == null || !storageKey.matches(STORAGE_KEY_PATTERN)) {
            throw new IllegalArgumentException("invalid storage key");
        }
    }

    private static ParsedText parse(byte[] bytes, String filename) throws IOException {
        // 1. 统一规范化扩展名，避免文件格式判断受大小写影响。
        String normalizedName = filename.toLowerCase(Locale.ROOT);
        // 2. PDF 走页数和提取长度均受限的解析路径。
        if (normalizedName.endsWith(".pdf")) {
            return parsePdf(bytes);
        }
        // 3. 文本和 Markdown 按严格 UTF-8 解码，拒绝畸形字符。
        if (normalizedName.endsWith(".txt") || normalizedName.endsWith(".md")) {
            String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            validateText(text);
            return new ParsedText("text/plain", text);
        }
        // 4. 未明确支持的格式不进入存储流程。
        throw new IllegalArgumentException("unsupported file");
    }

    private static ParsedText parsePdf(byte[] bytes) throws IOException {
        // 1. 先核对 PDF 文件头，避免将任意二进制交给解析器。
        if (bytes.length < 5 || !new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) {
            throw new IllegalArgumentException("invalid pdf");
        }
        // 2. 在资源边界内读取 PDF，确保解析器资源总会释放。
        try (PDDocument document = Loader.loadPDF(bytes)) {
            // 1. 拒绝加密或超页数 PDF，限定解析工作量。
            if (document.isEncrypted() || document.getNumberOfPages() > MAX_PDF_PAGES) {
                throw new IllegalArgumentException("invalid pdf");
            }
            // 2. 准备受限写入器，避免文本提取无上限占用内存。
            PDFTextStripper stripper = new PDFTextStripper();
            BoundedTextWriter writer = new BoundedTextWriter();
            // 3. 提取正文并验证文本边界后返回标准 MIME 结果。
            stripper.writeText(document, writer);
            String text = writer.toString();
            validateText(text);
            return new ParsedText("application/pdf", text);
        }
    }

    private static void validateText(String text) {
        if (text.isBlank() || text.indexOf('\0') >= 0
                || text.getBytes(StandardCharsets.UTF_8).length > MAX_TEXT_BYTES) {
            throw new IllegalArgumentException("invalid text");
        }
    }

    private static byte[] sha256(byte[] bytes) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256").digest(bytes);
    }

    /**
     * 保存后可由业务层映射的通用文件结果。
     *
     * @param storageKey 工具生成的稳定文件键
     * @param mime 已识别的受支持文件 MIME 类型
     * @param hash 原始文件字节的 SHA-256 摘要
     * @param text 有界提取的文件正文
     * @author AIGenerator
     */
    public record StoredFile(String storageKey, String mime, byte[] hash, String text) {
    }

    /**
     * 后台扫描使用的通用文件元数据。
     *
     * @param storageKey 工具生成的稳定文件键
     * @param modifiedAt 文件最后修改时间的毫秒时间戳
     * @author AIGenerator
     */
    public record StoredFileEntry(String storageKey, long modifiedAt) {
    }

    /**
     * 防止 PDF 解析过程无限积累文本的内存写入器。
     *
     * @author AIGenerator
     */
    private static final class BoundedTextWriter extends Writer {
        /**
     * 已提取且仍处于字符上限内的正文。
     *
     * @author AIGenerator
     */
        private final StringBuilder content = new StringBuilder();

        @Override
        public void write(char[] value, int offset, int count) throws IOException {
            // 1. 到达字符上限时停止 PDF 提取，避免内存继续增长。
            if (content.length() + count > MAX_PDF_CHARACTERS) {
                throw new IOException("pdf extraction limit");
            }
            // 2. 在确认容量后追加本次文本片段。
            content.append(value, offset, count);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        @Override
        public String toString() {
            return content.toString();
        }
    }

    private record ParsedText(String mime, String text) {
    }
}
