package com.sep490.hdbhms.property.application.port.in.command;

public record AttachPropertyImageCommand(Long propertyId, Long fileId, Integer sortOrder) {
}
