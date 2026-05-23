package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetRoomByCodeUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.RoomImageRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetRoomByCodeService implements GetRoomByCodeUseCase {
    RoomRepository roomRepository;

    @Override
    public Room getRoomByCode(String roomCode) {
//        return roomRepository.findByRoomCode(roomCode)
//                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        return null;
    }
}