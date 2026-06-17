package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.RoomTransferRequest;

public interface RoomTransferRepository {
    RoomTransferRequest save(RoomTransferRequest roomTransferRequest);
}
