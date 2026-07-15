package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.PromotionRole;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.RolePromotionStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaRolePromotionRepository;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetListVisitRequestsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetPropertyDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetRoomDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetVisitRequestDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.*;
import com.sep490.hdbhms.occupancy.application.port.out.VisitRequestRepository;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.VisitRequest;
import com.sep490.hdbhms.occupancy.domain.value_objects.VisitRequestStatus;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.CreateVisitRequestRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.VisitRequestStatusUpdateRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.VisitRequestDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.VisitRequestResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.VisitRequestWebMapper;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/visit-requests")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VisitRequestController {
    VisitRequestWebMapper visitRequestWebMapper;
    GetRoomDetailsUseCase getRoomDetailsUseCase;
    GetPropertyDetailsUseCase getPropertyDetailsUseCase;
    CreateVisitRequestUseCase createVisitRequestUseCase;
    GetListVisitRequestsUseCase getListVisitRequestsUseCase;
    GetVisitRequestDetailsUseCase getVisitRequestDetailsUseCase;
    VisitRequestRepository visitRequestRepository;
    JpaRolePromotionRepository jpaRolePromotionRepository;

    @PostMapping
    public ApiResponse<VisitRequestDetailsResponse> createVisitRequest(
            @Valid @RequestBody CreateVisitRequestRequest request
    ) {
        VisitRequest visitRequest = createVisitRequestUseCase.execute(
                visitRequestWebMapper.toCommand(request)
        );
        Room room = getRoomOrNull(visitRequest.getRoomId());
        Property property = getPropertyForVisitRequest(visitRequest, room);
        return ApiResponse.<VisitRequestDetailsResponse>builder()
                .data(
                        visitRequestWebMapper.toDetailsResponse(
                                visitRequest,
                                property,
                                room
                        )
                )
                .build();
    }

    @GetMapping
    @PreAuthorize("@visitRequestAccessGuard.canManage(authentication)")
    public ApiResponse<PageResponse<VisitRequestResponse>> getVisitRequests(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String propertyCode,
            @RequestParam(required = false) String roomCode,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) VisitRequestStatus status,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<Long> scopedPropertyIds = scopedPropertyIds(principal, propertyId);
        return ApiResponse.<PageResponse<VisitRequestResponse>>builder()
                .data(
                        PageResponse.fromPageToPageResponse(
                                getListVisitRequestsUseCase.execute(
                                                new GetListVisitRequestsQuery(
                                                        keyword,
                                                        propertyCode,
                                                        roomCode,
                                                        propertyId,
                                                        scopedPropertyIds,
                                                        roomId,
                                                        status,
                                                        from,
                                                        to,
                                                        pageable
                                                )
                                        )
                                        .map(this::toListResponse)
                        )
                )
                .build();
    }

    @GetMapping("/trash")
    @PreAuthorize("@visitRequestAccessGuard.canManage(authentication)")
    public ApiResponse<PageResponse<VisitRequestResponse>> getDeletedVisitRequests(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<Long> scopedPropertyIds = scopedPropertyIds(principal, null);
        return ApiResponse.<PageResponse<VisitRequestResponse>>builder()
                .data(
                        PageResponse.fromPageToPageResponse(
                                visitRequestRepository.findDeleted(scopedPropertyIds, pageable)
                                        .map(this::toListResponse)
                        )
                )
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@visitRequestAccessGuard.canManage(authentication)")
    public ApiResponse<VisitRequestDetailsResponse> getVisitRequestDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        VisitRequest visitRequest = getVisitRequestDetailsUseCase.execute(
                new GetVisitRequestDetailsQuery(id)
        );
        Room room = getRoomOrNull(visitRequest.getRoomId());
        Property property = getPropertyForVisitRequest(visitRequest, room);
        assertCanAccessProperty(principal, property == null ? visitRequest.getPropertyId() : property.getId());
        return ApiResponse.<VisitRequestDetailsResponse>builder()
                .data(
                        visitRequestWebMapper.toDetailsResponse(
                                visitRequest,
                                property,
                                room
                        )
                )
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@visitRequestAccessGuard.canManage(authentication)")
    public ApiResponse<VisitRequestDetailsResponse> moveVisitRequestToTrash(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        VisitRequest current = findVisitRequest(id);
        assertCanAccessVisitRequest(principal, current);
        VisitRequest deleted = current.getDeletedAt() == null
                ? visitRequestRepository.save(copyWithDeletedState(current, LocalDateTime.now(), null))
                : current;

        return ApiResponse.<VisitRequestDetailsResponse>builder()
                .data(toDetailsResponse(deleted))
                .build();
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("@visitRequestAccessGuard.canManage(authentication)")
    public ApiResponse<VisitRequestDetailsResponse> restoreVisitRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        VisitRequest current = findVisitRequest(id);
        assertCanAccessVisitRequest(principal, current);
        VisitRequest restored = current.getDeletedAt() == null
                ? current
                : visitRequestRepository.save(copyWithDeletedState(current, null, null));

        return ApiResponse.<VisitRequestDetailsResponse>builder()
                .data(toDetailsResponse(restored))
                .build();
    }

    @DeleteMapping("/{id}/force")
    @PreAuthorize("@visitRequestAccessGuard.canForceDelete(authentication)")
    public ApiResponse<Void> permanentlyDeleteVisitRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        assertOwner(principal);
        findVisitRequest(id);
        visitRequestRepository.deleteById(id);
        return ApiResponse.<Void>builder().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@visitRequestAccessGuard.canManage(authentication)")
    public ApiResponse<VisitRequestDetailsResponse> updateVisitRequestStatus(
            @PathVariable Long id,
            @Valid @RequestBody VisitRequestStatusUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        VisitRequest current = getVisitRequestDetailsUseCase.execute(
                new GetVisitRequestDetailsQuery(id)
        );
        assertCanAccessVisitRequest(principal, current);
        VisitRequest updated = VisitRequest.builder()
                .id(current.getId())
                .propertyId(current.getPropertyId())
                .roomId(current.getRoomId())
                .visitorName(current.getVisitorName())
                .visitorPhone(current.getVisitorPhone())
                .visitorEmail(current.getVisitorEmail())
                .preferredStart(current.getPreferredStart())
                .status(request.getStatus())
                .notes(current.getNotes())
                .createdAt(current.getCreatedAt())
                .updatedAt(current.getUpdatedAt())
                .deletedAt(current.getDeletedAt())
                .deletedByUserId(current.getDeletedByUserId())
                .build();
        updated = visitRequestRepository.save(updated);

        Room room = getRoomOrNull(updated.getRoomId());
        Property property = getPropertyForVisitRequest(updated, room);
        return ApiResponse.<VisitRequestDetailsResponse>builder()
                .data(
                        visitRequestWebMapper.toDetailsResponse(
                                updated,
                                property,
                                room
                        )
                )
                .build();
    }

    private VisitRequestResponse toListResponse(VisitRequest visitRequest) {
        Room room = getRoomOrNull(visitRequest.getRoomId());
        Property property = getPropertyForVisitRequest(visitRequest, room);
        Long propertyId = property == null ? visitRequest.getPropertyId() : property.getId();
        return VisitRequestResponse.builder()
                .id(visitRequest.getId())
                .propertyId(propertyId)
                .roomId(visitRequest.getRoomId())
                .visitorName(visitRequest.getVisitorName())
                .visitorEmail(visitRequest.getVisitorEmail())
                .visitorPhone(visitRequest.getVisitorPhone())
                .createdAt(visitRequest.getCreatedAt())
                .deletedAt(visitRequest.getDeletedAt())
                .roomName(room == null ? null : room.getName())
                .propertyName(property == null ? null : property.getName())
                .preferredStart(visitRequest.getPreferredStart())
                .status(visitRequest.getStatus())
                .notes(visitRequest.getNotes())
                .build();
    }

    private VisitRequestDetailsResponse toDetailsResponse(VisitRequest visitRequest) {
        Room room = getRoomOrNull(visitRequest.getRoomId());
        Property property = getPropertyForVisitRequest(visitRequest, room);
        return visitRequestWebMapper.toDetailsResponse(
                visitRequest,
                property,
                room
        );
    }

    private VisitRequest findVisitRequest(Long id) {
        return visitRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ApiErrorCode.VISIT_001));
    }

    private VisitRequest copyWithDeletedState(
            VisitRequest current,
            LocalDateTime deletedAt,
            Long deletedByUserId
    ) {
        return VisitRequest.builder()
                .id(current.getId())
                .propertyId(current.getPropertyId())
                .roomId(current.getRoomId())
                .visitorName(current.getVisitorName())
                .visitorPhone(current.getVisitorPhone())
                .visitorEmail(current.getVisitorEmail())
                .preferredStart(current.getPreferredStart())
                .status(current.getStatus())
                .notes(current.getNotes())
                .createdAt(current.getCreatedAt())
                .updatedAt(current.getUpdatedAt())
                .deletedAt(deletedAt)
                .deletedByUserId(deletedByUserId)
                .build();
    }

    private Property getPropertyOrNull(Long propertyId) {
        return propertyId == null ? null : getPropertyDetailsUseCase.execute(new GetPropertyDetailsQuery(propertyId));
    }

    private Property getPropertyForVisitRequest(VisitRequest visitRequest, Room room) {
        Long propertyId = room != null && room.getPropertyId() != null
                ? room.getPropertyId()
                : visitRequest.getPropertyId();
        return getPropertyOrNull(propertyId);
    }

    private List<Long> scopedPropertyIds(UserPrincipal principal, Long requestedPropertyId) {
        assertAuthenticatedManagerOrOwner(principal);
        if (principal.getRole() == Role.OWNER) {
            return null;
        }

        List<Long> propertyIds = managerPropertyIds(principal.getId());
        if (requestedPropertyId != null && !propertyIds.contains(requestedPropertyId)) {
            throw new AppException(ApiErrorCode.UNAUTHORIZED);
        }
        return propertyIds;
    }

    private void assertCanAccessVisitRequest(UserPrincipal principal, VisitRequest visitRequest) {
        Room room = getRoomOrNull(visitRequest.getRoomId());
        Property property = getPropertyForVisitRequest(visitRequest, room);
        Long propertyId = property == null ? visitRequest.getPropertyId() : property.getId();
        assertCanAccessProperty(principal, propertyId);
    }

    private void assertCanAccessProperty(UserPrincipal principal, Long propertyId) {
        assertAuthenticatedManagerOrOwner(principal);
        if (principal.getRole() == Role.OWNER) {
            return;
        }
        if (propertyId == null || !managerPropertyIds(principal.getId()).contains(propertyId)) {
            throw new AppException(ApiErrorCode.UNAUTHORIZED);
        }
    }

    private void assertAuthenticatedManagerOrOwner(UserPrincipal principal) {
        if (principal == null) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
        if (principal.getRole() != Role.OWNER && principal.getRole() != Role.MANAGER) {
            throw new AppException(ApiErrorCode.UNAUTHORIZED);
        }
    }

    private void assertOwner(UserPrincipal principal) {
        if (principal == null) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
        if (principal.getRole() != Role.OWNER) {
            throw new AppException(ApiErrorCode.UNAUTHORIZED);
        }
    }

    private List<Long> managerPropertyIds(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return jpaRolePromotionRepository
                .findActivePropertyIds(userId, PromotionRole.MANAGER, RolePromotionStatus.ACTIVE)
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Room getRoomOrNull(Long roomId) {
        return roomId == null ? null : getRoomDetailsUseCase.execute(new GetRoomDetailsQuery(roomId));
    }
}
