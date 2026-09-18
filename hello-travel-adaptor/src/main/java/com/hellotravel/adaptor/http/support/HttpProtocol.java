package com.hellotravel.adaptor.http.support;

/**
 * HTTP协议默认值与凭据名称；不承载领域状态规则。
 *
 * @author AIGenerator
 */
public final class HttpProtocol {
    /**
     * 协议默认页大小。
     *
     * @author AIGenerator
     */
    public static final int DEFAULT_PAGE_SIZE = 100;

    /**
     * 分页初始游标。
     *
     * @author AIGenerator
     */
    public static final long INITIAL_CURSOR = 0;

    /**
     * 单次附件上传字节上限。
     *
     * @author AIGenerator
     */
    public static final long MAX_UPLOAD_BYTES = 10L * 1024 * 1024;

    /**
     * 页面会话标识请求头。
     *
     * @author AIGenerator
     */
    public static final String SESSION_HEADER = "X-Session-ID";

    /**
     * 刷新请求证明请求头。
     *
     * @author AIGenerator
     */
    public static final String CSRF_HEADER = "X-CSRF-Token";

    /**
     * 原始刷新凭据专用Cookie。
     *
     * @author AIGenerator
     */
    public static final String REFRESH_COOKIE = "ht_refresh";

    /**
     * 签名设备身份专用Cookie。
     *
     * @author AIGenerator
     */
    public static final String DEVICE_COOKIE = "ht_device";

    /**
     * 刷新Cookie有效期秒数。
     *
     * @author AIGenerator
     */
    public static final long REFRESH_COOKIE_SECONDS = java.time.Duration.ofDays(7).toSeconds();

    /**
     * 设备Cookie有效期秒数。
     *
     * @author AIGenerator
     */
    public static final long DEVICE_COOKIE_SECONDS = java.time.Duration.ofDays(365).toSeconds();

    /**
     * 禁止实例化无状态转换器。
     *
     * @author AIGenerator
     */
    private HttpProtocol() {
    }

    /**
     * 将可空分页游标转为协议默认值。
     *
     * @param value 本次转换的value快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public static long cursor(Long value) {
        return value == null ? INITIAL_CURSOR : value;
    }

    /**
     * 将可空页大小转为协议默认值。
     *
     * @param value 本次转换的value快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public static int pageSize(Integer value) {
        return value == null ? DEFAULT_PAGE_SIZE : value;
    }
}
