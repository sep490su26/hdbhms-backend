package com.sep490.hdbhms.maintenance.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.Room;

public interface GetRoomFromLeaseContractPort {
    Room execute(Long tenantId);
}
