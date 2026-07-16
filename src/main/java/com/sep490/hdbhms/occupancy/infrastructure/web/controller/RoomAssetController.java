package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.occupancy.application.port.in.query.RoomAssetQueryUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.ManageRoomAssetUseCase;
import com.sep490.hdbhms.occupancy.domain.value_objects.PropertyStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.RoomAssetRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomAssetResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms/{roomId}/assets")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomAssetController {
    RoomAssetQueryUseCase roomAssetQueryUseCase;
    ManageRoomAssetUseCase manageRoomAssetUseCase;
    JpaRoomRepository roomRepository;

    @GetMapping
    public ApiResponse<List<RoomAssetResponse>> getRoomAssets(
            @PathVariable Long roomId
    ) {
        assertPubliclyVisible(roomId);
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
        assertPubliclyVisible(roomId);
        return ApiResponse.<RoomAssetResponse>builder()
                .code(0)
                .data(roomAssetQueryUseCase.getRoomAsset(roomId, assetId))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
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
    @PreAuthorize("hasRole('OWNER')")
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
    @PreAuthorize("hasRole('OWNER')")
    public void deleteRoomAsset(
            @PathVariable Long roomId,
            @PathVariable Long assetId
    ) {
        manageRoomAssetUseCase.deleteRoomAsset(roomId, assetId);
    }

    private void assertPubliclyVisible(Long roomId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        Role role = authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
                ? principal.getRole()
                : null;
        if (role == Role.OWNER || role == Role.MANAGER) {
            return;
        }
        var room = roomRepository.findById(roomId)
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        if (room.getProperty().getStatus() != PropertyStatus.ACTIVE
                || !RoomCatalogController.isPublicRoomStatus(room.getCurrentStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
    }
}
