package com.hellotravel.application.auth.support;

import com.hellotravel.domain.auth.repository.DeviceRepository;
import com.hellotravel.domain.auth.repository.EmailChallengeRepository;
import com.hellotravel.domain.auth.repository.LoginSessionRepository;
import com.hellotravel.domain.auth.repository.RefreshReceiptRepository;
import com.hellotravel.domain.auth.repository.UserAccountRepository;

import org.springframework.stereotype.Component;

/**
 * 认证应用读取所需的本域仓储端口，不聚合其他业务仓储。
 *
 * @author AIGenerator
 */
@Component
public final class AuthRepositories {

    public final UserAccountRepository userAccount;
    public final DeviceRepository device;
    public final LoginSessionRepository loginSession;
    public final EmailChallengeRepository emailChallenge;
    public final RefreshReceiptRepository refreshReceipt;

    public AuthRepositories(
            UserAccountRepository userAccount,
            DeviceRepository device,
            LoginSessionRepository loginSession,
            EmailChallengeRepository emailChallenge,
            RefreshReceiptRepository refreshReceipt) {
        this.userAccount = userAccount;
        this.device = device;
        this.loginSession = loginSession;
        this.emailChallenge = emailChallenge;
        this.refreshReceipt = refreshReceipt;
    }
}
