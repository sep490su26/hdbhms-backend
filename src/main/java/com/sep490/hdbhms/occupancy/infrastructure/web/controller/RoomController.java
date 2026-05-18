package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.usecase.BookRoomUseCase;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.SendDepositFormRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.RoomWebMapper;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomController {
    BookRoomUseCase bookRoomUseCase;
    RoomWebMapper roomWebMapper;

    @PostMapping("/book")
    public ApiResponse<Void> bookRoom(@ModelAttribute SendDepositFormRequest request) {
        log.info(request.toString());
        bookRoomUseCase.initDepositForm(roomWebMapper.toCommand(request));
        return ApiResponse.<Void>builder().build();
    }
}
