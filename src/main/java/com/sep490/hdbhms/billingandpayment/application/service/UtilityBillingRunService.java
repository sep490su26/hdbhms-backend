package com.sep490.hdbhms.billingandpayment.application.service;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceLineType;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceReason;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceType;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.UtilityBillingRunItemStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.UtilityBillingRunStatus;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceLineEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.RoomUtilityBaselineEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.UtilityBillingRunEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.UtilityBillingRunItemEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaPaymentIntentRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaRoomUtilityBaselineRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaRentOverrideRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaUtilityBillingRunItemRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaUtilityBillingRunRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.UtilityBillingItemAdjustmentRequest;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.UtilityBillingRunResponse;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.notification.application.service.BusinessNotificationPublisher;
import com.sep490.hdbhms.property.domain.value_objects.AnomalyType;
import com.sep490.hdbhms.property.domain.value_objects.AnomalySeverity;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.property.domain.value_objects.MeterType;
import com.sep490.hdbhms.property.domain.value_objects.ReadingStatus;
import com.sep490.hdbhms.property.domain.value_objects.UtilityType;
import com.sep490.hdbhms.property.application.service.MeterUsageCalculator;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.MeterReadingAnomalyEntity;
import com.sep490.hdbhms.property.application.service.MeterReadingAnomalyPolicy;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.MeterReadingBatchEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.MeterReadingEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.UtilityTariffEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterReadingAnomalyRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterReadingBatchRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaUtilityTariffRepository;
import com.sep490.hdbhms.shared.utils.id.SnowflakeIdGenerator;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
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
    static final long DEFAULT_SERVICE_FEE_WAIVE_ELECTRICITY_THRESHOLD =
            MeterReadingAnomalyPolicy.LOW_ELECTRICITY_AMOUNT_THRESHOLD;
    static final String ELECTRICITY_WAIVE_REASON = MeterReadingAnomalyPolicy.LOW_ELECTRICITY_AMOUNT_MESSAGE;
    static final DateTimeFormatter LEGACY_PERIOD = DateTimeFormatter.ofPattern("M/uuuu");
    static final DateTimeFormatter METER_READING_PERIOD = DateTimeFormatter.ofPattern("MM-uuuu");

    JpaPropertyRepository propertyRepository;
    JpaLeaseContractRepository leaseContractRepository;
    JpaMeterReadingRepository meterReadingRepository;
    JpaMeterReadingAnomalyRepository anomalyRepository;
    JpaMeterReadingBatchRepository meterReadingBatchRepository;
    JpaUtilityTariffRepository utilityTariffRepository;
    JpaUtilityBillingRunRepository runRepository;
    JpaUtilityBillingRunItemRepository itemRepository;
    JpaRoomUtilityBaselineRepository baselineRepository;
    MeterUsageCalculator meterUsageCalculator;
    JpaInvoiceRepository invoiceRepository;
    JpaInvoiceLineRepository invoiceLineRepository;
    JpaPaymentIntentRepository paymentIntentRepository;
    JpaRentOverrideRepository rentOverrideRepository;
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
                log.warn("Failed to create a utility billing period for property {}", property.getId(), exception);
            }
        }
        if (created > 0) {
            log.info("Created {} monthly utility billing periods for {}", created, period);
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
                .orElseThrow(() -> new AppException(ApiErrorCode.PROPERTY_NOT_FOUND));

        UtilityBillingRunEntity run = runRepository
                .findByProperty_IdAndBillingPeriodAndInvoiceReason(propertyId, period.toString(), reason)
                .orElseGet(() -> UtilityBillingRunEntity.builder()
                        .property(property)
                        .billingPeriod(period.toString())
                        .invoiceReason(reason)
                        .createdBy(currentUserId == null ? null : userRepository.getReferenceById(currentUserId))
                        .build());

        if (isRunActuallyPublished(run)) {
            markMeterReadingBatchConfirmed(run, currentUserId);
            return getRun(run.getId());
        }

        if (run.getStatus() == UtilityBillingRunStatus.INVOICES_CREATED) {
            run.setStatus(UtilityBillingRunStatus.PREVIEWED);
            run.setGeneratedBy(null);
            run.setGeneratedAt(null);
            run.setGeneratedInvoiceCount(0);
        }
        run.setStatus(UtilityBillingRunStatus.PREVIEWED);
        run = runRepository.saveAndFlush(run);
        itemRepository.deleteByRun_Id(run.getId());
        itemRepository.flush();

        List<RoomEntity> rooms = leaseContractRepository.findMeterReadingRoomsByPeriod(
                propertyId,
                BILLABLE_STATUSES,
                period.atDay(1),
                period.atEndOfMonth()
        );
        if (rooms.isEmpty()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        for (RoomEntity room : rooms) {
            itemRepository.save(buildItem(run, room, period));
        }
        syncRunTotals(run.getId());
        return getRun(run.getId());
    }

    @Transactional(readOnly = true)
    public UtilityBillingRunStatus getMonthlyRunStatus(Long propertyId, String billingPeriod) {
        if (propertyId == null || billingPeriod == null || billingPeriod.isBlank()) {
            return null;
        }
        YearMonth period = requirePeriod(billingPeriod);
        return runRepository.findByProperty_IdAndBillingPeriodAndInvoiceReason(
                        propertyId,
                        period.toString(),
                        InvoiceReason.MONTHLY
                )
                .map(this::effectiveRunStatus)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean hasIssuedMonthlyInvoices(Long propertyId, String billingPeriod) {
        if (propertyId == null || billingPeriod == null || billingPeriod.isBlank()) {
            return false;
        }
        return runRepository.findByProperty_IdAndBillingPeriodAndInvoiceReason(
                        propertyId,
                        requirePeriod(billingPeriod).toString(),
                        InvoiceReason.MONTHLY
                )
                .map(this::isRunActuallyPublished)
                .orElse(false);
    }

    @Transactional
    public void refreshMonthlyPreviewIfOpen(Long propertyId, String billingPeriod, Long currentUserId) {
        UtilityBillingRunStatus status = getMonthlyRunStatus(propertyId, billingPeriod);
        if (status == null || status == UtilityBillingRunStatus.INVOICES_CREATED) {
            return;
        }
        createPreview(propertyId, billingPeriod, InvoiceReason.MONTHLY.name(), currentUserId);
    }

    @Transactional(readOnly = true)
    public UtilityBillingRunResponse getRun(Long runId) {
        UtilityBillingRunEntity run = runRepository.findById(runId)
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
        return toResponse(run, itemRepository.findByRun_IdOrderByRoom_RoomCodeAscIdAsc(runId));
    }

    @Transactional(readOnly = true)
    public List<UtilityBillingRunResponse> listRuns(
            String billingPeriod,
            Long propertyId,
            String status,
            String invoiceReason
    ) {
        String normalizedPeriod = billingPeriod == null || billingPeriod.isBlank()
                ? null
                : requirePeriod(billingPeriod).toString();
        UtilityBillingRunStatus parsedStatus = parseRunStatus(status);
        InvoiceReason parsedReason = invoiceReason == null || invoiceReason.isBlank()
                ? null
                : parseReason(invoiceReason);
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
                .filter(run -> parsedStatus == null || effectiveRunStatus(run) == parsedStatus)
                .filter(run -> parsedReason == null || run.getInvoiceReason() == parsedReason)
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
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
        long discount = request == null || request.discountAmount() == null ? 0L : request.discountAmount();
        long rentSubtotal = calculateRentCharge(
                item.getLeaseContract(), requirePeriod(run.getBillingPeriod())
        ).amount();
        if (discount < 0 || discount > rentSubtotal) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
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
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
        if (run.getStatus() == UtilityBillingRunStatus.CANCELLED) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (isRunActuallyPublished(run)) {
            return getRun(runId);
        }

        if (run.getStatus() == UtilityBillingRunStatus.INVOICES_CREATED) {
            run.setStatus(UtilityBillingRunStatus.PREVIEWED);
            run.setGeneratedBy(null);
            run.setGeneratedAt(null);
            run.setGeneratedInvoiceCount(0);
            runRepository.save(run);
        }

        int paymentDueDays = dueDays == null ? 7 : dueDays;
        if (paymentDueDays <= 0) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        List<UtilityBillingRunItemEntity> items = itemRepository.findByRun_IdOrderByRoom_RoomCodeAscIdAsc(runId);
        long warningCount = items.stream()
                .filter(this::hasBlockingWarning)
                .count();
        if (warningCount > 0) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
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
                if (run.getInvoiceReason() == InvoiceReason.MONTHLY) {
                    MonthlyInvoices monthlyInvoices = createMonthlyInvoices(
                            item,
                            run,
                            paymentDueDays,
                            now,
                            currentUserId
                    );
                    item.setInvoice(monthlyInvoices.primaryInvoice());
                    item.setStatus(UtilityBillingRunItemStatus.INVOICED);
                    invoiceCount += monthlyInvoices.invoiceCount();
                    InvoiceEntity baselineInvoice = monthlyInvoices.utilityInvoice() == null
                            ? monthlyInvoices.primaryInvoice()
                            : monthlyInvoices.utilityInvoice();
                    advanceBaseline(item.getElectricityReading(), baselineInvoice);
                    advanceBaseline(item.getWaterReading(), baselineInvoice);
                } else {
                    RentCharge rentCharge = buildRentCharge(item.getLeaseContract(), requirePeriod(run.getBillingPeriod()));
                    InvoiceType invoiceType = rentCharge.amount() > 0 ? InvoiceType.RENT : InvoiceType.UTILITY;
                    InvoiceEntity invoice = findExistingInvoice(item, run, invoiceType)
                            .orElseGet(() -> createInvoice(
                                    item,
                                    run,
                                    paymentDueDays,
                                    now,
                                    currentUserId,
                                    rentCharge,
                                    invoiceType
                            ));
                    item.setInvoice(invoice);
                    item.setStatus(UtilityBillingRunItemStatus.INVOICED);
                    invoiceCount++;
                    advanceBaseline(item.getElectricityReading(), invoice);
                    advanceBaseline(item.getWaterReading(), invoice);
                }
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
        markMeterReadingBatchConfirmed(run, currentUserId);
        syncRunTotals(runId);
        return getRun(runId);
    }

    private void markMeterReadingBatchConfirmed(UtilityBillingRunEntity run, Long currentUserId) {
        if (run.getInvoiceReason() != InvoiceReason.MONTHLY || run.getProperty() == null) {
            return;
        }

        String readingPeriod = run.getBillingPeriod() == null
                ? null
                : YearMonth.parse(run.getBillingPeriod()).format(METER_READING_PERIOD);
        if (readingPeriod == null) {
            return;
        }

        meterReadingBatchRepository.findByProperty_IdAndReadingPeriod(
                        run.getProperty().getId(),
                        readingPeriod
                )
                .filter(batch -> batch.getStatus() != com.sep490.hdbhms.property.domain.value_objects.BatchStatus.CANCELLED)
                .ifPresent(batch -> {
                    batch.setStatus(com.sep490.hdbhms.property.domain.value_objects.BatchStatus.CONFIRMED);
                    if (batch.getConfirmedAt() == null) {
                        batch.setConfirmedAt(LocalDateTime.now());
                    }
                    if (currentUserId != null) {
                        batch.setConfirmedBy(userRepository.getReferenceById(currentUserId));
                    }
                    meterReadingBatchRepository.save(batch);
                });
    }

    @Transactional
    public UtilityBillingRunResponse publishBatch(Long runId, Integer dueDays, Long currentUserId) {
        return generateInvoices(runId, dueDays, currentUserId);
    }

    @Transactional
    public Long issueTransferInvoiceFromReadings(
            Long contractId,
            Long electricityReadingId,
            LocalDate handoverDate,
            Long currentUserId
    ) {
        LocalDate invoiceDate = handoverDate == null ? LocalDate.now() : handoverDate;
        YearMonth period = YearMonth.from(invoiceDate);
        LeaseContractEntity contract = leaseContractRepository.findByIdAndDeletedAtIsNull(contractId)
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
        RoomEntity room = contract.getRoom();
        MeterReadingEntity electricity = requireReading(electricityReadingId, room.getId(), MeterType.ELECTRICITY);

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
                buildItem(run, room, contract, electricity, null, period)
        );
        if (hasBlockingWarning(item)) {
            item.setStatus(UtilityBillingRunItemStatus.WARNING);
            itemRepository.save(item);
            syncRunTotals(run.getId());
            throw new AppException(ApiErrorCode.ROOM_TRANSFER_METER_READING_INVALID);
        }

        Long invoiceId = null;
        LocalDateTime now = LocalDateTime.now();
        if (hasBillableReadings(item) && safe(item.getTotalAmount()) > 0) {
            UtilityBillingRunItemEntity invoiceItem = item;
            UtilityBillingRunEntity invoiceRun = run;
            RentCharge rentCharge = buildRentCharge(invoiceItem.getLeaseContract(), period);
            InvoiceType invoiceType = rentCharge.amount() > 0 ? InvoiceType.RENT : InvoiceType.UTILITY;
            InvoiceEntity invoice = findExistingInvoice(invoiceItem, invoiceRun, invoiceType)
                    .orElseGet(() -> createInvoice(
                            invoiceItem,
                            invoiceRun,
                            7,
                            now,
                            currentUserId,
                            rentCharge,
                            invoiceType
                    ));
            item.setInvoice(invoice);
            item.setStatus(UtilityBillingRunItemStatus.INVOICED);
            invoiceId = invoice.getId();
            advanceBaseline(item.getElectricityReading(), invoice);
        } else {
            item.setStatus(UtilityBillingRunItemStatus.SKIPPED);
            advanceBaseline(item.getElectricityReading(), null);
        }

        itemRepository.save(item);
        run.setStatus(UtilityBillingRunStatus.INVOICES_CREATED);
        run.setGeneratedBy(currentUserId == null ? null : userRepository.getReferenceById(currentUserId));
        run.setGeneratedAt(now);
        runRepository.save(run);
        syncRunTotals(run.getId());
        return invoiceId;
    }

    /** Applies an approved tenant meter correction to the reading and its generated billing data. */
    @Transactional
    public void applyMeterReadingCorrection(
            Long meterReadingId,
            Long invoiceId,
            Long invoiceLineId,
            BigDecimal correctedCurrentValue
    ) {
        if (meterReadingId == null || correctedCurrentValue == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST_PAYLOAD);
        }

        MeterReadingEntity reading = meterReadingRepository.findById(meterReadingId)
                .orElseThrow(() -> new AppException(ApiErrorCode.METER_READING_NOT_FOUND));
        BigDecimal currentValue = correctedCurrentValue.setScale(3, RoundingMode.HALF_UP);
        if (currentValue.signum() < 0
                || reading.getPreviousValue() == null
                || currentValue.compareTo(reading.getPreviousValue()) < 0) {
            throw new AppException(ApiErrorCode.BILLING_METER_READING_PROPOSAL_BELOW_PREVIOUS);
        }

        InvoiceLineEntity line = invoiceLineRepository.findByMeterReading_IdOrderByIdDesc(meterReadingId)
                .stream()
                .filter(candidate -> invoiceLineId == null || invoiceLineId.equals(candidate.getId()))
                .filter(candidate -> invoiceId == null
                        || candidate.getInvoice() != null && invoiceId.equals(candidate.getInvoice().getId()))
                .filter(candidate -> candidate.getLineType() == InvoiceLineType.ELECTRICITY
                        || candidate.getLineType() == InvoiceLineType.WATER)
                .findFirst()
                .orElseThrow(() -> new AppException(ApiErrorCode.BILLING_INVOICE_LINE_NOT_FOUND));
        InvoiceEntity invoice = line.getInvoice();
        if (invoice == null || invoice.getStatus() == null || invoice.getStatus() == InvoiceStatus.VOIDED
                || invoice.getStatus() == InvoiceStatus.PAID || safe(invoice.getPaidAmount()) > 0L) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST_STATE);
        }

        UtilityType utilityType = line.getLineType() == InvoiceLineType.WATER
                ? UtilityType.WATER
                : UtilityType.ELECTRICITY;
        BigDecimal counterCapacity = counterCapacity(reading);
        MeterUsageCalculator.Calculation calculation = meterUsageCalculator.calculate(
                reading.getPreviousValue(),
                currentValue,
                counterCapacity,
                reading.getRolloverCount()
        );
        if (!calculation.valid()) {
            throw new AppException(ApiErrorCode.INVALID_METER_READING_VALUE);
        }
        UtilityTariffSnapshot tariff = readTariff(
                reading.getRoom().getProperty().getId(),
                utilityType,
                reading.getReadingDate()
        );
        int quantity = billableQuantity(calculation.usage(), tariff.freeAllowance());
        long unitPrice = line.getUnitPrice() == null || line.getUnitPrice() <= 0L
                ? tariff.unitPrice()
                : line.getUnitPrice();
        long amount = (long) quantity * unitPrice;

        InvoiceStatus originalStatus = invoice.getStatus();
        if (originalStatus != InvoiceStatus.DRAFT) {
            invoice.setStatus(InvoiceStatus.DRAFT);
            invoiceRepository.saveAndFlush(invoice);
        }

        reading.setCurrentValue(currentValue);
        meterReadingRepository.saveAndFlush(reading);

        line.setQuantity(quantity);
        line.setUnitPrice(unitPrice);
        line.setDescription("%s %s: %s -> %s".formatted(
                utilityType == UtilityType.WATER ? "Water" : "Electricity",
                invoice.getBillingPeriod(),
                valueText(reading.getPreviousValue()),
                valueText(currentValue)
        ));
        invoiceLineRepository.saveAndFlush(line);

        updateBillingRunItems(
                reading,
                line.getLineType(),
                calculation.usage(),
                quantity,
                unitPrice,
                amount
        );

        long subtotal = invoiceLineRepository.findByInvoice_IdOrderByIdAsc(invoice.getId())
                .stream()
                .mapToLong(this::persistedLineAmount)
                .sum();
        long discount = Math.max(safe(invoice.getDiscountAmount()), 0L);
        long total = Math.max(subtotal - discount, 0L);
        long paid = safe(invoice.getPaidAmount());
        if (paid > total) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        invoice.setSubtotalAmount(subtotal);
        invoice.setTotalAmount(total);
        invoice.setRemainingAmount(total - paid);
        invoice.setStatus(resolveCorrectedInvoiceStatus(originalStatus, invoice, total, paid));
        invoiceRepository.saveAndFlush(invoice);
        cancelPendingPaymentIntents(invoice);
        updateBaselineIfInvoiceIsLatest(reading, invoice);
    }

    private void updateBillingRunItems(
            MeterReadingEntity reading,
            InvoiceLineType lineType,
            BigDecimal usage,
            int quantity,
            long unitPrice,
            long amount
    ) {
        List<UtilityBillingRunItemEntity> items = lineType == InvoiceLineType.WATER
                ? itemRepository.findByWaterReading_Id(reading.getId())
                : itemRepository.findByElectricityReading_Id(reading.getId());
        for (UtilityBillingRunItemEntity item : items) {
            long previousAmount;
            if (lineType == InvoiceLineType.WATER) {
                previousAmount = safe(item.getWaterAmount());
                item.setWaterPrevious(reading.getPreviousValue());
                item.setWaterCurrent(reading.getCurrentValue());
                item.setWaterUsage(usage);
                item.setWaterQuantity(quantity);
                item.setWaterUnitPrice(unitPrice);
                item.setWaterAmount(amount);
            } else {
                previousAmount = safe(item.getElectricityAmount());
                item.setElectricityPrevious(reading.getPreviousValue());
                item.setElectricityCurrent(reading.getCurrentValue());
                item.setElectricityUsage(usage);
                item.setElectricityQuantity(quantity);
                item.setElectricityUnitPrice(unitPrice);
                item.setElectricityAmount(amount);
            }
            long subtotal = Math.max(safe(item.getSubtotalAmount()) - previousAmount + amount, 0L);
            long total = Math.max(subtotal - safe(item.getDiscountAmount()), 0L);
            item.setSubtotalAmount(subtotal);
            item.setTotalAmount(total);
            itemRepository.save(item);
            if (item.getRun() != null && item.getRun().getId() != null) {
                syncRunTotals(item.getRun().getId());
            }
        }
    }

    private InvoiceStatus resolveCorrectedInvoiceStatus(
            InvoiceStatus originalStatus,
            InvoiceEntity invoice,
            long total,
            long paid
    ) {
        if (originalStatus == InvoiceStatus.DRAFT) {
            return InvoiceStatus.DRAFT;
        }
        if (paid > 0L && paid == total) {
            return InvoiceStatus.PAID;
        }
        return invoice.getDueDate() != null && invoice.getDueDate().isBefore(LocalDateTime.now())
                ? InvoiceStatus.OVERDUE
                : InvoiceStatus.ISSUED;
    }

    private void cancelPendingPaymentIntents(InvoiceEntity invoice) {
        paymentIntentRepository.findByInvoice_IdAndStatusIn(
                        invoice.getId(),
                        List.of(PaymentIntentStatus.CREATED, PaymentIntentStatus.PENDING)
                )
                .forEach(paymentIntent -> {
                    paymentIntent.setStatus(PaymentIntentStatus.CANCELLED);
                    paymentIntentRepository.save(paymentIntent);
                });
    }

    private void updateBaselineIfInvoiceIsLatest(MeterReadingEntity reading, InvoiceEntity invoice) {
        baselineRepository.findByMeter_Id(reading.getMeter().getId()).ifPresent(baseline -> {
            Long lastInvoiceId = baseline.getLastInvoice() == null ? null : baseline.getLastInvoice().getId();
            if (lastInvoiceId == null || lastInvoiceId.equals(invoice.getId())) {
                baseline.setLastBilledReading(reading.getCurrentValue());
                baseline.setLastInvoice(invoice);
                baselineRepository.save(baseline);
            }
        });
    }

    private BigDecimal counterCapacity(MeterReadingEntity reading) {
        if (reading.getRolloverCount() != null
                && reading.getRolloverCount() > 0
                && reading.getCounterCapacitySnapshot() != null
                && reading.getCounterCapacitySnapshot().signum() > 0) {
            return reading.getCounterCapacitySnapshot();
        }
        return reading.getMeter() == null ? BigDecimal.ZERO : reading.getMeter().getCounterCapacity();
    }

    private long persistedLineAmount(InvoiceLineEntity line) {
        return (long) (line.getQuantity() == null ? 0 : line.getQuantity()) * safe(line.getUnitPrice());
    }

    private UtilityBillingRunItemEntity buildItem(
            UtilityBillingRunEntity run,
            RoomEntity room,
            YearMonth period
    ) {
        LeaseContractEntity contract = findContract(room.getId(), period);
        Map<MeterType, MeterReadingEntity> readings = findReadings(room.getId(), period);

        MeterReadingEntity electricity = readings.get(MeterType.ELECTRICITY);
        if (run.getInvoiceReason() == InvoiceReason.MONTHLY) {
            ensureLowElectricityAnomaly(electricity);
        }
        return buildItem(run, room, contract, electricity, null, period);
    }

    private void ensureLowElectricityAnomaly(MeterReadingEntity electricity) {
        if (electricity == null || electricity.getBatch() == null) {
            return;
        }

        Charge charge = buildCharge(electricity, UtilityType.ELECTRICITY);
        if (!charge.waived()) {
            return;
        }

        boolean alreadyDetected = anomalyRepository
                .findFirstByMeterReading_IdAndAnomalyTypeOrderByIdDesc(
                        electricity.getId(),
                        AnomalyType.OTHER
                )
                .map(anomaly -> ELECTRICITY_WAIVE_REASON.equals(anomaly.getMessage()))
                .orElse(false);
        if (alreadyDetected) {
            return;
        }

        anomalyRepository.saveAndFlush(MeterReadingAnomalyEntity.builder()
                .batch(electricity.getBatch())
                .meterReading(electricity)
                .anomalyType(AnomalyType.OTHER)
                .severity(AnomalySeverity.MEDIUM)
                .message(ELECTRICITY_WAIVE_REASON)
                .build());
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
        StringJoiner warnings = new StringJoiner("; ");
        if (contract == null) warnings.add("Không có hợp đồng đủ điều kiện tính tiền trong kỳ này");
        if (electricity == null || electricity.getCurrentValue() == null) warnings.add("Thiếu chỉ số điện");
        if (electricityCharge.warning() != null) warnings.add(electricityCharge.warning());
        if (anomalyMessage != null && !anomalyMessage.isBlank()) warnings.add(anomalyMessage);
        boolean canInvoice = contract != null
                && electricity != null
                && electricity.getCurrentValue() != null
                && electricityCharge.warning() == null;
        RentCharge rentCharge = canInvoice
                ? buildRentCharge(contract, period)
                : RentCharge.empty();
        ServiceFeeCharge serviceFeeCharge = canInvoice
                ? buildServiceFeeCharge(
                contract,
                room.getProperty().getId(),
                period,
                electricityCharge.calculatedAmount()
        )
                : ServiceFeeCharge.empty();
        long subtotal = electricityCharge.amount() + rentCharge.amount() + serviceFeeCharge.amount();
        UtilityBillingRunItemStatus status = resolveItemStatus(
                warnings.length() > 0,
                canInvoice,
                subtotal
        );

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
                .electricityWaived(electricityCharge.waived())
                .electricityWaiveReason(electricityCharge.waiveReason())
                .waterPrevious(null)
                .waterCurrent(null)
                .waterUsage(null)
                .waterQuantity(0)
                .waterUnitPrice(0L)
                .waterAmount(0L)
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
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        MeterReadingEntity reading = meterReadingRepository.findById(readingId)
                .orElseThrow(() -> new AppException(ApiErrorCode.METER_READING_NOT_FOUND));
        if (reading.getRoom() == null || !roomId.equals(reading.getRoom().getId())) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (reading.getMeter() == null || reading.getMeter().getMeterType() != meterType) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (reading.getStatus() == ReadingStatus.VOIDED) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
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
        BigDecimal counterCapacity = reading.getRolloverCount() != null
                && reading.getRolloverCount() > 0
                && reading.getCounterCapacitySnapshot() != null
                && reading.getCounterCapacitySnapshot().signum() > 0
                ? reading.getCounterCapacitySnapshot()
                : reading.getMeter().getCounterCapacity();
        MeterUsageCalculator.Calculation calculation = meterUsageCalculator.calculate(
                previous,
                current,
                counterCapacity,
                reading.getRolloverCount()
        );
        BigDecimal usage = calculation.usage();
        if (!calculation.valid()) {
            return new Charge(previous, current, usage, 0, 0L, 0L, "Chỉ số mới thấp hơn chỉ số gốc tính tiền");
        }

        UtilityTariffSnapshot tariff = readTariff(
                reading.getRoom().getProperty().getId(),
                utilityType,
                reading.getReadingDate()
        );
        int quantity = billableQuantity(usage, tariff.freeAllowance());
        long calculatedAmount = (long) quantity * tariff.unitPrice();
        UtilityTariffSnapshot serviceFeeTariff = readTariff(
                reading.getRoom().getProperty().getId(),
                UtilityType.SERVICE_FEE,
                reading.getReadingDate()
        );
        long threshold = serviceFeeTariff.serviceFeeWaiveElectricityThreshold() == null
                ? DEFAULT_SERVICE_FEE_WAIVE_ELECTRICITY_THRESHOLD
                : serviceFeeTariff.serviceFeeWaiveElectricityThreshold();
        boolean waived = utilityType == UtilityType.ELECTRICITY && calculatedAmount < threshold;
        return new Charge(
                previous,
                current,
                usage,
                waived ? 0 : quantity,
                tariff.unitPrice(),
                waived ? 0L : calculatedAmount,
                calculatedAmount,
                waived,
                waived ? ELECTRICITY_WAIVE_REASON : null,
                null
        );
    }

    private InvoiceEntity createInvoice(
            UtilityBillingRunItemEntity item,
            UtilityBillingRunEntity run,
            int dueDays,
            LocalDateTime now,
            Long currentUserId,
            RentCharge rentCharge,
            InvoiceType invoiceType
    ) {
        InvoiceEntity invoice = createInvoiceHeader(
                item,
                run,
                dueDays,
                now,
                currentUserId,
                invoiceType,
                safe(item.getSubtotalAmount()),
                invoiceType == InvoiceType.RENT
                        ? Math.min(
                        Math.max(
                                safe(item.getDiscountAmount()) > 0
                                        ? safe(item.getDiscountAmount())
                                        : configuredRentDiscount(
                                        item.getLeaseContract(),
                                        requirePeriod(run.getBillingPeriod()),
                                        rentCharge
                                ),
                                0L
                        ), rentCharge.amount())
                        : 0L
        );

        saveRentLine(invoice, rentCharge, item.getId());
        saveInvoiceLine(invoice, item, InvoiceLineType.ELECTRICITY, item.getElectricityReading(),
                item.getElectricityQuantity(), item.getElectricityUnitPrice(), "Electricity");
        saveServiceFeeLine(invoice, item, run.getBillingPeriod());

        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(now);
        InvoiceEntity issuedInvoice = invoiceRepository.saveAndFlush(invoice);
        publishInvoiceIssued(issuedInvoice, currentUserId);
        return issuedInvoice;
    }

    private MonthlyInvoices createMonthlyInvoices(
            UtilityBillingRunItemEntity item,
            UtilityBillingRunEntity run,
            int dueDays,
            LocalDateTime now,
            Long currentUserId
    ) {
        YearMonth billingPeriod = requirePeriod(run.getBillingPeriod());
        RentCharge rentCharge = buildRentCharge(item.getLeaseContract(), billingPeriod);
        long rentSubtotal = rentCharge.amount();
        long utilitySubtotal = utilitySubtotal(item);
        long serviceFeeSubtotal = safe(item.getServiceFeeAmount());
        long configuredRentDiscount = configuredRentDiscount(item.getLeaseContract(), billingPeriod, rentCharge);
        long requestedDiscount = Math.max(safe(item.getDiscountAmount()), 0L);
        long rentDiscount = Math.min(
                requestedDiscount > 0 ? requestedDiscount : configuredRentDiscount,
                rentSubtotal
        );

        InvoiceEntity rentInvoice = findExistingInvoice(item, run, InvoiceType.RENT).orElse(null);
        if (rentInvoice == null && rentSubtotal + serviceFeeSubtotal > 0) {
            rentInvoice = createMonthlyRentInvoice(
                    item,
                    run,
                    dueDays,
                    now,
                    currentUserId,
                    rentCharge,
                    rentDiscount
            );
        }

        InvoiceEntity utilityInvoice = findExistingInvoice(item, run, InvoiceType.UTILITY).orElse(null);
        if (utilityInvoice == null && utilitySubtotal > 0) {
            utilityInvoice = createMonthlyUtilityInvoice(
                    item,
                    run,
                    dueDays,
                    now,
                    currentUserId,
                    utilitySubtotal,
                    0L
            );
        }

        InvoiceEntity primaryInvoice = rentInvoice == null ? utilityInvoice : rentInvoice;
        return new MonthlyInvoices(
                primaryInvoice,
                utilityInvoice,
                (rentInvoice == null ? 0 : 1) + (utilityInvoice == null ? 0 : 1)
        );
    }

    private InvoiceEntity createMonthlyRentInvoice(
            UtilityBillingRunItemEntity item,
            UtilityBillingRunEntity run,
            int dueDays,
            LocalDateTime now,
            Long currentUserId,
            RentCharge rentCharge,
            long discountAmount
    ) {
        InvoiceEntity invoice = createInvoiceHeader(
                item,
                run,
                dueDays,
                now,
                currentUserId,
                InvoiceType.RENT,
                rentCharge.amount() + safe(item.getServiceFeeAmount()),
                discountAmount
        );
        saveRentLine(invoice, rentCharge, item.getId());
        saveServiceFeeLine(invoice, item, run.getBillingPeriod());
        return issueInvoice(invoice, now, currentUserId);
    }

    private InvoiceEntity createMonthlyUtilityInvoice(
            UtilityBillingRunItemEntity item,
            UtilityBillingRunEntity run,
            int dueDays,
            LocalDateTime now,
            Long currentUserId,
            long subtotalAmount,
            long discountAmount
    ) {
        InvoiceEntity invoice = createInvoiceHeader(
                item,
                run,
                dueDays,
                now,
                currentUserId,
                InvoiceType.UTILITY,
                subtotalAmount,
                discountAmount
        );
        saveInvoiceLine(invoice, item, InvoiceLineType.ELECTRICITY, item.getElectricityReading(),
                item.getElectricityQuantity(), item.getElectricityUnitPrice(), "Electricity");
        return issueInvoice(invoice, now, currentUserId);
    }

    private InvoiceEntity createInvoiceHeader(
            UtilityBillingRunItemEntity item,
            UtilityBillingRunEntity run,
            int dueDays,
            LocalDateTime now,
            Long currentUserId,
            InvoiceType invoiceType,
            long subtotalAmount,
            long discountAmount
    ) {
        long normalizedSubtotal = Math.max(subtotalAmount, 0L);
        long normalizedDiscount = Math.min(Math.max(discountAmount, 0L), normalizedSubtotal);
        long totalAmount = normalizedSubtotal - normalizedDiscount;
        return invoiceRepository.saveAndFlush(InvoiceEntity.builder()
                .invoiceCode("INV-" + (invoiceType == InvoiceType.RENT ? "RENT" : "UTL") + "-"
                        + run.getInvoiceReason().name() + "-" + item.getRoom().getId()
                        + "-" + run.getBillingPeriod().replace("-", "") + "-" + snowflakeIdGenerator.next())
                .property(run.getProperty())
                .room(item.getRoom())
                .leastContract(item.getLeaseContract())
                .invoiceType(invoiceType)
                .invoiceReason(run.getInvoiceReason())
                .revisionNo(nextRevision(item.getLeaseContract().getId(), run.getBillingPeriod(), invoiceType))
                .billingPeriod(run.getBillingPeriod())
                .issueDate(now)
                .dueDate(now.plusDays(dueDays))
                .status(InvoiceStatus.DRAFT)
                .subtotalAmount(normalizedSubtotal)
                .discountAmount(invoiceType == InvoiceType.RENT ? normalizedDiscount : 0L)
                .totalAmount(totalAmount)
                .paidAmount(0L)
                .remainingAmount(totalAmount)
                .createdBy(currentUserId == null ? null : userRepository.getReferenceById(currentUserId))
                .build());
    }

    private InvoiceEntity issueInvoice(InvoiceEntity invoice, LocalDateTime now, Long currentUserId) {
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(now);
        InvoiceEntity issuedInvoice = invoiceRepository.saveAndFlush(invoice);
        publishInvoiceIssued(issuedInvoice, currentUserId);
        return issuedInvoice;
    }

    private long utilitySubtotal(UtilityBillingRunItemEntity item) {
        return safe(item.getElectricityAmount());
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

    private void saveRentLine(InvoiceEntity invoice, RentCharge rentCharge, Long sourceId) {
        if (rentCharge == null || rentCharge.amount() <= 0) {
            return;
        }
        invoiceLineRepository.save(InvoiceLineEntity.builder()
                .invoice(invoice)
                .lineType(InvoiceLineType.ROOM_RENT)
                .description("Ti\u1ec1n ph\u00f2ng c\u00e1c k\u1ef3: " + periodList(rentCharge.periods()))
                .quantity(rentCharge.months())
                .unitPrice(rentCharge.monthlyAmount())
                .sourceType(SOURCE_TYPE)
                .sourceId(sourceId)
                .build());
    }

    private void saveServiceFeeLine(InvoiceEntity invoice, UtilityBillingRunItemEntity item, String billingPeriod) {
        if (!Boolean.TRUE.equals(item.getServiceFeeLineRequired()) && safe(item.getServiceFeeAmount()) <= 0) {
            return;
        }
        String description = item.getServiceFeeWaiveReason() != null && !item.getServiceFeeWaiveReason().isBlank()
                ? item.getServiceFeeWaiveReason()
                : "Phí dịch vụ " + invoice.getBillingPeriod();
        if (item.getServiceFeeWaiveReason() == null || item.getServiceFeeWaiveReason().isBlank()) {
            YearMonth period = requirePeriod(billingPeriod);
            int cycleMonths = paymentCycleMonths(invoice.getLeastContract());
            description = "Ph\u00ed d\u1ecbch v\u1ee5 c\u00e1c k\u1ef3: " + periodList(period, cycleMonths);
        }
        invoiceLineRepository.save(InvoiceLineEntity.builder()
                .invoice(invoice)
                .lineType(InvoiceLineType.SERVICE_FEE)
                .description(description)
                .quantity(serviceFeeQuantity(item))
                .unitPrice(safe(item.getServiceFeeUnitPrice()))
                .sourceType(SOURCE_TYPE)
                .sourceId(item.getId())
                .build());
    }

    private int serviceFeeQuantity(UtilityBillingRunItemEntity item) {
        long amount = safe(item.getServiceFeeAmount());
        long unitPrice = safe(item.getServiceFeeUnitPrice());
        if (amount <= 0 || unitPrice <= 0) {
            return 0;
        }
        return Math.toIntExact((amount + unitPrice - 1) / unitPrice);
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

    private Optional<InvoiceEntity> findExistingInvoice(
            UtilityBillingRunItemEntity item,
            UtilityBillingRunEntity run,
            InvoiceType invoiceType
    ) {
        if (item.getLeaseContract() == null) {
            return Optional.empty();
        }
        return invoiceRepository.findFirstByLeastContract_IdAndBillingPeriodAndInvoiceTypeAndInvoiceReasonAndStatusNotOrderByIdDesc(
                item.getLeaseContract().getId(),
                run.getBillingPeriod(),
                invoiceType,
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
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
        List<UtilityBillingRunItemEntity> items = itemRepository.findByRun_IdOrderByRoom_RoomCodeAscIdAsc(runId);
        run.setTotalRooms(items.size());
        run.setReadyCount((int) items.stream().filter(item -> item.getStatus() == UtilityBillingRunItemStatus.READY).count());
        run.setWarningCount((int) items.stream().filter(item -> item.getStatus() == UtilityBillingRunItemStatus.WARNING).count());
        run.setSkippedCount((int) items.stream().filter(item -> item.getStatus() == UtilityBillingRunItemStatus.SKIPPED).count());
        run.setGeneratedInvoiceCount(run.getStatus() == UtilityBillingRunStatus.INVOICES_CREATED
                ? countIssuedInvoices(run)
                : (int) items.stream().filter(item -> item.getStatus() == UtilityBillingRunItemStatus.INVOICED).count());
        run.setSubtotalAmount(items.stream().mapToLong(item -> safe(item.getSubtotalAmount())).sum());
        run.setDiscountAmount(items.stream().mapToLong(item -> safe(item.getDiscountAmount())).sum());
        run.setTotalAmount(items.stream().mapToLong(item -> safe(item.getTotalAmount())).sum());
        runRepository.save(run);
    }

    private int countIssuedInvoices(UtilityBillingRunEntity run) {
        if (run == null || run.getId() == null || run.getInvoiceReason() == null) {
            return 0;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT invoice.invoice_id)
                FROM invoices invoice
                JOIN utility_billing_run_items item
                  ON item.lease_contract_id = invoice.lease_contract_id
                WHERE item.run_id = ?
                  AND invoice.billing_period = ?
                  AND invoice.invoice_reason = ?
                  AND invoice.status <> 'VOIDED'
                """, Integer.class, run.getId(), run.getBillingPeriod(), run.getInvoiceReason().name());
        return count == null ? 0 : count;
    }

    private UtilityBillingRunEntity requireEditableRun(Long runId) {
        UtilityBillingRunEntity run = runRepository.findById(runId)
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
        if (isRunActuallyPublished(run) || run.getStatus() == UtilityBillingRunStatus.CANCELLED) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        return run;
    }

    private boolean hasBlockingWarning(UtilityBillingRunItemEntity item) {
        return item != null
                && (item.getStatus() == UtilityBillingRunItemStatus.WARNING
                || (item.getWarningMessage() != null && !item.getWarningMessage().isBlank()));
    }

    private boolean hasBillableReadings(UtilityBillingRunItemEntity item) {
        return item.getLeaseContract() != null
                && item.getElectricityReading() != null
                && item.getElectricityCurrent() != null
                && notNegative(item.getElectricityUsage());
    }

    private boolean hasIssuedInvoices(Long runId) {
        if (runId == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM utility_billing_run_items item
                JOIN invoices invoice ON invoice.invoice_id = item.invoice_id
                WHERE item.run_id = ?
                  AND invoice.status <> 'VOIDED'
                """, Integer.class, runId);
        return count != null && count > 0;
    }

    private boolean hasBlockingWarnings(Long runId) {
        if (runId == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM utility_billing_run_items
                WHERE run_id = ?
                  AND (
                        status = 'WARNING'
                     OR (warning_message IS NOT NULL AND TRIM(warning_message) <> '')
                  )
                """, Integer.class, runId);
        return count != null && count > 0;
    }

    private boolean isRunActuallyPublished(UtilityBillingRunEntity run) {
        if (run == null || run.getStatus() != UtilityBillingRunStatus.INVOICES_CREATED) {
            return false;
        }
        return hasIssuedInvoices(run.getId()) || !hasBlockingWarnings(run.getId());
    }

    private UtilityBillingRunStatus effectiveRunStatus(UtilityBillingRunEntity run) {
        if (run == null || run.getStatus() != UtilityBillingRunStatus.INVOICES_CREATED) {
            return run == null ? null : run.getStatus();
        }
        return isRunActuallyPublished(run)
                ? UtilityBillingRunStatus.INVOICES_CREATED
                : UtilityBillingRunStatus.PREVIEWED;
    }

    private RentCharge buildRentCharge(LeaseContractEntity contract, YearMonth period) {
        RentCharge charge = calculateRentCharge(contract, period);
        if (charge.amount() <= 0 || hasRoomRentLineForContractAndPeriod(contract.getId(), period.toString())) {
            return RentCharge.empty();
        }
        return charge;
    }

    private RentCharge calculateRentCharge(LeaseContractEntity contract, YearMonth period) {
        if (contract == null || period == null) {
            return RentCharge.empty();
        }
        LocalDate rentStartDate = contract.getRentStartDate() != null
                ? contract.getRentStartDate()
                : contract.getStartDate();
        int cycleMonths = paymentCycleMonths(contract);
        if (rentStartDate == null
                || !isPaymentCycleStart(period, YearMonth.from(rentStartDate), cycleMonths)) {
            return RentCharge.empty();
        }
        long monthlyRent = safe(contract.getMonthlyRent());
        return monthlyRent <= 0
                ? RentCharge.empty()
                : new RentCharge(
                        monthlyRent,
                        monthlyRent * cycleMonths,
                        cycleMonths,
                        periods(period, cycleMonths)
                );
    }

    private long configuredRentDiscount(
            LeaseContractEntity contract,
            YearMonth period,
            RentCharge rentCharge
    ) {
        if (contract == null || contract.getId() == null || period == null || rentCharge == null || rentCharge.amount() <= 0) {
            return 0L;
        }
        return rentOverrideRepository.findByContract_IdAndBillingPeriod(contract.getId(), period.toString())
                .map(override -> {
                    long explicitDiscount = safe(override.getDiscountAmount());
                    if (explicitDiscount > 0) {
                        return Math.min(explicitDiscount, rentCharge.amount());
                    }
                    long legacyMonthlyDiscount = Math.max(
                            safe(contract.getMonthlyRent()) - safe(override.getOverrideMonthlyRent()),
                            0L
                    );
                    return Math.min(
                            legacyMonthlyDiscount * Math.max(rentCharge.months(), 1),
                            rentCharge.amount()
                    );
                })
                .orElse(0L);
    }

    private int paymentCycleMonths(LeaseContractEntity contract) {
        return contract == null || contract.getPaymentCycleMonths() == null
                ? 1
                : Math.max(contract.getPaymentCycleMonths(), 1);
    }

    private boolean hasRoomRentLineForContractAndPeriod(Long contractId, String billingPeriod) {
        if (contractId == null || billingPeriod == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM invoices invoice
                JOIN invoice_lines line ON line.invoice_id = invoice.invoice_id
                WHERE invoice.lease_contract_id = ?
                  AND invoice.billing_period = ?
                  AND invoice.status <> 'VOIDED'
                  AND line.line_type = 'ROOM_RENT'
                """, Integer.class, contractId, billingPeriod);
        return count != null && count > 0;
    }

    private boolean isPaymentCycleStart(YearMonth billingPeriod, YearMonth chargeStartPeriod, int cycleMonths) {
        return isServiceFeeDue(billingPeriod, chargeStartPeriod, cycleMonths);
    }

    private String periodList(YearMonth start, int months) {
        return periodList(periods(start, months));
    }

    private List<YearMonth> periods(YearMonth start, int months) {
        List<YearMonth> periods = new ArrayList<>();
        for (int index = 0; index < Math.max(months, 1); index++) {
            periods.add(start.plusMonths(index));
        }
        return periods;
    }

    private String periodList(List<YearMonth> periods) {
        return periods.stream().map(YearMonth::toString).collect(Collectors.joining(", "));
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
            return new ServiceFeeCharge(0L, 0L, true, "Phí dịch vụ đã được quyết toán trong tháng chuyển phòng.", false);
        }

        int occupantCount = activeOccupantCount(contract.getId());
        if (occupantCount <= 0) {
            return ServiceFeeCharge.empty();
        }

        UtilityTariffSnapshot tariff = readTariff(propertyId, UtilityType.SERVICE_FEE, period.atEndOfMonth());
        int paymentCycleMonths = paymentCycleMonths(contract);
        LocalDate rentStartDate = contract.getRentStartDate() != null
                ? contract.getRentStartDate()
                : contract.getStartDate();
        if (rentStartDate != null
                && !isPaymentCycleStart(period, YearMonth.from(rentStartDate), paymentCycleMonths)) {
            return ServiceFeeCharge.empty();
        }
        if (isServiceFeeWaived(electricityAmount, tariff.serviceFeeWaiveElectricityThreshold())) {
            return new ServiceFeeCharge(
                    tariff.unitPrice(),
                    0L,
                    true,
                    "Ph\u00ed d\u1ecbch v\u1ee5 \u0111\u01b0\u1ee3c mi\u1ec5n v\u00ec ti\u1ec1n \u0111i\u1ec7n < 100.000\u0111",
                    true
            );
        }
        return new ServiceFeeCharge(
                tariff.unitPrice(),
                tariff.unitPrice() * occupantCount * paymentCycleMonths,
                false,
                null,
                true
        );
    }

    static boolean isServiceFeeWaived(long electricityAmount, Long threshold) {
        return threshold != null && threshold > 0L && electricityAmount < threshold;
    }

    static boolean isServiceFeeDue(YearMonth billingPeriod, YearMonth chargeStartPeriod, int paymentCycleMonths) {
        if (billingPeriod == null || chargeStartPeriod == null || paymentCycleMonths <= 0) {
            return false;
        }
        long monthsSinceChargeStart = ChronoUnit.MONTHS.between(chargeStartPeriod, billingPeriod);
        return monthsSinceChargeStart >= 0 && monthsSinceChargeStart % paymentCycleMonths == 0;
    }

    static UtilityBillingRunItemStatus resolveItemStatus(boolean hasWarnings, boolean canInvoice, long subtotal) {
        if (hasWarnings) {
            return UtilityBillingRunItemStatus.WARNING;
        }
        if (!canInvoice || subtotal <= 0) {
            return UtilityBillingRunItemStatus.SKIPPED;
        }
        return UtilityBillingRunItemStatus.READY;
    }

    private int activeOccupantCount(Long contractId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM contract_occupants
                WHERE contract_id = ?
                  AND status = 'ACTIVE'
                """, Integer.class, contractId);
        return count == null ? 0 : count;
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
                    case SERVICE_FEE -> new UtilityTariffSnapshot(
                            50000L,
                            0L,
                            DEFAULT_SERVICE_FEE_WAIVE_ELECTRICITY_THRESHOLD
                    );
                });
    }

    private UtilityTariffSnapshot toSnapshot(UtilityTariffEntity tariff) {
        Long waiveThreshold = tariff.getServiceFeeWaiveElectricityThreshold();
        if (tariff.getUtilityType() == UtilityType.SERVICE_FEE && waiveThreshold == null) {
            waiveThreshold = DEFAULT_SERVICE_FEE_WAIVE_ELECTRICITY_THRESHOLD;
        }
        return new UtilityTariffSnapshot(
                safe(tariff.getUnitPrice()),
                safe(tariff.getFreeAllowance()),
                waiveThreshold
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
        UtilityBillingRunStatus responseStatus = effectiveRunStatus(run);
        return new UtilityBillingRunResponse(
                run.getId(),
                run.getProperty() == null ? null : run.getProperty().getId(),
                run.getProperty() == null ? null : run.getProperty().getName(),
                run.getBillingPeriod(),
                run.getInvoiceReason() == null ? null : run.getInvoiceReason().name(),
                responseStatus == null ? null : responseStatus.name(),
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
        YearMonth billingPeriod = requirePeriod(item.getRun().getBillingPeriod());
        RentCharge calculatedRentCharge = calculateRentCharge(item.getLeaseContract(), billingPeriod);
        long invoicedRentAmount = item.getInvoice() == null
                ? 0L
                : invoiceLineRepository.findByInvoice_IdOrderByIdAsc(item.getInvoice().getId()).stream()
                .filter(line -> line.getLineType() == InvoiceLineType.ROOM_RENT)
                .mapToLong(this::lineAmount)
                .sum();
        RentCharge previewRentCharge = item.getInvoice() == null
                ? buildRentCharge(item.getLeaseContract(), billingPeriod)
                : RentCharge.empty();
        long roomRentAmount = invoicedRentAmount > 0 ? invoicedRentAmount : previewRentCharge.amount();
        boolean hasCycleCharge = roomRentAmount > 0
                || safe(item.getServiceFeeAmount()) > 0
                || Boolean.TRUE.equals(item.getServiceFeeLineRequired());
        List<String> payablePeriods = hasCycleCharge
                ? calculatedRentCharge.periods().stream().map(YearMonth::toString).toList()
                : List.of();
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
                item.getElectricityWaived(),
                item.getElectricityWaiveReason(),
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
                item.getInvoice() == null ? null : item.getInvoice().getInvoiceCode(),
                item.getInvoice() == null || item.getInvoice().getInvoiceType() == null
                        ? (roomRentAmount > 0 ? InvoiceType.RENT.name() : InvoiceType.UTILITY.name())
                        : item.getInvoice().getInvoiceType().name(),
                roomRentAmount,
                payablePeriods
        );
    }

    private InvoiceReason parseReason(String value) {
        if (value == null || value.isBlank()) {
            return InvoiceReason.MONTHLY;
        }
        try {
            return InvoiceReason.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
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
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
    }

    private YearMonth requirePeriod(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        String period = value.trim();
        try {
            return period.contains("/") ? YearMonth.parse(period, LEGACY_PERIOD) : YearMonth.parse(period);
        } catch (DateTimeParseException exception) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
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

    private long lineAmount(InvoiceLineEntity line) {
        if (line == null) {
            return 0L;
        }
        return line.getAmount() == null
                ? safe(line.getUnitPrice()) * (line.getQuantity() == null ? 1L : line.getQuantity())
                : line.getAmount();
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
            long calculatedAmount,
            boolean waived,
            String waiveReason,
            String warning
    ) {
        private Charge(
                BigDecimal previous,
                BigDecimal current,
                BigDecimal usage,
                int quantity,
                long unitPrice,
                long amount,
                String warning
        ) {
            this(previous, current, usage, quantity, unitPrice, amount, amount, false, null, warning);
        }

        static Charge empty() {
            return new Charge(null, null, null, 0, 0L, 0L, null);
        }
    }

    private record UtilityTariffSnapshot(long unitPrice, long freeAllowance, Long serviceFeeWaiveElectricityThreshold) {
    }

    private record RentCharge(
            long monthlyAmount,
            long amount,
            int months,
            List<YearMonth> periods
    ) {
        static RentCharge empty() {
            return new RentCharge(0L, 0L, 0, List.of());
        }
    }

    private record MonthlyInvoices(
            InvoiceEntity primaryInvoice,
            InvoiceEntity utilityInvoice,
            int invoiceCount
    ) {
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
