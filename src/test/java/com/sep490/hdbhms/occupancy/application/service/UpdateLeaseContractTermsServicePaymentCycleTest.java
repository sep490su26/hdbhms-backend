package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractManagementUseCase;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.property.application.service.RoomCommitmentChecker;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateLeaseContractTermsServicePaymentCycleTest {
    private static final LocalDate START_DATE = LocalDate.of(2026, 8, 1);
    private static final LocalDate END_DATE = LocalDate.of(2027, 8, 1);

    @Test
    void rejectsPaymentCycleChangeAfterContractLeavesPreSigningState() {
        LeaseContractEntity contract = contract(LeaseStatus.ACTIVE);
        UpdateLeaseContractTermsService service = service(contract, 0);

        AppException exception = assertThrows(
                AppException.class,
                () -> service.execute(command(3))
        );

        assertEquals(ApiErrorCode.LEASE_PAYMENT_CYCLE_UPDATE_NOT_ALLOWED, exception.getApiErrorCode());
    }

    @Test
    void rejectsPaymentCycleChangeWhenContractHasNonVoidedInvoice() {
        LeaseContractEntity contract = contract(LeaseStatus.DRAFT);
        UpdateLeaseContractTermsService service = service(contract, 1);

        AppException exception = assertThrows(
                AppException.class,
                () -> service.execute(command(3))
        );

        assertEquals(ApiErrorCode.LEASE_PAYMENT_CYCLE_UPDATE_NOT_ALLOWED, exception.getApiErrorCode());
    }

    @Test
    void rejectsPaymentCycleChangeWhenSignedFileAlreadyExists() {
        LeaseContractEntity contract = contract(LeaseStatus.PENDING_SIGNATURE);
        contract.setSignedFile(FileMetadataEntity.builder().id(91L).build());
        UpdateLeaseContractTermsService service = service(contract, 0);

        AppException exception = assertThrows(
                AppException.class,
                () -> service.execute(command(3))
        );

        assertEquals(ApiErrorCode.LEASE_PAYMENT_CYCLE_UPDATE_NOT_ALLOWED, exception.getApiErrorCode());
    }

    @Test
    void allowsPaymentCycleChangeForDraftWithoutInvoice() {
        LeaseContractEntity contract = contract(LeaseStatus.DRAFT);
        JpaLeaseContractRepository repository = mock(JpaLeaseContractRepository.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        LeaseContractWorkflowSupport workflowSupport = mock(LeaseContractWorkflowSupport.class);
        GetLeaseContractManagementUseCase getManagement = mock(GetLeaseContractManagementUseCase.class);
        UpdateLeaseContractTermsService service = new UpdateLeaseContractTermsService(
                repository,
                mock(JpaRoomRepository.class),
                workflowSupport,
                mock(RoomCommitmentChecker.class),
                getManagement,
                jdbcTemplate
        );
        when(repository.findById(11L)).thenReturn(java.util.Optional.of(contract));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(11L))).thenReturn(0L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(11L))).thenReturn(0);
        when(getManagement.findOne(11L)).thenReturn(null);

        service.execute(command(3));

        assertEquals(3, contract.getPaymentCycleMonths());
        verify(repository).save(contract);
    }

    @Test
    void allowsOwnerAdjustmentToChangeFinancialTermsAfterActivation() {
        LeaseContractEntity contract = contract(LeaseStatus.ACTIVE);
        JpaLeaseContractRepository repository = mock(JpaLeaseContractRepository.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        LeaseContractWorkflowSupport workflowSupport = mock(LeaseContractWorkflowSupport.class);
        GetLeaseContractManagementUseCase getManagement = mock(GetLeaseContractManagementUseCase.class);
        UpdateLeaseContractTermsService service = new UpdateLeaseContractTermsService(
                repository,
                mock(JpaRoomRepository.class),
                workflowSupport,
                mock(RoomCommitmentChecker.class),
                getManagement,
                jdbcTemplate
        );
        when(repository.findById(11L)).thenReturn(java.util.Optional.of(contract));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(11L))).thenReturn(0L);
        when(getManagement.findOne(11L)).thenReturn(null);
        doNothing().when(workflowSupport).validateContractTerms(any(), any(), any(), any());

        service.execute(command(3, true, false));

        assertEquals(3, contract.getPaymentCycleMonths());
        verify(repository).save(contract);
    }

    @Test
    void allowsFinancialAdjustmentForPendingSignatureWithSignedFile() {
        LeaseContractEntity contract = contract(LeaseStatus.PENDING_SIGNATURE);
        contract.setSignedFile(FileMetadataEntity.builder().id(91L).build());
        UpdateLeaseContractTermsService service = service(contract, 0);

        service.execute(command(3, true, false));

        assertEquals(3, contract.getPaymentCycleMonths());
    }

    @Test
    void rejectsThreeMonthCycleWhenRemainingTermIsNotDivisible() {
        LeaseContractEntity contract = contract(LeaseStatus.ACTIVE);
        contract.setEndDate(LocalDate.now().plusMonths(8));
        UpdateLeaseContractTermsService service = service(contract, 0);

        AppException exception = assertThrows(
                AppException.class,
                () -> service.execute(command(
                        3,
                        contract.getStartDate(),
                        contract.getEndDate(),
                        true,
                        false
                ))
        );

        assertEquals(ApiErrorCode.LEASE_PAYMENT_CYCLE_TERM_INVALID, exception.getApiErrorCode());
    }

    @Test
    void allowsRenewalFinancialTermsForExpiredContract() {
        LeaseContractEntity contract = contract(LeaseStatus.EXPIRED);
        UpdateLeaseContractTermsService service = service(contract, 0);

        service.execute(command(3, true, true));

        assertEquals(3, contract.getPaymentCycleMonths());
    }

    @Test
    void rejectsDateChangeAfterSigningWithoutRenewalFlow() {
        LeaseContractEntity contract = contract(LeaseStatus.ACTIVE);
        UpdateLeaseContractTermsService service = service(contract, 0);

        AppException exception = assertThrows(
                AppException.class,
                () -> service.execute(new com.sep490.hdbhms.occupancy.application.port.in.command.UpdateLeaseContractTermsCommand(
                        11L,
                        START_DATE,
                        END_DATE.plusMonths(1),
                        1,
                        3_000_000L,
                        3_000_000L
                ))
        );

        assertEquals(ApiErrorCode.LEASE_CONTRACT_DATES_UPDATE_NOT_ALLOWED, exception.getApiErrorCode());
    }

    private UpdateLeaseContractTermsService service(LeaseContractEntity contract, int invoiceCount) {
        JpaLeaseContractRepository repository = mock(JpaLeaseContractRepository.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        LeaseContractWorkflowSupport workflowSupport = mock(LeaseContractWorkflowSupport.class);
        when(repository.findById(11L)).thenReturn(java.util.Optional.of(contract));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(11L))).thenReturn(0L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(11L))).thenReturn(invoiceCount);
        doNothing().when(workflowSupport).validateContractTerms(any(), any(), any(), any());
        doCallRealMethod().when(workflowSupport).validatePaymentCycleMatchesTerm(any(), any(), any());

        return new UpdateLeaseContractTermsService(
                repository,
                mock(JpaRoomRepository.class),
                workflowSupport,
                mock(RoomCommitmentChecker.class),
                mock(GetLeaseContractManagementUseCase.class),
                jdbcTemplate
        );
    }

    private LeaseContractEntity contract(LeaseStatus status) {
        return LeaseContractEntity.builder()
                .id(11L)
                .status(status)
                .paymentCycleMonths(1)
                .monthlyRent(3_000_000L)
                .depositAmount(3_000_000L)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .build();
    }

    private com.sep490.hdbhms.occupancy.application.port.in.command.UpdateLeaseContractTermsCommand command(
            int paymentCycleMonths
    ) {
        return command(paymentCycleMonths, false, false);
    }

    private com.sep490.hdbhms.occupancy.application.port.in.command.UpdateLeaseContractTermsCommand command(
            int paymentCycleMonths,
            boolean allowPostSigningFinancialChange,
            boolean allowPostSigningDateChange
    ) {
        return command(
                paymentCycleMonths,
                START_DATE,
                END_DATE,
                allowPostSigningFinancialChange,
                allowPostSigningDateChange
        );
    }

    private com.sep490.hdbhms.occupancy.application.port.in.command.UpdateLeaseContractTermsCommand command(
            int paymentCycleMonths,
            LocalDate startDate,
            LocalDate endDate,
            boolean allowPostSigningFinancialChange,
            boolean allowPostSigningDateChange
    ) {
        return new com.sep490.hdbhms.occupancy.application.port.in.command.UpdateLeaseContractTermsCommand(
                11L,
                startDate,
                endDate,
                paymentCycleMonths,
                3_000_000L,
                3_000_000L,
                allowPostSigningFinancialChange,
                allowPostSigningDateChange
        );
    }
}
