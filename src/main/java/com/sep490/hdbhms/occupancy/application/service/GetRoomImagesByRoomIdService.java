package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetRoomImagesByRoomIdQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetRoomImagesByRoomIdUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.RoomImageRepository;
import com.sep490.hdbhms.occupancy.domain.model.RoomImage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetRoomImagesByRoomIdService implements GetRoomImagesByRoomIdUseCase {
    RoomImageRepository roomImageRepository;

    @Override
    public List<RoomImage> execute(GetRoomImagesByRoomIdQuery query) {
        return roomImageRepository.findAllByRoomId(query.roomId());
    }
}
