package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetDepositAgreementDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetListDepositAgreementsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetRoomDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetDepositAgreementDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetMyListDepositAgreementsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetRoomDetailsUseCase;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.DepositAgreementDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.DepositAgreementResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.DepositAgreementWebMapper;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deposit-agreements")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepositAgreementController {
    GetRoomDetailsUseCase getRoomDetailsUseCase;
    DepositAgreementWebMapper depositAgreementWebMapper;
    GetMyListDepositAgreementsUseCase getMyListDepositAgreementsUseCase;
    GetDepositAgreementDetailsUseCase getDepositAgreementDetailsUseCase;

    @GetMapping("/me")
    public ApiResponse<PageResponse<DepositAgreementResponse>> getMyDepositAgreements(
            @RequestParam(required = false) DepositAgreementStatus status,
            @RequestParam(required = false) LocalDateTime signedFrom,
            @RequestParam(required = false) LocalDateTime signedTo,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        return ApiResponse.<PageResponse<DepositAgreementResponse>>builder()
                .data(
                        PageResponse.fromPageToPageResponse(
                                getMyListDepositAgreementsUseCase.execute(
                                        new GetListDepositAgreementsQuery(
                                                userId,
                                                status,
                                                signedFrom,
                                                signedTo,
                                                pageable
                                        )
                                ).map(depositAgreement -> {
                                    Room room = getRoomDetailsUseCase.execute(
                                            new GetRoomDetailsQuery(depositAgreement.getRoomId())
                                    );
                                    return DepositAgreementResponse.builder()
                                            .id(depositAgreement.getId())
                                            .depositCode(depositAgreement.getDepositCode())
                                            .roomCode(room.getRoomCode())
                                            .status(depositAgreement.getStatus())
                                            .confirmedAt(depositAgreement.getConfirmedAt())
                                            .build();
                                })
                        )
                )
                .build();
    }

    @GetMapping("/{depositAgreementId}")
    public ApiResponse<DepositAgreementDetailsResponse> getDepositAgreementDetails(
            @PathVariable("depositAgreementId") Long depositAgreementId
    ) {
        DepositAgreement depositAgreement = getDepositAgreementDetailsUseCase.execute(
                new GetDepositAgreementDetailsQuery(depositAgreementId)
        );
        Room room = getRoomDetailsUseCase.execute(
                new GetRoomDetailsQuery(depositAgreement.getRoomId())
        );
        return ApiResponse.<DepositAgreementDetailsResponse>builder()
                .data(
                        depositAgreementWebMapper.toDetailsResponse(
                                depositAgreement,
                                room
                        )
                )
                .build();
    }
}
