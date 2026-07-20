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
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.notification.application.service.BusinessNotificationPublisher;
import com.sep490.hdbhms.occupancy.domain.value_objects.AnomalyType;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.MeterType;
import com.sep490.hdbhms.occupancy.domain.value_objects.ReadingStatus;
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
import java.util.LinkedHashMap;
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
    static final String INVOICE_TARGET = "INVOICE";
    static final String INVOICE_ISSUED_EVENT = "INVOICE_ISSUED";
    static final String UTILITY_BILLING_RUN_TARGET = "UTILITY_BILLING_RUN";
    static final String UTILITY_METER_READING_PERIOD_OPENED_EVENT = "UTILITY_METER_READING_PERIOD_OPENED";
    static final DateTimeFormatter LEGACY_PERIOD = DateTimeFormatter.ofPattern("M/uuuu");
    static final DateTimeFormatter METER_READING_PERIOD = DateTimeFormatter.ofPattern("MM-uuuu");

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
    BusinessNotificationPublisher notificationPublisher;
    SnowflakeIdGenerator snowflakeIdGenerator;
    JdbcTemplate jdbcTemplate;

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
                    UtilityBillingRunResponse run = createPreview(property.getId(), period.toString(), InvoiceReason.MONTHLY.name(), null);
                    publishMeterReadingPeriodOpened(run);
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

    @Transactional(readOnly = true)
    public List<UtilityBillingRunResponse> listRuns(String billingPeriod, Long propertyId, String status) {
        String normalizedPeriod = billingPeriod == null || billingPeriod.isBlank()
                ? null
                : requirePeriod(billingPeriod).toString();
        UtilityBillingRunStatus parsedStatus = parseRunStatus(status);
        List<UtilityBillingRunEntity> runs;
        if (propertyId != null && normalizedPeriod != null) {
            runs = runRepository.findByProperty_IdAndBillingPeriodOrderByIdDesc(propertyId, normalizedPeriod);
        } else if (propertyId != null) {
            runs = runRepository.findByProperty_IdOrderByBillingPeriodDescIdDesc(propertyId);
        } else if (normalizedPeriod != null) {
            runs = runRepository.findByBillingPeriodOrderByProperty_NameAscIdDesc(normalizedPeriod);
        } else {
            runs = runRepository.findAllByOrderByBillingPeriodDescIdDesc();
        }
        return runs.stream()
                .filter(run -> parsedStatus == null || run.getStatus() == parsedStatus)
                .map(run -> toResponse(run, List.of()))
                .toList();
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

        List<UtilityBillingRunItemEntity> items = itemRepository.findByRun_IdOrderByRoom_RoomCodeAscIdAsc(runId);
        long warningCount = items.stream()
                .filter(item -> item.getStatus() == UtilityBillingRunItemStatus.WARNING)
                .count();
        if (warningCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Utility billing batch still has items that need review."
            );
        }

        int invoiceCount = 0;
        LocalDateTime now = LocalDateTime.now();
        for (UtilityBillingRunItemEntity item : items) {
            if (!hasBillableReadings(item)) {
                item.setStatus(UtilityBillingRunItemStatus.SKIPPED);
                itemRepository.save(item);
                continue;
            }

            if (safe(item.getTotalAmount()) > 0) {
                InvoiceEntity invoice = findExistingInvoice(item, run)
                        .orElseGet(() -> createInvoice(item, run, paymentDueDays, now, currentUserId));
                item.setInvoice(invoice);
                item.setStatus(UtilityBillingRunItemStatus.INVOICED);
                invoiceCount++;
                advanceBaseline(item.getElectricityReading(), invoice);
                advanceBaseline(item.getWaterReading(), invoice);
            } else {
                item.setStatus(UtilityBillingRunItemStatus.SKIPPED);
                advanceBaseline(item.getElectricityReading(), null);
                advanceBaseline(item.getWaterReading(), null);
            }
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

    @Transactional
    public UtilityBillingRunResponse publishBatch(Long runId, Integer dueDays, Long currentUserId) {
        return generateInvoices(runId, dueDays, currentUserId);
    }

    @Transactional
    public Long issueTransferInvoiceFromReadings(
            Long contractId,
            Long electricityReadingId,
            Long waterReadingId,
            LocalDate handoverDate,
            Long currentUserId
    ) {
        LocalDate invoiceDate = handoverDate == null ? LocalDate.now() : handoverDate;
        YearMonth period = YearMonth.from(invoiceDate);
        LeaseContractEntity contract = leaseContractRepository.findByIdAndDeletedAtIsNull(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease contract not found."));
        RoomEntity room = contract.getRoom();
        MeterReadingEntity electricity = requireReading(electricityReadingId, room.getId(), MeterType.ELECTRICITY);
        MeterReadingEntity water = requireReading(waterReadingId, room.getId(), MeterType.WATER);

        UtilityBillingRunEntity run = runRepository
                .findByProperty_IdAndBillingPeriodAndInvoiceReason(
                        room.getProperty().getId(),
                        period.toString(),
                        InvoiceReason.TRANSFER
                )
                .orElseGet(() -> UtilityBillingRunEntity.builder()
                        .property(room.getProperty())
                        .billingPeriod(period.toString())
                        .invoiceReason(InvoiceReason.TRANSFER)
                        .createdBy(currentUserId == null ? null : userRepository.getReferenceById(currentUserId))
                        .build());
        run.setStatus(UtilityBillingRunStatus.PREVIEWED);
        run = runRepository.saveAndFlush(run);

        UtilityBillingRunItemEntity existingItem = itemRepository.findByRun_IdAndRoom_Id(run.getId(), room.getId())
                .orElse(null);
        if (existingItem != null
                && existingItem.getInvoice() != null
                && existingItem.getInvoice().getStatus() != InvoiceStatus.VOIDED) {
            return existingItem.getInvoice().getId();
        }
        if (existingItem != null) {
            itemRepository.delete(existingItem);
            itemRepository.flush();
        }

        UtilityBillingRunItemEntity item = itemRepository.saveAndFlush(
                buildItem(run, room, contract, electricity, water, period)
        );
        Long invoiceId = null;
        LocalDateTime now = LocalDateTime.now();
        if (hasBillableReadings(item) && safe(item.getTotalAmount()) > 0) {
            UtilityBillingRunItemEntity invoiceItem = item;
            UtilityBillingRunEntity invoiceRun = run;
            InvoiceEntity invoice = findExistingInvoice(invoiceItem, invoiceRun)
                    .orElseGet(() -> createInvoice(invoiceItem, invoiceRun, 7, now, currentUserId));
            item.setInvoice(invoice);
            item.setStatus(UtilityBillingRunItemStatus.INVOICED);
            invoiceId = invoice.getId();
            advanceBaseline(item.getElectricityReading(), invoice);
            advanceBaseline(item.getWaterReading(), invoice);
        } else {
            item.setStatus(UtilityBillingRunItemStatus.SKIPPED);
            advanceBaseline(item.getElectricityReading(), null);
            advanceBaseline(item.getWaterReading(), null);
        }

        itemRepository.save(item);
        run.setStatus(UtilityBillingRunStatus.INVOICES_CREATED);
        run.setGeneratedBy(currentUserId == null ? null : userRepository.getReferenceById(currentUserId));
        run.setGeneratedAt(now);
        runRepository.save(run);
        syncRunTotals(run.getId());
        return invoiceId;
    }

    private UtilityBillingRunItemEntity buildItem(
            UtilityBillingRunEntity run,
            RoomEntity room,
            YearMonth period
    ) {
        LeaseContractEntity contract = findContract(room.getId(), period);
        Map<MeterType, MeterReadingEntity> readings = findReadings(room.getId(), period);

        MeterReadingEntity electricity = readings.get(MeterType.ELECTRICITY);
        MeterReadingEntity water = readings.get(MeterType.WATER);
        return buildItem(run, room, contract, electricity, water, period);
    }

    private UtilityBillingRunItemEntity buildItem(
            UtilityBillingRunEntity run,
            RoomEntity room,
            LeaseContractEntity contract,
            MeterReadingEntity electricity,
            MeterReadingEntity water,
            YearMonth period
    ) {
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

        boolean canInvoice = contract != null
                && electricity != null
                && water != null
                && electricityCharge.warning() == null
                && waterCharge.warning() == null;
        ServiceFeeCharge serviceFeeCharge = canInvoice
                ? buildServiceFeeCharge(contract, room.getProperty().getId(), period, electricityCharge.amount())
                : ServiceFeeCharge.empty();
        long subtotal = electricityCharge.amount() + waterCharge.amount() + serviceFeeCharge.amount();
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
                .serviceFeeUnitPrice(serviceFeeCharge.unitPrice())
                .serviceFeeAmount(serviceFeeCharge.amount())
                .serviceFeeWaived(serviceFeeCharge.waived())
                .serviceFeeWaiveReason(serviceFeeCharge.waiveReason())
                .serviceFeeLineRequired(serviceFeeCharge.lineRequired())
                .subtotalAmount(subtotal)
                .discountAmount(0L)
                .totalAmount(subtotal)
                .warningMessage(warnings.length() == 0 ? null : warnings.toString())
                .status(status)
                .build();
    }

    private MeterReadingEntity requireReading(Long readingId, Long roomId, MeterType meterType) {
        if (readingId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, meterType + " reading is required.");
        }
        MeterReadingEntity reading = meterReadingRepository.findById(readingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meter reading not found."));
        if (reading.getRoom() == null || !roomId.equals(reading.getRoom().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meter reading does not belong to transfer room.");
        }
        if (reading.getMeter() == null || reading.getMeter().getMeterType() != meterType) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meter reading type is invalid.");
        }
        if (reading.getStatus() == ReadingStatus.VOIDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meter reading has been voided.");
        }
        return reading;
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
        saveServiceFeeLine(invoice, item);

        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(now);
        InvoiceEntity issuedInvoice = invoiceRepository.saveAndFlush(invoice);
        publishInvoiceIssued(issuedInvoice, currentUserId);
        return issuedInvoice;
    }

    private void publishMeterReadingPeriodOpened(UtilityBillingRunResponse run) {
        if (run == null || run.runId() == null) {
            return;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("runId", run.runId());
        data.put("propertyId", run.propertyId());
        data.put("propertyName", run.propertyName());
        data.put("billingPeriod", run.billingPeriod());
        data.put("period", run.billingPeriod());
        data.put("totalRooms", run.totalRooms());
        data.put("readyCount", run.readyCount());
        data.put("warningCount", run.warningCount());
        data.put("skippedCount", run.skippedCount());
        data.put("targetRoute", "/dashboard/meter-readings");

        for (Long recipientId : managerRecipientIds(run.propertyId())) {
            notificationPublisher.publish(
                    UTILITY_METER_READING_PERIOD_OPENED_EVENT,
                    recipientId,
                    UTILITY_BILLING_RUN_TARGET,
                    run.runId(),
                    data
            );
        }
    }

    private void publishInvoiceIssued(InvoiceEntity invoice, Long actorUserId) {
        if (invoice == null || invoice.getId() == null) {
            return;
        }
        List<Long> recipients = findInvoiceTenantRecipientIds(invoice);
        if (recipients.isEmpty()) {
            return;
        }
        Map<String, Object> data = invoiceNotificationData(invoice, actorUserId);
        for (Long recipientId : recipients) {
            notificationPublisher.publish(INVOICE_ISSUED_EVENT, recipientId, INVOICE_TARGET, invoice.getId(), data);
        }
    }

    private void saveServiceFeeLine(InvoiceEntity invoice, UtilityBillingRunItemEntity item) {
        if (!Boolean.TRUE.equals(item.getServiceFeeLineRequired()) && safe(item.getServiceFeeAmount()) <= 0) {
            return;
        }
        String description = item.getServiceFeeWaiveReason() != null && !item.getServiceFeeWaiveReason().isBlank()
                ? item.getServiceFeeWaiveReason()
                : "Service fee " + invoice.getBillingPeriod();
        invoiceLineRepository.save(InvoiceLineEntity.builder()
                .invoice(invoice)
                .lineType(InvoiceLineType.SERVICE_FEE)
                .description(description)
                .quantity(safe(item.getServiceFeeAmount()) > 0 ? 1 : 0)
                .unitPrice(safe(item.getServiceFeeUnitPrice()))
                .sourceType(SOURCE_TYPE)
                .sourceId(item.getId())
                .build());
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

    private Map<MeterType, MeterReadingEntity> findReadings(Long roomId, YearMonth period) {
        List<MeterReadingEntity> readings = meterReadingRepository
                .findLatestActiveByRoomAndPeriod(roomId, period.format(METER_READING_PERIOD));
        if (readings.isEmpty()) {
            readings = meterReadingRepository.findLatestActiveByRoomAndPeriod(roomId, period.toString());
        }
        return readings.stream()
                .collect(Collectors.toMap(
                        reading -> reading.getMeter().getMeterType(),
                        Function.identity(),
                        (first, ignored) -> first
                ));
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

    private ServiceFeeCharge buildServiceFeeCharge(
            LeaseContractEntity contract,
            Long propertyId,
            YearMonth period,
            long electricityAmount
    ) {
        if (contract == null) {
            return ServiceFeeCharge.empty();
        }
        String billingPeriod = period.toString();
        if (hasServiceFeeLineForContractAndPeriod(contract.getId(), billingPeriod)
                || hasServiceFeeSettledByRoomTransfer(contract.getId(), billingPeriod)) {
            return new ServiceFeeCharge(0L, 0L, true, "Service fee already settled in transfer month.", false);
        }

        UtilityTariffSnapshot tariff = readTariff(propertyId, UtilityType.SERVICE_FEE, period.atEndOfMonth());
        Long waiveThreshold = tariff.serviceFeeWaiveElectricityThreshold();
        if (waiveThreshold != null && electricityAmount < waiveThreshold) {
            return new ServiceFeeCharge(
                    tariff.unitPrice(),
                    0L,
                    true,
                    "Service fee waived because electricity amount is below " + waiveThreshold + ".",
                    true
            );
        }
        return new ServiceFeeCharge(tariff.unitPrice(), tariff.unitPrice(), false, null, true);
    }

    private boolean hasServiceFeeLineForContractAndPeriod(Long contractId, String billingPeriod) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM invoices invoice
                JOIN invoice_lines line ON line.invoice_id = invoice.invoice_id
                WHERE invoice.lease_contract_id = ?
                  AND invoice.billing_period = ?
                  AND invoice.status <> 'VOIDED'
                  AND line.line_type = 'SERVICE_FEE'
                """, Integer.class, contractId, billingPeriod);
        return count != null && count > 0;
    }

    private boolean hasServiceFeeSettledByRoomTransfer(Long contractId, String billingPeriod) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM room_transfer_requests request
                JOIN invoices invoice
                  ON invoice.billing_period = ?
                 AND invoice.status <> 'VOIDED'
                JOIN invoice_lines line
                  ON line.invoice_id = invoice.invoice_id
                 AND line.line_type = 'SERVICE_FEE'
                WHERE DATE_FORMAT(request.requested_transfer_date, '%Y-%m') = ?
                  AND (
                        (request.new_contract_id = ? AND invoice.lease_contract_id = request.old_contract_id)
                     OR (request.old_contract_id = ? AND invoice.lease_contract_id = request.new_contract_id)
                  )
                """, Integer.class, billingPeriod, billingPeriod, contractId, contractId);
        return count != null && count > 0;
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
                    case ELECTRICITY -> new UtilityTariffSnapshot(3500L, 0L, null);
                    case WATER -> new UtilityTariffSnapshot(20000L, 6L, null);
                    case SERVICE_FEE -> new UtilityTariffSnapshot(50000L, 0L, 100000L);
                });
    }

    private UtilityTariffSnapshot toSnapshot(UtilityTariffEntity tariff) {
        return new UtilityTariffSnapshot(
                safe(tariff.getUnitPrice()),
                safe(tariff.getFreeAllowance()),
                tariff.getServiceFeeWaiveElectricityThreshold()
        );
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

    private List<Long> managerRecipientIds(Long propertyId) {
        if (propertyId != null) {
            List<Long> managerIds = jdbcTemplate.queryForList("""
                            SELECT staff_user_id
                            FROM property_staff_assignments
                            WHERE property_id = ?
                              AND assignment_status = 'ACTIVE'
                              AND assigned_role = 'MANAGER'
                            ORDER BY is_primary DESC, property_staff_assignment_id ASC
                            """,
                    Long.class,
                    propertyId
            );
            if (!managerIds.isEmpty()) {
                return managerIds;
            }
        }
        return userRepository.findIdsByRolesAndStatus(List.of(Role.OWNER), AccountStatus.ACTIVE);
    }

    private List<Long> findInvoiceTenantRecipientIds(InvoiceEntity invoice) {
        if (invoice == null || invoice.getRoom() == null || invoice.getRoom().getId() == null) {
            return List.of();
        }
        return jdbcTemplate.queryForList("""
                        SELECT DISTINCT u.user_id
                        FROM users u
                        JOIN person_profiles pp
                          ON pp.user_id = u.user_id
                         AND pp.deleted_at IS NULL
                        JOIN (
                            SELECT lc.primary_tenant_profile_id AS tenant_profile_id
                            FROM lease_contracts lc
                            WHERE lc.deleted_at IS NULL
                              AND lc.room_id = ?
                              AND lc.status IN ('SIGNED', 'ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
                            UNION
                            SELECT co.tenant_profile_id AS tenant_profile_id
                            FROM contract_occupants co
                            JOIN lease_contracts lc
                              ON lc.lease_contract_id = co.contract_id
                            WHERE co.status = 'ACTIVE'
                              AND co.tenant_profile_id IS NOT NULL
                              AND lc.deleted_at IS NULL
                              AND lc.room_id = ?
                              AND lc.status IN ('SIGNED', 'ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
                        ) occupied
                          ON occupied.tenant_profile_id = pp.person_profile_id
                        WHERE u.status = 'ACTIVE'
                          AND u.deleted_at IS NULL
                          AND u.role = 'TENANT'
                        ORDER BY u.user_id
                        """,
                Long.class,
                invoice.getRoom().getId(),
                invoice.getRoom().getId()
        );
    }

    private Map<String, Object> invoiceNotificationData(InvoiceEntity invoice, Long actorUserId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("invoiceId", invoice.getId());
        payload.put("invoiceCode", invoice.getInvoiceCode());
        payload.put("invoiceType", invoice.getInvoiceType() == null ? null : invoice.getInvoiceType().name());
        payload.put("billingPeriod", invoice.getBillingPeriod());
        payload.put("period", invoice.getBillingPeriod());
        payload.put("propertyId", invoice.getProperty() == null ? null : invoice.getProperty().getId());
        payload.put("propertyName", invoice.getProperty() == null ? null : invoice.getProperty().getName());
        payload.put("roomId", invoice.getRoom() == null ? null : invoice.getRoom().getId());
        payload.put("roomCode", invoice.getRoom() == null ? null : invoice.getRoom().getRoomCode());
        payload.put("amount", safe(invoice.getTotalAmount()));
        payload.put("totalAmount", safe(invoice.getTotalAmount()));
        payload.put("paidAmount", safe(invoice.getPaidAmount()));
        payload.put("remainingAmount", safe(invoice.getRemainingAmount()));
        payload.put("dueDate", invoice.getDueDate() == null ? null : invoice.getDueDate().toLocalDate().toString());
        payload.put("status", invoice.getStatus() == null ? null : invoice.getStatus().name());
        payload.put("actorUserId", actorUserId);
        payload.put("targetRoute", "/payment");
        return payload;
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
                item.getServiceFeeUnitPrice(),
                item.getServiceFeeAmount(),
                item.getServiceFeeWaived(),
                item.getServiceFeeWaiveReason(),
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

    private UtilityBillingRunStatus parseRunStatus(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value.trim())) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("PREVIEW".equals(normalized)) {
            normalized = UtilityBillingRunStatus.PREVIEWED.name();
        } else if ("PUBLISHED".equals(normalized)) {
            normalized = UtilityBillingRunStatus.INVOICES_CREATED.name();
        }
        try {
            return UtilityBillingRunStatus.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid utility billing batch status.");
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

    private record UtilityTariffSnapshot(long unitPrice, long freeAllowance, Long serviceFeeWaiveElectricityThreshold) {
    }

    private record ServiceFeeCharge(
            long unitPrice,
            long amount,
            boolean waived,
            String waiveReason,
            boolean lineRequired
    ) {
        static ServiceFeeCharge empty() {
            return new ServiceFeeCharge(0L, 0L, false, null, false);
        }
    }
}
