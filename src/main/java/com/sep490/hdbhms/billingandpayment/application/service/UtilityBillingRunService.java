package com.sep490.hdbhms.billingandpayment.application.service;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceLineType;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceReason;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceType;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.UtilityBillingRunItemStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.UtilityBillingRunStatus;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceLineEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.RoomUtilityBaselineEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.UtilityBillingRunEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.UtilityBillingRunItemEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaRoomUtilityBaselineRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaUtilityBillingRunItemRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaUtilityBillingRunRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.UtilityBillingItemAdjustmentRequest;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.UtilityBillingRunResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.occupancy.domain.value_objects.AnomalyType;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.MeterType;
import com.sep490.hdbhms.occupancy.domain.value_objects.UtilityType;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingAnomalyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.UtilityTariffEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingAnomalyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaUtilityTariffRepository;
import com.sep490.hdbhms.shared.id.SnowflakeIdGenerator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UtilityBillingRunService {
    static final List<LeaseStatus> BILLABLE_STATUSES = List.of(
            LeaseStatus.ACTIVE,
            LeaseStatus.EXPIRING_SOON,
            LeaseStatus.TERMINATION_PENDING
    );
    static final String SOURCE_TYPE = "UTILITY_BILLING_RUN_ITEM";
    static final DateTimeFormatter LEGACY_PERIOD = DateTimeFormatter.ofPattern("M/uuuu");

    JpaPropertyRepository propertyRepository;
    JpaLeaseContractRepository leaseContractRepository;
    JpaMeterReadingRepository meterReadingRepository;
    JpaMeterReadingAnomalyRepository anomalyRepository;
    JpaUtilityTariffRepository utilityTariffRepository;
    JpaUtilityBillingRunRepository runRepository;
    JpaUtilityBillingRunItemRepository itemRepository;
    JpaRoomUtilityBaselineRepository baselineRepository;
    JpaInvoiceRepository invoiceRepository;
    JpaInvoiceLineRepository invoiceLineRepository;
    JpaUserRepository userRepository;
    SnowflakeIdGenerator snowflakeIdGenerator;
    JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "0 0 2 25 * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void createMonthlyRunsOnBillingDay() {
        YearMonth period = YearMonth.now();
        int created = 0;
        for (PropertyEntity property : propertyRepository.findAllByDeletedAtIsNull()) {
            try {
                if (!runRepository.existsByProperty_IdAndBillingPeriodAndInvoiceReasonAndStatusNot(
                        property.getId(),
                        period.toString(),
                        InvoiceReason.MONTHLY,
                        UtilityBillingRunStatus.CANCELLED
                )) {
                    createPreview(property.getId(), period.toString(), InvoiceReason.MONTHLY.name(), null);
                    created++;
                }
            } catch (RuntimeException exception) {
                log.warn("Failed to create utility billing run for property {}", property.getId(), exception);
            }
        }
        if (created > 0) {
            log.info("Created {} monthly utility billing runs for {}", created, period);
        }
    }

    @Transactional
    public UtilityBillingRunResponse createPreview(
            Long propertyId,
            String billingPeriod,
            String invoiceReason,
            Long currentUserId
    ) {
        YearMonth period = requirePeriod(billingPeriod);
        InvoiceReason reason = parseReason(invoiceReason);
        PropertyEntity property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found."));

        UtilityBillingRunEntity run = runRepository
                .findByProperty_IdAndBillingPeriodAndInvoiceReason(propertyId, period.toString(), reason)
                .orElseGet(() -> UtilityBillingRunEntity.builder()
                        .property(property)
                        .billingPeriod(period.toString())
                        .invoiceReason(reason)
                        .createdBy(currentUserId == null ? null : userRepository.getReferenceById(currentUserId))
                        .build());

        if (run.getStatus() == UtilityBillingRunStatus.INVOICES_CREATED) {
            return getRun(run.getId());
        }

        run.setStatus(UtilityBillingRunStatus.PREVIEWED);
        run = runRepository.saveAndFlush(run);
        itemRepository.deleteByRun_Id(run.getId());

        List<RoomEntity> rooms = leaseContractRepository.findMeterReadingRoomsByPeriod(
                propertyId,
                BILLABLE_STATUSES,
                period.atDay(1),
                period.atEndOfMonth()
        );
        if (rooms.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No rooms require utility billing in this period.");
        }

        for (RoomEntity room : rooms) {
            itemRepository.save(buildItem(run, room, period));
        }
        syncRunTotals(run.getId());
        return getRun(run.getId());
    }

    @Transactional(readOnly = true)
    public UtilityBillingRunResponse getRun(Long runId) {
        UtilityBillingRunEntity run = runRepository.findById(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utility billing run not found."));
        return toResponse(run, itemRepository.findByRun_IdOrderByRoom_RoomCodeAscIdAsc(runId));
    }

    @Transactional
    public UtilityBillingRunResponse updateAdjustment(
            Long runId,
            Long itemId,
            UtilityBillingItemAdjustmentRequest request
    ) {
        UtilityBillingRunEntity run = requireEditableRun(runId);
        UtilityBillingRunItemEntity item = itemRepository.findByIdAndRun_Id(itemId, runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utility billing item not found."));
        long discount = request == null || request.discountAmount() == null ? 0L : request.discountAmount();
        if (discount < 0 || discount > safe(item.getSubtotalAmount())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount amount is invalid.");
        }
        item.setDiscountAmount(discount);
        item.setAdjustmentReason(request == null ? null : cleanText(request.adjustmentReason()));
        item.setTotalAmount(Math.max(safe(item.getSubtotalAmount()) - discount, 0L));
        itemRepository.save(item);
        syncRunTotals(run.getId());
        return getRun(run.getId());
    }

    @Transactional
    public UtilityBillingRunResponse confirmRun(Long runId) {
        UtilityBillingRunEntity run = requireEditableRun(runId);
        run.setStatus(UtilityBillingRunStatus.CONFIRMED);
        runRepository.save(run);
        return getRun(runId);
    }

    @Transactional
    public UtilityBillingRunResponse generateInvoices(Long runId, Integer dueDays, Long currentUserId) {
        UtilityBillingRunEntity run = runRepository.findById(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utility billing run not found."));
        if (run.getStatus() == UtilityBillingRunStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utility billing run is cancelled.");
        }
        if (run.getStatus() == UtilityBillingRunStatus.INVOICES_CREATED) {
            return getRun(runId);
        }

        int paymentDueDays = dueDays == null ? 7 : dueDays;
        if (paymentDueDays <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Due days must be greater than 0.");
        }

        int invoiceCount = 0;
        LocalDateTime now = LocalDateTime.now();
        for (UtilityBillingRunItemEntity item : itemRepository.findByRun_IdOrderByRoom_RoomCodeAscIdAsc(runId)) {
            if (!hasBillableReadings(item)) {
                item.setStatus(UtilityBillingRunItemStatus.SKIPPED);
                itemRepository.save(item);
                continue;
            }

            InvoiceEntity invoice = null;
            if (safe(item.getTotalAmount()) > 0) {
                invoice = findExistingInvoice(item, run)
                        .orElseGet(() -> createInvoice(item, run, paymentDueDays, now, currentUserId));
                item.setInvoice(invoice);
                item.setStatus(UtilityBillingRunItemStatus.INVOICED);
                invoiceCount++;
            } else {
                item.setStatus(UtilityBillingRunItemStatus.SKIPPED);
            }

            advanceBaseline(item.getElectricityReading(), invoice);
            advanceBaseline(item.getWaterReading(), invoice);
            itemRepository.save(item);
        }

        run.setStatus(UtilityBillingRunStatus.INVOICES_CREATED);
        run.setGeneratedBy(currentUserId == null ? null : userRepository.getReferenceById(currentUserId));
        run.setGeneratedAt(now);
        run.setGeneratedInvoiceCount(invoiceCount);
        runRepository.save(run);
        syncRunTotals(runId);
        return getRun(runId);
    }

    private UtilityBillingRunItemEntity buildItem(
            UtilityBillingRunEntity run,
            RoomEntity room,
            YearMonth period
    ) {
        LeaseContractEntity contract = findContract(room.getId(), period);
        Map<MeterType, MeterReadingEntity> readings = meterReadingRepository
                .findLatestActiveByRoomAndPeriod(room.getId(), period.toString())
                .stream()
                .collect(Collectors.toMap(
                        reading -> reading.getMeter().getMeterType(),
                        Function.identity(),
                        (first, ignored) -> first
                ));

        MeterReadingEntity electricity = readings.get(MeterType.ELECTRICITY);
        MeterReadingEntity water = readings.get(MeterType.WATER);
        List<Long> readingIds = new ArrayList<>();
        if (electricity != null) readingIds.add(electricity.getId());
        if (water != null) readingIds.add(water.getId());
        String anomalyMessage = readAnomalyMessage(readingIds);

        Charge electricityCharge = buildCharge(electricity, UtilityType.ELECTRICITY);
        Charge waterCharge = buildCharge(water, UtilityType.WATER);
        StringJoiner warnings = new StringJoiner("; ");
        if (contract == null) warnings.add("No billable contract in this period");
        if (electricity == null) warnings.add("Missing electricity reading");
        if (water == null) warnings.add("Missing water reading");
        if (electricityCharge.warning() != null) warnings.add(electricityCharge.warning());
        if (waterCharge.warning() != null) warnings.add(waterCharge.warning());
        if (anomalyMessage != null && !anomalyMessage.isBlank()) warnings.add(anomalyMessage);

        long subtotal = electricityCharge.amount() + waterCharge.amount();
        boolean canInvoice = contract != null
                && electricity != null
                && water != null
                && electricityCharge.warning() == null
                && waterCharge.warning() == null;
        UtilityBillingRunItemStatus status;
        if (!canInvoice || subtotal <= 0) {
            status = UtilityBillingRunItemStatus.SKIPPED;
        } else if (warnings.length() > 0) {
            status = UtilityBillingRunItemStatus.WARNING;
        } else {
            status = UtilityBillingRunItemStatus.READY;
        }

        return UtilityBillingRunItemEntity.builder()
                .run(run)
                .room(room)
                .leaseContract(contract)
                .electricityReading(electricity)
                .waterReading(water)
                .electricityPrevious(electricityCharge.previous())
                .electricityCurrent(electricityCharge.current())
                .electricityUsage(electricityCharge.usage())
                .electricityQuantity(electricityCharge.quantity())
                .electricityUnitPrice(electricityCharge.unitPrice())
                .electricityAmount(electricityCharge.amount())
                .waterPrevious(waterCharge.previous())
                .waterCurrent(waterCharge.current())
                .waterUsage(waterCharge.usage())
                .waterQuantity(waterCharge.quantity())
                .waterUnitPrice(waterCharge.unitPrice())
                .waterAmount(waterCharge.amount())
                .subtotalAmount(subtotal)
                .discountAmount(0L)
                .totalAmount(subtotal)
                .warningMessage(warnings.length() == 0 ? null : warnings.toString())
                .status(status)
                .build();
    }

    private Charge buildCharge(MeterReadingEntity reading, UtilityType utilityType) {
        if (reading == null) {
            return Charge.empty();
        }
        BigDecimal previous = baselineRepository.findByMeter_Id(reading.getMeter().getId())
                .map(RoomUtilityBaselineEntity::getLastBilledReading)
                .orElseGet(() -> safe(reading.getPreviousValue()));
        BigDecimal current = safe(reading.getCurrentValue());
        BigDecimal usage = current.subtract(previous);
        if (usage.compareTo(BigDecimal.ZERO) < 0) {
            return new Charge(previous, current, usage, 0, 0L, 0L, "Current reading is lower than billing baseline");
        }

        UtilityTariffSnapshot tariff = readTariff(
                reading.getRoom().getProperty().getId(),
                utilityType,
                reading.getReadingDate()
        );
        int quantity = billableQuantity(usage, tariff.freeAllowance());
        long amount = quantity * tariff.unitPrice();
        return new Charge(previous, current, usage, quantity, tariff.unitPrice(), amount, null);
    }

    private InvoiceEntity createInvoice(
            UtilityBillingRunItemEntity item,
            UtilityBillingRunEntity run,
            int dueDays,
            LocalDateTime now,
            Long currentUserId
    ) {
        InvoiceEntity invoice = invoiceRepository.saveAndFlush(InvoiceEntity.builder()
                .invoiceCode("INV-UTL-" + run.getInvoiceReason().name() + "-" + item.getRoom().getId()
                        + "-" + run.getBillingPeriod().replace("-", "") + "-" + snowflakeIdGenerator.next())
                .property(run.getProperty())
                .room(item.getRoom())
                .leastContract(item.getLeaseContract())
                .invoiceType(InvoiceType.UTILITY)
                .invoiceReason(run.getInvoiceReason())
                .revisionNo(nextRevision(item.getLeaseContract().getId(), run.getBillingPeriod(), InvoiceType.UTILITY))
                .billingPeriod(run.getBillingPeriod())
                .issueDate(now)
                .dueDate(now.plusDays(dueDays))
                .status(InvoiceStatus.DRAFT)
                .subtotalAmount(safe(item.getSubtotalAmount()))
                .discountAmount(safe(item.getDiscountAmount()))
                .totalAmount(safe(item.getTotalAmount()))
                .paidAmount(0L)
                .remainingAmount(safe(item.getTotalAmount()))
                .createdBy(currentUserId == null ? null : userRepository.getReferenceById(currentUserId))
                .build());

        saveInvoiceLine(invoice, item, InvoiceLineType.ELECTRICITY, item.getElectricityReading(),
                item.getElectricityQuantity(), item.getElectricityUnitPrice(), "Electricity");
        saveInvoiceLine(invoice, item, InvoiceLineType.WATER, item.getWaterReading(),
                item.getWaterQuantity(), item.getWaterUnitPrice(), "Water");

        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(now);
        return invoiceRepository.saveAndFlush(invoice);
    }

    private void saveInvoiceLine(
            InvoiceEntity invoice,
            UtilityBillingRunItemEntity item,
            InvoiceLineType lineType,
            MeterReadingEntity reading,
            Integer quantity,
            Long unitPrice,
            String label
    ) {
        if (reading == null) {
            return;
        }
        BigDecimal previous = lineType == InvoiceLineType.ELECTRICITY
                ? item.getElectricityPrevious()
                : item.getWaterPrevious();
        BigDecimal current = lineType == InvoiceLineType.ELECTRICITY
                ? item.getElectricityCurrent()
                : item.getWaterCurrent();
        invoiceLineRepository.save(InvoiceLineEntity.builder()
                .invoice(invoice)
                .lineType(lineType)
                .description("%s %s: %s -> %s".formatted(
                        label,
                        invoice.getBillingPeriod(),
                        valueText(previous),
                        valueText(current)
                ))
                .quantity(quantity == null ? 0 : quantity)
                .unitPrice(unitPrice == null ? 0L : unitPrice)
                .meterReading(reading)
                .sourceType(SOURCE_TYPE)
                .sourceId(item.getId())
                .build());
    }

    private Optional<InvoiceEntity> findExistingInvoice(UtilityBillingRunItemEntity item, UtilityBillingRunEntity run) {
        if (item.getLeaseContract() == null) {
            return Optional.empty();
        }
        return invoiceRepository.findFirstByLeastContract_IdAndBillingPeriodAndInvoiceTypeAndInvoiceReasonAndStatusNotOrderByIdDesc(
                item.getLeaseContract().getId(),
                run.getBillingPeriod(),
                InvoiceType.UTILITY,
                run.getInvoiceReason(),
                InvoiceStatus.VOIDED
        );
    }

    private void advanceBaseline(MeterReadingEntity reading, InvoiceEntity invoice) {
        if (reading == null || reading.getCurrentValue() == null || reading.getMeter() == null) {
            return;
        }
        RoomUtilityBaselineEntity baseline = baselineRepository.findByMeter_Id(reading.getMeter().getId())
                .orElseGet(() -> RoomUtilityBaselineEntity.builder()
                        .room(reading.getRoom())
                        .meter(reading.getMeter())
                        .build());
        baseline.setRoom(reading.getRoom());
        baseline.setMeter(reading.getMeter());
        baseline.setLastBilledReading(reading.getCurrentValue());
        baseline.setLastInvoice(invoice);
        baselineRepository.save(baseline);
    }

    private void syncRunTotals(Long runId) {
        UtilityBillingRunEntity run = runRepository.findById(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utility billing run not found."));
        List<UtilityBillingRunItemEntity> items = itemRepository.findByRun_IdOrderByRoom_RoomCodeAscIdAsc(runId);
        run.setTotalRooms(items.size());
        run.setReadyCount((int) items.stream().filter(item -> item.getStatus() == UtilityBillingRunItemStatus.READY).count());
        run.setWarningCount((int) items.stream().filter(item -> item.getStatus() == UtilityBillingRunItemStatus.WARNING).count());
        run.setSkippedCount((int) items.stream().filter(item -> item.getStatus() == UtilityBillingRunItemStatus.SKIPPED).count());
        run.setGeneratedInvoiceCount((int) items.stream().filter(item -> item.getStatus() == UtilityBillingRunItemStatus.INVOICED).count());
        run.setSubtotalAmount(items.stream().mapToLong(item -> safe(item.getSubtotalAmount())).sum());
        run.setDiscountAmount(items.stream().mapToLong(item -> safe(item.getDiscountAmount())).sum());
        run.setTotalAmount(items.stream().mapToLong(item -> safe(item.getTotalAmount())).sum());
        runRepository.save(run);
    }

    private UtilityBillingRunEntity requireEditableRun(Long runId) {
        UtilityBillingRunEntity run = runRepository.findById(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utility billing run not found."));
        if (run.getStatus() == UtilityBillingRunStatus.INVOICES_CREATED
                || run.getStatus() == UtilityBillingRunStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utility billing run can no longer be edited.");
        }
        return run;
    }

    private boolean hasBillableReadings(UtilityBillingRunItemEntity item) {
        return item.getLeaseContract() != null
                && item.getElectricityReading() != null
                && item.getWaterReading() != null
                && notNegative(item.getElectricityUsage())
                && notNegative(item.getWaterUsage());
    }

    private boolean notNegative(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) >= 0;
    }

    private LeaseContractEntity findContract(Long roomId, YearMonth period) {
        return leaseContractRepository.findMeterReadingContractsByRoomAndPeriod(
                roomId,
                BILLABLE_STATUSES,
                period.atDay(1),
                period.atEndOfMonth()
        ).stream().findFirst().orElse(null);
    }

    private String readAnomalyMessage(List<Long> readingIds) {
        if (readingIds.isEmpty()) {
            return null;
        }
        List<String> messages = anomalyRepository.findByMeterReading_IdInAndResolvedAtIsNullOrderByIdAsc(readingIds)
                .stream()
                .filter(anomaly -> anomaly.getAnomalyType() != AnomalyType.MISSING_READING)
                .map(MeterReadingAnomalyEntity::getMessage)
                .filter(message -> message != null && !message.isBlank())
                .toList();
        return messages.isEmpty() ? null : String.join("; ", messages);
    }

    static int billableQuantity(BigDecimal usage, long freeAllowance) {
        BigDecimal billableUsage = safe(usage).subtract(BigDecimal.valueOf(freeAllowance));
        if (billableUsage.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return billableUsage.setScale(0, RoundingMode.CEILING).intValue();
    }

    private UtilityTariffSnapshot readTariff(Long propertyId, UtilityType utilityType, LocalDate readingDate) {
        return utilityTariffRepository.findEffectiveTariffs(propertyId, utilityType, readingDate)
                .stream()
                .findFirst()
                .map(this::toSnapshot)
                .orElseGet(() -> switch (utilityType) {
                    case ELECTRICITY -> new UtilityTariffSnapshot(3500L, 0L);
                    case WATER -> new UtilityTariffSnapshot(20000L, 6L);
                    default -> new UtilityTariffSnapshot(0L, 0L);
                });
    }

    private UtilityTariffSnapshot toSnapshot(UtilityTariffEntity tariff) {
        return new UtilityTariffSnapshot(safe(tariff.getUnitPrice()), safe(tariff.getFreeAllowance()));
    }

    private int nextRevision(Long contractId, String billingPeriod, InvoiceType invoiceType) {
        Integer maxRevision = jdbcTemplate.queryForObject("""
                        SELECT COALESCE(MAX(revision_no), 0)
                        FROM invoices
                        WHERE lease_contract_id = ?
                          AND billing_period = ?
                          AND invoice_type = ?
                        """,
                Integer.class,
                contractId,
                billingPeriod,
                invoiceType.name()
        );
        return (maxRevision == null ? 0 : maxRevision) + 1;
    }

    private UtilityBillingRunResponse toResponse(UtilityBillingRunEntity run, List<UtilityBillingRunItemEntity> items) {
        return new UtilityBillingRunResponse(
                run.getId(),
                run.getProperty() == null ? null : run.getProperty().getId(),
                run.getProperty() == null ? null : run.getProperty().getName(),
                run.getBillingPeriod(),
                run.getInvoiceReason() == null ? null : run.getInvoiceReason().name(),
                run.getStatus() == null ? null : run.getStatus().name(),
                run.getTotalRooms(),
                run.getReadyCount(),
                run.getWarningCount(),
                run.getSkippedCount(),
                run.getGeneratedInvoiceCount(),
                run.getSubtotalAmount(),
                run.getDiscountAmount(),
                run.getTotalAmount(),
                run.getGeneratedAt(),
                items.stream().map(this::toItemResponse).toList()
        );
    }

    private UtilityBillingRunResponse.Item toItemResponse(UtilityBillingRunItemEntity item) {
        return new UtilityBillingRunResponse.Item(
                item.getId(),
                item.getRoom() == null ? null : item.getRoom().getId(),
                item.getRoom() == null ? null : item.getRoom().getRoomCode(),
                item.getLeaseContract() == null ? null : item.getLeaseContract().getId(),
                item.getLeaseContract() == null ? null : item.getLeaseContract().getContractCode(),
                item.getElectricityReading() == null ? null : item.getElectricityReading().getId(),
                item.getElectricityPrevious(),
                item.getElectricityCurrent(),
                item.getElectricityUsage(),
                item.getElectricityQuantity(),
                item.getElectricityUnitPrice(),
                item.getElectricityAmount(),
                item.getWaterReading() == null ? null : item.getWaterReading().getId(),
                item.getWaterPrevious(),
                item.getWaterCurrent(),
                item.getWaterUsage(),
                item.getWaterQuantity(),
                item.getWaterUnitPrice(),
                item.getWaterAmount(),
                item.getSubtotalAmount(),
                item.getDiscountAmount(),
                item.getTotalAmount(),
                item.getWarningMessage(),
                item.getAdjustmentReason(),
                item.getStatus() == null ? null : item.getStatus().name(),
                item.getInvoice() == null ? null : item.getInvoice().getId(),
                item.getInvoice() == null ? null : item.getInvoice().getInvoiceCode()
        );
    }

    private InvoiceReason parseReason(String value) {
        if (value == null || value.isBlank()) {
            return InvoiceReason.MONTHLY;
        }
        try {
            return InvoiceReason.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid invoice reason.");
        }
    }

    private YearMonth requirePeriod(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Billing period is required.");
        }
        String period = value.trim();
        try {
            return period.contains("/") ? YearMonth.parse(period, LEGACY_PERIOD) : YearMonth.parse(period);
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Billing period must be yyyy-MM or MM/yyyy.");
        }
    }

    private String cleanText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }

    private String valueText(BigDecimal value) {
        return safe(value).stripTrailingZeros().toPlainString();
    }

    private record Charge(
            BigDecimal previous,
            BigDecimal current,
            BigDecimal usage,
            int quantity,
            long unitPrice,
            long amount,
            String warning
    ) {
        static Charge empty() {
            return new Charge(null, null, null, 0, 0L, 0L, null);
        }
    }

    private record UtilityTariffSnapshot(long unitPrice, long freeAllowance) {
    }
}
