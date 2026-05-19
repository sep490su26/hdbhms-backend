package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.command.SendDepositFormCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.BookRoomUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.DepositFormRepository;
import com.sep490.hdbhms.occupancy.domain.model.DepositForm;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
                command.fullName(),
                command.dob(),
                command.email(),
                command.phone(),
                command.permanentAddress(),
                command.idNumber(),
                command.idIssueDate(),
                command.idIssuePlace(),
                command.expectedMoveInDate(),
                command.expectedLeaseSignDate()
        );
        log.info(depositForm.toString());
        depositFormRepository.save(depositForm);
    }
}
