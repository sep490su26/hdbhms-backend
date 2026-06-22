package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetRoomImagesByRoomIdQuery;
import com.sep490.hdbhms.occupancy.application.port.out.RoomImageRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.RoomImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetRoomImagesByRoomIdServiceTest {

    @Mock
    RoomRepository roomRepository;

    @Mock
    RoomImageRepository roomImageRepository;

    @InjectMocks
    GetRoomImagesByRoomIdService service;

    @Test
    void returnsOwnRoomImagesWhenTheyExist() {
        Room room = Room.builder()
                .id(101L)
                .roomCode("P101")
                .build();
        RoomImage image = RoomImage.builder()
                .id(1L)
                .roomId(101L)
                .fileId(9001L)
                .sortOrder(0)
                .build();

        when(roomRepository.findById(101L)).thenReturn(Optional.of(room));
        when(roomImageRepository.findAllByRoomId(101L)).thenReturn(List.of(image));

        List<RoomImage> result = service.execute(new GetRoomImagesByRoomIdQuery(101L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isFallback()).isFalse();
        assertThat(result.get(0).getSourceRoomCode()).isNull();
        assertThat(result.get(0).getFileId()).isEqualTo(9001L);
    }

    @Test
    void fallsBackToMappedSampleRoomWhenOwnImagesAreMissing() {
        Room room = Room.builder()
                .id(201L)
                .roomCode("P201")
                .build();
        Room sampleRoom = Room.builder()
                .id(102L)
                .roomCode("P102")
                .build();
        RoomImage sampleImage = RoomImage.builder()
                .id(2L)
                .roomId(102L)
                .fileId(9002L)
                .sortOrder(0)
                .build();

        when(roomRepository.findById(201L)).thenReturn(Optional.of(room));
        when(roomImageRepository.findAllByRoomId(201L)).thenReturn(List.of());
        when(roomRepository.findByRoomCode("P102")).thenReturn(Optional.of(sampleRoom));
        when(roomImageRepository.findAllByRoomId(102L)).thenReturn(List.of(sampleImage));

        List<RoomImage> result = service.execute(new GetRoomImagesByRoomIdQuery(201L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isFallback()).isTrue();
        assertThat(result.get(0).getSourceRoomCode()).isEqualTo("P102");
        assertThat(result.get(0).getFileId()).isEqualTo(9002L);
    }
}
