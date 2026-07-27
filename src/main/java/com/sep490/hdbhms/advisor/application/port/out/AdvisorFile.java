package com.sep490.hdbhms.advisor.application.port.out;

public record AdvisorFile(byte[] body, String contentType, String contentDisposition) {
}
