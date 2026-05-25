package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.adapter;

import com.sep490.hdbhms.identityandaccess.application.port.out.OtpCodeGenerator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Random;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OtpCodeGeneratorAdapter implements OtpCodeGenerator {
    Random random = new SecureRandom();

    @Override
    public String generate() {
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
