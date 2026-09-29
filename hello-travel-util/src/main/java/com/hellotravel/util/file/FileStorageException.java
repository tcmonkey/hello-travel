package com.hellotravel.util.file;

/**
 * 本地文件工具的受控失败，不向调用方暴露底层文件系统细节。
 *
 * @author AIGenerator
 */
public final class FileStorageException extends RuntimeException {

    /**
     * 失败的稳定技术分类，供上层转换为本层错误语义。
     *
     * @author AIGenerator
     */
    public enum Reason {
        /**
     * 参数、格式或受控路径不合法。
     *
     * @author AIGenerator
     */
        INVALID,
        /**
     * 单机解析并发已达到上限。
     *
     * @author AIGenerator
     */
        BUSY,
        /**
     * 文件系统或算法执行未完成。
     *
     * @author AIGenerator
     */
        FAILED
    }

    /**
     * 当前失败的稳定技术分类。
     *
     * @author AIGenerator
     */
    private final Reason reason;

    /**
     * 创建带原始异常的文件工具失败。
     *
     * @param reason 稳定技术分类
     * @param cause 原始技术异常
     * @author AIGenerator
     */
    public FileStorageException(Reason reason, Throwable cause) {
        super(reason.name(), cause);
        this.reason = reason;
    }

    /**
     * 创建无需保留原始异常的文件工具失败。
     *
     * @param reason 稳定技术分类
     * @author AIGenerator
     */
    public FileStorageException(Reason reason) {
        this(reason, null);
    }

    /**
     * 返回当前失败的稳定技术分类。
     *
     * @return 文件工具失败分类
     * @author AIGenerator
     */
    public Reason reason() {
        return reason;
    }
}
