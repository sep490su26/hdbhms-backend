package com.sep490.hdbhms.file.domain.model;

import com.sep490.hdbhms.file.domain.valueObjects.FileCategory;
import com.sep490.hdbhms.shared.utils.StringUtils;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileMetadata {
    final Long id;
    final Long ownerUserId;
    String storageKey;
    String originalName;
    final String mimeType;
    final Long sizeBytes;
    final String sha256Checksum;
    FileCategory category;
    boolean isSensitive;

    final LocalDateTime createdAt;
    final LocalDateTime deletedAt;


    public static FileMetadata of(
            Long ownerUserId,
            String originalName,
            String contentType,
            Long sizeBytes,
            String sha256Checksum,
            FileCategory category,
            boolean isSensitive
    ) {
        return FileMetadata.builder()
                .ownerUserId(ownerUserId)
                .originalName(originalName)
                .mimeType(contentType)
                .sizeBytes(sizeBytes)
                .sha256Checksum(sha256Checksum)
                .category(category)
                .isSensitive(isSensitive)
                .build();
    }

    public void setStorageKey(String storageKey) {
        if (StringUtils.isEmpty(storageKey)) {
            throw new IllegalArgumentException("path is empty");
        }
        this.storageKey = storageKey;
    }
}
