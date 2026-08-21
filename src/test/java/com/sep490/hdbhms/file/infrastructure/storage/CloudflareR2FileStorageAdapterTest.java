package com.sep490.hdbhms.file.infrastructure.storage;

import com.sep490.hdbhms.file.infrastructure.config.FileProperties;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloudflareR2FileStorageAdapterTest {

    @Test
    void putsObjectInConfiguredBucket() throws Exception {
        S3Client s3Client = mock(S3Client.class);
        when(s3Client.putObject((PutObjectRequest) any(), (RequestBody) any()))
                .thenReturn(PutObjectResponse.builder().build());

        FileProperties properties = properties();
        CloudflareR2FileStorageAdapter adapter = new CloudflareR2FileStorageAdapter(s3Client, properties);

        String storageKey = adapter.put(
                "files/file-id.png",
                new ByteArrayInputStream(new byte[]{1, 2, 3}),
                3,
                "image/png"
        );

        assertEquals("files/file-id.png", storageKey);
        var requestCaptor = forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertEquals("hdbhms-files", requestCaptor.getValue().bucket());
        assertEquals("files/file-id.png", requestCaptor.getValue().key());
        assertEquals("image/png", requestCaptor.getValue().contentType());
    }

    @Test
    void getsObjectBytesFromConfiguredBucket() throws Exception {
        S3Client s3Client = mock(S3Client.class);
        byte[] expected = new byte[]{4, 5, 6};
        when(s3Client.getObjectAsBytes((GetObjectRequest) any()))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), expected));

        FileProperties properties = properties();
        CloudflareR2FileStorageAdapter adapter = new CloudflareR2FileStorageAdapter(s3Client, properties);

        assertArrayEquals(expected, adapter.get("files/file-id.png"));
        var requestCaptor = forClass(GetObjectRequest.class);
        verify(s3Client).getObjectAsBytes(requestCaptor.capture());
        assertEquals("hdbhms-files", requestCaptor.getValue().bucket());
        assertEquals("files/file-id.png", requestCaptor.getValue().key());
    }

    private FileProperties properties() {
        FileProperties properties = new FileProperties();
        properties.getStorage().getR2().setBucket("hdbhms-files");
        return properties;
    }
}
