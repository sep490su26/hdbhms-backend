package com.sep490.hdbhms.billingandpayment.infrastructure.adapter;

import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.PromotionRole;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.RolePromotionStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaRolePromotionRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.notification.application.service.BusinessNotificationPublisher;
import com.sep490.hdbhms.occupancy.application.port.out.DepositAgreementRepository;
import com.sep490.hdbhms.occupancy.application.port.out.PropertyRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomHoldRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.application.service.DepositContractDocumentService;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.RoomHold;
import com.sep490.hdbhms.occupancy.domain.value_objects.PropertyStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomHoldStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.shared.event.NotificationEvent;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DepositCompletionAdapterTest {

    @Test
    void executeMarksDepositPaidAndSchedulesOfficialContractGeneration() {
        DepositAgreement agreement = DepositAgreement.builder()
                .id(7L)
                .depositCode("DC-TEST-007")
                .roomId(101L)
                .roomHoldId(55L)
                .amount(1_000_000L)
                .status(DepositAgreementStatus.PENDING_PAYMENT)
                .build();
        Room room = Room.builder()
                .id(101L)
                .propertyId(9L)
                .roomCode("101")
                .build();
        Property property = Property.builder()
                .id(9L)
                .name("Nha tro Test")
                .build();
        RoomHold roomHold = RoomHold.builder()
                .id(55L)
                .roomId(101L)
                .status(RoomHoldStatus.ACTIVE)
                .build();
        AtomicLong cancelledHoldId = new AtomicLong();
        AtomicReference<DepositAgreement> assignedAgreement = new AtomicReference<>();
        RecordingContractDocumentService contractDocumentService = new RecordingContractDocumentService();
        JpaUserRepository userRepository = mock(JpaUserRepository.class);
        JpaRolePromotionRepository rolePromotionRepository = mock(JpaRolePromotionRepository.class);
        List<NotificationEvent> notifications = new ArrayList<>();
        BusinessNotificationPublisher notificationPublisher = new BusinessNotificationPublisher(notifications::add);

        when(userRepository.findIdsByRolesAndStatus(List.of(Role.OWNER), AccountStatus.ACTIVE))
                .thenReturn(List.of(1L));
        when(rolePromotionRepository.findActiveUserIdsByPropertyId(
                9L,
                PromotionRole.MANAGER,
                RolePromotionStatus.ACTIVE,
                AccountStatus.ACTIVE
        )).thenReturn(List.of(2L, 1L));

        DepositCompletionAdapter adapter = new DepositCompletionAdapter(
                new FakeRoomRepository(room),
                new FakeRoomHoldRepository(roomHold),
                new FakePropertyRepository(property),
                new FakeDepositAgreementRepository(agreement),
                cancelledHoldId::set,
                assignedAgreement::set,
                contractDocumentService,
                userRepository,
                rolePromotionRepository,
                notificationPublisher
        );

        adapter.execute(Invoice.builder().depositAgreementId(7L).build());

        assertEquals(RoomHoldStatus.CONFIRMED, roomHold.getStatus());
        assertEquals(55L, cancelledHoldId.get());
        assertSame(agreement, assignedAgreement.get());
        assertEquals(DepositAgreementStatus.PAID, agreement.getStatus());
        assertEquals(7L, contractDocumentService.depositAgreementId);
        assertEquals(2, notifications.size());
        assertEquals(List.of(1L, 2L), notifications.stream().map(NotificationEvent::getUserId).toList());
        assertEquals("DEPOSIT_CREATED", notifications.get(0).getEventType());
        assertEquals("DEPOSIT_AGREEMENT", notifications.get(0).getTargetType());
        assertEquals(7L, notifications.get(0).getTargetId());
        assertEquals("/dashboard/deposit-contracts", notifications.get(0).getData().get("targetRoute"));
    }

    private static final class RecordingContractDocumentService extends DepositContractDocumentService {
        private Long depositAgreementId;

        private RecordingContractDocumentService() {
            super(null, null, null, null, null, null, null, null, null, null);
        }

        @Override
        public void generateOfficialContractAfterCommit(Long depositAgreementId) {
            this.depositAgreementId = depositAgreementId;
        }
    }

    private static final class FakeDepositAgreementRepository implements DepositAgreementRepository {
        private final DepositAgreement agreement;

        private FakeDepositAgreementRepository(DepositAgreement agreement) {
            this.agreement = agreement;
        }

        @Override
        public DepositAgreement save(DepositAgreement depositAgreement) {
            return depositAgreement;
        }

        @Override
        public Optional<DepositAgreement> findById(Long id) {
            return Optional.of(agreement);
        }

        @Override
        public List<DepositAgreement> findAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Page<DepositAgreement> findAll(List<Long> ids, DepositAgreementStatus status, List<DepositAgreementStatus> statuses, String search, Long floorId, java.time.LocalDateTime signedFrom, java.time.LocalDateTime signedTo, Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DepositAgreement> findAllByTenantId(Long tenantId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DepositAgreement> findAllAccessibleByUserId(Long userId) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeRoomHoldRepository implements RoomHoldRepository {
        private final RoomHold roomHold;

        private FakeRoomHoldRepository(RoomHold roomHold) {
            this.roomHold = roomHold;
        }

        @Override
        public RoomHold save(RoomHold roomHold) {
            return roomHold;
        }

        @Override
        public Optional<RoomHold> findById(Long id) {
            return Optional.of(roomHold);
        }

        @Override
        public boolean existsByRoomIdAndStatusIn(Long roomId, List<RoomHoldStatus> active) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<RoomHold> findExpiredUnconfirmedHolds(java.time.LocalDateTime now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<RoomHold> findActiveHoldByRoomId(Long roomId, java.time.LocalDateTime now) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakePropertyRepository implements PropertyRepository {
        private final Property property;

        private FakePropertyRepository(Property property) {
            this.property = property;
        }

        @Override
        public Property save(Property property) {
            return property;
        }

        @Override
        public Optional<Property> findById(Long id) {
            return Optional.ofNullable(property);
        }

        @Override
        public Page<Property> findAll(PropertyStatus status, Pageable pageable) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeRoomRepository implements RoomRepository {
        private final Room room;

        private FakeRoomRepository(Room room) {
            this.room = room;
        }

        @Override
        public Room save(Room room) {
            return room;
        }

        @Override
        public Optional<Room> findById(Long id) {
            return Optional.ofNullable(room);
        }

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
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsActiveByPropertyIdAndRoomCode(Long propertyId, String roomCode) {
            return false;
        }

        @Override
        public int updateRoomStatusIfCurrent(Long roomId, RoomStatus expectedStatus, RoomStatus newStatus) {
            return expectedStatus == RoomStatus.ON_HOLD && newStatus == RoomStatus.RESERVED ? 1 : 0;
        }

        @Override
        public List<Room> findAll() {
            throw new UnsupportedOperationException();
        }
    }
}
