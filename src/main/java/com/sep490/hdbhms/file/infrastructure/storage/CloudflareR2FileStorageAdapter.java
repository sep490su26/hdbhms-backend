package com.sep490.hdbhms.file.infrastructure.storage;

import com.sep490.hdbhms.file.application.port.out.FileStoragePort;
import com.sep490.hdbhms.file.infrastructure.config.FileProperties;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@ConditionalOnProperty(prefix = "app.file.storage", name = "provider", havingValue = "r2")
public class CloudflareR2FileStorageAdapter implements FileStoragePort {
    S3Client s3Client;
    FileProperties fileProperties;

    @Override
    public String put(String storageKey, InputStream content, long contentLength, String contentType) throws IOException {
        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(bucket())
                .key(storageKey);
        if (contentType != null && !contentType.isBlank()) {
            requestBuilder.contentType(contentType);
        }

        try {
            s3Client.putObject(requestBuilder.build(), RequestBody.fromInputStream(content, contentLength));
            return storageKey;
        } catch (SdkException exception) {
            throw storageException("put", storageKey, exception);
        }
    }

    @Override
    public byte[] get(String storageKey) throws IOException {
        try {
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket())
                    .key(storageKey)
                    .build());
            return response.asByteArray();
        } catch (SdkException exception) {
            throw storageException("get", storageKey, exception);
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket())
                    .key(storageKey)
                    .build());
        } catch (SdkException exception) {
            throw storageException("delete", storageKey, exception);
        }
    }

    private String bucket() throws IOException {
        String bucket = fileProperties.getStorage().getR2().getBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IOException("app.file.storage.r2.bucket must be configured when R2 storage is enabled");
        }
        return bucket;
    }

    private IOException storageException(String operation, String storageKey, SdkException cause) {
        return new IOException("Unable to " + operation + " R2 object: " + storageKey, cause);
    }
}
