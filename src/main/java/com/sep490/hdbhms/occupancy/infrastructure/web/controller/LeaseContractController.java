package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetLeaseContractDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetListLeaseContractsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetRoomDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetMyListLeaseContractsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetRoomDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractManagementService;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.LeaseContractWebMapper;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
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

    @GetMapping("/management")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<List<LeaseContractManagementResponse>> getManagementContracts() {
        return ApiResponse.<List<LeaseContractManagementResponse>>builder()
                .data(leaseContractManagementService.findAllForManagement())
                .build();
    }

    @PostMapping("/management/deposits/{depositAgreementId}/signed-file")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<LeaseContractManagementResponse> uploadSignedFileForDeposit(
            @PathVariable Long depositAgreementId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(leaseContractManagementService.uploadSignedFileForDeposit(depositAgreementId, file))
                .build();
    }

    @PostMapping("/management/deposits/{depositAgreementId}/draft")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<LeaseContractManagementResponse> createDraftLeaseContractForDeposit(
            @PathVariable Long depositAgreementId
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(leaseContractManagementService.createDraftLeaseContractForDeposit(depositAgreementId))
                .build();
    }

    @PostMapping("/{leaseContractId}/signed-file")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<LeaseContractManagementResponse> uploadSignedFile(
            @PathVariable Long leaseContractId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(leaseContractManagementService.uploadSignedFile(leaseContractId, file))
                .build();
    }

    @PostMapping("/{leaseContractId}/activate")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<LeaseContractManagementResponse> activateLeaseContract(
            @PathVariable Long leaseContractId
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(leaseContractManagementService.activate(leaseContractId))
                .build();
    }

    @PostMapping("/{leaseContractId}/liquidate")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
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
                                            .roomCode(room.getRoomCode())
                                            .status(leaseContract.getStatus())
                                            .signedAt(leaseContract.getSignedAt())
                                            .build();
                                })
                        )
                )
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
}
