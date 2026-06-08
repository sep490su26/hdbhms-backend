package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetLeaseContractDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetListLeaseContractsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetRoomDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetMyListLeaseContractsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetRoomDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractManagementService;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractQueryService;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractQueryDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractRenewalResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomRentalHistoryResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.LeaseContractWebMapper;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/lease-contracts")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LeaseContractController {
    GetRoomDetailsUseCase getRoomDetailsUseCase;
    LeaseContractWebMapper leaseContractWebMapper;
    GetMyListLeaseContractsUseCase getMyListLeaseContractsUseCase;
    GetLeaseContractDetailsUseCase getLeaseContractDetailsUseCase;
    LeaseContractManagementService leaseContractManagementService;
    LeaseContractQueryService leaseContractQueryService;

    @GetMapping("/management")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<List<LeaseContractManagementResponse>> getManagementContracts() {
        return ApiResponse.<List<LeaseContractManagementResponse>>builder()
                .data(leaseContractManagementService.findAllForManagement())
                .build();
    }

    @GetMapping("/management/{leaseContractId}")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<LeaseContractQueryDetailsResponse> getManagementContractDetails(
            @PathVariable Long leaseContractId
    ) {
        return ApiResponse.<LeaseContractQueryDetailsResponse>builder()
                .data(leaseContractQueryService.getManagementContractDetails(leaseContractId))
                .build();
    }

    @GetMapping("/management/rooms/{roomId}/rental-history")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<RoomRentalHistoryResponse> getManagementRoomRentalHistory(
            @PathVariable Long roomId
    ) {
        return ApiResponse.<RoomRentalHistoryResponse>builder()
                .data(leaseContractQueryService.getManagementRoomRentalHistory(roomId))
                .build();
    }

    @PostMapping("/management/deposits/{depositAgreementId}/signed-file")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<LeaseContractManagementResponse> uploadSignedFileForDeposit(
            @PathVariable Long depositAgreementId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(leaseContractManagementService.uploadSignedFileForDeposit(depositAgreementId, file))
                .build();
    }

    @PostMapping("/management/deposits/{depositAgreementId}/draft")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<LeaseContractManagementResponse> createDraftLeaseContractForDeposit(
            @PathVariable Long depositAgreementId
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(leaseContractManagementService.createDraftLeaseContractForDeposit(depositAgreementId))
                .build();
    }

    @PostMapping("/{leaseContractId}/signed-file")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<LeaseContractManagementResponse> uploadSignedFile(
            @PathVariable Long leaseContractId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(leaseContractManagementService.uploadSignedFile(leaseContractId, file))
                .build();
    }

    @PostMapping("/{leaseContractId}/activate")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<LeaseContractManagementResponse> activateLeaseContract(
            @PathVariable Long leaseContractId
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(leaseContractManagementService.activate(leaseContractId))
                .build();
    }

    @PatchMapping("/{leaseContractId}/terms")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<LeaseContractManagementResponse> updateLeaseContractTerms(
            @PathVariable Long leaseContractId,
            @Valid @RequestBody LeaseContractTermsUpdateRequest request
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(leaseContractManagementService.updateTerms(
                        leaseContractId,
                        request.startDate(),
                        request.endDate(),
                        request.paymentCycleMonths(),
                        request.monthlyRent(),
                        request.depositAmount()
                ))
                .build();
    }

    @PostMapping("/{leaseContractId}/liquidate")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<LeaseContractManagementResponse> liquidateLeaseContract(
            @PathVariable Long leaseContractId,
            @RequestBody(required = false) LeaseContractLiquidationRequest request
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(leaseContractManagementService.liquidate(
                        leaseContractId,
                        request != null ? request.liquidationDate() : null,
                        request != null ? request.reason() : null
                ))
                .build();
    }

    @PostMapping("/{leaseContractId}/renew")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<LeaseContractRenewalResponse> renewLeaseContract(
            @PathVariable Long leaseContractId,
            @Valid @RequestBody LeaseContractRenewalRequest request
    ) {
        return ApiResponse.<LeaseContractRenewalResponse>builder()
                .data(leaseContractManagementService.renew(
                        leaseContractId,
                        request.newStartDate(),
                        request.newEndDate(),
                        request.monthlyRent(),
                        request.paymentCycleMonths(),
                        request.depositAmount(),
                        request.note()
                ))
                .build();
    }

    @PostMapping("/{leaseContractId}/tenant-intention")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<LeaseContractManagementResponse> recordTenantIntention(
            @PathVariable Long leaseContractId,
            @Valid @RequestBody TenantIntentionRequest request
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(leaseContractManagementService.recordTenantIntention(
                        leaseContractId,
                        request.intention(),
                        request.expectedMoveOutDate(),
                        request.note()
                ))
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<PageResponse<LeaseContractResponse>> getMyLeaseContracts(
            @RequestParam(required = false) LeaseStatus status,
            @RequestParam(required = false) LocalDateTime signedFrom,
            @RequestParam(required = false) LocalDateTime signedTo,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        return ApiResponse.<PageResponse<LeaseContractResponse>>builder()
                .data(
                        PageResponse.fromPageToPageResponse(
                                getMyListLeaseContractsUseCase.execute(
                                        new GetListLeaseContractsQuery(
                                                userId,
                                                status,
                                                signedFrom,
                                                signedTo,
                                                pageable
                                        )
                                ).map(leaseContract -> {
                                    Room room = getRoomDetailsUseCase.execute(
                                            new GetRoomDetailsQuery(leaseContract.getRoomId())
                                    );
                                    return LeaseContractResponse.builder()
                                            .id(leaseContract.getId())
                                            .contractCode(leaseContract.getContractCode())
                                            .roomId(room.getId())
                                            .roomCode(room.getRoomCode())
                                            .roomName(room.getName())
                                            .status(leaseContract.getStatus())
                                            .signedAt(leaseContract.getSignedAt())
                                            .build();
                                })
                        )
                )
                .build();
    }

    @GetMapping("/me/active-rooms")
    public ApiResponse<List<LeaseContractQueryService.ActiveRoomItem>> getMyActiveRooms() {
        return ApiResponse.<List<LeaseContractQueryService.ActiveRoomItem>>builder()
                .data(leaseContractQueryService.getMyActiveRooms())
                .build();
    }

    @GetMapping("/{leaseContractId}")
    public ApiResponse<LeaseContractDetailsResponse> getLeaseContractDetails(
            @PathVariable("leaseContractId") Long leaseContractId
    ) {
        LeaseContract leaseContract = getLeaseContractDetailsUseCase.execute(
                new GetLeaseContractDetailsQuery(leaseContractId)
        );
        Room room = getRoomDetailsUseCase.execute(
                new GetRoomDetailsQuery(leaseContract.getRoomId())
        );
        return ApiResponse.<LeaseContractDetailsResponse>builder()
                .data(
                        leaseContractWebMapper.toDetailsResponse(
                                leaseContract,
                                room
                        )
                )
                .build();
    }

    public record LeaseContractLiquidationRequest(
            LocalDate liquidationDate,
            String reason
    ) {
    }

    public record LeaseContractTermsUpdateRequest(
            @NotNull(message = "Ngày bắt đầu hợp đồng là bắt buộc.")
            LocalDate startDate,
            @NotNull(message = "Ngày kết thúc hợp đồng là bắt buộc.")
            LocalDate endDate,
            @NotNull(message = "Chu kỳ thanh toán là bắt buộc.")
            Integer paymentCycleMonths,
            @NotNull(message = "Giá thuê mỗi tháng là bắt buộc.")
            @Positive(message = "Giá thuê mỗi tháng phải lớn hơn 0.")
            Long monthlyRent,
            @NotNull(message = "Tiền cọc là bắt buộc.")
            @PositiveOrZero(message = "Tiền cọc phải lớn hơn hoặc bằng 0.")
            Long depositAmount
    ) {
    }

    public record LeaseContractRenewalRequest(
            @Size(max = 80, message = "Mã hợp đồng mới không được vượt quá 80 ký tự.")
            String newContractCode,
            @NotNull(message = "Ngày bắt đầu mới là bắt buộc.")
            LocalDate newStartDate,
            @NotNull(message = "Ngày kết thúc mới là bắt buộc.")
            LocalDate newEndDate,
            @NotNull(message = "Giá thuê mỗi tháng là bắt buộc.")
            @Positive(message = "Giá thuê mỗi tháng phải lớn hơn 0.")
            Long monthlyRent,
            @NotNull(message = "Chu kỳ thanh toán là bắt buộc.")
            Integer paymentCycleMonths,
            @NotNull(message = "Tiền cọc là bắt buộc.")
            @PositiveOrZero(message = "Tiền cọc phải lớn hơn hoặc bằng 0.")
            Long depositAmount,
            @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự.")
            String note
    ) {
    }

    public record TenantIntentionRequest(
            @NotNull(message = "Ý định khách là bắt buộc.")
            String intention,
            LocalDate expectedMoveOutDate,
            @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự.")
            String note
    ) {
    }
}
