package com.sep490.hdbhms.common.email;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class ConsoleEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailService.class);

    private final Environment environment;

    public ConsoleEmailService(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void sendTemporaryAccount(String to, String loginId, String temporaryPassword) {
        boolean devProfile = Arrays.asList(environment.getActiveProfiles()).contains("dev");
        if (devProfile) {
            log.info("""
                    DEV EMAIL - Temporary tenant account
                    To: {}
                    Login ID: {}
                    Temporary password: {}
                    Guide: Open the HDBHMS mobile app and sign in, then change password.
                    TODO: Configure SMTP provider for non-dev delivery.
                    """, to, loginId, temporaryPassword);
            return;
        }

        log.info("Temporary tenant account email queued for {} with login ID {}. Password is not logged.", to, loginId);
    }
}
