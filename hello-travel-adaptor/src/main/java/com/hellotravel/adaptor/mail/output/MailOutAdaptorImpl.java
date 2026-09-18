package com.hellotravel.adaptor.mail.output;

import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.application.mail.adaptor.MailOutAdaptor;
import com.hellotravel.application.mail.command.MailCommand;
import com.hellotravel.common.error.Failures;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.auth.MailDO;

import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/**
 * 真实SMTP验证码发送；缺少配置失败关闭，不提供固定验证码后门。
 *
 * @author AIGenerator
 */
@Component
public final class MailOutAdaptorImpl implements MailOutAdaptor {

    private final Environment environment;

    public MailOutAdaptorImpl(Environment environment) {
        this.environment = environment;
    }

    /**
     * 发送真实验证码邮件，未知结果不自动重复发送。
     *
     * @author AIGenerator
     * @param mailCommand 当前用例命令，归属来自服务端
     * @return 当前操作的业务结果
     */
    public Result<MailDO> deliver(MailCommand mailCommand) {
        try {
            // 1. SMTP配置不完整时明确返回未配置结果，避免伪报投递成功。
            if (environment.getProperty("SMTP_HOST") == null
                    || environment.getProperty("SMTP_FROM") == null) {
                return Result.failure(AdaptorErrorCode.UNAVAILABLE);
            }
            // 2. 在异常捕获或资源释放边界内完成本段处理，失败不得伪装为成功。
            try {
                // 1. 取得配置的邮件发件地址，供本段后续处理使用。
                var sender = new JavaMailSenderImpl();
                // 2. 映射本段快照字段，业务状态规则不放入PO赋值。
                sender.setHost(environment.getRequiredProperty("SMTP_HOST"));
                sender.setPort(environment.getProperty("SMTP_PORT", Integer.class, 587));
                sender.setUsername(environment.getProperty("SMTP_USERNAME"));
                sender.setPassword(environment.getProperty("SMTP_PASSWORD"));
                // 3. 取得本服务的运行配置，供本段后续处理使用。
                var properties = sender.getJavaMailProperties();
                // 4. 映射本段快照字段，业务状态规则不放入PO赋值。
                properties.setProperty(
                        "mail.smtp.auth", Boolean.toString(sender.getUsername() != null));
                properties.setProperty(
                        "mail.smtp.starttls.enable",
                        environment.getProperty("SMTP_STARTTLS", "true"));
                properties.setProperty(
                        "mail.smtp.starttls.required",
                        environment.getProperty("SMTP_STARTTLS", "true"));
                properties.setProperty("mail.smtp.connectiontimeout", "5000");
                properties.setProperty("mail.smtp.timeout", "10000");
                properties.setProperty("mail.smtp.writetimeout", "10000");
                properties.setProperty("mail.debug", "false");
                // 5. 取得本轮消息快照，供本段后续处理使用。
                SimpleMailMessage message = new SimpleMailMessage();
                // 6. 映射本段快照字段，业务状态规则不放入PO赋值。
                message.setFrom(environment.getRequiredProperty("SMTP_FROM"));
                message.setTo(mailCommand.email());
                message.setSubject("Hello Travel 邮箱验证码");
                message.setText(
                        "您的验证码为："
                                + mailCommand.code()
                                + "，有效期5分钟。请勿向他人提供。如非本人操作，请忽略。\n请求编号："
                                + mailCommand.challengeId());
                // 7. 执行send职责步骤，并把失败交给所属事务或入口处理。
                sender.send(message);
                // 8. 将本层成功数据封装为标准结果，保持对外模型隔离。
                return Result.success(new MailDO(true));
            } catch (Exception exception) {
                return Result.failure(AdaptorErrorCode.UNAVAILABLE);
            }
        } catch (Exception exception) {
            return Failures.capture(exception, AdaptorErrorCode.FAILED);
        }
    }
}
