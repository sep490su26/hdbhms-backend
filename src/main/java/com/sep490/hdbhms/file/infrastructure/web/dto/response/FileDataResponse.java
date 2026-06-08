package com.sep490.hdbhms.file.infrastructure.web.dto.response;

import org.springframework.core.io.Resource;

public record FileDataResponse(String contentType, Resource resource, boolean sensitive, Long ownerUserId) {
}
