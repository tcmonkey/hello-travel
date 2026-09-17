package com.hellotravel.adaptor.file.output.model;

/**
 * PDF提取在写入过程中限制字符数，防止先无限分配后才判断大小。
 *
 * @author AIGenerator
 */
public final class BoundedTextWriter extends java.io.Writer {

    /**
     * 保存content对应的有界运行状态。
     *
     * @author AIGenerator
     */
    private final StringBuilder content = new StringBuilder();

    /**
     * 在字符上限内追加PDF提取正文。
     *
     * @author AIGenerator
     * @param value 受控业务载荷
     * @param offset 受控offset参数
     * @param count 受控count参数
     */
    public void write(char[] value, int offset, int count) throws java.io.IOException {
        if (content.length() + count > 524288) {
            throw new java.io.IOException("extraction limit");
        }
        content.append(value, offset, count);
    }

    /**
     * 实现内存写入器刷新契约。
     *
     * @author AIGenerator
     */
    public void flush() {
    }

    /**
     * 释放写入契约，内存内容不访问外部资源。
     *
     * @author AIGenerator
     */
    public void close() {
    }

    /**
     * 返回已提取的有界明文。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public String toString() {
        return content.toString();
    }
}
