package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.adapter;

import com.sep490.hdbhms.identityandaccess.application.port.out.PasswordResetTokenGenerator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PasswordResetTokenGeneratorAdapter implements PasswordResetTokenGenerator {
    @Override
    public String generate() {
        //TODO: Implement proper logic for password reset token, for now just some random uuid
        return UUID.randomUUID().toString();
    }
}
