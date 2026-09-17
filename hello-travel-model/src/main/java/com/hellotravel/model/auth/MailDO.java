package com.hellotravel.model.auth;

/**
 * SMTP投递确认，不代表用户已阅读。
 *
 * @param delivered SMTP是否确认接收
 * @author AIGenerator
 */
public record MailDO(boolean delivered) {
}
