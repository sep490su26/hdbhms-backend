package com.sep490.hdbhms.property.application.port.in.query;

import com.sep490.hdbhms.property.infrastructure.web.dto.response.RoomAssetResponse;

import java.util.List;

public interface RoomAssetQueryUseCase {
    List<RoomAssetResponse> getRoomAssets(Long roomId);
    RoomAssetResponse getRoomAsset(Long roomId, Long assetId);
}
