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
import com.sep490.hdbhms.shared.utils.RequestCodeBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    @Transactional
    public ChangeRequest submitLiquidationRequest(
            Long leaseContractId,
            LocalDate liquidationDate,
            String reason,
            String liquidationMode,
            List<Long> leavingProfileIds,
            List<Long> stayingProfileIds,
            Long replacementPrimaryTenantProfileId
    ) {
        UserPrincipal principal = currentPrincipal();
        LeaseContractEntity contract = getContract(leaseContractId);
        assertLifecycleAllowed(contract, RequestType.CONTRACT_LIQUIDATION);
        return createChangeRequest(
                principal,
                contract,
                RequestType.CONTRACT_LIQUIDATION,
                "Yêu cầu thanh lý hợp đồng " + contract.getRoom().getName(),
                reason,
                liquidationPayload(
                        contract,
                        liquidationDate,
                        reason,
                        liquidationMode,
                        leavingProfileIds,
                        stayingProfileIds,
                        replacementPrimaryTenantProfileId
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
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hợp đồng đã có yêu cầu đang cho duyệt.");
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
        payload.put("startDate", contract.getStartDate());
        payload.put("endDate", contract.getEndDate());
        return payload;
    }

    private Map<String, Object> liquidationPayload(
            LeaseContractEntity contract,
            LocalDate liquidationDate,
            String reason,
            String liquidationMode,
            List<Long> leavingProfileIds,
            List<Long> stayingProfileIds,
            Long replacementPrimaryTenantProfileId
    ) {
        Map<String, Object> payload = basePayload("CONTRACT_LIQUIDATION", contract);
        payload.put("liquidationDate", liquidationDate == null ? LocalDate.now() : liquidationDate);
        payload.put("reason", reason);
        if (liquidationMode != null && !liquidationMode.isBlank()) {
            payload.put("liquidationMode", liquidationMode.trim());
        }
        if (leavingProfileIds != null && !leavingProfileIds.isEmpty()) {
            payload.put("leavingProfileIds", leavingProfileIds);
        }
        if (stayingProfileIds != null && !stayingProfileIds.isEmpty()) {
            payload.put("stayingProfileIds", stayingProfileIds);
        }
        if (replacementPrimaryTenantProfileId != null) {
            payload.put("replacementPrimaryTenantProfileId", replacementPrimaryTenantProfileId);
        }
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng thuê.");
        }
        if (requestType == RequestType.CONTRACT_LIQUIDATION && !LIQUIDATABLE_STATUSES.contains(contract.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hợp đồng không thể thanh lý.");
        }
        if (requestType == RequestType.CONTRACT_RENEWAL && !RENEWABLE_STATUSES.contains(contract.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hợp đồng không thể gia hạn.");
        }
        if (requestType == RequestType.ADD_CO_OCCUPANT && !ADD_CO_OCCUPANT_STATUSES.contains(contract.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hợp đồng không thế thêm người ở cùng.");
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
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ người o cùng."));
            if (profile.getDeletedAt() != null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ người ở cùng.");
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên người ở cùng là bắt buộc.");
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
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Người này đã là người đứng tên hợp đồng.");
        }
        if (contractOccupantRepository.findFirstByContract_IdAndTenantProfile_IdAndStatus(
                contract.getId(),
                profile.getId(),
                OccupantStatus.ACTIVE
        ).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Người này đã là người ở cùng trong hợp đồng.");
        }

        RoomEntity room = contract.getRoom();
        int maxOccupants = room != null && room.getMaxOccupants() != null ? room.getMaxOccupants() : 3;
        int activeOccupants = contractOccupantRepository.findAllByContract_IdAndStatus(
                contract.getId(),
                OccupantStatus.ACTIVE
        ).size();
        if (activeOccupants >= maxOccupants) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phòng đã dạt số người ở tối đa.");
        }
    }

    private LeaseContractEntity getContract(Long leaseContractId) {
        return leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng thuê."));
    }

    private UserPrincipal currentPrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated.");
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tạo được nội dung yêu cầu.");
        }
    }
}
