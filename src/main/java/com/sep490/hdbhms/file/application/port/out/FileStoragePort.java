package com.sep490.hdbhms.file.application.port.out;

import java.io.IOException;
import java.io.InputStream;

/**
 * Stores file bytes independently from the metadata repository.
 */
public interface FileStoragePort {

    String put(String storageKey, InputStream content, long contentLength, String contentType) throws IOException;

    byte[] get(String storageKey) throws IOException;

    void delete(String storageKey) throws IOException;
}
