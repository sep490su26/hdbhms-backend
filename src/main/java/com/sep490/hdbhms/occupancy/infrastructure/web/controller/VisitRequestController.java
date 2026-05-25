package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetListVisitRequestsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetPropertyDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetVisitRequestDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.CreateVisitRequestUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetListVisitRequestsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetPropertyDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetVisitRequestDetailsUseCase;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.domain.model.VisitRequest;
import com.sep490.hdbhms.occupancy.domain.value_objects.VisitRequestSource;
import com.sep490.hdbhms.occupancy.domain.value_objects.VisitRequestStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.VisitRequestEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaVisitRequestRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.CreateVisitRequestRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.VisitRequestCreateRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.VisitRequestStatusUpdateRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.VisitRequestUpdateRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.*;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.VisitRequestWebMapper;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VisitRequestController {
    VisitRequestWebMapper visitRequestWebMapper;
    GetPropertyDetailsUseCase getPropertyDetailsUseCase;
    CreateVisitRequestUseCase createVisitRequestUseCase;
    GetListVisitRequestsUseCase getListVisitRequestsUseCase;
    GetVisitRequestDetailsUseCase getVisitRequestDetailsUseCase;
    JpaVisitRequestRepository jpaVisitRequestRepository;
    JpaPropertyRepository jpaPropertyRepository;
    JpaRoomRepository jpaRoomRepository;

    @PostMapping("/visit-requests")
    public ApiResponse<VisitRequestDetailsResponse> createVisitRequest(
            @Valid @RequestBody CreateVisitRequestRequest request
    ) {
        VisitRequest visitRequest = createVisitRequestUseCase.execute(
                visitRequestWebMapper.toCommand(request)
        );
        Property property = getPropertyDetailsUseCase.execute(
                new GetPropertyDetailsQuery(visitRequest.getPropertyId())
        );
        return ApiResponse.<VisitRequestDetailsResponse>builder()
                .data(
                        visitRequestWebMapper.toDetailsResponse(
                                visitRequest,
                                property
                        )
                )
                .build();
    }

    @GetMapping("/visit-requests")
    public ApiResponse<PageResponse<VisitRequestResponse>> getVisitRequests(
            String keyword,
            String propertyCode,
            String roomCode,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        return ApiResponse.<PageResponse<VisitRequestResponse>>builder()
                .data(
                        PageResponse.fromPageToPageResponse(
                                getListVisitRequestsUseCase.execute(
                                                new GetListVisitRequestsQuery(
                                                        keyword,
                                                        propertyCode,
                                                        roomCode,
                                                        null,
                                                        null,
                                                        null,
                                                        from,
                                                        to,
                                                        pageable
                                                )
                                        )
                                        .map(visitRequestWebMapper::toResponse)
                        )
                )
                .build();
    }

    @GetMapping("/tenants/{tenantId}/visit-requests")
    @Transactional(readOnly = true)
    @PreAuthorize("@visitRequestAccessGuard.canManage(authentication)")
    public ApiResponse<VisitRequestListResponse> getTenantVisitRequests(
            @PathVariable Long tenantId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) VisitRequestStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "preferredStart,desc") String sort
    ) {
        Page<VisitRequestEntity> visitRequests = jpaVisitRequestRepository.findAll(
                buildVisitRequestSpecification(keyword, propertyId, roomId, status, fromDate, toDate, false),
                PageRequest.of(Math.max(page - 1, 0), size, parseSort(sort))
        );

        return ApiResponse.<VisitRequestListResponse>builder()
                .data(VisitRequestListResponse.builder()
                        .items(visitRequests.map(this::toManagementResponse).getContent())
                        .total(visitRequests.getTotalElements())
                        .page(visitRequests.getNumber() + 1)
                        .size(visitRequests.getSize())
                        .totalPages(visitRequests.getTotalPages())
                        .build())
                .build();
    }

    @GetMapping("/tenants/{tenantId}/visit-requests/trash")
    @Transactional(readOnly = true)
    @PreAuthorize("@visitRequestAccessGuard.canManage(authentication)")
    public ApiResponse<VisitRequestListResponse> getTenantVisitRequestTrash(
            @PathVariable Long tenantId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) VisitRequestStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "deletedAt,desc") String sort
    ) {
        Page<VisitRequestEntity> visitRequests = jpaVisitRequestRepository.findAll(
                buildVisitRequestSpecification(keyword, propertyId, roomId, status, fromDate, toDate, true),
                PageRequest.of(Math.max(page - 1, 0), size, parseSort(sort))
        );

        return ApiResponse.<VisitRequestListResponse>builder()
                .data(VisitRequestListResponse.builder()
                        .items(visitRequests.map(this::toManagementResponse).getContent())
                        .total(visitRequests.getTotalElements())
                        .page(visitRequests.getNumber() + 1)
                        .size(visitRequests.getSize())
                        .totalPages(visitRequests.getTotalPages())
                        .build())
                .build();
    }

    @GetMapping("/tenants/{tenantId}/visit-requests/stats")
    @Transactional(readOnly = true)
    @PreAuthorize("@visitRequestAccessGuard.canManage(authentication)")
    public ApiResponse<VisitRequestStatsResponse> getTenantVisitRequestStats(
            @PathVariable Long tenantId,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        LocalDate today = LocalDate.now();
        long todayCount = jpaVisitRequestRepository.count(
                buildVisitRequestSpecification(null, propertyId, roomId, null, today, today, false)
        );
        long pendingCount = jpaVisitRequestRepository.count(
                buildVisitRequestSpecification(null, propertyId, roomId, VisitRequestStatus.PENDING, fromDate, toDate, false)
        );
        long viewedCount = jpaVisitRequestRepository.count(
                buildVisitRequestSpecification(null, propertyId, roomId, VisitRequestStatus.VIEWED, fromDate, toDate, false)
        );
        int closingRate = 0;

        return ApiResponse.<VisitRequestStatsResponse>builder()
                .data(VisitRequestStatsResponse.builder()
                        .todayCount(todayCount)
                        .pendingCount(pendingCount)
                        .viewedCount(viewedCount)
                        .closingRate(closingRate)
                        .build())
                .build();
    }

    @PostMapping("/tenants/{tenantId}/visit-requests")
    @Transactional
    @PreAuthorize("@visitRequestAccessGuard.canManage(authentication)")
    public ApiResponse<VisitRequestResponse> createTenantVisitRequest(
            @PathVariable Long tenantId,
            @Valid @RequestBody VisitRequestCreateRequest request
    ) {
        PropertyEntity property = requireProperty(request.getPropertyId());
        RoomEntity room = requireRoomInProperty(request.getRoomId(), request.getPropertyId());
        VisitRequestEntity entity = VisitRequestEntity.builder()
                .property(property)
                .room(room)
                .visitorName(request.getCustomerName())
                .visitorPhone(request.getPhone())
                .preferredStart(request.getAppointmentAt())
                .status(VisitRequestStatus.PENDING)
                .source(request.getSource() == null ? VisitRequestSource.OTHER : request.getSource())
                .notes(request.getNote())
                .build();

        return ApiResponse.<VisitRequestResponse>builder()
                .data(toManagementResponse(jpaVisitRequestRepository.save(entity)))
                .build();
    }

    @PutMapping("/tenants/{tenantId}/visit-requests/{id}")
    @Transactional
    @PreAuthorize("@visitRequestAccessGuard.canManage(authentication)")
    public ApiResponse<VisitRequestResponse> updateTenantVisitRequest(
            @PathVariable Long tenantId,
            @PathVariable Long id,
            @Valid @RequestBody VisitRequestUpdateRequest request
    ) {
        VisitRequestEntity entity = requireVisitRequest(id);
        PropertyEntity property = requireProperty(request.getPropertyId());
        RoomEntity room = requireRoomInProperty(request.getRoomId(), request.getPropertyId());

        entity.setProperty(property);
        entity.setRoom(room);
        entity.setVisitorName(request.getCustomerName());
        entity.setVisitorPhone(request.getPhone());
        entity.setPreferredStart(request.getAppointmentAt());
        entity.setSource(request.getSource() == null ? VisitRequestSource.OTHER : request.getSource());
        entity.setNotes(request.getNote());
        entity.setStatus(request.getStatus() == null ? VisitRequestStatus.PENDING : request.getStatus());

        return ApiResponse.<VisitRequestResponse>builder()
                .data(toManagementResponse(jpaVisitRequestRepository.save(entity)))
                .build();
    }

    @PatchMapping("/tenants/{tenantId}/visit-requests/{id}/status")
    @Transactional
    @PreAuthorize("@visitRequestAccessGuard.canManage(authentication)")
    public ApiResponse<VisitRequestResponse> updateTenantVisitRequestStatus(
            @PathVariable Long tenantId,
            @PathVariable Long id,
            @Valid @RequestBody VisitRequestStatusUpdateRequest request
    ) {
        VisitRequestEntity entity = requireVisitRequest(id);
        entity.setStatus(request.getStatus());

        return ApiResponse.<VisitRequestResponse>builder()
                .data(toManagementResponse(jpaVisitRequestRepository.save(entity)))
                .build();
    }

    @DeleteMapping("/tenants/{tenantId}/visit-requests/{id}")
    @Transactional
    @PreAuthorize("@visitRequestAccessGuard.canManage(authentication)")
    public ApiResponse<Void> deleteTenantVisitRequest(
            @PathVariable Long tenantId,
            @PathVariable Long id
    ) {
        VisitRequestEntity entity = requireVisitRequest(id);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setDeletedBy(AuthUtils.getCurrentAuthenticationId());
        jpaVisitRequestRepository.save(entity);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/tenants/{tenantId}/visit-requests/{id}/restore")
    @Transactional
    @PreAuthorize("@visitRequestAccessGuard.canManage(authentication)")
    public ApiResponse<VisitRequestResponse> restoreTenantVisitRequest(
            @PathVariable Long tenantId,
            @PathVariable Long id
    ) {
        VisitRequestEntity entity = requireDeletedVisitRequest(id);
        entity.setDeletedAt(null);
        entity.setDeletedBy(null);
        return ApiResponse.<VisitRequestResponse>builder()
                .data(toManagementResponse(jpaVisitRequestRepository.save(entity)))
                .build();
    }

    @DeleteMapping("/tenants/{tenantId}/visit-requests/{id}/force")
    @Transactional
    @PreAuthorize("@visitRequestAccessGuard.canForceDelete(authentication)")
    public ApiResponse<Void> forceDeleteTenantVisitRequest(
            @PathVariable Long tenantId,
            @PathVariable Long id
    ) {
        VisitRequestEntity entity = jpaVisitRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ApiErrorCode.VISIT_001));
        if (entity.getDeletedAt() == null) {
            throw new AppException(ApiErrorCode.VISIT_005);
        }
        jpaVisitRequestRepository.delete(entity);
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/visit-requests/{id}")
    public ApiResponse<VisitRequestDetailsResponse> getVisitRequestDetails(
            @PathVariable Long id
    ) {
        VisitRequest visitRequest = getVisitRequestDetailsUseCase.execute(
                new GetVisitRequestDetailsQuery(id)
        );
        Property property = getPropertyDetailsUseCase.execute(
                new GetPropertyDetailsQuery(visitRequest.getPropertyId())
        );
        return ApiResponse.<VisitRequestDetailsResponse>builder()
                .data(
                        visitRequestWebMapper.toDetailsResponse(
                                visitRequest,
                                property
                        )
                )
                .build();
    }

    private VisitRequestEntity requireVisitRequest(Long id) {
        return jpaVisitRequestRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ApiErrorCode.VISIT_001));
    }

    private VisitRequestEntity requireDeletedVisitRequest(Long id) {
        return jpaVisitRequestRepository.findByIdAndDeletedAtIsNotNull(id)
                .orElseThrow(() -> new AppException(ApiErrorCode.VISIT_001));
    }

    private PropertyEntity requireProperty(Long propertyId) {
        return jpaPropertyRepository.findById(propertyId)
                .orElseThrow(() -> new AppException(ApiErrorCode.VISIT_002));
    }

    private RoomEntity requireRoomInProperty(Long roomId, Long propertyId) {
        if (roomId == null) {
            return null;
        }

        RoomEntity room = jpaRoomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ApiErrorCode.VISIT_002));
        if (room.getProperty() == null || !room.getProperty().getId().equals(propertyId)) {
            throw new AppException(ApiErrorCode.VISIT_002);
        }
        return room;
    }

    private Specification<VisitRequestEntity> buildVisitRequestSpecification(
            String keyword,
            Long propertyId,
            Long roomId,
            VisitRequestStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            boolean deletedOnly
    ) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();
            predicates = deletedOnly
                    ? cb.and(predicates, cb.isNotNull(root.get("deletedAt")))
                    : cb.and(predicates, cb.isNull(root.get("deletedAt")));

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates = cb.and(predicates, cb.or(
                        cb.like(cb.lower(root.get("visitorName")), pattern),
                        cb.like(root.get("visitorPhone"), pattern)
                ));
            }

            if (propertyId != null) {
                predicates = cb.and(predicates, cb.equal(root.join("property").get("id"), propertyId));
            }

            if (roomId != null) {
                predicates = cb.and(predicates, cb.equal(root.join("room").get("id"), roomId));
            }

            if (status != null) {
                predicates = cb.and(predicates, cb.equal(root.get("status"), status));
            }

            if (fromDate != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("preferredStart"), fromDate.atStartOfDay()));
            }

            if (toDate != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("preferredStart"), toDate.atTime(LocalTime.MAX)));
            }

            return predicates;
        };
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "preferredStart");
        }
        String[] parts = sort.split(",");
        String property = parts[0].isBlank() ? "preferredStart" : parts[0];
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }

    private VisitRequestResponse toManagementResponse(VisitRequestEntity entity) {
        String roomCode = entity.getRoom() == null ? null : entity.getRoom().getRoomCode();
        return VisitRequestResponse.builder()
                .id(entity.getId())
                .customerName(entity.getVisitorName())
                .phone(entity.getVisitorPhone())
                .propertyId(entity.getProperty() == null ? null : entity.getProperty().getId())
                .propertyName(entity.getProperty() == null ? null : entity.getProperty().getName())
                .roomId(entity.getRoom() == null ? null : entity.getRoom().getId())
                .roomCode(roomCode)
                .appointmentAt(entity.getPreferredStart())
                .status(entity.getStatus())
                .statusLabel(entity.getStatus() == null ? null : entity.getStatus().label())
                .source(entity.getSource())
                .note(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }
}
