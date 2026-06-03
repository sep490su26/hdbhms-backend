package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.file.application.port.in.usecase.DownloadFileUseCase;
import com.sep490.hdbhms.file.application.port.in.usecase.UploadFileUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.DepositAgreementRepository;
import com.sep490.hdbhms.occupancy.application.port.out.DepositFormRepository;
import com.sep490.hdbhms.occupancy.application.port.out.PropertyRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.model.DepositForm;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.value_objects.PropertyStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.shared.constant.DefaultConfig;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

class DepositContractDocumentServiceTest {

    @Test
    void generateOfficialContractAfterCommitDoesNotBreakPaymentFlowWhenPdfUploadFails() {
        DepositAgreement agreement = DepositAgreement.builder()
                .id(11L)
                .depositCode("DC-TEST-001")
                .roomId(101L)
                .depositFormId(301L)
                .amount(1_000_000L)
                .expectedLeaseSignDate(LocalDate.now().plusDays(2))
                .expectedMoveInDate(LocalDate.now().plusDays(5))
                .status(DepositAgreementStatus.PAID)
                .confirmedAt(LocalDateTime.now())
                .build();

        var service = new DepositContractDocumentService(
                new FakeRoomRepository(),
                new FakePropertyRepository(),
                new FakeDepositFormRepository(),
                failingUploadUseCase(),
                query -> null,
                defaultConfig(),
                new FakeDepositAgreementRepository(agreement),
                new ImmediateTransactionManager(),
                null,
                null
        );

        assertDoesNotThrow(() -> service.generateOfficialContractAfterCommit(11L));
        assertNull(agreement.getContractFileId());
    }

    private static UploadFileUseCase failingUploadUseCase() {
        return command -> {
            throw new IOException("Simulated storage failure");
        };
    }

    private static DefaultConfig defaultConfig() {
        DefaultConfig config = new DefaultConfig();
        config.getOwner().setFullName("Hải Đăng House");
        config.getOwner().setPhone("0900000000");
        config.getOwner().setEmail("owner@haidang.test");
        return config;
    }

    private static final class ImmediateTransactionManager implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
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
            return List.of(agreement);
        }

        @Override
        public Page<DepositAgreement> findAll(List<Long> ids, DepositAgreementStatus status, LocalDateTime signedFrom, LocalDateTime signedTo, Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DepositAgreement> findAllByTenantId(Long tenantId) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeRoomRepository implements RoomRepository {
        @Override
        public Room save(Room room) {
            return room;
        }

        @Override
        public Optional<Room> findById(Long id) {
            return Optional.of(Room.builder()
                    .id(id)
                    .propertyId(1L)
                    .floorId(1L)
                    .roomCode("101")
                    .name("Phòng 101")
                    .areaM2(BigDecimal.valueOf(18))
                    .listedPrice(2_200_000L)
                    .currentStatus(RoomStatus.RESERVED)
                    .maxOccupants(3)
                    .build());
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
        public int updateRoomStatusIfCurrent(Long roomId, RoomStatus expectedStatus, RoomStatus newStatus) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Room> findAll() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakePropertyRepository implements PropertyRepository {
        @Override
        public Property save(Property property) {
            return property;
        }

        @Override
        public Optional<Property> findById(Long id) {
            return Optional.of(Property.builder()
                    .id(id)
                    .propertyCode("HD1")
                    .name("Hải Đăng 1")
                    .addressLine("Số 1, Hà Nội")
                    .build());
        }

        @Override
        public Page<Property> findAll(PropertyStatus status, Pageable pageable) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeDepositFormRepository implements DepositFormRepository {
        @Override
        public Optional<DepositForm> findById(Long id) {
            return Optional.of(DepositForm.builder()
                    .id(id)
                    .fullName("Nguyễn Văn A")
                    .dob(LocalDate.of(1999, 1, 1))
                    .phone("0900000001")
                    .email("tenant@haidang.test")
                    .idNumber("001199900001")
                    .idIssueDate(LocalDate.of(2020, 1, 1))
                    .idIssuePlace("Cục CSQLHC về TTXH")
                    .permanentAddress("Hà Nội")
                    .build());
        }

        @Override
        public DepositForm save(DepositForm depositForm) {
            return depositForm;
        }
    }
}
