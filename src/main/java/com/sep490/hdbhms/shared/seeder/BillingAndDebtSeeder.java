package com.sep490.hdbhms.shared.seeder;

import com.sep490.hdbhms.billingandpayment.application.service.DebtDashboardService;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.InvoiceLineType;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.InvoiceType;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.TransactionProvider;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.TransactionStatus;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.DebtNoticeTrackerEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceLineEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PaymentAllocationEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaDebtNoticeTrackerRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaPaymentAllocationRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaPaymentTransactionRepository;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.Gender;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.PersonProfileEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaPersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.occupancy.domain.valueObjects.FloorStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.PropertyStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.PropertyType;
import com.sep490.hdbhms.occupancy.domain.valueObjects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.FloorEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaFloorRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BillingAndDebtSeeder implements CommandLineRunner {
    static final String SEED_PROPERTY_CODE = "BILLING-DEBT-SEED";
    static final String SEED_MARKER_INVOICE = "SEED-501-CURRENT-RENT";
    static final String TENANT_PASSWORD = "Tenant@123";

    JpaPropertyRepository propertyRepository;
    JpaFloorRepository floorRepository;
    JpaRoomRepository roomRepository;
    JpaLeaseContractRepository leaseContractRepository;
    JpaUserRepository userRepository;
    JpaPersonProfileRepository personProfileRepository;
    JpaInvoiceRepository invoiceRepository;
    JpaInvoiceLineRepository invoiceLineRepository;
    JpaPaymentTransactionRepository paymentTransactionRepository;
    JpaPaymentAllocationRepository paymentAllocationRepository;
    JpaDebtNoticeTrackerRepository debtNoticeTrackerRepository;
    PasswordEncoder passwordEncoder;
    DebtDashboardService debtDashboardService;

    @Override
    @Transactional
    public void run(String... args) {
        if (invoiceRepository.existsByInvoiceCode(SEED_MARKER_INVOICE)) {
            log.info("Billing/debt seed data already exists. Skipping.");
            return;
        }

        UserEntity owner = userRepository.findByRole(Role.OWNER).orElse(null);
        PropertyEntity property = getOrCreateProperty();
        FloorEntity floor = getOrCreateFloor(property);

        SeedRoom room501 = getOrCreateSeedRoom(property, floor, "501", "Nguyen Van Ngoan", "0909000501", 3_500_000L);
        SeedRoom room502 = getOrCreateSeedRoom(property, floor, "502", "Tran Thi No Dien", "0909000502", 3_800_000L);
        SeedRoom room503 = getOrCreateSeedRoom(property, floor, "503", "Le Van No Xau", "0909000503", 4_200_000L);

        YearMonth current = YearMonth.now();
        seedGoodTenant(room501, current, owner);
        seedUtilityDebtTenant(room502, current, owner);
        seedBadDebtTenant(room503, current, owner);
        seedDebtTracker(room503.contract);

        debtDashboardService.processOverdueDebts();
        log.info("Seeded billing/debt demo data for property {} with rooms 501, 502, 503.", property.getName());
    }

    private void seedGoodTenant(SeedRoom seedRoom, YearMonth current, UserEntity owner) {
        createInvoice(seedRoom, current.minusMonths(2), InvoiceType.RENT, InvoiceStatus.PAID, 3_500_000L, owner);
        createInvoice(seedRoom, current.minusMonths(2), InvoiceType.UTILITY, InvoiceStatus.PAID, 420_000L, owner);
        createInvoice(seedRoom, current.minusMonths(1), InvoiceType.RENT, InvoiceStatus.PAID, 3_500_000L, owner);
        createInvoice(seedRoom, current.minusMonths(1), InvoiceType.UTILITY, InvoiceStatus.PAID, 390_000L, owner);
        createInvoice(seedRoom, current, InvoiceType.RENT, InvoiceStatus.ISSUED, 3_500_000L, owner, SEED_MARKER_INVOICE);
        createInvoice(seedRoom, current, InvoiceType.UTILITY, InvoiceStatus.DRAFT, 410_000L, owner);
    }

    private void seedUtilityDebtTenant(SeedRoom seedRoom, YearMonth current, UserEntity owner) {
        createInvoice(seedRoom, current.minusMonths(2), InvoiceType.RENT, InvoiceStatus.PAID, 3_800_000L, owner);
        createInvoice(seedRoom, current.minusMonths(1), InvoiceType.RENT, InvoiceStatus.PAID, 3_800_000L, owner);
        createInvoice(seedRoom, current.minusMonths(1), InvoiceType.UTILITY, InvoiceStatus.OVERDUE, 560_000L, owner);
        createInvoice(seedRoom, current, InvoiceType.RENT, InvoiceStatus.PAID, 3_800_000L, owner);
        createInvoice(seedRoom, current, InvoiceType.UTILITY, InvoiceStatus.OVERDUE, 610_000L, owner);
    }

    private void seedBadDebtTenant(SeedRoom seedRoom, YearMonth current, UserEntity owner) {
        for (int i = 3; i >= 1; i--) {
            YearMonth period = current.minusMonths(i);
            createInvoice(seedRoom, period, InvoiceType.RENT, InvoiceStatus.OVERDUE, 4_200_000L, owner);
            createInvoice(seedRoom, period, InvoiceType.UTILITY, InvoiceStatus.OVERDUE, 700_000L + (i * 50_000L), owner);
        }
        createInvoice(seedRoom, current, InvoiceType.RENT, InvoiceStatus.OVERDUE, 4_200_000L, owner);
        createInvoice(seedRoom, current, InvoiceType.UTILITY, InvoiceStatus.OVERDUE, 760_000L, owner);
    }

    private PropertyEntity getOrCreateProperty() {
        Optional<PropertyEntity> existing = propertyRepository.findAllByDeletedAtIsNull()
                .stream()
                .filter((property) -> SEED_PROPERTY_CODE.equals(property.getPropertyCode()))
                .findFirst();
        return existing.orElseGet(() -> propertyRepository.save(PropertyEntity.builder()
                .propertyCode(SEED_PROPERTY_CODE)
                .name("Toa nha Seed Hoa Don Cong No")
                .propertyType(PropertyType.MINI_APARTMENT)
                .addressLine("123 Duong Demo, Quan Test")
                .description("Seed property for billing and debt dashboard testing.")
                .status(PropertyStatus.ACTIVE)
                .build()));
    }

    private FloorEntity getOrCreateFloor(PropertyEntity property) {
        return floorRepository.findAllByProperty_IdAndDeletedAtIsNull(property.getId())
                .stream()
                .filter((floor) -> "F5".equals(floor.getFloorCode()))
                .findFirst()
                .orElseGet(() -> floorRepository.save(FloorEntity.builder()
                        .property(property)
                        .floorCode("F5")
                        .name("Tang 5")
                        .sortOrder(5)
                        .status(FloorStatus.ACTIVE)
                        .build()));
    }

    private SeedRoom getOrCreateSeedRoom(
            PropertyEntity property,
            FloorEntity floor,
            String roomCode,
            String tenantName,
            String tenantPhone,
            long rent
    ) {
        RoomEntity room = roomRepository.findAllByProperty_IdAndDeletedAtIsNull(property.getId())
                .stream()
                .filter((item) -> roomCode.equals(item.getRoomCode()))
                .findFirst()
                .orElseGet(() -> roomRepository.save(RoomEntity.builder()
                        .property(property)
                        .floor(floor)
                        .roomCode(roomCode)
                        .name("Phong " + roomCode)
                        .listedPrice(rent)
                        .currentStatus(RoomStatus.OCCUPIED)
                        .maxOccupants(3)
                        .sortOrder(Integer.parseInt(roomCode))
                        .build()));
        room.setCurrentStatus(RoomStatus.OCCUPIED);
        room.setListedPrice(rent);
        room = roomRepository.save(room);

        UserEntity tenantUser = getOrCreateTenantUser(tenantName, tenantPhone);
        PersonProfileEntity profile = getOrCreateProfile(tenantUser, tenantName, tenantPhone);
        LeaseContractEntity contract = getOrCreateLeaseContract(room, profile, rent);
        return new SeedRoom(room, contract, tenantName);
    }

    private UserEntity getOrCreateTenantUser(String fullName, String phone) {
        return userRepository.findByPhone(phone)
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .phone(phone)
                        .email(phone + "@seed.hdbhms.local")
                        .passwordHash(passwordEncoder.encode(TENANT_PASSWORD))
                        .role(Role.TENANT)
                        .status(AccountStatus.ACTIVE)
                        .mustChangePassword(false)
                        .build()));
    }

    private PersonProfileEntity getOrCreateProfile(UserEntity user, String fullName, String phone) {
        return personProfileRepository.findByUser_Id(user.getId())
                .or(() -> personProfileRepository.findFirstByPhoneAndDeletedAtIsNull(phone))
                .orElseGet(() -> personProfileRepository.save(PersonProfileEntity.builder()
                        .user(user)
                        .fullName(fullName)
                        .gender(Gender.UNKNOWN)
                        .phone(phone)
                        .email(user.getEmail())
                        .permanentAddress("Dia chi thuong tru demo")
                        .build()));
    }

    private LeaseContractEntity getOrCreateLeaseContract(RoomEntity room, PersonProfileEntity profile, long rent) {
        List<LeaseStatus> activeStatuses = List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON, LeaseStatus.SIGNED, LeaseStatus.CONFIRMED);
        return leaseContractRepository.findFirstByRoom_IdAndStatusInAndDeletedAtIsNullOrderByIdDesc(room.getId(), activeStatuses)
                .orElseGet(() -> leaseContractRepository.save(LeaseContractEntity.builder()
                        .contractCode("SEED-HD-" + room.getRoomCode())
                        .room(room)
                        .primaryTenantProfile(profile)
                        .startDate(LocalDate.now().minusMonths(8))
                        .endDate(LocalDate.now().plusMonths(16))
                        .rentStartDate(LocalDate.now().minusMonths(8))
                        .monthlyRent(rent)
                        .paymentCycleMonths(1)
                        .depositAmount(rent)
                        .status(LeaseStatus.ACTIVE)
                        .build()));
    }

    private InvoiceEntity createInvoice(
            SeedRoom seedRoom,
            YearMonth period,
            InvoiceType invoiceType,
            InvoiceStatus status,
            long amount,
            UserEntity owner
    ) {
        return createInvoice(seedRoom, period, invoiceType, status, amount, owner, invoiceCode(seedRoom.room.getRoomCode(), period, invoiceType));
    }

    private InvoiceEntity createInvoice(
            SeedRoom seedRoom,
            YearMonth period,
            InvoiceType invoiceType,
            InvoiceStatus status,
            long amount,
            UserEntity owner,
            String invoiceCode
    ) {
        if (invoiceRepository.existsByInvoiceCode(invoiceCode)) {
            throw new IllegalStateException("Seed invoice already exists unexpectedly: " + invoiceCode);
        }
        LocalDateTime issueDate = period.atDay(1).atTime(8, 0);
        LocalDateTime dueDate = resolveDueDate(period, status);
        long paidAmount = status == InvoiceStatus.PAID ? amount : 0L;
        long remainingAmount = Math.max(amount - paidAmount, 0L);

        InvoiceEntity invoice = invoiceRepository.save(InvoiceEntity.builder()
                .invoiceCode(invoiceCode)
                .property(seedRoom.room.getProperty())
                .room(seedRoom.room)
                .leastContract(seedRoom.contract)
                .invoiceType(invoiceType)
                .billingPeriod(period.toString())
                .issueDate(issueDate)
                .dueDate(dueDate)
                .status(status)
                .subtotalAmount(amount)
                .discountAmount(0L)
                .totalAmount(amount)
                .paidAmount(paidAmount)
                .remainingAmount(remainingAmount)
                .createdBy(owner)
                .issuedAt(status == InvoiceStatus.DRAFT ? null : issueDate)
                .build());
        createInvoiceLines(invoice, invoiceType, amount);
        if (status == InvoiceStatus.PAID) {
            createPayment(invoice, seedRoom.tenantName, owner);
        }
        return invoice;
    }

    private void createInvoiceLines(InvoiceEntity invoice, InvoiceType invoiceType, long amount) {
        if (invoiceType == InvoiceType.UTILITY) {
            long electricity = Math.round(amount * 0.7);
            long water = amount - electricity;
            invoiceLineRepository.save(InvoiceLineEntity.builder()
                    .invoice(invoice)
                    .lineType(InvoiceLineType.ELECTRICITY)
                    .description("Tien dien thang " + invoice.getBillingPeriod())
                    .quantity(1)
                    .unitPrice(electricity)
                    .build());
            invoiceLineRepository.save(InvoiceLineEntity.builder()
                    .invoice(invoice)
                    .lineType(InvoiceLineType.WATER)
                    .description("Tien nuoc thang " + invoice.getBillingPeriod())
                    .quantity(1)
                    .unitPrice(water)
                    .build());
            return;
        }

        invoiceLineRepository.save(InvoiceLineEntity.builder()
                .invoice(invoice)
                .lineType(InvoiceLineType.ROOM_RENT)
                .description("Tien phong thang " + invoice.getBillingPeriod())
                .quantity(1)
                .unitPrice(amount)
                .build());
    }

    private void createPayment(InvoiceEntity invoice, String payerName, UserEntity owner) {
        String providerTransactionId = "SEED-TXN-" + invoice.getInvoiceCode();
        if (paymentTransactionRepository.existsByProviderAndProviderTransactionId(TransactionProvider.CASH, providerTransactionId)) {
            return;
        }
        Instant transactionTime = invoice.getDueDate()
                .minusDays(1)
                .atZone(ZoneId.systemDefault())
                .toInstant();
        PaymentTransactionEntity transaction = paymentTransactionRepository.save(PaymentTransactionEntity.builder()
                .provider(TransactionProvider.CASH)
                .providerTransactionId(providerTransactionId)
                .amount(invoice.getTotalAmount())
                .transactionTime(transactionTime)
                .payerName(payerName)
                .content("Thanh toan seed " + invoice.getInvoiceCode())
                .status(TransactionStatus.MATCHED)
                .rawPayload(providerTransactionId.getBytes(StandardCharsets.UTF_8))
                .confirmedBy(owner)
                .confirmedAt(transactionTime)
                .build());
        paymentAllocationRepository.save(PaymentAllocationEntity.builder()
                .paymentTransaction(transaction)
                .invoice(invoice)
                .amount(invoice.getTotalAmount())
                .allocatedBy(owner)
                .build());
    }

    private void seedDebtTracker(LeaseContractEntity contract) {
        DebtNoticeTrackerEntity tracker = debtNoticeTrackerRepository
                .findByLeaseContract_Id(contract.getId())
                .orElseGet(() -> DebtNoticeTrackerEntity.builder()
                        .leaseContract(contract)
                        .build());
        tracker.setUnresponsiveCount(3);
        tracker.setLastNoticeDate(LocalDate.now());
        debtNoticeTrackerRepository.save(tracker);
    }

    private LocalDateTime resolveDueDate(YearMonth period, InvoiceStatus status) {
        if (status == InvoiceStatus.OVERDUE) {
            return period.atDay(5).atTime(23, 59, 59);
        }
        if (status == InvoiceStatus.PAID && period.isBefore(YearMonth.now())) {
            return period.atDay(5).atTime(23, 59, 59);
        }
        return LocalDate.now().plusDays(7).atTime(23, 59, 59);
    }

    private String invoiceCode(String roomCode, YearMonth period, InvoiceType invoiceType) {
        return "SEED-" + roomCode + "-" + period + "-" + invoiceType.name().toUpperCase(Locale.ROOT);
    }

    private record SeedRoom(RoomEntity room, LeaseContractEntity contract, String tenantName) {
    }
}
