package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.usecase.BookRoomUseCase;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.SendDepositFormRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.RoomWebMapper;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deposit/")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepositController {
    RoomWebMapper roomWebMapper;
    BookRoomUseCase bookRoomUseCase;

    @PostMapping("/checkout")
    public ApiResponse<Void> bookRoom(
            @RequestPart("metadata") SendDepositFormRequest request,
            @RequestPart("idFrontFile") MultipartFile idFrontFile,
            @RequestPart("idBackFile") MultipartFile idBackFile,
            @RequestPart("idPortraitFile") MultipartFile portraitFile
    ) {
        bookRoomUseCase.initDepositForm(
                roomWebMapper.toCommand(request, idFrontFile, idBackFile, portraitFile)
        );
        return ApiResponse.<Void>builder().build();
    }
}
