package com.sep490.hdbhms.billingandpayment.application.service;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceLineType;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceType;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceLineEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.MockUtilityInvoiceBatchResponse;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.MockUtilityInvoiceResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.MeterType;
import com.sep490.hdbhms.occupancy.domain.value_objects.UtilityType;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaUtilityTariffRepository;
import com.sep490.hdbhms.shared.id.SnowflakeIdGenerator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MockUtilityInvoiceService {
    static final List<LeaseStatus> CONTRACT_STATUSES = List.of(
            LeaseStatus.ACTIVE,
            LeaseStatus.EXPIRING_SOON,
            LeaseStatus.TERMINATION_PENDING,
            LeaseStatus.EXPIRED,
            LeaseStatus.LIQUIDATED,
            LeaseStatus.RENEWED
    );
    static final String SOURCE_TYPE = "MOCK_UTILITY_INVOICE";
    static final DateTimeFormatter LEGACY_PERIOD = DateTimeFormatter.ofPattern("M/uuuu");

    JpaPropertyRepository propertyRepository;
    JpaRoomRepository roomRepository;
    JpaLeaseContractRepository leaseContractRepository;
    JpaMeterReadingRepository meterReadingRepository;
    JpaUtilityTariffRepository utilityTariffRepository;
    JpaInvoiceRepository invoiceRepository;
    JpaInvoiceLineRepository invoiceLineRepository;
    JpaUserRepository userRepository;
    SnowflakeIdGenerator snowflakeIdGenerator;
    JdbcTemplate jdbcTemplate;

    @Transactional
    public MockUtilityInvoiceResponse createForRoom(Long roomId, String billingPeriod, Integer dueDays, Long currentUserId) {
        YearMonth period = requirePeriod(billingPeriod);
        RoomEntity room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay phong."));

        LeaseContractEntity contract = findContract(room.getId(), period);
        if (contract == null) {
            return skipped(room, period, "Phong khong co hop dong thue phat sinh trong ky.");
        }

        InvoiceEntity existing = invoiceRepository
                .findFirstByLeastContract_IdAndBillingPeriodAndInvoiceTypeAndStatusNotOrderByIdDesc(
                        contract.getId(),
                        period.toString(),
                        InvoiceType.UTILITY,
                        InvoiceStatus.VOIDED
                )
                .orElse(null);
        if (existing != null) {
            return toResponse(existing, false, "Hoa don dien nuoc da ton tai.");
        }

        List<MeterCharge> charges = buildCharges(room, period);
        long totalAmount = charges.stream().mapToLong(MeterCharge::amount).sum();
        if (charges.isEmpty()) {
            return skipped(room, period, "Phong chua co chi so dien/nuoc trong ky.");
        }
        if (totalAmount <= 0) {
            return skipped(room, period, "Ky nay khong phat sinh tien dien nuoc.");
        }

        LocalDateTime now = LocalDateTime.now();
        int paymentDueDays = dueDays == null ? 7 : dueDays;
        if (paymentDueDays <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Han thanh toan phai lon hon 0 ngay.");
        }

        InvoiceEntity invoice = invoiceRepository.saveAndFlush(InvoiceEntity.builder()
                .invoiceCode("INV-MOCK-UTL-" + room.getId() + "-" + period.toString().replace("-", "") + "-" + snowflakeIdGenerator.next())
                .property(room.getProperty())
                .room(room)
                .leastContract(contract)
                .invoiceType(InvoiceType.UTILITY)
                .revisionNo(nextRevision(contract.getId(), period))
                .billingPeriod(period.toString())
                .issueDate(now)
                .dueDate(now.plusDays(paymentDueDays))
                .status(InvoiceStatus.DRAFT)
                .subtotalAmount(totalAmount)
                .discountAmount(0L)
                .totalAmount(totalAmount)
                .paidAmount(0L)
                .remainingAmount(totalAmount)
                .createdBy(currentUserId == null ? null : userRepository.getReferenceById(currentUserId))
                .build());

        for (MeterCharge charge : charges) {
            invoiceLineRepository.save(InvoiceLineEntity.builder()
                    .invoice(invoice)
                    .lineType(charge.lineType())
                    .description(charge.description())
                    .quantity(charge.quantity())
                    .unitPrice(charge.unitPrice())
                    .meterReading(charge.reading())
                    .sourceType(SOURCE_TYPE)
                    .sourceId(charge.reading().getId())
                    .build());
        }

        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(now);
        invoice = invoiceRepository.saveAndFlush(invoice);
        return toResponse(invoice, true, "Da tao mock hoa don dien nuoc.");
    }

    @Transactional
    public MockUtilityInvoiceBatchResponse createForProperty(Long propertyId, String billingPeriod, Integer dueDays, Long currentUserId) {
        YearMonth period = requirePeriod(billingPeriod);
        propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay co so."));

        List<RoomEntity> rooms = leaseContractRepository.findMeterReadingRoomsByPeriod(
                propertyId,
                CONTRACT_STATUSES,
                period.atDay(1),
                period.atEndOfMonth()
        );
        if (rooms.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khong co phong can tao hoa don dien nuoc trong ky nay.");
        }

        List<MockUtilityInvoiceResponse> results = rooms.stream()
                .map(room -> createForRoom(room.getId(), period.toString(), dueDays, currentUserId))
                .toList();
        int createdCount = (int) results.stream().filter(result -> Boolean.TRUE.equals(result.created())).count();
        int skippedCount = (int) results.stream().filter(result -> result.invoiceId() == null).count();
        return new MockUtilityInvoiceBatchResponse(propertyId, period.toString(), createdCount, skippedCount, results);
    }

    private List<MeterCharge> buildCharges(RoomEntity room, YearMonth period) {
        List<MeterCharge> charges = new ArrayList<>();
        for (MeterReadingEntity reading : meterReadingRepository.findLatestActiveByRoomAndPeriod(room.getId(), period.toString())) {
            MeterType meterType = reading.getMeter().getMeterType();
            if (meterType == MeterType.ELECTRICITY) {
                charges.add(buildCharge(reading, InvoiceLineType.ELECTRICITY, UtilityType.ELECTRICITY, "Tien dien"));
            } else if (meterType == MeterType.WATER) {
                charges.add(buildCharge(reading, InvoiceLineType.WATER, UtilityType.WATER, "Tien nuoc"));
            }
        }
        return charges;
    }

    private MeterCharge buildCharge(
            MeterReadingEntity reading,
            InvoiceLineType lineType,
            UtilityType utilityType,
            String label
    ) {
        BigDecimal previousValue = reading.getPreviousValue() == null ? BigDecimal.ZERO : reading.getPreviousValue();
        BigDecimal currentValue = reading.getCurrentValue() == null ? BigDecimal.ZERO : reading.getCurrentValue();
        BigDecimal usage = currentValue.subtract(previousValue);
        if (usage.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " co chi so moi nho hon chi so cu.");
        }

        UtilityTariffSnapshot tariff = readTariff(
                reading.getRoom().getProperty().getId(),
                utilityType,
                reading.getReadingDate()
        );
        int quantity = billableQuantity(usage, tariff.freeAllowance());
        long amount = quantity * tariff.unitPrice();
        String description = "%s %s: %s -> %s, mien phi %d, tinh %d x %d".formatted(
                label,
                reading.getReadingPeriod(),
                previousValue.stripTrailingZeros().toPlainString(),
                currentValue.stripTrailingZeros().toPlainString(),
                tariff.freeAllowance(),
                quantity,
                tariff.unitPrice()
        );
        return new MeterCharge(reading, lineType, description, quantity, tariff.unitPrice(), amount);
    }

    static int billableQuantity(BigDecimal usage, long freeAllowance) {
        BigDecimal billableUsage = usage.subtract(BigDecimal.valueOf(freeAllowance));
        if (billableUsage.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return billableUsage.setScale(0, RoundingMode.CEILING).intValue();
    }

    private UtilityTariffSnapshot readTariff(Long propertyId, UtilityType utilityType, LocalDate readingDate) {
        return utilityTariffRepository.findEffectiveTariffs(propertyId, utilityType, readingDate)
                .stream()
                .findFirst()
                .map(tariff -> new UtilityTariffSnapshot(tariff.getUnitPrice(), tariff.getFreeAllowance()))
                .orElseGet(() -> switch (utilityType) {
                    case ELECTRICITY -> new UtilityTariffSnapshot(3500L, 0L);
                    case WATER -> new UtilityTariffSnapshot(20000L, 6L);
                    default -> new UtilityTariffSnapshot(0L, 0L);
                });
    }

    private LeaseContractEntity findContract(Long roomId, YearMonth period) {
        return leaseContractRepository.findMeterReadingContractsByRoomAndPeriod(
                roomId,
                CONTRACT_STATUSES,
                period.atDay(1),
                period.atEndOfMonth()
        ).stream().findFirst().orElse(null);
    }

    private int nextRevision(Long contractId, YearMonth period) {
        Integer maxRevision = jdbcTemplate.queryForObject("""
                        SELECT COALESCE(MAX(revision_no), 0)
                        FROM invoices
                        WHERE lease_contract_id = ?
                          AND billing_period = ?
                          AND invoice_type = ?
                        """,
                Integer.class,
                contractId,
                period.toString(),
                InvoiceType.UTILITY.name()
        );
        return (maxRevision == null ? 0 : maxRevision) + 1;
    }

    private YearMonth requirePeriod(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Billing period la bat buoc.");
        }
        String period = value.trim();
        try {
            return period.contains("/") ? YearMonth.parse(period, LEGACY_PERIOD) : YearMonth.parse(period);
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Billing period phai co dinh dang yyyy-MM hoac MM/yyyy.");
        }
    }

    private MockUtilityInvoiceResponse skipped(RoomEntity room, YearMonth period, String message) {
        return new MockUtilityInvoiceResponse(
                null,
                null,
                period.toString(),
                room.getProperty().getId(),
                room.getId(),
                room.getRoomCode(),
                null,
                null,
                "SKIPPED",
                0L,
                false,
                message,
                List.of()
        );
    }

    private MockUtilityInvoiceResponse toResponse(InvoiceEntity invoice, boolean created, String message) {
        List<MockUtilityInvoiceResponse.Line> lines = invoiceLineRepository.findByInvoice_IdOrderByIdAsc(invoice.getId())
                .stream()
                .map(this::toLineResponse)
                .toList();
        return new MockUtilityInvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceCode(),
                invoice.getBillingPeriod(),
                invoice.getProperty() == null ? null : invoice.getProperty().getId(),
                invoice.getRoom() == null ? null : invoice.getRoom().getId(),
                invoice.getRoom() == null ? null : invoice.getRoom().getRoomCode(),
                invoice.getLeastContract() == null ? null : invoice.getLeastContract().getId(),
                invoice.getLeastContract() == null ? null : invoice.getLeastContract().getContractCode(),
                invoice.getStatus() == null ? null : invoice.getStatus().name(),
                invoice.getTotalAmount(),
                created,
                message,
                lines
        );
    }

    private MockUtilityInvoiceResponse.Line toLineResponse(InvoiceLineEntity line) {
        return new MockUtilityInvoiceResponse.Line(
                line.getId(),
                line.getLineType() == null ? null : line.getLineType().name(),
                line.getDescription(),
                line.getQuantity(),
                line.getUnitPrice(),
                lineAmount(line),
                line.getMeterReading() == null ? null : line.getMeterReading().getId()
        );
    }

    private long lineAmount(InvoiceLineEntity line) {
        if (line.getAmount() != null) {
            return line.getAmount();
        }
        return (line.getUnitPrice() == null ? 0L : line.getUnitPrice())
                * (line.getQuantity() == null ? 1L : line.getQuantity());
    }

    private record MeterCharge(
            MeterReadingEntity reading,
            InvoiceLineType lineType,
            String description,
            int quantity,
            long unitPrice,
            long amount
    ) {
    }

    private record UtilityTariffSnapshot(long unitPrice, long freeAllowance) {
    }
}
