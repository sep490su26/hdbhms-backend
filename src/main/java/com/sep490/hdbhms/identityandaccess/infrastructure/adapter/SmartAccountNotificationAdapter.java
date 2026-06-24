package com.sep490.hdbhms.identityandaccess.infrastructure.adapter;

import com.sep490.hdbhms.identityandaccess.application.port.out.SendPreCreatedAccountPort;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SmartAccountNotificationAdapter implements SendPreCreatedAccountPort {
    
    // Pattern for validating email addresses
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    SendEmailPreCreatedAccountAdapter emailAdapter;
    SendSmsPreCreatedAccountAdapter smsAdapter;

    @Override
    public void sendAccountInformation(
            String email,
            String fullName,
            String phone,
            String randomPassword
    ) {
        if (isValidEmail(email)) {
            emailAdapter.sendAccountInformation(email, fullName, phone, randomPassword);
        } else {
            smsAdapter.sendAccountInformation(email, fullName, phone, randomPassword);
        }
    }

    @Override
    public void sendAccountInformationBatch(
            String email,
            String recipientFullName,
            String phone,
            List<AccountCredential> credentials
    ) {
        log.info(email);
        if (isValidEmail(email)) {
            emailAdapter.sendAccountInformationBatch(email, recipientFullName, phone, credentials);
        } else {
            smsAdapter.sendAccountInformationBatch(email, recipientFullName, phone, credentials);
        }
    }

    /**
     * Validates if the email is not null, not empty, and matches email pattern.
     * Also rejects synthetic tenant emails (e.g., tenant.hdbhms.local domain).
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String trimmedEmail = email.trim();

        // Reject synthetic tenant emails (internal placeholder emails)
        if (trimmedEmail.toLowerCase().endsWith("@tenant.hdbhms.local") ||
            trimmedEmail.toLowerCase().endsWith("tenant.hdbhms.local")) {
            return false;
        }

        // Check if matches email pattern
        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            return false;
        }

        return true;
    }
}
