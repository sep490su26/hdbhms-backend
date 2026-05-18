package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.command.SendDepositFormCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.BookRoomUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.DepositFormRepository;
import com.sep490.hdbhms.occupancy.domain.model.DepositForm;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookRoomService implements BookRoomUseCase {
    DepositFormRepository depositFormRepository;

    @Override
    public void initDepositForm(SendDepositFormCommand command) {
        DepositForm depositForm = DepositForm.newDepositForm(
                command.roomId(),
                command.idNumber(),
                command.fullName(),
                command.email(),
                command.phone(),
                command.expectedMoveInDate(),
                command.expectedLeaseSignDate()
        );
        depositFormRepository.save(depositForm);
    }
}
