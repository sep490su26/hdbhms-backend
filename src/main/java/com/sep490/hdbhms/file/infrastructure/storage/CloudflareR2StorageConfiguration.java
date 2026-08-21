package com.sep490.hdbhms.file.infrastructure.storage;

import com.sep490.hdbhms.file.infrastructure.config.FileProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
@ConditionalOnProperty(prefix = "app.file.storage", name = "provider", havingValue = "r2")
public class CloudflareR2StorageConfiguration {

    @Bean(destroyMethod = "close")
    public S3Client cloudflareR2S3Client(FileProperties fileProperties) {
        FileProperties.R2 r2 = fileProperties.getStorage().getR2();
        requireConfigured(r2.getEndpoint(), "app.file.storage.r2.endpoint");
        requireConfigured(r2.getAccessKeyId(), "app.file.storage.r2.access-key-id");
        requireConfigured(r2.getSecretAccessKey(), "app.file.storage.r2.secret-access-key");

        return S3Client.builder()
                .endpointOverride(URI.create(r2.getEndpoint()))
                .region(Region.of(r2.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(r2.getAccessKeyId(), r2.getSecretAccessKey())
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(r2.isPathStyleAccess())
                        .build())
                .build();
    }

    private static void requireConfigured(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must be configured when R2 storage is enabled");
        }
    }
}
