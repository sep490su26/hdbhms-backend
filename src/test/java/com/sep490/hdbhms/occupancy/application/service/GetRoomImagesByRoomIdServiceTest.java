package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetRoomImagesByRoomIdQuery;
import com.sep490.hdbhms.occupancy.application.port.out.RoomImageRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.RoomImage;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetRoomImagesByRoomIdServiceTest {

    // ---------- fake RoomRepository ----------
    private static final class StubRoomRepository implements RoomRepository {
        private final Map<Long, Room> byId = new HashMap<>();
        private final Map<String, Room> byCode = new HashMap<>();

        void addRoom(Room room) {
            byId.put(room.getId(), room);
            byCode.put(room.getRoomCode(), room);
        }

        @Override
        public Room save(Room room) { return room; }

        @Override
        public Optional<Room> findById(Long id) { return Optional.ofNullable(byId.get(id)); }

        @Override
        public List<Room> findAllByPropertyIdAndFloorId(Long propertyId, Long floorId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Page<Room> findAll(List<Long> ids, RoomStatus status, Long minPrice, Long maxPrice, Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Room> findByRoomCode(String roomCode) {
            return Optional.ofNullable(byCode.get(roomCode));
        }

        @Override
        public boolean existsActiveByPropertyIdAndRoomCode(Long propertyId, String roomCode) {
            return byCode.containsKey(roomCode);
        }

        @Override
        public int updateRoomStatusIfCurrent(Long roomId, RoomStatus expectedStatus, RoomStatus newStatus) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Room> findAll() { throw new UnsupportedOperationException(); }
    }

    // ---------- fake RoomImageRepository ----------
    private static final class StubRoomImageRepository implements RoomImageRepository {
        private final Map<Long, List<RoomImage>> byRoomId = new HashMap<>();

        void putImages(Long roomId, List<RoomImage> images) {
            byRoomId.put(roomId, images);
        }

        @Override
        public RoomImage save(RoomImage roomImage) { return roomImage; }

        @Override
        public Optional<RoomImage> findById(Long id) { throw new UnsupportedOperationException(); }

        @Override
        public List<RoomImage> findAllByRoomId(Long roomId) {
            return byRoomId.getOrDefault(roomId, List.of());
        }
    }

    // ---------- fake ResourcePatternResolver (no static samples) ----------
    private static final class EmptySampleResourceResolver implements ResourcePatternResolver {
        @Override
        public Resource[] getResources(String locationPattern) throws IOException {
            return new Resource[0];
        }

        @Override
        public Resource getResource(String location) { return null; }

        @Override
        public ClassLoader getClassLoader() { return null; }
    }

    // ---------- tests ----------

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

        StubRoomRepository roomRepo = new StubRoomRepository();
        roomRepo.addRoom(room);

        StubRoomImageRepository imageRepo = new StubRoomImageRepository();
        imageRepo.putImages(101L, List.of(image));

        GetRoomImagesByRoomIdService service =
                new GetRoomImagesByRoomIdService(roomRepo, imageRepo, new EmptySampleResourceResolver());

        List<RoomImage> result = service.execute(new GetRoomImagesByRoomIdQuery(101L));

        assertEquals(1, result.size());
        assertFalse(result.get(0).isFallback());
        assertNull(result.get(0).getSourceRoomCode());
        assertEquals(9001L, result.get(0).getFileId());
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

        StubRoomRepository roomRepo = new StubRoomRepository();
        roomRepo.addRoom(room);
        roomRepo.addRoom(sampleRoom);

        StubRoomImageRepository imageRepo = new StubRoomImageRepository();
        imageRepo.putImages(201L, List.of());
        imageRepo.putImages(102L, List.of(sampleImage));

        GetRoomImagesByRoomIdService service =
                new GetRoomImagesByRoomIdService(roomRepo, imageRepo, new EmptySampleResourceResolver());

        List<RoomImage> result = service.execute(new GetRoomImagesByRoomIdQuery(201L));

        assertEquals(1, result.size());
        assertTrue(result.get(0).isFallback());
        assertEquals("P102", result.get(0).getSourceRoomCode());
        assertEquals(9002L, result.get(0).getFileId());
    }
}
