package com.sep490.hdbhms.property.application.port.in.command;

public record AttachRoomImageCommand(Long roomId, Long fileId, Integer sortOrder) {
}
