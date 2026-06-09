package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.query.RoomAssetQueryUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.ManageRoomAssetUseCase;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.RoomAssetRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomAssetResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms/{roomId}/assets")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomAssetController {
    RoomAssetQueryUseCase roomAssetQueryUseCase;
    ManageRoomAssetUseCase manageRoomAssetUseCase;

    @GetMapping
    public ApiResponse<List<RoomAssetResponse>> getRoomAssets(
            @PathVariable Long roomId
    ) {
        return ApiResponse.<List<RoomAssetResponse>>builder()
                .code(0)
                .data(roomAssetQueryUseCase.getRoomAssets(roomId))
                .build();
    }

    @GetMapping("/{assetId}")
    public ApiResponse<RoomAssetResponse> getRoomAsset(
            @PathVariable Long roomId,
            @PathVariable Long assetId
    ) {
        return ApiResponse.<RoomAssetResponse>builder()
                .code(0)
                .data(roomAssetQueryUseCase.getRoomAsset(roomId, assetId))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RoomAssetResponse> createRoomAsset(
            @PathVariable Long roomId,
            @Valid @RequestBody RoomAssetRequest request
    ) {
        return ApiResponse.<RoomAssetResponse>builder()
                .code(0)
                .data(manageRoomAssetUseCase.createRoomAsset(roomId, request))
                .build();
    }

    @PutMapping("/{assetId}")
    public ApiResponse<RoomAssetResponse> updateRoomAsset(
            @PathVariable Long roomId,
            @PathVariable Long assetId,
            @Valid @RequestBody RoomAssetRequest request
    ) {
        return ApiResponse.<RoomAssetResponse>builder()
                .code(0)
                .data(manageRoomAssetUseCase.updateRoomAsset(roomId, assetId, request))
                .build();
    }

    @DeleteMapping("/{assetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoomAsset(
            @PathVariable Long roomId,
            @PathVariable Long assetId
    ) {
        manageRoomAssetUseCase.deleteRoomAsset(roomId, assetId);
    }
}
