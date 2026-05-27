package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetDepositAgreementDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetDepositAgreementDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.DepositAgreementRepository;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetDepositAgreementDetailsService implements GetDepositAgreementDetailsUseCase {
    DepositAgreementRepository depositAgreementRepository;

    @Override
    public DepositAgreement execute(GetDepositAgreementDetailsQuery query) {
        return depositAgreementRepository.findById(query.depositAgreementId())
                .orElseThrow(() -> new IllegalArgumentException("Deposit agreement not found"));
    }
}
