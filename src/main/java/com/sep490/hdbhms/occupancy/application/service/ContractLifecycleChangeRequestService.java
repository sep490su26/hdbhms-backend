package com.sep490.hdbhms.occupancy.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.changerequest.application.service.ChangeRequestNotificationService;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestRepository;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.AssignedRole;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestStatus;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequesterRole;
import com.sep490.hdbhms.changerequest.domain.value_objects.TargetType;
import com.sep490.hdbhms.changerequest.infrastructure.persistence.jpa.JpaChangeRequestRepository;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Gender;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.PersonProfileEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaPersonProfileRepository;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractOccupantRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.property.application.service.RoomCommitmentChecker;
import com.sep490.hdbhms.shared.utils.RequestCodeBuilder;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ContractLifecycleChangeRequestService {
    static final List<RequestStatus> OPEN_REQUEST_STATUSES = List.of(
            RequestStatus.PENDING,
            RequestStatus.UNDER_REVIEW,
            RequestStatus.PROCESSING
    );
    static final List<LeaseStatus> LIQUIDATABLE_STATUSES = List.of(
            LeaseStatus.ACTIVE,
            LeaseStatus.EXPIRING_SOON,
            LeaseStatus.EXPIRED,
            LeaseStatus.TERMINATION_PENDING
    );
    static final List<LeaseStatus> RENEWABLE_STATUSES = List.of(
            LeaseStatus.ACTIVE,
            LeaseStatus.EXPIRING_SOON,
            LeaseStatus.EXPIRED
    );
    static final List<LeaseStatus> ADD_CO_OCCUPANT_STATUSES = List.of(
            LeaseStatus.ACTIVE,
            LeaseStatus.EXPIRING_SOON
    );

    JpaLeaseContractRepository leaseContractRepository;
    JpaPersonProfileRepository personProfileRepository;
    JpaContractOccupantRepository contractOccupantRepository;
    JpaChangeRequestRepository jpaChangeRequestRepository;
    ChangeRequestRepository changeRequestRepository;
    ObjectMapper objectMapper;
    ChangeRequestNotificationService changeRequestNotificationService;
    RoomCommitmentChecker roomCommitmentChecker;
    LeaseContractWorkflowSupport workflowSupport;
    JdbcTemplate jdbcTemplate;

    @Transactional
    public ChangeRequest submitLiquidationRequest(
            Long leaseContractId,
            LocalDate liquidationDate,
            String reason
    ) {
        UserPrincipal principal = currentPrincipal();
        LeaseContractEntity contract = getContract(leaseContractId);
        assertLifecycleAllowed(contract, RequestType.CONTRACT_LIQUIDATION);
        LeaseContractDebtPolicy.requireNoOutstandingDebt(jdbcTemplate, contract.getId());
        return createChangeRequest(
                principal,
                contract,
                RequestType.CONTRACT_LIQUIDATION,
                "Yêu cầu thanh lý hợp đồng " + contract.getRoom().getName(),
                reason,
                liquidationPayload(
                        contract,
                        liquidationDate,
                        reason
                )
        );
    }

    @Transactional
    public ChangeRequest submitRenewalRequest(
            Long leaseContractId,
            LocalDate newStartDate,
            LocalDate newEndDate,
            Integer renewalTermMonths,
            Long monthlyRent,
            Integer paymentCycleMonths,
            Long depositAmount,
            String note
    ) {
        UserPrincipal principal = currentPrincipal();
        LeaseContractEntity contract = getContract(leaseContractId);
        assertLifecycleAllowed(contract, RequestType.CONTRACT_RENEWAL);
        LeaseContractDebtPolicy.requireNoOutstandingDebt(jdbcTemplate, contract.getId());
        workflowSupport.validatePaymentCycleMatchesTerm(
                newStartDate,
                newEndDate,
                paymentCycleMonths
        );
        return createChangeRequest(
                principal,
                contract,
                RequestType.CONTRACT_RENEWAL,
                "Yêu cầu gia hạn hợp đồng " + contract.getContractCode(),
                note,
                renewalPayload(contract, newStartDate, newEndDate, renewalTermMonths, monthlyRent, paymentCycleMonths, depositAmount, note)
        );
    }

    @Transactional
    public ChangeRequest submitAddCoOccupantRequest(
            Long leaseContractId,
            Long tenantProfileId,
            String fullName,
            LocalDate dob,
            Gender gender,
            String phone,
            String email,
            String permanentAddress,
            LocalDate moveInDate,
            String note
    ) {
        UserPrincipal principal = currentPrincipal();
        LeaseContractEntity contract = getContract(leaseContractId);
        assertLifecycleAllowed(contract, RequestType.ADD_CO_OCCUPANT);
        PersonProfileEntity profile = resolveOrCreateCoOccupantProfile(
                tenantProfileId,
                fullName,
                dob,
                gender,
                phone,
                email,
                permanentAddress
        );
        assertCanRequestAddCoOccupant(contract, profile);
        String occupantName = profile.getFullName() == null ? "người ở cùng" : profile.getFullName();
        return createChangeRequest(
                principal,
                contract,
                RequestType.ADD_CO_OCCUPANT,
                "Yêu cầu thêm người ở cùng hợp đồng " + contract.getContractCode(),
                (note == null || note.isBlank()) ? "Thêm người ở cùng: " + occupantName : note,
                addCoOccupantPayload(contract, profile, moveInDate, note)
        );
    }

    @Transactional
    public ChangeRequest submitFinancialTermsRequest(
            Long leaseContractId,
            Long monthlyRent,
            Integer paymentCycleMonths,
            String note
    ) {
        UserPrincipal principal = currentPrincipal();
        if (principal.getRole() != Role.MANAGER) {
            throw new AppException(ApiErrorCode.FORBIDDEN_OPERATION);
        }
        LeaseContractEntity contract = getContract(leaseContractId);
        boolean signedTerms = List.of(LeaseStatus.SIGNED, LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON).contains(contract.getStatus())
                || (contract.getStatus() == LeaseStatus.PENDING_SIGNATURE
                && (contract.getSignedFile() != null || contract.getSignedAt() != null));
        if (!signedTerms) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (monthlyRent == null || monthlyRent <= 0
                || !List.of(1, 3).contains(paymentCycleMonths)) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (monthlyRent.equals(contract.getMonthlyRent())
                && paymentCycleMonths.equals(contract.getPaymentCycleMonths())) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        LeaseContractDebtPolicy.requireNoOutstandingDebt(jdbcTemplate, contract.getId());
        workflowSupport.validateContractTerms(
                contract.getStartDate(),
                paymentCycleMonths,
                monthlyRent,
                contract.getDepositAmount()
        );
        workflowSupport.validatePaymentCycleMatchesTerm(
                resolveRemainingTermStart(contract),
                contract.getEndDate(),
                paymentCycleMonths
        );
        Map<String, Object> payload = basePayload("CONTRACT_FINANCIAL_TERMS_ADJUSTMENT", contract);
        payload.put("monthlyRent", monthlyRent);
        payload.put("paymentCycleMonths", paymentCycleMonths);
        payload.put("previousMonthlyRent", contract.getMonthlyRent());
        payload.put("previousPaymentCycleMonths", contract.getPaymentCycleMonths());
        payload.put("note", note);
        return createChangeRequest(
                principal,
                contract,
                RequestType.RENT_PRICE_ADJUSTMENT,
                "Yêu cầu điều chỉnh điều khoản tài chính " + contract.getContractCode(),
                note,
                payload
        );
    }

    private ChangeRequest createChangeRequest(
            UserPrincipal principal,
            LeaseContractEntity contract,
            RequestType requestType,
            String title,
            String description,
            Map<String, Object> payload
    ) {
        if (jpaChangeRequestRepository.existsByRequestTypeAndTargetTypeAndTargetIdAndStatusIn(
                requestType,
                TargetType.CONTRACT,
                contract.getId(),
                OPEN_REQUEST_STATUSES
        )) {
            throw new AppException(ApiErrorCode.OPERATION_CONFLICT);
        }

        ChangeRequest changeRequest = ChangeRequest.builder()
                .requestCode(nextChangeRequestCode(requestType, contract))
                .requestType(requestType)
                .requesterId(principal.getId())
                .requesterRole(toRequesterRole(principal.getRole()))
                .targetType(TargetType.CONTRACT)
                .targetId(contract.getId())
                .title(title)
                .description(description == null ? "" : description)
                .requestPayload(writePayload(payload))
                .assignedRole(AssignedRole.OWNER)
                .status(RequestStatus.PENDING)
                .build();
        ChangeRequest savedRequest = changeRequestRepository.save(changeRequest);
        changeRequestNotificationService.notifyCreated(savedRequest);
        return savedRequest;
    }

    private Map<String, Object> basePayload(String requestKind, LeaseContractEntity contract) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestKind", requestKind);
        payload.put("contractId", contract.getId());
        payload.put("contractCode", contract.getContractCode());
        payload.put("roomId", contract.getRoom() == null ? null : contract.getRoom().getId());
        payload.put("roomCode", contract.getRoom() == null ? null : contract.getRoom().getRoomCode());
        payload.put("primaryTenantProfileId", contract.getPrimaryTenantProfile() == null
                ? null
                : contract.getPrimaryTenantProfile().getId());
        payload.put("primaryTenantUserId", contract.getPrimaryTenantProfile() == null
                || contract.getPrimaryTenantProfile().getUser() == null
                ? null
                : contract.getPrimaryTenantProfile().getUser().getId());
        payload.put("startDate", contract.getStartDate());
        payload.put("endDate", contract.getEndDate());
        return payload;
    }

    private Map<String, Object> liquidationPayload(
            LeaseContractEntity contract,
            LocalDate liquidationDate,
            String reason
    ) {
        Map<String, Object> payload = basePayload("CONTRACT_LIQUIDATION", contract);
        payload.put("liquidationDate", liquidationDate == null ? LocalDate.now() : liquidationDate);
        payload.put("reason", reason);
        return payload;
    }

    private Map<String, Object> renewalPayload(
            LeaseContractEntity contract,
            LocalDate newStartDate,
            LocalDate newEndDate,
            Integer renewalTermMonths,
            Long monthlyRent,
            Integer paymentCycleMonths,
            Long depositAmount,
            String note
    ) {
        Map<String, Object> payload = basePayload("CONTRACT_RENEWAL", contract);
        payload.put("oldEndDate", contract.getEndDate());
        payload.put("newStartDate", newStartDate);
        payload.put("newEndDate", newEndDate);
        payload.put("renewalTermMonths", renewalTermMonths);
        payload.put("monthlyRent", monthlyRent);
        payload.put("paymentCycleMonths", paymentCycleMonths);
        payload.put("depositAmount", depositAmount);
        payload.put("note", note);
        return payload;
    }

    private LocalDate resolveRemainingTermStart(LeaseContractEntity contract) {
        LocalDate today = LocalDate.now();
        return contract.getStartDate() != null && contract.getStartDate().isAfter(today)
                ? contract.getStartDate()
                : today;
    }

    private Map<String, Object> addCoOccupantPayload(
            LeaseContractEntity contract,
            PersonProfileEntity profile,
            LocalDate moveInDate,
            String note
    ) {
        Map<String, Object> payload = basePayload("ADD_CO_OCCUPANT", contract);
        payload.put("tenantProfileId", profile.getId());
        payload.put("fullName", profile.getFullName());
        payload.put("dob", profile.getDob());
        payload.put("gender", profile.getGender());
        payload.put("phone", profile.getPhone());
        payload.put("email", profile.getEmail());
        payload.put("permanentAddress", profile.getPermanentAddress());
        payload.put("moveInDate", moveInDate == null ? LocalDate.now() : moveInDate);
        payload.put("note", note);
        return payload;
    }

    private void assertLifecycleAllowed(LeaseContractEntity contract, RequestType requestType) {
        if (contract.getDeletedAt() != null) {
            throw new AppException(ApiErrorCode.RESOURCE_NOT_FOUND);
        }
        if (requestType == RequestType.CONTRACT_LIQUIDATION && !LIQUIDATABLE_STATUSES.contains(contract.getStatus())) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (requestType == RequestType.CONTRACT_RENEWAL && !RENEWABLE_STATUSES.contains(contract.getStatus())) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (requestType == RequestType.CONTRACT_RENEWAL
                && contract.getRoom() != null
                && roomCommitmentChecker.checkRenewBlockers(
                        contract.getRoom().getId(),
                        contract.getId(),
                        contract.getEndDate()
                ) != RoomCommitmentChecker.Blocker.NONE) {
            throw new AppException(ApiErrorCode.LEASE_RENEWAL_ROOM_RESERVED_BY_OTHER_TENANT);
        }
        if (requestType == RequestType.ADD_CO_OCCUPANT && !ADD_CO_OCCUPANT_STATUSES.contains(contract.getStatus())) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (requestType == RequestType.ADD_CO_OCCUPANT
                && contract.getRoom() != null
                && roomCommitmentChecker.isSoonVacantBookingCase(
                contract.getRoom().getId(),
                contract.getId(),
                contract.getEndDate()
        )) {
            throw new AppException(ApiErrorCode.ROOM_CO_OCCUPANT_ADD_BLOCKED_BY_BOOKING);
        }
    }

    private PersonProfileEntity resolveOrCreateCoOccupantProfile(
            Long tenantProfileId,
            String fullName,
            LocalDate dob,
            Gender gender,
            String phone,
            String email,
            String permanentAddress
    ) {
        if (tenantProfileId != null) {
            PersonProfileEntity profile = personProfileRepository.findById(tenantProfileId)
                    .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
            if (profile.getDeletedAt() != null) {
                throw new AppException(ApiErrorCode.RESOURCE_NOT_FOUND);
            }
            return profile;
        }

        String cleanedPhone = trimToNull(phone);
        if (cleanedPhone != null) {
            PersonProfileEntity profile = personProfileRepository.findFirstByPhoneAndDeletedAtIsNull(cleanedPhone)
                    .orElse(null);
            if (profile != null) {
                return profile;
            }
        }

        String cleanedFullName = trimToNull(fullName);
        if (cleanedFullName == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        return personProfileRepository.save(PersonProfileEntity.builder()
                .fullName(cleanedFullName)
                .dob(dob)
                .gender(gender == null ? Gender.UNKNOWN : gender)
                .phone(cleanedPhone)
                .email(trimToNull(email))
                .permanentAddress(trimToNull(permanentAddress))
                .build());
    }

    private void assertCanRequestAddCoOccupant(LeaseContractEntity contract, PersonProfileEntity profile) {
        if (contract.getPrimaryTenantProfile() != null
                && contract.getPrimaryTenantProfile().getId() != null
                && contract.getPrimaryTenantProfile().getId().equals(profile.getId())) {
            throw new AppException(ApiErrorCode.OPERATION_CONFLICT);
        }
        if (contractOccupantRepository.findFirstByContract_IdAndTenantProfile_IdAndStatus(
                contract.getId(),
                profile.getId(),
                OccupantStatus.ACTIVE
        ).isPresent()) {
            throw new AppException(ApiErrorCode.OPERATION_CONFLICT);
        }

        RoomEntity room = contract.getRoom();
        int maxOccupants = room != null && room.getMaxOccupants() != null ? room.getMaxOccupants() : 3;
        int activeOccupants = contractOccupantRepository.findAllByContract_IdAndStatus(
                contract.getId(),
                OccupantStatus.ACTIVE
        ).size();
        if (activeOccupants >= maxOccupants) {
            throw new AppException(ApiErrorCode.OPERATION_CONFLICT);
        }
    }

    private LeaseContractEntity getContract(Long leaseContractId) {
        return leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
    }

    private UserPrincipal currentPrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
        return principal;
    }

    private RequesterRole toRequesterRole(Role role) {
        if (role == Role.OWNER) return RequesterRole.OWNER;
        if (role == Role.MANAGER) return RequesterRole.MANAGER;
        return RequesterRole.TENANT;
    }

    private String nextChangeRequestCode(RequestType requestType, LeaseContractEntity contract) {
        RoomEntity room = contract.getRoom();
        return RequestCodeBuilder.nextAvailable(
                requestType,
                room == null ? null : room.getRoomCode(),
                LocalDate.now(),
                changeRequestRepository::existsByRequestCode
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String writePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new AppException(ApiErrorCode.UNDEFINED);
        }
    }
}
