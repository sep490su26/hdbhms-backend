package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.RoomAssetRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomAssetResponse;

public interface ManageRoomAssetUseCase {
    RoomAssetResponse createRoomAsset(Long roomId, RoomAssetRequest request);
    RoomAssetResponse updateRoomAsset(Long roomId, Long assetId, RoomAssetRequest request);
    void deleteRoomAsset(Long roomId, Long assetId);
}
